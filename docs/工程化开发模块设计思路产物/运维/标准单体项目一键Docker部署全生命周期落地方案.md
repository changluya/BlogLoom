# 标准单体项目（前后端）一键 Docker 命令部署的全生命周期落地方案

> 模块：运维 / 部署
> 适用：单体架构、前后端分离（如 Spring Boot + Vue/React + MySQL）的项目
> 目标：给出从开发、打包、分发、部署、升级、回滚、备份到卸载的**全生命周期**一键化落地方案与可复用模板

---

## 1. 文档定位

本文是一套**方法论 + 模板**，用于把任意「单体后端 + 前端 SPA + 关系型数据库」项目，落地为：

- 一个镜像同时包含后端与前端；
- 运行时只需 **两个容器**（应用 + 数据库）；
- 一条命令完成打包 / 部署 / 升级；
- 数据库变更随容器启动自动、幂等地执行；
- 可分发给最终用户（Docker Hub / 私有仓库 / 离线 tar）。

BlogLoom 是本文的一个完整参考实现（见第 12 节）。

## 2. 核心目标与原则

| 目标 | 说明 |
| --- | --- |
| 开箱即用 | 用户无需 JDK/Node/Maven，只要有 Docker |
| 少即是多 | 尽量 2 个容器（应用 + 数据库），不引入额外 Web 服务器/缓存/迁移容器 |
| 一键化 | 打包、部署、升级各一条命令 |
| 幂等升级 | 数据库脚本按记录执行，重复执行安全 |
| 数据可迁移 | 运行时数据落在部署目录，随目录整体备份/迁移 |
| 可分发 | 镜像可推送仓库，也可离线 tar |
| 可回滚 | 镜像按版本 tag，回滚 = 切回旧 tag |

**贯穿原则：约定优于配置。** 用固定的目录与文件名约定，换取脚本的通用与稳定。

## 3. 总体架构设计

### 3.1 两容器模型

```text
┌───────────────────────────────┐
│            app 容器            │
│  启动脚本: 迁移 DB → 启动应用   │
│  后端(8080) 同时托管前端静态资源 │
│   /       前端页面              │
│   /admin  API                  │
└──────────────┬────────────────┘
               │ 内网
        ┌──────▼──────┐
        │   db 容器    │
        │  MySQL/PG    │
        └─────────────┘
```

- 前端由**后端托管**，省去 Nginx 容器与跨域配置；
- 数据库用官方镜像，数据绑定挂载/命名卷持久化；
- 只对外暴露「Web 端口」（+ 可选数据库端口）。

### 3.2 一体化镜像 vs 分离镜像

| 方案 | 优点 | 缺点 | 建议 |
| --- | --- | --- | --- |
| 一体化（后端托管前端） | 容器少、无跨域、部署简单 | 前后端需一起发版 | **推荐** |
| 前后端分离镜像 + Nginx | 各自独立发版 | 容器多、配置复杂 | 大型/多前端团队 |

### 3.3 端口规划

- 使用**非默认端口**（如 Web 18080、DB 13306）避免与宿主常见服务冲突；
- 端口通过 `.env` 参数化，便于调整；
- 用户版只暴露 Web 端口，数据库仅内网。

## 4. 目录规范

### 4.1 仓库目录（约定）

```text
project/
├── backend/                 # 后端源码
├── frontend/                # 前端源码（可多个 SPA）
├── conf/                    # 外置配置 + 运行时目录
│   ├── application.properties   # 支持 ${ENV:default} 占位符
│   ├── logback-spring.xml
│   ├── logs/                    # 运行日志（持久化）
│   └── upload/                  # 上传资源（持久化）
├── sql/
│   └── increment/<version>/*.sql  # 增量 SQL（含全量初始化基线）
├── data/                    # 运行时数据（持久化）
│   ├── mysql/
│   └── sql-local/           # 已执行 SQL 记录
└── docker/                  # 部署定义与脚本
    ├── Dockerfile
    ├── docker-compose.yml
    ├── .env.example
    ├── scripts/             # 宿主机脚本
    │   ├── common.sh
    │   ├── package.sh
    │   ├── deploy.sh
    │   ├── upgrade.sh
    │   └── push.sh
    ├── container/           # 打进镜像的内容
    │   ├── bin/             # start.sh / upgrade-sql.sh
    │   └── db/init/         # 数据库初始化脚本
    └── standalone/          # 用户版一键部署
        ├── install.sh
        └── docker-compose.yml
```

