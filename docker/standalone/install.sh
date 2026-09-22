#!/usr/bin/env bash
#
# =============================================================================
# BlogLoom 一键部署脚本（用户版，install.sh）
# =============================================================================
# 用途：
#   面向只想快速把博客跑起来的用户：无需 JDK / Node / Maven，无需克隆仓库，
#   只要服务器装了 Docker，即可从 Docker Hub 拉取镜像并一键启动。
#
# 用法（在任意空文件夹执行）：
#   mkdir blogloom && cd blogloom
#   curl -fsSL https://raw.githubusercontent.com/changluya/BlogLoom/master/docker/standalone/install.sh | bash
#
# 核心步骤：
#   0) 前置校验（docker / docker compose v2）
#   1) 解析镜像版本 tag（环境变量 > Docker Hub 最新版本 > latest）
#   2) 生成自包含 docker-compose.yml（仅依赖 Docker Hub）
#   3) 写入 .env（记录当前版本）
#   4) 生成 upgrade.sh（后续一键升级）
#   5) 创建绑定挂载数据目录 ./data/{mysql,logs,upload,sql-local}
#   6) 拉取镜像并启动（应用启动时自动初始化/升级数据库）
#
# 说明：
#   - 所有运行时数据都落在当前目录 ./data/ 下，便于查看与备份；
#   - 只对外暴露一个 Web 端口（默认 18080），MySQL 仅容器内网；
#   - 默认账号：admin / 123456（登录后请立即修改）。
#
# 可选环境变量（也可在同目录 .env 中覆盖）：
#   WEB_PORT=18080            对外 Web 端口
#   MYSQL_ROOT_PASSWORD=...   数据库密码
#   TOKEN_SECRET=...          登录令牌密钥
#   BLOG_API / BLOG_CMS / BLOG_VIEW  站点对外地址
#   IMAGE_REPO / IMAGE_TAG    镜像仓库与版本（默认自动解析最新版本）
# =============================================================================

set -euo pipefail

WEB_PORT="${WEB_PORT:-18080}"
IMAGE_REPO="${IMAGE_REPO:-codercl/blogloom}"

# ---------------------------------------------------------------------------
# 步骤 0：前置校验
# ---------------------------------------------------------------------------
command -v docker >/dev/null 2>&1 || { echo "[ERROR] 未安装 Docker，请先安装：https://docs.docker.com/engine/install/"; exit 1; }
docker compose version >/dev/null 2>&1 || { echo "[ERROR] 未安装 Docker Compose v2"; exit 1; }

# ---------------------------------------------------------------------------
# 从 Docker Hub 解析最新的语义化版本 tag（如 1.0.1）
#   - 调用 Docker Hub Tags API，筛选形如 x.y.z 的 tag，取最大版本号；
#   - 缺少 curl/python3 或请求失败时返回空，由调用方回退。
# ---------------------------------------------------------------------------
resolve_latest_tag() {
    command -v curl >/dev/null 2>&1 || return 0
    command -v python3 >/dev/null 2>&1 || return 0
    curl -fsSL "https://hub.docker.com/v2/repositories/${IMAGE_REPO}/tags?page_size=100" 2>/dev/null \
        | python3 -c '
import sys, json, re
try:
    d = json.load(sys.stdin)
except Exception:
    sys.exit(0)
vs = []
for t in d.get("results", []):
    m = re.fullmatch(r"v?(\d+)\.(\d+)\.(\d+)", t.get("name", ""))
    if m:
        vs.append((tuple(int(x) for x in m.groups()), t["name"]))
if vs:
    print(sorted(vs)[-1][1])
' 2>/dev/null || true
}

# ---------------------------------------------------------------------------
# 检测宿主 CPU 架构，归一化为 Docker 平台标识。
#   多架构镜像（amd64/arm64 manifest）在 pull 时由 Docker 按此架构自动选择，
#   这里仅用于显式提示当前使用的架构，无需手工指定 platform。
# ---------------------------------------------------------------------------
detect_arch() {
    case "$(uname -m)" in
        x86_64|amd64)   echo "linux/amd64" ;;
        aarch64|arm64)  echo "linux/arm64" ;;
        armv7l|armhf)   echo "linux/arm/v7" ;;
        *)              echo "linux/$(uname -m)" ;;
    esac
}

