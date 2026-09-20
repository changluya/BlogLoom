---
title: "Admin console (blog-cms-ui)"
description: The content management and operations console.
---

`blog-cms-ui` is the author-facing console where all write operations happen.

## Stack

- Vue 2.6.11 + Vite 4
- Element UI
- Vuex, Vue Router
- ECharts, mavon-editor

## Key features

- Dashboard: PV, UV, content counts and visitor map
- Authoring: Markdown editing, categories, tags and publishing
- Content: posts, moments, categories, tags and comments
- Recycle bin: restore or permanently delete removed posts, with bulk actions
- Columns: two-level columns with cover, summary and import/export
- Local knowledge base: directory tree management and bulk ZIP import/export
- Pages: site settings, friends and about page
- Media: local and Aliyun OSS upload configuration with connectivity test
- System: accounts and scheduled jobs
- Logs: job, login, operation, exception and access logs

## Develop and build

```bash
cd blog-cms-ui
npm install
npm run dev      # http://localhost:8079
npm run build    # production build
```

## Default account

```text
username: admin
password: 123456
```

Change the password immediately after the first login.

## Next steps

- [Dashboard](/v1/en/guide/admin/dashboard)
- [Authoring](/v1/en/guide/admin/article-editor)
- [Site settings](/v1/en/guide/admin/site)
