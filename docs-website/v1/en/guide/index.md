---
title: "What is BlogLoom?"
description: An open-source blog CMS covering the full content lifecycle.
---

BlogLoom is a full-stack blogging platform built with Spring Boot, MyBatis and Vue. It consists of a public blog frontend, a content management console and a backend service, covering everything from Markdown authoring to site operations.

```text
Markdown authoring → Content organization → Review & publish → Presentation → Comments → Analytics → Maintenance
```

## The problems it solves

- **Scattered content**: writing, publishing, comments and analytics spread across tools. BlogLoom brings them into one system.
- **Complex deployment**: a standard decoupled architecture with a predictable, maintainable deployment process.
- **Hard to extend**: a clearly layered backend and independent frontend modules make it easy to swap themes or add APIs.

## System components

| Module | Responsibility | Default URL |
| --- | --- | --- |
| `blog-view-ui` | Public blog portal, reading and interaction | `http://localhost:8080` |
| `blog-cms-ui` | Content management and operations console | `http://localhost:8079` |
| `blog-backend` | REST API, auth, business logic, data access, SEO rendering and scheduling | `http://localhost:8090` |
| MySQL | Posts, users, comments, config and logs | Database `blogloom` |
| Cache (`cache_entry`) | Hot data such as site configuration | Stored with MySQL |

## Core capabilities

- **Content loop**: Markdown authoring, categories/tags, columns, moments, comments, recycle bin and local knowledge base.
- **Site configuration**: site name, favicon, avatar, home banner, social links, friend links, about page and custom display module.
- **SEO and subscription**: server-rendered public pages, auto-generated `robots.txt` / `sitemap.xml` / `rss.xml`, search-platform verification and Google AdSense integration.
- **AI Skill sync**: download a skill package to import, update and query local Markdown with natural language.
- **Operations data**: visits, visitor count, content counts, category/tag distribution and visitor map.
- **One-command deployment**: a Docker all-in-one image + MySQL with command-based deploy and incremental upgrade.

## Next steps

<CardGroup cols={2}>
  <Card title="Quick start" href="/v1/en/guide/quickstart">
    Initialize the database and run all three apps in five minutes.
  </Card>
  <Card title="Architecture" href="/v1/en/guide/architecture">
    How the frontend, admin and backend collaborate.
  </Card>
  <Card title="Core concepts" href="/v1/en/guide/concepts/content-model">
    Understand posts, categories, tags, columns and moments.
  </Card>
  <Card title="Deploy" href="/v1/en/deploy/index">
    From environment setup to production deployment.
  </Card>
</CardGroup>

> BlogLoom is a derivative of [Naccl/NBlog](https://github.com/Naccl/NBlog), licensed under the original MIT License.
