---
title: "BlogLoom 未来详细博客专栏规划"
description: 面向内容运营的博客专栏选题规划：系统架构、内容域、SEO、Skill 等专题的写作路线图。
---

# BlogLoom 博客专栏规划

## 一、专栏愿景

> 把 Markdown 内容编织成可创作、可发布、可运营、可持续演进的博客系统。

BlogLoom 的博客专栏不只介绍如何搭建一个博客页面，而是完整记录一套内容工程系统从需求、模型、接口、前端交互到部署运维的实现过程。每一篇文章都尽量对应仓库中的真实代码、设计文档或可运行的验证步骤，读者可以沿着文章理解“为什么这样设计”，也可以直接运行项目验证“这样设计是否可靠”。

BlogLoom 的内容主线不是单向的文章 CRUD，而是一条完整闭环：

```text
Markdown 创作
      ↓
文章模型 / 分类 / 标签 / 专栏
      ↓
正文解析 / 图片上传 / 资源归档
      ↓
知识库导入 / 发布与可见性控制
      ↓
前台阅读 / 搜索 / SEO / 评论互动
      ↓
CMS 运营 / 访问统计 / 日志审计
      ↓
Skill 分发 / Docker 部署 / 增量升级
```

专栏采用 **Lifecycle-first（围绕内容生命周期）+ Bottom-up（从基础设施向产品能力展开）** 的叙事方式，依次讲清楚：

1. 系统如何分层，后端、CMS、博客前台如何协作；
2. 一篇文章如何从 Markdown 变成数据库中的内容实体；
3. 图片、封面和附件如何上传、归档、访问和清理；
4. 知识库、专栏和批量迁移如何解决内容整理问题；
5. 内容如何被搜索引擎、读者和外部分发渠道消费；
6. 系统如何被部署、升级、监控和持续维护。

### 1.1 专栏定位

- **面向对象**：希望理解博客系统工程实现的 Java / Vue 开发者、个人站长和开源项目贡献者。
- **内容风格**：原理解释 + 源码拆解 + 接口/数据流 + 可运行实战 + 边界复盘。
- **代码原则**：文章中的结论必须能在当前仓库代码、SQL、前端页面或设计文档中找到依据。
- **演进原则**：代码已经支持的能力优先沉淀；仍在设计中的能力明确标记为规划项，不把设想写成现状。

### 1.2 文章类型与命名规范

每篇文章建议使用以下格式：

```text
BlogLoom<专栏层> <专题类别> <类型><序号>、<标题>
```

示例：

```text
BlogLoom内容域 原理01、从 Blog 实体到文章发布闭环
BlogLoom知识库 实战01、从 Markdown 目录批量导入博客
BlogLoom前台 原理02、文章详情页的 Markdown 渲染与阅读体验
BlogLoomPublisher Skill 实战01、用自然语言完成文章同步与发布
```

文章类型统一使用：

| 类型 | 适用内容 |
| --- | --- |
| 原理篇 | 设计动机、模型、协议、源码结构、事务与边界 |
| 实战篇 | 本地启动、接口调用、页面操作、部署和故障排查 |
| 复盘篇 | 方案对比、历史演进、踩坑、性能和安全取舍 |
| 番外篇 | 开源协作、版本发布、测试、规范和生态扩展 |

完成进度使用以下标记：

- `[已完成]`：仓库已有对应实现或设计文档，可直接整理为博客；
- `[进行中]`：已有部分代码或方案，仍需补齐验证、示例或文章内容；
- `[规划中]`：后续随着功能演进再实现和撰写。

> 这里的“已完成”表示已有可复用的代码/设计素材，不等同于文章已经发布到官网。

---

## 二、第一层：系统架构与工程基础

> 目标：先让读者建立 BlogLoom 的全局认知，再理解后续每个模块为什么这样拆分。

### 2.1 系统全貌与模块边界

| 序号 | 文章名称 | 文章位置 | 完成进度 |
| --- | --- | --- | --- |
| 1 | BlogLoom系统架构 原理01、从前后端分离到内容生产闭环 | `README.md`、`docs-website/v1/zh/guide/architecture.md` | `[已完成]` |
| 2 | BlogLoom系统架构 原理02、blog-backend、blog-cms-ui 与 blog-view-ui 的职责边界 | `README.md`、三个模块的源码目录 | `[进行中]` |
| 3 | BlogLoom系统架构 原理03、从 Controller 到 Mapper 的后端分层实践 | `blog-backend/src/main/java/com/changlu/blogloom/` | `[规划中]` |
| 4 | BlogLoom系统架构 复盘01、为什么保留模块化业务包而不是继续堆叠 Service | `blog-backend/src/main/java/com/changlu/blogloom/module/` | `[规划中]` |
| 5 | BlogLoom工程化 番外01、JDK 8、Spring Boot 2 与前端构建链的兼容边界 | `README.md`、各模块 `pom.xml` / `package.json` | `[进行中]` |

BlogLoom 的核心组成如下：

