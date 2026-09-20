---
title: "Markdown 写作"
description: 文章编辑支持的 Markdown 语法与内容字段。
---

BlogLoom 的文章以 Markdown 撰写，由后端使用 commonmark-java 解析并渲染到前台。

## 支持的语法

- 标准 Markdown：标题、列表、引用、链接、图片、表格
- 围栏代码块，并在前台高亮展示
- 嵌套列表与任务列表
- 图片与外部资源引用

## 文章字段

在管理后台创建文章时，除正文外还可配置：

| 字段 | 说明 |
| --- | --- |
| `title` | 文章标题 |
| `description` | 文章描述，用于列表与 SEO 摘要 |
| `firstPicture` | 封面图 |
| `category` | 所属分类 |
| `tags` | 关联标签 |
| `isRecommend` | 是否推荐到首页 |
| `isPublished` | 是否发布 |
| `password` | 可选的访问密码 |
| `copyright` | 版权说明配置 |

## 目录与摘要

- 前台会根据标题自动生成文章目录。
- `description` 会作为文章摘要展示在列表页。
- 支持通过配置在首页展示推荐文章。

## 图片处理

正文中的图片可通过图床管理统一上传，支持本地与阿里云 OSS。详见[图床存储](/v1/zh/integration/storage/local)。

## 下一步

- [文章编辑器](/v1/zh/guide/admin/article-editor)
- [文章阅读](/v1/zh/guide/frontend/article)
