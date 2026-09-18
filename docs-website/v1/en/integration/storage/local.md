---
title: "Local image storage"
description: Upload images to a local server directory.
---

Local storage is the default image hosting option; images are stored in the server's filesystem.

## Configuration

- The upload directory defaults to `conf/upload/`.
- `conf/static/` exposes static assets for access.
- You can review and switch storage providers under Media in the console.

## Access

- Uploaded images are served through the configured domain or path.
- Nginx can serve the upload directory directly for better performance.

## Notes

- Ensure the upload directory exists and is writable.
- Back up the upload directory regularly.
- Restrict executable permissions to avoid security issues.
- Watch disk space, especially on image-heavy sites.

## Next steps

- [GitHub image hosting](/v1/en/integration/storage/github)
- [Media](/v1/en/guide/admin/media)