### 4.2 容器内目录（conf / lib / bin 约定）

```text
/opt/app/
├── bin/       # 启动与迁移脚本
├── conf/      # 配置、日志、上传、前端静态资源
│   ├── static/app/    # 前端产物（映射到 /）
│   └── static/admin/  # 管理端产物（映射到 /admin）
├── lib/       # 可执行 jar / 二进制
└── sql/       # 增量 SQL 与执行记录
```

> 应用启动时以 `APP_HOME` 为工作目录，从而定位 `conf/`（如 Spring 的 `user.dir.conf`）。

## 5. 全生命周期流程

### 5.1 开发阶段

1. 外置配置化：把数据库地址、密钥、站点地址抽到 `conf/application.properties`，用 `${ENV:default}` 占位，使**同一份配置**既跑本地也跑容器；
2. 前端构建参数化：接口地址（`VITE_API_URL`）、静态资源前缀（`base`）用环境变量控制；
3. 统一静态资源路径：前端产物目录避免与后端上传路径（如 `/static/**`）冲突；
4. 约定 SQL 目录：`sql/increment/<version>/时间戳_v<版本>.sql`。

### 5.2 打包阶段

**多阶段 Dockerfile**（构建与运行分离，最终镜像不含构建工具）：

```text
阶段1 backend：maven 构建 jar
阶段2 frontend：node 构建静态产物（可多阶段对应多个前端）
阶段3 runtime：基础运行时 + JRE/运行时 + 拷贝产物
```

关键点：

- 构建上下文为**仓库根目录**，便于访问各模块与 `conf/`；
- 先拷贝依赖清单（pom.xml/package.json）预热依赖，利用层缓存；
- 内置默认配置与增量 SQL（用户版无需挂载）；
- 镜像 tag = 版本号，同时打 `latest`。

产物：

- `dist/app-<version>.tar`：镜像包（`docker save`）；
- `dist/app-deploy-<version>.tar.gz`：离线部署包（脚本 + 配置 + SQL + 镜像）。

### 5.3 分发阶段

| 方式 | 适用 | 命令 |
| --- | --- | --- |
| Docker Hub / 私有仓库 | 有网络、持续交付 | `docker tag && docker push` |
| 离线 tar | 内网/无外网 | `docker save` → 传输 → `docker load` |

推送脚本应支持：按配置的 tag 推送、同时更新 `latest`、复用基础层。

### 5.4 部署阶段（首次）

```text
deploy.sh
  ├─ 生成 .env（随机密码/密钥）
  ├─ 创建持久化目录
  ├─ 加载镜像（tar / 本地 / 仓库拉取）
  └─ docker compose up -d
       └─ app 启动脚本：等待 DB → 执行增量 SQL（首次=全量初始化）→ 启动应用
```

要点：用 `depends_on: service_healthy` + 启动脚本双重等待数据库；首次初始化与后续升级走**同一套 SQL 执行逻辑**。

### 5.5 升级阶段（增量、幂等）

```text
upgrade.sh
  ├─ 加载新版本镜像
  ├─ 重建 app 容器
  └─ 启动脚本执行未记录过的增量 SQL（按文件名排序，成功后写记录）
```

幂等规则：

1. 扫描 `sql/increment/**/*.sql`，排除全量基线（如 `init.sql`）；
2. 与执行记录目录比对，已执行的跳过；
3. 成功后写记录（文件或数据库表）；
4. 存量库自动跳过「含 CREATE DATABASE 的全量脚本」，避免误删。

> 只改 SQL 时，`docker compose restart app` 即可触发；发新版本则重建容器。

### 5.6 回滚阶段

