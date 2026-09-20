#!/usr/bin/env bash
#
# =============================================================================
# BlogLoom 用户版升级脚本（upgrade.sh）
# =============================================================================
# 用途：
#   在用户部署目录执行，完成「解析最新版本 tag → 拉取该版本镜像 → 重启服务」。
#   容器启动脚本会自动执行数据库增量 SQL，因此升级无需手工处理数据库。
#   使用具体版本号 tag（而非 latest），便于通过 docker ps/images 直观看到运行版本。
#
# 用法（在部署目录执行）：
#   ./upgrade.sh            # 自动解析并升级到最新版本
#   ./upgrade.sh 1.0.1      # 升级到指定版本
#
# 等价手动命令：
#   docker compose pull && docker compose up -d
#
# 核心步骤：
#   0) 前置校验（docker / compose / 部署目录）
#   1) 解析目标版本 tag（参数 > Docker Hub 最新版本 > 回退 latest）
#   2) 把版本写入 .env 的 IMAGE_TAG（保证后续 compose 命令一致）
#   3) docker compose pull 拉取目标版本镜像
#   4) docker compose up -d 重建并启动（启动时自动执行增量 SQL）
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# 步骤 0：前置校验
# ---------------------------------------------------------------------------
command -v docker >/dev/null 2>&1 || { echo "[ERROR] 未安装 Docker"; exit 1; }
docker compose version >/dev/null 2>&1 || { echo "[ERROR] 未安装 Docker Compose v2"; exit 1; }
[ -f docker-compose.yml ] || { echo "[ERROR] 当前目录没有 docker-compose.yml，请在部署目录执行"; exit 1; }

IMAGE_REPO="${IMAGE_REPO:-codercl/blogloom}"

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
# 步骤 1：解析目标版本 tag
#   优先级：命令行参数 > 环境变量/Docker Hub 最新版本 > latest
# ---------------------------------------------------------------------------
if [ -n "${1:-}" ]; then
    TAG="$1"
    echo "[upgrade] 指定版本：$TAG"
else
    TAG="$(resolve_latest_tag)"
    if [ -n "$TAG" ]; then
        echo "[upgrade] 解析到最新版本：$TAG"
    else
        TAG="${IMAGE_TAG:-latest}"
        echo "[upgrade] 未解析到版本，回退使用：$TAG"
    fi
fi

# ---------------------------------------------------------------------------
# 步骤 2：把版本写入 .env 的 IMAGE_TAG
#   保证后续 docker compose 命令与本次升级使用同一版本。
# ---------------------------------------------------------------------------
if [ -f .env ] && grep -q '^IMAGE_TAG=' .env; then
    sed -i.bak -E "s|^IMAGE_TAG=.*|IMAGE_TAG=${TAG}|" .env && rm -f .env.bak
else
    echo "IMAGE_TAG=${TAG}" >> .env
fi
export IMAGE_TAG="$TAG"

# ---------------------------------------------------------------------------
# 步骤 3：拉取目标版本镜像
# ---------------------------------------------------------------------------
echo "[upgrade] 拉取镜像 ${IMAGE_REPO}:${TAG} ..."
docker compose pull

# ---------------------------------------------------------------------------
# 步骤 4：重建并启动
#   镜像变化时 compose 会重建应用容器；容器入口 start.sh 自动执行增量 SQL。
# ---------------------------------------------------------------------------
echo "[upgrade] 重启服务（容器启动时自动执行增量 SQL）..."
docker compose up -d

# ---------------------------------------------------------------------------
# 步骤 5：输出结果
# ---------------------------------------------------------------------------
echo "[done] 已升级到 ${IMAGE_REPO}:${TAG}"
echo "       查看运行版本：docker ps --format '{{.Names}} {{.Image}}'"
echo "       查看日志：    docker logs -f blogloom-app"
