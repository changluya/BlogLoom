#!/usr/bin/env bash
#
# =============================================================================
# 一键推送镜像到 Docker Hub（push.sh）
# =============================================================================
# 用途：
#   构建并推送 BlogLoom 一体化镜像到 Docker Hub。
#   默认使用 docker buildx 构建 **多架构镜像**（linux/amd64 + linux/arm64），
#   一次性生成多平台 manifest；部署端 docker pull / docker compose pull 时，
#   会按宿主架构自动选择对应镜像层，无需手工指定 platform。
#   MySQL 使用官方镜像，无需推送。
#
# 用法（在 docker/ 目录执行）：
#   ./scripts/push.sh                       # 多架构构建并推送 :<IMAGE_TAG> 与 :latest
#   ./scripts/push.sh 1.0.1                 # 指定 tag
#   ./scripts/push.sh --platform linux/amd64,linux/arm64
#   ./scripts/push.sh --no-latest           # 只推送指定 tag，不推送 latest
#   ./scripts/push.sh --prune               # 构建前清理 buildx 缓存，释放磁盘空间
#   ./scripts/push.sh --single              # 使用本地已构建镜像推送（旧行为，单架构）
#
# 前置条件：
#   1) 已登录 Docker Hub：docker login -u <DOCKERHUB_USER>
#   2) 多架构模式需要 docker buildx（Docker Desktop 自带；Linux 装 docker-buildx-plugin）
#   3) --single 模式需要本地已构建镜像：./scripts/package.sh <版本号>
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
. "$SCRIPT_DIR/common.sh"

require_docker
ensure_env
load_env

PLATFORMS="linux/amd64,linux/arm64"
SINGLE=false
PUSH_LATEST=true
PRUNE=false
VERSION=""
while [ $# -gt 0 ]; do
    case "$1" in
        --platform)   PLATFORMS="${2:-}"; shift 2 ;;
        --platform=*) PLATFORMS="${1#*=}"; shift ;;
        --single)     SINGLE=true; shift ;;
        --no-latest)  PUSH_LATEST=false; shift ;;
        --prune)      PRUNE=true; shift ;;
        *)            VERSION="$1"; shift ;;
    esac
done
[ -n "$VERSION" ] || VERSION="${IMAGE_TAG:-1.0.0}"
[ -n "$PLATFORMS" ] || { echo "[ERROR] --platform 不能为空" >&2; exit 1; }

HUB_USER="${DOCKERHUB_USER:-codercl}"
REPO="${HUB_USER}/blogloom"

# 检查是否已登录 Docker Hub（未登录则给出提示）
if ! docker system info 2>/dev/null | grep -qi "username"; then
    printf '[warn] 未检测到 Docker Hub 登录状态；若推送失败请先执行：docker login -u %s\n' "$HUB_USER"
fi

if [ "$SINGLE" = true ]; then
    # -------------------------------------------------------------------------
    # 单架构模式：为本地已构建镜像打标签并推送（镜像由 package.sh 生成）。
    # -------------------------------------------------------------------------
    LOCAL_IMAGE="blogloom:${VERSION}"
    docker image inspect "$LOCAL_IMAGE" >/dev/null 2>&1 \
        || { printf '[ERROR] 本地不存在镜像 %s，请先执行 ./scripts/package.sh %s\n' "$LOCAL_IMAGE" "$VERSION" >&2; exit 1; }

    printf '[push] %s -> %s:%s（单架构）\n' "$LOCAL_IMAGE" "$REPO" "$VERSION"
    docker tag "$LOCAL_IMAGE" "${REPO}:${VERSION}"
    docker push "${REPO}:${VERSION}"

    if [ "$PUSH_LATEST" = true ]; then
        printf '[push] %s -> %s:latest\n' "$LOCAL_IMAGE" "$REPO"
        docker tag "$LOCAL_IMAGE" "${REPO}:latest"
        docker push "${REPO}:latest"
    fi
else
    # -------------------------------------------------------------------------
    # 多架构模式：buildx 构建并直接推送。
    #   多平台镜像无法 docker load 到本地，因此必须 --push（或 --output oci）。
    #   --provenance=false 只生成各架构镜像清单，避免 attestation 干扰平台匹配。
    # -------------------------------------------------------------------------
    ensure_buildx_builder
    # 构建前检查 Docker 磁盘：多架构构建会缓存两套架构的中间层，容易撑满磁盘
    if [ "$PRUNE" = true ]; then
        prune_buildx_cache "$BUILDX_BUILDER"
    else
        warn_docker_disk 8192
    fi

    printf '[push] 多架构构建并推送 %s:%s（平台：%s）\n' "$REPO" "$VERSION" "$PLATFORMS"
    TAGS=(-t "${REPO}:${VERSION}")
    if [ "$PUSH_LATEST" = true ]; then
        TAGS+=(-t "${REPO}:latest")
    fi
    if ! docker buildx build \
        --builder "$BUILDX_BUILDER" \
        --platform "$PLATFORMS" \
        --provenance=false \
        -f "$DOCKERFILE" \
        "${TAGS[@]}" \
        --push \
        "$REPO_ROOT"; then
        print_docker_disk_help "$BUILDX_BUILDER"
        exit 1
    fi
fi

printf '[done] 推送完成：\n'
printf '       %s:%s\n' "$REPO" "$VERSION"
if [ "$PUSH_LATEST" = true ]; then
    printf '       %s:latest\n' "$REPO"
fi
printf '[next] 部署端可用：IMAGE_REPO=%s ./scripts/deploy.sh\n' "$REPO"
