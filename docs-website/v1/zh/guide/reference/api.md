---
title: "API 参考"
description: 公开端与管理端 REST 接口概览。
---

BlogLoom 后端通过 REST API 同时服务博客前台与管理后台。公开接口用于读取内容，管理接口用于写入与运营。

## 接口分组

| 分组 | 前缀 | 说明 |
| --- | --- | --- |
| 公开内容 | `/api` | 文章、分类、标签、归档、动态、评论 |
| 站点信息 | `/api` | 站点配置、友链、关于页 |
| 管理接口 | `/admin` | 内容管理、设置、日志与统计 |
| 认证接口 | `/auth` | 登录与令牌 |

> 具体路径以仓库中的 `controller` 实现为准，本文给出分组与调用约定。

## 请求约定

- 使用 JSON 作为请求与响应格式。
- 列表接口统一支持分页参数。
- 管理接口需要携带认证令牌。
- 统一错误结构：`code`、`msg`、`data`。

## 示例

```bash
# 获取文章列表
curl "http://localhost:8090/api/blog?page=1&size=10"

# 获取站点信息
curl "http://localhost:8090/api/site"
```

## 下一步

- [身份认证](/v1/zh/guide/reference/authentication)
- [数据库结构](/v1/zh/guide/reference/database)
- [后端服务](/v1/zh/guide/modules/blog-backend)
