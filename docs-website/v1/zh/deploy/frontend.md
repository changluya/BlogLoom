---
title: "构建与部署前端"
description: 构建博客前台与管理后台的静态资源。
---

两个前端应用构建后均为静态资源，建议由 Nginx 托管。

## 构建管理后台

```bash
cd blog-cms-ui
npm install
npm run build
```

## 构建博客前台

```bash
cd blog-view-ui
npm install
npm run build
```

## 配置 API 地址

构建生产前端前，请分别检查两个模块的 `.env.production`，将 `VITE_API_URL` 修改为实际后端地址。

## 部署静态资源

- 将构建产物部署到 Nginx 的静态目录。
- 配置 history 模式路由回退到 `index.html`。
- 为静态资源配置缓存策略。

## 下一步

- [Nginx 配置](/v1/zh/deploy/nginx)
- [部署后端](/v1/zh/deploy/backend)
