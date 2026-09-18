---
title: "Initialize the database"
description: Create the database and initialize it with the incremental script.
---

BlogLoom stores business data in MySQL. Initialization runs through `bin/local/upgrate-sql.sh`; manually importing `init.sql` is no longer required.

## 1. Create the database

```sql
CREATE DATABASE blogloom
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

> The script verifies that the database exists before running, so create it first.

## 2. Configure the connection

Copy the template on first use:

```bash
cp bin/local/deploy.conf.example bin/local/deploy.conf
```

Edit `bin/local/deploy.conf`:

| Setting | Description |
| --- | --- |
| `DB_HOST` / `DB_PORT` | MySQL host and port |
| `DB_USER` / `DB_PASSWORD` | Database user and password |
| `DB_NAME` | Database name, default `blogloom` |

A custom config file can be used as well:

```bash
LOCAL_UPGRADE_CONFIG=/path/to/deploy.conf bin/local/upgrate-sql.sh
```

## 3. Run the incremental SQL

Preview the pending scripts, then run:

```bash
bin/local/upgrate-sql.sh --dry-run   # list pending SQL only, no DB connection
bin/local/upgrate-sql.sh             # execute and record
```

Script behavior:

- **Pending**: `sql/increment/<version>/**/*.sql`, sorted by filename (timestamp);
- **Recorded**: on success, a copy is written to the flat `sql/local/` directory;
- **Idempotent**: files already recorded in `sql/local/` are skipped, so reruns are safe;
- **Failure keeps context**: working files and logs are kept in `sql/local/.work/` on failure.

## Accounts and security

- Create a dedicated database account for the app; avoid root.
- Grant only the necessary privileges.
- Never commit the database password to version control.

## Next steps

- [Deploy the backend](/v1/en/deploy/backend)
- [Database](/v1/en/guide/reference/database)
