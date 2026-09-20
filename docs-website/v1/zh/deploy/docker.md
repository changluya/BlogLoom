---
title: "Docker 部署"
description: 使用 Docker 一键部署 BlogLoom。
---

BlogLoom 提供容器化部署方案，只需 Docker 与 Docker Compose v2，无需在服务器安装 JDK 或 Node.js。

## 架构

只有两个容器：

| 容器 | 说明 |
| --- | --- |
| `blogloom-app` | 一体化镜像，包含后端、博客前台与管理后台页面；启动时先执行增量 SQL，再启动服务 |
| `blogloom-mysql` | MySQL 8 |

对外只暴露两个固定非默认端口：

| 入口 | 端口 |
| --- | --- |
| Web（前台 / 后台 / API） | 18080 |
| MySQL | 13306 |

## 用户视角：一键部署

面向只想快速把博客跑起来的用户，服务器安装 Docker 后执行：

```bash
mkdir blogloom && cd blogloom
curl -fsSL https://raw.githubusercontent.com/changluya/BlogLoom/master/docker/standalone/install.sh | bash
```

脚本会自动解析最新版本、拉取镜像、启动 `blogloom-app` 与 `blogloom-mysql` 并完成数据库初始化。

部署完成后：

| 入口 | 地址 |
| --- | --- |
| 博客前台 | `http://<服务器IP>:18080` |
| 管理后台 | `http://<服务器IP>:18080/cms` |
| 默认账号 | `admin` / `123456`（登录后请立即修改） |

后续升级：

```bash
cd blogloom
./upgrade.sh            # 自动解析最新版本并升级
./upgrade.sh 1.0.1      # 升级到指定版本
```

## 开发者视角：构建与发布

在仓库 `docker/` 目录下提供了一键脚本：

```bash
cd docker
./scripts/package.sh 1.0.0   # 打包镜像与离线部署包
./scripts/deploy.sh          # 部署（仓库版或离线包）
./scripts/upgrade.sh         # 增量升级
./scripts/push.sh            # 推送镜像到 Docker Hub
```

## 数据库增量升级

- 应用容器启动脚本按「等待 MySQL → 执行增量 SQL → 启动 Java」的顺序执行。
- 增量 SQL 与 `data/sql-local/` 中的记录比对，只执行未记录过的脚本，**幂等且数据不丢失**。
- 首次启动即完成全量初始化，重启或重建仅做增量检查。

## 数据持久化

`data/mysql`、`conf/logs`、`conf/upload` 与 `data/sql-local` 会挂载到宿主机，升级与重启不会丢失；备份时重点备份 `data/mysql` 与 `conf/upload`。

## 更多说明

自定义配置、数据目录、备份与运维等完整说明见仓库 [docker/README.md](https://github.com/changluya/BlogLoom/blob/master/docker/README.md)。

## 下一步

- [部署概览](/v1/zh/deploy/index)
- [环境要求](/v1/zh/deploy/environment)
- [运维](/v1/zh/deploy/operations)
