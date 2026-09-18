---
title: "Media"
description: Upload configuration and third-party object storage.
---

Media management handles storage for post images and site assets, supporting local and third-party storage.

## Supported storage

| Storage | Description |
| --- | --- |
| Local | Upload to a local server directory |
| GitHub | Store images in a GitHub repository |
| Upyun | Upyun object storage |
| Tencent Cloud | Tencent Cloud object storage (COS) |

## Configuration

In the console under Media, choose a storage provider and fill in the credentials, such as repository, domain or object storage keys.

## Security notes

- Never commit object storage keys or access tokens to version control.
- Use dedicated buckets and least-privilege credentials in production.
- Review the access permissions of the upload directory regularly.

## Next steps

- [Image storage integrations](/v1/en/integration/storage/local)
- [Site settings](/v1/en/guide/admin/site)
