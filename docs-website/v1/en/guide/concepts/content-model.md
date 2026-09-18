---
title: "Content model"
description: How posts, categories, tags, columns, moments and comments relate.
---

BlogLoom organizes content with a set of related objects. Understanding them helps you configure the site and console correctly.

## Core objects

| Object | Description | Relations |
| --- | --- | --- |
| **Post (Blog)** | A Markdown article, the core entity | Belongs to one category, links to many tags and columns |
| **Category** | Top-level grouping for posts | A post belongs to one category |
| **Tag** | Topic marker for posts | A post can have many tags |
| **Column** | A series of posts, tree-structured | Many-to-many with posts |
| **Moment** | Short-form content | Supports likes |
| **Comment** | Visitor interaction under a post | Belongs to a post; supports replies |
| **Site setting** | Site-level configurable content | Name, avatar, friends, about page, etc. |

## Relationships

```text
Category 1 ──── * Post (Blog) * ──── * Tag
                       │
                       ├── * Comment
                       └── * Column (many-to-many)
Moment ── * Like
```

## Design conventions

- **One category per post**: categories for archiving, tags for topical search.
- **Many-to-many columns**: a post can appear in several series.
- **Nested comments**: replies use a parent/child relationship.
- **Moments are separate**: for short content that needs no category.

## Next steps

- [Markdown authoring](/v1/en/guide/concepts/markdown)
- [Comments](/v1/en/guide/concepts/comments)
- [Database](/v1/en/guide/reference/database)
