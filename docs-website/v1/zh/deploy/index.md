---
title: "部署概览"
description: 从本地开发到生产部署的完整路径。
---

本部分描述如何在生产环境部署 BlogLoom，覆盖环境准备、安装部署、配置与运维。

## 部署路径

```text
环境准备 → 数据库初始化 → 部署后端 → 构建并部署前端 → 配置 Nginx → 上线运维
```

## 部署形态

BlogLoom 由三个应用组成：

| 应用 | 部署方式 |
| --- | --- |
| `blog-backend` | 打包为可执行 JAR，以 Java 进程运行 |
| `blog-cms-ui` | 构建为静态资源，由 Nginx 托管 |
| `blog-view-ui` | 构建为静态资源，由 Nginx 托管 |

## 推荐方案

- 生产部署时由 Nginx 托管前端静态资源并反向代理后端 API。
- 仅开放必要端口，数据库与 Redis 不直接暴露到公网。
- 使用外置配置管理生产环境参数。

## 下一步

- [环境要求](/v1/zh/deploy/environment)
- [部署后端](/v1/zh/deploy/backend)
- [Nginx 配置](/v1/zh/deploy/nginx)
