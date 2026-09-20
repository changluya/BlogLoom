---
title: "Deployment overview"
description: The path from local development to production.
---

This section describes how to deploy BlogLoom to production, covering environment setup, installation, configuration and operations.

## Deployment path

```text
Environment → Database → Backend → Frontend build & deploy → Nginx → Operations
```

## Deployment shape

BlogLoom is made of three applications:

| Application | Deployment |
| --- | --- |
| `blog-backend` | Packaged as an executable JAR and run as a Java process |
| `blog-cms-ui` | Built to static assets served by Nginx |
| `blog-view-ui` | Built to static assets served by Nginx |

## One-click Docker deployment

Besides manual deployment, BlogLoom offers a containerized one-click option: with only Docker and Docker Compose v2, it starts the `blogloom-app` and `blogloom-mysql` containers and automatically initializes and incrementally upgrades the database on startup, with no JDK or Node.js on the server. See [Docker deployment](/v1/en/deploy/docker).

## Recommended approach

- For manual deployment, serve frontend assets with Nginx and reverse proxy the backend API.
- You can also deploy with Docker in one command, without installing JDK or Node.js.
- Open only the necessary ports; keep the database off the public internet.
- Use external configuration for production parameters.

## Next steps

- [Environment](/v1/en/deploy/environment)
- [Docker deployment](/v1/en/deploy/docker)
- [Deploy the backend](/v1/en/deploy/backend)
- [Nginx](/v1/en/deploy/nginx)
