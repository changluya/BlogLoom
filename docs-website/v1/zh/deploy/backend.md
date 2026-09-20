---
title: "部署后端"
description: 打包并运行 blog-backend。
---

后端是一个 Spring Boot 应用，可打包为可执行 JAR 后运行。

## 构建

```bash
cd blog-backend
mvn clean package
```

构建产物位于 `target/` 目录下的可执行 JAR。

## 配置

本地开发配置位于 `blog-backend/src/main/resources/application-dev.properties`。部署时也可使用根目录下的外置配置：

```text
conf/application.properties
```

启动前至少检查：

- MySQL 地址、用户名和密码
- `token.secretKey` 登录令牌密钥
- `blog.api`、`blog.cms` 与 `blog.view` 的实际访问地址
- 邮件通知与图床配置

## 运行

```bash
java -jar blog-backend/target/*.jar
```

默认监听 `8090` 端口。生产环境建议使用进程守护工具管理后端进程，并将日志输出到 `conf/logs/`。

## 下一步

- [构建与部署前端](/v1/zh/deploy/frontend)
- [配置说明](/v1/zh/deploy/configuration)
- [安全加固](/v1/zh/deploy/security)