| 模块 | 主要职责 | 文章重点 |
| --- | --- | --- |
| `blog-backend` | REST API、认证、业务逻辑、数据访问、任务调度 | 领域模块、接口契约、事务、横切能力 |
| `blog-cms-ui` | 内容创作、迁移、站点配置和运营管理 | 页面状态、表单提交、接口编排、管理体验 |
| `blog-view-ui` | 首页、文章阅读、搜索、互动和站点展示 | 路由、渲染、响应式、阅读体验 |
| MySQL | 文章、用户、评论、日志、任务等持久化 | 表结构、增量 SQL、索引和数据兼容 |
| `skills/blogloom-publisher-skill` | AI Agent 可调用的内容导入与渠道分发能力 | 工具契约、SOP、鉴权、渠道适配 |

### 2.2 数据流总览

一篇文章从创建到展示，至少会经过以下边界：

```text
CMS 编辑器 / 本地 Markdown / Skill
                ↓
        Blog DTO + 资源上传接口
                ↓
  BlogService + Markdown / 资源归档服务
                ↓
       Blog / Category / Tag / Column
                ↓
      Mapper + MySQL + 缓存 / 统计
                ↓
 View API / SEO API / RSS / Sitemap
                ↓
        博客前台与搜索引擎
```

这一层的文章需要固定回答四个问题：

1. 数据在哪一层产生，在哪一层校验；
2. 哪些内容是数据库字段，哪些内容是正文中的 Markdown；
3. 哪些操作必须放在同一个事务中；
4. 一个失败发生后，文件、数据库、缓存和前端状态如何保持可恢复。

---

## 三、第二层：内容域与后端核心业务

> 目标：解释博客系统最核心的内容模型，以及文章从草稿、发布到下线的生命周期。

### 3.1 文章模型、发布状态与查询规则

| 序号 | 文章名称 | 文章位置 | 完成进度 |
| --- | --- | --- | --- |
| 1 | BlogLoom内容域 原理01、从 Blog 实体到 Blog DTO / VO 的内容模型设计 | `blog-backend/src/main/java/com/changlu/blogloom/entity/Blog.java`、`model/dto`、`model/vo` | `[进行中]` |
| 2 | BlogLoom内容域 原理02、文章可见性、密码访问、置顶与推荐状态如何协同 | `BlogVisibility.java`、`BlogAdminController.java`、`BlogServiceImpl.java` | `[规划中]` |
| 3 | BlogLoom内容域 原理03、分类、标签与专栏：三种内容组织方式的边界 | `Category`、`Tag`、`module/column` | `[进行中]` |
| 4 | BlogLoom内容域 原理04、首页博客列表的过滤、排序与分页规则 | `docs/prd/核心功能业务逻辑/前台核心逻辑设计/首页博客列表查询规则设计.md` | `[已完成]` |
| 5 | BlogLoom内容域 实战01、从 CMS 编辑一篇 Markdown 文章到前台可读 | `blog-cms-ui/src/views/blog/blog/WriteBlog.vue`、博客前台文章页面 | `[进行中]` |
| 6 | BlogLoom内容域 复盘01、文章删除为什么要先进入回收站 | `BlogAdminController.java`、回收站相关接口与页面 | `[规划中]` |

这一组文章重点讲清楚：

- 文章元数据与正文内容分离，避免把展示字段和正文解析逻辑混在一起；
- 前台只消费公开内容，后台可以看到更多管理状态；
- 分类用于主分类筛选，标签用于横向关键词，专栏用于系列内容聚合；
- 置顶、推荐、发布时间、更新时间等字段必须有稳定的排序和展示语义；
- 删除操作需要区分“业务不可见”“可恢复删除”和“彻底删除”。

### 3.2 专栏模型与多对多关系

| 序号 | 文章名称 | 文章位置 | 完成进度 |
| --- | --- | --- | --- |
| 1 | BlogLoom专栏 原理01、为什么专栏不能直接复用分类表 | `docs/开发文档留存/release_1.0.x/03、BlogLoom 博客专栏管理与前台展示设计方案.md` | `[已完成]` |
| 2 | BlogLoom专栏 原理02、两级专栏模型与 blog_column_relation 关系表 | 同上 | `[已完成]` |
| 3 | BlogLoom专栏 实战01、后台专栏树的创建、编辑、移动、排序与删除 | `blog-backend/src/main/java/com/changlu/blogloom/module/column/` | `[进行中]` |
| 4 | BlogLoom专栏 实战02、博客编辑时如何在事务内维护专栏关系 | `ColumnAdminController.java`、`BlogColumnServiceImpl.java` | `[进行中]` |
| 5 | BlogLoom专栏 原理03、专栏树与前台专栏详情页的数据查询 | `ColumnController.java`、`ColumnTreeVo.java` | `[进行中]` |
| 6 | BlogLoom专栏 复盘01、两级限制、排序不连续和删除引用的边界处理 | 专栏模块 DTO / Service 校验逻辑 | `[规划中]` |

专栏篇必须明确以下约束：

