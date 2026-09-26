# BlogLoom Docker 部署

本目录提供 BlogLoom 的容器化方案，**只有两个容器**：

- `blogloom-app`：Spring Boot 后端服务，同时提供博客前台与管理后台页面；容器启动脚本会先执行增量 SQL，再启动服务；
- `blogloom-mysql`：MySQL 8。

对外只暴露两个固定非默认端口：Web `18080`、MySQL `13306`。

```text
┌──────────────────────────────────────────────┐
│                blogloom-app                  │
│   bin/start.sh → 执行增量 SQL → 启动后端       │
│   Spring Boot（8090）                         │
│   ├─ /            博客前台（conf/static/view）│
│   ├─ /cms         管理后台（conf/static/cms） │
│   ├─ /admin/**    管理端 API                  │
│   └─ /static/**   上传资源                     │
└───────────────┬──────────────────────────────┘
                │ 内部网络 3306
        ┌───────▼────────┐
        │  blogloom-mysql │
        └────────────────┘
```

> **按你的身份选择视角：**
> - **只想把博客跑起来** → [一、用户视角（一键部署）](#一用户视角一键部署使用-docker-hub-镜像)
> - **要构建 / 打包 / 发布 / 二次开发** → [二、本地开发视角（构建 → 部署 → 发布）](#二本地开发视角构建--打包--部署--发布)

---

## 一、用户视角（两个亮点）

> 面向只想快速把博客跑起来的用户：**无需 JDK / Node / Maven，也无需克隆仓库**，只要服务器装了 Docker，直接用已发布到 Docker Hub 的镜像即可。

**前置条件**：Linux/macOS 已安装 Docker，或 Windows 已安装并启动 Docker Desktop；需支持 Docker Compose v2 并可访问 Docker Hub。

### 亮点一：第一次本地快速部署（一条命令）

```bash
mkdir blogloom && cd blogloom
curl -fsSL https://raw.githubusercontent.com/changluya/BlogLoom/master/docker/standalone/install.sh | bash
```

Windows PowerShell：

```powershell
New-Item -ItemType Directory -Force blogloom | Out-Null; Set-Location blogloom
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$ProgressPreference = 'SilentlyContinue'
irm 'https://raw.githubusercontent.com/changluya/BlogLoom/master/docker/standalone/install.ps1' | iex
```

> 如提示“基础连接已经关闭”，请使用 [Windows `curl.exe` 回退方案](standalone/README.md#windows-powershell-一键部署)。

自动解析最新版本、拉取镜像、启动 `blogloom-app` + `blogloom-mysql` 并完成数据库初始化。

| 入口 | 地址 |
| --- | --- |
| 博客前台 | `http://<服务器IP>:18080` |
| 管理后台 | `http://<服务器IP>:18080/cms` |
| 默认账号 | `admin` / `123456`（登录后请立即修改） |

### 亮点二：后续快速升级服务（一条命令）

```bash
cd blogloom
./upgrade.sh            # 自动解析最新版本并升级
./upgrade.sh 1.0.1      # 升级到指定版本
```

自动拉取新版本镜像并重启，容器启动时自动执行数据库增量 SQL，**数据不丢失**。

> 更多说明（自定义配置、数据目录、备份、运维）见 [standalone/README.md](./standalone/README.md)。

---

## 二、本地开发视角（构建 → 打包 → 部署 → 发布）

### 1. 架构与设计

- **一体化镜像**：一个镜像同时打包后端 jar、博客前台产物、管理后台产物、增量 SQL；
- **后端托管前端**：`conf/static/view/` 映射根路径 `/`，`conf/static/cms/` 映射 `/cms`；后端通过 `SpaForwardConfig` 为 history 路由做回退，无需额外 Nginx；
- **两容器**：`blogloom-app` + `blogloom-mysql`，不引入 Redis / 迁移容器；
- **启动时增量升级**：容器入口 `bin/start.sh` 顺序执行「等待 MySQL → 执行增量 SQL → 启动 Java」；
- **幂等迁移**：扫描 `sql/increment/**/*.sql`（排除 `init.sql`），与 `data/sql-local/` 记录比对，只执行未记录过的 SQL，成功后写入记录；数据库已有数据时自动跳过全量初始化脚本。

因此：**首次启动 = 全量初始化；重启/重建 = 增量升级检查。**

### 2. 目录结构

`docker/` 目录（部署定义与脚本）：

```text
docker/
├── README.md
├── Dockerfile                  # 一体化镜像（后端 + 两个前端 + JRE + 内置 SQL）
├── docker-compose.yml          # 仓库版：app + mysql，挂载仓库 conf/sql/data
├── .env.example                # 部署配置示例
├── scripts/                    # 宿主机一键脚本
│   ├── common.sh               # 公共函数
│   ├── package.sh              # 一键打包
│   ├── deploy.sh               # 一键部署
│   ├── upgrade.sh              # 一键升级（增量）
│   └── push.sh                 # 推送镜像到 Docker Hub
├── container/                  # 镜像内使用的内容
│   ├── bin/                    # start.sh / upgrade-sql.sh
│   └── mysql/init/             # MySQL 首次初始化授权脚本
└── standalone/                 # 用户版一键部署（仅需 Docker + Docker Hub）
    ├── install.sh              # 一键部署脚本
    ├── upgrade.sh              # 一键升级脚本
    ├── docker-compose.yml      # 自包含 compose（绑定挂载 ./data）
    └── README.md
```

挂载的宿主目录统一位于**仓库根目录**下：

```text
BlogLoom/
├── conf/
│   ├── application.properties  # 应用配置（挂载，只读）
│   ├── logback-spring.xml      # 日志配置（挂载，只读）
│   ├── logs/                   # 运行日志（挂载，持久化）
│   └── upload/                 # 上传资源（挂载，持久化）
├── sql/
│   └── increment/              # 增量 SQL（挂载，只读）
└── data/
    ├── mysql/                  # MySQL 数据（挂载，持久化）
    └── sql-local/              # 已执行 SQL 记录（挂载，持久化）
```

容器内遵循 `conf / lib / bin` 部署结构：

```text
/opt/blogloom/
├── bin/                        # start.sh / upgrade-sql.sh
├── conf/
│   ├── application.properties  # 外置配置
│   ├── logback-spring.xml
│   ├── static/view             # 博客前台（映射到 /）
│   ├── static/cms              # 管理后台（映射到 /cms）
│   ├── logs                    # 日志
│   └── upload                  # 上传资源
├── lib/blog-backend.jar
└── sql/
    ├── increment/              # 增量 SQL
    └── local/                  # 已执行记录
```

### 3. 环境要求

- 已安装 Docker 与 Docker Compose v2；
- 打包机可访问外网（构建阶段拉取 Maven / npm 依赖）。

### 4. 一键打包

```bash
cd docker
./scripts/package.sh 1.0.0
```

产物位于 `docker/dist/`：

- `blogloom-1.0.0.tar`：镜像包（`docker load` 使用）；
- `blogloom-deploy-1.0.0.tar.gz`：离线部署包（含 `docker/`、`conf/` 配置与 `sql/increment`）。

### 5. 一键部署（仓库版）

方式 A：服务器已有仓库代码

```bash
cd docker
./scripts/deploy.sh
```

方式 B：离线部署包

```bash
tar xzf blogloom-deploy-1.0.0.tar.gz
cd docker
./scripts/deploy.sh
```

方式 C：直接使用 Docker Hub 镜像

```bash
IMAGE_REPO=codercl/blogloom ./scripts/deploy.sh
```

`deploy.sh` 会自动：生成 `.env`（随机 MySQL 密码 / 密钥）→ 创建 `data/`、`conf/logs`、`conf/upload` → 加载镜像（tar / 本地 / 仓库拉取）→ 启动两个容器；应用启动时自动等待 MySQL 并执行增量 SQL（首次即全量初始化）。

> 首次部署务必检查 `docker/.env` 中的 `BLOG_API`、`BLOG_CMS`、`BLOG_VIEW`，改为真实对外访问地址，否则上传资源、邮件与通知中的链接会指向错误地址。

### 6. 一键升级（增量）

```bash
cd docker
./scripts/upgrade.sh                       # 使用 .env 中的 IMAGE_TAG
./scripts/upgrade.sh dist/blogloom-1.0.1.tar
./scripts/upgrade.sh dist/blogloom-1.0.1.tar --dry-run   # 仅预览待执行 SQL
```

升级流程：加载新镜像 → 确保 MySQL 运行 → 用新镜像重建应用容器 → 启动脚本按 `data/sql-local` 历史记录执行未执行过的增量 SQL → 服务就绪。

> 仅更新 `sql/increment/` 下的增量 SQL 时，也可直接 `docker compose restart blogloom` 触发升级。
> 升级前建议备份数据库：`docker exec blogloom-mysql mysqldump -uroot -p blogloom > backup.sql`

### 7. 推送镜像到 Docker Hub（多架构）

仅需推送 BlogLoom 应用镜像（MySQL 使用官方 `mysql:8.0`，无需推送）。

```bash
docker login -u codercl          # 步骤 1：登录（PAT 需 Read/Write/Delete 权限）
./scripts/push.sh 1.0.0          # 步骤 2：一键多架构构建并推送 <DOCKERHUB_USER>/blogloom:1.0.0 与 :latest
./scripts/push.sh --no-latest    # 只推送指定 tag
./scripts/push.sh --prune 1.0.0  # 构建前清理 buildx 缓存，释放 Docker 磁盘空间
```

`push.sh` 默认用 `docker buildx` 构建 **多架构镜像**（`linux/amd64` + `linux/arm64`）并直接推送，生成多平台 manifest：

```text
codercl/blogloom:<IMAGE_TAG>    → linux/amd64 + linux/arm64
codercl/blogloom:latest         → linux/amd64 + linux/arm64
```

部署端 `docker compose pull` / `docker pull` 时会**按宿主架构自动选择**对应镜像层（amd64 服务器拉 amd64、arm64 服务器拉 arm64），不再出现平台不匹配告警。

其他用法：

```bash
./scripts/push.sh --platform linux/amd64,linux/arm64   # 自定义平台
./scripts/push.sh --single                             # 回退：推送 package.sh 构建的本地单架构镜像
```

> 多架构构建需要 `docker buildx`（Docker Desktop 自带；Linux 安装 `docker-buildx-plugin`），首次会拉取 buildkit 镜像。
> 多平台镜像无法 `docker load` 到本地，因此 `push.sh` 采用 buildx `--push` 直推；离线单架构镜像仍由 `package.sh` 生成 tar。
> 多架构构建会缓存两套架构的中间层，长期累积可能撑满 Docker 磁盘；脚本会在可回收空间过大时提醒，报 `no space left on device` 时按提示清理，或直接 `./scripts/push.sh --prune`。
> 由于应用镜像基于 `mysql:8.0`，基础层在 Hub 已存在，推送时会自动复用（`Mounted from library/mysql`），只有 JRE、jar、前端等自定义层需要上传。

### 8. 配置说明

**`.env`（docker/.env）**

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `WEB_PORT` | `18080` | 服务对外端口（前台/后台/API 共用） |
| `MYSQL_PORT` | `13306` | MySQL 对外端口 |
| `IMAGE_TAG` | `1.0.0` | 镜像版本，与打包版本一致（push.sh 也按此 tag 推送） |
| `IMAGE_REPO` | `blogloom` | 镜像仓库；从 Docker Hub 部署时设为 `codercl/blogloom` |
| `DOCKERHUB_USER` | `codercl` | Docker Hub 用户名（push.sh 推送镜像时使用） |
| `MYSQL_DATABASE` | `blogloom` | 数据库名 |
| `MYSQL_ROOT_PASSWORD` | 随机生成 | MySQL root 密码 |
| `MYSQL_PASSWORD` | 随机生成 | 应用账号 `blogloom` 密码 |
| `TOKEN_SECRET` | 随机生成 | 登录令牌密钥 |
| `BLOG_API` / `BLOG_CMS` / `BLOG_VIEW` | `http://localhost:18080` 等 | 站点对外地址 |
| `JAVA_OPTS` | `-Xms256m -Xmx512m` | JVM 参数 |

**`conf/application.properties`**：仓库根目录的配置文件会挂载到容器 `/opt/blogloom/conf/application.properties`，支持环境变量占位符（如 `${DB_HOST:mysql}`）。邮件、Telegram 通知等按需填写。

### 9. 数据持久化与备份

| 宿主目录 | 容器目录 | 内容 |
| --- | --- | --- |
| `data/mysql` | `/var/lib/mysql` | MySQL 数据 |
| `conf/logs` | `/opt/blogloom/conf/logs` | 运行日志 |
| `conf/upload` | `/opt/blogloom/conf/upload` | 上传资源 |
| `data/sql-local` | `/opt/blogloom/sql/local` | 已执行 SQL 记录 |

升级、重启不会丢失以上数据。备份时重点备份 `data/mysql` 与 `conf/upload`。

### 10. 常见问题

- **端口被占用**：修改 `docker/.env` 中 `WEB_PORT` / `MYSQL_PORT` 后重新 `./scripts/deploy.sh`；
- **前端刷新 404**：后端已内置 SPA history 路由回退，无需额外配置 Nginx；
- **查看启动日志**：`docker logs -f blogloom-app`；数据库日志：`docker logs -f blogloom-mysql`；
- **单独预览/执行增量 SQL**：`docker compose run --rm -T --no-deps --entrypoint /opt/blogloom/bin/upgrade-sql.sh blogloom [--dry-run]`；
- **重新初始化数据库**：停止服务后删除 `data/mysql` 与 `data/sql-local`，再执行 `./scripts/deploy.sh`（会清空全部数据）。

### 11. 与本地脚本的一致性

容器内 `docker/container/bin/upgrade-sql.sh` 与仓库 `bin/local/upgrate-sql.sh` 规则一致：按文件名排序、排除 `init.sql`、扁平记录目录、成功后归档。区别仅是容器内通过环境变量（`DB_HOST`、`DB_USER` 等）读取数据库配置，并额外提供「存量库跳过全量初始化」的保护。
