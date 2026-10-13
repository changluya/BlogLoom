---
title: "BlogLoom：支持全站 SEO 与 Google AdSense 的开源博客 CMS"
description: 自托管博客与内容管理，提供 Markdown 创作、全站 SEO、Google AdSense 接入以及一键部署与升级。
mode: custom
toc: false
---

<div className="blogloom-landing">

<div className="hs-hero">

<div>

<h1 className="hs-hero__headline">
自托管开源博客 CMS，<span className="hs-hero__accent">全站 SEO 与 Google AdSense 接入</span>。
</h1>

<p className="hs-hero__desc">
BlogLoom 将 Markdown 创作、内容管理、读者互动和运营分析集中到一套可自托管平台。后台统一维护全站 SEO、订阅文件和 Google AdSense 广告配置，支持 Docker 一键部署与版本升级。
</p>

<div className="hs-cta">
 <a href="/v1/zh/guide/quickstart" className="hs-btn hs-btn--primary">快速开始 →</a>
 <a href="/v1/zh/guide/index" className="hs-btn hs-btn--secondary">阅读文档</a>
</div>

</div>

<div className="hs-window">

<div className="hs-tabs" role="tablist">
<button className="hs-tab active" data-panel="backend" aria-pressed="true">后端</button>
<button className="hs-tab" data-panel="cms" aria-pressed="false">管理后台</button>
<button className="hs-tab" data-panel="view" aria-pressed="false">博客前台</button>
</div>

<div className="hs-code-panel" id="backend">
```bash
cp bin/local/deploy.conf.example bin/local/deploy.conf
bin/local/upgrate-sql.sh

cd blog-backend
mvn spring-boot:run
```
</div>

<div className="hs-code-panel" id="cms" style={{"display": "none"}}>
```bash
cd blog-cms-ui
npm install
npm run dev
# http://localhost:8079
```
</div>

<div className="hs-code-panel" id="view" style={{"display": "none"}}>
```bash
cd blog-view-ui
npm install
npm run dev
# http://localhost:8080
```
</div>

</div>

</div>

<div className="hs-adoption">

<div className="hs-adoption__eyebrow">
 <span className="hs-adoption__eyebrow-dot"></span>开箱即用
</div>

<div className="hs-adoption__stats">

<div className="hs-stat">
 <span className="hs-stat__val">三端分离</span> <span className="hs-stat__label">前台 / 后台 / 服务独立运行与构建</span>
</div>

<div className="hs-stat">
 <span className="hs-stat__val">完整闭环</span> <span className="hs-stat__label">创作 → 审核 → 展示 → 互动 → 统计</span>
</div>

</div>

</div>

<div className="hs-section">

<div className="hs-chip">核心原则</div>

<h2>关注从内容生产到站点运营的完整链路。</h2>

<p>
BlogLoom 把文章、分类、标签、专栏、动态、评论与站点配置统一纳入内容模型，前台展示与后台管理共享同一套后端接口。
</p>

<ul>
<li><strong>前后端分离</strong> —— 公开博客、管理后台与 REST API 独立运行、独立构建。</li>
<li><strong>Markdown 优先</strong> —— 支持 Markdown 编辑、目录、代码内容、图片与文章描述。</li>
<li><strong>站点可配置</strong> —— 站名、头像、轮播、社交链接、友链与关于页集中维护。</li>
</ul>

<a href="/v1/zh/guide/architecture" className="hs-btn hs-btn--secondary">了解系统架构 →</a>

</div>

<div className="hs-section">

<div className="hs-chip">核心能力</div>

<h2>一个博客平台该有的能力，这里都有。</h2>

<div className="hs-cards">

 <div className="hs-card">
  <h3>内容创作</h3>
  <p>Markdown 编辑、本地知识库、分类标签、专栏与发布配置。</p>
  <a href="/v1/zh/guide/admin/article-editor">了解更多 →</a>
 </div>

 <div className="hs-card">
  <h3>博客前台</h3>
  <p>首页、文章、归档、搜索、动态、自定义展示与评论互动。</p>
  <a href="/v1/zh/guide/frontend/home">了解更多 →</a>
 </div>

  <div className="hs-card">
   <h3>管理后台</h3>
   <p>仪表盘、内容管理、回收站、站点设置、SEO 与广告、日志中心。</p>
   <a href="/v1/zh/guide/admin/dashboard">了解更多 →</a>
  </div>

  <div className="hs-card">
   <h3>SEO 与变现</h3>
   <p>公开页面服务端直出，robots / sitemap / rss，站长平台验证与 Google AdSense。</p>
   <a href="/v1/zh/guide/admin/seo-and-monetization">了解更多 →</a>
  </div>

  <div className="hs-card">
   <h3>图床存储</h3>
   <p>本地上传与阿里云 OSS 等存储接入，支持连通性测试。</p>
   <a href="/v1/zh/integration/storage/local">了解更多 →</a>
  </div>

  <div className="hs-card">
   <h3>运营数据</h3>
   <p>PV、UV、内容数量、分类标签文章数聚合与访客地域。</p>
   <a href="/v1/zh/guide/admin/dashboard">了解更多 →</a>
  </div>

  <div className="hs-card">
   <h3>部署运维</h3>
   <p>环境要求、安装部署、Docker 一键部署、配置与故障排查。</p>
   <a href="/v1/zh/deploy/index">了解更多 →</a>
  </div>

</div>

</div>

<div className="hs-section">

<div className="hs-chip">开始使用</div>

<h2>准备好搭建你的博客了吗？</h2>

<p>
跟随快速开始，完成数据库初始化、启动后端并运行博客前台与管理后台。全过程基于 JDK、Maven 与 Node.js；也可以使用 Docker 一键部署，无需在服务器安装 JDK 或 Node.js。
</p>

<a href="/v1/zh/guide/quickstart" className="hs-btn hs-btn--primary">开始搭建 →</a>

</div>

<div className="hs-faq">

<h2>常见问题</h2>

<Accordion title="需要哪些运行环境？">
`JDK 8+`、`Maven 3.6+`、`Node.js 16+`，以及 `MySQL 5.7+`。详见[环境要求](/v1/zh/deploy/environment)。
</Accordion>

<Accordion title="默认后台账号是什么？">
初始化全量 SQL 后，默认用户名为 `admin`，密码为 `123456`，首次登录后请立即修改。
</Accordion>

<Accordion title="支持哪些图片存储方式？">
内置本地上传，并支持阿里云 OSS 图床配置，详见[图床存储](/v1/zh/integration/storage/local)。
</Accordion>

<Accordion title="可以二次开发吗？">
可以。后端分层清晰，前台与管理端职责独立，方便更换主题或扩展接口。
</Accordion>

<Accordion title="BlogLoom 与 NBlog 是什么关系？">
BlogLoom 基于 [Naccl/NBlog](https://github.com/Naccl/NBlog) 二次开发，在遵循 MIT License 的基础上完成品牌化、工程整理与体验优化。
</Accordion>

</div>

</div>
