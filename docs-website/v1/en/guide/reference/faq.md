---
title: "FAQ"
description: Common questions about deployment and usage.
---

## The backend fails to start with a database connection error

Check the MySQL host, user and password in `application-dev.properties`, and confirm the database was created and `init.sql` imported.

## The frontend reports API errors after startup

Confirm the backend is running on port `8090`, and check that `VITE_API_URL` in `.env.development` points to the correct backend.

## Login says the account or password is wrong

The default account is `admin` / `123456`. If the password was changed, use the new one; if forgotten, reset it through the database.

## Page content does not update

Rendering may depend on both database site settings and the Redis cache. After changing seed data, sync the database and clear caches as needed.

## Images cannot be uploaded

Check the image hosting configuration, and verify the local upload directory or third-party storage credentials and permissions.

## No email notification for comments

Confirm the mail service is configured correctly and check the authorization code and sender settings.

## How do I upgrade the database?

For a fresh environment, import `sql/increment/init.sql`; for an existing one, apply incremental scripts with `bin/local/upgrate-sql.sh`.

## Next steps

- [Troubleshooting](/v1/en/deploy/troubleshooting)
- [Configuration](/v1/en/deploy/configuration)
