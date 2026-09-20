#!/usr/bin/env bash
#
# =============================================================================
# 一键打包（package.sh）
# =============================================================================
# 用途：
#   在开发机构建 BlogLoom 一体化镜像（后端 + 博客前台 + 管理后台），
#   并导出可离线传输的镜像包与部署包，供服务器一键部署 / 升级使用。
#
# 用法（在 docker/ 目录执行）：
#   ./scripts/package.sh [版本号]
#   ./scripts/package.sh 1.0.0        # 不传则取 .env 的 IMAGE_TAG，默认 1.0.0
#
# 产物（docker/dist/）：
#   blogloom-<版本号>.tar           仅镜像包（docker load 使用）
#   blogloom-deploy-<版本号>.tar.gz 离线部署包（含 docker/ 脚本、conf/ 配置与 sql/ 增量 SQL）
#
# 核心流程：
#   1. 解析版本号与镜像名；
#   2. docker build 构建一体化镜像（构建上下文为仓库根目录）；
#   3. docker save 导出镜像 tar；
#   4. 生成离线部署包（tar.gz），把镜像 tar 与部署所需文件一并打包。
# =============================================================================

set -euo pipefail

# 引入公共库（路径解析、require_docker、read_env_value、DIST_DIR 等）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
. "$SCRIPT_DIR/common.sh"

# 前置校验：docker 与 docker compose v2 可用
require_docker

# ---------------------------------------------------------------------------
# 步骤 1：解析版本号与镜像名
#   优先使用命令行参数；否则读取 .env 的 IMAGE_TAG；再否则默认 1.0.0。
# ---------------------------------------------------------------------------
VERSION="${1:-$(read_env_value IMAGE_TAG 1.0.0)}"
IMAGE="blogloom:${VERSION}"

mkdir -p "$DIST_DIR"

# ---------------------------------------------------------------------------
# 步骤 2：构建镜像
#   -f 指定 docker/Dockerfile；构建上下文为仓库根目录（末尾的 "$REPO_ROOT"），
#   因为 Dockerfile 需要访问 blog-backend/、blog-view-ui/、blog-cms-ui/、conf/、docker/container/。
#   镜像内阶段：backend-build（Maven）→ view-build（Node）→ cms-build（Node）→ runtime。
# ---------------------------------------------------------------------------
printf '[build] 构建镜像 %s（上下文：%s）\n' "$IMAGE" "$REPO_ROOT"
docker build -f "$DOCKERFILE" -t "$IMAGE" "$REPO_ROOT"

# ---------------------------------------------------------------------------
# 步骤 3：导出镜像 tar（供服务器 docker load 使用，适合离线/无构建环境）
# ---------------------------------------------------------------------------
IMAGE_TAR="$DIST_DIR/blogloom-${VERSION}.tar"
printf '[save] 导出镜像到 %s\n' "$IMAGE_TAR"
docker save "$IMAGE" -o "$IMAGE_TAR"

# ---------------------------------------------------------------------------
# 步骤 4：生成离线部署包
#   打包内容（保持仓库相对路径，解压后可直接 cd docker && ./scripts/deploy.sh）：
#     docker/                部署定义与脚本、镜像 tar（dist/）
#     conf/                  配置模板（application.properties、logback-spring.xml）
#     sql/increment/         增量 SQL
#   运行时会生成的 data/、conf/logs、conf/upload 不纳入打包，由 deploy.sh 创建。
# ---------------------------------------------------------------------------
BUNDLE="$DIST_DIR/blogloom-deploy-${VERSION}.tar.gz"
printf '[bundle] 生成离线部署包 %s\n' "$BUNDLE"
tar --exclude='.DS_Store' -czf "$BUNDLE" \
    -C "$REPO_ROOT" \
    docker/Dockerfile \
    docker/docker-compose.yml \
    docker/README.md \
    docker/.env.example \
    docker/scripts \
    docker/container \
    docker/dist/blogloom-${VERSION}.tar \
    sql/increment \
    conf/application.properties \
    conf/logback-spring.xml

# ---------------------------------------------------------------------------
# 步骤 5：输出产物路径与后续操作提示
# ---------------------------------------------------------------------------
printf '[done] 打包完成：\n'
printf '       镜像包：%s\n' "$IMAGE_TAR"
printf '       部署包：%s\n' "$BUNDLE"
printf '[next] 本地部署：./scripts/deploy.sh %s\n' "$IMAGE_TAR"
printf '[next] 离线部署：上传部署包到服务器解压后执行 cd docker && ./scripts/deploy.sh\n'
