# 搜索引擎SEO优化调研学习02、第一阶段核心改造、服务端直出与站点级文件

> 系列文章：
> - 上篇：[搜索引擎SEO优化调研学习01、文章页优化、站点变现与数据监测](./搜索引擎SEO优化调研学习01、文章页优化、站点变现与数据监测.md)
> - 本篇：**02、第一阶段核心改造、服务端直出与站点级文件**（BlogLoom 落地篇）

> **本篇定位：** 上篇（01）是"教科书 + 拆解篇"，回答了**"SEO 是什么、别人怎么做、数据在哪看"**；本篇（02）是"落地篇"，把 01 讲到的能力**收敛到 BlogLoom 第一阶段的最小改造范围**，给出可执行的功能需求与工程实现方案。因此，建议先读 01 建立全局认知，再读 02 动手改造；本篇结尾也专门列出与 01 的章节对应关系。

在给开源博客平台 BlogLoom 做搜索引擎优化（SEO）改造时，我们就遇到了这样一个非常具体的问题：

> **辛辛苦苦写完一篇文章，发到自己的博客上，结果在 Google、百度里怎么搜都搜不到。**

不是内容不好，也不是没人访问——而是**搜索引擎根本读不到我们的文章**。这篇文章就围绕这个问题，把 BlogLoom 第一阶段要做的 SEO 改造，从"为什么做"到"怎么做"再到"怎么验证"，完整梳理一遍。

本文的目标是：**初步梳理下一阶段需要优化的核心功能点需求，并逐一对照 BlogLoom 现有系统核心功能，给出可落地的改造设计方案。**

---

# 一、背景引入：为什么 BlogLoom 必须做 SEO

## 1.1、一个真实场景：文章发布了，却搜不到

BlogLoom 是我做的一套开源博客平台：

+ 开源地址：<https://github.com/changluya/BlogLoom>
+ 技术栈：Spring Boot 2.2.7 + MyBatis + Vue 2.6 + Vite
+ 定位：给个人开发者 / 内容创作者自己搭博客用，也方便二次开发
+ 线上站点：<https://blog.changlu.cloud/>

它由三块组成：

| 模块 | 作用 |
| --- | --- |
| `blog-view-ui` | 博客前台（Vue2 单页应用），读者看文章的地方 |
| `blog-cms-ui` | 管理后台（Vue 单页应用），作者写文章的地方 |
| `blog-backend` | 后端服务（Spring Boot 接口 + MyBatis），负责提供数据 |

功能其实已经挺全了：Markdown 写作、分类标签专栏、评论、站点配置、访问统计、Docker 一键部署……**唯独"搜索引擎收录"这块，几乎是空白。**

那么问题来了：**明明文章内容都在数据库里，为什么搜索引擎看不到？**

## 1.2、先搞懂：搜索引擎是怎么工作的

在动手之前，先把搜索引擎的大致工作过程理解清楚，后面每一步改造才有依据。

| 阶段 | 搜索引擎在做什么 | BlogLoom 需要解决什么 |
| --- | --- | --- |
| 发现 | 找到你的文章网址 | 有导航、文章链接和 Sitemap |
| 抓取 | 访问网址，读取页面 | 页面可访问，正文能被读取 |
| 索引（收录） | 分析页面，决定是否放进搜索数据库 | 内容清晰、有价值，减少重复页面 |
| 排名与展示 | 用户搜索时，挑选相关页面展示 | 内容符合搜索需求，网站体验良好 |

**重点：提交网址不等于一定收录，收录也不等于排名靠前。** 第一阶段我们只解决最前面两环——**能被发现、能被抓取**。

## 1.3、实测现状：爬虫眼里的 BlogLoom 是个空壳

光说"没有 SEO"太虚，我们直接访问线上站看看结果。

| 请求 | 返回结果 | 说明 |
| --- | --- | --- |
| `GET https://blog.changlu.cloud/` | 200，但只有一个空壳 | 源码里只有 `<title>BlogLoom</title>` 和一个空的 `<div id="app">`，正文一个字都没有 |
| `GET /robots.txt` | 404 | 没有抓取规则文件 |
| `GET /sitemap.xml` | 404 | 没有站点地图 |
| `GET /rss.xml` | 404 | 没有订阅源 |
| `GET /blog/513`（文章链接） | 404 | 直接打开文章链接，竟然是 404 |
| `GET /blog?id=513`（接口） | 200，有完整正文 | 内容都在，只是没渲染到页面上 |

这里有两个扎心的点：

+ **爬虫拿到的是空壳。** 正文是浏览器执行 JS 后、再用 axios 调接口渲染出来的；爬虫不执行 JS，自然啥也读不到。
+ **连直接打开文章链接都 404。** 前台是 history 路由，但服务器没做 fallback——从首页点进去没问题，可一复制链接发给别人、或者刷新页面，就 404 了。

请求链路大概是这样的：

```plain
浏览器 / 爬虫
   ↓
blog-view-ui 的 index.html（空壳，<title>BlogLoom</title>）
   ↓ 浏览器执行 JS
axios 调 blog-backend 的 /blog 接口
   ↓
Vue 在浏览器里渲染正文，用 document.title 改标题
```

结果就是，爬虫和真实用户看到的完全是两个世界：

| 谁在看 | 看到的东西 |
| --- | --- |
| 爬虫（不执行 JS） | `<title>BlogLoom</title>` + 空 `div`，没有正文，也没有描述 |
| 用户（浏览器） | 正文、标题、评论，全都正常显示 |

**一句话：不是内容不够好，是搜索引擎根本读不到。** 这就是眼下最大的困局，也是必须第一个解决的地方。

## 1.4、本阶段的范围与目标

改 SEO 也得有个盼头。我们要的其实就三件事，而且有先后顺序：

| 优先级 | 目标 | 怎么判断做到了 |
| --- | --- | --- |
| ① | **能被收录** | 在 Google / Bing / 百度搜 `site:blog.changlu.cloud`，能看到我的文章 |
| ② | **做好 SEO** | 每篇文章有独立的标题、摘要、canonical、结构化数据；站点有 sitemap / robots / rss / 分享卡片 |
| ③ | **能看数据** | Search Console 看收录和搜索词，GA4 / Umami 看流量来源，形成每周复盘的习惯 |

围绕这个目标，本文按阶段划分范围：

| 阶段 | 主题 | 是否属于本文范围 |
| --- | --- | --- |
| 一阶段 | 内容页服务端直出 + 页面级 meta + 站点级文件（robots / sitemap / rss）+ 收录控制 | ✅ 本文 |
| 一阶段 | 数据监测接入（Search Console / GA4 或 Umami） | ✅ 本文（偏配置，少量代码） |
| 二阶段 | 阅读页性能（Core Web Vitals）、站内互链深化、主动推送 | ⏳ 后续 |
| 三阶段 | 变现（AdSense / CPS / 赞助） | ⏳ 后续 |
| 四阶段 | GEO（`geo:*` 元信息 + 可信度建设） | ⏳ 后续 |

**重点：先能被抓，才谈得上收录；先被收录，数据才有得看。** 所以阶段一、二的技术底座必须先做扎实。

---

# 二、核心概念讲解：一阶段要补哪些能力

## 2.1、八项基础能力总览

SEO 不是一个单点功能，而是一套"内容基础设施"。对博客平台来说，核心就是下面这 8 项能力：

| 能力 | 解决问题 | BlogLoom 要做什么 |
| --- | --- | --- |
| 独立 URL | 每篇文章有稳定地址 | 后端提供 `/blog/{id}` 内容页 |
| `<title>` | 告诉搜索引擎这页讲什么 | 每页唯一标题（文章标题 + 站点名） |
| `description` | 页面摘要 | 输出 `<meta name="description">` |
| canonical | 同一内容多网址时声明首选 | 输出无参数的标准 URL |
| 结构化数据 | 让搜索理解内容类型、作者、时间 | 输出 `BlogPosting` JSON-LD |
| Open Graph | 分享到社交平台的卡片 | 输出 `og:title/description/image/url` |
| Sitemap / robots / RSS | 发现、抓取、订阅 | 提供三个站点级文件 |
| 正文直出 | 爬虫能读到正文 | 服务端渲染进首屏 HTML |

## 2.2、能力与现状对照

那么，BlogLoom 现在到底有哪些、缺哪些？我们对照真实代码看一遍。

| 调研文档要求的能力 | BlogLoom 现状 | 关键代码位置 | 缺口 |
| --- | --- | --- | --- |
| 独立 URL | 有 `/blog/:id`（前端路由），后端无同名内容页 | `blog-view-ui/src/router/index.js:30-35` | 后端无服务端直出页面 |
| 独立 `<title>` | 仅靠 JS 改 `document.title` | `views/blog/Blog.vue:180`、`views/Index.vue:134` | 首屏 HTML 固定为 `BlogLoom` |
| `description` | 文章实体有此字段，但**详情接口不返回** | `entity/Blog.java:26`；`model/vo/BlogDetail.java:23-39` | `BlogDetail` 无 `description` |
| canonical | 无 | — | 全缺 |
| 结构化数据 | 无 | — | 全缺 |
| Open Graph | 无 | — | 全缺 |
| robots / sitemap / rss | 无（均 404） | — | 全缺 |
| 正文直出 | 否（`v-html` 客户端渲染） | `views/blog/Blog.vue:34` | 首屏无正文 |
| 聚合页可抓取 | 标签/分类/归档均为 SPA | `views/tag/Tag.vue` 等 | 爬虫读不到 |
| 收录控制 | `/cms/`、`/admin/` 无 `noindex` | — | 可能被误收录 |

## 2.3、核心技术约束（改造前必须知道）

**注意：**下面几条约束会直接决定改造方案，务必先看清楚。

| 序号 | 约束 | 说明 |
| --- | --- | --- |
| 1 | SPA 回退过滤器会"抢"请求 | `SpaForwardFilter` 以 `Integer.MIN_VALUE` 顺序拦截所有请求，`/blog/1` 这类无扩展名 GET HTML 请求会被**先转发到 `index.html`**，根本进不了后端 Controller |
| 2 | `.xml` / `.txt` 端点不受影响 | 过滤器对"最后一段带扩展名"的请求不回退（`SpaForwardConfig.java:85-87`），所以 `/sitemap.xml`、`/rss.xml`、`/robots.txt` 可直接由 Controller 提供 |
| 3 | 公开接口全部免鉴权 | `SecurityConfig` 中 `anyRequest().permitAll()`（`config/SecurityConfig.java:56`），新增 SEO 端点默认公开 |
| 4 | 无 `slug` 字段 | `blog` 表及实体均无 slug，路由用数字 id，一阶段可继续用 id |
| 5 | 详情接口缺字段 | `BlogDetail` 缺 `firstPicture`、`description`、作者信息，而 `blog` 表里都有 |
| 6 | 同源部署 | 生产环境 API 用相对路径（`VITE_API_URL=/`、`/admin/`），meta 里的 URL 需用 `blog.view` 配置拼接真实域名 |

**重点：**约束 1 是最容易被忽略、也最致命的坑。如果不改过滤器，后面写再多 Controller 都不会生效。

---

# 三、实现思路：方案选型与总体架构

## 3.1、服务端渲染的三条路线对比

既然核心问题是"爬虫读不到"，那就要让页面**在服务端就能输出正文**。这里有三条路线，各有取舍。

| 路线 | 做法 | 优点 | 代价 | 适合 |
| --- | --- | --- | --- | --- |
| **方案一：后端直出** | 后端用 Thymeleaf 渲染 `/blog/{id}`，返回带正文与 meta 的 HTML | 最贴合现有 Spring Boot（项目已依赖 thymeleaf）；收录效果最好、最稳 | 详情/列表页要从 SPA 分出来，改后端模板 | **推荐主路线** |
| **方案二：预渲染 / 动态渲染** | 部署时或对爬虫 UA 用无头浏览器渲染后返回 HTML | 前端基本不改，能当过渡 | 需额外服务；内容更新要重渲染；有 cloaking 风险 | 短期过渡 |
| **方案三：迁移 SSR 框架** | 改用 Nuxt / Next 等 SSR 框架 | 体验与 SEO 兼顾 | Vue2 迁移成本大 | 长期重构时 |

**弊端说明：**

+ 方案二对爬虫返回一份内容、对用户返回另一份内容，属于"动态渲染"。Google 已明确表示**动态渲染只是过渡方案**，长期仍应使用服务端渲染或静态生成，且处理不当可能被判定为 cloaking。
+ 方案三虽然最优雅，但要重写整个前端，对本项目（Vue2）来说一阶段成本过高，不划算。

## 3.2、为什么选后端 Thymeleaf 直出

那么，我们的选择是什么？**直接采用方案一，并且不做 UA 嗅探。**

理由有三：

1. **零新增依赖**：`spring-boot-starter-thymeleaf` 已经在 `pom.xml:120-124`，目前只用来发邮件，能力现成。
2. **收录最稳**：内容页对读者和爬虫返回**同一份**服务端渲染 HTML，符合搜索引擎规范。
3. **改动可控**：只需把"需要被收录的页面"交给后端，后台管理（`blog-cms-ui`）、登录等继续用 SPA 即可。

