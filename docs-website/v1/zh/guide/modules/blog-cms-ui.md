---
title: "管理后台（blog-cms-ui）"
description: 面向站长的内容管理与运营后台。
---

`blog-cms-ui` 是面向站长的内容管理与运营后台，所有写操作都在此完成。

## 技术栈

- Vue 2.6.11 + Vite 4
- Element UI
- Vuex、Vue Router
- ECharts、mavon-editor

## 主要功能

- 数据仪表盘：PV、UV、内容数量与访客地图
- 文章创作：Markdown 编辑、分类标签与发布配置
- 内容管理：文章、动态、分类、标签与评论
- 页面管理：站点设置、友链与关于页
- 图床管理：上传配置与第三方存储
- 系统管理：账号与定时任务
- 日志中心：任务、登录、操作、异常与访问日志

## 开发与构建

```bash
cd blog-cms-ui
npm install
npm run dev      # http://localhost:8079
npm run build    # 生产构建
```

## 默认账号

```text
用户名：admin
密码：123456
```

首次登录后请立即修改密码。

## 下一步

- [数据仪表盘](/v1/zh/guide/admin/dashboard)
- [文章创作](/v1/zh/guide/admin/article-editor)
- [站点设置](/v1/zh/guide/admin/site)
