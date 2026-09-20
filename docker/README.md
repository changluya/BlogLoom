# BlogLoom Docker 一键部署

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

## 用户快速部署（服务器一键）

> 面向只想快速把博客跑起来的用户：**无需 JDK / Node / Maven，也无需克隆仓库**，只要服务器装了 Docker，直接用已发布到 Docker Hub 的镜像即可。

**前置条件**：Linux 服务器已安装 Docker 与 Docker Compose v2，且可访问 Docker Hub。

```bash
# 1. 创建并进入一个空文件夹
mkdir blogloom && cd blogloom

# 2. 一键部署（自动拉取镜像并启动）
curl -fsSL https://raw.githubusercontent.com/changluya/BlogLoom/master/docker/standalone/install.sh | bash
```

脚本会自动：写入 `docker-compose.yml` → 从 Docker Hub 拉取 `codercl/blogloom` 镜像 → 启动 `blogloom-app` + `blogloom-mysql` 两个容器；应用首次启动自动完成数据库初始化。

访问地址：

- 博客前台：`http://<服务器IP>:18080`
- 管理后台：`http://<服务器IP>:18080/cms`
- 默认账号：`admin / 123456`（登录后请立即修改）

**版本升级**（自动解析最新版本 tag → 拉取 → 重启，容器启动时自动执行增量 SQL）：

```bash
cd blogloom
./upgrade.sh            # 自动解析并升级到最新版本
./upgrade.sh 1.0.1      # 升级到指定版本
# 等价：docker compose pull && docker compose up -d
```

> 使用具体版本号 tag（而非 `latest`），`docker ps` / `docker images` 可直接看到运行版本。

自定义配置：在同目录创建 `.env`（如 `WEB_PORT`、`MYSQL_ROOT_PASSWORD`、`TOKEN_SECRET`、`BLOG_API`、`IMAGE_TAG`）后执行 `docker compose up -d`。

数据落盘：所有运行时数据都绑定挂载到部署目录下的 `./data/`（`mysql`、`logs`、`upload`、`sql-local`），便于查看与备份。

完整说明见 [standalone/README.md](./standalone/README.md)。

## 升级原理

容器默认启动命令是 `bin/start.sh`，启动流程与本地 `bin/local/upgrate-sql.sh` 一致：

1. 等待 MySQL 可用；
2. 扫描挂载的 `sql/increment/<版本>/**/*.sql`（排除 `init.sql`），按文件名排序；
3. 与挂载的 `data/sql-local/` 历史执行记录比对，**只执行未记录过的 SQL**，成功后写入记录，可重复执行（幂等）；
4. 数据库已存在数据时，自动跳过全量初始化脚本（含 `CREATE DATABASE` 的基线文件）；
5. 启动后端服务。

因此 **重启应用容器即完成增量升级**（`docker compose restart blogloom` 或 `./scripts/upgrade.sh`），不需要额外的迁移容器或代码层迁移逻辑。

## 目录结构

`docker/` 目录（部署定义与脚本）：

```text
docker/
├── README.md
├── Dockerfile                  # 一体化镜像（后端 + 两个前端）
├── docker-compose.yml          # app + mysql
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
    ├── docker-compose.yml      # 自包含 compose（命名卷）
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

## 环境要求

- 已安装 Docker 与 Docker Compose v2
- 打包机可访问外网（构建阶段拉取 Maven / npm 依赖）

## 本地快捷打包 + 部署服务步骤

> 适用于在同一台机器上快速拉起完整环境（构建镜像 → 启动服务 → 初始化数据库），全部命令在 `docker/` 目录执行。

**步骤 0：前置检查**

```bash
docker -v && docker compose version
```

**步骤 1：一键打包（构建一体化镜像）**

```bash
cd docker
./scripts/package.sh 1.0.0
```

产物（`docker/dist/`）：

- `blogloom-1.0.0.tar`：镜像包
- `blogloom-deploy-1.0.0.tar.gz`：离线部署包

**步骤 2：一键部署（加载镜像并启动服务）**

```bash
./scripts/deploy.sh
```

脚本会自动：生成 `.env`（随机 MySQL 密码 / 密钥）→ 创建 `data/`、`conf/logs`、`conf/upload` → `docker load` 加载镜像 → 启动 `blogloom-mysql` 与 `blogloom-app`；应用启动时自动等待 MySQL 并执行增量 SQL（首次即全量初始化）。

**步骤 3：验证服务**

```bash
docker compose ps                                   # 两个容器均 Up，mysql 为 healthy
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:18080/          # 200
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:18080/cms       # 200
curl -s http://localhost:18080/site | head -c 120   # 返回 JSON
docker logs -f blogloom-app                         # 查看启动日志
```

**步骤 4：访问地址**

| 入口 | 地址 |
| --- | --- |
| 博客前台 | http://localhost:18080 |
| 管理后台 | http://localhost:18080/cms |
| 默认账号 | `admin` / `123456` |

**常用运维命令**

```bash
docker compose restart blogloom     # 重启应用（自动执行增量 SQL / 升级）
docker logs -f blogloom-app         # 应用日志
docker compose down                 # 停止服务（数据保留）
```

> 如需更换端口，修改 `docker/.env` 的 `WEB_PORT` / `MYSQL_PORT` 后重新 `./scripts/deploy.sh`。

## 快速开始

### 1. 一键打包（在开发机执行）

```bash
cd docker
./scripts/package.sh 1.0.0
```

产物位于 `docker/dist/`：

- `blogloom-1.0.0.tar`：镜像包
- `blogloom-deploy-1.0.0.tar.gz`：离线部署包（含 `docker/`、`conf/` 配置与 `sql/increment`）

### 2. 一键部署（在服务器执行）

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

`deploy.sh` 会自动完成：

1. 首次运行时从 `.env.example` 生成 `.env`，并写入随机 MySQL 密码与 `token.secretKey`；
2. 创建 `data/`、`conf/logs`、`conf/upload` 等持久化目录；
3. `docker load` 加载镜像；
4. `docker compose up -d` 启动两个容器；应用启动时自动等待 MySQL 并执行增量 SQL（首次即全量初始化）。

访问地址：

- 博客前台：`http://<服务器IP>:18080`
- 管理后台：`http://<服务器IP>:18080/cms`
- 默认账号：`admin / 123456`（登录后请立即修改）

