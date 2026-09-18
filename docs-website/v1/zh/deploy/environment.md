---
title: "环境要求"
description: 运行 BlogLoom 所需的软件与版本。
---

## 运行环境

| 组件 | 版本要求 |
| --- | --- |
| JDK | 8 或更高版本 |
| Maven | 3.6+ |
| Node.js | 16+ |
| npm | 随 Node.js 安装 |
| MySQL | 5.7+ 或 8 |
| Redis | 5+ |

## 端口占用

| 服务 | 默认端口 |
| --- | --- |
| `blog-view-ui` | 8080 |
| `blog-cms-ui` | 8079 |
| `blog-backend` | 8090 |
| MySQL | 3306 |
| Redis | 6379 |

## 目录准备

运行时会使用以下目录：

| 目录 | 用途 |
| --- | --- |
| `conf/logs/` | 运行日志 |
| `conf/upload/` | 本地上传文件 |
| `conf/static/` | 外部静态资源 |

## 下一步

- [初始化数据库](/v1/zh/deploy/database)
- [部署后端](/v1/zh/deploy/backend)
