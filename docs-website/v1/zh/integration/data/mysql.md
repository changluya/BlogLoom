---
title: "MySQL"
description: 内容与日志数据的持久化存储。
---

MySQL 是 BlogLoom 的主数据库，保存文章、评论、配置与日志等数据。

## 版本要求

- MySQL 5.7+ 或 MySQL 8
- 字符集建议 `utf8mb4`

## 连接配置

在 `application-dev.properties` 或 `conf/application.properties` 中配置：

- 数据库地址与端口
- 数据库名（默认 `blogloom`）
- 用户名与密码
- 连接池参数

## 数据访问

- 使用 MyBatis 进行数据访问。
- 列表查询使用 PageHelper 分页。
- 统一在 Mapper XML 中维护 SQL。

## 运维建议

- 定期备份数据库。
- 为应用配置独立账号与最小权限。
- 监控慢查询并优化索引。
- 升级前先备份并验证脚本。

## 下一步

- [数据库结构](/v1/zh/guide/reference/database)
- [初始化数据库](/v1/zh/deploy/database)
