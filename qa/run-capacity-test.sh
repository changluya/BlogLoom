#!/usr/bin/env bash
#
# =============================================================================
# BlogLoom 容量测试（run-capacity-test.sh）
# =============================================================================
# 目的：
#   测量「给定容器资源配置下的最大 QPS」，即 CPU/内存配额 → 最大 QPS 的对应关系。
#
# 压测工具：
#   优先使用 wrk（多线程、低开销，避免压测客户端先于服务端成为瓶颈）；
#   未安装 wrk 时回退到 ab。
#
# 最大 QPS 判定：
#   在无失败请求（socket 错误 = 0、非 2xx = 0）的前提下观测到的最高吞吐。
#   同时检查应用 CPU 是否达到配额，以判断瓶颈是否真的在应用 CPU。
#
# 方法：
#   对每个资源场景（如 1 核/1G、2 核/1G、4 核/1G、8 核/1G）：
#     1) 用 compose 覆盖文件为 blogloom-app 设置 cpus / mem_limit 并重建容器；
#     2) 预热；
#     3) 逐级提高并发压测（QPS 测量阶段不采样，避免干扰）；
#     4) 取无失败请求下的峰值 QPS 作为该配置的最大 QPS；
#     5) 在峰值并发下采样容器 CPU/内存，记录实际资源占用。
#   全部场景结束后恢复为无限制配置。
#
# 用法（仓库根目录）：
#   qa/run-capacity-test.sh
#   TOOL=wrk DURATION=10 RAMP_CONCURRENCY="50 100 200 400" qa/run-capacity-test.sh
#
# 可调环境变量：
#   TOOL             压测工具：wrk（默认，若可用）或 ab
#   SCENARIOS        场景列表，格式 "名称|CPU核数|内存上限"，空格分隔
#   DURATION         每个并发档位压测时长（秒，默认 10）
#   RAMP_CONCURRENCY 并发梯度（默认 "50 100 200 400"）
#   WRK_THREADS      wrk 线程数（默认 4）
#   RESOURCE_DURATION 峰值资源采样时长（秒，默认 12）
#   TARGET_NAME/PATH/ACCEPT 压测目标接口（默认 blogs 列表）
#
# 产物：qa/results/capacity-<时间戳>/
#   capacity.tsv       各场景最大 QPS 与资源配置
#   raw/ramp_<场景>.tsv 各场景并发梯度明细
#   report.md          容量报告
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DOCKER_DIR="$REPO_ROOT/docker"
COMPOSE_FILE="$DOCKER_DIR/docker-compose.yml"
LIMITS_FILE="$SCRIPT_DIR/docker-compose.limits.yml"
ENV_FILE="$DOCKER_DIR/.env"

WEB_HOST="${WEB_HOST:-127.0.0.1}"
WEB_PORT="${WEB_PORT:-}"
if [ -z "$WEB_PORT" ] && [ -f "$ENV_FILE" ]; then
    WEB_PORT="$(grep -E '^WEB_PORT=' "$ENV_FILE" | tail -n1 | cut -d= -f2- | tr -d '"' || true)"
fi
WEB_PORT="${WEB_PORT:-18080}"
BASE_URL="http://${WEB_HOST}:${WEB_PORT}"

APP_CONTAINER="${APP_CONTAINER:-blogloom-app}"
DB_CONTAINER="${DB_CONTAINER:-blogloom-mysql}"

DURATION="${DURATION:-10}"
RAMP_CONCURRENCY="${RAMP_CONCURRENCY:-50 100 200 400}"
WRK_THREADS="${WRK_THREADS:-4}"
RESOURCE_DURATION="${RESOURCE_DURATION:-12}"
TARGET_NAME="${TARGET_NAME:-blogs}"
TARGET_PATH="${TARGET_PATH:-/blogs?pageNum=1&pageSize=10}"
TARGET_ACCEPT="${TARGET_ACCEPT:-application/json}"
SCENARIOS="${SCENARIOS:-1c_1g|1|1g 2c_1g|2|1g 4c_1g|4|1g 8c_1g|8|1g}"

