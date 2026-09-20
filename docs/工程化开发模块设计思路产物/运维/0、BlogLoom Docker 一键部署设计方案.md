# BlogLoom Docker 一键部署设计方案

> 模块：运维 / 部署
> 范围：`docker/`、`conf/`、`sql/`、后端同源托管、前端构建适配、Docker Hub 分发
> 目标：以两个容器（应用 + MySQL）完成一键打包、一键部署、一键增量升级，并支持镜像分发与用户版一键部署

---

## 1. 背景

BlogLoom 原本依赖 JDK、Maven、Node、MySQL 等一整套本地环境，部署与升级成本较高：

- 前后端分离：博客前台（`blog-view-ui`）、管理后台（`blog-cms-ui`）与后端（`blog-backend`）需分别构建与托管；
- 环境依赖多：后端需 JDK8、Maven，前端需 Node，数据库需 MySQL；
- 升级无统一机制：数据库增量 SQL 依赖人工执行，容易出现漏执行、重复执行；
- 历史脚本 `bin/local/upgrate-sql.sh` 只能在本机执行，无法覆盖容器化场景；
- 分发成本高：用户需要克隆仓库、安装环境才能部署。

因此需要一个「开箱即用、可离线、可增量升级、可分发给最终用户」的容器化部署方案，并尽量复用已有的增量 SQL 规则与目录约定。

## 2. 目标功能

1. **一键打包**：一条命令构建包含后端 + 两个前端的镜像，并导出可离线传输的产物；
2. **一键部署（仓库版）**：一条命令在服务器启动服务，首次自动完成数据库全量初始化；
3. **一键升级（增量）**：一条命令加载新版本并完成数据库增量升级，且可重复执行（幂等）；
4. **只有两个容器**：应用服务（自带前端页面） + MySQL，不引入 Nginx、Redis、额外迁移容器；
5. **两个固定非默认端口**：Web `18080`、MySQL `13306`，避免与常见端口冲突；
6. **挂载目录收口在部署目录/仓库根目录**：便于查找、备份与迁移；
7. **升级逻辑由启动 bash 脚本完成**：不引入代码层的迁移逻辑，保持与本地脚本一致；
8. **镜像分发**：一键推送到 Docker Hub，供部署端拉取；
9. **用户版一键部署**：最终用户只需 Docker，一条命令即可拉起完整环境，无需克隆仓库或安装构建环境。

## 3. 核心设计思路

### 3.1 一体化应用镜像（后端托管前端）

- 一个镜像同时打包：后端 jar、博客前台产物、管理后台产物、增量 SQL；
- 前端不单独用 Nginx 托管，而是由后端直接提供静态资源：
  - `conf/static/view/` → 映射到根路径 `/`（博客前台）；
  - `conf/static/cms/` → 映射到 `/cms`（管理后台）；
- 后端通过 `SpaForwardConfig` 为 history 路由做回退，刷新任意前端路由都能返回对应 `index.html`。

### 3.2 运行时基础镜像复用 `mysql:8.0`

应用镜像的运行时基础不是纯 JRE 镜像，而是 `mysql:8.0`，再把 JRE 从 `eclipse-temurin` 拷贝进来：

```dockerfile
FROM eclipse-temurin:8-jre-focal AS jre
FROM mysql:8.0 AS runtime
COPY --from=jre /opt/java/openjdk /opt/java/openjdk
```

这样做的原因：

- `mysql:8.0` 自带 `mysql` 客户端与常用 shell 工具（`find`、`sort`、`grep`、`sed` 等）；
- 容器的默认启动脚本就能直接执行增量 SQL，**无需 apt/apk 安装依赖**，构建稳定且可离线；
- 避免引入「一次性迁移容器」或「代码层迁移逻辑」，严格满足「两个容器」的诉求。

### 3.3 增量升级由启动脚本完成

容器入口固定为 `bin/start.sh`，顺序执行：

```text
等待 MySQL → 执行 bin/upgrade-sql.sh（增量 SQL）→ 启动 java -jar
```

因此：

- 首次启动 = 全量初始化；
- 每次重启（`docker compose restart blogloom`）= 增量升级检查；
- 升级新版本 = 加载新镜像并重建容器，启动时自动完成增量 SQL。

