#!/usr/bin/env bash
#
# =============================================================================
# BlogLoom 容器压测与资源占用测试（run-stress-test.sh）
# =============================================================================
# 用途：
#   针对已通过 docker 一键启动的 BlogLoom（blogloom-app + blogloom-mysql），
#   测量：服务启动耗时、空闲资源占用、前台首页/博客相关接口 QPS 与延迟、
#        不同并发下的资源消耗，并生成一份 Markdown 报告。
#
# 前置条件：
#   - 已执行 docker/scripts/deploy.sh，两个容器处于运行状态；
#   - 本机具备 ab（ApacheBench）、docker、curl、python3。
#
# 用法（在仓库根目录执行）：
#   qa/run-stress-test.sh
#   WEB_PORT=18080 DURATION=8 qa/run-stress-test.sh
#
# 常用可调环境变量：
#   WEB_HOST / WEB_PORT            被测服务地址（默认 127.0.0.1:18080，端口缺省从 docker/.env 读取）
#   DURATION                       并发梯度每个档位的压测时长（秒，默认 8）
#   ENDPOINT_DURATION              单接口对比压测时长（秒，默认 5）
#   ENDPOINT_CONCURRENCY           单接口对比并发（默认 20）
#   RAMP_CONCURRENCY               并发梯度（默认 "1 10 25 50 100 200"）
#   RAMP_TARGET_NAME/PATH/ACCEPT   并发梯度目标接口（默认 blogs 列表）
#
# 产物：qa/results/<时间戳>/
#   raw/               原始数据（ab 输出、docker stats 采样）
#   meta.env           环境与资源配置
#   report.md          最终报告
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# 路径解析
# ---------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DOCKER_DIR="$REPO_ROOT/docker"

# ---------------------------------------------------------------------------
# 测试配置
# ---------------------------------------------------------------------------
WEB_HOST="${WEB_HOST:-127.0.0.1}"
WEB_PORT="${WEB_PORT:-}"
if [ -z "$WEB_PORT" ] && [ -f "$DOCKER_DIR/.env" ]; then
    WEB_PORT="$(grep -E '^WEB_PORT=' "$DOCKER_DIR/.env" | tail -n1 | cut -d= -f2- | tr -d '"' || true)"
fi
WEB_PORT="${WEB_PORT:-18080}"
BASE_URL="http://${WEB_HOST}:${WEB_PORT}"

APP_CONTAINER="${APP_CONTAINER:-blogloom-app}"
DB_CONTAINER="${DB_CONTAINER:-blogloom-mysql}"

DURATION="${DURATION:-8}"
ENDPOINT_DURATION="${ENDPOINT_DURATION:-5}"
ENDPOINT_CONCURRENCY="${ENDPOINT_CONCURRENCY:-20}"
RAMP_CONCURRENCY="${RAMP_CONCURRENCY:-1 10 25 50 100 200}"
RESOURCE_DURATION="${RESOURCE_DURATION:-20}"   # 峰值负载资源采样时长（秒）
RAMP_TARGET_NAME="${RAMP_TARGET_NAME:-blogs}"
RAMP_TARGET_PATH="${RAMP_TARGET_PATH:-/blogs?pageNum=1&pageSize=10}"
RAMP_TARGET_ACCEPT="${RAMP_TARGET_ACCEPT:-application/json}"

# 前台首页 / 博客相关接口：name|path|accept
ENDPOINTS=(
    "homepage|/|text/html"
    "site|/site|application/json"
    "blogs|/blogs?pageNum=1&pageSize=10|application/json"
    "about|/about|application/json"
    "archives|/archives|application/json"
    "moments|/moments?pageNum=1&pageSize=10|application/json"
    "blog_detail|/blog?id=1|application/json"
    "comments|/comments?page=1|application/json"
)

TS="$(date +%Y%m%d-%H%M%S)"
OUT="$SCRIPT_DIR/results/$TS"
RAW="$OUT/raw"
mkdir -p "$RAW"

log() { printf '[qa] %s\n' "$*"; }
fail() { printf '[qa][ERROR] %s\n' "$*" >&2; exit 1; }

