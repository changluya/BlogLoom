---
title: "From Local Markdown to a Published Blog: BlogLoom's Content Workflow"
description: "See how the knowledge-base tree, batch migration, article backups and AI Skill sync bring local Markdown content into BlogLoom."
---

BlogLoom is designed for developers and creators who write in Markdown. Keep authoring posts locally or use the admin editor, then organize and publish them with the knowledge-base tree, batch migration and AI Skill.

## Write and publish in one console

The article editor supports Markdown, live preview and full-screen writing. For each post, set its title, description, cover, category, tags and column. Publishing options control whether a post is public, private or password-protected, whether it is pinned or recommended, and whether comments and appreciation are enabled.

Images can be uploaded to the configured image-storage provider. BlogLoom manages the post and its related resources after saving.

## Organize existing posts with folders

The knowledge base presents posts as a directory tree. Create folders, adjust their hierarchy and order, and preview or edit documents in the console. This works well for grouping an existing Markdown library by topic before moving it to the blog.

**Migrate local posts** accepts nested folders or selected Markdown files. Imports can follow the folder structure or use the knowledge-base path in each post's metadata. A preflight check runs before import, progress is shown during the operation, and an error cancels the whole import so it does not leave a partial result. Categories and tags can be created and linked during import. Mark a cover in the body with `![coverImg](image-url)`.

For a local archive, download the full post library with its directory structure and import it again later when needed.

## Sync local content with an AI Skill

Download the dedicated AI Skill from the Posts page in the admin console. A Skill-capable assistant can then use natural language to:

- Import one post or sync a directory in bulk;
- Update an existing post body or change its title, description, category, tags, column and visibility;
- Search for posts and inspect the site's categories, tags and columns.

The Skill uses the same import capability as the console migration flow. Repeating a sync updates existing content and avoids duplicate posts. Download the package again to refresh its login token after it expires.

## A workflow for ongoing publishing

Local Markdown remains portable and easy to back up. The blog console handles organization, access settings and public presentation. Folder organization, batch import, backups and AI sync connect local writing to site publishing.

## Continue reading

- [AI Skill sync guide](/v1/en/integration/ai-skill)
- [Article authoring](/v1/en/guide/admin/article-editor)
- [Content management and migration](/v1/en/guide/admin/content)
- [Markdown and post format](/v1/en/guide/concepts/markdown)