### 3.4 增量 SQL 记录与幂等

沿用本地 `bin/local/upgrate-sql.sh` 的规则：

- 待执行目录：`sql/increment/<版本>/**/*.sql`（排除 `init.sql`）；
- 执行记录目录：`data/sql-local/<SQL 文件名>`（扁平结构，按文件名去重）；
- 按文件名（时间戳前缀）排序执行；
- 执行成功后才写入记录，因此可重复执行（幂等）；
- **存量库保护**：数据库已有数据时，自动跳过包含 `CREATE DATABASE` 的全量初始化脚本，避免误 `DROP TABLE`。

### 3.5 挂载目录收口

两种部署模式都遵循「数据落在部署目录下」的原则：

**仓库版**（挂载自仓库根目录）：

| 宿主目录（仓库根） | 容器目录 | 用途 | 读写 |
| --- | --- | --- | --- |
| `conf/application.properties` | `/opt/blogloom/conf/application.properties` | 应用配置 | 只读 |
| `conf/logback-spring.xml` | `/opt/blogloom/conf/logback-spring.xml` | 日志配置 | 只读 |
| `conf/logs` | `/opt/blogloom/conf/logs` | 运行日志 | 读写 |
| `conf/upload` | `/opt/blogloom/conf/upload` | 上传资源 | 读写 |
| `sql/increment` | `/opt/blogloom/sql/increment` | 增量 SQL | 只读 |
| `data/sql-local` | `/opt/blogloom/sql/local` | 已执行 SQL 记录 | 读写 |
| `data/mysql` | `/var/lib/mysql` | MySQL 数据 | 读写 |

**用户版**（挂载自部署目录 `./data/`）：

| 宿主目录（部署目录） | 容器目录 |
| --- | --- |
| `./data/mysql` | `/var/lib/mysql` |
| `./data/logs` | `/opt/blogloom/conf/logs` |
| `./data/upload` | `/opt/blogloom/conf/upload` |
| `./data/sql-local` | `/opt/blogloom/sql/local` |

配置支持环境变量占位符（如 `${DB_HOST:localhost}`），同一份 `conf/application.properties` 既可用于本地，也可用于容器。

### 3.6 镜像分发：Docker Hub

- 应用镜像发布到 Docker Hub：`<DOCKERHUB_USER>/blogloom:<IMAGE_TAG>` 与 `:latest`；
- MySQL 使用官方 `mysql:8.0`，无需推送；
- 推送 tag 取自配置参数 `.env` 的 `IMAGE_TAG`，仓库用户取自 `DOCKERHUB_USER`；
- 由于应用镜像基于 `mysql:8.0`，基础层在 Hub 已存在，推送时自动复用（`Mounted from library/mysql`），仅上传自定义层。

### 3.7 用户版一键部署（standalone）

- 镜像内置增量 SQL，用户无需挂载 SQL 文件；
- 自包含 `install.sh`：写入 `docker-compose.yml` → 创建 `./data/` → 拉取镜像 → 启动；
- 只暴露一个 Web 端口，MySQL 仅容器内网；
- 数据全部绑定挂载到部署目录 `./data/`，便于备份与迁移。

## 4. 核心流程步骤细节

### 4.1 一键打包流程（`docker/scripts/package.sh`）

```text
package.sh [版本号]
  ├─ 读取版本号（参数 / .env 的 IMAGE_TAG / 默认 1.0.0）
  ├─ docker build -f docker/Dockerfile -t blogloom:<版本> .   （上下文为仓库根目录）
  │    ├─ 阶段1 backend-build：mvn clean package -Dmaven.test.skip=true → blog-backend.jar
  │    ├─ 阶段2 view-build   ：VITE_API_URL=/ VITE_ASSETS_DIR=assets npm run build
  │    ├─ 阶段3 cms-build    ：VITE_API_URL=/admin/ npm run build -- --base=/cms/
  │    └─ 阶段4 runtime      ：mysql:8.0 + JRE，写入 bin/conf/lib/sql，产出镜像
  ├─ docker save → docker/dist/blogloom-<版本>.tar
  └─ 生成离线部署包 → docker/dist/blogloom-deploy-<版本>.tar.gz
       （含 docker/、conf/application.properties、conf/logback-spring.xml、sql/increment）
```

