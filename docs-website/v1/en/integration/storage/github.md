---
title: "GitHub image hosting"
description: Store images in a GitHub repository.
---

Store images in a GitHub repository, ideal when you want assets versioned alongside code.

## Settings

- Repository (`owner/repo`)
- Branch
- Access token
- Custom domain (optional)

## Usage

1. Select GitHub under Media in the console.
2. Fill in the repository, branch and access token.
3. Uploading commits the asset to the repository and returns a public URL.

## Notes

- Use a least-privilege token, never an account password.
- Do not commit tokens to version control or embed them in frontend code.
- Public repositories serve images directly; private ones need extra access handling.
- Watch repository size and access speed with many large files.

## Next steps

- [Upyun image hosting](/v1/en/integration/storage/upyun)
- [Media](/v1/en/guide/admin/media)
