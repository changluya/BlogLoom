---
title: "Environment"
description: Required software and versions.
---

## Runtime

| Component | Version |
| --- | --- |
| JDK | 8 or later |
| Maven | 3.6+ |
| Node.js | 16+ |
| npm | Bundled with Node.js |
| MySQL | 5.7+ or 8 |
| Redis | 5+ |

## Ports

| Service | Default port |
| --- | --- |
| `blog-view-ui` | 8080 |
| `blog-cms-ui` | 8079 |
| `blog-backend` | 8090 |
| MySQL | 3306 |
| Redis | 6379 |

## Directories

| Directory | Purpose |
| --- | --- |
| `conf/logs/` | Runtime logs |
| `conf/upload/` | Local uploads |
| `conf/static/` | External static assets |

## Next steps

- [Initialize the database](/v1/en/deploy/database)
- [Deploy the backend](/v1/en/deploy/backend)
