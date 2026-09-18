---
title: "Security hardening"
description: Security checks to complete before going live.
---

Complete the following checks before going live.

## Credentials

- Change the default `admin/123456` password.
- Generate a long, random `token.secretKey`.
- Use a dedicated database account with least privilege.
- Never commit database passwords, token secrets, mail codes or object storage keys.

## Network and transport

- Open only necessary ports; keep MySQL and Redis off the public internet.
- Enable HTTPS to protect login and data transfer.
- Reverse proxy the backend API through Nginx.

## Content safety

- Sanitize comment content before rendering.
- Restrict executable permissions on upload directories.
- Review third-party image hosting access policies regularly.

## Backup

- Back up MySQL regularly.
- Keep configuration and external file backups.

## Next steps

- [Configuration](/v1/en/deploy/configuration)
- [Operations](/v1/en/deploy/operations)