**重点：内容页对读者和爬虫返回同一份 HTML，绝不能分成两份，否则就有 cloaking 风险。**

## 3.3、目标请求链路

改造后，一个请求会这样流动：

```plain
浏览器 / 爬虫
   │
   ├─ GET /blog/513
   │        ↓
   │   SpaForwardFilter：命中"服务端直出放行名单" → 不转发
   │        ↓
   │   SeoPageController.getBlogPage()：查库 → 注入 model
   │        ↓
   │   Thymeleaf 渲染 seo/blog.html（首屏含正文 + title/description/canonical/OG/JSON-LD）
   │        ↓
   │   爬虫拿到完整 HTML；读者看到文章（可嵌入评论等轻交互）
   │
   ├─ GET /sitemap.xml   → SeoFileController（produces=application/xml）
   ├─ GET /rss.xml       → SeoFileController
   └─ GET /robots.txt    → SeoFileController（text/plain）
```

## 3.4、新增代码结构

**统一约定：后续所有 SEO 相关代码，全部放在 `blog-backend/src/main/java/com/changlu/blogloom/module` 下的独立 `seo` 模块中**，与现有 `module/column`、`module/knowledge` 保持一致的 `api / service / domain / support` 分层，不再散落到顶层 `controller`、`service`、`model` 等包。

```plain
blog-backend/src/main/java/com/changlu/blogloom/module/seo/
├── api/                               # 接口层（Controller，对外提供 HTTP / 视图）
│   ├── SeoConfigController.java       # 后台 SEO 配置（GET/POST /admin/seo/config）✅ 已落地
│   ├── SeoVerificationController.java # 后台 SEO 平台关联（GET/POST /admin/seo/verification）✅ 已落地
│   ├── SpaIndexController.java        # SPA 入口注入站点验证 meta（方案 B）✅ 已落地
│   ├── SeoPageController.java         # 内容页直出（@Controller，返回 Thymeleaf 视图）
│   └── SeoFileController.java         # robots / sitemap / rss（@RestController，产出文本/XML）
├── service/                           # 业务层
│   ├── SeoMetaService.java            # 组装页面 SEO 元数据（title/description/canonical/og/ldJson）
│   ├── SitemapService.java            # 生成 sitemap.xml
│   └── RssService.java                # 生成 rss.xml
├── domain/                            # 领域模型
│   ├── SeoConfig.java                 # 后台 SEO 配置载体（seoDomain 等）✅ 已落地
│   ├── SeoVerification.java           # 站点验证载体（baidu/bing/google）✅ 已落地
│   └── SeoMeta.java                   # 公共 meta 载体（title/description/canonical/og/ldJson）
├── support/                           # 辅助工具
│   ├── SeoUrlResolver.java            # 统一 canonical / 绝对 URL 拼接（读后台 seoDomain，兜底 blog.view）
│   └── SeoMetaBuilder.java            # BlogDetail → SeoMeta
└── dao/                               # 如插件需要独立查询（可选）

blog-backend/src/main/resources/templates/seo/   # Thymeleaf 模板仍放 resources 下
├── layout.html                        # 公共骨架（header/footer）
├── blog.html                          # 文章详情页
├── list.html                          # 列表/标签/分类/归档通用页
└── fragments/
    └── seo-head.html                  # title/description/canonical/og/JSON-LD 片段
```

**为什么这样归类：**

| 原因 | 说明 |
| --- | --- |
| 与现有架构一致 | `column`、`knowledge` 都是 `api/dao/domain/service/support`，SEO 沿用同一套分层，降低理解成本 |
| 职责内聚 | SEO 的控制器、业务、模型、工具都收在 `module/seo` 一处，便于查找与维护 |
| 便于演进 | 后续加 SEO 后台配置（F15）、站点设置等，都继续往 `module/seo` 里加，不污染其它模块 |
| 模板例外 | Thymeleaf 按约定从 `classpath:/templates/` 解析，故模板放在 `resources/templates/seo/`，不放 java 包内 |

> 一句话：**Java 代码进 `module/seo`，模板进 `resources/templates/seo`。**

## 3.5、数据流：一次建模、全站复用

**重点：**所有 meta 都从文章数据这一份源头产生，模板里不写死任何标题或描述。

```plain
blog 表（已有字段）
   ├─ title        → <title> / og:title / JSON-LD headline
   ├─ description  → meta description / og:description
   ├─ first_picture→ og:image / JSON-LD image
   ├─ create_time  → article:published_time / datePublished
   ├─ update_time  → article:modified_time / dateModified / sitemap lastmod
   ├─ category     → article:section / 分类聚合页
   ├─ tags         → article:tag / 标签聚合页
   └─ id           → canonical / og:url / sitemap loc
```

**三条原则贯穿全文：**

1. **数据驱动模板**：文章数据 → 一个公共模板片段 → 生成全部 meta，不硬编码、不落字段。
2. **服务端直出**：正文和 meta 都在首屏 HTML 里，不依赖 JavaScript。
3. **一次建模、全站复用**：`title / summary / cover / 时间 / 标签` 在数据库定一次，详情页、Sitemap、RSS、OG 共用。

---

# 四、核心功能点需求梳理

## 4.1、优先级总表

改造不能一锅端，要有优先级。下面把 14 项功能点按"优化类型"拆成 6 组，逐组列出**核心优化点**（核心改在哪里）、目标与验收标准。

**第一组：内容直出与数据地基（P0）**

| 编号 | 功能点 | 优先级 | 核心优化点（改在哪里） | 目标 | 验收标准 |
| --- | --- | --- | --- | --- | --- |
| **F1** | 数据层补齐 | P0 | `BlogDetail` VO 增加字段 + `BlogMapper.xml` 详情查询补列并 join 作者 | 让详情数据能支撑全部 meta | `BlogDetail` 返回 `firstPicture`、`description`、作者 |
| **F2** | 文章详情页服务端直出 | P0 | 新增 `SeoPageController` + 修改 `SpaForwardConfig` 放行 + Thymeleaf 模板 `seo/blog.html` | `curl /blog/{id}` 拿到正文 + 正确 title | 禁用 JS 后正文与 meta 仍在源码中 |

**第二组：页面级 SEO 元信息（P0）**

| 编号 | 功能点 | 优先级 | 核心优化点（改在哪里） | 目标 | 验收标准 |
| --- | --- | --- | --- | --- | --- |
| **F3** | 页面级 SEO meta | P0 | 公共片段 `templates/seo/fragments/seo-head.html` 输出 `title` / `description` | title / description | 每页唯一 title；description 非空 |
| **F4** | canonical 与 URL 策略 | P0 | `SeoUrlResolver` 统一拼接绝对 URL（读 `blog.view`） | 同一内容声明首选网址 | canonical 指向无参数标准 URL |
| **F5** | 结构化数据 JSON-LD | P0 | `SeoMetaBuilder` 生成 `BlogPosting`，在 `seo-head.html` 输出 | `BlogPosting` 让搜索理解文章 | 富媒体测试工具校验通过 |
| **F6** | Open Graph | P0 | `seo-head.html` 内的 `og:*` 与 `article:*` 标签 | 分享卡片正确 | 微信/QQ/Slack 分享出现标题+摘要+封面 |

**第三组：站点级收录文件（P0）**

| 编号 | 功能点 | 优先级 | 核心优化点（改在哪里） | 目标 | 验收标准 |
| --- | --- | --- | --- | --- | --- |
| **F7** | robots.txt | P0 | 新增 `SeoFileController.robots()` | 提供抓取规则并声明 Sitemap | `/robots.txt` 200，含 Sitemap 行 |
| **F8** | sitemap.xml | P0 | `SeoFileController.sitemap()` + 公开文章查询 | 列出全部公开文章 | `/sitemap.xml` 200，仅含公开已发布文章 |
| **F9** | RSS/Atom 订阅 | P1 | `SeoFileController.rss()` | 读者可订阅、阅读器可发现 | `/rss.xml` 200，HTML 中有 `<link rel="alternate">` |

**第四组：聚合页可抓取（P1）**

| 编号 | 功能点 | 优先级 | 核心优化点（改在哪里） | 目标 | 验收标准 |
| --- | --- | --- | --- | --- | --- |
| **F10** | 聚合页可抓取 | P1 | `SeoPageController` 复用通用模板 `seo/list.html` | 列表/标签/分类/归档页直出 | `curl /tag/{name}` 能看到文章列表 |

**第五组：收录控制与数据监测（P1）**

| 编号 | 功能点 | 优先级 | 核心优化点（改在哪里） | 目标 | 验收标准 |
| --- | --- | --- | --- | --- | --- |
| **F11** | 收录控制 | P1 | `blog-cms-ui/index.html` 加 `noindex` + `/cms`、`/admin` 响应头 `X-Robots-Tag` | 防止后台被收录 | `/cms/` 与 `/admin/` 输出 `noindex` |
| **F12** | 数据监测接入 | P1 | Search Console + GA4 统计脚本注入公共片段 | 能看收录与流量 | Search Console 验证成功；GA4 有数据 |
| **F15** | SEO 后台配置支持 | P1 | 后台站点设置新增 SEO 域名（`seoDomain`）、GA4 统计脚本、事件上报开关；后端按配置注入 | SEO 域名 / 统计脚本 / 事件上报均可后台配置 | 后台填 SEO 域名即用于 canonical/sitemap；填 GA4 ID 即生效；开关控制事件上报 |

**第六组：加速与性能优化（P2）**

| 编号 | 功能点 | 优先级 | 核心优化点（改在哪里） | 目标 | 验收标准 |
| --- | --- | --- | --- | --- | --- |
| **F13** | 主动推送（可选） | P2 | 接入百度 `push.js` / 必应 IndexNow | 加速收录 | 百度/必应推送有成功记录 |
| **F14** | 阅读页性能 | P2 | 正文图片懒加载 + `index.html` 第三方脚本异步 | LCP/CLS 达标 | 图片懒加载、第三方脚本异步 |

## 4.2、一阶段最小闭环

那么，哪些是必须先做的？答案是：

> **F1 + F2 + F3 + F4 + F5 + F6 + F7 + F8**

做完这八项，就能让爬虫读到内容并提交收录。F9～F12、F15 紧随其后，F13、F14 作为收尾优化。

## 4.3、核心功能实现原理与改造要点

前面 4.1、4.2 讲的是"要做什么"，这一节讲"**为什么这么做、底层到底怎么跑、真正动手时重点在哪**"。理解了原理，第五章的代码才不是照抄——遇到问题时也知道该改哪一环。

### 4.3.1、爬虫读不到的本质：CSR 与 SSR 的渲染差异

**实现原理：**

网页正文有两条产出路径：

| 渲染方式 | 正文在哪里生成 | 爬虫何时能看到 |
| --- | --- | --- |
| **CSR（客户端渲染）** | 浏览器下载 JS 后，由 JS 调接口、再把数据塞进 DOM | 必须执行完 JS 才看得到 |
| **SSR（服务端渲染）** | 服务器直接把数据和模板拼成完整 HTML 返回 | 打开响应源码就能看到 |

BlogLoom 是典型的 CSR。真实流程如下：

```plain
爬虫 GET /blog/513
   ↓
到时返回 static/view/index.html（空壳，<title>BlogLoom</title>，<div id="app"></div>）
   ↓
（爬虫通常不执行 JS，流程到此为止 → 空壳）
   ↓ 若是浏览器
下载并执行 JS → Vue 挂载 → axios 调 GET /blog?id=513 → 拿到 JSON → v-html 渲染 → document.title 改标题
```

**重点：**问题不在数据，而在**数据出现在哪一层**。接口 `GET /blog?id=513` 返回的 JSON 里正文是完整的（`BlogController.java:62-97` 返回 `Result(BlogDetail)`），只是它没被拼进首屏 HTML。

**如何优化改造：**

把"需要被收录的页面"从 CSR 切成 SSR——由后端 `@Controller` + Thymeleaf 把 `BlogDetail` 直接渲染进 HTML 首屏。这样爬虫拿到的就是完整正文，与是否执行 JS 无关。

**用例对比（输入相同，输出不同）：**

+ **输入：**`GET /blog/513`，请求头 `Accept: text/html`
+ **改造前输出（CSR 空壳）：**

```html
<head><title>BlogLoom</title></head>
<body><div id="app"></div></body>
```

+ **改造后输出（SSR 直出）：**

```html
<head>
  <title>Java基础学习笔记 09、IO流 - BlogLoom</title>
  <meta name="description" content="从 File 类入手，系统讲解 IO 流……">
</head>
<body>
  <h1>Java基础学习笔记 09、IO流</h1>
  <div class="content">……完整正文……</div>
</body>
```

> 同一个输入，一个输出里没有正文，一个首屏就是完整文章——这就是收录成败的分水岭。

### 4.3.2、服务端直出：请求生命周期与 SPA 回退冲突

**实现原理：**

同一个请求在 Spring Boot 里可能经过三道关，顺序决定成败：

