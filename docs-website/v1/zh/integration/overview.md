---
title: "集成概览"
description: BlogLoom 的外部依赖与可扩展能力。
---

BlogLoom 以 MySQL 为基础，缓存由内部 MySQL 表实现，并可通过配置接入图床、邮件与地域解析等外部能力。

## 集成分类

| 分类 | 能力 |
| --- | --- |
| 图床存储 | 本地上传、阿里云 OSS |
| 通知 | 评论邮件通知 |
| 数据存储 | MySQL、内部缓存（`cache_entry` 表） |
| 工具 | IP 地域解析、Markdown 解析、定时任务 |
| AI 技能 | 下载专属技能包，用 AI 助手同步本地 Markdown 文章 |

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
- [阿里云 OSS 图床](/v1/zh/integration/storage/aliyun)
- [MySQL](/v1/zh/integration/data/mysql)
- [缓存](/v1/zh/integration/data/cache)
- [AI Skill 同步](/v1/zh/integration/ai-skill)