### 4.2 一键部署流程（仓库版，`docker/scripts/deploy.sh`）

```text
deploy.sh [镜像 tar 可选]
  ├─ 允许环境变量覆盖 IMAGE_REPO / IMAGE_TAG（用于直接从 Docker Hub 拉取）
  ├─ ensure_env      ：.env 不存在则由 .env.example 生成，随机写入 MySQL 密码与 TOKEN_SECRET
  ├─ ensure_dirs     ：创建 data/mysql、data/sql-local、conf/logs、conf/upload、dist
  ├─ 镜像处理        ：有 tar → docker load；本地有镜像 → 直接用；IMAGE_REPO 含 "/" → 由 compose 拉取
  └─ docker compose up -d
       ├─ mysql 启动 → healthcheck 通过
       └─ blogloom 启动（depends_on: service_healthy）
            └─ bin/start.sh：
                 1) 轮询等待 MySQL（mysql 客户端 SELECT 1）
                 2) bin/upgrade-sql.sh 执行增量 SQL（首次即全量初始化）
                 3) exec java -jar lib/blog-backend.jar
```

### 4.3 启动时增量升级流程（`docker/container/bin/start.sh` + `upgrade-sql.sh`）

```text
start.sh
  ├─ 读取 DB_HOST/DB_PORT/DB_USER/DB_PASSWORD/DB_NAME（来自 compose 环境变量）
  ├─ 等待 MySQL 就绪（最多 60 次 × 2s）
  ├─ 调用 bin/upgrade-sql.sh
  └─ exec java ${JAVA_OPTS} -jar /opt/blogloom/lib/blog-backend.jar
```

`upgrade-sql.sh` 核心步骤：

```text
1) 校验 DB_NAME；确保 sql/local 与 .work 目录存在
2) 扫描 sql/increment/**/*.sql（排除 init.sql），按路径排序，得到 PENDING_FILES
3) 重名检测（扁平记录目录要求文件名唯一）
4) 查询 information_schema 判断目标库是否已有表（存量库判断）
5) 逐个比对 sql/local 记录：
     - 已记录            → 跳过
     - 未记录 + 全量初始化 + 库已有表 → 记录并跳过（存量库保护）
     - 其余              → 加入待执行列表
6) 无待执行 → 输出「数据库已是最新版本」并退出
7) 有则逐个执行：
     mysql --database=<db> < <sql> 成功 → 复制一份到 sql/local 作为执行记录
8) 输出成功数量与记录目录
```

### 4.4 一键升级流程（`docker/scripts/upgrade.sh`）

```text
upgrade.sh [镜像 tar] [--dry-run]
  ├─ ensure_env / load_env / ensure_dirs
  ├─ 加载新版本镜像（docker load / 本地 / Hub 拉取）
  ├─ docker compose up -d --wait mysql
  ├─ --dry-run 模式：
  │    docker compose run --rm -T --no-deps \
  │      --entrypoint /opt/blogloom/bin/upgrade-sql.sh blogloom --dry-run
  │    （仅预览待执行 SQL，不改库、不重启）
  └─ 正式升级：
       docker compose up -d --force-recreate blogloom
         └─ 容器重建 → start.sh → upgrade-sql.sh 执行未记录增量 SQL → 启动服务
```

> 仅新增/更新 `sql/increment/` 下的 SQL 时，可直接 `docker compose restart blogloom` 触发升级，效果等价。

### 4.5 推送镜像到 Docker Hub（`docker/scripts/push.sh`）

```text
push.sh [版本号] [--no-latest]
  ├─ ensure_env / load_env（读取 IMAGE_TAG、DOCKERHUB_USER）
  ├─ 校验本地镜像 blogloom:<IMAGE_TAG> 存在（否则提示先 package.sh）
  ├─ docker tag blogloom:<tag>  <user>/blogloom:<tag>
  ├─ docker push               <user>/blogloom:<tag>
  └─ docker tag/push           <user>/blogloom:latest（可 --no-latest 跳过）
```

前置：`docker login -u <DOCKERHUB_USER>`。

### 4.6 用户版一键部署流程（`docker/standalone/install.sh`）

