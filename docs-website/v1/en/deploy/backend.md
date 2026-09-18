---
title: "Deploy the backend"
description: Package and run blog-backend.
---

The backend is a Spring Boot application, packaged as an executable JAR.

## Build

```bash
cd blog-backend
mvn clean package
```

The artifact is an executable JAR under `target/`.

## Configuration

Local development config is at `blog-backend/src/main/resources/application-dev.properties`. For deployment you can also use the external config at:

```text
conf/application.properties
```

Check at least:

- MySQL host, user and password
- Redis host, database index and password
- `token.secretKey`
- Actual URLs for `blog.api`, `blog.cms` and `blog.view`
- Email and image storage settings

## Run

```bash
java -jar blog-backend/target/*.jar
```

Listens on `8090` by default. In production, run it under a process supervisor and write logs to `conf/logs/`.

## Next steps

- [Build and deploy the frontend](/v1/en/deploy/frontend)
- [Configuration](/v1/en/deploy/configuration)
- [Security](/v1/en/deploy/security)
