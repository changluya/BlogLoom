---
title: "Integration overview"
description: External dependencies and extensible capabilities.
---

BlogLoom builds on MySQL and Redis, and can integrate image hosting, email and geolocation through configuration.

## Categories

| Category | Capabilities |
| --- | --- |
| Image storage | Local upload, GitHub, Upyun, Tencent Cloud |
| Notifications | Comment email notifications |
| Data stores | MySQL, Redis |
| Utilities | IP geolocation, Markdown parsing, scheduled jobs |

## Configuration entry points

- Image hosting and mail are configured in the console or backend config.
- Data store connections live in `application-dev.properties` or `conf/application.properties`.
- Restart services or clear caches as needed after changes.

## Design principles

- External capabilities are added through adapters, making implementations replaceable.
- Credentials are separated from business configuration and never hard-coded.
- Core content features remain available even when a third-party dependency is down.

## Next steps

- [Local image storage](/v1/en/integration/storage/local)
- [MySQL](/v1/en/integration/data/mysql)
- [Redis](/v1/en/integration/data/redis)
