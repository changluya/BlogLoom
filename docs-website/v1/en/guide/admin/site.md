---
title: "Site settings"
description: Site info, social links, friends and about page.
---

Site settings maintain visitor-facing site content that is reflected in the public blog.

## Configurable items

- **Basics**: site name, logo, avatar, bio, footer
- **Carousel**: banner images and links
- **Social links**: GitHub, email and other external links
- **Friends**: name, URL and avatar
- **About**: about content and custom pages

## How changes take effect

- The public blog renders from site configuration returned by the backend.
- Some configuration is cached in Redis; after changes you may need to clear the cache.

## Notes

- After changing seed data, running environments must sync the database and clear caches as needed.
- Images and graphics can be uploaded through media management.

## Next steps

- [Media](/v1/en/guide/admin/media)
- [Home and navigation](/v1/en/guide/frontend/home)
