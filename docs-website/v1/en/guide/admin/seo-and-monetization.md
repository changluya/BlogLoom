---
title: "SEO, Feeds and Monetization"
description: Site-level SEO config, robots / sitemap / rss site-level files and Google AdSense ad integration.
---

BlogLoom has built-in site-level SEO: public pages are server-rendered, robots / sitemap / rss are generated automatically, and site verification and Google AdSense are supported.

## SEO config

In the console's "SEO → SEO config" page, set the site's public SEO domain (canonical, sitemap and RSS all use it), with quick view and download of the core files:

| File | Role |
| --- | --- |
| `robots.txt` | Crawl rules and Sitemap declaration |
| `sitemap.xml` | Public article map |
| `rss.xml` | Subscription feed |
| `ads.txt` | Ad authorized-seller declaration |

## Server-side rendering

Public pages such as article detail, home, tag, category, column and archive are server-rendered with backend Thymeleaf, adding page-level title / description / canonical / Open Graph / JSON-LD so search engines can read the body directly.

## Search platform site verification

The console's "SEO → SEO platform association" supports Baidu / Bing / Google site verification: paste the whole verification code (the `content` value is extracted automatically); after saving, the site home and aggregation pages output the corresponding verification meta; leaving it empty outputs nothing.

## Monetization (Google AdSense)

The console's "SEO → Ad platform association" configures:

- Publisher ID
- Auto / manual ad slots (top, in-article, bottom, below the right TOC)
- `ads.txt` content

Paste the ad-unit code to auto-detect `client` / `slot`; when enabled, the loader and ad units are injected per position on both the front end and SSE pages; leaving it empty serves no ads.

## Next steps

- [SEO 01: Article page optimization, monetization and data monitoring](/v1/en/blogs/seo/01-article-page-monetization)
- [SEO 02: Server-side rendering and site-level files](/v1/en/blogs/seo/02-ssr-and-site-files)
- [SEO 03: Google AdSense integration](/v1/en/blogs/seo/03-adsense-guide)