- `parent_id=0` 表示一级专栏，首期最多两级；
- 一篇博客可以加入多个专栏，但同一专栏中不能重复关联；
- 专栏是系列聚合，不替代分类、标签和知识库目录；
- 有子专栏或博客引用的专栏默认采用保守删除策略；
- 前台只展示公开专栏和公开博客，后台数据不能直接透传。

### 3.3 Markdown 解析与正文扩展

| 序号 | 文章名称 | 文章位置 | 完成进度 |
| --- | --- | --- | --- |
| 1 | BlogLoom Markdown 原理01、从 Markdown 文本到安全 HTML | `blog-backend/src/main/java/com/changlu/blogloom/util/markdown/MarkdownUtils.java` | `[已完成]` |
| 2 | BlogLoom Markdown 原理02、代码高亮、目录生成与文章摘要处理 | `MarkdownUtils.java`、博客详情 VO、前台渲染页面 | `[进行中]` |
| 3 | BlogLoom Markdown 原理03、自定义 cover 与 heimu 扩展的解析链路 | `util/markdown/ext/cover`、`util/markdown/ext/heimu` | `[已完成]` |
| 4 | BlogLoom Markdown 复盘01、HTML 清洗、外部图片与 XSS 风险 | `blog-view-ui` Markdown 渲染与后端内容处理逻辑 | `[规划中]` |
| 5 | BlogLoom Markdown 实战01、编写一篇兼容 BlogLoom 的高质量 Markdown 文章 | `docs-website/v1/zh/guide/concepts/markdown.md` | `[已完成]` |

---

## 四、第三层：知识库、迁移与内容资产管理

> 目标：把“本地 Markdown 文件”纳入平台内容闭环，解决目录归位、元数据解析、批量导入、导出和重复导入问题。

### 4.1 知识库目录模型

| 序号 | 文章名称 | 文章位置 | 完成进度 |
| --- | --- | --- | --- |
| 1 | BlogLoom知识库 原理01、为什么需要独立的 knowledge_node 目录树 | `blog-backend/src/main/java/com/changlu/blogloom/module/knowledge/` | `[已完成]` |
| 2 | BlogLoom知识库 原理02、目录节点、文章节点与博客实体的映射 | `KnowledgeNode.java`、`KnowledgeNodeType.java`、`KnowledgeArticleMetadata.java` | `[已完成]` |
| 3 | BlogLoom知识库 实战01、从本地目录树导入单篇与批量 Markdown | `docs/开发文档留存/release_1.0.x/02、BlogLoom 本地 Markdown 知识库导入导出设计方案.md` | `[已完成]` |
| 4 | BlogLoom知识库 实战02、ZIP 导入导出与目录结构互逆 | 同上 | `[已完成]` |
| 5 | BlogLoom知识库 原理03、导入预检、异步任务与进度轮询 | `KnowledgeImportPreview.java`、`KnowledgeImportWorker.java`、`KnowledgeImportProgress.java` | `[已完成]` |
| 6 | BlogLoom知识库 复盘01、同名文件的 SKIP / RENAME 策略与幂等导入 | `KnowledgeImportOptions.java`、导入任务服务 | `[进行中]` |
| 7 | BlogLoom知识库 原理04、文章元数据和 coverImg 的解析约定 | `KnowledgeArticleMetadataParser.java`、`KnowledgeCoverExtractor.java` | `[已完成]` |

知识库导入文章需要覆盖一条完整链路：

```text
选择目录 / ZIP
      ↓
读取 Markdown 与 front matter / 约定元数据
      ↓
文件预检：格式、路径、同名、目标节点
      ↓
创建或复用目录节点
      ↓
创建博客、分类、标签与专栏关系
      ↓
归档图片与封面资源
      ↓
异步执行并记录进度、成功数、失败数和跳过数
      ↓
输出可重复执行的导入结果
```

### 4.2 专栏与平台内容批量迁移

| 序号 | 文章名称 | 文章位置 | 完成进度 |
| --- | --- | --- | --- |
| 1 | BlogLoom迁移 原理01、从平台专栏数据映射到本地内容模型 | `docs/开发文档留存/release_1.0.x/04、支持批量本地迁移导入平台专栏、博客（包含分类、标签）.md` | `[已完成]` |
| 2 | BlogLoom迁移 实战01、批量导入专栏、博客、分类和标签 | 同上、`module/column` 与 `module/knowledge` | `[进行中]` |
| 3 | BlogLoom迁移 复盘01、外部平台字段缺失、编码和图片地址兼容 | 迁移脚本与资源归档模块 | `[规划中]` |
| 4 | BlogLoom迁移 原理02、如何设计可重试、可预览、可追踪的导入任务 | `ColumnImportSession`、`KnowledgeImportSession` | `[进行中]` |

---

## 五、第四层：上传、图床与资源归档

> 目标：解释图片和附件为什么不能只当作一个 URL 字符串处理，以及资源如何在临时目录、文章目录和图床渠道之间流转。

### 5.1 本地资源生命周期

