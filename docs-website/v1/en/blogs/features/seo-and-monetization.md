---
title: "SEO, Feeds and Site Monetization"
description: "See how BlogLoom combines server-rendered pages, site-level SEO files, search-engine verification and Google AdSense settings."
---

After publishing, site owners also need search engines and feed readers to access their content, and a central place to maintain related settings. BlogLoom brings public-page SEO, site-level files, platform verification and ad settings into one site-management workflow.

## Expose page metadata to search engines

Public pages such as post details, the home page, categories, tags, columns and archives support server-side rendering. Pages can include a title, description, canonical URL, Open Graph sharing metadata and JSON-LD structured data so search engines and social platforms can read the page and post details.

Set the public SEO domain in the console, then view or download these files:

- `robots.txt` declares crawl rules and the Sitemap URL;
- `sitemap.xml` lists public post pages;
- `rss.xml` provides a post feed;
- `ads.txt` publishes the site's authorized ad sellers.

## Verify the site with search platforms

Paste the site-verification code from Baidu, Bing, Google or another supported platform into the SEO association settings. BlogLoom extracts the verification value and adds the corresponding meta tag to public pages.

## Configure Google AdSense

The console supports a publisher ID, auto ads and manual ad slots, as well as the site's `ads.txt` content. Manual slots can be placed near the top, in the post body, at the bottom or beside the table of contents. When enabled, the public front end and server-rendered pages output the loader and ad slots according to the configuration; with no configuration, ads are not served.

SEO metadata and site files help search and feed tools access content. Search ranking, indexing time and ad revenue depend on external platforms and the site's content, so BlogLoom does not guarantee those outcomes.

## Continue reading

- [Site settings](/v1/en/guide/admin/site)
- [Article reading and page structure](/v1/en/guide/frontend/article)
- [Integration overview](/v1/en/integration/overview)
