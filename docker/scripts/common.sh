#!/usr/bin/env bash
#
# =============================================================================
# BlogLoom Docker 脚本公共库（被 package.sh / deploy.sh / upgrade.sh 通过 source 引入）
# =============================================================================
# 作用：
#   1. 统一解析脚本自身、docker 目录、仓库根目录等关键路径；
#   2. 提供 .env 生成 / 读取、目录准备、docker compose 封装等公共能力；
#   3. 保证三个一键脚本使用同一套路径与配置，避免各自拼接出现偏差。
#
# 目录约定：
#   scripts/  → 本文件所在目录（docker/scripts）
#   docker/   → 部署定义目录（Dockerfile、docker-compose.yml、.env、dist）
#   仓库根     → 挂载的宿主目录（conf/、sql/、data/）所在位置
# =============================================================================

set -euo pipefail

# ---------- 关键路径解析 ----------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"   # docker/scripts
DOCKER_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"                    # docker/
REPO_ROOT="$(cd "$DOCKER_DIR/.." && pwd)"                     # 仓库根目录
ENV_FILE="$DOCKER_DIR/.env"                                   # 部署配置（含密码/密钥，已 gitignore）
ENV_EXAMPLE="$DOCKER_DIR/.env.example"                        # 配置模板
DIST_DIR="$DOCKER_DIR/dist"                                   # 打包产物目录
COMPOSE_FILE="$DOCKER_DIR/docker-compose.yml"                 # compose 文件
DOCKERFILE="$DOCKER_DIR/Dockerfile"                           # 镜像构建文件

# -----------------------------------------------------------------------------
# random_hex：生成 24 字节随机十六进制串（48 位），用于初始密码与令牌密钥。
# 优先使用 openssl，缺失时回退到 /dev/urandom。
# -----------------------------------------------------------------------------
random_hex() {
    openssl rand -hex 24 2>/dev/null || od -An -tx1 -N24 /dev/urandom | tr -d ' \n'
}

# -----------------------------------------------------------------------------
# read_env_value <key> [default]：读取 .env 中某个键的值，不存在则返回默认值。
# 用于 package.sh 在未显式传参时取 IMAGE_TAG。
# -----------------------------------------------------------------------------
read_env_value() {
    local key="$1" default="${2:-}"
    if [ -f "$ENV_FILE" ]; then
        local line
        line="$(grep -E "^${key}=" "$ENV_FILE" | tail -n1 || true)"
        if [ -n "$line" ]; then
            printf '%s' "${line#*=}"
            return
        fi
    fi
    printf '%s' "$default"
}

# -----------------------------------------------------------------------------
# replace_env_value <key> <value>：就地替换 .env 中某个键的值（跨 macOS/GNU sed）。
# -----------------------------------------------------------------------------
replace_env_value() {
    local key="$1" value="$2"
    sed -i.bak -E "s|^${key}=.*|${key}=${value}|" "$ENV_FILE" && rm -f "$ENV_FILE.bak"
}

# -----------------------------------------------------------------------------
# ensure_env：确保 .env 存在。
# 核心步骤：
#   1) 若 .env 不存在，从 .env.example 复制一份；
#   2) 为 MYSQL_ROOT_PASSWORD / MYSQL_PASSWORD / TOKEN_SECRET 写入随机值；
#   3) 提示用户按需修改 BLOG_API / BLOG_CMS / BLOG_VIEW 等对外地址。
# -----------------------------------------------------------------------------
ensure_env() {
    if [ ! -f "$ENV_FILE" ]; then
        cp "$ENV_EXAMPLE" "$ENV_FILE"
        replace_env_value MYSQL_ROOT_PASSWORD "$(random_hex)"
        replace_env_value MYSQL_PASSWORD "$(random_hex)"
        replace_env_value TOKEN_SECRET "$(random_hex)"
        printf '[init] 已生成 %s（随机密码/密钥）。\n' "$ENV_FILE"
        printf '[init] 请按需修改 BLOG_API / BLOG_CMS / BLOG_VIEW 为真实访问地址。\n'
    fi
}

# -----------------------------------------------------------------------------
# load_env：将 .env 中的所有变量导出为当前 shell 的环境变量，
# 供脚本读取 WEB_PORT、MYSQL_PORT、IMAGE_TAG 等。
# -----------------------------------------------------------------------------
load_env() {
    set -a
    # shellcheck source=/dev/null
    . "$ENV_FILE"
    set +a
}

# -----------------------------------------------------------------------------
# ensure_dirs：创建挂载所需的宿主目录（统一位于仓库根目录下）。
#   data/mysql       MySQL 数据
#   data/sql-local   已执行 SQL 记录
#   conf/logs        运行日志
#   conf/upload      上传资源
#   docker/dist      打包产物
# -----------------------------------------------------------------------------
ensure_dirs() {
    # 挂载的宿主目录统一位于仓库根目录下
    mkdir -p "$REPO_ROOT/data/mysql" "$REPO_ROOT/data/sql-local" \
        "$REPO_ROOT/conf/logs" "$REPO_ROOT/conf/upload" "$DIST_DIR"
}

