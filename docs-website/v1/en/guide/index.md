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
| `blog-backend` | REST API, auth, business logic, data access, scheduling | `http://localhost:8090` |
| MySQL | Posts, users, comments, config and logs | Database `blogloom` |
| Redis | Login state, cache and temporary data | Default `127.0.0.1:6379` |

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
