---
title: "IP geolocation"
description: Resolve visitor locations for analytics.
---

BlogLoom uses ip2region and related capabilities to resolve visitor IP locations for the visitor map and source analysis.

## Capabilities

- Resolve country, province and city for an IP.
- Detect client and access source.
- Feed geographic distribution into the dashboard.

## Data sources

- Access records are written by the backend during request handling.
- Locations are resolved and aggregated during analysis.
- Combine with access logs under `conf/logs/`.

## Notes

- When behind a reverse proxy, forward the real IP (`X-Forwarded-For`).
- Update the offline IP database periodically.
- Location data is for statistics only; observe privacy compliance.

## Next steps

- [Dashboard](/v1/en/guide/admin/dashboard)
- [Nginx](/v1/en/deploy/nginx)
