---
title: "配置说明"
description: 后端与前端的关键配置项。
---

BlogLoom 的配置分为后端配置与前端环境变量两部分。

## 后端配置

本地开发配置位于：

```text
blog-backend/src/main/resources/application-dev.properties
```

外置配置参考：

```text
conf/application.properties
```

关键配置项：

| 配置 | 说明 |
| --- | --- |
| 数据源 | MySQL 地址、用户名、密码 |
| Redis | 地址、数据库编号、密码 |
| `token.secretKey` | 登录令牌签名密钥 |
| `blog.api` | 后端 API 对外地址 |
| `blog.cms` | 管理后台地址 |
| `blog.view` | 博客前台地址 |
| 邮件 | 发件服务器与授权码 |
| 上传 | 本地目录或第三方图床 |

## 前端环境变量

| 文件 | 变量 | 说明 |
| --- | --- | --- |
| `.env.development` | `VITE_API_URL` | 开发环境 API 地址 |
| `.env.production` | `VITE_API_URL` | 生产环境 API 地址 |

## 目录约定

- `conf/logs/`：运行日志
- `conf/upload/`：本地上传
- `conf/static/`：外部静态资源

## 下一步

- [安全加固](/v1/zh/deploy/security)
- [身份认证](/v1/zh/guide/reference/authentication)
