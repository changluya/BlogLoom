# BlogLoom

<p align="center">
  <strong>一套面向内容创作者的博客前后端一体化管理与发布平台</strong>
</p>

<p align="center">
  让博客文章的创作、管理、发布与展示集中在一个平台中完成。
</p>

<p align="center">
  <img src="https://img.shields.io/badge/JDK-1.8+-orange">
  <img src="https://img.shields.io/badge/SpringBoot-2.2.7.RELEASE-brightgreen">
  <img src="https://img.shields.io/badge/MyBatis-3.5.5-red">
  <img src="https://img.shields.io/badge/Vue-2.6.11-brightgreen">
  <img src="https://img.shields.io/badge/license-MIT-blue">
</p>

## 项目介绍

**BlogLoom** 是一套基于 **Spring Boot + Vue + MyBatis** 构建的开源博客前后端平台，目标是提供从内容管理到博客发布、前台展示的一站式体验。

它不仅是一个博客页面，更希望成为一套可持续二次开发的博客基础平台：通过统一后台管理文章、分类、标签、评论和站点配置，并通过独立博客前台完成内容发布与展示，让个人开发者和内容创作者能够快速搭建、维护和演进自己的博客系统。

BlogLoom 当前基于开源项目 [Naccl/NBlog](https://github.com/Naccl/NBlog) 进行二次开发，在保留原项目 MIT License 与版权声明的基础上持续演进。

> 当前阶段以稳定继承 NBlog 核心能力为主。数据库名、Java 包名及部分内部结构暂时保持兼容，后续将逐步完成技术栈升级与平台化改造。

## 我们希望解决什么问题

传统个人博客通常存在几个问题：前台与后台割裂、文章发布流程分散、站点配置依赖改代码、后续升级和扩展成本较高。

BlogLoom 希望把这些能力集中起来，形成统一工作流：

```text
内容创作
   ↓
文章管理
   ↓
分类 / 标签 / 页面组织
   ↓
预览与发布
   ↓
博客前台展示
   ↓
评论 / 访问 / 内容持续维护
```

最终目标是让使用者把精力更多放在 **写内容和运营博客** 上，而不是重复搭建博客基础设施。

## 核心能力

### 内容管理

- Markdown 文章编辑与管理
- 草稿、发布等文章状态管理
- 分类与标签管理
- 文章目录与代码高亮
- 站点基础信息配置

### 博客前台

- 独立博客门户
- 首页、文章详情、分类与标签页面
- 响应式页面布局
- 评论与互动
- 文章排版、图片预览、音乐等内容展示能力

### 管理后台

- 文章统一管理
- 分类、标签、评论管理
- 博客站点设置
- 数据统计与内容维护
- 基于权限认证的后台访问控制

### 发布能力

当前 BlogLoom 已具备从后台管理文章并发布到博客前台的完整链路。

后续会进一步围绕“**一处创作，多处发布**”进行增强，包括：

- SEO 与搜索引擎收录优化
- Sitemap / RSS
- 多站点管理
- 主题与页面配置
- CSDN、掘金、博客园等内容渠道发布适配
- Webhook / API 发布能力
- AI 辅助写作与内容整理

> 上述增强能力中，未完成的部分属于 BlogLoom 后续规划，不代表当前版本已经全部实现。

## 项目结构

```text
BlogLoom
├── blog-backend  # Spring Boot 后端服务
├── blog-cms-ui   # 博客管理后台
├── blog-view-ui  # 博客公开前台
├── pic           # README / 项目图片资源
├── LICENSE       # MIT License
└── deploy.sh     # 部署脚本
```

整体采用前后端分离架构：

```text
┌─────────────────────┐
│      blog-cms-ui    │
│   博客管理后台 Vue   │
└──────────┬──────────┘
           │
           │ REST API
           ▼
┌─────────────────────┐
│      blog-backend   │
│ Spring Boot/MyBatis │
└──────────┬──────────┘
           │
       MySQL / Redis
           │
           ▼
┌─────────────────────┐
│      blog-view-ui   │
│   博客公开前台 Vue   │
└─────────────────────┘
```

## 技术栈

### 后端

- Spring Boot
- Spring Security
- MyBatis
- PageHelper
- MySQL
- Redis
- JWT
- Quartz
- commonmark-java
- ip2region

### 前端

当前版本继承 NBlog 原有技术栈：

- Vue 2
- Vue Router
- Vuex
- Element UI
- Semantic UI
- Axios
- PrismJS
- ECharts
- mavonEditor

后续 BlogLoom 将逐步推进前端和后端技术栈现代化升级。

## 快速开始

### 1. 初始化数据库

创建 MySQL 数据库：

```sql
CREATE DATABASE nblog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后执行：

```text
blog-backend/nblog.sql
```

完成基础表结构与初始化数据导入。

### 2. 配置后端

修改：

```text
blog-backend/src/main/resources/application-dev.properties
```

至少确认以下配置：

- MySQL 连接信息
- Redis 地址
- `token.secretKey`
- 邮箱配置（如需评论通知）
- 博客 API 地址

### 3. 启动后端

启动 `blog-backend` Spring Boot 服务。

### 4. 启动管理后台

```bash
cd blog-cms-ui
npm install
npm run dev
```

### 5. 启动博客前台

```bash
cd blog-view-ui
npm install
npm run dev
```

## 默认账号

原始初始化数据中的后台账号为：

```text
用户名：Admin
密码：123456
```

首次部署后请尽快修改默认凭据及 `token.secretKey`，并检查生产环境数据库、Redis、邮件等配置。

## Roadmap

BlogLoom 后续将围绕“**博客管理 + 发布 + 分发**”持续演进：

- [ ] JDK 17 / Spring Boot 3 升级
- [ ] Vue 3 + TypeScript + Vite 管理后台升级
- [ ] MyBatis / MyBatis-Plus 能力整理
- [ ] SEO、Sitemap、RSS 完善
- [ ] 前台 SSR / SSG 能力
- [ ] 多站点 Blog Site 模型
- [ ] 主题与页面配置体系
- [ ] 多渠道文章发布
- [ ] 媒体资源统一管理
- [ ] AI 辅助写作与文章整理
- [ ] Docker Compose 一键部署

## 开源说明

BlogLoom 是一个持续演进中的开源项目，目前在 NBlog 成熟博客能力的基础上进行产品化和平台化改造。

如果你也希望拥有一套能够自主控制、方便二次开发的博客前后端平台，可以基于 BlogLoom 继续构建自己的博客产品。

## 鸣谢

BlogLoom 基于 [Naccl/NBlog](https://github.com/Naccl/NBlog) 进行二次开发。

特别感谢 **Naccl** 以及 NBlog 的所有贡献者提供了优秀的开源博客项目。原项目采用 **MIT License**，BlogLoom 在遵循其许可要求、保留原版权与许可声明的基础上继续开发。

同时感谢 Spring Boot、Vue、MyBatis、Element UI、Semantic UI 以及项目中使用到的所有开源项目和贡献者。

## License

BlogLoom 采用 [MIT License](./LICENSE) 开源。

本项目基于同样采用 MIT License 的 [Naccl/NBlog](https://github.com/Naccl/NBlog) 二次开发，并持续保留原项目版权与许可声明。