- 镜像回滚：把 `IMAGE_TAG` 切回旧版本，`docker compose up -d` 重建；
- 数据库回滚：**DDL 无法自动回滚**，需依赖：
  - 升级前备份（`mysqldump`）；
  - 增量脚本尽量可逆（新增列/表优先，避免破坏性变更）；
  - 必要时提供 `sql/rollback/<version>/*.sql` 并手动执行。

### 5.7 备份与恢复

- 备份：打包部署目录（`data/` 关键）+ 定期 `mysqldump`；
- 恢复：恢复 `data/mysql`（或导入 dump）+ 恢复 `data/upload`；
- 记录目录 `data/sql-local` 一并备份，避免恢复后重复执行。

### 5.8 监控与日志

- 日志：应用输出到 `conf/logs`（绑定挂载），`docker logs` 兜底；
- 健康检查：DB 容器 healthcheck；应用可加 HTTP 健康接口；
- 资源：`docker stats` / cAdvisor；必要时为容器设置 CPU/内存配额。

### 5.9 卸载与清理

```bash
docker compose down            # 停止并删除容器（保留数据）
docker compose down -v         # 连同命名卷一起删除
rm -rf data conf/logs conf/upload   # 彻底清理（谨慎）
```

## 6. 关键设计决策与取舍

| 决策 | 选择 | 理由 |
| --- | --- | --- |
| 容器数量 | 2（应用+数据库） | 简单、够用、易维护 |
| 前端托管 | 后端托管 | 省 Nginx、免跨域 |
| 迁移时机 | 应用启动时 | 无需额外迁移容器；天然幂等 |
| 迁移记录 | 文件（sql-local） | 与本地脚本一致、直观；也可用 DB 表 |
| 运行时基础镜像 | 自带 DB 客户端的镜像 | 启动脚本能直接执行 SQL，免装依赖 |
| 数据落盘 | 绑定挂载到部署目录 | 可见、可备份、可迁移 |
| 端口 | 非默认端口 + 参数化 | 避免冲突，便于调整 |

## 7. 核心脚本清单与职责

| 脚本 | 位置 | 职责 |
| --- | --- | --- |
| `common.sh` | 宿主机 | 路径解析、.env 生成/加载、compose 封装 |
| `package.sh` | 宿主机 | 构建镜像 + 导出 tar/离线包 |
| `deploy.sh` | 宿主机 | 加载镜像 + 启动 |
| `upgrade.sh` | 宿主机 | 加载新镜像 + 重建（+ dry-run） |
| `push.sh` | 宿主机 | 推送镜像到仓库 |
| `start.sh` | 容器内 | 等待 DB → 迁移 → 启动应用 |
| `upgrade-sql.sh` | 容器内 | 增量 SQL 执行（幂等） |
| `install.sh` | 用户版 | 写 compose + 建数据目录 + 拉镜像 + 启动 |

## 8. 通用模板

### 8.1 Dockerfile 模板

```dockerfile
# 1. 后端构建
FROM maven:3.8-jdk-8 AS backend
WORKDIR /workspace
COPY backend/pom.xml ./backend/pom.xml
RUN cd backend && mvn -B -q dependency:resolve
COPY backend/src ./backend/src
RUN cd backend && mvn -B -q clean package -Dmaven.test.skip=true

# 2. 前端构建（多前端则重复此阶段）
FROM node:18 AS frontend
WORKDIR /workspace/frontend
COPY frontend/package.json ./
RUN npm install --no-audit --no-fund
COPY frontend/ ./
RUN VITE_API_URL=/ npm run build

# 3. 运行时
FROM eclipse-temurin:8-jre-focal AS jre
FROM mysql:8.0 AS runtime
COPY --from=jre /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk PATH=/opt/java/openjdk/bin:$PATH \
    TZ=Asia/Shanghai APP_HOME=/opt/app
RUN mkdir -p ${APP_HOME}/{bin,conf/logs,conf/upload,lib,sql/increment,sql/local}
COPY docker/container/bin/ ${APP_HOME}/bin/
COPY conf/application.properties conf/logback-spring.xml ${APP_HOME}/conf/
COPY sql/increment/ ${APP_HOME}/sql/increment/
COPY --from=backend /workspace/backend/target/app.jar ${APP_HOME}/lib/app.jar
COPY --from=frontend /workspace/frontend/dist/ ${APP_HOME}/conf/static/app/
RUN chmod +x ${APP_HOME}/bin/*.sh
WORKDIR ${APP_HOME}
EXPOSE 8080
ENTRYPOINT ["/opt/app/bin/start.sh"]
```

