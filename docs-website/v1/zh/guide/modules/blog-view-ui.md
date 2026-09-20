---
title: "博客前台（blog-view-ui）"
description: 面向访客的博客门户模块。
---

`blog-view-ui` 是面向访客的博客门户，负责展示内容并提供阅读与互动体验。

## 技术栈

- Vue 2.6.11 + Vite 4
- Semantic UI 与 Element UI
- Vuex、Vue Router
- Axios、v-viewer、vue-lazyload

## 主要页面

- 首页 Banner、站点导航、个人信息卡片与侧栏自定义展示模块
- 文章列表与文章详情
- 分类、标签、归档与专栏阅读
- 站内搜索
- 动态展示与点赞
- 友链与关于页

## 开发与构建

```bash
cd blog-view-ui
npm install
npm run dev      # http://localhost:8080
npm run build    # 生产构建
```

开发环境 API 地址由 `.env.development` 中的 `VITE_API_URL` 控制；生产环境对应 `.env.production`。

## 下一步

- [首页与导航](/v1/zh/guide/frontend/home)
- [文章阅读](/v1/zh/guide/frontend/article)
- [构建与部署前端](/v1/zh/deploy/frontend)
