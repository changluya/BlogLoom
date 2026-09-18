---
title: "BlogLoom 1.0 release"
description: Scope and upgrade notes for the first release.
---

BlogLoom 1.0 is the first official release, derived from Naccl/NBlog with rebranding, reorganization and UI polish.

## Scope

- **Three apps**: the public blog, admin console and backend run and build independently.
- **Content loop**: posts, categories, tags, columns, moments and comments.
- **Site configuration**: name, avatar, carousel, social links, friends and about page.
- **Analytics**: PV, UV, content counts, category/tag distribution and visitor map.
- **Media**: local upload plus GitHub, Upyun and Tencent Cloud configuration.
- **System**: accounts, scheduled jobs and log center.

## Database scripts

- Full script: `sql/increment/init.sql`
- Incremental scripts: `sql/increment/1.0/`

## Upgrade notes

For a fresh environment, import `init.sql`. For an existing one, apply incremental scripts with `bin/local/upgrate-sql.sh` after backing up the database.

## Known notes

- The default account is `admin` / `123456`; change it after the first login.
- Rendering may depend on the Redis cache; clear it after changing data.

## Next steps

- [Quick start](/v1/en/guide/quickstart)
- [Deployment](/v1/en/deploy/index)
- [Roadmap](/v1/en/community/roadmap)