```text
用户操作：
  mkdir blogloom && cd blogloom
  curl -fsSL <repo>/docker/standalone/install.sh | bash

install.sh：
  ├─ 校验 docker / docker compose v2
  ├─ 写入自包含 docker-compose.yml（若不存在）
  ├─ mkdir -p data/{mysql,logs,upload,sql-local}
  └─ docker compose up -d
       ├─ 拉取 codercl/blogloom:<IMAGE_TAG>（默认 latest）
       ├─ mysql 健康后启动 blogloom
       └─ 应用内置 SQL 自动完成初始化
```

### 4.7 前后端同源托管流程

前端构建期适配：

- 博客前台：`VITE_API_URL=/`（接口走根路径）、`VITE_ASSETS_DIR=assets`（产物目录避开后台 `/static/**` 上传资源）；
- 管理后台：`VITE_API_URL=/admin/`（接口走 `/admin`）、`--base=/cms/`（挂载在 `/cms`）。

后端运行期适配：

- `BlogApiApplication` 将静态资源目录设置为 `file:${user.dir.conf}/static/view/`（根路径）与 `file:${user.dir.conf}/static/`（子路径）；
- `SpaForwardConfig` 仅对「GET + `Accept: text/html` + 无扩展名 + 非保留前缀」的请求回退到 `index.html` / `cms/index.html`，避免误伤接口与静态资源；
- 管理后台登录跳转使用 `import.meta.env.BASE_URL`，兼容 `/cms/` 子路径。

## 5. 端口与访问入口

| 入口 | 地址 | 说明 |
| --- | --- | --- |
| 博客前台 | `http://<IP>:18080/` | `conf/static/view` |
| 管理后台 | `http://<IP>:18080/cms` | `conf/static/cms` |
| 管理端 API | `http://<IP>:18080/admin/**` | Spring Security + JWT |
| 上传资源 | `http://<IP>:18080/static/**` | 映射 `conf/upload` |
| MySQL | `<IP>:13306`（仓库版）/ 不对外（用户版） | 应用内部走 `mysql:3306` |

## 6. 核心文件与代码路径

### 6.1 Docker 部署定义与脚本

| 文件 | 职责 |
| --- | --- |
| `docker/Dockerfile` | 一体化镜像：后端 + 两个前端 + JRE + 内置 SQL（基于 mysql:8.0） |
| `docker/docker-compose.yml` | 仓库版：app + mysql，挂载仓库 `conf/`、`sql/`、`data/` |
| `docker/.env.example` | 部署配置模板（端口、镜像仓库、密码、站点地址等） |
| `docker/README.md` | 部署与升级使用说明（含用户快速部署章节） |
| `docker/scripts/common.sh` | 公共函数：环境加载、随机密钥、镜像加载、compose 封装 |
| `docker/scripts/package.sh` | 一键打包（构建镜像 + 导出 tar + 离线部署包） |
| `docker/scripts/deploy.sh` | 一键部署（支持 Hub 拉取） |
| `docker/scripts/upgrade.sh` | 一键升级（含 `--dry-run`） |
| `docker/scripts/push.sh` | 一键推送镜像到 Docker Hub |
| `docker/container/bin/start.sh` | 容器入口：等待 MySQL → 增量 SQL → 启动 Java |
| `docker/container/bin/upgrade-sql.sh` | 容器内增量 SQL 执行（与本地脚本同规则） |
| `docker/container/mysql/init/00-grant.sql` | MySQL 首次初始化补授应用账号权限 |
| `docker/standalone/install.sh` | 用户版一键部署脚本（自包含） |
| `docker/standalone/docker-compose.yml` | 用户版 compose（Hub 镜像 + 绑定挂载 `./data`） |
| `docker/standalone/README.md` | 用户版部署说明 |

### 6.2 后端同源托管相关

| 文件 | 职责 |
| --- | --- |
| `blog-backend/src/main/java/com/changlu/blogloom/BlogApiApplication.java` | 静态资源目录 `static/view` + `static` 配置 |
| `blog-backend/src/main/java/com/changlu/blogloom/config/SpaForwardConfig.java` | SPA history 路由回退过滤器 |
| `blog-backend/src/main/java/com/changlu/blogloom/config/WebConfig.java` | 上传资源 `/static/**` 映射（保持兼容） |

