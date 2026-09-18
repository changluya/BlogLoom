---
title: "MySQL"
description: Persistent storage for content and logs.
---

MySQL is BlogLoom's primary database, storing posts, comments, configuration and logs.

## Version

- MySQL 5.7+ or MySQL 8
- `utf8mb4` recommended

## Connection config

In `application-dev.properties` or `conf/application.properties`:

- Host and port
- Database name (default `blogloom`)
- User and password
- Connection pool settings

## Data access

- MyBatis for data access.
- PageHelper for list pagination.
- SQL maintained in mapper XML files.

## Operations

- Back up the database regularly.
- Use a dedicated, least-privilege account for the app.
- Monitor slow queries and optimize indexes.
- Back up and verify scripts before upgrading.

## Next steps

- [Database](/v1/en/guide/reference/database)
- [Initialize the database](/v1/en/deploy/database)
