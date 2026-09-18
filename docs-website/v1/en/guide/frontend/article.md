---
title: "Reading articles"
description: The post detail page and reading experience.
---

The post detail page renders Markdown content and provides a table of contents, image preview and interaction.

## Page structure

- Title, publish time, category and tags
- Markdown body and an auto-generated table of contents
- Copyright notice and previous/next navigation
- Comment section and replies

## Rendering

- The backend parses Markdown with CommonMark; the frontend renders the result.
- Code blocks are syntax highlighted.
- Images open in a viewer (v-viewer).
- Posts can optionally be protected by an access password.

## Reading enhancements

- The table of contents tracks the current section while scrolling
- Lazy-loaded images
- Responsive typography

## Next steps

- [Comments](/v1/en/guide/concepts/comments)
- [Markdown authoring](/v1/en/guide/concepts/markdown)
- [Authoring](/v1/en/guide/admin/article-editor)
