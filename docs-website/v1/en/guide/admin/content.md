---
title: "Content Management"
description: Central maintenance of articles, moments, categories, tags, comments, columns and the recycle bin.
---

Content management covers the daily maintenance of all content objects — the most frequently used module in the admin console.

## Objects

| Object | Operations |
| --- | --- |
| Articles | Create, edit, publish, unpublish, delete (moves to recycle bin) |
| Recycle bin | View deleted articles, restore or permanently delete, single or batch |
| Moments | Create, edit, delete, with images |
| Categories | Create, rename, sort, delete (one category per article) |
| Tags | Create, rename, delete (multiple tags per article) |
| Columns | Two-level management with cover, intro, sorting, import/export and article association |
| Local knowledge base | Directory-tree article management, batch ZIP import/export |
| Comments | Review, reply, delete |

## Organization tips

- Use categories for stable sections and tags for topic markers.
- Aggregate series with columns; columns support two levels with cover and intro.
- Organize articles in the knowledge base with a directory tree and batch ZIP import/export; import parses title, tags, category, summary, columns, create/update time and knowledge-base path metadata. The cover is marked in the body with `![coverImg](image-URL)` (only the `coverImg` alt is recognized; normal images aren't covers).
- On import, missing tags/categories are created and associated; columns are only matched (including second-level), never auto-created.
- Deleting an article moves it to the recycle bin: it's hidden from the front end and lists immediately, but its category, tags, columns and comments are preserved and it can be restored at any time.
- The "Skill download" button in the top right of the article management page provides a skill package for AI-assisted Markdown sync — see [AI Skill sync](/v1/en/integration/ai-skill).

## Relation to SEO

Public articles enter `sitemap.xml` and `rss.xml`, while drafts, recycle bin and password-protected articles do not. Consider the impact on already-indexed content before deleting an article.

## Next steps

- [Article authoring](/v1/en/guide/admin/article-editor)
- [Content model](/v1/en/guide/concepts/content-model)
- [AI Skill sync](/v1/en/integration/ai-skill)
- [Comments and interaction](/v1/en/guide/concepts/comments)