# ---------------------------------------------------------------------------
# 前置校验
# ---------------------------------------------------------------------------
command -v ab >/dev/null 2>&1 || fail "未找到 ab（ApacheBench）"
command -v docker >/dev/null 2>&1 || fail "未找到 docker"
command -v python3 >/dev/null 2>&1 || fail "未找到 python3"
docker inspect "$APP_CONTAINER" >/dev/null 2>&1 || fail "容器 $APP_CONTAINER 未运行，请先执行 docker/scripts/deploy.sh"
docker inspect "$DB_CONTAINER" >/dev/null 2>&1 || fail "容器 $DB_CONTAINER 未运行，请先执行 docker/scripts/deploy.sh"

# 提高文件描述符上限，避免高并发下 ab 报 too many open files
ulimit -n 4096 2>/dev/null || true

log "被测地址：$BASE_URL"
log "输出目录：$OUT"

# ---------------------------------------------------------------------------
# 采集环境与资源配置 -> meta.env
# ---------------------------------------------------------------------------
collect_meta() {
    local meta="$OUT/meta.env"
    : > "$meta"

    {
        echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S %z')"
        echo "host_os=$(uname -s) $(uname -r)"
        echo "host_arch=$(uname -m)"
        echo "host_cpus=$(sysctl -n hw.ncpu 2>/dev/null || nproc 2>/dev/null || echo '?')"
        echo "host_mem_bytes=$(sysctl -n hw.memsize 2>/dev/null || awk '/MemTotal/{print $2*1024}' /proc/meminfo 2>/dev/null || echo '?')"
        echo "docker_version=$(docker version --format '{{.Server.Version}}' 2>/dev/null || echo '?')"
        echo "docker_cpus=$(docker info --format '{{.NCPU}}' 2>/dev/null || echo '?')"
        echo "docker_mem_bytes=$(docker info --format '{{.MemTotal}}' 2>/dev/null || echo '?')"
        echo "web_url=$BASE_URL"
        echo "app_container=$APP_CONTAINER"
        echo "db_container=$DB_CONTAINER"
        echo "app_image=$(docker inspect --format '{{.Config.Image}}' "$APP_CONTAINER")"
        echo "db_image=$(docker inspect --format '{{.Config.Image}}' "$DB_CONTAINER")"
        # 容器资源限制（0 表示未限制）
        echo "app_mem_limit_bytes=$(docker inspect --format '{{.HostConfig.Memory}}' "$APP_CONTAINER")"
        echo "app_cpu_limit_nanos=$(docker inspect --format '{{.HostConfig.NanoCpus}}' "$APP_CONTAINER")"
        echo "db_mem_limit_bytes=$(docker inspect --format '{{.HostConfig.Memory}}' "$DB_CONTAINER")"
        echo "db_cpu_limit_nanos=$(docker inspect --format '{{.HostConfig.NanoCpus}}' "$DB_CONTAINER")"
        # 应用 JVM 参数
        echo "app_java_opts=$(docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "$APP_CONTAINER" | grep '^JAVA_OPTS=' | head -n1 | cut -d= -f2- || echo '')"
        echo "ab_version=$(ab -V 2>/dev/null | head -n1 | tr -d '\n' || echo '?')"
    } >> "$meta"
    log "已采集环境与资源配置"
}

# ---------------------------------------------------------------------------
# 测量服务启动耗时（重启应用容器 -> 首次 /site 返回成功）
# ---------------------------------------------------------------------------
measure_startup() {
    log "重启应用容器并测量启动耗时 ..."
    docker restart "$APP_CONTAINER" >/dev/null
    local start now
    start="$(date +%s)"
    while true; do
        if curl -sf -o /dev/null "$BASE_URL/site"; then
            break
        fi
        now="$(date +%s)"
        if [ $((now - start)) -gt 180 ]; then
            fail "应用启动超时（>180s）"
        fi
        sleep 1
    done
    echo "startup_seconds=$(( $(date +%s) - start ))" >> "$OUT/meta.env"
    log "启动耗时：$(( $(date +%s) - start ))s"
}

