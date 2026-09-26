# BlogLoom 一键部署（用户版）

只想快速把博客跑起来？**只需 Docker**，无需 JDK / Node / Maven，也无需克隆仓库。

## Linux / macOS 三步部署

```bash
# 1. 创建并进入一个空文件夹
mkdir blogloom && cd blogloom

# 2. 一键部署（自动拉取镜像并启动）
curl -fsSL https://raw.githubusercontent.com/changluya/BlogLoom/master/docker/standalone/install.sh | bash

# 3. 浏览器访问
#    博客前台 http://<服务器IP>:18080
#    管理后台 http://<服务器IP>:18080/cms
#    默认账号 admin / 123456
```

脚本会自动写入 `docker-compose.yml`、从 Docker Hub 拉取 `codercl/blogloom` 镜像并启动 `blogloom-app` + `blogloom-mysql` 两个容器，应用启动时自动完成数据库初始化。

## Windows PowerShell 一键部署

Windows 无需安装 Bash。请在 **PowerShell** 中执行（不要在 CMD 中执行）：

```powershell
New-Item -ItemType Directory -Force blogloom | Out-Null
Set-Location blogloom
irm https://raw.githubusercontent.com/changluya/BlogLoom/master/docker/standalone/install.ps1 | iex
```

> `install.sh` 必须由 Bash 解释，Windows 没有 `/bin/bash` 时会在运行脚本前失败，因此 Windows 需要使用上面的 PowerShell 入口。

## 前置条件

- Linux/macOS 已安装 Docker，或 Windows 已安装并启动 Docker Desktop
- Docker Compose v2（`docker compose`）
- 服务器可访问 Docker Hub

## 自定义配置（可选）

首次部署时，脚本会**自动生成随机 MySQL root 密码与登录令牌密钥**并写入部署目录的 `.env`，部署完成后会在终端打印，请妥善保存（重复执行/升级会复用，不会改变）。

也可以在同目录创建 `.env` 提前指定（则不会自动生成）：

```bash
WEB_PORT=8080
MYSQL_PORT=13306
MYSQL_ROOT_PASSWORD=your-strong-password   # 不设置则首次自动生成随机密码
TOKEN_SECRET=your-long-random-secret       # 不设置则首次自动生成随机密钥
BLOG_API=http://your-domain:8080
BLOG_CMS=http://your-domain:8080/cms
BLOG_VIEW=http://your-domain:8080
IMAGE_TAG=1.0.0        # 默认 latest
```

修改后重新 `docker compose up -d` 生效。

> 查看当前数据库密码：`grep MYSQL_ROOT_PASSWORD .env`。

## 连接数据库（外网）

MySQL 默认映射到宿主机 `13306` 端口（容器内仍为 3306），可用 `MYSQL_PORT` 覆盖：

```text
主机：<服务器IP>
端口：13306
用户：root
密码：见部署目录 .env 中的 MYSQL_ROOT_PASSWORD（首次自动随机生成）
数据库：blogloom          # 可用 MYSQL_DATABASE 覆盖
```

> **安全提示**：MySQL 对外暴露存在风险。请在服务器安全组/防火墙中仅放行可信来源 IP 到 `MYSQL_PORT`，切勿对整个公网开放。
>
> 如不需要外网直连，删除 `docker-compose.yml` 中 mysql 服务的 `ports` 段即可（应用仍可通过容器内网 `mysql:3306` 连接）。

## 版本升级

升级流程：**自动从 Docker Hub 解析最新版本 tag → 拉取该版本镜像 → 重启**（容器启动时自动执行数据库增量 SQL）。使用具体版本号 tag（而非 `latest`），便于通过 `docker images` 直观看到运行版本。

```bash
cd blogloom
./upgrade.sh            # 自动解析并升级到最新版本
./upgrade.sh 1.0.1      # 升级到指定版本
```

Windows PowerShell：

```powershell
cd blogloom
powershell -ExecutionPolicy Bypass -File .\upgrade.ps1
# 指定版本
powershell -ExecutionPolicy Bypass -File .\upgrade.ps1 1.0.1
```

等价手动命令：

```bash
docker compose pull && docker compose up -d
```

> Linux/macOS 安装会生成 `upgrade.sh`，Windows 安装会下载 `upgrade.ps1`，并把当前版本写入 `.env` 的 `IMAGE_TAG`。数据在 `./data/`，升级不会丢失。
> 查看当前运行版本：`docker ps --format '{{.Names}} {{.Image}}'`。

## 常用运维

```bash
docker compose ps                 # 查看状态
docker logs -f blogloom-app       # 应用日志
docker compose restart blogloom   # 重启（自动执行增量 SQL）
docker compose down               # 停止（数据保留）
```

## 数据目录

所有运行时数据都绑定挂载到**部署目录下的 `./data/`**，便于查看与备份：

```text
blogloom/
├── docker-compose.yml
├── install.sh
└── data/
    ├── mysql/       # MySQL 数据
    ├── logs/        # 运行日志
    ├── upload/      # 上传资源
    └── sql-local/   # 已执行 SQL 记录
```

重启 / 升级不会丢失；备份时重点备份 `data/mysql` 与 `data/upload`。删除 `data/mysql` 与 `data/sql-local` 后重启可重新初始化数据库（会清空数据）。

## 手动方式（不使用 install.sh）

```bash
curl -fsSLO https://raw.githubusercontent.com/changluya/BlogLoom/master/docker/standalone/docker-compose.yml
docker compose up -d
```
