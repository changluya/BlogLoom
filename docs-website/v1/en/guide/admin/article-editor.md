---
title: "Article Authoring"
description: Authoring and publishing articles with the Markdown editor.
---

Article authoring is the admin console's core feature, offering Markdown editing and full publishing configuration.

## Flow

1. Create a new article under "Articles".
2. Fill in the title, category, tags and description.
3. Write the Markdown body with mavon-editor, with live preview and full-screen writing.
4. Configure the cover, recommendation, columns and publish state.
5. Save or publish.

## Configurable fields

| Field | Description |
| --- | --- |
| Title | Article title |
| Description | List and SEO summary |
| Cover | List cover and share cover |
| Category | Owning category |
| Tags | Associated tags |
| Columns | Associated series (multi-select) |
| Recommend | Whether to feature on the home page |
| Pinned | Whether to pin |
| Public/private/password | Visibility; password-protected articles don't enter the sitemap |
| Comments toggle | Whether comments are allowed |
| Reward toggle | Whether rewards are allowed |

## Image upload

Images in the editor can be uploaded via the image host config, supporting local and Aliyun OSS — see [Image storage](/v1/en/integration/storage/local).

## Relation to SEO

After publishing, the article is server-rendered into a page with title / description / canonical / Open Graph / JSON-LD, so fill in the title and description carefully.

## Next steps

- [Markdown writing](/v1/en/guide/concepts/markdown)
- [Content management](/v1/en/guide/admin/content)
- [Article reading](/v1/en/guide/frontend/article)
