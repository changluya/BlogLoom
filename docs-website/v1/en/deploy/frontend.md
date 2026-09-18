---
title: "Build and deploy the frontend"
description: Build static assets for the blog and admin console.
---

Both frontend apps build to static assets, best served by Nginx.

## Build the admin console

```bash
cd blog-cms-ui
npm install
npm run build
```

## Build the public blog

```bash
cd blog-view-ui
npm install
npm run build
```

## Configure the API URL

Before building for production, check each module's `.env.production` and set `VITE_API_URL` to the real backend address.

## Deploy assets

- Deploy the build output to an Nginx static directory.
- Fall back to `index.html` for history-mode routes.
- Configure caching for static assets.

## Next steps

- [Nginx](/v1/en/deploy/nginx)
- [Deploy the backend](/v1/en/deploy/backend)