# ---------------------------------------------------------------------------
# docker stats 采样
#   sample_stats_once：单次无流采样（用于空闲基线，此时无压测流量）
#   流式采样器    ：单个 docker stats 进程持续输出，开销远低于反复拉起 CLI，
#                   避免采样本身抢占 CPU 而影响 QPS 结果。
# ---------------------------------------------------------------------------
sample_stats_once() {
    docker stats --no-stream --format '{{.Name}},{{.CPUPerc}},{{.MemUsage}},{{.MemPerc}}' \
        "$APP_CONTAINER" "$DB_CONTAINER" 2>/dev/null
}

# 空闲基线：无压测流量时采样 3 次
collect_baseline() {
    local f="$OUT/baseline.csv"
    echo "name,cpu_perc,mem_usage,mem_perc" > "$f"
    log "采集空闲基线（3 次）..."
    for _ in 1 2 3; do
        sample_stats_once >> "$f"
        sleep 2
    done
}

# ---------------------------------------------------------------------------
# 预热：让 JIT、数据库缓存、连接池进入稳态，避免冷启动拉低 QPS。
# ---------------------------------------------------------------------------
warmup() {
    log "预热（JIT / 缓存 / 连接池）..."
    ab -n 500 -c 20 -k -q -H "Accept: application/json" "${BASE_URL}/blogs?pageNum=1&pageSize=10" >/dev/null 2>&1 || true
    ab -n 300 -c 20 -k -q -H "Accept: application/json" "${BASE_URL}/site" >/dev/null 2>&1 || true
    ab -n 300 -c 20 -k -q -H "Accept: text/html" "${BASE_URL}/" >/dev/null 2>&1 || true
    sleep 3
}

# 后台流式采样器：docker stats 直接写入 raw 文件（含 ANSI 控制码，停止后清洗）
start_sampler() {
    local raw="$OUT/samples_raw.txt"
    : > "$raw"
    docker stats --format '{{.Name}},{{.CPUPerc}},{{.MemUsage}},{{.MemPerc}}' \
        "$APP_CONTAINER" "$DB_CONTAINER" > "$raw" 2>/dev/null &
    SAMPLER_PID=$!
    log "资源采样器已启动（pid=${SAMPLER_PID}）"
}

stop_sampler() {
    if [ -n "${SAMPLER_PID:-}" ]; then
        # docker stats 流式进程可能忽略 SIGTERM，这里直接 SIGKILL 避免 wait 阻塞
        kill -9 "$SAMPLER_PID" 2>/dev/null || true
        wait "$SAMPLER_PID" 2>/dev/null || true
        # 清洗 ANSI 控制码，生成规范 CSV（注意把逗号包含进捕获组，避免被替换掉）
        if [ -f "$OUT/samples_raw.txt" ]; then
            {
                echo "name,cpu_perc,mem_usage,mem_perc"
                sed -E "s/.*(${APP_CONTAINER}|${DB_CONTAINER},)/\1/" "$OUT/samples_raw.txt" \
                    | grep -E "^(${APP_CONTAINER}|${DB_CONTAINER})," || true
            } > "$OUT/samples.csv"
        fi
        log "资源采样器已停止"
    fi
}
trap 'stop_sampler' EXIT

# ---------------------------------------------------------------------------
# 运行一次 ab 并解析结果，追加到 TSV
#   $1 输出文件 $2 名称 $3 并发 $4 路径 $5 Accept $6 压测时长(秒)
# ---------------------------------------------------------------------------
run_ab() {
    local out="$1" name="$2" conc="$3" path="$4" accept="$5" duration="$6"
    ab -t "$duration" -c "$conc" -k -q -H "Accept: $accept" "${BASE_URL}${path}" > "$out" 2>&1 || true

    local rps mean p50 p90 p95 p99 complete failed non2xx
    rps="$(awk '/Requests per second/{print $4; exit}' "$out")"
    mean="$(awk '/Time per request:/{print $4; exit}' "$out")"
    p50="$(awk '$1=="50%"{print $2; exit}' "$out")"
    p90="$(awk '$1=="90%"{print $2; exit}' "$out")"
    p95="$(awk '$1=="95%"{print $2; exit}' "$out")"
    p99="$(awk '$1=="99%"{print $2; exit}' "$out")"
    complete="$(awk '/Complete requests:/{print $3; exit}' "$out")"
    failed="$(awk '/Failed requests:/{print $3; exit}' "$out")"
    non2xx="$(awk '/Non-2xx responses:/{print $3; exit}' "$out")"
    printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
        "$name" "$conc" "${rps:-0}" "${mean:-0}" "${p50:-0}" "${p90:-0}" "${p95:-0}" "${p99:-0}" \
        "${complete:-0}" "${failed:-0}" "${non2xx:-0}"
}

