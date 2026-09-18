---
title: "后端服务（blog-backend）"
description: REST API、认证、业务逻辑、数据访问与任务调度。
---

`blog-backend` 是 BlogLoom 的核心服务，为博客前台与管理后台提供统一的 REST API。

## 技术栈

| 范围 | 技术 |
| --- | --- |
| 基础 | Java 8、Spring Boot 2.2.7、Spring MVC |
| 数据与缓存 | MyBatis、PageHelper、MySQL、Redis |
| 安全与任务 | Spring Security、JWT、Quartz、Spring Retry |
| 内容与工具 | commonmark-java、ip2region、Yauaa、Hutool |

## 分层结构

```text
controller/   公开端与管理端 REST 接口
service/      业务服务及实现
mapper/       MyBatis Mapper 接口
entity/       数据库实体
model/        DTO 与 VO
config/       安全、Web、Redis 等配置
task/         定时任务
util/         Markdown、上传、通知等工具
```

## 主要能力

- Spring Security + JWT 管理端身份认证
- MyBatis 数据访问与 PageHelper 分页
- Redis 缓存及临时状态管理
- Quartz 定时任务
- CommonMark Markdown 解析
- 评论通知与邮件发送
- 本地及第三方对象存储上传适配
- IP 地域解析、客户端与访问来源识别
- 统一异常处理与操作日志

## 启动

```bash
cd blog-backend
mvn spring-boot:run
```

默认端口为 `8090`。

## 下一步

- [API 参考](/v1/zh/guide/reference/api)
- [身份认证](/v1/zh/guide/reference/authentication)
- [部署后端](/v1/zh/deploy/backend)
