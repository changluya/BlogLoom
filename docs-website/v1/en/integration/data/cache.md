---
title: "Cache"
description: Internal caching backed by MySQL.
---

BlogLoom implements caching inside the application, storing cache data in the MySQL table `cache_entry`. No external cache service such as Redis is required.

## How it works

| Item | Description |
| --- | --- |
| Storage | MySQL table `cache_entry` |
| Access | Read and written by the backend cache module, transparent to business code |
| Extra service | No Redis or other cache middleware to deploy |

## Usage

- Caches are persisted together with the database; backing up the database includes cache data.
- Hot data such as site configuration is cached; clear the cache after changes for them to take effect.
- Clearing the cache does not affect business data such as posts and comments.

## Operations

- No dedicated port or password is needed for caching.
- Monitor table growth together with the database.
- When debugging cache issues, check that the database connection is healthy.

## Next steps

- [MySQL](/v1/en/integration/data/mysql)
- [Configuration](/v1/en/deploy/configuration)