| 序号 | 文章名称 | 文章位置 | 完成进度 |
| --- | --- | --- | --- |
| 1 | BlogLoom资源 原理01、统一上传根目录与访问 URL 规范 | `docs/工程化开发模块设计思路产物/backend/BlogLoom-上传文件目录与访问路径规范.md` | `[已完成]` |
| 2 | BlogLoom资源 原理02、临时上传、文章归档与正文 URL 重写 | `docs/开发文档留存/release_1.0.x/01、BlogLoom 本地文件上传与博客资源归档设计方案.md` | `[已完成]` |
| 3 | BlogLoom资源 实战01、编辑器上传图片并在保存文章时完成归档 | `BlogResourceAdminController.java`、`BlogResourceService.java`、`WriteBlog.vue` | `[已完成]` |
| 4 | BlogLoom资源 原理03、Markdown 图片、HTML src / href / poster 的统一扫描 | `BlogResourceService.java` | `[已完成]` |
| 5 | BlogLoom资源 复盘01、路径穿越、文件名、扩展名和外部 URL 安全校验 | `LocalResourceStorageService.java`、`UploadUtils.java` | `[已完成]` |
| 6 | BlogLoom资源 规划01、孤儿资源、超时临时文件和文章删除后的清理机制 | 资源归档设计文档的待完善项 | `[规划中]` |

资源归档文章需要强调一致性边界：

- 文件先上传到临时目录，保存成功后再归档到文章目录；
- 复制成功后才把临时资源加入待清理集合；
- 数据库保存失败时，不能假设文件操作自动回滚；
- 已归档 URL、历史 `/image/**` URL 和外部图床 URL 必须兼容；
- 服务端生成文件名并校验规范化后的绝对路径，不能直接信任客户端路径。

### 5.2 图床渠道抽象

| 序号 | 文章名称 | 文章位置 | 完成进度 |
| --- | --- | --- | --- |
| 1 | BlogLoom图床 原理01、本地上传与阿里云 OSS 的渠道抽象 | `service/storage/UploadChannel.java`、`UploadChannelManager.java` | `[已完成]` |
| 2 | BlogLoom图床 实战01、后台配置图床并执行连通性测试 | `BlogLoom-站点设置与图床配置项参数说明.md`、`ImageHostConnectivityService.java` | `[已完成]` |
| 3 | BlogLoom图床 复盘01、为什么移除历史第三方渠道并保留兼容边界 | `docs/工程化开发模块设计思路产物/backend/相关核心设计思路备份/图床/BlogLoom-图床历史支持情况与配置管理.md` | `[已完成]` |
| 4 | BlogLoom图床 规划01、将更多对象存储接入统一资源归档链路 | `UploadChannel` 扩展点 | `[规划中]` |

---

## 六、第五层：认证、安全与后端横切能力

> 目标：讲清楚一个内容平台如何区分公开访问和管理访问，并让异常、审计、限流、访客统计和任务执行具备可追踪性。

### 6.1 管理端认证与接口安全

| 序号 | 文章名称 | 文章位置 | 完成进度 |
| --- | --- | --- | --- |
| 1 | BlogLoom安全 原理01、Spring Security + JWT 的登录认证链路 | `SecurityConfig.java`、`JwtLoginFilter.java`、`JwtFilter.java` | `[已完成]` |
| 2 | BlogLoom安全 原理02、公开接口、管理接口与认证失败响应 | `MyAuthenticationEntryPoint.java`、各 Controller | `[已完成]` |
| 3 | BlogLoom安全 原理03、AccessLimit、登录日志与操作日志的组合 | `AccessLimit.java`、`OperationLogger.java`、对应 Aspect | `[已完成]` |
| 4 | BlogLoom安全 复盘01、内容平台常见的 XSS、路径穿越和敏感配置风险 | `SECURITY.md`、上传与 Markdown 处理逻辑 | `[进行中]` |
| 5 | BlogLoom安全 实战01、生产环境配置令牌密钥、上传目录与数据库凭据 | `conf/application.properties.conf`、部署文档 | `[已完成]` |

### 6.2 统一异常、日志、统计与定时任务

| 序号 | 文章名称 | 文章位置 | 完成进度 |
| --- | --- | --- | --- |
| 1 | BlogLoom后端 原理01、统一响应 Result 与 Controller 异常处理 | `Result.java`、`ControllerExceptionHandler.java`、异常类 | `[已完成]` |
| 2 | BlogLoom后端 原理02、访问日志、访客识别与行为统计 | `VisitLogAspect.java`、`Visitor`、`VisitRecord` | `[已完成]` |
| 3 | BlogLoom后端 原理03、Quartz 任务注册、执行日志与失败处理 | `ScheduleUtils.java`、`ScheduleJob`、后台任务管理 | `[进行中]` |
| 4 | BlogLoom后端 原理04、MySQL 缓存服务与缓存同步任务 | `module/cache`、`CacheSyncScheduleTask.java` | `[已完成]` |
| 5 | BlogLoom运维 复盘01、Redis 依赖的历史评估与移除 | `docs/工程化开发模块设计思路产物/backend/相关核心设计思路备份/早期历史/Redis 组件相关功能梳理与移除评估.md` | `[已完成]` |

