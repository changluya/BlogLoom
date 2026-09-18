---
title: "腾讯云图床"
description: 接入腾讯云对象存储（COS）。
---

腾讯云对象存储（COS）可作为博客图片的云端图床。

## 配置项

- SecretId / SecretKey
- 存储桶（Bucket）
- 地域（Region）
- 访问域名

## 使用方式

1. 在管理后台「图床管理」中选择腾讯云。
2. 填写密钥、存储桶与地域。
3. 配置访问域名后上传图片。

## 注意事项

- 使用子账号密钥并授予最小权限。
- 不要将 SecretKey 提交到版本库。
- 按需配置存储桶的访问权限与防盗链策略。
- 结合 CDN 提升图片访问速度。

## 下一步

- [本地图床](/v1/zh/integration/storage/local)
- [图床管理](/v1/zh/guide/admin/media)