# ---------------------------------------------------------------------------
# 步骤 1：解析镜像版本 tag
#   优先级：环境变量 IMAGE_TAG > Docker Hub 最新版本 > latest
# ---------------------------------------------------------------------------
TAG="${IMAGE_TAG:-}"
if [ -z "$TAG" ]; then
    TAG="$(resolve_latest_tag)"
fi
if [ -n "$TAG" ]; then
    echo "[init] 使用镜像版本：${IMAGE_REPO}:${TAG}"
else
    TAG="latest"
    echo "[init] 未能解析版本，使用：${IMAGE_REPO}:${TAG}"
fi
export IMAGE_TAG="$TAG"

# ---------------------------------------------------------------------------
# 步骤 2：生成自包含 docker-compose.yml（若不存在）
#   仅依赖 Docker Hub 镜像；数据绑定挂载到 ./data；只暴露 Web 端口。
# ---------------------------------------------------------------------------
if [ ! -f docker-compose.yml ]; then
    cat > docker-compose.yml <<'YAML'
name: blogloom

services:
  mysql:
    image: mysql:8.0
    container_name: blogloom-mysql
    restart: unless-stopped
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-blogloom_root_pwd}
      MYSQL_ROOT_HOST: "%"
      MYSQL_DATABASE: ${MYSQL_DATABASE:-blogloom}
      TZ: ${TZ:-Asia/Shanghai}
    volumes:
      - ./data/mysql:/var/lib/mysql
    healthcheck:
      test: ["CMD-SHELL", "mysql -h 127.0.0.1 -uroot -p$$MYSQL_ROOT_PASSWORD -e 'SELECT 1' >/dev/null 2>&1"]
      interval: 5s
      timeout: 5s
      retries: 30

  blogloom:
    image: ${IMAGE_REPO:-codercl/blogloom}:${IMAGE_TAG:-latest}
    container_name: blogloom-app
    restart: unless-stopped
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      DB_HOST: mysql
      DB_PORT: "3306"
      DB_USER: root
      DB_PASSWORD: ${MYSQL_ROOT_PASSWORD:-blogloom_root_pwd}
      DB_NAME: ${MYSQL_DATABASE:-blogloom}
      BLOG_NAME: ${BLOG_NAME:-BlogLoom}
      BLOG_API: ${BLOG_API:-http://localhost:18080}
      BLOG_CMS: ${BLOG_CMS:-http://localhost:18080/cms}
      BLOG_VIEW: ${BLOG_VIEW:-http://localhost:18080}
      TOKEN_SECRET: ${TOKEN_SECRET:-please-change-this-token-secret}
      TZ: ${TZ:-Asia/Shanghai}
      JAVA_OPTS: ${JAVA_OPTS:--Xms256m -Xmx512m}
    ports:
      - "${WEB_PORT:-18080}:8090"
    volumes:
      - ./data/logs:/opt/blogloom/conf/logs
      - ./data/upload:/opt/blogloom/conf/upload
      - ./data/sql-local:/opt/blogloom/sql/local
YAML
    echo "[init] 已生成 docker-compose.yml"
fi

# ---------------------------------------------------------------------------
# 步骤 3：写入 .env（记录当前使用的版本，保证后续 compose 命令一致）
# ---------------------------------------------------------------------------
if [ -f .env ] && grep -q '^IMAGE_TAG=' .env; then
    sed -i.bak -E "s|^IMAGE_TAG=.*|IMAGE_TAG=${TAG}|" .env && rm -f .env.bak
else
    echo "IMAGE_TAG=${TAG}" >> .env
fi

# ---------------------------------------------------------------------------
# 步骤 4：生成 upgrade.sh（若不存在）
#   后续升级只需 ./upgrade.sh [版本号]，内部为「解析版本 → pull → up -d」。
# ---------------------------------------------------------------------------
if [ ! -f upgrade.sh ]; then
    cat > upgrade.sh <<'SH'
#!/usr/bin/env bash
#
# BlogLoom 用户版升级脚本：解析最新版本 tag → 拉取镜像 → 重启（启动时自动执行增量 SQL）。
# 用法：./upgrade.sh            # 自动升级到最新版本
#       ./upgrade.sh 1.0.1      # 升级到指定版本
# 等价：docker compose pull && docker compose up -d
set -euo pipefail
# 前置校验
command -v docker >/dev/null 2>&1 || { echo "[ERROR] 未安装 Docker"; exit 1; }
docker compose version >/dev/null 2>&1 || { echo "[ERROR] 未安装 Docker Compose v2"; exit 1; }
[ -f docker-compose.yml ] || { echo "[ERROR] 当前目录没有 docker-compose.yml，请在部署目录执行"; exit 1; }
IMAGE_REPO="${IMAGE_REPO:-codercl/blogloom}"
# 从 Docker Hub 解析最新语义化版本 tag；失败返回空
resolve_latest_tag() {
    command -v curl >/dev/null 2>&1 || return 0
    command -v python3 >/dev/null 2>&1 || return 0
    curl -fsSL "https://hub.docker.com/v2/repositories/${IMAGE_REPO}/tags?page_size=100" 2>/dev/null \
        | python3 -c '
import sys, json, re
try:
    d = json.load(sys.stdin)
except Exception:
    sys.exit(0)
vs = []
for t in d.get("results", []):
    m = re.fullmatch(r"v?(\d+)\.(\d+)\.(\d+)", t.get("name", ""))
    if m:
        vs.append((tuple(int(x) for x in m.groups()), t["name"]))
if vs:
    print(sorted(vs)[-1][1])
' 2>/dev/null || true
}
# 检测宿主架构（多架构镜像由 Docker 按此自动选择）
detect_arch() {
    case "$(uname -m)" in
        x86_64|amd64)   echo "linux/amd64" ;;
        aarch64|arm64)  echo "linux/arm64" ;;
        armv7l|armhf)   echo "linux/arm/v7" ;;
        *)              echo "linux/$(uname -m)" ;;
    esac
}
# 步骤1：解析目标版本（参数 > Docker Hub 最新 > 回退 latest）
if [ -n "${1:-}" ]; then
    TAG="$1"; echo "[upgrade] 指定版本：$TAG"
