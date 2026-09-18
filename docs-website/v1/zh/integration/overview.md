---
title: "集成概览"
description: BlogLoom 的外部依赖与可扩展能力。
---

BlogLoom 以 MySQL 与 Redis 为基础，并可通过配置接入图床、邮件与地域解析等外部能力。

## 集成分类

| 分类 | 能力 |
| --- | --- |
| 图床存储 | 本地上传、GitHub、又拍云、腾讯云 |
| 通知 | 评论邮件通知 |
| 数据存储 | MySQL、Redis |
| 工具 | IP 地域解析、Markdown 解析、定时任务 |

## 配置入口

- 图床与邮件等配置在管理后台或后端配置文件中维护。
- 数据存储连接在 `application-dev.properties` 或 `conf/application.properties` 中配置。
- 修改配置后按需重启服务或清理缓存。

## 设计原则

- 外部能力通过适配层接入，便于替换实现。
- 凭据与业务配置分离，避免硬编码。
- 第三方依赖不可用时，核心内容功能仍应可用。

## 下一步

- [本地图床](/v1/zh/integration/storage/local)
- [MySQL](/v1/zh/integration/data/mysql)
- [Redis](/v1/zh/integration/data/redis)
