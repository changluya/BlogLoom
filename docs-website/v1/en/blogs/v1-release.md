---
title: "BlogLoom 1.0 release"
description: Scope and upgrade notes for the first release.
---

BlogLoom 1.0 is the first official release, derived from Naccl/NBlog with rebranding, reorganization and UI polish.

## Scope

- **Three apps**: the public blog, admin console and backend run and build independently.
- **Content loop**: posts, categories, tags, columns, moments and comments.
- **Content management**: a blog recycle bin (restore or delete permanently, with bulk actions), two-level column management (cover, summary, import/export) and a local Markdown knowledge base (directory tree, bulk ZIP import/export).
- **Site configuration**: name, avatar, carousel, social links, friends, about page and a custom home section.
- **Analytics**: PV, UV, content counts, category/tag post aggregation and visitor map.
- **Media**: local upload and Aliyun OSS configuration, with a connectivity test.
- **System**: accounts, scheduled jobs and log center.
- **Containerized deployment**: a one-click Docker option.

## Database scripts

- Full baseline: the earliest full script under `sql/increment/1.0/` (schema + tables)
- Incremental scripts: the remaining timestamped scripts under `sql/increment/1.0/`

## Upgrade notes

For a fresh environment, run `bin/local/upgrate-sql.sh` to initialize (the baseline includes the schema). For an existing one, rerun it to apply only the scripts not yet recorded in `sql/local/`; back up the database first.

## Known notes

- The default account is `admin` / `123456`; change it after the first login.
- Rendering may depend on the internal `cache_entry` cache; clear it after changing data.

## Next steps

- [Quick start](/v1/en/guide/quickstart)
- [Deployment](/v1/en/deploy/index)
- [Roadmap](/v1/en/community/roadmap)
