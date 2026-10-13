---
title: "Article Reading"
description: The article detail page structure and reading experience.
---

The article detail page renders the Markdown body and provides a table of contents, image preview and interaction entries. Public article pages are server-rendered (Thymeleaf), serving both reading and SEO.

## Structure

- Article title, publish time, category and tags
- Markdown body and an auto-generated table of contents
- Copyright notice and previous / next navigation
- Comments and replies, reward entry
- A compact top navigation specific to the detail page

## Content rendering

- The backend parses Markdown with CommonMark; the front end displays the result.
- Code blocks are highlighted.
- Images support click-to-preview (v-viewer).
- Articles can optionally be password-protected.
- Focus mode hides both sidebars and keeps only the body.

## Reading enhancements

- The TOC tracks the current section while scrolling
- Lazy-loaded images
- Responsive typography
- Page-level SEO meta (title / description / canonical / Open Graph / JSON-LD)

## Next steps

- [Comments and interaction](/v1/en/guide/concepts/comments)
- [Markdown writing](/v1/en/guide/concepts/markdown)
- [SEO, feeds and monetization](/v1/en/blogs/features/seo-and-monetization)
