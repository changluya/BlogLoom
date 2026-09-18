---
title: "Backend service (blog-backend)"
description: REST API, auth, business logic, data access and scheduling.
---

`blog-backend` is the core service, exposing a single REST API for both the public blog and the admin console.

## Stack

| Area | Technology |
| --- | --- |
| Foundation | Java 8, Spring Boot 2.2.7, Spring MVC |
| Data & cache | MyBatis, PageHelper, MySQL, Redis |
| Security & jobs | Spring Security, JWT, Quartz, Spring Retry |
| Content & utilities | commonmark-java, ip2region, Yauaa, Hutool |

## Layered structure

```text
controller/   public and admin REST endpoints
service/      business services and implementations
mapper/       MyBatis mapper interfaces
entity/       database entities
model/        DTOs and VOs
config/       security, web, redis and other config
task/         scheduled jobs
util/         markdown, upload, notification utilities
```

## Capabilities

- Spring Security + JWT authentication for the console
- MyBatis data access and PageHelper pagination
- Redis caching and temporary state
- Quartz scheduled jobs
- CommonMark Markdown parsing
- Comment notifications and email delivery
- Local and third-party object storage adapters
- IP geolocation and client/source detection
- Unified exception handling and operation logs

## Run

```bash
cd blog-backend
mvn spring-boot:run
```

Listens on `8090` by default.

## Next steps

- [API reference](/v1/en/guide/reference/api)
- [Authentication](/v1/en/guide/reference/authentication)
- [Deploy the backend](/v1/en/deploy/backend)