# 选择压测工具
if [ "${TOOL:-}" = "ab" ]; then
    LOADER="ab"
elif command -v wrk >/dev/null 2>&1; then
    LOADER="wrk"
elif command -v ab >/dev/null 2>&1; then
    LOADER="ab"
else
    printf '[capacity][ERROR] 未找到 wrk 或 ab\n' >&2
    exit 1
fi

TS="$(date +%Y%m%d-%H%M%S)"
OUT="$SCRIPT_DIR/results/capacity-$TS"
RAW="$OUT/raw"
mkdir -p "$RAW"

log() { printf '[capacity] %s\n' "$*"; }
fail() { printf '[capacity][ERROR] %s\n' "$*" >&2; exit 1; }

docker inspect "$APP_CONTAINER" >/dev/null 2>&1 || fail "容器 $APP_CONTAINER 未运行，请先执行 docker/scripts/deploy.sh"
[ -f "$LIMITS_FILE" ] || fail "缺少 $LIMITS_FILE"
[ -f "$ENV_FILE" ] || fail "缺少 $ENV_FILE，请先执行 docker/scripts/deploy.sh"
ulimit -n 8192 2>/dev/null || true

compose_limited() {
    QA_APP_CPUS="$1" QA_APP_MEM_LIMIT="$2" \
        docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" -f "$LIMITS_FILE" "${@:3}"
}
compose_default() {
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

wait_ready() {
    local start now
    start="$(date +%s)"
    while true; do
        if curl -sf -o /dev/null "$BASE_URL/site"; then
            return 0
        fi
        now="$(date +%s)"
        [ $((now - start)) -gt 180 ] && fail "应用启动超时"
        sleep 1
    done
}

# 带单位的时间 -> 毫秒
unit_to_ms() {
    local v="$1"
    case "$v" in
        *us) awk -v x="${v%us}" 'BEGIN{printf "%.3f", x/1000}' ;;
        *ms) awk -v x="${v%ms}" 'BEGIN{printf "%.3f", x}' ;;
        *s)  awk -v x="${v%s}"  'BEGIN{printf "%.3f", x*1000}' ;;
        *)   printf '%s' "$v" ;;
    esac
}

# 运行一次压测，输出 TSV：concurrency rps mean_ms p50_ms p90_ms p99_ms complete failed non2xx
run_load_row() {
    local out="$1" conc="$2"
    if [ "$LOADER" = "wrk" ]; then
        wrk -t"$WRK_THREADS" -c"$conc" -d"${DURATION}s" --latency \
            -H "Accept: $TARGET_ACCEPT" "${BASE_URL}${TARGET_PATH}" > "$out" 2>&1 || true
        local rps complete mean p50 p90 p99 errs non2xx
        rps="$(awk '/Requests\/sec:/{print $2; exit}' "$out")"
        complete="$(awk '/requests in/{print $1; exit}' "$out")"
        mean="$(unit_to_ms "$(awk '/^[[:space:]]*Latency/{print $2; exit}' "$out")")"
        p50="$(unit_to_ms "$(awk '$1=="50%"{print $2; exit}' "$out")")"
        p90="$(unit_to_ms "$(awk '$1=="90%"{print $2; exit}' "$out")")"
        p99="$(unit_to_ms "$(awk '$1=="99%"{print $2; exit}' "$out")")"
        errs="$(awk '/Socket errors:/{for(i=1;i<=NF;i++){if($i ~ /^[0-9]+,?$/){gsub(/,/,"",$i); s+=$i}}} END{print s+0}' "$out")"
        non2xx="$(awk '/Non-2xx/{print $NF; exit}' "$out")"
        printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
            "$conc" "${rps:-0}" "${mean:-0}" "${p50:-0}" "${p90:-0}" "${p99:-0}" \
            "${complete:-0}" "${errs:-0}" "${non2xx:-0}"
    else
        ab -t "$DURATION" -c "$conc" -k -q -H "Accept: $TARGET_ACCEPT" \
            "${BASE_URL}${TARGET_PATH}" > "$out" 2>&1 || true
        local rps mean p50 p90 p99 complete failed non2xx
        rps="$(awk '/Requests per second/{print $4; exit}' "$out")"
        mean="$(awk '/Time per request:/{print $4; exit}' "$out")"
        p50="$(awk '$1=="50%"{print $2; exit}' "$out")"
        p90="$(awk '$1=="90%"{print $2; exit}' "$out")"
        p99="$(awk '$1=="99%"{print $2; exit}' "$out")"
        complete="$(awk '/Complete requests:/{print $3; exit}' "$out")"
        failed="$(awk '/Failed requests:/{print $3; exit}' "$out")"
        non2xx="$(awk '/Non-2xx responses:/{print $3; exit}' "$out")"
        printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
            "$conc" "${rps:-0}" "${mean:-0}" "${p50:-0}" "${p90:-0}" "${p99:-0}" \
            "${complete:-0}" "${failed:-0}" "${non2xx:-0}"
    fi
}

