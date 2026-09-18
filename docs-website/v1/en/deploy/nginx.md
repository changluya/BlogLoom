---
title: "Nginx"
description: Serve frontend assets and reverse proxy the backend API.
---

In production, use Nginx as a single entry point: serve both frontend apps and reverse proxy the backend API.

## Basic structure

```nginx
server {
    listen 80;
    server_name blog.example.com;

    # Public blog
    location / {
        root /var/www/blogloom/view;
        try_files $uri $uri/ /index.html;
    }

    # Admin console
    location /admin/ {
        alias /var/www/blogloom/cms/;
        try_files $uri $uri/ /admin/index.html;
    }

    # Backend API
    location /api/ {
        proxy_pass http://127.0.0.1:8090/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

## Notes

- With history-mode routing, fall back to `index.html`.
- Forward the real IP so visitor analytics and geolocation are accurate.
- Enable HTTPS and install certificates in production.

## Next steps

- [Configuration](/v1/en/deploy/configuration)
- [Security](/v1/en/deploy/security)
