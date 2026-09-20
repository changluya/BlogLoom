#!/usr/bin/env bash
#
# =============================================================================
# 一键部署（deploy.sh）
# =============================================================================
# 用途：
#   在服务器首次部署（或重新部署）BlogLoom：加载镜像并启动「应用 + MySQL」两个容器。
#   首次启动时，应用容器入口会自动等待 MySQL 并执行增量 SQL（即全量初始化）。
#
# 用法（在 docker/ 目录执行）：
#   ./scripts/deploy.sh                          # 使用 .env 的 IMAGE_TAG，必要时从 dist/ 加载镜像
#   ./scripts/deploy.sh dist/blogloom-1.0.0.tar  # 指定镜像包
#
# 核心流程：
#   1. 生成/加载 .env（含随机密码与密钥）；
#   2. 创建持久化目录（data/、conf/logs、conf/upload）；
#   3. 加载镜像（若提供/存在镜像 tar）；
#   4. docker compose up -d 启动 mysql 与 blogloom；
#   5. 应用容器启动脚本执行增量 SQL 后启动后端。
# =============================================================================

set -euo pipefail

# 引入公共库
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
. "$SCRIPT_DIR/common.sh"

# 前置校验：docker 与 docker compose v2 可用
require_docker

# 允许通过环境变量临时覆盖 .env 中的镜像仓库/版本（便于直接使用 Docker Hub 镜像）
#   例：IMAGE_REPO=codercl/blogloom ./scripts/deploy.sh
OVERRIDE_IMAGE_REPO="${IMAGE_REPO:-}"
OVERRIDE_IMAGE_TAG="${IMAGE_TAG:-}"

# ---------------------------------------------------------------------------
# 步骤 1：准备 .env 并加载环境变量
#   ensure_env：.env 不存在则由 .env.example 生成，并写入随机 MySQL 密码与 TOKEN_SECRET；
#   load_env  ：导出 .env 变量（IMAGE_TAG、WEB_PORT、MYSQL_PORT 等）。
# ---------------------------------------------------------------------------
ensure_env
load_env
# 命令行/环境变量优先级高于 .env（docker compose 同样以 shell 环境变量优先）
if [ -n "$OVERRIDE_IMAGE_REPO" ]; then export IMAGE_REPO="$OVERRIDE_IMAGE_REPO"; fi
if [ -n "$OVERRIDE_IMAGE_TAG" ]; then export IMAGE_TAG="$OVERRIDE_IMAGE_TAG"; fi

# ---------------------------------------------------------------------------
# 步骤 2：创建持久化目录（仓库根目录下）
#   data/mysql、data/sql-local、conf/logs、conf/upload、docker/dist
# ---------------------------------------------------------------------------
ensure_dirs

# ---------------------------------------------------------------------------
# 步骤 3：加载镜像
#   优先使用命令行指定的 tar；未指定时取 dist/blogloom-<IMAGE_TAG>.tar；
#   若 tar 不存在，则尝试使用本地已存在的同名镜像。
#   若镜像仓库为 Docker Hub（IMAGE_REPO 含 "/"），则允许 compose 自动 pull。
# ---------------------------------------------------------------------------
IMAGE_REPO="${IMAGE_REPO:-blogloom}"
FULL_IMAGE="${IMAGE_REPO}:${IMAGE_TAG}"
TAR_PATH="${1:-$DIST_DIR/blogloom-${IMAGE_TAG}.tar}"
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
# 步骤 4：启动服务
#   docker compose up -d：
#     - mysql 先启动，healthcheck 通过后；
#     - blogloom 启动（depends_on: service_healthy）；
#     - 应用容器入口 bin/start.sh 会：等待 MySQL → 执行增量 SQL → 启动 java。
#   首次即完成数据库全量初始化。
# ---------------------------------------------------------------------------
printf '[deploy] 启动服务（Web 端口 %s，MySQL 端口 %s）\n' "$WEB_PORT" "$MYSQL_PORT"
printf '        应用启动时会自动等待 MySQL 并执行增量 SQL（首次即全量初始化）。\n'
compose up -d

# ---------------------------------------------------------------------------
# 步骤 5：输出访问信息
# ---------------------------------------------------------------------------
printf '[done] 部署完成，请稍候查看日志：\n'
printf '        docker logs -f blogloom-app\n'
printf '[info] 博客前台：http://<服务器IP>:%s\n' "$WEB_PORT"
printf '[info] 管理后台：http://<服务器IP>:%s/cms\n' "$WEB_PORT"
printf '[info] 默认账号：admin / 123456（请登录后立即修改）\n'