---

## 七、第六层：博客前台与阅读体验

> 目标：从访客视角讲清楚内容如何被发现、阅读和互动，并解释视觉效果背后的性能与兼容性取舍。

### 7.1 前台页面与内容消费

| 序号 | 文章名称 | 文章位置 | 完成进度 |
| --- | --- | --- | --- |
| 1 | BlogLoom前台 原理01、Vue 前台的路由、状态与接口组织 | `blog-view-ui/src/`、`docs-website/v1/zh/guide/frontend/` | `[进行中]` |
| 2 | BlogLoom前台 原理02、首页内容聚合、分页和空状态设计 | `首页博客列表查询规则设计.md`、前台首页页面 | `[已完成]` |
| 3 | BlogLoom前台 原理03、文章详情页的 Markdown 渲染、目录与图片预览 | `blog-view-ui/src/views/`、文章详情相关组件 | `[进行中]` |
| 4 | BlogLoom前台 实战01、分类、标签、专栏、归档和搜索的统一浏览体验 | `blog-view-ui/src/`、前台功能文档 | `[进行中]` |
| 5 | BlogLoom前台 原理04、评论、回复、点赞与通知渠道 | `CommentController.java`、`util/comment/channel` | `[进行中]` |
| 6 | BlogLoom前台 复盘01、懒加载、回到顶部与阅读性能优化 | `blog-view-ui` 组件和样式 | `[规划中]` |

### 7.2 响应式视觉与交互

| 序号 | 文章名称 | 文章位置 | 完成进度 |
| --- | --- | --- | --- |
| 1 | BlogLoom前台视觉 原理01、首页首图固定背景、滚动视差与局部遮罩 | `docs/工程化开发模块设计思路产物/front/BlogLoom 首页首图滚动视差与固定遮罩实现方案.md` | `[已完成]` |
| 2 | BlogLoom前台视觉 原理02、移动端适配的断点、交互和维护规范 | `docs/工程化开发模块设计思路产物/front/BlogLoom 移动端适配与维护规范.md` | `[已完成]` |
| 3 | BlogLoom前台视觉 原理03、外部图床防盗链与图片加载兼容 | `docs/工程化开发模块设计思路产物/front/BlogLoom 前台外部图床图片防盗链优化方案.md` | `[已完成]` |
| 4 | BlogLoom前台视觉 规划01、主题变量、组件复用与可配置主题边界 | `blog-view-ui/src/styles/`、前台组件 | `[规划中]` |

---

## 八、第七层：CMS 内容创作与运营后台

> 目标：围绕“站长每天真正会使用的工作流”，讲解编辑、管理、迁移、配置、统计和审计如何组合成一个可用的后台。

### 8.1 内容创作与管理

| 序号 | 文章名称 | 文章位置 | 完成进度 |
| --- | --- | --- | --- |
| 1 | BlogLoom CMS 原理01、Markdown 编辑器与文章元数据表单设计 | `docs/prd/产品功能文档-CMS系统.md`、`WriteBlog.vue` | `[已完成]` |
| 2 | BlogLoom CMS 实战01、文章新建、编辑、发布和回收站操作 | `blog-cms-ui/src/views/blog/`、`BlogAdminController.java` | `[进行中]` |
| 3 | BlogLoom CMS 实战02、知识库目录树、导入预览和任务进度 | `blog-cms-ui/src/views/knowledge/`、知识库模块 | `[进行中]` |
| 4 | BlogLoom CMS 实战03、专栏管理与博客多选专栏 | `blog-cms-ui/src/views/blog/column/`、专栏模块 | `[进行中]` |
| 5 | BlogLoom CMS 复盘01、管理页面中的批量操作、确认和错误反馈 | `blog-cms-ui/src/`、各 Admin Controller | `[规划中]` |

### 8.2 站点配置、数据看板与日志中心

| 序号 | 文章名称 | 文章位置 | 完成进度 |
| --- | --- | --- | --- |
| 1 | BlogLoom运营 原理01、站点设置、资料卡、页脚、友链和自定义模块 | `BlogLoom-站点设置与图床配置项参数说明.md`、`SiteSettingAdminController.java` | `[已完成]` |
| 2 | BlogLoom运营 原理02、仪表盘指标、访客地图与内容统计 | `DashboardAdminController.java`、`VisitorAdminController.java` | `[进行中]` |
| 3 | BlogLoom运营 实战01、日志中心：访问、登录、操作、异常和任务日志 | `blog-cms-ui/src/views/log/`、日志 Controller | `[进行中]` |
| 4 | BlogLoom运营 原理03、系统配置如何与环境变量、外置配置协同 | `EnvironmentContext.java`、`SystemPropertyUtil.java`、`conf/` | `[已完成]` |

---

## 九、第八层：SEO、搜索与服务端输出

