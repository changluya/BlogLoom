---
title: "Quick start"
description: From database setup to launching the blog and admin console.
---

This guide gets you to a minimal running BlogLoom: initialize the database, start the backend, then launch the admin console and the public blog.

> Prefer not to install JDK and Node.js on the server? See [Docker deployment](/v1/en/deploy/docker) for a one-command start.

## Prerequisites

- JDK 8 or later
- Maven 3.6+
- Node.js 16+ and npm
- MySQL 5.7+ or MySQL 8

## 1. Initialize the database

```sql
CREATE DATABASE blogloom
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

```bash
mysql -u root -p < sql/increment/init.sql
```

For a fresh environment, importing `sql/increment/init.sql` is enough.

## 2. Configure and start the backend

Local development config lives at:

```text
blog-backend/src/main/resources/application-dev.properties
```

Check the database, `token.secretKey` and URL settings, then run:

```bash
cd blog-backend
mvn spring-boot:run
```

The backend listens on `8090`.

## 3. Start the admin console

```bash
cd blog-cms-ui
npm install
npm run dev
```

Open `http://localhost:8079` and sign in with the default account:

```text
username: admin
password: 123456
```

## 4. Start the public blog

```bash
cd blog-view-ui
npm install
npm run dev
```

Open `http://localhost:8080` to view the site.

## Next steps

- [Architecture](/v1/en/guide/architecture)
- [Content model](/v1/en/guide/concepts/content-model)
- [Deploy](/v1/en/deploy/index)
- [FAQ](/v1/en/guide/reference/faq)