# 后台流式资源采样
start_sampler() {
    : > "$OUT/samples_raw.txt"
    docker stats --format '{{.Name}},{{.CPUPerc}},{{.MemUsage}},{{.MemPerc}}' \
        "$APP_CONTAINER" "$DB_CONTAINER" > "$OUT/samples_raw.txt" 2>/dev/null &
    SAMPLER_PID=$!
}
stop_sampler() {
    if [ -n "${SAMPLER_PID:-}" ]; then
        kill -9 "$SAMPLER_PID" 2>/dev/null || true
        wait "$SAMPLER_PID" 2>/dev/null || true
        {
            echo "name,cpu_perc,mem_usage,mem_perc"
            sed -E "s/.*(${APP_CONTAINER}|${DB_CONTAINER},)/\1/" "$OUT/samples_raw.txt" \
                | grep -E "^(${APP_CONTAINER}|${DB_CONTAINER})," || true
        } > "$OUT/samples.csv"
    fi
}
trap 'stop_sampler' EXIT

# ---------------------------------------------------------------------------
# 环境信息
# ---------------------------------------------------------------------------
{
    echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S %z')"
    echo "host_os=$(uname -s) $(uname -r)"
    echo "host_arch=$(uname -m)"
    echo "host_cpus=$(sysctl -n hw.ncpu 2>/dev/null || nproc 2>/dev/null || echo '?')"
    echo "host_mem_bytes=$(sysctl -n hw.memsize 2>/dev/null || echo '?')"
    echo "docker_version=$(docker version --format '{{.Server.Version}}' 2>/dev/null || echo '?')"
    echo "docker_cpus=$(docker info --format '{{.NCPU}}' 2>/dev/null || echo '?')"
    echo "docker_mem_bytes=$(docker info --format '{{.MemTotal}}' 2>/dev/null || echo '?')"
    echo "web_url=$BASE_URL"
    echo "target=$TARGET_PATH"
    echo "loader=$LOADER"
    echo "duration=$DURATION"
    echo "ramp_concurrency=$RAMP_CONCURRENCY"
} > "$OUT/meta.env"

log "压测工具：$LOADER"
log "输出目录：$OUT"
CAP_TSV="$OUT/capacity.tsv"
printf 'name\tcpus\tmem\tpeak_qps\tpeak_conc\tpeak_p90_ms\tpeak_p99_ms\tpeak_cpu_perc\tpeak_mem_mib\n' > "$CAP_TSV"

