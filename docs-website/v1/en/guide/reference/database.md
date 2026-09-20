---
title: "Database"
description: Database scripts, initialization and incremental upgrades.
---

BlogLoom persists content and logs in MySQL. Database scripts live under `sql/`, and both initialization and upgrades run through incremental scripts.

## Script layout

```text
sql/
├── increment/          # Pending incremental SQL, grouped by version
│   └── 1.0/            #   1.0, named by timestamp
│       ├── 20260915000000_v1.0.x.sql   # baseline (schema + tables)
│       └── 2026..._v1.0.x.sql
└── local/              # Records of executed SQL (local, not committed)
```

- Filename format: `<timestamp>_v<version>.x.sql`; the script runs them sorted by filename.
- The earliest baseline script under `sql/increment/1.0/` contains the full schema.
- `sql/local/` is a flat execution record and is git-ignored.

## Initialization and upgrades

After creating the database, configure `bin/local/deploy.conf` and run:

```bash
bin/local/upgrate-sql.sh --dry-run   # preview pending SQL
bin/local/upgrate-sql.sh             # execute and record
```

See [Initialize the database](/v1/en/deploy/database).

## Execution rules

- Scripts already recorded in `sql/local/` are skipped, so reruns are safe.
- Different versions or changes must not share a filename (the record directory is flat).
- New incremental SQL uses the current time; backfilled changes use their historical time to keep timestamps ordered.

## Main objects

- Posts, categories, tags, columns and their relations
- Moments, comments and likes
- Site settings, friends and about page
- Users, roles and logs

## Notes

- When structure or seed data changes, add a new incremental script instead of editing an executed one.
- After changing seed data, sync the database and clear caches as needed.

## Next steps

- [MySQL integration](/v1/en/integration/data/mysql)
- [Initialize the database](/v1/en/deploy/database)
