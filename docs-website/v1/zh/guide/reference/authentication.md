---
title: "身份认证"
description: 管理端登录与基于 JWT 的鉴权流程。
---

管理后台的写操作需要身份认证。BlogLoom 使用 Spring Security 与 JWT 管理登录状态。

## 登录流程

1. 管理后台提交用户名与密码。
2. 后端校验通过后签发 JWT 令牌。
3. 前端在后续请求的请求头中携带令牌。
4. 后端校验令牌并解析当前用户。

## 令牌与状态

- 令牌使用配置项 `token.secretKey` 签名。
- 登录状态等临时数据保存在内部 `cache_entry` 表中。
- 令牌失效或过期后需要重新登录。

## 安全建议

- 生产环境使用足够长且随机的 `token.secretKey`。
- 首次登录后立即修改默认密码。
- 仅通过 HTTPS 传输登录请求。
- 不要把密钥与账号密码提交到版本库。

## 下一步

- [API 参考](/v1/zh/guide/reference/api)
- [安全加固](/v1/zh/deploy/security)
- [配置说明](/v1/zh/deploy/configuration)