```plain
HTTP 请求
   ↓ ① Servlet Filter（顺序 = Integer.MIN_VALUE，最先执行）
SpaForwardFilter：命中规则则 forward 到 /index.html
   ↓ ② DispatcherServlet 路由
@Controller 映射（SeoPageController）
   ↓ ③ 视图解析
ThymeleafViewResolver → 渲染 templates/seo/blog.html
```

**关键冲突（必须理解）：**

`SpaForwardConfig` 的过滤器**先于 Controller 执行**，它对"GET + Accept 含 text/html + 路径无扩展名"的请求一律转发到 `index.html`（`SpaForwardConfig.java:71-88`）。也就是说——**如果不动它，`/blog/513` 永远到不了我们新写的 Controller**，`SeoPageController` 只是摆设。

**改造要点（三点）：**

1. 增加"服务端直出放行名单"（`SSR_PREFIXES`），命中即 `return false` 不转发，把请求交回 Controller。
2. 放行判定仍要保留"仅 HTML 导航"的约束：只放行 `Accept: text/html`、无扩展名的 GET，避免误伤 `/blogs`、`/blog?id=` 等 JSON 接口。
3. 带扩展名的路径（`sitemap.xml` / `rss.xml` / `robots.txt`）本身就不会被回退，可直接由 Controller 命中，无需进放行名单。

**重点：**这是整个改造的"总闸"。顺序错了，后面所有页面直出都不会生效。

**用例对比（三类请求的输入 → 输出）：**

| 输入请求 | 改造前输出 | 改造后输出 |
| --- | --- | --- |
| `GET /blog/513`（`Accept: text/html`） | 被 forward 到 `index.html` 空壳 | `SeoPageController` 渲染的完整文章 HTML |
| `GET /blog?id=513`（`Accept: application/json`） | `Result(BlogDetail)` JSON | 不变，仍是 JSON（接口不受影响） |
| `GET /sitemap.xml`（带扩展名） | 不由过滤器处理 | 直接命中 `SeoFileController` 输出 XML |

> 关键点：放行名单只接管"HTML 导航"这一类请求，接口 JSON 与站点级文件都不受影响。

### 4.3.3、元信息数据流：从数据库到 `<head>`

**实现原理：**

`title` / `description` / `canonical` / OG / JSON-LD 都写在 HTML 的 `<head>` 里，搜索引擎**只解析响应的 HTML 源码**，不执行 JS（尤其百度、必应对 JS 支持弱）。因此元信息必须在服务端就生成好。

数据流如下（一次建模、全站复用）：

```plain
blog 表
  ├─ title / description / first_picture / create_time / update_time
  ├─ category / tags / user_id
  ↓ BlogMapper.xml（详情查询，需补列 + join 作者）
BlogDetail（VO，需补 firstPicture / description / author）
  ↓ SeoMetaBuilder（+ SeoUrlResolver 拼绝对 URL、拼 webTitleSuffix 后缀）
SeoMeta（title / description / canonicalUrl / image / publishedTime / ...）
  ↓ Thymeleaf 公共片段 seo-head.html
HTML <head>
```

**canonical 的原理：**同一篇文章可能有多个 URL（`/blog/513`、`?utm_source=xx`、`?from=home`）。`rel="canonical"` 是在告诉搜索引擎"这一堆地址对应的首选版本是哪个"，从而**合并重复内容的权重信号**。它只是"信号"，不会让浏览器跳转，也不保证引擎一定采纳。

**改造要点：**

+ 详情接口 `BlogDetail` 当前**缺 `description` 和 `firstPicture`**（`BlogDetail.java:23-39`、`BlogMapper.xml:312-319` 都没查），必须先补，否则 meta/OG 无数据可填。
+ 域名不能硬编码，统一由 `SeoUrlResolver` 取后台 SEO 域名 `seoDomain`（兜底 `blog.view`）拼接绝对 URL，保证 canonical / `og:url` / sitemap `loc` 三者一致。
+ `title` 后缀复用站点配置 `webTitleSuffix`，与现有 `document.title` 逻辑保持同源。

**用例（输入一篇文章数据 → 输出 head 元信息）：**

输入（`blog` 表一行）：

```plain
id=513
title=Java基础学习笔记 09、IO流
description=从 File 类入手，系统讲解 IO 流……
first_picture=https://pictured-bed.oss-cn-beijing.aliyuncs.com/cover/513.png
create_time=2026-10-01 10:00:00
update_time=2026-10-02 12:00:00
category=Java
tags=[Java, IO流]
author=长路
```

输出（渲染进 `<head>`）：

```html
<title>Java基础学习笔记 09、IO流 - BlogLoom</title>
<meta name="description" content="从 File 类入手，系统讲解 IO 流……">
<link rel="canonical" href="https://blog.changlu.cloud/blog/513">
<meta property="og:image" content="https://pictured-bed.oss-cn-beijing.aliyuncs.com/cover/513.png">
<meta property="og:url" content="https://blog.changlu.cloud/blog/513">
```

**canonical 用例：**访问 `/blog/513?utm_source=csdn&from=home`，输出里的 canonical 仍是干净的 `https://blog.changlu.cloud/blog/513`。

### 4.3.4、结构化数据（JSON-LD）的生成原理

**实现原理：**

结构化数据 = 用搜索引擎能懂的统一词汇（Schema.org）描述内容。JSON-LD 是最被推荐的承载格式，放在 `<script type="application/ld+json">` 里。搜索引擎解析后，知道"这是一篇 `BlogPosting`，作者是谁、什么时候发布/修改、讲什么"，从而更好地理解内容、并可能展示富媒体结果。

生成流程：

```plain
BlogDetail
  ↓ SeoMetaBuilder 用 Jackson ObjectMapper 组装 ObjectNode
{ "@context": "...", "@type": "BlogPosting", "headline": ..., "author": ..., "datePublished": ... }
  ↓ ObjectMapper.writeValueAsString 序列化（避免手拼转义）
  ↓ 注入 seo-head.html 的 <script type="application/ld+json">
```

**重点：**

+ 用 `ObjectMapper` 序列化，**不要手工字符串拼接** JSON（引号/中文/HTML 转义极易出错）。
+ `dateModified` 必须取真实的 `update_time`，**不能每次刷新时间假装有更新**；信息也要与页面可见内容一致。
+ `@type` 选 `BlogPosting`（`schema.org` 里 `BlogPosting` 是 `Article` 的子类型）。

**用例（输入 `BlogDetail` → 输出 JSON-LD）：**

```html
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "BlogPosting",
  "headline": "Java基础学习笔记 09、IO流",
  "description": "从 File 类入手，系统讲解 IO 流……",
  "image": "https://pictured-bed.oss-cn-beijing.aliyuncs.com/cover/513.png",
  "datePublished": "2026-10-01T10:00:00+08:00",
  "dateModified": "2026-10-02T12:00:00+08:00",
  "inLanguage": "zh-CN",
  "keywords": ["Java", "IO流"],
  "author": { "@type": "Person", "name": "长路" },
  "mainEntityOfPage": "https://blog.changlu.cloud/blog/513"
}
</script>
```

> 校验方式：把这段贴进 Google 富媒体结果测试工具 / Schema 校验器，应无错误。

### 4.3.5、站点级文件（robots / sitemap / rss）的协议原理

**实现原理与流程：**

| 文件 | 依据协议 | 作用 | 生成/更新时机 |
| --- | --- | --- | --- |
| `robots.txt` | RFC 9309 | 告诉爬虫哪些路径可抓 | 站点级，基本固定 |
| `sitemap.xml` | Sitemaps.org 协议 | 列出公开页面，辅助发现抓取 | 发布/修改/删除文章后应同步 |
| `rss.xml` | RSS 2.0 | 供读者/聚合器订阅 | 发布新文章时更新（输出最近约 20 篇） |

BlogLoom 侧的关键机制：

+ 三者都是**带扩展名的路径**，`SpaForwardFilter` 不会回退它们，所以可由 `SeoFileController` 直接产出（`produces` 分别设 `text/plain` / `application/xml` / `application/rss+xml`）。
+ `sitemap.xml` 必须**动态生成**：从 `blog` 表查 `is_published=1 and is_deleted=0 and password 为空`，与 canonical 用同一套 URL 规则。
+ `rss.xml` 由文章页 `<link rel="alternate" type="application/rss+xml">` 声明自动发现（写进公共片段即可）。

**改造要点：**

+ **不要**在 robots 里 `Disallow: /blog`——BlogLoom 接口与内容页都在根路径，会误屏蔽文章。
+ 私密/草稿/密码文章**一律不进 sitemap 与 rss**，但这只是"不主动暴露"，真正的保护仍靠鉴权。
+ `lastmod` 用真实 `update_time`；sitemap `loc` 与 canonical 完全一致。

**用例（三个端点各自的输入 → 输出）：**

+ **`GET /sitemap.xml`**（输入：无参）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    <loc>https://blog.changlu.cloud/blog/513</loc>
    <lastmod>2026-10-02</lastmod>
  </url>
  <!-- ……其余公开文章，草稿/私密/密码文章不出现 …… -->
</urlset>
```

+ **`GET /robots.txt`**（输入：无参）：

```plain
User-agent: *
Disallow: /admin/
Disallow: /cms/
Disallow: /login
Allow: /

Sitemap: https://blog.changlu.cloud/sitemap.xml
```

+ **`GET /rss.xml`**（输入：无参，取最近约 20 篇公开文章）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
  <channel>
    <title>长路的博客</title>
    <link>https://blog.changlu.cloud</link>
    <item>
      <title>Java基础学习笔记 09、IO流</title>
      <link>https://blog.changlu.cloud/blog/513</link>
      <guid>https://blog.changlu.cloud/blog/513</guid>
      <pubDate>Thu, 01 Oct 2026 10:00:00 +0800</pubDate>
      <description>从 File 类入手，系统讲解 IO 流……</description>
    </item>
  </channel>
</rss>
```

**Sitemap 的规模与上限（BlogLoom 几百篇无需担心）：**

Sitemaps 协议对**单个 sitemap 文件**有两条硬限制：

| 限制 | 上限 | 未超限 | 超过时 |
| --- | --- | --- | --- |
| URL 条数 | 50,000 条 | 一个 `sitemap.xml` 全量列出 | 拆成多个子 sitemap，再用 `sitemapindex` 汇总 |
| 文件大小 | 未压缩 50MB | 同上 | 同上 |

BlogLoom 当前规模（五六百篇）：

+ **数量**：约 500～600 条 URL，远低于 5 万上限。
+ **体积**：每条 `<url>` 约 100 字节，整份约几十 KB，远低于 50MB。
+ **结论**：单个 `sitemap.xml` 全量输出即可，**无需分页、无需 `sitemapindex`**。

当前实现的范围（与 5.1.3.2 一致）：

+ **包含**：`/home`、`/archives` 两个静态页 + 全部公开文章（`is_published=1 and is_deleted=0 and password 为空`）。
+ **排除**：草稿、回收站、密码保护文章。
+ **暂未加入（可选增强）**：`/tag/{name}`、`/category/{name}`、`/column/{id}` 等聚合页。

**优化建议（非必须）：**sitemap 实际只用到 `id` 与 `update_time`，可为它单独用一个更轻的查询（当前 `SeoArticleMapper` 顺带 `select` 了 `title/description/first_picture`）；几百篇下可忽略，数据量上万时再考虑裁剪列或加缓存。

### 4.3.6、抓取与收录：收录控制的原理与搜索引擎流程

**先分清两个概念（最重要）：**

| 指令 | 作用 | 写在哪里 | 前提 |
| --- | --- | --- | --- |
| `Disallow` | 阻止**抓取** | `robots.txt` | — |
| `noindex` | 阻止**收录** | 页面 `<meta name="robots">` 或响应头 `X-Robots-Tag` | **必须先允许抓取**，引擎才读得到 |

**原理：**搜索引擎读不读得到 `noindex`，取决于它**能不能抓取**这个页面。所以对一个既能抓又不该被收录的页面（比如 `/cms/` 后台），正确做法是"允许抓取 + 页面输出 noindex"；如果又 Disallow 又指望它读 noindex，逻辑上是矛盾的。

**搜索引擎收录的完整流程（以 Google 为主）：**

```plain
① 发现：站内链接 / 外链 / Sitemap / 手动提交 URL
   ↓
② 抓取：Googlebot 按抓取预算取回 HTML
   ↓
③ 渲染：Googlebot 用 Chromium 渲染 JS（有排队延迟，非实时）
   ↓
④ 索引：内容质量、重复度、canonical、noindex 综合判定
   ↓
⑤ 展示/排名：用户搜索时排序展示
```

**重点：**Google 会渲染 JS，但**渲染是延迟的、有预算的**；百度、必应对 JS 渲染支持更弱。所以依赖 CSR"等 Googlebot 渲染"并不可靠——**服务端直出能让流程在第 ② 步就拿到正文**，收录更快更稳。

**如何推动与监测收录（按搜索引擎分开说明）：**

三个主流引擎的官方入口与提交方式如下：

