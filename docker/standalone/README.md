# BlogLoom 一键部署（用户版）

只想快速把博客跑起来？**只需 Docker**，无需 JDK / Node / Maven，也无需克隆仓库。

## 三步部署

```bash
# 1. 创建并进入一个空文件夹
mkdir blogloom && cd blogloom

# 2. 一键部署（自动拉取镜像并启动）
curl -fsSL https://raw.githubusercontent.com/changluya/BlogLoom/feat_1.0.x_4/docker/standalone/install.sh | bash

# 3. 浏览器访问
#    博客前台 http://<服务器IP>:18080
#    管理后台 http://<服务器IP>:18080/cms
#    默认账号 admin / 123456
```

脚本会自动写入 `docker-compose.yml`、从 Docker Hub 拉取 `codercl/blogloom` 镜像并启动 `blogloom-app` + `blogloom-mysql` 两个容器，应用启动时自动完成数据库初始化。

## 前置条件

- Linux 服务器，已安装 Docker 与 Docker Compose v2
- 服务器可访问 Docker Hub

## 自定义配置（可选）

在部署目录创建 `.env` 覆盖默认值：

```bash
WEB_PORT=8080
MYSQL_ROOT_PASSWORD=your-strong-password
TOKEN_SECRET=your-long-random-secret
BLOG_API=http://your-domain:8080
BLOG_CMS=http://your-domain:8080/cms
BLOG_VIEW=http://your-domain:8080
IMAGE_TAG=1.0.0        # 默认 latest
```

修改后重新 `docker compose up -d` 生效。

## 版本升级

升级流程：**自动从 Docker Hub 解析最新版本 tag → 拉取该版本镜像 → 重启**（容器启动时自动执行数据库增量 SQL）。使用具体版本号 tag（而非 `latest`），便于通过 `docker images` 直观看到运行版本。

```bash
cd blogloom
./upgrade.sh            # 自动解析并升级到最新版本
./upgrade.sh 1.0.1      # 升级到指定版本
```

等价手动命令：

```bash
docker compose pull && docker compose up -d
```

> `install.sh` 已自动生成 `upgrade.sh`，并把当前版本写入 `.env` 的 `IMAGE_TAG`。数据在 `./data/`，升级不会丢失。
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
curl -fsSLO https://raw.githubusercontent.com/changluya/BlogLoom/feat_1.0.x_4/docker/standalone/docker-compose.yml
docker compose up -d
```
