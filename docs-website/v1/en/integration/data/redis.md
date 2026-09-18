---
title: "Redis"
description: Login state, cache and temporary data.
---

Redis stores login state, caches site data and holds temporary business data.

## Version

Redis 5+, default address `127.0.0.1:6379`.

## Usage

| Usage | Description |
| --- | --- |
| Login state | Console sessions and token state |
| Cache | Hot data such as site configuration |
| Temporary data | Short-lived business state |

## Connection config

Configure host, database index and password in `application-dev.properties` or `conf/application.properties`.

## Operations

- Set an access password and restrict network access.
- Monitor memory usage and configure an eviction policy if needed.
- After changing site configuration, clear the relevant cache if the frontend does not update.

## Next steps

- [MySQL](/v1/en/integration/data/mysql)
- [Configuration](/v1/en/deploy/configuration)
