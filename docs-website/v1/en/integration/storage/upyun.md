---
title: "Upyun image hosting"
description: Integrate Upyun object storage.
---

Upyun provides object storage and CDN, a good fit for hosting blog images.

## Settings

- Service name
- Operator and password
- Access domain
- Storage directory

## Usage

1. Select Upyun under Media in the console.
2. Fill in the service name, operator and password.
3. Configure the access domain and start uploading.

## Notes

- Keep credentials in backend configuration only; never expose them to the frontend.
- Define sensible directory and naming rules for uploads.
- Enable CDN acceleration and caching as needed.

## Next steps

- [Tencent Cloud image hosting](/v1/en/integration/storage/tencent)
- [Media](/v1/en/guide/admin/media)
