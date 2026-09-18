---
title: "Operations"
description: Backup, upgrade and routine maintenance.
---

This page covers routine operations: backup, upgrade and maintenance.

## Backup

- Back up the database regularly with `mysqldump`.
- Back up `conf/upload/`, `conf/static/` and `conf/application.properties`.
- Store backups in a separate location and verify they can be restored.

## Upgrade

1. Back up the database and configuration.
2. Pull the new version.
3. Apply incremental SQL or run `bin/local/upgrate-sql.sh`.
4. Rebuild the backend and frontends.
5. Restart the services and run a regression check.

## Routine maintenance

- Watch exception and access logs under `conf/logs/`.
- Clean up expired logs and stale caches.
- Check disk space, especially the upload directory.
- Assess compatibility before upgrading dependencies.

## Rollback

Keep the previous build artifacts and a database backup. On failure, roll back the service first, then assess data recovery.

## Next steps

- [Troubleshooting](/v1/en/deploy/troubleshooting)
- [Database](/v1/en/guide/reference/database)