# -----------------------------------------------------------------------------
# compose <args...>：统一以 .env 与指定 compose 文件调用 docker compose。
# -----------------------------------------------------------------------------
compose() {
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

# -----------------------------------------------------------------------------
# require_docker：校验 docker 与 docker compose v2 是否可用，缺失则直接退出。
# -----------------------------------------------------------------------------
require_docker() {
    command -v docker >/dev/null 2>&1 || { echo "[ERROR] 未找到 docker" >&2; exit 1; }
    docker compose version >/dev/null 2>&1 || { echo "[ERROR] 未找到 docker compose v2" >&2; exit 1; }
}

# -----------------------------------------------------------------------------
# detect_arch：检测宿主 CPU 架构，归一化为 Docker 平台标识。
#   用于多架构镜像构建与拉取时展示实际使用的架构。
# -----------------------------------------------------------------------------
detect_arch() {
    case "$(uname -m)" in
        x86_64|amd64)   echo "linux/amd64" ;;
        aarch64|arm64)  echo "linux/arm64" ;;
        armv7l|armhf)   echo "linux/arm/v7" ;;
        *)              echo "linux/$(uname -m)" ;;
    esac
}

# -----------------------------------------------------------------------------
# ensure_buildx_builder：确保存在可用的 buildx builder（多架构构建依赖）。
#   默认使用名为 blogloom-builder 的 docker-container 驱动 builder，
#   由 push.sh 通过 --builder 显式指定，不改变全局默认 builder。
# -----------------------------------------------------------------------------
BUILDX_BUILDER="${BUILDX_BUILDER:-blogloom-builder}"
ensure_buildx_builder() {
    docker buildx version >/dev/null 2>&1 \
        || { echo "[ERROR] 未找到 docker buildx（多架构构建需要 docker-buildx-plugin）" >&2; exit 1; }
    if ! docker buildx inspect "$BUILDX_BUILDER" >/dev/null 2>&1; then
        printf '[buildx] 创建 builder %s（首次会拉取 buildkit 镜像）\n' "$BUILDX_BUILDER"
        docker buildx create --name "$BUILDX_BUILDER" >/dev/null
    fi
    docker buildx inspect --bootstrap "$BUILDX_BUILDER" >/dev/null 2>&1 || true
}

# -----------------------------------------------------------------------------
# prune_buildx_cache：清理指定 builder 的构建缓存，释放 Docker 磁盘空间。
# 多架构构建会缓存两套架构的中间层，长期累积可能撑满 Docker 磁盘。
# -----------------------------------------------------------------------------
prune_buildx_cache() {
    local builder="${1:-$BUILDX_BUILDER}"
    printf '[prune] 清理 buildx 构建缓存（builder：%s）\n' "$builder"
    docker buildx prune -af --builder "$builder" >/dev/null 2>&1 || true
}

# -----------------------------------------------------------------------------
# print_docker_disk_help：构建因磁盘空间不足失败时输出清理指引。
# -----------------------------------------------------------------------------
print_docker_disk_help() {
    local builder="${1:-$BUILDX_BUILDER}"
    printf '[ERROR] 构建失败，疑似 Docker 磁盘空间不足（no space left on device）。\n' >&2
    printf '        可尝试以下清理后重试：\n' >&2
    printf '        - 清理 buildx 构建缓存：docker buildx prune -af --builder %s\n' "$builder" >&2
    printf '        - 清理未使用镜像/容器/缓存：docker system prune -af\n' >&2
    printf '        - 清理未使用数据卷（谨慎，会删数据）：docker volume prune\n' >&2
    printf '        - Docker Desktop → Settings → Resources 增大磁盘容量后重启\n' >&2
    printf '        - 或直接带 --prune 重试：./scripts/push.sh --prune <版本号>\n' >&2
}

# -----------------------------------------------------------------------------
# warn_docker_disk：构建前检查 Docker 可回收空间，超过阈值时给出提醒。
# 阈值 MB，默认 8192（8GB）；检测失败时静默跳过，不影响构建。
# -----------------------------------------------------------------------------
warn_docker_disk() {
    local threshold_mb="${1:-8192}" reclaim=""
    # docker system df 的 Reclaimable 形如 "30.6GB (95%)"，统一换算为 MB
    reclaim="$(docker system df --format '{{.Reclaimable}}' 2>/dev/null | awk '
        { v=$1; u=v; gsub(/[0-9.]/,"",u); gsub(/[^0-9.]/,"",v);
          if (u=="GB") v=v*1024; else if (u=="kB") v=v/1024; else if (u=="B") v=v/1048576;
          s+=v } END { printf "%d", s }')" 2>/dev/null || true
    [ -n "$reclaim" ] || return 0
    if [ "$reclaim" -ge "$threshold_mb" ] 2>/dev/null; then
        printf '[warn] Docker 可回收空间约 %s MB，磁盘偏紧；如构建报 “no space left”，可执行：\n' "$reclaim"
        printf '       docker system prune -af   # 或 ./scripts/push.sh --prune\n'
    fi
}

# -----------------------------------------------------------------------------
# load_image_tar <tar 路径>：若传入镜像 tar 且文件存在，则执行 docker load。
# 传空值时直接返回，交由调用方决定是否使用本地已有镜像。
# -----------------------------------------------------------------------------
load_image_tar() {
    local tar="$1"
    [ -n "$tar" ] || return 0
    [ -f "$tar" ] || { echo "[ERROR] 镜像包不存在：$tar" >&2; exit 1; }
    printf '[image] docker load -i %s\n' "$tar"
    docker load -i "$tar"
}