> 目标：解释 SPA 博客为什么需要额外的 SEO 设计，以及标题、摘要、目录、站点地图、RSS 和验证文件如何统一生成。

### 9.1 SEO 基础与第一阶段改造

| 序号 | 文章名称 | 文章位置 | 完成进度 |
| --- | --- | --- | --- |
| 1 | BlogLoom SEO 原理01、文章页 SEO、站点变现与数据监测 | `docs/工程化开发模块设计思路产物/backend/核心迭代功能设计/搜索引擎SEO优化改造/搜索引擎SEO优化调研学习01、文章页优化、站点变现与数据监测.md` | `[已完成]` |
| 2 | BlogLoom SEO 原理02、SPA 博客为什么需要服务端直出与站点级文件 | `docs/工程化开发模块设计思路产物/backend/核心迭代功能设计/搜索引擎SEO优化改造/搜索引擎SEO优化调研学习02、第一阶段核心改造、服务端直出与站点级文件.md` | `[已完成]` |
| 3 | BlogLoom SEO 原理03、Meta、Canonical、Open Graph 与结构化信息生成 | `module/seo/domain`、`SeoMetaBuilder.java`、`SeoHtmlSupport.java` | `[进行中]` |
| 4 | BlogLoom SEO 实战01、Sitemap、RSS、robots 与搜索引擎验证文件 | `SitemapService.java`、`RssService.java`、`SeoFileService.java` | `[进行中]` |
| 5 | BlogLoom SEO 原理04、SPA fallback 与文章详情页的服务端 HTML | `SpaIndexController.java`、`SeoPageController.java` | `[进行中]` |
| 6 | BlogLoom SEO 规划01、从页面级 SEO 到可观测的收录效果闭环 | SEO 模块与站点数据模块 | `[规划中]` |

SEO 篇应将“理论指标”和“代码行为”分开：搜索引擎是否收录不能由应用直接保证，但应用可以稳定输出可抓取的标题、描述、规范链接、站点地图、RSS 和验证文件，并通过数据监测不断调整。

---

## 十、第九层：BlogLoom Publisher Skill 与多渠道分发

> 目标：记录如何把 BlogLoom 的内容管理能力封装成 AI Agent 可以理解、调用、验证和扩展的 Skill。

### 10.1 Skill 基础架构

| 序号 | 文章名称 | 文章位置 | 完成进度 |
| --- | --- | --- | --- |
| 1 | BlogLoom Publisher Skill 原理01、为什么用 Skill 连接本地 Markdown 与博客平台 | `docs/开发文档留存/release_1.0.x/05、支持博客系统后台构建skill.md` | `[已完成]` |
| 2 | BlogLoom Publisher Skill 原理02、SKILL.md、SOP 与脚本工具的三层分离 | `docs/工程化开发模块设计思路产物/backend/核心迭代功能设计/skill设计/blogloom-publisher-skill/BlogLoom-Skill设计思路与功能实现.md` | `[已完成]` |
| 3 | BlogLoom Publisher Skill 原理03、REST 原子工具与业务编排工具的统一契约 | `skills/blogloom-publisher-skill/references/00-tool-contract.md`、`scripts/` | `[已完成]` |
| 4 | BlogLoom Publisher Skill 实战01、单篇、目录导入、查询和文章更新 | `skills/blogloom-publisher-skill/SKILL.md`、`scripts/publisher.js` | `[已完成]` |
| 5 | BlogLoom Publisher Skill 原理04、Token 注入、错误输出、幂等和可测试性 | Skill 配置、脚本和测试目录 | `[进行中]` |

Skill 的统一调用链路：

```text
自然语言意图
      ↓
SKILL.md 场景路由
      ↓
references/*.md SOP
      ↓
scripts/*.js 渠道 / 平台工具
      ↓
REST API 或浏览器自动化
      ↓
结构化 JSON 结果 + 可判读错误
```

### 10.2 渠道适配：CSDN 与公众号

| 序号 | 文章名称 | 文章位置 | 完成进度 |
| --- | --- | --- | --- |
| 1 | BlogLoom Publisher Skill CSDN篇 原理01、从统一工具契约到 CSDN 渠道适配 | `BlogLoom-分发Skill之CSDN设计思路与功能实现.md` | `[已完成]` |
| 2 | BlogLoom Publisher Skill CSDN篇 实战01、登录态、编辑器定位、Markdown 与图片处理 | `skills/blogloom-publisher-skill/scripts/channels/csdn/` | `[进行中]` |
| 3 | BlogLoom Publisher Skill 公众号篇 原理01、Markdown 到公众号富文本的转换 | `BlogLoom-分发Skill之公众号(gzh)设计思路与功能实现.md` | `[已完成]` |
| 4 | BlogLoom Publisher Skill 公众号篇 实战01、扫码登录、存草稿、发表和删除 | `skills/blogloom-publisher-skill/scripts/channels/gzh/` | `[进行中]` |
| 5 | BlogLoom Publisher Skill 渠道篇 复盘01、选择器脆弱性、假登录与发表终态判断 | 对应渠道 SOP、测试和维护文档 | `[进行中]` |
| 6 | BlogLoom Publisher Skill 规划01、渠道插件化与更多内容平台适配 | `scripts/channels/` 扩展约定 | `[规划中]` |