| 搜索引擎 | 官方平台（入口网址） | 提交 / 推送方式 | 关键说明 |
| --- | --- | --- | --- |
| **Google** | [Search Console](https://search.google.com/search-console/) | ① 提交 `sitemap.xml`；② "网址检查 → 请求编入索引" | **无开放的主动推送 API**，也不采用 IndexNow；靠 Sitemap + 内链 + 手动请求 |
| **百度** | [百度搜索资源平台](https://ziyuan.baidu.com/) | ① 手动提交；② API 主动推送；③ `push.js` 自动推送；④ 提交 Sitemap | 国内收录主战场，`push.js` 让访客浏览即自动推送 |
| **Bing** | [Bing Webmaster Tools](https://www.bing.com/webmasters/) | ① 提交 `sitemap.xml`；② **IndexNow** 主动推送；③ 手动提交 URL | IndexNow 一次提交，Bing / Yandex 等多平台共享 |

**① Google：**
+ 在 Search Console 用 DNS TXT 验证 → 提交 `sitemap.xml` → 新文章用"网址检查"请求编入索引。
+ **注意：**Google 已停用 sitemap ping 端点，也**不支持 IndexNow**，所以别指望"推送 API"；Sitemap + 站内内链 + Search Console 手动请求才是正道。

**② 百度：**
+ 在百度搜索资源平台验证站点后，可用三种提交方式：
  + **手动提交**：后台逐个粘贴 URL（适合少量）；
  + **API 主动推送**：发布文章时服务端 POST 到推送接口（最快、最实时）；
  + **自动推送 `push.js`**：页面被访问时自动推送（接入最省事）。

```html
<!-- 百度自动推送：放进公共 head 片段或 blog-view-ui/index.html -->
<script src="https://zz.bdstatic.com/linksubmit/push.js"></script>
```

**③ Bing：**
+ 在 Bing Webmaster Tools 验证站点 → 提交 `sitemap.xml`。
+ 接入 **IndexNow**：在站点根目录放一个 `<你的key>.txt`（内容即 key），发布文章时 POST 到 IndexNow 接口：

```json
POST https://api.indexnow.org/indexnow
{
  "host": "blog.changlu.cloud",
  "key": "你的key",
  "keyLocation": "https://blog.changlu.cloud/你的key.txt",
  "urlList": ["https://blog.changlu.cloud/blog/513"]
}
```

**通用建议：**

1. 三个引擎都**先提交 Sitemap**——这是覆盖面最广的"发现"入口。
2. 服务端直出后，`/blog/{id}` 源码即有正文，收录更快更稳。
3. 读懂"已发现 / 已抓取 - 尚未编入索引"等状态，多数是时效与权重问题，别盲目改代码。

**用例对比（谁能被抓、谁能被收录）：**

| 输入 | 响应头 / HTML 输出 | 结果 |
| --- | --- | --- |
| `GET /cms/` | 头：`X-Robots-Tag: noindex, nofollow`；HTML：`<meta name="robots" content="noindex, nofollow">` | 可抓取，但**不收录** |
| `GET /admin/...` | `robots.txt` 中已 `Disallow`；接口返回 401/JSON | **不抓取**（后台本身需鉴权） |
| `GET /blog/513` | 无 `noindex`，canonical 指向自身 | 可抓取、可收录 |

> 记住：`Disallow` 挡的是"抓取"，`noindex` 挡的是"收录"；想让引擎读到 `noindex`，就必须先让它抓得到。

### 4.3.7、数据监测原理

**实现原理：**

| 平台 | 数据从哪来 | 能回答什么 |
| --- | --- | --- |
| Search Console | Google 自己抓取/索引/搜索日志 | 收录状态、搜索词、展示/点击/CTR/排名 |
| GA4 / Umami | 页面里统计脚本上报的事件与访问 | 访客、PV、来源、热门文章、事件（GitHub 点击等） |

**改造要点：**

+ 统计脚本要同时覆盖**服务端直出页**和 **SPA 页**（两处各一份，否则会漏记）；推荐用 F15 后台配置统一注入（见 4.3.8）。
+ 脚本一律 `async`，不阻塞首屏。
+ 业务动作（GitHub 入口、订阅入口点击）需要**显式埋点事件**，GA4 不会自动识别。

**用例（输入事件 → 输出报表）：**

| 用户动作（输入） | 埋点代码 | GA4 中的输出 |
| --- | --- | --- |
| 访问 `/blog/513` | 页面加载自动上报 | 实时报表出现 1 次网页浏览，热门文章 +1 |
| 点击 GitHub 入口 | `gtag('event','click_github',{...})` | 事件 `click_github` 计数 +1，可按来源拆分 |
| 点击 RSS 订阅 | `gtag('event','click_rss',{...})` | 事件 `click_rss` 计数 +1 |

### 4.3.8、SEO 后台配置（统计脚本 / 事件上报）的实现原理

**实现原理：**

把"注入统计脚本""开启事件上报"从**硬编码**改为**配置驱动**：后台把 GA4 ID、开关等存进站点配置（复用现有 `SiteSetting`），后端渲染时按配置决定要不要输出脚本，前端按开关决定要不要上报事件。本质和 4.3.3 一样——**配置即数据，模板按数据决策**。

数据流：

```plain
后台站点设置页
  ├─ gaMeasurementId = G-XXXXXXX
  └─ enableEventTracking = true
        ↓ 保存到 site_setting 表
后端 SiteSettingService / SeoMetaBuilder 读取
        ↓ 注入 model / GET /site 返回 siteInfo
  ├─ 服务端直出页：seo-head.html 用 th:if 条件渲染 <script>
  └─ SPA：main.js 读 siteInfo 动态注入 <script>
        ↓
enableEventTracking 决定 trackEvent() 是否真正上报
```

**改造要点：**

+ 新增配置项尽量**复用现有 `SiteSetting` 机制**，不必新建表。
+ 服务端直出页用 `th:if` 判断：ID 为空则**不输出**，避免注入空 ID 的无效脚本。
+ SPA **不要**把脚本写死进静态 `index.html`，改为运行时读 `GET /site` 动态注入，才能做到"改配置不重新构建"。
+ 事件上报统一走 `trackEvent()` 入口，受开关控制。

**用例（后台配置 → 前台行为）：**

| 后台配置（输入） | 前台输出（结果） |
| --- | --- |
| GA4 ID = `G-XXXXXXX`，开关 = `true` | 页面源码出现 GA4 脚本；点击 GitHub / RSS 上报事件 |
| GA4 ID = 空 | 不输出任何统计脚本 |
| 开关 = `false` | 脚本仍在，但 `trackEvent()` 直接 return，不上报事件 |

### 4.3.9、阅读页性能优化的原理

**实现原理：**

Core Web Vitals 的三大指标，各自对应一类浏览器行为：

| 指标 | 衡量什么 | 主要受什么影响 |
| --- | --- | --- |
| **LCP** | 首屏主内容出现时间 | 首屏资源是否阻塞、封面图是否过大 |
| **INP** | 交互响应是否及时 | 同步长脚本是否占满主线程 |
| **CLS** | 布局是否意外偏移 | 图片/广告是否预留了尺寸 |

**优化手法与原理：**

+ **图片懒加载**（`loading="lazy"`）：非首屏图片延后加载，减少首屏带宽竞争；而**首屏封面反而要 `fetchpriority="high"` 提前加载**。
+ **预留尺寸**（`width` / `height` 或 `aspect-ratio`）：浏览器提前知道占位，加载后不回顶，降低 CLS。
+ **脚本异步**（`defer` / `async` / 按需 `import()`）：不阻塞 HTML 解析与首屏渲染，改善 LCP / INP。

**用例（写法 → 指标表现）：**

| 输入（写法） | 输出（体验 / 指标） |
| --- | --- |
| 正文 `<img loading="lazy" width height>` | 不抢首屏带宽，加载后不抖动（CLS 低） |
| 首屏封面 `<img fetchpriority="high">` | 主内容更快出现（LCP 低） |
| `<script defer src="tocbot">` | 不阻塞首屏（LCP / INP 改善） |

**一句话理解本节：**SEO 的所有工作，本质是让内容**在爬虫不经 JS 就能理解的层（HTML 源码 + 站点级文件 + 协议信号）**里完整呈现；BlogLoom 的改造重点，就是打通"数据库 → 服务端直出 → head/站点文件"这条链路，并绕开 SPA 回退这个总闸。

---

# 五、实战代码：逐项改造

> 本章分两部分：
> - **5.1 功能代码优化实践**：把需要被收录的页面改造成"服务端直出 + 标准 meta"，并补齐站点级文件、收录控制与 SEO 后台配置（F1～F11、F14、F15）。
> - **5.2 搜索引擎平台收录**：代码改完后，如何针对国外（Google、Bing）与国内（百度）逐步推动收录，并持续监测（F12 数据监测、F13 主动推送）。

## 5.1、功能代码优化实践

### 5.1.1、内容直出与数据地基

#### 5.1.1.1、数据层补齐（详情接口）

**问题：**文章页拿不到封面和摘要。

**现状：**`getBlogByIdAndIsPublished` 的 SQL（`mapper/BlogMapper.xml:312-319`）未查 `first_picture`、`description`，`BlogDetail`（`model/vo/BlogDetail.java:23-39`）也没有这两个字段。

**改造点 1：**`BlogDetail.java` 增加字段。

```java
private String firstPicture;  // 封面，用于 og:image / JSON-LD image
private String description;   // 摘要，用于 meta description / og:description
private String authorName;    // 作者名
private String authorUrl;     // 作者主页
```

**改造点 2：**`BlogMapper.xml` 的 `blogDetail` resultMap 与查询补齐列（并 join 作者）。

```xml
<result property="firstPicture" column="first_picture"/>
<result property="description" column="description"/>
<result property="authorName" column="author_name"/>
```

```xml
<select id="getBlogByIdAndIsPublished" resultMap="blogDetail">
    select b.id, b.title, b.content, b.first_picture, b.description,
           b.is_appreciation, b.is_comment_enabled, b.is_top,
           b.create_time, b.update_time, b.views, b.words, b.read_time, b.password,
           c.category_name,
           t.tag_name as tag_name, t.color,
           u.nickname as author_name
    from ((((blog as b
        left join category as c on b.category_id=c.id)
        left join blog_tag as bt on b.id=bt.blog_id)
        left join tag as t on bt.tag_id=t.id)
        left join user as u on b.user_id=u.id)
    where b.id=#{id} and b.is_published=true and b.is_deleted=0
</select>
```

**注意：**若 `user` 表无 `nickname` 字段，改用实际字段（如 `username`）；也可退化为从站点配置读博主信息。作者 URL 可由 `blog.view + /about` 拼出，不强制。

**验收：**`GET /blog?id=513` 返回中包含 `firstPicture`、`description`、`authorName`。

#### 5.1.1.2、文章详情页服务端直出

**现状：**`/blog/{id}` 是前端路由，后端无对应页面；且 `SpaForwardFilter` 会先把请求转发到 `index.html`。

**改造点 1：**新增 `SeoPageController`（注意是 `@Controller` 不是 `@RestController`），按 3.4 约定放在 `module/seo/api` 下。

```java
package com.changlu.blogloom.module.seo.api;

@Controller
public class SeoPageController {

    @Autowired private BlogService blogService;
    @Autowired private SeoMetaBuilder seoMetaBuilder;

    @GetMapping("/blog/{id}")
    public String blogPage(@PathVariable Long id, Model model) {
        BlogDetail blog = blogService.getBlogByIdAndIsPublished(id);
        if (blog == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("seo", seoMetaBuilder.forArticle(blog));
        model.addAttribute("article", blog);
        return "seo/blog";
    }
}
```

**改造点 2：**修改 `SpaForwardConfig`，放行服务端直出路由。

在 `RESERVED_PREFIXES`（`SpaForwardConfig.java:33-35`）之外，增加一个"服务端直出前缀"名单，命中时不回退：

```java
private static final List<String> SSR_PREFIXES = Arrays.asList(
        "/blog", "/tag", "/category", "/column", "/archives", "/about", "/friends"
);
```

在 `shouldForward` 内最先判断：

```java
for (String prefix : SSR_PREFIXES) {
    if (path.equals(prefix) || path.startsWith(prefix + "/")) {
        return false;   // 交给后端 Controller 直出
    }
}
```

**注意：**`/blog` 前缀会同时匹配接口 `/blogs`（`BlogController.java:47`）。但接口请求的 `Accept` 不是 `text/html`、本就不会回退，`/blogs` 返回 JSON 不受影响；同理 `/blog?id=` 详情接口也不受影响。真正被接管的是浏览器导航 `/blog/513` 这类无扩展名 HTML 请求。

**改造点 3（可选）：**`blog-view-ui` 的 `/blog/:id` 保留用于站内跳转；用户从首页点进文章时若被 Vue Router 拦截，则仍走 SPA；**硬刷新/爬虫**则拿到服务端直出 HTML。二者内容一致即可。

**Thymeleaf 模板 `seo/blog.html`（骨架，正文直出）：**

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head th:replace="seo/fragments/seo-head :: head(${seo})"></head>
<body>
<article>
    <h1 th:text="${article.title}">文章标题</h1>
    <div class="meta">
        <time th:text="${#dates.format(article.createTime,'yyyy-MM-dd')}"></time>
        <span th:text="${article.category?.name}"></span>
    </div>
    <!-- 正文已是 HTML，用 utext 直出 -->
    <div class="content" th:utext="${article.content}">正文</div>
    <div class="tags">
        <a th:each="tag : ${article.tags}"
           th:href="@{'/tag/' + ${tag.name}}"
           th:text="'#' + ${tag.name}">#标签</a>
    </div>
</article>
</body>
</html>
```

**重点：**正文用 `th:utext`（等价于现有 `v-html`）。一阶段模板样式可先精简，后续再统一；关键是**正文与 meta 必须在首屏 HTML 里**。

**标题层级要求：**全页只有一个 `<h1>`（文章标题），正文内的 Markdown 标题从 `<h2>` 起。若原 Markdown 渲染出多个 `h1`，建议在 `SeoMetaBuilder` 或渲染阶段降级。

**验收：**`curl -H 'Accept: text/html' https://域名/blog/513` 能看到 `<h1>` 正文和正确 `<title>`。


### 5.1.2、页面级 SEO 元信息

#### 5.1.2.1、页面级 SEO meta

**改造点：**所有内容页复用公共 head 片段 `fragments/seo-head.html`。

```html
<head th:fragment="head(seo)" xmlns:th="http://www.thymeleaf.org">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${seo.title}">标题 - BlogLoom</title>
    <meta name="description" th:content="${seo.description}">
    <link rel="canonical" th:href="${seo.canonicalUrl}">

    <meta property="og:type" content="article">
    <meta property="og:title" th:content="${seo.title}">
    <meta property="og:description" th:content="${seo.description}">
    <meta property="og:image" th:content="${seo.image}">
    <meta property="og:url" th:content="${seo.canonicalUrl}">
    <meta property="og:site_name" th:content="${seo.siteName}">
    <meta property="article:published_time" th:content="${seo.publishedTime}">
    <meta property="article:modified_time" th:content="${seo.modifiedTime}">
    <meta property="article:author" th:content="${seo.authorName}">
    <meta property="article:section" th:content="${seo.category}">
    <meta property="article:tag" th:each="t : ${seo.tags}" th:content="${t}">

    <link rel="alternate" type="application/rss+xml"
          th:title="${seo.siteName}" th:href="${seo.baseUrl} + '/rss.xml'">
</head>
```

`SeoMeta` 与 `SeoMetaBuilder` 负责把 `BlogDetail` 转成上述字段：

| 字段 | 来源 | 规则 |
| --- | --- | --- |
| `title` | `article.title + webTitleSuffix` | 复用现有 `webTitleSuffix` 站点配置 |
| `description` | `article.description` | 为空时截取正文纯文本前 120 字 |
| `canonicalUrl` | `blog.view + /blog/{id}` | **不带任何追踪参数** |
| `image` | `article.firstPicture` | **必须是完整绝对 URL**（第三方平台要抓取） |
| `publishedTime` / `modifiedTime` | `create_time` / `update_time` | ISO 8601 带时区 |

**验收：**查看源码只有一个 `<title>`；description 非空；canonical 与 sitemap、站内链接三者一致。

#### 5.1.2.2、canonical 与 URL 策略

**现状：**无 canonical；前端路由参数可能带 `?spm=`、`?from=` 等，易产生重复 URL。

**改造点：**

1. 统一标准地址：`{SEO 域名}/blog/{id}`（SEO 域名 = 后台 `seoDomain`，兜底 `blog.view`；一阶段沿用 id，slug 为可选增强）。
2. canonical、`og:url`、`sitemap loc`、站内文章链接**全部使用同一个标准 URL**。
3. 所有绝对 URL 由 `SeoUrlResolver` 统一拼接，**不硬编码域名**。
4. 明确 canonical 只是"首选网址信号"，不做跳转；若需强制去参数可另加重定向（一阶段可不做）。

**域名从哪来（重点）：**

建议**抽成后台系统配置项**（可在管理后台直接改、即时生效），同时保留 `blog.view` 作为兜底。取值优先级如下：

| 优先级 | 来源 | 位置 | 说明 |
| --- | --- | --- | --- |
| ①（优先） | 后台站点设置 | `site_setting` 表，键 `seoDomain` | 后台可改，改完即时生效，不用重启 |
| ②（兜底） | 配置文件 | `conf/application.properties` 的 `blog.view` / 环境变量 `BLOG_VIEW` | 后台为空时使用，默认 `http://localhost:8081` |

**① 新增后台配置项**（复用现有 `SiteSetting` 机制，与 `webTitleSuffix`、`favicon` 同类）：

| 字段 | 值（示例） |
| --- | --- |
| `nameEn` | `seoDomain` |
| `nameZh` | SEO 域名 |
| `value` | `https://blog.changlu.cloud` |
| `type` | `1`（随 `GET /site` 的 siteInfo 返回） |

管理后台的站点设置页加一个"SEO 域名"输入框即可（沿用现有保存逻辑，无需新建表）。

**② 服务层新增读取方法**（与现有 `getWebTitleSuffix()` 同风格）：

```java
// SiteSettingService 接口新增
String getSeoDomain();   // 读取 nameEn = 'seoDomain' 的值，为空返回 null
```

**③ SeoUrlResolver 读取：后台优先，兜底配置：**

```java
@Component
public class SeoUrlResolver {
    private final SiteSettingService siteSettingService; // 读后台配置
    private final BlogProperties blogProperties;         // 兜底配置

    public SeoUrlResolver(SiteSettingService siteSettingService, BlogProperties blogProperties) {
        this.siteSettingService = siteSettingService;
        this.blogProperties = blogProperties;
    }

    /** 站点根地址：优先后台 seoDomain，其次 blog.view（去掉结尾斜杠） */
    public String baseUrl() {
        String seoDomain = siteSettingService.getSeoDomain();
        String url = (seoDomain != null && !seoDomain.trim().isEmpty())
                ? seoDomain : blogProperties.getView();
        return trimTrailingSlash(url);
    }

    /** 文章标准 URL：{baseUrl}/blog/{id} */
    public String articleUrl(Long id) {
        return baseUrl() + "/blog/" + id;
    }
}
```

**统一出口（关键）：**后续所有需要绝对地址的 SEO 策略，**统统只认这一处「SEO 域名」配置**，全部由 `SeoUrlResolver.baseUrl()` 派生：

| 用到的地方 | 取值 |
| --- | --- |
| `<link rel="canonical">` | `{seoDomain}/blog/{id}` |
| `og:url` | `{seoDomain}/blog/{id}` |
| sitemap `loc` | `{seoDomain}/blog/{id}` |
| RSS `<link>` / `<guid>` | `{seoDomain}/blog/{id}` |
| JSON-LD `url` / `mainEntityOfPage` | `{seoDomain}/blog/{id}` |

**为什么放后台配置：**换域名、迁移、多环境部署时，**只改后台这一处即可，不用改配置文件、不用重新部署**；上表所有地址同步指向新域名，避免出现"有的页面指向 A 域名、有的指向 B 域名"的权重分散。

**注意（线上部署最易踩的坑）：**若后台没配、配置文件也没改，会落到默认 `http://localhost:8081`，导致 canonical / `og:url` / sitemap `loc` / RSS 全是 localhost，被搜索引擎直接丢弃。**上线前务必在后台把"SEO 域名"填成真实域名。**

**注意两点：**

+ canonical **不会让浏览器跳转**；浏览器跳转需要重定向。
+ 不要让所有文章都指向首页，也不要把不同内容的分页随意指向同一页。

**验收：**带 `?utm_source=xx` 访问时，页面 canonical 仍指向无参数标准地址。

#### 5.1.2.3、结构化数据 JSON-LD

**改造点：**在 `seo-head.html` 片段中输出 `BlogPosting`。

```html
<script type="application/ld+json" th:inline="javascript">
/*[[${seo.ldJson}]]*/
</script>
```

由 `SeoMetaBuilder` 构建 JSON（推荐用 Jackson `ObjectMapper` 序列化，避免手拼转义问题）：

```java
ObjectNode ld = mapper.createObjectNode();
ld.put("@context", "https://schema.org");
ld.put("@type", "BlogPosting");
ld.put("headline", article.getTitle());
ld.put("description", article.getDescription());
ld.put("image", abs(article.getFirstPicture()));
ld.put("datePublished", toIso(article.getCreateTime()));
ld.put("dateModified", toIso(article.getUpdateTime()));
ld.put("inLanguage", "zh-CN");
ld.put("mainEntityOfPage", canonicalUrl);
ArrayNode kw = ld.putArray("keywords");
article.getTags().forEach(t -> kw.add(t.getName()));
ObjectNode author = ld.putObject("author");
author.put("@type", "Person");
author.put("name", article.getAuthorName());
ObjectNode publisher = ld.putObject("publisher");
publisher.put("@type", "Organization");
publisher.put("name", siteName);
ObjectNode logo = publisher.putObject("logo");
logo.put("@type", "ImageObject");
logo.put("url", abs(siteLogo));
```

**注意：**`dateModified` 必须取**真实的实质修改时间**（`update_time`），不能每次自动刷新伪装更新。

**验收：**用 Google 富媒体结果测试工具 / Schema 校验器无错误。

#### 5.1.2.4、Open Graph

Open Graph 的标签已包含在 5.3 的公共片段里，这里补充几个关键点：

1. `og:image` 必须**完整 URL 且公开可访问**（`/static/**` 上传图是公开的，符合）。
2. SEO 与 OG 复用同一份 `article.title/summary/cover`，只维护一处数据。
3. 摘要与 description 复用，不要两套文案。

**弊端说明：**如果什么都不配置，平台可能自行从 HTML 里"猜"标题、摘要和图片，很容易出现**图片选错、标题被截断、没有封面**等问题，分享出去很难看。

**验收：**把文章链接发到微信/QQ/Slack，出现封面 + 标题 + 摘要卡片。


### 5.1.3、站点级收录文件

#### 5.1.3.1、robots.txt

**现状：**404。

**改造点：**新增 `SeoFileController`（按 3.4 约定放在 `module/seo/api` 下），统一承载 robots / sitemap / rss 三个端点。

```java
@GetMapping(value = "/robots.txt", produces = "text/plain;charset=UTF-8")
public String robots() {
    return "User-agent: *\n"
         + "Disallow: /admin/\n"
         + "Disallow: /cms/\n"
         + "Disallow: /login\n"
         + "Allow: /\n\n"
         + "Sitemap: " + seoUrlResolver.baseUrl() + "/sitemap.xml\n";
}
```

**重点（容易踩坑）：**

+ BlogLoom 的接口不在 `/api/` 下，而是根路径（`/blog?id=`、`/blogs`、`/site`…），**不要**写 `Disallow: /blog`，否则会误屏蔽文章页。
+ `robots.txt` 是抓取规则，**不是权限控制**；后台仍靠 `/admin` 的 JWT 鉴权。
+ `SpaForwardConfig` 对带扩展名请求不回退，`/robots.txt` 可直接由 Controller 命中；也可放 `conf/static/view/robots.txt` 由静态资源提供，二选一。

**验收：**`curl https://域名/robots.txt` 返回规则且含 Sitemap 行。

#### 5.1.3.2、sitemap.xml

**现状：**404。

**改造点：**新增 Sitemap 生成，**只包含公开、已发布、非密码保护文章**（私密文章不进 sitemap，但仍需鉴权保护）。

```java
@GetMapping(value = "/sitemap.xml", produces = "application/xml;charset=UTF-8")
public String sitemap() {
    List<Blog> blogs = blogService.listPublishedForSitemap(); // is_published=1 and is_deleted=0 and (password is null or password='')
    StringBuilder sb = new StringBuilder();
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
    for (Blog b : blogs) {
        sb.append("  <url>\n")
          .append("    <loc>").append(seoUrlResolver.articleUrl(b.getId())).append("</loc>\n")
          .append("    <lastmod>").append(toDate(b.getUpdateTime())).append("</lastmod>\n")
          .append("  </url>\n");
    }
    sb.append("</urlset>");
    return sb.toString();
}
```

**要点：**

+ `loc` 必须与 canonical 完全一致。
+ `lastmod` 用 `update_time`（实现上可新增一个只查 `id, update_time` 的 Mapper 查询）。
+ 首页、分类、标签、专栏页也可加入（各自一条 `<url>`），一阶段可先只放文章。
+ 文章数超过 5 万或文件超 50MB 时需拆分 sitemap index（当前规模不涉及）。
+ **不要**包含 `/admin`、`/cms`、草稿、密码保护文章。

**验收：**`curl https://域名/sitemap.xml` 内容正确；草稿与私密文章不出现。

#### 5.1.3.3、RSS 订阅

**现状：**404。

**改造点：**提供 `/rss.xml`（RSS 2.0，输出最近约 20 篇公开文章），并把 `<link rel="alternate">` 放进公共 head 片段（已在 5.3）。Atom 一阶段可不做，RSS 已覆盖绝大多数阅读器。

```java
@GetMapping(value = "/rss.xml", produces = "application/rss+xml;charset=UTF-8")
public String rss() {
    // channel: title/link/description/lastBuildDate
    // 每 item: title/link/guid/pubDate/description(摘要)
}
```

**重点：**只输出摘要或受控正文，不输出私密内容；发布日期用 `create_time`。

**验收：**用 Feedly/Inoreader 等能订阅成功；文章页源码含 RSS 自动发现链接。


### 5.1.4、聚合页可抓取

#### 5.1.4.1、聚合页可抓取（列表 / 标签 / 分类 / 归档）

**现状：**`/home`、`/archives`、`/tag/:name`、`/category/:name`、`/column/:id` 全是 SPA，爬虫读不到列表。

**改造点（P1）：**在 `SeoPageController` 增加对应服务端直出页面，复用 `list.html` 通用模板。

| 路由 | 对应现有接口 | 复用数据服务 |
| --- | --- | --- |
| `GET /home`（或 `/`） | `GET /blogs`（`BlogController.java:47`） | `blogService.getBlogInfoListByIsPublished` |
| `GET /tag/{name}` | `GET /tag`（`TagController.java:32`） | 标签服务 + 分页 |
| `GET /category/{name}` | `GET /category`（`CategoryController.java:32`） | 分类服务 + 分页 |
| `GET /archives` | `GET /archives`（`ArchiveController.java:29`） | 归档服务 |
| `GET /column/{id}` | `GET /column/{id}/blogs`（`ColumnController.java:14`） | 专栏服务 |

同时把对应前缀加入 `SSR_PREFIXES`（见 5.1.1.2）。列表页每篇文章输出指向 `/blog/{id}` 的 `<a>` 内链，形成可抓取的内链网络。标签/分类这类聚合链接建议加 `rel="nofollow"`，避免权重被滥用。

**验收：**`curl /tag/Java` 能看到文章标题链接列表。


### 5.1.5、收录控制与后台配置

#### 5.1.5.1、收录控制（防止后台被收录）

**现状：**`/cms/`（管理后台）与 `/admin/` 无 `noindex`，可能被爬虫收录。

**改造点：**

1. `blog-cms-ui/index.html` 的 `<head>` 增加：

```html
<meta name="robots" content="noindex, nofollow">
```

2. 对 `/cms/**`、`/admin/**` 在响应头统一加 `X-Robots-Tag: noindex, nofollow`（可用一个简单 Filter 或 `WebConfig` 拦截器实现），双保险。
3. `robots.txt` 已 Disallow `/admin/`、`/cms/`（见 5.1.3.1）。

**注意：**Google 要能"抓取"页面才能读到 `noindex`。因此 robots 里 Disallow 与页面 noindex 同时用没问题（后台本就不需要收录），但概念上要分清：**Disallow 阻止抓取，noindex 阻止收录**。

**验收：**`curl -I /cms/` 含 `X-Robots-Tag`；后台页面源码含 noindex。

#### 5.1.5.2、SEO 优化后台配置支持（SEO 域名 / 统计脚本 / 事件上报）

**问题：**SEO 域名（`blog.view`）如果只能靠改配置文件 / 环境变量，换域名或迁移就得重新部署；GA4 脚本若写死在 `seo-head.html` 和 `index.html` 里，更换衡量 ID、临时关闭统计、新增埋点也都要改代码。更合理的做法是**把它们统一抽成后台可配置项**，做到"不改代码即可开关"。

**改造点：**

1. **后台站点设置新增配置项**（复用现有 `SiteSetting` 机制）：
   + `seoDomain`：SEO 域名（如 `https://blog.changlu.cloud`），供 `SeoUrlResolver` 拼绝对 URL，**优先级高于 `blog.view`**（详见 5.1.2.2）；
   + `gaMeasurementId`：GA4 衡量 ID（如 `G-XXXXXXX`），空则不注入脚本；
   + `enableEventTracking`：事件上报开关（`true` / `false`）；
   + （可选）`customHeadScript`：自定义 head 脚本，用于扩展其它统计平台。

2. **服务端直出页按配置注入**（`templates/seo/fragments/seo-head.html`）：

```html
<!-- 仅当后台配置了 GA4 ID 时才注入 -->
<th:block th:if="${seo.gaMeasurementId != null and !seo.gaMeasurementId.isEmpty()}">
  <script async
          th:src="'https://www.googletagmanager.com/gtag/js?id=' + ${seo.gaMeasurementId}"></script>
  <script th:inline="javascript">
    window.dataLayer = window.dataLayer || [];
    function gtag(){dataLayer.push(arguments);}
    gtag('js', new Date());
    gtag('config', /*[[${seo.gaMeasurementId}]]*/);
  </script>
</th:block>
```

3. **SPA 页动态注入**（`blog-view-ui` 复用已有的 `GET /site`，不要再写死进静态 `index.html`）：

```js
// main.js：读取站点配置，动态注入 GA4；后台改了 ID 无需重新构建
fetch('/site').then(r => r.json()).then(res => {
  const id = res.data.siteInfo.gaMeasurementId;
  if (!id) return;
  const s = document.createElement('script');
  s.async = true;
  s.src = `https://www.googletagmanager.com/gtag/js?id=${id}`;
  document.head.appendChild(s);
  window.dataLayer = window.dataLayer || [];
  window.gtag = function () { window.dataLayer.push(arguments); };
  gtag('js', new Date());
  gtag('config', id);
});
```

4. **事件上报受开关控制**（`enableEventTracking` 关闭时不埋点）：

```js
// 统一入口：由后台开关决定是否上报
function trackEvent(name, params) {
  if (!window.__eventTrackingEnabled) return; // 来自 siteInfo.enableEventTracking
  if (typeof gtag === 'function') gtag('event', name, params);
}
// 使用
trackEvent('click_github', { event_category: 'outbound', event_label: 'github' });
trackEvent('click_rss', { event_category: 'subscribe', event_label: 'rss' });
```

**配置项一览：**

| 配置项 | 键名 | 示例 | 说明 |
| --- | --- | --- | --- |
| GA4 衡量 ID | `gaMeasurementId` | `G-XXXXXXX` | 空则不注入统计脚本 |
| 事件上报开关 | `enableEventTracking` | `true` / `false` | 控制 GitHub / 订阅点击是否上报 |
| 自定义 head 脚本（可选） | `customHeadScript` | `<script>…</script>` | 扩展其它统计平台 |

**验收：**

+ 后台填入 `gaMeasurementId` 保存后，前台页面源码出现 GA 脚本；清空后脚本消失。
+ `enableEventTracking` 关闭时，点击 GitHub / RSS 不再上报事件。
+ 全程**无需改代码、无需重新部署**。

#### 5.1.5.3、SEO 配置页（后台栏目落地）

**目标：**在管理后台提供「**SEO优化**（顶级栏目）→ SEO配置」页面，用于配置 SEO 域名（`seoDomain`）——即 5.1.2.2 里全站绝对地址的**统一出口**。

**页面位置：**侧边栏顶级栏目 **SEO优化** → **SEO配置**（与「博客管理」「图床管理」同级）。

**页面字段与取值规则：**

| 字段 | 配置键 | 取值规则 |
| --- | --- | --- |
| SEO 域名 | `site_setting.seoDomain` | 后台有值 → 用后台值；后台为空 → 展示配置参数 `blog.view` |

> 即"默认读取配置参数展示；后台配置了就用后台的"。

**接口：**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/seo/config` | 返回 `{ seoDomain, defaultSeoDomain, configured }` |
| POST | `/admin/seo/config` | body `{ seoDomain }`，保存；留空表示清除后台配置、回退 `blog.view` |
| GET | `/admin/seo/file/{name}/content` | 快捷**查看**：返回 `robots`/`sitemap`/`rss` 的文本内容 |
| GET | `/admin/seo/file/{name}` | 快捷**下载**：作为附件下载（`Content-Disposition: attachment`） |

**核心文件：快捷查看 / 下载：**

页面新增「核心文件」卡片，列出三个站点级文件，每个都支持**查看**与**下载**：

| 文件 | 路径 | 说明 |
| --- | --- | --- |
| `robots.txt` | `/robots.txt` | 抓取规则，声明 Sitemap 地址 |
| `sitemap.xml` | `/sitemap.xml` | 公开文章地图，辅助搜索引擎发现 |
| `rss.xml` | `/rss.xml` | 最近公开文章的订阅源 |

+ **查看**：点击「查看」→ `GET /admin/seo/file/{name}/content` → 弹窗（`el-dialog` + `<pre>` 深色代码块）展示文件内容。
+ **下载**：点击「下载」→ `GET /admin/seo/file/{name}`（`responseType: 'blob'`）→ 前端创建 Blob 并触发浏览器下载。
+ **单一数据源**：三个文件的内容统一由 `module/seo/service/SeoFileService` 产出，公开端点（`/robots.txt` 等）与后台查看/下载**复用同一份内容**，避免不一致。

**数据流：**

```plain
后台「SEO配置」页
   ↓ GET /admin/seo/config
SeoConfigController
   ↓ siteSettingService.getSeoDomain()（后台值） ＋ BlogProperties.getView()（兜底）
返回 { seoDomain: 生效值, defaultSeoDomain: blog.view, configured }
   ↓ 页面展示：seoDomain（后台为空时即 blog.view 的值）
用户保存 → POST /admin/seo/config → 写入 site_setting(seoDomain) → 清理站点缓存

查看/下载 → SeoConfigController → SeoFileService（robots/sitemap/rss）
                                          ↑ 公开端点 SeoFileController 也走同一份
```

**已落地文件清单：**

| 层 | 文件 |
| --- | --- |
| SQL | `sql/increment/1.0/20261006000000_v1.0.x.sql`（seed `seoDomain`） |
| 常量 | `constant/SiteSettingConstants.java`（`SEO_DOMAIN`） |
| Mapper | `mapper/SiteSettingMapper.java` + `resources/mapper/SiteSettingMapper.xml`（`getSeoDomain`） |
| Service | `service/SiteSettingService.java` + `impl/SiteSettingServiceImpl.java`（`getSeoDomain` / `saveSeoDomain`） |
| SEO 文件服务 | `module/seo/service/SeoFileService.java` + `impl/SeoFileServiceImpl.java`（robots/sitemap/rss 内容统一出口） |
| API | `module/seo/api/SeoConfigController.java`、`module/seo/api/SeoFileController.java`、`module/seo/domain/SeoConfig.java` |
| CMS | `blog-cms-ui/src/api/seo.js`、`views/blog/seo/SeoConfig.vue`、`icons/svg/seo.svg`（自定义 SEO 图标）、`router/index.js`（新增"SEO优化"顶级菜单）、`settings.js` |

**与 F15 的关系：**本页是 F15 的第一块落地（SEO 域名 + 核心文件查看/下载）；后续 `gaMeasurementId`、`enableEventTracking` 等配置项继续加在同一页即可。

**验收：**

+ 后台顶级栏目「SEO优化 → SEO配置」可正常打开。
+ 默认展示 `blog.view`；保存后 `site_setting.seoDomain` 有值、再次进入展示后台值；留空保存则回退展示 `blog.view`。
+ 保存后清理站点缓存，前台生成的绝对地址使用该域名。
+ 「核心文件」卡片可对 `robots.txt` / `sitemap.xml` / `rss.xml` 逐个**查看**（弹窗展示内容）与**下载**（保存到本地）。

#### 5.1.5.4、SEO 平台关联（站点验证，方案 B）

**目标：**在后台「SEO优化 → SEO平台关联」配置百度 / Bing / Google 的站点验证 `content`，保存后**自动注入站点首页 `<head>`**，无需改代码、无需重新部署。

**页面位置：**侧边栏顶级栏目 **SEO优化** → **SEO平台关联**。

**页面字段与配置键：**

| 平台 | 验证 meta | 配置键（site_setting） |
| --- | --- | --- |
| 百度 | `baidu-site-verification` | `baiduSiteVerification` |
| Bing | `msvalidate.01` | `bingSiteVerification` |
| Google | `google-site-verification` | `googleSiteVerification` |

> 每平台一个配置：用户从平台后台复制整段验证代码中的 `content` 值填入即可；页面提供「复制 meta」按钮。

**接口：**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/seo/verification` | 返回 `{ baidu, bing, google }` |
| POST | `/admin/seo/verification` | body `{ baidu, bing, google }`，保存 |

**注入实现（方案 B：SPA 入口注入）：**

由于根路径 `/` 由 SPA 的 `index.html` 承载，而百度验证不执行 JS，必须在**服务端返回的 HTML** 里就有验证 meta。实现方式：

```plain
GET /  或  /index.html
   ↓ SpaForwardFilter：/ 回退到 /index.html（其它 SPA 路由同理）
SpaIndexController（@Controller）
   ↓ 读取前台构建产物 conf/static/view/index.html
   ↓ 取 site_setting 中的三个验证 content，SeoVerificationSupport 拼接 meta
   ↓ 注入 </head> 之前
返回注入后的 HTML（浏览器 URL 不变）
```

> `SpaIndexController` 映射 `/`、`/index.html`，因此所有回退到 SPA 的页面（`/moments`、`/friends` 等）都会带上验证 meta；未配置时原样返回。

**已落地文件清单：**

| 层 | 文件 |
| --- | --- |
| 常量 | `constant/SiteSettingConstants.java`（`BAIDU_/BING_/GOOGLE_SITE_VERIFICATION`） |
| Service | `service/SiteSettingService.java` + `impl/SiteSettingServiceImpl.java`（`getSeoVerifications` / `saveSeoVerifications`） |
| 支持类 | `module/seo/support/SeoVerificationSupport.java`（meta 拼接与注入） |
| API | `module/seo/api/SeoVerificationController.java`（GET/POST `/admin/seo/verification`）、`module/seo/api/SpaIndexController.java`（方案 B 注入）、`module/seo/domain/SeoVerification.java` |
| CMS | `blog-cms-ui/src/api/seo.js`、`views/blog/seo/SeoVerification.vue`、`router/index.js`（新增"SEO平台关联"菜单） |

**验收：**

+ 后台「SEO优化 → SEO平台关联」可打开，三个平台各有一个 content 输入框与「复制 meta」按钮。
+ 填入并保存后，`curl -s https://域名/ | grep 'baidu-site-verification'` 能命中对应 meta。
+ 清空后保存，对应 meta 从首页源码消失。

### 5.1.6、阅读页性能优化

#### 5.1.6.1、代码优化：阅读页性能与资源加载

**问题：**详情页正文图片未懒加载，`index.html` 里 Prism / APlayer / Meting / tocbot 等脚本在首屏同步加载，拖慢 LCP，还可能因图片无尺寸造成 CLS。

**改造点：**

1. **正文图片懒加载**：Thymeleaf 渲染正文后，给正文区 `<img>` 加 `loading="lazy"` 与 `decoding="async"`；**首屏封面图除外**，反而给 `fetchpriority="high"`。

```html
<!-- 首屏封面：优先加载 -->
<img th:src="${article.firstPicture}" fetchpriority="high" width="1200" height="630" alt="封面">
<!-- 正文图片：懒加载 -->
<img loading="lazy" decoding="async" alt="">
```

2. **给图片/广告位预留尺寸**：统一设置 `width`/`height` 或 CSS `aspect-ratio`，避免加载后把正文顶下去（CLS）。

3. **第三方脚本异步化**：把 Prism / APlayer / Meting / tocbot 改为 `<script defer>` 或按需动态 `import()`，不阻塞首屏。

4. **静态资源缓存与压缩**：Vite 构建产物已带 hash，配合长缓存；后端开启 `server.compression.enabled=true`（gzip/br）。

5. **统计脚本异步**：GA4 脚本一律 `async`（见 5.2.1）。

**验收：**PageSpeed Insights / Lighthouse 中 LCP、CLS 明显改善；Open DevTools Network 可见正文图片为懒加载。

## 5.2、搜索引擎平台收录

代码改完只是**"具备被收录的条件"**，并不等于马上被收录。下面这部分讲清楚：如何针对 **blog.changlu.cloud** 一步步推动**国外（Google、Bing）**与**国内（百度）**搜索引擎真正收录文章，并持续监测。

> 三个平台的通用前提：F7/F8 已完成，`https://blog.changlu.cloud/sitemap.xml` 能公开访问（`curl` 返回 200）。

### 5.2.1、数据监测接入

**目标：**能回答"有没有被收录""人从哪来""哪篇最受欢迎"。建议按下面顺序接入——**先 Search Console（决定收录），再访问统计（本阶段用 GA4）**。

**接入顺序总览**

| 步骤 | 做什么 | 入口网址 | 产出 |
| --- | --- | --- | --- |
| ① | 验证站点所有权 | https://search.google.com/search-console/ | 站点资源建立 |
| ② | 提交 Sitemap | 上方站点 → 左侧"站点地图" | 加速发现收录 |
| ③ | 创建访问统计 | https://analytics.google.com/ | 拿到衡量 ID |
| ④ | 安装统计脚本 | 代码：公共 head 片段 + `blog-view-ui/index.html` | 开始上报 |
| ⑤ | 配置事件埋点 | GA4 事件 | GitHub / 订阅点击 |
| ⑥ | 验证数据 | GA4 实时报表 / Search Console | 数据可见 |

**前置条件：**F7/F8 已完成，`https://blog.changlu.cloud/sitemap.xml` 能公开访问（`curl` 返回 200）。

**Step ① 验证站点所有权（Search Console）**

1. 打开 https://search.google.com/search-console/ ，用 Google 账号登录。
2. 左上角"添加资源"，选**"网域"**方式，填 `blog.changlu.cloud`。
3. 复制给出的 TXT 记录（形如 `google-site-verification=xxxx`），到你的 **DNS 服务商**添加一条 TXT 解析。
4. 回到 Search Console 点"验证"，通过后资源建立。
   + 若不便改 DNS，也可选"网址前缀"方式：填 `https://blog.changlu.cloud/`，用 HTML 文件或 `<meta name="google-site-verification" content="...">` 验证。

**Step ② 提交 Sitemap**

1. 进入该资源，左侧菜单点 **"站点地图 / Sitemaps"**。
2. 输入框填 `sitemap.xml`（完整地址 `https://blog.changlu.cloud/sitemap.xml`），点"提交"。
3. 状态变为"成功"即被读取；**收录需等 Google 抓取，通常几天到几周**，不必反复提交。

**Step ③ 创建访问统计（本阶段选定 GA4）**

1. 打开 https://analytics.google.com/ → "管理" → 创建**媒体资源**。
2. 在资源下创建**网站数据流（Web）**，网址填 `https://blog.changlu.cloud`。
3. 记下生成的**衡量 ID（Measurement ID，形如 `G-XXXXXXX`）**。

> 备选：若日后希望数据自持，可改用自建 **Umami**（https://umami.is/），接入方式类似；本阶段先用 GA4 即可，不必两边都接。

**Step ④ 安装统计脚本（两处都要放）**

> **推荐：**用 F15 后台配置实现（见 5.1.5.2），后台填 GA4 ID 即自动注入，无需改代码；下面为手动方式，供未做后台时参考。

+ 服务端直出页：放进公共片段 `templates/seo/fragments/seo-head.html`（或用 F15 片段按配置注入）。
+ SPA 页：由 `main.js` 读取 `GET /site` 动态注入（或用 F15 方案，避免写死进 `index.html`）。
+ 脚本一律 `async`，避免阻塞首屏。

```html
<!-- GA4 示例，G-XXXXXXX 换成你的衡量 ID -->
<script async src="https://www.googletagmanager.com/gtag/js?id=G-XXXXXXX"></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());
  gtag('config', 'G-XXXXXXX');
</script>
```

**Step ⑤ 配置事件埋点（GA4）**

> **推荐：**通过 F15 的"事件上报开关"统一控制（见 5.1.5.2）；下面为直接埋点写法。

GitHub 入口、RSS/订阅入口的点击需**显式上报事件**（GA4 不会自动识别）：

```js
// 点击 GitHub 入口
gtag('event', 'click_github', { event_category: 'outbound', event_label: 'github' });
// 点击 RSS 订阅
gtag('event', 'click_rss', { event_category: 'subscribe', event_label: 'rss' });
```

**Step ⑥ 验证**

+ GA4：打开"实时（Realtime）"报表，用手机访问博客，能看到当前访问与埋点事件。
+ Search Console：确认"站点地图"状态为"成功"；用"网址检查"输入一篇文章 URL，能显示是否可编入索引。

**三组数据分开看**

| 想看什么 | 主要看哪里 |
| --- | --- |
| 收录、搜索关键词、展示/点击/排名 | Search Console |
| 用户数、PV、来源、热门文章 | GA4（或 Umami） |
| 广告展示、收益、结算 | 广告联盟后台（二阶段） |

> 官方帮助：Search Console 验证站点所有权 https://support.google.com/webmasters/answer/9008080 ；GA4 创建数据流 https://support.google.com/analytics/answer/9304153 。

**验收：**Search Console 站点地图状态为"成功"且开始抓取；GA4 实时报表能看到当前访问与埋点事件。

### 5.2.2、Google 平台收录（国外）

**平台入口：**https://search.google.com/search-console/

**目标：**让 `blog.changlu.cloud` 的文章进入 Google 索引（国外主战场）。

**操作步骤：**

1. **验证所有权**：添加资源，选"网域"方式填 `blog.changlu.cloud`，在 DNS 服务商加 TXT 记录后验证；或选"网址前缀" `https://blog.changlu.cloud/`，用 HTML 文件 / `<meta name="google-site-verification" content="...">` 验证。

   ![image-20261006183505691](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610061835953.png)   ![image-20261006214051311](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610062140807.png)  

   需要在dns域名映射配置中填写txt：

   ![image-20261006214646547](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610062146663.png)  

   配置的腾讯云DNS配置如下：

   ![img](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610062151157.png)  

   保存好进行google console验证即可，验证通过：

   ![image-20261006215219058](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610062152232.png)      

2. **提交 Sitemap**：左侧"站点地图"，提交 `https://blog.changlu.cloud/sitemap.xml`。

   选择对应的博客域名然后进行站点地图配置：

   ![image-20261006215356368](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610062153435.png)  

   提交成功：

   ![image-20261006215432122](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610062154230.png)  

   后续就是进行等待提交了：

   ![image-20261006215933774](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610062159940.png)  

   自己测试验证为：

   ```      shell
   # 测试curl
   curl -A "Googlebot" \
   -sS -o /dev/null \
   -w "HTTP=%{http_code}\nTYPE=%{content_type}\nSIZE=%{size_download}\n" \
   https://blog.changlu.cloud/sitemap.xml
   
   # 返回值
   HTTP=200
   TYPE=application/xml;charset=UTF-8
   SIZE=52765
   ```
   Google 官方明确把 `robots.txt` 阻止 Sitemap、404、服务器暂时不可用等列为 Sitemap `Couldn't fetch` 的主要原因。[谷歌帮助](https://support.google.com/webmasters/answer/7451001?hl=en-EN&utm_source=chatgpt.com)，所以如果

3. **请求编入索引（新文章）**：用"网址检查"输入 `https://blog.changlu.cloud/blog/513`，确认"网址可编入索引"后点"请求编入索引"。

   搜索对应的google search console即可：

   ![image-20261007101844677](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610071018090.png)

   进行测试并进行请求编入索引即可：

   ![image-20261007101920785](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610071019928.png)    

4. **等待抓取与收录**：通常数天到数周，**不要反复狂点**，重点是把内容与结构做好。

**主动推送：**Google **没有开放的主动推送 API，也不采用 IndexNow**。所以 Google 这边不用找"推送接口"，做法就是 **Sitemap + 站内内链 + Search Console 手动请求**。

**注意：**提交 / 请求**不等于一定收录**，也不等于排名靠前——它只是"通知"，最终由 Google 判断。

### 5.2.3、百度平台收录（国内）

> 说明：需要绑定微信、手机号、邮箱以及进行身份验证

**平台入口：**https://ziyuan.baidu.com/

**目标：**让 `blog.changlu.cloud` 的文章进入百度索引（国内主战场），点击进入到百度平台的站点管理：https://ziyuan.baidu.com/site/index。

**操作步骤：**

1. **验证站点**：添加站点 `https://blog.changlu.cloud`，用文件验证 / HTML 标签 / CNAME 任一种方式验证。

   ![image-20261006220244456](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610062202551.png)  

   添加网站：

   ![image-20261006220759536](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610062207605.png)

   添加站点属性：

   ![image-20261006221915228](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610062219424.png)  

   选择使用html标签验证：

   ![image-20261007005606721](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610070056866.png)配置如下：

   ![image-20261007005649226](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610070056313.png)  

   验证通过：

   ![image-20261007005833197](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610070058288.png)          

2. **提交 Sitemap**：在"普通收录 → sitemap"提交 `https://blog.changlu.cloud/sitemap.xml`。

   ![image-20261007102257719](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610071022059.png)  

3. **主动推送（F13，可三选一或组合）**：
   + **API 主动推送（推荐，最快）**：发布文章时服务端 POST 到推送接口；
   + **自动推送 `push.js`（最省事）**：页面被访问时自动推送；
   + **手动提交**：后台逐个粘贴 URL。

```html
<!-- 百度自动推送：放进公共 head 片段或 blog-view-ui/index.html -->
<script src="https://zz.bdstatic.com/linksubmit/push.js"></script>
```

**注意：**百度对 JS 渲染支持弱，**服务端直出后收录明显更稳**，这也是 5.1 直出改造对国内收录的关键价值。

### 5.2.4、Bing 平台收录（国外）

**平台入口：**https://www.bing.com/webmasters/

**目标：**让文章进入 Bing（以及共享 IndexNow 的 Yandex 等）索引。

**操作步骤：**

1. **验证站点**：添加 `blog.changlu.cloud`，可直接**从 Google Search Console 导入**，或用 DNS / meta 验证。

   ![image-20261006221133125](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610062211822.png)  

   验证方式选择html验证即可：

   ![image-20261007004403388](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610070044584.png)  

   在网站后台配置配置即可：

   ![image-20261007004720695](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610070047758.png)

   点击Verify即可进行验证通过：   ![image-20261007004642975](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610070046128.png)  

2. **提交 Sitemap**：提交 `https://blog.changlu.cloud/sitemap.xml`。

   提交sitemap：

   ![image-20261007004749290](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610070047521.png)  

   此时状态就是在处理中：

   ![image-20261007004828462](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610070048597.png)

   等待一会即可成功：

   ![image-20261007005106416](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610070051582.png)    

3. **IndexNow 主动推送（F13）**：在站点根目录放 `<你的key>.txt`（内容即 key），发布文章时 POST：

```json
POST https://api.indexnow.org/indexnow
{
  "host": "blog.changlu.cloud",
  "key": "你的key",
  "keyLocation": "https://blog.changlu.cloud/你的key.txt",
  "urlList": ["https://blog.changlu.cloud/blog/513"]
}
```

> Bing 支持从 Google Search Console 导入站点配置，可省去重复验证。

### 5.2.5、读懂收录状态与常见问题

在各平台后台（以 Search Console 的"页面"报告为例）里，常见状态与含义如下：

| 状态 | 含义 | 常见原因 | 对策 |
| --- | --- | --- | --- |
| 已编入索引 | 正常收录 | — | 保持更新 |
| 已发现 - 尚未编入索引 | 引擎知道 URL 但还没抓 | 站点新、权重低、抓取预算不足 | 提交 Sitemap、加内链、耐心等待 |
| 已抓取 - 尚未编入索引 | 抓了但未收录 | 内容质量/重复度高、与其他页雷同 | 提升原创度、完善 description |
| 已排除（alternate/canonical） | 被 canonical 指向别处 | 页面重复、canonical 配错 | 检查 canonical 是否自指标准 URL |
| 已排除（noindex） | 页面上有 noindex | 误加 meta robots | 确认内容页无 noindex |
| 已被 robots.txt 屏蔽 | 抓取被拦截 | robots 规则过严 | 检查是否误 Disallow 文章路径 |

**重点：**看到"已发现 / 已抓取 - 尚未编入索引"是**新手最常见的正常现象**，多数是内容权重和时效问题，先按上表排查，别急着改代码。

### 5.2.6、收录监控节奏

**每周花五分钟**，固定看这五项，别被海量指标淹没：

```plain
① 搜索点击（Search Console）
② 访客数（GA4 / Umami）
③ 热门文章（GA4 / Umami）
④ 来源渠道（GA4 / Umami）
⑤ GitHub / 订阅入口点击（配置事件后看）
```

另外可定期用 `site:blog.changlu.cloud` 粗略核对收录数量，并与 Search Console 的"已编入索引"页面数交叉验证。

---

# 六、验证测试

## 6.1、功能验收测试

> **原则：本节只保留"可整屏截图"的功能级验收；细粒度校验（字段、URL 拼接、HTML 处理、懒加载属性、JSON-LD 字段、XML 合法性等）一律收敛到单元测试（见 6.1.3），不在此重复。**

### 6.1.1、功能测试验证（浏览器 / 后台，可直接截图）

| 序号 | 功能点 | 操作 | 预期结果（截图依据） | 截图 |
| --- | --- | --- | --- | --- |
| 1 | 文章详情页直出 | 打开 `/blog/{id}` | 标题、正文、目录、评论占位正常渲染 | |
| 2 | 文章页 SEO 信息齐全 | 查看网页源代码 | 唯一 `<title>`、`description`、canonical、Open Graph、`application/ld+json` 齐全 | |
| 3 | 分享卡片 | 分享文章链接到微信 / QQ | 出现标题 + 摘要 + 封面卡片 | |
| 4 | 聚合页直出 | 打开 `/home`、`/tag/x`、`/category/x`、`/archives`、`/column/1` | 均正常展示文章列表 | |
| 5 | 站点级文件可访问 | 浏览器打开 `/robots.txt`、`/sitemap.xml`、`/rss.xml` | 分别展示抓取规则 / 文章地图 / 订阅内容 | |
| 6 | SEO 域名配置 | 后台「SEO优化 → SEO配置」填域名并保存 | 保存成功；canonical / sitemap / RSS 使用新域名 | |
| 7 | 核心文件查看 / 下载 | 后台「SEO配置 → 核心文件」点「查看」「下载」 | 弹窗展示内容；下载得到三个文件 | |
| 8 | 阅读页性能 | Lighthouse / PageSpeed Insights | 性能指标达标 | |
| 9 | 收录情况 | 搜索 `site:域名` / Search Console | 有收录与搜索词数据 | |

### 6.1.2、curl 测试验证（整份输出，可直接复制）

> 约定：`https://blog.changlu.cloud` 换成你的 SEO 域名；`513` 换成实际文章 id。

| 序号 | 功能点 | 完整 curl 命令 | 预期结果 | 截图 |
| --- | --- | --- | --- | --- |
| 1 | 详情页直出 | `curl -s -H 'Accept: text/html' https://blog.changlu.cloud/blog/513` | 返回完整文章 HTML | |
| 2 | robots.txt | `curl -s https://blog.changlu.cloud/robots.txt` | 返回规则与 `Sitemap:` 行 | |
| 3 | sitemap.xml | `curl -s https://blog.changlu.cloud/sitemap.xml` | 返回 `<urlset>` 文章地图 | |
| 4 | rss.xml | `curl -s https://blog.changlu.cloud/rss.xml` | 返回 RSS 2.0 订阅源 | |
| 5 | 聚合页 | `curl -s -H 'Accept: text/html' https://blog.changlu.cloud/home` | 返回文章列表 HTML | |
| 6 | 后台收录控制 | `curl -sI https://blog.changlu.cloud/cms/` | 响应头含 `X-Robots-Tag` | |

### 6.1.3、细粒度校验（收敛到单元测试）

下列细粒度点不做人工截图，全部由后端单元测试覆盖，回归时一条命令即可校验：

| 单元测试类 | 覆盖的细粒度点 |
| --- | --- |
| `SeoUrlResolverTest` | `seoDomain` 优先 + 去尾斜杠、外链保持绝对地址 |
| `SeoMetaBuilderTest` | canonical、Open Graph、`BlogPosting` JSON-LD 字段 |
| `SeoHtmlSupportTest` | `h1→h2`、图片 `loading=lazy`、`data-src` 还原、TOC 提取与占位渲染 |
| `SeoFeedServiceTest` | sitemap 含 canonical 且 XML 合法；rss 转义且 XML 合法 |
| `SeoSiteLayoutServiceImplTest` | 站点布局组装、缓存还原、私密文章过滤 |
| `SeoEndpointIntegrationTest` | 详情页 / 标签聚合页直出、站点级文件端点、仅公开文章 |

运行方式：

```bash
cd blog-backend
mvn -Dtest='Seo*Test' test
```

## 6.2、一阶段上线验收清单

| 阶段 | 项目 | 完成 |
| --- | --- | --- |
| M1 | `GET /blog?id=` 返回 `firstPicture` / `description` / 作者 | ☐ |
| M2 | `curl -H 'Accept: text/html' /blog/{id}` 能看到 `<h1>` 与正文 | ☐ |
| M2 | 每页唯一 `<title>`，含站点名后缀 | ☐ |
| M2 | `meta description` 非空且与文章一致 | ☐ |
| M2 | canonical = `/blog/{id}`（无参数），站内链接/Sitemap 一致 | ☐ |
| M3 | `BlogPosting` JSON-LD 通过 Schema/富媒体校验 | ☐ |
| M3 | 分享到微信/QQ 出现标题+摘要+封面 | ☐ |
| M4 | `/robots.txt` 200 且含 Sitemap 行 | ☐ |
| M4 | `/sitemap.xml` 200，仅含公开已发布文章 | ☐ |
| M4 | `/rss.xml` 200，页面含 RSS 自动发现链接 | ☐ |
| M5 | `curl /tag/{name}`、`/category/{name}`、`/archives` 可见列表 | ☐ |
| M5 | `/cms/` 与 `/admin/` 输出 `noindex` / `X-Robots-Tag` | ☐ |
| M6 | Search Console 验证成功并提交 Sitemap | ☐ |
| M6 | GA4 有实时数据；GitHub/订阅点击配置事件 | ☐ |

## 6.3、实施里程碑

| 里程碑 | 内容 | 交付物 | 预计改动 |
| --- | --- | --- | --- |
| **M1 数据与地基** | F1 数据层补齐；`SeoUrlResolver` / `SeoMeta` / `SeoMetaBuilder` 骨架 | 详情接口含封面/摘要/作者 | 3 个类 + Mapper |
| **M2 详情页直出** | F2 + F3；修改 `SpaForwardConfig`；`seo/blog.html` + `seo-head.html` | `curl /blog/{id}` 可见正文与 title | 1 Controller + 2 模板 + 过滤器 |
| **M3 结构化与传播** | F4 canonical + F5 JSON-LD + F6 OG | 校验工具通过、分享卡片正确 | 扩展 Builder + 片段 |
| **M4 站点级文件** | F7 robots + F8 sitemap + F9 rss | 三个端点 200 | 1 Controller + Generator |
| **M5 聚合页与收录控制** | F10 聚合页直出 + F11 noindex | 聚合页可抓取、后台不被收录 | 复用列表模板 |
| **M6 收录推进与数据监测** | F12 Search Console + GA4；F13 主动推送；F15 SEO 后台配置 | 有收录与流量数据 | 配置 + 片段注入 + 推送 + 后台配置项 |

**建议顺序：**M1 → M2 → M3 → M4 完成即"能抓能收"，可先提交 Search Console；M5、M6 紧随。

## 6.4、风险与注意事项

| 风险 | 说明 | 对策 |
| --- | --- | --- |
| SPA 回退抢请求 | 不改 `SpaForwardConfig` 则 Controller 永不生效 | M2 必做过滤器放行 |
| `/blog` 前缀误伤接口 | `/blogs`、`/blog?id=` 也是该前缀 | 过滤器只对 `Accept: text/html` 且无扩展名的导航请求放行 |
| 直出页面样式与 SPA 不一致 | 两套页面维护成本 | 一阶段样式精简，逐步统一；不做 UA 嗅探以免 cloaking |
| 首屏渲染性能 | Thymeleaf 直出正文可能变大 | 图片懒加载、静态资源缓存；P2 再优化 |
| canonical/绝对 URL 配错域名 | 多环境（local/test/prod） | 统一走 `SeoUrlResolver` + `blog.view` 配置 |
| 私密文章泄露 | 密码保护文章 | Sitemap/RSS/聚合页一律排除 `password` 非空文章；仍需鉴权兜底 |
| `dateModified` 造假 | 每次刷新时间伪装更新 | 取真实 `update_time` |

---

# 七、总结

回到最开始那个场景——**文章发布了却搜不到**。经过这一轮的梳理，我们其实已经把问题拆解得很清楚了：

```plain
问题：爬虫拿到空壳
  ↓
方案：后端 Thymeleaf 直出（方案一）
  ↓
最小闭环：F1 数据补齐 → F2 详情页直出 → F3 meta → F4 canonical → F5 JSON-LD → F6 OG → F7 robots → F8 sitemap
  ↓
进阶：F9 rss → F10 聚合页 → F11 收录控制 → F12 数据监测 → F13 主动推送 → F14 性能
```

**一句话总结：一阶段先改 `SpaForwardConfig` 放行、补齐 `BlogDetail` 字段、用 Thymeleaf 把 `/blog/{id}` 正文与 meta 直出，再补 `robots / sitemap / rss` 三个端点，最后接上 Search Console 与统计。做完这些，BlogLoom 才真正从"爬虫眼里的空壳"变成"能被收录的博客"。**

至于第二阶段（性能、内链）、第三阶段（变现）、第四阶段（GEO），都建立在这一阶段的内容与流量之上，不必颠倒顺序。

---

# 参考资料

[1]. [Sitemaps XML 协议（Sitemaps.org）](https://www.sitemaps.org/protocol.html)

[2]. [RFC 9309：Robots Exclusion Protocol](https://www.rfc-editor.org/rfc/rfc9309.html)

[3]. [RFC 6596：The Canonical Link Relation](https://www.rfc-editor.org/rfc/rfc6596.html)

[4]. [Schema.org 官方站点](https://schema.org/)

[5]. [JSON-LD 1.1（W3C）](https://www.w3.org/TR/json-ld11/)

[6]. [RSS 2.0 Specification（RSS Advisory Board）](https://www.rssboard.org/rss-specification)

[7]. [Google 搜索中心：针对生成式 AI 功能优化网站](https://developers.google.com/search/docs/fundamentals/ai-optimization-guide)

[8]. [Google Search Console](https://search.google.com/search-console/)

[9]. [Google Analytics（GA4）](https://analytics.google.com/)

[10]. [CSDN 参考文章（文章页拆解对象）](https://blog.csdn.net/cl939974883/article/details/167174108)

[11]. [BlogLoom 开源项目（GitHub）](https://github.com/changluya/BlogLoom)

---

整理者：长路　创建时间：2026.10.6　更新时间：2026.10.6
