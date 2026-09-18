---
title: "Nginx 配置"
description: 托管前端静态资源并反向代理后端 API。
---

生产环境推荐由 Nginx 统一入口：托管两个前端应用，并反向代理后端 API。

## 基本结构

```nginx
server {
    listen 80;
    server_name blog.example.com;

    # 博客前台
    location / {
        root /var/www/blogloom/view;
        try_files $uri $uri/ /index.html;
    }

    # 管理后台
    location /admin/ {
        alias /var/www/blogloom/cms/;
        try_files $uri $uri/ /admin/index.html;
    }

    # 后端 API
    location /api/ {
        proxy_pass http://127.0.0.1:8090/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

## 要点

- 前端使用 history 路由时，需要回退到 `index.html`。
- 反向代理需转发真实 IP，保证访客统计与地域解析准确。
- 生产环境建议启用 HTTPS 并配置证书。

## 下一步

- [配置说明](/v1/zh/deploy/configuration)
- [安全加固](/v1/zh/deploy/security)
