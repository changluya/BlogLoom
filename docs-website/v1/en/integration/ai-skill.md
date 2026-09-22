---
title: "AI Skill sync"
description: Download a dedicated AI skill package and sync local Markdown posts to BlogLoom in natural language.
---

BlogLoom ships an out-of-the-box **AI Skill package**. In the admin console, open **Posts** and click **Skill download** in the top-right corner. The package embeds your site URL and login token, so once you hand it to a Skill-capable AI assistant you can sync local posts with a single sentence.

## Get started

1. Sign in to the admin console, open **Posts**, and click **Skill download** to get `blogloom-skill.zip`;
2. Unzip, install dependencies and load the environment:

```bash
cd blogloom-skill
pip install requests
source ./skill.env.sh      # loads BLOOM_BASE_URL / BLOOM_API_TOKEN (injected at download time)
```

3. Hand the whole `blogloom-skill/` directory to a Skill-capable AI assistant.

> When the token expires, download the skill package again to refresh it.

## What it can do

| Capability | Description |
| --- | --- |
| Create / bulk import | Import a single post or a whole directory; auto-place into knowledge-base directories, auto-create and link tags and categories, and match existing columns |
| Smart sync | Update when a post with the same title exists, otherwise create; re-running never duplicates posts |
| Update posts | Sync a new local body, or change title, description, category, tags, columns, visibility, top and recommend flags |
| Query posts | Search posts by title, view details, and list all categories, tags and columns |

The skill package reuses the same import endpoint as the admin **Migrate local posts** feature, so behavior matches the UI exactly.

## Markdown post format

Each post is a plain `.md` file: a **JSON block at the very top** declares metadata, followed by normal Markdown body. The cover is not a metadata field — mark it in the body with `![coverImg](image-url)`.

````markdown
```json
{
  "title": "SqlParser quick start",
  "tags": "AI,SEO,Content Marketing",
  "category": "SqlParser",
  "articleSummary": "A quick introduction to SqlParser.",
  "columns": "SqlParser, AI",
  "createTime": "2026-09-19 14:30:00",
  "updateTime": "2026-09-19 14:30:00",
  "knowledgeBasePath": "/backend/SqlParser"
}
```

# SqlParser quick start

![coverImg](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/cover.png)

Body content……
````

| Part | Description |
| --- | --- |
| Metadata block | Must be at the very top, fenced with ` ```json `; it is removed from the body after import |
| Fields | `title`, `tags`, `category`, `articleSummary`, `columns`, `createTime`, `updateTime`, `knowledgeBasePath` are all optional, each with a fallback |
| Cover | `![coverImg](url)` in the body; only the `coverImg` marker counts, ordinary images are ignored |
| Columns | Only matches existing columns (including two-level ones); never auto-creates |

## Example prompts

Just describe what you want; the assistant picks the right flow:

| Scenario | Example prompt |
| --- | --- |
| Import one post | "Import `/Users/me/blogs/maven.md` into my blog" |
| Import a directory | "Sync every post under `/Users/me/blogs/Java` to the site" |
| Place into a directory | "Import this post into the knowledge-base directory `SqlParser`" |
| Sync an update | "I edited `maven.md`, sync it to the live post" |
| Search posts | "Find posts whose title contains `Maven`" |
| Change visibility | "Set post 11 to private" |
| Top / recommend | "Pin post 11 to the top" |
| Change columns | "Change post 11's column to `Maven&Gradle`" |

## Next steps

- [Content management](/v1/en/guide/admin/content)
- [Markdown & content](/v1/en/guide/concepts/markdown)
- [Integration overview](/v1/en/integration/overview)