### 8.2 docker-compose.yml 模板（仓库版）

```yaml
name: myapp
services:
  mysql:
    image: mysql:8.0
    restart: unless-stopped
    command: ["--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci"]
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE:-myapp}
      MYSQL_USER: appuser
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      TZ: ${TZ:-Asia/Shanghai}
    ports: ["${MYSQL_PORT:-13306}:3306"]
    volumes:
      - ./data/mysql:/var/lib/mysql
      - ./container/db/init:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD-SHELL", "mysql -h 127.0.0.1 -uroot -p$$MYSQL_ROOT_PASSWORD -e 'SELECT 1' >/dev/null 2>&1"]
      interval: 5s
      timeout: 5s
      retries: 30
  app:
    image: ${IMAGE_REPO:-myapp}:${IMAGE_TAG:-1.0.0}
    restart: unless-stopped
    depends_on:
      mysql: { condition: service_healthy }
    environment:
      DB_HOST: mysql
      DB_PORT: "3306"
      DB_USER: appuser
      DB_PASSWORD: ${MYSQL_PASSWORD}
      DB_NAME: ${MYSQL_DATABASE:-myapp}
      TOKEN_SECRET: ${TOKEN_SECRET}
      TZ: ${TZ:-Asia/Shanghai}
      JAVA_OPTS: ${JAVA_OPTS:--Xms256m -Xmx512m}
    ports: ["${WEB_PORT:-18080}:8080"]
    volumes:
      - ./conf/application.properties:/opt/app/conf/application.properties:ro
      - ./conf/logs:/opt/app/conf/logs
      - ./conf/upload:/opt/app/conf/upload
      - ./sql/increment:/opt/app/sql/increment:ro
      - ./data/sql-local:/opt/app/sql/local
```

### 8.3 start.sh 模板

```bash
#!/usr/bin/env bash
set -euo pipefail
APP_HOME="${APP_HOME:-/opt/app}"; cd "$APP_HOME"
: "${DB_HOST:=mysql}" ; : "${DB_PORT:=3306}" ; : "${DB_USER:=appuser}"
: "${DB_PASSWORD:=}" ; : "${DB_NAME:=myapp}"

echo "[start] 等待数据库 ${DB_HOST}:${DB_PORT} ..."
for i in $(seq 1 60); do
  MYSQL_PWD="$DB_PASSWORD" mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" \
    --connect-timeout=3 -e 'SELECT 1' >/dev/null 2>&1 && break
  [ "$i" = 60 ] && { echo "[start][ERROR] 数据库未就绪"; exit 1; }
  sleep 2
done

echo "[start] 执行增量 SQL ..."
"$APP_HOME/bin/upgrade-sql.sh"

echo "[start] 启动应用 ..."
exec java ${JAVA_OPTS:-} -jar "$APP_HOME/lib/app.jar"
```

### 8.4 upgrade-sql.sh 模板（要点）

```bash
#!/usr/bin/env bash
set -uo pipefail
PENDING_DIR="${PENDING_SQL_DIR:-$APP_HOME/sql/increment}"
RECORD_DIR="${EXECUTED_SQL_DIR:-$APP_HOME/sql/local}"
# 1) 扫描 *.sql（排除 init.sql），按文件名排序
# 2) 与 RECORD_DIR 比对，已执行跳过
# 3) 存量库（information_schema 有表）跳过含 CREATE DATABASE 的全量脚本
# 4) 逐个 mysql --database=<db> < <sql>，成功后 cp 到 RECORD_DIR
```

### 8.5 install.sh 模板（用户版）

