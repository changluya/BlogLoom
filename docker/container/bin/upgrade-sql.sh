#!/usr/bin/env bash
#
# =============================================================================
# BlogLoom 容器内增量 SQL 执行脚本（upgrade-sql.sh）
# =============================================================================
# 位置：容器内 /opt/blogloom/bin/upgrade-sql.sh，由 bin/start.sh 在启动 Java 前调用。
# 规则来源：与仓库 bin/local/upgrate-sql.sh 保持一致。
#
# 输入：
#   待执行文件：${APP_HOME}/sql/increment/<版本>/**/*.sql（排除 init.sql）
#   执行记录  ：${APP_HOME}/sql/local/<SQL文件名>（扁平结构，宿主机 data/sql-local 持久化）
#
# 数据库连接（由 docker-compose 注入环境变量）：
#   DB_HOST / DB_PORT / DB_USER / DB_PASSWORD / DB_NAME
#
# 核心特性：
#   1. 按文件名（时间戳前缀）排序执行，保证迁移顺序；
#   2. 已执行过的 SQL 跳过，未执行的成功后才写入记录 → 可重复执行（幂等）；
#   3. 存量库保护：目标库已有数据时，自动跳过全量初始化脚本（含 CREATE DATABASE），
#      避免在已有数据的库上误执行 DROP TABLE。
#
# 核心流程：
#   参数解析 → 环境校验 → 扫描待执行 SQL → 重名检测 → 存量库判断
#   → 过滤出未执行清单 → （dry-run 仅预览）→ 逐个执行并记录 → 输出结果
#
# 用法：upgrade-sql.sh [--dry-run]
#   --dry-run   只列出待执行 SQL，不连接或修改数据库
# =============================================================================

set -uo pipefail

# ---------------------------------------------------------------------------
# 配置区：目录与数据库连接参数（均可用环境变量覆盖）
# ---------------------------------------------------------------------------
APP_HOME="${APP_HOME:-/opt/blogloom}"
PENDING_SQL_DIR="${PENDING_SQL_DIR:-$APP_HOME/sql/increment}"   # 待执行增量 SQL 目录
EXECUTED_SQL_DIR="${EXECUTED_SQL_DIR:-$APP_HOME/sql/local}"    # 已执行记录目录
WORK_DIR="$EXECUTED_SQL_DIR/.work"                              # 执行期临时工作目录

DB_HOST="${DB_HOST:-mysql}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-blogloom}"
DB_PASSWORD="${DB_PASSWORD:-}"
DB_NAME="${DB_NAME:-blogloom}"

# ---------------------------------------------------------------------------
# 日志输出：带颜色的 info/success/warn/fail
# ---------------------------------------------------------------------------
BLUE='\033[0;34m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
DRY_RUN=false

info() { printf "${BLUE}[INFO]${NC} %s\n" "$1"; }
success() { printf "${GREEN}[SUCCESS]${NC} %s\n" "$1"; }
warn() { printf "${YELLOW}[WARN]${NC} %s\n" "$1"; }
fail() { printf "${RED}[ERROR]${NC} %s\n" "$1" >&2; exit 1; }

# ---------------------------------------------------------------------------
# 步骤 1：解析参数（--dry-run / -h）
# ---------------------------------------------------------------------------
while [ "$#" -gt 0 ]; do
    case "$1" in
        --dry-run) DRY_RUN=true ;;
        -h|--help)
            cat <<EOF
用法：$0 [--dry-run]

  --dry-run    只列出待执行 SQL，不连接或修改数据库
EOF
            exit 0
            ;;
        *) fail "未知参数：$1" ;;
    esac
    shift
done

# ---------------------------------------------------------------------------
# 步骤 2：环境校验
#   - 数据库名只允许字母/数字/下划线（用于拼接 information_schema 查询）；
#   - 待执行目录必须存在；
#   - 预建记录目录与临时工作目录。
# ---------------------------------------------------------------------------
[[ "$DB_NAME" =~ ^[A-Za-z0-9_]+$ ]] || fail "数据库名称仅允许字母、数字和下划线：$DB_NAME"
[ -d "$PENDING_SQL_DIR" ] || fail "待升级 SQL 目录不存在：$PENDING_SQL_DIR"

mkdir -p "$EXECUTED_SQL_DIR" "$WORK_DIR"

# ---------------------------------------------------------------------------
# is_full_init <file>：判断是否为全量初始化脚本（特征：包含 CREATE DATABASE）。
# 用于存量库保护：已有数据的库跳过此类脚本，避免误 DROP TABLE。
# ---------------------------------------------------------------------------
is_full_init() {
    # 全量初始化脚本特征：包含 CREATE DATABASE。
    grep -Eiq 'CREATE[[:space:]]+DATABASE' "$1"
}

# ---------------------------------------------------------------------------
# 步骤 3：扫描待执行 SQL
#   递归查找 sql/increment 下所有 *.sql（排除 init.sql），按路径排序。
#   文件名以时间戳前缀命名，排序即等于迁移顺序。
# ---------------------------------------------------------------------------
PENDING_FILES=()
while IFS= read -r sql_file; do
    [ -n "$sql_file" ] && PENDING_FILES+=("$sql_file")
done < <(find "$PENDING_SQL_DIR" -type f -name '*.sql' ! -name 'init.sql' | sort)

