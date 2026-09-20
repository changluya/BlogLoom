---
title: "Aliyun OSS image storage"
description: Store images in Aliyun OSS.
---

Aliyun OSS image storage uploads images to Aliyun object storage, ideal when you want to decouple assets from the server and serve them through cloud storage.

## Configuration

- `accessKeyId`: Aliyun access key ID
- `accessKeySecret`: Aliyun access key secret
- `bucket`: OSS bucket name
- `area`: bucket region (endpoint area)
- `path`: storage path prefix

## Usage

1. Select Aliyun OSS under Media in the console.
2. Fill in the access keys, bucket, region and storage path.
3. Run the connectivity test to verify the configuration.
4. After uploading, images are written to the bucket and a URL is returned.

## Connectivity test

- Run the connectivity test under Media for a saved or in-progress configuration.
- The backend simulates uploading a probe image and deletes it immediately, confirming the keys, bucket and region are reachable.
- Once it passes, you can safely switch to this provider.

## Notes

- Use least-privilege access keys rather than a primary account key.
- Never commit keys to version control or expose them in frontend code.
- Mind bucket read/write permissions and CORS so the blog can load images.
- Watch storage cost and delivery speed when hosting many large files.

## Next steps

- [Local image storage](/v1/en/integration/storage/local)
- [Media](/v1/en/guide/admin/media)
