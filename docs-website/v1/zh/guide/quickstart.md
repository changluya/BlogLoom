---
title: "快速开始"
description: 从数据库初始化到启动博客前台与管理后台。
---

本文带你完成 BlogLoom 的最小可运行环境：初始化数据库、启动后端、启动管理后台与博客前台。

> 不想在服务器安装 JDK 与 Node.js？可直接参考 [Docker 部署](/v1/zh/deploy/docker) 一键启动。

## 准备工作

确保本机具备以下环境：

- JDK 8 或更高版本
- Maven 3.6+
- Node.js 16+ 与 npm
- MySQL 5.7+ 或 MySQL 8

## 1. 初始化数据库

创建数据库：

```sql
CREATE DATABASE blogloom
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

复制并填写数据库连接配置：

```bash
cp bin/local/deploy.conf.example bin/local/deploy.conf
```

执行增量 SQL 完成初始化：

```bash
bin/local/upgrate-sql.sh --dry-run   # 预览待执行 SQL
bin/local/upgrate-sql.sh             # 执行并记录
```

脚本会按时间戳顺序执行 `sql/increment/` 下的增量脚本（基线已包含建表语句），并将执行记录写入 `sql/local/`，可安全重复执行。详见[初始化数据库](/v1/zh/deploy/database)。

## 2. 配置并启动后端

本地开发配置位于：

```text
blog-backend/src/main/resources/application-dev.properties
```

至少检查数据库、`token.secretKey` 与访问地址配置，然后启动：

```bash
cd blog-backend
mvn spring-boot:run
```

后端默认运行在 `8090` 端口。

## 3. 启动管理后台

```bash
cd blog-cms-ui
npm install
npm run dev
```

访问 `http://localhost:8079`，使用默认账号登录：

```text
用户名：admin
密码：123456
```

## 4. 启动博客前台

```bash
cd blog-view-ui
npm install
npm run dev
```

访问 `http://localhost:8080` 查看博客站点。

## 下一步

- [系统架构](/v1/zh/guide/architecture) —— 三端如何协作
- [内容模型](/v1/zh/guide/concepts/content-model) —— 文章、分类、标签与专栏
- [部署与运维](/v1/zh/deploy/index) —— 生产环境部署
- [常见问题](/v1/zh/guide/reference/faq) —— 启动过程中的高频问题