### 6.3 前端构建适配相关

| 文件 | 职责 |
| --- | --- |
| `blog-view-ui/vite.config.js` | `assetsDir` 支持 `VITE_ASSETS_DIR` 覆盖 |
| `blog-cms-ui/src/util/request.js` | 登录跳转使用 `import.meta.env.BASE_URL`，兼容 `/cms/` |

### 6.4 配置与 SQL

| 文件 | 职责 |
| --- | --- |
| `conf/application.properties` | 外置配置，支持 `${ENV:default}` 占位符 |
| `conf/application.properties.conf` | 配置模板 |
| `conf/logback-spring.xml` | 日志配置（输出到 `conf/logs`） |
| `sql/increment/<版本>/**/*.sql` | 增量 SQL（含全量初始化基线，已内置进镜像） |
| `data/sql-local/` | 已执行 SQL 记录（运行时生成） |
| `bin/local/upgrate-sql.sh` | 本地增量升级脚本（容器脚本的同规则来源） |
| `.dockerignore` / `.gitignore` | 构建上下文裁剪 / 运行时数据忽略 |

## 7. 部署模式对比

| 维度 | 仓库版 | 用户版（standalone） |
| --- | --- | --- |
| 适用对象 | 二次开发者 / 运维 | 只想快速跑起来的最终用户 |
| 前置条件 | Docker + 仓库代码 | 仅 Docker |
| 镜像来源 | 本地构建 / 离线 tar / Docker Hub | Docker Hub |
| 增量 SQL | 挂载 `sql/increment`（可热更） | 内置镜像（随版本发布） |
| 配置 | 仓库 `conf/application.properties` | compose 内置 + 可选 `.env` |
| 数据目录 | 仓库根 `data/`、`conf/logs`、`conf/upload` | 部署目录 `./data/` |
| 端口 | Web 18080 + MySQL 13306 | 仅 Web 18080 |
| 升级 | `upgrade.sh` / 替换 SQL + 重启 | `docker compose pull && up -d` |

## 8. 验证记录

| 场景 | 操作 | 结果 |
| --- | --- | --- |
| 一键打包 | `./scripts/package.sh 1.0.0` | 生成镜像 tar 与离线部署包 |
| 首次部署 | `./scripts/deploy.sh` | 2 个容器启动，自动执行 10 条 SQL 完成初始化，`admin/123456` 登录成功 |
| 增量升级 | 新增 1 条 SQL 后 `./scripts/upgrade.sh` | 启动日志「跳过 10 条、执行 1 条」，记录数 10→11 |
| 结构变更 | 新增 `ALTER TABLE ... ADD COLUMN` | 重启后执行成功，列已存在 |
| 幂等重启 | 再次重启容器 | 输出「没有需要执行的增量 SQL」，无重复执行 |
| 重启即升级 | 新增 SQL 后 `docker compose restart blogloom` | 自动执行新增 SQL，无需额外命令 |
| 推送镜像 | `./scripts/push.sh` | 推送 `codercl/blogloom:1.0.0` 与 `:latest`，基础层复用 `library/mysql` |
| Hub 拉取部署 | 删除本地镜像 + `IMAGE_REPO=codercl/blogloom ./scripts/deploy.sh` | 从 Docker Hub 拉取并启动，`RepoDigest` 与推送一致 |
| 用户版一键部署 | `/Users/edy/Downloads/blogloom` 执行 `bash install.sh` | 生成 compose、拉镜像、10 条 SQL 初始化，数据落在 `./data/`，接口 200 |
| 页面与接口 | `/`、`/cms`、`/cms/dashboard`、`/about`、`/site`、`/admin/login` | 均正常返回 |

## 9. 后续演进

- 支持多架构镜像构建（`buildx`，amd64/arm64）；
- 提供 GitHub Actions 自动构建并推送镜像；
- 精简运行时镜像（仅拷贝 mysql 客户端到精简 JRE 基础镜像）；
- 增加数据库备份/恢复一键脚本与定时备份；
- 引入容器健康检查与滚动更新策略；
- 提供 Helm / 一键脚本的多实例编排能力。
