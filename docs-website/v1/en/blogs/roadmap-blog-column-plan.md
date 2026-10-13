---
title: "BlogLoom Future Blog Column Plan"
description: A content-roadmap of blog column topics: system architecture, content domain, SEO, Skill and other series plotted across phases.
---

# BlogLoom Future Blog Column Plan

This article lays out the planned blog column series for BlogLoom, mapping topic clusters to concrete source and documentation references so the writing can proceed steadily.

## Purpose

BlogLoom is not only an open-source blog platform but also an engineering-practice project. These columns keep a written record of the design decisions, implementation details and retrospectives, so readers can learn from them and the project gains long-term documentation.

## Column clusters

### 1. System architecture

How the three modules split responsibilities, how a request flows from the front end to the database, and why the modular business packages are kept.

| No. | Topic | Reference | Status |
| --- | --- | --- | --- |
| 1 | Architecture 01: the BlogLoom system overview | `README.md` | [Done] |
| 2 | Architecture 02: the responsibility boundaries of blog-backend, blog-cms-ui and blog-view-ui | Module sources | [In progress] |
| 3 | Architecture 03: backend layering from Controller to Mapper | `blog-backend/.../` | [Planned] |
| 4 | Architecture retrospective 01: why keep modular business packages | `module/` | [Planned] |

### 2. Content domain

The content model, Markdown rendering, the knowledge base and the writing flow.

| No. | Topic | Reference | Status |
| --- | --- | --- | --- |
| 1 | Content 01: from the Blog entity to Blog DTO/VO | `entity/Blog.java`, `model/dto`, `model/vo` | [In progress] |
| 5 | Content in practice 01: from editing a Markdown article in the CMS to reading it on the front end | `WriteBlog.vue`, front-end article page | [In progress] |
| 3 | Columns in practice 01: creating, editing, moving, sorting and deleting the column tree | `module/column/` | [In progress] |
| 1 | Markdown 01: from Markdown text to safe HTML | `MarkdownUtils.java` | [Done] |
| 1 | Knowledge base 01: why an independent `knowledge_node` directory tree | `module/knowledge/` | [Done] |

### 3. Content operations

CMS workflows, bulk operations and the log center.

| No. | Topic | Reference | Status |
| --- | --- | --- | --- |
| 2 | CMS in practice 01: article create, edit, publish and recycle bin | `blog-cms-ui/src/views/blog/`, `BlogAdminController.java` | [In progress] |
| 3 | CMS in practice 02: knowledge base tree, import preview and task progress | `views/knowledge/` | [In progress] |
| 4 | CMS in practice 03: column management and multi-column articles | `views/blog/column/` | [In progress] |
| 5 | CMS retrospective 01: bulk operations, confirmations and error feedback | `blog-cms-ui/src/` | [Planned] |
| 3 | Operations in practice 01: the log center | `views/log/`, log controllers | [In progress] |

### 4. SEO and monetization

Article-page SEO, server-side rendering, site-level files, data monitoring and AdSense.

| No. | Topic | Reference | Status |
| --- | --- | --- | --- |
| 1 | SEO 01: article page optimization, monetization and data monitoring | This blog series | [Done] |
| 2 | SEO 02: why SPA blogs need server-side rendering and site-level files | This blog series | [Done] |
| 3 | SEO 03: Google AdSense integration guide | This blog series | [Done] |

### 5. AI Skill

The BlogLoom content-sync skill and the publisher distribution skill.

| No. | Topic | Reference | Status |
| --- | --- | --- | --- |
| 1 | BlogLoom Skill design and implementation | `resources/skill/blogloom-skill/` | [Done] |
| 2 | Publisher Skill 01: one-click publishing skill from 0 to 1 | `blogloom-publisher-skill` | [Done] |
| 3 | Publisher Skill 02: CSDN channel design and implementation | `blogloom-publisher-skill` | [Done] |
| 4 | Publisher Skill: WeChat Official Account (gzh) design and implementation | `blogloom-publisher-skill` | [Done] |

### 6. Deployment and operations

One-command Docker deployment, incremental upgrade, configuration and troubleshooting.

| No. | Topic | Reference | Status |
| --- | --- | --- | --- |
| 1 | Docker one-command deployment design | `docs/工程化开发模块设计思路产物/运维/` | [Done] |
| 2 | Incremental SQL and smooth upgrades | `sql/increment/` | [Planned] |

## Writing conventions

+ Each column article follows the pattern: background → approach → implementation → pitfalls → verification.
+ Prefer real code and real commands over abstract description.
+ Keep the source references accurate so edits stay traceable.

## Next steps

- [BlogLoom 1.0 release](/v1/en/blogs/v1-release)
- [BlogLoom Publisher Skill 01](/v1/en/blogs/blogloom-publisher-skill/01-publisher-skill)
- [Roadmap](/v1/en/community/roadmap)
