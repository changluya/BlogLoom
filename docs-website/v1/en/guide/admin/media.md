---
title: "Image Host"
description: Local and Aliyun OSS upload configuration and connectivity test.
---

Image host management configures storage for article images and site assets, supporting local and Aliyun OSS storage.

## Supported storage

| Storage | Description |
| --- | --- |
| Local | Upload to a local server directory; works out of the box with no parameters |
| Aliyun OSS | Upload to Alibaba Cloud Object Storage |

## Configuration

In the admin console's "Image Host" page, choose the storage method and fill in the credentials, such as the access key, bucket name, region and storage path. The selected method's config is highlighted, and switching takes effect immediately without a restart.

Each method provides a "connectivity test": it actually uploads and deletes an image to confirm the parameters are correct and read/write works, helping you rule out problems before real use.

## Security tips

- Never commit object-storage keys or access tokens to the repository.
- In production, use a dedicated bucket and least-privilege credentials.
- Periodically review the access permissions of the upload directory.

## Next steps

- [Image storage integration](/v1/en/integration/storage/local)
- [Aliyun OSS storage](/v1/en/integration/storage/aliyun)
- [Site settings](/v1/en/guide/admin/site)
