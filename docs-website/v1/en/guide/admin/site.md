---
title: "Site Settings"
description: Site info, home banner, social links, friend links, about page and the custom display module.
---

Site settings maintain the site-level content shown to visitors; changes are reflected on the front end.

## Configurable items

- **Basics**: site name, favicon, avatar, footer QR code and text, copyright and filing
- **Home banner**: banner image and link
- **Social links**: GitHub, email and other external links
- **Friend links**: name, URL and avatar (friends page)
- **About me**: about page content
- **Reward QR code**: the article reward entry
- **Player playlist**: front-end music player configuration
- **Custom display module**: a title and content, shown in the front-end sidebar when enabled (announcements, tech groups, official accounts...)

## How it takes effect

- The front end reads the site config returned by the backend and renders it.
- Some config is cached in the internal `cache_entry` table; clearing the cache may be needed for immediate effect.

## Notes

- After changing initial data, a running environment needs a database sync and, as needed, a cache clear.
- Graphic and image assets can be uploaded uniformly via image host management.

## Next steps

- [Image host](/v1/en/guide/admin/media)
- [Home and navigation](/v1/en/guide/frontend/home)
- [SEO, feeds and monetization](/v1/en/blogs/features/seo-and-monetization)