```bash
#!/usr/bin/env bash
set -euo pipefail
WEB_PORT="${WEB_PORT:-18080}"
command -v docker >/dev/null || { echo "请先安装 Docker"; exit 1; }
docker compose version >/dev/null || { echo "请先安装 Docker Compose v2"; exit 1; }
[ -f docker-compose.yml ] || cat > docker-compose.yml <<'YAML'
# ... 自包含 compose（image: <user>/app:<tag>，绑定挂载 ./data）...
YAML
mkdir -p data/mysql data/logs data/upload data/sql-local
docker compose up -d
echo "访问 http://<服务器IP>:${WEB_PORT}"
```

## 9. 最佳实践与常见坑

- **数据库就绪**：必须用 healthcheck + 启动脚本双重等待，否则首启易失败；
- **迁移幂等**：记录目录要持久化，且按「文件名」去重；不要用「是否报错」判断；
- **DDL 不可回滚**：升级前先备份；增量脚本尽量只做新增；
- **静态资源冲突**：前端产物目录不要和后端上传路径重名（如都叫 `/static`）；
- **SPA 刷新 404**：history 路由需后端回退到 `index.html`（只对文档导航回退，别误伤接口）；
- **同源路径**：前端接口 base 与后端前缀要对齐，避免子路径部署（如 `/admin`）冲突；
- **环境变量优先**：compose 里 shell 环境变量优先于 `.env`，可据此做临时覆盖；
- **镜像体积**：多阶段构建 + 复用基础层；推送时利用 Hub 层复用；
- **安全**：默认密码/密钥首次部署后必须修改；数据库端口尽量不对外。

## 10. 落地检查清单（Checklist）

- [ ] 外置配置支持 `${ENV:default}`，本地与容器共用
- [ ] 前端构建参数化（API base、资源前缀）
- [ ] 多阶段 Dockerfile，构建上下文为仓库根
- [ ] 镜像内置默认配置与增量 SQL
- [ ] start.sh：等待 DB → 迁移 → 启动
- [ ] upgrade-sql.sh：排序、去重、幂等、存量库保护
- [ ] compose：healthcheck + depends_on + 绑定挂载
- [ ] 一键脚本：package / deploy / upgrade / push / install
- [ ] .env 参数化（端口、镜像、密码、站点地址）
- [ ] 数据落在部署目录，备份/恢复路径明确
- [ ] 首启、增量升级、幂等重启、Hub 拉取部署均已验证

## 11. 生命周期命令速查

```bash
# 打包（开发机）
./scripts/package.sh 1.0.0

# 推送镜像
docker login -u <user>
./scripts/push.sh

# 部署（仓库版）
./scripts/deploy.sh
IMAGE_REPO=<user>/app ./scripts/deploy.sh     # 直接拉仓库镜像

# 升级
./scripts/upgrade.sh                          # 重建容器并执行增量 SQL
./scripts/upgrade.sh --dry-run                # 预览待执行 SQL

# 用户版一键部署
mkdir app && cd app && curl -fsSL <repo>/docker/standalone/install.sh | bash

# 运维
docker compose ps / logs -f app / restart app
docker compose pull && docker compose up -d    # 升级到最新
docker compose down                            # 停止（保留数据）
```

## 12. 参考实现（BlogLoom）

BlogLoom 完整落地了本方案，可作为模板对照：

- 设计文档：`docs/工程化开发模块设计思路产物/运维/BlogLoom Docker 一键部署设计方案.md`
- 部署目录：`docker/`（Dockerfile、docker-compose.yml、scripts/、container/、standalone/）
- 用户说明：`docker/README.md`、`docker/standalone/README.md`
- 增量 SQL：`sql/increment/<版本>/*.sql`；本地脚本：`bin/local/upgrate-sql.sh`

关键落地结果：

- 2 个容器（`blogloom-app` + `blogloom-mysql`），Web 18080 / MySQL 13306；
- 后端托管两个前端（`/` 与 `/cms`），`SpaForwardConfig` 处理 history 回退；
- 镜像基于 `mysql:8.0` + JRE，启动脚本直接执行增量 SQL；
- 一键打包 / 部署 / 升级 / 推送 Docker Hub / 用户版一键部署全部实测通过。
