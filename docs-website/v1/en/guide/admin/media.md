---
title: "Media"
description: Upload configuration for local and Aliyun OSS storage.
---

Media management handles storage for post images and site assets, supporting local and Aliyun OSS storage.

## Supported storage

| Storage | Description |
| --- | --- |
| Local | Upload to a local server directory |
| Aliyun OSS | Upload to Aliyun object storage |

## Configuration

In the console under Media, choose a storage provider and fill in the credentials, such as the bucket, region, storage path and access keys, then run the connectivity test to verify the configuration.

## Security notes

- Never commit object storage keys or access tokens to version control.
- Use dedicated buckets and least-privilege credentials in production.
- Review the access permissions of the upload directory regularly.

## Next steps

- [Image storage integrations](/v1/en/integration/storage/local)
- [Aliyun OSS image storage](/v1/en/integration/storage/aliyun)
- [Site settings](/v1/en/guide/admin/site)
