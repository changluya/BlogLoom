#!/usr/bin/env bash
#
# =============================================================================
# BlogLoom 容器启动入口（start.sh）
# =============================================================================
# 位置：容器内 /opt/blogloom/bin/start.sh，也是镜像的默认 ENTRYPOINT。
#
# 用途：
#   作为应用容器的唯一入口，在启动后端前完成数据库增量升级，实现「重启即升级」。
#   增量规则与本地 bin/local/upgrate-sql.sh 一致，基于 sql/local 历史记录
#   只执行未执行过的 SQL（首次启动即全量初始化）。
#
# 核心流程：
#   1. 读取数据库连接参数（由 docker-compose 注入的环境变量）；
#   2. 轮询等待 MySQL 可用（最多 60 次 × 2s）；
#   3. 调用 bin/upgrade-sql.sh 执行增量 SQL；
#   4. 以 exec 方式启动后端 jar（成为 PID 1，便于容器正确接收停止信号）。
#
# 关键环境变量（由 docker-compose 提供）：
#   DB_HOST / DB_PORT / DB_USER / DB_PASSWORD / DB_NAME   数据库连接
#   JAVA_OPTS                                              JVM 参数
#   APP_HOME                                               应用根目录（默认 /opt/blogloom）
# =============================================================================

set -euo pipefail

# 应用根目录：conf/、lib/、bin/、sql/ 均相对该目录。
APP_HOME="${APP_HOME:-/opt/blogloom}"
cd "$APP_HOME"

# 数据库连接参数：默认值仅用于兜底，实际以 compose 注入为准。
DB_HOST="${DB_HOST:-mysql}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-blogloom}"
DB_PASSWORD="${DB_PASSWORD:-}"
DB_NAME="${DB_NAME:-blogloom}"

info() { printf '[start] %s\n' "$1"; }
fail() { printf '[start][ERROR] %s\n' "$1" >&2; exit 1; }

# 增量脚本依赖 mysql 客户端；镜像基于 mysql:8.0，已内置。
command -v mysql >/dev/null 2>&1 || fail "未找到 mysql 客户端"

# ---------------------------------------------------------------------------
# 步骤 1：等待 MySQL 就绪
#   通过 mysql 客户端执行 SELECT 1 探测（最多 60 次，每次间隔 2s）。
#   虽然 compose 已配置 depends_on: service_healthy，这里再做一次兜底，
#   保证直接 docker run 或重启场景下也能等到数据库可用。
# ---------------------------------------------------------------------------
info "等待 MySQL ${DB_HOST}:${DB_PORT} ..."
READY="false"
for _ in $(seq 1 60); do
    if MYSQL_PWD="$DB_PASSWORD" mysql \
        --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USER" \
        --connect-timeout=3 --batch --skip-column-names \
        --execute="SELECT 1" >/dev/null 2>&1; then
        READY="true"
        break
    fi
    sleep 2
done
[ "$READY" = "true" ] || fail "MySQL 未在预期时间内就绪"

# ---------------------------------------------------------------------------
# 步骤 2：执行增量 SQL
#   upgrade-sql.sh 会扫描 sql/increment、比对 sql/local 记录并执行未执行过的 SQL；
#   首次启动时即完成全量初始化，后续重启只执行新增的增量 SQL（幂等）。
# ---------------------------------------------------------------------------
info "检查并执行增量 SQL ..."
"$APP_HOME/bin/upgrade-sql.sh"

# ---------------------------------------------------------------------------
# 步骤 3：启动后端服务
#   使用 exec 让 java 接管 PID 1，docker stop 时信号能正确传递。
# ---------------------------------------------------------------------------
info "启动 BlogLoom 服务 ..."
exec java ${JAVA_OPTS:-} -jar "$APP_HOME/lib/blog-backend.jar"
