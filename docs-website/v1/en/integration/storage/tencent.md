---
title: "Tencent Cloud image hosting"
description: Integrate Tencent Cloud Object Storage (COS).
---

Tencent Cloud Object Storage (COS) can serve as a cloud image host for the blog.

## Settings

- SecretId / SecretKey
- Bucket
- Region
- Access domain

## Usage

1. Select Tencent Cloud under Media in the console.
2. Fill in the keys, bucket and region.
3. Configure the access domain and upload.

## Notes

- Use a sub-account key with least privilege.
- Never commit the SecretKey to version control.
- Configure bucket access permissions and hotlink protection as needed.
- Use CDN to speed up image delivery.

## Next steps

- [Local image storage](/v1/en/integration/storage/local)
- [Media](/v1/en/guide/admin/media)
