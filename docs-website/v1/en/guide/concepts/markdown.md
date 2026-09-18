---
title: "Markdown authoring"
description: Supported Markdown syntax and post fields.
---

Posts are written in Markdown, parsed by the backend with commonmark-java and rendered in the public blog.

## Supported syntax

- Standard Markdown: headings, lists, quotes, links, images, tables
- Fenced code blocks highlighted in the frontend
- Nested and task lists
- Images and external resources

## Post fields

When creating a post in the admin console, you can configure:

| Field | Description |
| --- | --- |
| `title` | Post title |
| `description` | Summary for lists and SEO |
| `firstPicture` | Cover image |
| `category` | Category |
| `tags` | Related tags |
| `isRecommend` | Feature on the home page |
| `isPublished` | Publish or keep as draft |
| `password` | Optional access password |
| `copyright` | Copyright notice |

## Table of contents and summary

- The frontend builds a table of contents from headings.
- `description` is shown as the summary in lists.
- Featured posts can be highlighted on the home page.

## Images

Images in the body can be uploaded through the image hosting configuration. See [Image storage](/v1/en/integration/storage/local).

## Next steps

- [Article editor](/v1/en/guide/admin/article-editor)
- [Reading articles](/v1/en/guide/frontend/article)
