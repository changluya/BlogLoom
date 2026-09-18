---
title: "Architecture"
description: How the public blog, admin console and backend fit together.
---

BlogLoom uses a decoupled architecture. Three applications run and build independently and communicate over a REST API.

## Overview

```text
┌──────────────────────┐          ┌──────────────────────┐
│    blog-view-ui      │          │     blog-cms-ui      │
│   Public blog        │          │   Admin console      │
└──────────┬───────────┘          └──────────┬───────────┘
           │            REST API             │
           └──────────────┬──────────────────┘
                          ▼
               ┌──────────────────────┐
               │    blog-backend      │
               │ Spring Boot/MyBatis  │
               └──────────┬───────────┘
                          │
                 ┌────────┴────────┐
                 ▼                 ▼
              MySQL              Redis
```

## Responsibilities

| Module | Stack | Responsibility |
| --- | --- | --- |
| `blog-view-ui` | Vue 2 + Semantic UI | Home, articles, archive, search, moments and comments |
| `blog-cms-ui` | Vue 2 + Element UI | Content, site settings, media, logs and analytics |
| `blog-backend` | Spring Boot + MyBatis | Auth, business logic, data access, scheduling and storage |

## Data flow

1. Visitors browse `blog-view-ui`, which fetches posts, comments and moments through the REST API.
2. Authors write and configure content in `blog-cms-ui`; write operations are authenticated and sent to the backend.
3. `blog-backend` applies business logic, reads and writes MySQL, and uses Redis for caching and login state.
4. Scheduled jobs, email notifications and file uploads are handled centrally by the backend.

## Extension points

- **Frontend**: the two apps are independent and can adopt different UI frameworks or themes.
- **Backend**: the `controller` / `service` / `mapper` layers make new APIs and modules easy to add.
- **Storage**: an upload adapter supports local and third-party image hosting.

## Next steps

- [Backend service](/v1/en/guide/modules/blog-backend)
- [Public blog](/v1/en/guide/modules/blog-view-ui)
- [Admin console](/v1/en/guide/modules/blog-cms-ui)
