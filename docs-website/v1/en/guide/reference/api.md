---
title: "API reference"
description: An overview of public and admin REST endpoints.
---

The backend serves both the public blog and the admin console through REST APIs. Public endpoints read content; admin endpoints write and operate.

## Endpoint groups

| Group | Prefix | Description |
| --- | --- | --- |
| Public content | `/api` | Posts, categories, tags, archive, moments, comments |
| Site info | `/api` | Site config, friends, about page |
| Admin | `/admin` | Content, settings, logs and analytics |
| Auth | `/auth` | Login and tokens |

> Exact paths follow the `controller` implementations in the repository; this page describes groups and conventions.

## Conventions

- JSON for requests and responses.
- List endpoints support pagination.
- Admin endpoints require an auth token.
- Unified error structure: `code`, `msg`, `data`.

## Examples

```bash
# List posts
curl "http://localhost:8090/api/blog?page=1&size=10"

# Site info
curl "http://localhost:8090/api/site"
```

## Next steps

- [Authentication](/v1/en/guide/reference/authentication)
- [Database](/v1/en/guide/reference/database)
- [Backend service](/v1/en/guide/modules/blog-backend)
