---
title: "什么是 BlogLoom？"
description: 一套覆盖内容生产到站点运营的开源博客 CMS。
---

BlogLoom 是一套基于 Spring Boot、MyBatis 和 Vue 构建的博客前后端一体化平台。它由公开博客前台、内容管理后台和后端服务三个模块组成，覆盖从 Markdown 创作到站点运营的完整链路。

```text
Markdown 创作 → 内容组织 → 审核与发布 → 前台展示 → 评论互动 → 访问分析 → 持续维护
```

## 它解决什么问题

- **内容分散**：写作、发布、评论、统计分别散落在多个工具中，BlogLoom 将它们集中到一个系统。
- **部署复杂**：希望用标准的前后端分离架构，部署过程可预期、可维护。
- **二次开发困难**：后端分层清晰、前端模块独立，方便替换主题或扩展接口。

## 系统组成

| 模块 | 职责 | 默认地址 |
| --- | --- | --- |
| `blog-view-ui` | 面向访客的博客门户、文章阅读与互动 | `http://localhost:8080` |
| `blog-cms-ui` | 面向站长的内容管理与运营后台 | `http://localhost:8079` |
| `blog-backend` | REST API、认证、业务逻辑、数据访问与任务调度 | `http://localhost:8090` |
| MySQL | 文章、用户、评论、配置及日志等持久化数据 | 数据库名 `blogloom` |
| 缓存（`cache_entry`） | 站点配置等热点数据缓存 | 随 MySQL 存储 |

## 下一步

<CardGroup cols={2}>
  <Card title="快速开始" href="/v1/zh/guide/quickstart">
    五分钟内完成数据库初始化并启动三端服务。
  </Card>
  <Card title="系统架构" href="/v1/zh/guide/architecture">
    了解前台、后台与后端之间的数据流与职责边界。
  </Card>
  <Card title="核心概念" href="/v1/zh/guide/concepts/content-model">
    理解文章、分类、标签、专栏与动态等核心对象。
  </Card>
  <Card title="部署与运维" href="/v1/zh/deploy/index">
    从环境准备到生产部署与故障排查。
  </Card>
</CardGroup>

> BlogLoom 当前版本基于 [Naccl/NBlog](https://github.com/Naccl/NBlog) 二次开发，遵循原项目 MIT License 并保留版权声明。