> 首次部署务必检查 `docker/.env` 中的 `BLOG_API`、`BLOG_CMS`、`BLOG_VIEW`，改为真实对外访问地址，否则上传资源、邮件与通知中的链接会指向错误地址。

### 3. 一键升级（增量，在服务器执行）

```bash
cd docker
./scripts/upgrade.sh                       # 使用 .env 中的 IMAGE_TAG，从 dist/ 加载镜像
./scripts/upgrade.sh dist/blogloom-1.0.1.tar
./scripts/upgrade.sh dist/blogloom-1.0.1.tar --dry-run   # 仅预览待执行 SQL
```

升级流程：

1. 加载新版本镜像；
2. 确保 MySQL 运行；
3. 使用新镜像重启应用容器，启动脚本按 `data/sql-local` 历史记录执行未执行过的增量 SQL；
4. 服务就绪。

> 仅更新 `sql/increment/` 下的增量 SQL 时，也可直接 `docker compose restart blogloom` 触发升级。
> 升级前建议备份数据库：`docker exec blogloom-mysql mysqldump -uroot -p blogloom > backup.sql`

## 推送镜像到 Docker Hub

仅需推送 BlogLoom 应用镜像（MySQL 使用官方 `mysql:8.0`，无需推送）。

**步骤 1：登录 Docker Hub**

```bash
docker login -u codercl
```

**步骤 2：构建本地镜像（若尚未构建）**

```bash
cd docker
./scripts/package.sh 1.0.0
```

**步骤 3：一键推送**

```bash
./scripts/push.sh            # 推送 codercl/blogloom:<.env 的 IMAGE_TAG> 与 :latest
./scripts/push.sh 1.0.1      # 指定 tag
./scripts/push.sh --no-latest
```

推送脚本按 `.env` 的 `IMAGE_TAG` 打标签，仓库用户取 `.env` 的 `DOCKERHUB_USER`（默认 `codercl`）：

```text
codercl/blogloom:<IMAGE_TAG>
codercl/blogloom:latest
```

> 由于应用镜像基于 `mysql:8.0`，基础层在 Hub 已存在，推送时会自动复用（`Mounted from library/mysql`），只有 JRE、jar、前端等自定义层需要上传。

**步骤 4：从 Docker Hub 部署（可选）**

在部署端的 `.env` 中设置：

```bash
IMAGE_REPO=codercl/blogloom
IMAGE_TAG=1.0.0
```

然后执行 `./scripts/deploy.sh`：本地无该镜像时会由 `docker compose` 自动从 Docker Hub 拉取并启动。

## 配置说明

### `.env`（docker/.env）

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

### `conf/application.properties`

仓库根目录的 `conf/application.properties` 会挂载到容器 `/opt/blogloom/conf/application.properties`，支持环境变量占位符（如 `${DB_HOST:mysql}`）。邮件、Telegram 通知等按需填写。

## 数据持久化与备份

| 宿主目录 | 容器目录 | 内容 |
| --- | --- | --- |
| `data/mysql` | `/var/lib/mysql` | MySQL 数据 |
| `conf/logs` | `/opt/blogloom/conf/logs` | 运行日志 |
| `conf/upload` | `/opt/blogloom/conf/upload` | 上传资源 |
| `data/sql-local` | `/opt/blogloom/sql/local` | 已执行 SQL 记录 |

升级、重启不会丢失以上数据。备份时重点备份 `data/mysql` 与 `conf/upload`。

## 常见问题

- **端口被占用**：修改 `docker/.env` 中 `WEB_PORT` / `MYSQL_PORT` 后重新 `./scripts/deploy.sh`。
- **前端刷新 404**：后端已内置 SPA history 路由回退，无需额外配置 Nginx。
- **查看启动日志**：`docker logs -f blogloom-app`；数据库日志：`docker logs -f blogloom-mysql`。
- **单独预览/执行增量 SQL**：`docker compose run --rm -T --no-deps --entrypoint /opt/blogloom/bin/upgrade-sql.sh blogloom [--dry-run]`。
- **重新初始化数据库**：停止服务后删除 `data/mysql` 与 `data/sql-local`，再执行 `./scripts/deploy.sh`（会清空全部数据）。
- **升级到镜像仓库**：当前为本地 tar 流程；后续可将 `package.sh` 的 `docker save` 改为 `docker push`，并在 `docker-compose.yml` 中改用仓库镜像地址。

## 与本地脚本的一致性

容器内 `docker/container/bin/upgrade-sql.sh` 与仓库 `bin/local/upgrate-sql.sh` 规则一致：按文件名排序、排除 `init.sql`、扁平记录目录、成功后归档。区别仅是容器内通过环境变量（`DB_HOST`、`DB_USER` 等）读取数据库配置，并额外提供「存量库跳过全量初始化」的保护。
