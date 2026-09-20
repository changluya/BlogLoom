#!/usr/bin/env bash
#
# =============================================================================
# 一键推送镜像到 Docker Hub（push.sh）
# =============================================================================
# 用途：
#   将本地构建的 BlogLoom 镜像打上 Docker Hub 仓库标签并推送。
#   - 镜像 tag 取自 .env 的 IMAGE_TAG（或命令行参数）；
#   - 仓库用户名取自 .env 的 DOCKERHUB_USER（默认 codercl）。
#   MySQL 使用官方镜像，无需推送。
#
# 用法（在 docker/ 目录执行）：
#   ./scripts/push.sh                 # 推送 <user>/blogloom:<IMAGE_TAG> 与 :latest
#   ./scripts/push.sh 1.0.1           # 指定 tag
#   ./scripts/push.sh --no-latest     # 只推送指定 tag，不推送 latest
#
# 前置条件：
#   1) 已登录 Docker Hub：docker login -u <DOCKERHUB_USER>
#   2) 已构建本地镜像：  ./scripts/package.sh <版本号>
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
. "$SCRIPT_DIR/common.sh"

require_docker
ensure_env
load_env

VERSION=""
PUSH_LATEST=true
for arg in "$@"; do
    case "$arg" in
        --no-latest) PUSH_LATEST=false ;;
        *) VERSION="$arg" ;;
    esac
done
[ -n "$VERSION" ] || VERSION="${IMAGE_TAG:-1.0.0}"

HUB_USER="${DOCKERHUB_USER:-codercl}"
REPO="${HUB_USER}/blogloom"
LOCAL_IMAGE="blogloom:${VERSION}"

# 本地镜像必须存在（由 package.sh 构建）
docker image inspect "$LOCAL_IMAGE" >/dev/null 2>&1 \
    || { printf '[ERROR] 本地不存在镜像 %s，请先执行 ./scripts/package.sh %s\n' "$LOCAL_IMAGE" "$VERSION" >&2; exit 1; }

# 检查是否已登录 Docker Hub（未登录则给出提示）
if ! docker system info 2>/dev/null | grep -qi "username"; then
    printf '[warn] 未检测到 Docker Hub 登录状态；若推送失败请先执行：docker login -u %s\n' "$HUB_USER"
fi

printf '[push] %s -> %s:%s\n' "$LOCAL_IMAGE" "$REPO" "$VERSION"
docker tag "$LOCAL_IMAGE" "${REPO}:${VERSION}"
docker push "${REPO}:${VERSION}"

if [ "$PUSH_LATEST" = true ]; then
    printf '[push] %s -> %s:latest\n' "$LOCAL_IMAGE" "$REPO"
    docker tag "$LOCAL_IMAGE" "${REPO}:latest"
    docker push "${REPO}:latest"
fi

printf '[done] 推送完成：\n'
printf '       %s:%s\n' "$REPO" "$VERSION"
[ "$PUSH_LATEST" = true ] && printf '       %s:latest\n' "$REPO"
printf '[next] 部署端可用：IMAGE_REPO=%s ./scripts/deploy.sh\n' "$REPO"