渠道分发文章必须明确：

- BlogLoom 内部内容模型是唯一的源数据；渠道适配只负责格式转换和渠道动作；
- 登录态、Cookie、Token 等敏感信息不能进入文章正文、日志或提交记录；
- 浏览器自动化必须有明确的选择器、成功判定、超时、重试和人工接管边界；
- 发布、存草稿、删除等破坏性动作需要显式确认或单独的测试命令；
- 每个渠道都要有独立 SOP 和测试，不能把渠道差异塞进通用脚本。

---

## 十一、第十层：部署、升级、质量与开源协作

> 目标：说明 BlogLoom 如何从本地开发项目变成别人可以安装、升级、备份和排障的开源产品。

### 11.1 Docker 与增量升级

| 序号 | 文章名称 | 文章位置 | 完成进度 |
| --- | --- | --- | --- |
| 1 | BlogLoom部署 实战01、Docker 一键部署的镜像、Compose 与数据目录设计 | `docs/工程化开发模块设计思路产物/运维/0、BlogLoom Docker 一键部署设计方案.md` | `[已完成]` |
| 2 | BlogLoom部署 原理01、容器启动时等待 MySQL 并执行增量 SQL | `docker/container/bin/start.sh`、`upgrade-sql.sh` | `[已完成]` |
| 3 | BlogLoom部署 原理02、为什么要把 MySQL、上传文件、日志和 SQL 记录持久化 | `docker/standalone/docker-compose.yml`、`docker/README.md` | `[已完成]` |
| 4 | BlogLoom部署 实战02、版本升级、回滚、备份与常见故障排查 | `docker/standalone/README.md`、升级脚本 | `[进行中]` |
| 5 | BlogLoom部署 原理03、多架构镜像与 GitHub 发布脚本 | `docs/开发文档留存/release_1.0.x/06、github相关开源优化 & 部署脚本支持多架构.md` | `[已完成]` |

### 11.2 测试、压测与发布规范

| 序号 | 文章名称 | 文章位置 | 完成进度 |
| --- | --- | --- | --- |
| 1 | BlogLoom质量保障 番外01、为什么增量 SQL 必须可重复执行且可追踪 | `sql/increment/`、`bin/local/upgrate-sql.sh` | `[已完成]` |
| 2 | BlogLoom质量保障 番外02、Skill 单测、渠道 Mock 与浏览器自动化测试 | `skills/blogloom-publisher-skill/test/` | `[进行中]` |
| 3 | BlogLoom质量保障 番外03、压力测试、容量测试与结果报告 | `qa/README.md`、`qa/run-stress-test.sh`、`qa/run-capacity-test.sh` | `[已完成]` |
| 4 | BlogLoom开源协作 番外01、CHANGELOG、Issue、贡献指南与安全响应 | `CHANGELOG.md`、`CONTRIBUTING.md`、`SECURITY.md` | `[进行中]` |
| 5 | BlogLoom开源协作 番外02、从版本发布到官网文档同步 | `docs/releases/`、`docs-website/` | `[进行中]` |

---

## 十二、番外篇：设计取舍与长期演进

这一组文章不以某个单独功能为主，而是记录 BlogLoom 在持续开发过程中值得复用的工程判断。

```text
BlogLoom番外01、[已完成] 为什么先做内容闭环，再做主题与生态
BlogLoom番外02、[已完成] 分类、标签、专栏、知识库的职责边界
BlogLoom番外03、[已完成] 文件系统与数据库的一致性：没有分布式事务时怎么办
BlogLoom番外04、[进行中] SPA 博客的 SEO 不是只加几个 meta 标签
BlogLoom番外05、[进行中] 从后台导入到 AI Skill：为什么必须复用已有接口
BlogLoom番外06、[规划中] 个人博客如何在功能丰富和可维护性之间取舍
BlogLoom番外07、[规划中] BlogLoom 与 Headless CMS、静态博客的差异
BlogLoom番外08、[规划中] 主题系统、插件系统与多作者能力的演进边界
BlogLoom番外09、[规划中] 从单体博客到可扩展内容平台的最小演进路径
```

长期演进方向只在实际需求和代码边界清晰后展开，优先级包括：

- 补齐文章版本、草稿和发布审核等内容工作流；
- 增强资源引用分析、孤儿资源清理和备份恢复；
- 完善 SEO 数据监测、搜索能力和服务端输出；
- 将渠道分发抽象为可独立测试、可替换的适配器；
- 在不破坏个人博客简单部署体验的前提下探索主题、插件和多作者能力。

---

## 十三、发布节奏与优先级建议

