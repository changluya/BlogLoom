---
title: "Troubleshooting"
description: Diagnose common issues by symptom.
---

## Backend will not start

1. Check the exception stack in the startup log.
2. Verify MySQL and Redis are reachable.
3. Confirm configuration is complete and secrets are set.
4. Confirm port `8090` is free.

## Blank frontend or 404 API calls

1. Confirm the backend is running.
2. Check `VITE_API_URL` in `.env.production`.
3. Check the Nginx reverse proxy and route fallback.

## Login state issues

1. Check that Redis is healthy.
2. Confirm `token.secretKey` is unchanged.
3. Clear browser storage and try again.

## Content does not update

1. Confirm the database has been updated.
2. Clear the Redis cache and refresh.
3. Check browser and CDN caches.

## Image upload fails

1. Check the image hosting configuration and credentials.
2. Check upload directory permissions and disk space.
3. Look for upload exceptions in the backend log.

## Collect information

Prefer to collect the backend log, browser console, API responses and database state first.

## Next steps

- [Operations](/v1/en/deploy/operations)
- [FAQ](/v1/en/guide/reference/faq)