else
    TAG="$(resolve_latest_tag)"
    if [ -n "$TAG" ]; then echo "[upgrade] 解析到最新版本：$TAG"; else TAG="${IMAGE_TAG:-latest}"; echo "[upgrade] 未解析到版本，回退：$TAG"; fi
fi
# 步骤2：写入 .env
if [ -f .env ] && grep -q '^IMAGE_TAG=' .env; then
    sed -i.bak -E "s|^IMAGE_TAG=.*|IMAGE_TAG=${TAG}|" .env && rm -f .env.bak
else
    echo "IMAGE_TAG=${TAG}" >> .env
fi
export IMAGE_TAG="$TAG"
# 步骤3：拉取镜像（多架构镜像按宿主架构自动选择）
echo "[upgrade] 宿主架构：$(detect_arch)，拉取镜像 ${IMAGE_REPO}:${TAG} ..."; docker compose pull
# 步骤4：重建并启动（启动时自动执行增量 SQL）
echo "[upgrade] 重启服务（容器启动时自动执行增量 SQL）..."; docker compose up -d
echo "[done] 已升级到 ${IMAGE_REPO}:${TAG}"
SH
    chmod +x upgrade.sh
    echo "[init] 已生成 upgrade.sh"
fi

# ---------------------------------------------------------------------------
# 步骤 5：创建绑定挂载的数据目录（数据全部落在当前目录 ./data 下）
# ---------------------------------------------------------------------------
mkdir -p data/mysql data/logs data/upload data/sql-local
echo "[init] 数据目录：$(pwd)/data"

# ---------------------------------------------------------------------------
# 步骤 6：拉取镜像并启动服务
#   镜像为多架构（linux/amd64 + linux/arm64），Docker 会按宿主架构自动选择；
#   首次会下载镜像；应用容器入口自动执行增量 SQL（首次=全量初始化）。
# ---------------------------------------------------------------------------
ARCH="$(detect_arch)"
echo "[deploy] 检测到宿主架构：${ARCH}，将从多架构镜像中自动选择对应架构"
echo "[deploy] 拉取镜像并启动服务（首次会下载镜像，请稍候）..."
docker compose pull
docker compose up -d

# ---------------------------------------------------------------------------
# 步骤 7：输出访问信息
# ---------------------------------------------------------------------------
echo
echo "================ BlogLoom 部署完成 ================"
echo "  博客前台：http://<服务器IP>:${WEB_PORT}"
echo "  管理后台：http://<服务器IP>:${WEB_PORT}/cms"
echo "  默认账号：admin / 123456（请登录后立即修改）"
echo "  运行版本：${IMAGE_REPO}:${TAG}"
echo "  数据目录：$(pwd)/data"
echo "  版本升级：./upgrade.sh"
echo "  查看日志：docker logs -f blogloom-app"
echo "  停止服务：docker compose down"
echo "==================================================="
