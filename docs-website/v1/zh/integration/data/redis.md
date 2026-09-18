---
title: "Redis"
description: 登录状态、缓存与临时数据。
---

Redis 用于保存登录状态、缓存站点数据与临时业务数据。

## 版本要求

Redis 5+，默认地址 `127.0.0.1:6379`。

## 用途

| 用途 | 说明 |
| --- | --- |
| 登录状态 | 管理端会话与令牌状态 |
| 缓存 | 站点配置等热点数据 |
| 临时数据 | 短期有效的业务状态 |

## 连接配置

在 `application-dev.properties` 或 `conf/application.properties` 中配置地址、数据库编号与密码。

## 运维建议

- 为 Redis 设置访问密码并限制网络访问。
- 监控内存使用，必要时配置淘汰策略。
- 修改站点配置后，如前台未更新，可清理相关缓存。

## 下一步

- [MySQL](/v1/zh/integration/data/mysql)
- [配置说明](/v1/zh/deploy/configuration)
