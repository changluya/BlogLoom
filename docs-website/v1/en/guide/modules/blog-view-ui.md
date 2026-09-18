---
title: "Public blog (blog-view-ui)"
description: The visitor-facing blog portal.
---

`blog-view-ui` is the visitor-facing portal that presents content and provides reading and interaction.

## Stack

- Vue 2.6.11 + Vite 4
- Semantic UI and Element UI
- Vuex, Vue Router
- Axios, v-viewer, vue-lazyload

## Key pages

- Home banner, site navigation and profile card
- Post list and post detail
- Category, tag and archive views
- Site search
- Moments and likes
- Friends and about pages

## Develop and build

```bash
cd blog-view-ui
npm install
npm run dev      # http://localhost:8080
npm run build    # production build
```

The API URL is controlled by `VITE_API_URL` in `.env.development`; use `.env.production` for production.

## Next steps

- [Home and navigation](/v1/en/guide/frontend/home)
- [Reading articles](/v1/en/guide/frontend/article)
- [Build and deploy the frontend](/v1/en/deploy/frontend)