# ---------------------------------------------------------------------------
# 主流程
# ---------------------------------------------------------------------------
collect_meta
measure_startup
# 启动后等待稳定
sleep 5
collect_baseline
warmup

# 阶段一：单接口对比（固定并发）。此阶段不启动采样器，避免采样抢占 CPU 影响 QPS 准确性。
log "阶段一：单接口对比压测（并发 ${ENDPOINT_CONCURRENCY}，每个 ${ENDPOINT_DURATION}s）..."
EP_TSV="$RAW/endpoint.tsv"
printf 'name\tconcurrency\trps\tmean_ms\tp50_ms\tp90_ms\tp95_ms\tp99_ms\tcomplete\tfailed\tnon2xx\n' > "$EP_TSV"
for item in "${ENDPOINTS[@]}"; do
    IFS='|' read -r name path accept <<< "$item"
    log "  - ${name} ${path}"
    run_ab "$RAW/ab_endpoint_${name}.txt" "$name" "$ENDPOINT_CONCURRENCY" "$path" "$accept" "$ENDPOINT_DURATION" >> "$EP_TSV"
done

# 阶段二：并发梯度（寻找 QPS 上限）。同样不启动采样器。
log "阶段二：并发梯度压测（目标 ${RAMP_TARGET_NAME} ${RAMP_TARGET_PATH}，每档 ${DURATION}s）..."
RAMP_TSV="$RAW/ramp.tsv"
printf 'name\tconcurrency\trps\tmean_ms\tp50_ms\tp90_ms\tp95_ms\tp99_ms\tcomplete\tfailed\tnon2xx\n' > "$RAMP_TSV"
for conc in $RAMP_CONCURRENCY; do
    log "  - 并发 ${conc}"
    run_ab "$RAW/ab_ramp_c${conc}.txt" "$RAMP_TARGET_NAME" "$conc" "$RAMP_TARGET_PATH" "$RAMP_TARGET_ACCEPT" "$DURATION" >> "$RAMP_TSV"
done

# 取并发梯度中 QPS 最高的档位作为峰值负载
PEAK_CONC="$(awk 'NR>1 && $3+0>max {max=$3+0; c=$2} END{print c}' "$RAMP_TSV")"
[ -n "$PEAK_CONC" ] || PEAK_CONC="$ENDPOINT_CONCURRENCY"
echo "peak_concurrency=$PEAK_CONC" >> "$OUT/meta.env"

# 阶段三：在峰值并发下采样资源占用（采样器仅在此时开启）
log "阶段三：峰值负载资源采样（并发 ${PEAK_CONC}，${RESOURCE_DURATION}s）..."
start_sampler
ab -t "$RESOURCE_DURATION" -c "$PEAK_CONC" -k -q -H "Accept: $RAMP_TARGET_ACCEPT" \
    "${BASE_URL}${RAMP_TARGET_PATH}" > "$RAW/ab_resource.txt" 2>&1 || true
stop_sampler

# ---------------------------------------------------------------------------
# 生成报告
# ---------------------------------------------------------------------------
log "生成报告 ..."
python3 "$SCRIPT_DIR/lib/report.py" "$OUT" > "$OUT/report.md"

log "完成。报告：$OUT/report.md"
printf '\n================ 报告摘要 ================\n'
sed -n '1,80p' "$OUT/report.md"
