---
title: "文章阅读"
description: 文章详情页的结构与阅读体验。
---

文章详情页负责渲染 Markdown 正文，并提供目录、图片预览与互动入口。

## 页面结构

- 文章标题、发布时间、分类与标签
- Markdown 正文与自动生成的目录
- 版权说明与上一篇 / 下一篇导航
- 评论区与回复

## 内容渲染

- 后端使用 CommonMark 解析 Markdown，前端展示渲染结果。
- 代码块以高亮形式呈现。
- 图片支持点击预览（v-viewer）。
- 支持按需为文章设置访问密码。

## 阅读增强

- 目录随滚动定位当前章节
- 图片懒加载
- 响应式排版

## 下一步

- [评论与互动](/v1/zh/guide/concepts/comments)
- [Markdown 写作](/v1/zh/guide/concepts/markdown)
- [文章创作](/v1/zh/guide/admin/article-editor)
