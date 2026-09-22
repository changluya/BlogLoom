---
title: "Content management"
description: Centralized maintenance of posts, moments, categories, tags and comments.
---

Content management covers day-to-day maintenance of every content object on the site.

## Objects

| Object | Actions |
| --- | --- |
| Posts | Create, edit, publish, unpublish, delete |
| Recycle bin | View deleted posts, restore or delete permanently, with bulk actions |
| Moments | Create, edit, delete |
| Categories | Create, rename, reorder, delete |
| Tags | Create, rename, delete |
| Columns | Two-level columns with cover, summary and import/export; link posts |
| Local knowledge base | Manage posts as a directory tree; bulk ZIP import/export |
| Comments | Review, reply, delete |

## Organization tips

- Use categories for stable sections and tags for topics.
- Use columns to group series for continuous reading; columns support two levels, a cover and a summary.
- Use the local knowledge base to organize posts as a directory tree and bulk import/export as ZIP, parsing metadata such as title, tags, category, summary, columns, create/update time and knowledge-base path on import; mark the cover in the body with `![coverImg](image-url)` — ordinary images are not treated as covers.
- On import, missing tags and categories are created and linked automatically; columns only match existing ones (including two-level columns) and are never auto-created.
- The Posts page offers a **Skill download** in the top-right corner, which lets an AI assistant sync local posts in natural language; see [AI Skill sync](/v1/en/integration/ai-skill).
- Clean up unused tags regularly to keep search clear.

## Next steps

- [Authoring](/v1/en/guide/admin/article-editor)
- [Content model](/v1/en/guide/concepts/content-model)
- [AI Skill sync](/v1/en/integration/ai-skill)
- [Comments](/v1/en/guide/concepts/comments)
