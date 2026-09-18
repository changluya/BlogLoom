---
title: "Configuration"
description: Key backend and frontend configuration.
---

BlogLoom configuration is split between backend config and frontend environment variables.

## Backend configuration

Local development config:

```text
blog-backend/src/main/resources/application-dev.properties
```

External config reference:

```text
conf/application.properties
```

Key settings:

| Setting | Description |
| --- | --- |
| Datasource | MySQL host, user, password |
| Redis | Host, database index, password |
| `token.secretKey` | Login token signing key |
| `blog.api` | Public backend API URL |
| `blog.cms` | Admin console URL |
| `blog.view` | Public blog URL |
| Mail | Sender server and authorization code |
| Upload | Local directory or third-party storage |

## Frontend environment variables

| File | Variable | Description |
| --- | --- | --- |
| `.env.development` | `VITE_API_URL` | Development API URL |
| `.env.production` | `VITE_API_URL` | Production API URL |

## Directory conventions

- `conf/logs/`: runtime logs
- `conf/upload/`: local uploads
- `conf/static/`: external static assets

## Next steps

- [Security](/v1/en/deploy/security)
- [Authentication](/v1/en/guide/reference/authentication)
