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
