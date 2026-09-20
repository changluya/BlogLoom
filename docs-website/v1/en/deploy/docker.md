---
title: "Docker deployment"
description: Deploy BlogLoom with Docker in one command.
---

BlogLoom ships a containerized deployment that needs only Docker and Docker Compose v2 — no JDK or Node.js on the server.

## Architecture

There are just two containers:

| Container | Description |
| --- | --- |
| `blogloom-app` | All-in-one image with the backend, public blog and admin console; it runs incremental SQL before starting the service |
| `blogloom-mysql` | MySQL 8 |

Only two fixed, non-default ports are exposed:

| Entry | Port |
| --- | --- |
| Web (blog / admin / API) | 18080 |
| MySQL | 13306 |

## User path: one-click deployment

For users who just want to get the blog running, install Docker and run:

```bash
mkdir blogloom && cd blogloom
curl -fsSL https://raw.githubusercontent.com/changluya/BlogLoom/master/docker/standalone/install.sh | bash
```

The script resolves the latest version, pulls the image, starts `blogloom-app` and `blogloom-mysql`, and initializes the database.

After deployment:

| Entry | URL |
| --- | --- |
| Public blog | `http://<server-ip>:18080` |
| Admin console | `http://<server-ip>:18080/cms` |
| Default account | `admin` / `123456` (change it after signing in) |

Upgrade later:

```bash
cd blogloom
./upgrade.sh            # resolve and upgrade to the latest version
./upgrade.sh 1.0.1      # upgrade to a specific version
```

## Developer path: build and release

One-click scripts live under `docker/` in the repository:

```bash
cd docker
./scripts/package.sh 1.0.0   # package the image and offline bundle
./scripts/deploy.sh          # deploy (repo or offline bundle)
./scripts/upgrade.sh         # incremental upgrade
./scripts/push.sh            # push the image to Docker Hub
```

## Incremental database upgrades

- The app entrypoint runs in order: wait for MySQL → run incremental SQL → start Java.
- Incremental SQL is compared against records in `data/sql-local/`; only unrecorded scripts run, making it **idempotent with no data loss**.
- The first start performs full initialization; restarts and rebuilds only check for increments.

## Data persistence

`data/mysql`, `conf/logs`, `conf/upload` and `data/sql-local` are mounted to the host, so upgrades and restarts do not lose data. Back up `data/mysql` and `conf/upload` in particular.

## More information

See the repository's [docker/README.md](https://github.com/changluya/BlogLoom/blob/master/docker/README.md) for custom configuration, data directories, backup and operations.

## Next steps

- [Deployment overview](/v1/en/deploy/index)
- [Environment](/v1/en/deploy/environment)
- [Operations](/v1/en/deploy/operations)
