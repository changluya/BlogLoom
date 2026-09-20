#!/usr/bin/env bash
#
# =============================================================================
# 一键升级（upgrade.sh）
# =============================================================================
# 用途：
#   在服务器升级到新版本：加载新镜像 → 重建应用容器 → 容器启动时按历史记录
#   执行未执行过的增量 SQL，实现无缝增量升级。
#
# 用法（在 docker/ 目录执行）：
#   ./scripts/upgrade.sh                                  # 使用 .env 的 IMAGE_TAG，从 dist/ 加载镜像
#   ./scripts/upgrade.sh dist/blogloom-1.0.1.tar
#   ./scripts/upgrade.sh dist/blogloom-1.0.1.tar --dry-run   # 仅预览待执行 SQL，不改库、不重启
#
# 升级依据：
#   sql/increment/ 下的增量 SQL 与 data/sql-local/ 中的历史执行记录；
#   已执行过的 SQL 不重复执行；数据库已有数据时自动跳过全量初始化脚本。
#
# 核心流程：
#   1. 准备 .env 与目录，加载新镜像；
#   2. 确保 MySQL 运行；
#   3. dry-run 时仅预览待执行 SQL 并退出；
#   4. 正式升级：重建应用容器，容器入口 start.sh 执行增量 SQL 后启动服务。
# =============================================================================

set -euo pipefail

# 引入公共库
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
. "$SCRIPT_DIR/common.sh"

# 前置校验：docker 与 docker compose v2 可用
require_docker

# ---------------------------------------------------------------------------
# 步骤 1：准备 .env 与持久化目录
# ---------------------------------------------------------------------------
ensure_env
load_env
ensure_dirs

# ---------------------------------------------------------------------------
# 步骤 2：解析参数
#   --dry-run  仅预览待执行 SQL
#   其他参数   视为镜像 tar 路径
# ---------------------------------------------------------------------------
TAR_PATH=""
DRY_RUN=""
for arg in "$@"; do
    case "$arg" in
        --dry-run) DRY_RUN="--dry-run" ;;
        *) TAR_PATH="$arg" ;;
    esac
done
[ -n "$TAR_PATH" ] || TAR_PATH="$DIST_DIR/blogloom-${IMAGE_TAG}.tar"

# ---------------------------------------------------------------------------
# 步骤 3：加载新版本镜像
#   优先使用指定/默认的 tar；否则使用本地已有镜像。
# ---------------------------------------------------------------------------
IMAGE_REPO="${IMAGE_REPO:-blogloom}"
FULL_IMAGE="${IMAGE_REPO}:${IMAGE_TAG}"
if [ -f "$TAR_PATH" ]; then
    load_image_tar "$TAR_PATH"
elif docker image inspect "$FULL_IMAGE" >/dev/null 2>&1; then
    printf '[image] 使用本地镜像 %s\n' "$FULL_IMAGE"
elif printf '%s' "$IMAGE_REPO" | grep -q '/'; then
    printf '[image] 本地无 %s，将由 docker compose 从仓库拉取\n' "$FULL_IMAGE"
else
    printf '[ERROR] 本地不存在镜像 %s，请先执行 ./scripts/package.sh 或指定镜像包\n' "$FULL_IMAGE" >&2
    exit 1
fi

# ---------------------------------------------------------------------------
# 步骤 4：确保 MySQL 运行（等待健康检查通过）
# ---------------------------------------------------------------------------
printf '[upgrade] 确保 MySQL 运行 ...\n'
compose up -d --wait mysql

# ---------------------------------------------------------------------------
# 步骤 5：dry-run 模式
#   复用应用镜像内的 bin/upgrade-sql.sh（mysql 客户端已内置），
#   以一次性容器运行，仅比对 sql/increment 与 data/sql-local 并打印待执行清单，
#   不修改数据库，也不重启应用。
# ---------------------------------------------------------------------------
if [ -n "$DRY_RUN" ]; then
    printf '[upgrade] 预览待执行增量 SQL ...\n'
    compose run --rm -T --no-deps --entrypoint /opt/blogloom/bin/upgrade-sql.sh blogloom --dry-run
    printf '[done] dry-run 完成，未修改数据库，也未重启应用。\n'
    exit 0
fi

# ---------------------------------------------------------------------------
# 步骤 6：正式升级
#   使用新镜像强制重建应用容器；容器入口 bin/start.sh 会：
#     等待 MySQL → 执行未记录的增量 SQL → 启动 java。
#   仅新增/更新 sql/increment 下的 SQL 时，也可直接 docker compose restart blogloom。
# ---------------------------------------------------------------------------
printf '[upgrade] 使用镜像 blogloom:%s 重启应用（启动时自动执行增量 SQL）...\n' "$IMAGE_TAG"
compose up -d --force-recreate blogloom

printf '[done] 升级完成。查看日志：docker logs -f blogloom-app\n'