| 阶段 | 优先专栏 | 目标 |
| --- | --- | --- |
| P0 | 系统架构、内容域、Markdown、资源归档 | 建立项目认知，解释博客最基本的内容闭环 |
| P0 | 知识库、批量迁移、专栏 | 展示 BlogLoom 区别于普通博客的内容整理能力 |
| P1 | CMS、前台阅读、评论与站点配置 | 从源码走到可操作的产品体验 |
| P1 | Publisher Skill、CSDN、公众号 | 展示 AI 协作与多渠道内容分发能力 |
| P1 | SEO、Sitemap、RSS、服务端输出 | 解释内容如何被搜索引擎发现和持续运营 |
| P2 | Docker、增量 SQL、多架构发布 | 让读者可以稳定部署、升级和维护 |
| P2 | 压测、容量测试、开源协作 | 建立项目质量和工程可信度 |
| P3 | 主题、插件、多作者、更多渠道 | 随真实需求逐步扩展，不提前堆叠复杂抽象 |

### 13.1 推荐的首批文章顺序

建议先发布下面 12 篇，读者可以从“项目是什么”一路读到“如何部署和扩展”：

1. `BlogLoom系统架构 原理01、从前后端分离到内容生产闭环`
2. `BlogLoom内容域 原理01、从 Blog 实体到 Blog DTO / VO 的内容模型设计`
3. `BlogLoom Markdown 原理01、从 Markdown 文本到安全 HTML`
4. `BlogLoom资源 原理02、临时上传、文章归档与正文 URL 重写`
5. `BlogLoom知识库 原理03、导入预检、异步任务与进度轮询`
6. `BlogLoom专栏 原理02、两级专栏模型与 blog_column_relation 关系表`
7. `BlogLoom CMS 实战01、文章新建、编辑、发布和回收站操作`
8. `BlogLoom前台 原理03、文章详情页的 Markdown 渲染、目录与图片预览`
9. `BlogLoom SEO 原理02、SPA 博客为什么需要服务端直出与站点级文件`
10. `BlogLoom Publisher Skill 原理02、SKILL.md、SOP 与脚本工具的三层分离`
11. `BlogLoom部署 实战01、Docker 一键部署的镜像、Compose 与数据目录设计`
12. `BlogLoom质量保障 番外03、压力测试、容量测试与结果报告`

### 13.2 每篇文章的固定交付模板

为了让专栏长期保持一致，每篇文章建议包含以下内容：

1. 背景与问题：当前功能解决了什么真实痛点；
2. 方案对比：至少说明一个未采用方案及原因；
3. 核心模型：表结构、DTO、接口或前端状态；
4. 数据流：从用户操作到数据库、文件或外部渠道的完整路径；
5. 源码索引：列出关键类、页面、SQL 和脚本；
6. 可运行实战：给出启动条件、请求示例或页面操作；
7. 边界与失败：重复执行、异常、权限、安全和兼容性；
8. 验证结果：测试、截图、日志或压测数据；
9. 后续演进：明确当前未实现的能力，不把规划项混入现状。

### 13.3 专栏维护规则

- 代码变更先更新对应设计文档，再决定是否新增或调整博客文章；
- 文章标题、文章路径和代码模块保持稳定，重构时增加迁移说明；
- 已完成、进行中和规划中的状态必须随代码真实情况更新；
- 每篇文章尽量提供一个最小可运行示例，避免只停留在概念描述；
- 涉及账号、Token、Cookie、密码和对象存储密钥时，示例只使用脱敏占位符；
- 文章中引用的截图、接口和配置应与当前版本保持一致；
- 当一个功能同时涉及后端、CMS、前台和部署时，明确主文章与交叉引用，避免重复解释。

---

## 十四、核心源码索引

| 能力 | 关键位置 |
| --- | --- |
| 后端入口与配置 | `blog-backend/src/main/java/com/changlu/blogloom/BlogApiApplication.java`、`config/`、`env/` |
| 文章、分类、标签 | `entity/Blog.java`、`service/impl/BlogServiceImpl.java`、对应 Mapper 与 Controller |
| 专栏 | `module/column/` |
| 知识库 | `module/knowledge/` |
| SEO | `module/seo/` |
| Markdown | `util/markdown/` |
| 上传与资源归档 | `service/BlogResourceService.java`、`service/storage/`、`util/upload/` |
| 认证与安全 | `SecurityConfig.java`、`JwtFilter.java`、`JwtLoginFilter.java` |
| 日志与访问统计 | `aspect/`、`entity/*Log.java`、`controller/admin/*Log*Controller.java` |
| 定时任务与缓存 | `task/`、`util/quartz/`、`module/cache/` |
| 管理后台 | `blog-cms-ui/src/` |
| 博客前台 | `blog-view-ui/src/` |
| Publisher Skill | `skills/blogloom-publisher-skill/` |
| Docker 与升级 | `docker/`、`bin/local/`、`sql/increment/` |
| 压测与容量测试 | `qa/` |

> 选题原则：**先有真实能力，再沉淀文章；先讲清边界，再扩展抽象；先保证可运行，再追求完整生态。**