# ---------------------------------------------------------------------------
# 逐场景测试
# ---------------------------------------------------------------------------
for scenario in $SCENARIOS; do
    IFS='|' read -r name cpus mem <<< "$scenario"
    log "==== 场景 ${name}（CPU=${cpus}，内存=${mem}）===="

    compose_limited "$cpus" "$mem" up -d --force-recreate blogloom >/dev/null 2>&1
    wait_ready
    sleep 3

    # 预热
    if [ "$LOADER" = "wrk" ]; then
        wrk -t"$WRK_THREADS" -c20 -d3s -H "Accept: $TARGET_ACCEPT" "${BASE_URL}${TARGET_PATH}" >/dev/null 2>&1 || true
        wrk -t"$WRK_THREADS" -c20 -d2s -H "Accept: application/json" "${BASE_URL}/site" >/dev/null 2>&1 || true
    else
        ab -n 400 -c 20 -k -q -H "Accept: $TARGET_ACCEPT" "${BASE_URL}${TARGET_PATH}" >/dev/null 2>&1 || true
    fi
    sleep 2

    SC_TSV="$RAW/ramp_${name}.tsv"
    printf 'concurrency\trps\tmean_ms\tp50_ms\tp90_ms\tp99_ms\tcomplete\tfailed\tnon2xx\n' > "$SC_TSV"
    for conc in $RAMP_CONCURRENCY; do
        log "  - 并发 ${conc}"
        run_load_row "$RAW/ab_${name}_c${conc}.txt" "$conc" >> "$SC_TSV"
    done

    # 取「错误率 <= 1% 且非2xx=0」下的峰值 QPS（错误率 = 失败 / 总请求）
    read -r peak_qps peak_conc peak_p90 peak_p99 <<< "$(awk -F'\t' '
        NR>1 {
            err=$8+0; tot=$7+0+err; rate=(tot>0)?err/tot:1;
            if (rate<=0.01 && $9+0==0 && $2+0>max){max=$2+0; c=$1; p90=$5; p99=$6}
        }
        END { printf "%s %s %s %s", max+0, c, p90, p99 }' "$SC_TSV")"
    if [ "${peak_qps:-0}" = "0" ]; then
        # 兜底：无满足条件的档位时取绝对峰值
        read -r peak_qps peak_conc peak_p90 peak_p99 <<< "$(awk -F'\t' 'NR>1 && $2+0>max{max=$2+0;c=$1;p90=$5;p99=$6} END{printf "%s %s %s %s", max+0, c, p90, p99}' "$SC_TSV")"
    fi

    # 峰值并发下采样实际资源占用
    start_sampler
    if [ "$LOADER" = "wrk" ]; then
        wrk -t"$WRK_THREADS" -c"$peak_conc" -d"${RESOURCE_DURATION}s" -H "Accept: $TARGET_ACCEPT" \
            "${BASE_URL}${TARGET_PATH}" > "$RAW/ab_${name}_resource.txt" 2>&1 || true
    else
        ab -t "$RESOURCE_DURATION" -c "$peak_conc" -k -q -H "Accept: $TARGET_ACCEPT" \
            "${BASE_URL}${TARGET_PATH}" > "$RAW/ab_${name}_resource.txt" 2>&1 || true
    fi
    stop_sampler

    peak_cpu="$(awk -F, 'NR>1 && $1=="'"$APP_CONTAINER"'" {v=$2+0; if(v>m)m=v} END{print m+0}' "$OUT/samples.csv")"
    peak_mem_mib="$(awk -F, 'NR>1 && $1=="'"$APP_CONTAINER"'" {n=$3+0; u=$3; gsub(/[0-9.]/,"",u); f=(u=="GiB")?1024:1; if(n*f>m)m=n*f} END{printf "%.1f", m+0}' "$OUT/samples.csv")"

    printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
        "$name" "$cpus" "$mem" "$peak_qps" "$peak_conc" "${peak_p90:-0}" "${peak_p99:-0}" "$peak_cpu" "$peak_mem_mib" >> "$CAP_TSV"
    log "  最大 QPS=${peak_qps} @ 并发 ${peak_conc}（峰值 CPU ${peak_cpu}%，内存 ${peak_mem_mib} MiB）"
done

# 恢复默认（无限制）配置
log "恢复默认无限制配置 ..."
compose_default up -d --force-recreate blogloom >/dev/null 2>&1
wait_ready

# 生成报告
log "生成报告 ..."
python3 "$SCRIPT_DIR/lib/capacity_report.py" "$OUT" > "$OUT/report.md"
log "完成。报告：$OUT/report.md"
printf '\n================ 报告摘要 ================\n'
sed -n '1,90p' "$OUT/report.md"
