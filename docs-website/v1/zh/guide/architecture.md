---
title: "系统架构"
description: 理解博客前台、管理后台与后端服务之间的职责与数据流。
---

BlogLoom 采用前后端分离架构，三个应用各自独立运行、独立构建，通过 REST API 通信。

## 总体结构

```text
┌──────────────────────┐          ┌──────────────────────┐
│    blog-view-ui      │          │     blog-cms-ui      │
│  公开博客 / 访客端    │          │  内容管理 / 运营后台   │
└──────────┬───────────┘          └──────────┬───────────┘
           │            REST API             │
           └──────────────┬──────────────────┘
                          ▼
               ┌──────────────────────┐
               │    blog-backend      │
               │ Spring Boot/MyBatis  │
               └──────────┬───────────┘
                          │
                          ▼
                MySQL（含内部缓存 cache_entry）
```

## 三端职责

| 模块 | 技术栈 | 职责 |
| --- | --- | --- |
| `blog-view-ui` | Vue 2 + Semantic UI | 首页、文章、归档、搜索、动态、评论等访客体验 |
| `blog-cms-ui` | Vue 2 + Element UI | 内容管理、站点设置、图床、日志与统计 |
| `blog-backend` | Spring Boot + MyBatis | 认证、业务逻辑、数据访问、定时任务与文件存储 |

## 数据流

1. 访客在 `blog-view-ui` 浏览页面，前端通过 REST API 请求文章、评论、动态等数据。
2. 站长在 `blog-cms-ui` 中创作与配置，写操作经过鉴权后提交到后端。
3. `blog-backend` 处理业务逻辑，读写 MySQL，并通过内部 `cache_entry` 表实现缓存与登录状态。
4. 定时任务、邮件通知与文件上传由后端统一调度。

## 扩展点

- **前端**：两个前端应用相互独立，可分别更换 UI 框架或主题。
- **后端**：`controller` / `service` / `mapper` 分层清晰，便于新增接口与模块。
- **存储**：通过上传适配层支持本地与阿里云 OSS 图床。

## 下一步

- [后端服务](/v1/zh/guide/modules/blog-backend)
- [博客前台](/v1/zh/guide/modules/blog-view-ui)
- [管理后台](/v1/zh/guide/modules/blog-cms-ui)