# ---------------------------------------------------------------------------
# 步骤 4：重名检测
#   记录目录为扁平结构（只按文件名去重），因此不同版本下不允许出现同名 SQL。
# ---------------------------------------------------------------------------
DUPLICATE_FILENAMES="$(find "$PENDING_SQL_DIR" -type f -name '*.sql' ! -name 'init.sql' -exec basename {} \; | sort | uniq -d)"
if [ -n "$DUPLICATE_FILENAMES" ]; then
    printf '%s\n' "$DUPLICATE_FILENAMES" >&2
    fail "发现重名 SQL；sql/local 使用扁平记录目录，请先将上述文件改为唯一名称"
fi

# ---------------------------------------------------------------------------
# 步骤 5：判断目标库是否已有数据（存量库判断）
#   db_has_tables：DB_TABLE_COUNT > 0 即视为存量库。
#   仅在非 dry-run 时连接数据库统计 information_schema 中的表数量。
# ---------------------------------------------------------------------------
DB_TABLE_COUNT=""
db_has_tables() {
    [ -n "$DB_TABLE_COUNT" ] && [ "$DB_TABLE_COUNT" -gt 0 ] 2>/dev/null
}

if [ "$DRY_RUN" = false ]; then
    command -v mysql >/dev/null 2>&1 || fail "未找到 mysql 客户端"
    DB_TABLE_COUNT="$(MYSQL_PWD="$DB_PASSWORD" mysql \
        --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USER" \
        --batch --skip-column-names \
        --execute="SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '$DB_NAME'" 2>&1)" \
        || fail "无法连接 MySQL 或检查数据库：$DB_TABLE_COUNT"
fi

# ---------------------------------------------------------------------------
# 步骤 6：过滤出真正需要执行的 SQL
#   规则：
#     - 记录目录已存在同名文件        → 已执行，跳过；
#     - 未记录 + 全量初始化 + 存量库  → 记录并跳过（存量库保护）；
#     - 其余                          → 加入待执行列表。
# ---------------------------------------------------------------------------
UPGRADE_FILES=()
UPGRADE_COUNT=0
for sql_file in "${PENDING_FILES[@]}"; do
    [ -n "$sql_file" ] || continue
    relative_path="${sql_file#$PENDING_SQL_DIR/}"
    filename="$(basename "$sql_file")"
    if [ -f "$EXECUTED_SQL_DIR/$filename" ]; then
        info "跳过已执行 SQL：$relative_path"
        continue
    fi
    if [ "$DRY_RUN" = false ] && db_has_tables && is_full_init "$sql_file"; then
        warn "数据库已存在数据，跳过多余的全量初始化脚本：$relative_path"
        cp -f "$sql_file" "$EXECUTED_SQL_DIR/$filename"
        continue
    fi
    UPGRADE_FILES+=("$sql_file")
    UPGRADE_COUNT=$((UPGRADE_COUNT + 1))
done

# ---------------------------------------------------------------------------
# 步骤 7：无待执行则直接结束（数据库已是最新版本）
# ---------------------------------------------------------------------------
if [ "$UPGRADE_COUNT" -eq 0 ]; then
    success "没有需要执行的增量 SQL，数据库已是最新版本"
    exit 0
fi

# 打印待执行清单
printf "待执行 SQL（共 %s 个）：\n" "$UPGRADE_COUNT"
for sql_file in "${UPGRADE_FILES[@]}"; do
    printf "  - %s\n" "${sql_file#$PENDING_SQL_DIR/}"
done

# dry-run：仅预览，不连接数据库、不改库、不写记录
if [ "$DRY_RUN" = true ]; then
    warn "dry-run 模式：未连接数据库，也未归档 SQL 文件"
    exit 0
fi

info "数据库：$DB_USER@$DB_HOST:$DB_PORT/$DB_NAME"
warn "MySQL DDL 可能隐式提交；执行前请确认数据库已有可用备份"

# ---------------------------------------------------------------------------
# 步骤 8：逐个执行并记录
#   每个 SQL：
#     1) 复制到工作目录执行（保留源文件不被修改）；
#     2) mysql --database=<db> 执行，输出写入日志；
#     3) 成功 → 复制一份到记录目录（记录已执行），清理工作文件与日志；
#     4) 失败 → 打印日志前 200 行并终止（工作文件与日志保留，便于排查）。
# ---------------------------------------------------------------------------
SUCCESS_COUNT=0
for sql_file in "${UPGRADE_FILES[@]}"; do
    relative_path="${sql_file#$PENDING_SQL_DIR/}"
    filename="$(basename "$relative_path")"
    work_sql="$WORK_DIR/$filename"
    log_file="$WORK_DIR/$filename.log"

    info "执行：$relative_path"
    cp -f "$sql_file" "$work_sql"
    if MYSQL_PWD="$DB_PASSWORD" mysql \
        --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USER" \
        --database="$DB_NAME" --default-character-set=utf8mb4 --show-warnings \
        < "$work_sql" > "$log_file" 2>&1; then
        cp -f "$sql_file" "$EXECUTED_SQL_DIR/$filename"
        rm -f "$work_sql" "$log_file"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        success "执行并记录：$relative_path"
    else
        [ ! -s "$log_file" ] || sed -n '1,200p' "$log_file" >&2
        fail "SQL 执行失败：$relative_path；工作文件和日志保留在 $WORK_DIR"
    fi
done

# ---------------------------------------------------------------------------
# 步骤 9：输出结果
# ---------------------------------------------------------------------------
success "数据库升级完成，成功执行 $SUCCESS_COUNT 个 SQL；记录目录：$EXECUTED_SQL_DIR"
