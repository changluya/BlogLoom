---
title: "Authentication"
description: Console login and JWT-based authorization.
---

Write operations in the admin console require authentication. BlogLoom uses Spring Security and JWT to manage login state.

## Login flow

1. The console submits a username and password.
2. The backend verifies them and issues a JWT.
3. The frontend sends the token in the request header of subsequent calls.
4. The backend validates the token and resolves the current user.

## Token and state

- The token is signed with the `token.secretKey` configuration.
- Login state and similar temporary data are stored in the internal `cache_entry` table.
- When a token expires or is invalidated, sign in again.

## Security recommendations

- Use a long, random `token.secretKey` in production.
- Change the default password immediately.
- Transfer login requests over HTTPS only.
- Never commit secrets or credentials to version control.

## Next steps

- [API reference](/v1/en/guide/reference/api)
- [Security hardening](/v1/en/deploy/security)
- [Configuration](/v1/en/deploy/configuration)
