---
title: "BlogLoom v1"
description: An open-source blogging platform for developers and creators
mode: custom
toc: false
---

<div className="blogloom-landing">

<div className="hs-hero">

<div>

<h1 className="hs-hero__headline">
From Markdown to publishing, <span className="hs-hero__accent">an open-source blog CMS in one place</span>.
</h1>

<p className="hs-hero__desc">
BlogLoom is built with Spring Boot, MyBatis and Vue, and ships as three parts: a public blog frontend, a content management console, and a backend service. Writing, review, publishing, comments and analytics all live in one decoupled system — ideal for personal tech blogs, open-source project sites, or a content platform you can keep extending.
</p>

<div className="hs-cta">
 <a href="/v1/en/guide/quickstart" className="hs-btn hs-btn--primary">Quick start →</a>
 <a href="/v1/en/guide/index" className="hs-btn hs-btn--secondary">Read the docs</a>
</div>

</div>

<div className="hs-window">

<div className="hs-tabs" role="tablist">
<button className="hs-tab active" data-panel="backend" aria-pressed="true">Backend</button>
<button className="hs-tab" data-panel="cms" aria-pressed="false">Admin</button>
<button className="hs-tab" data-panel="view" aria-pressed="false">Blog</button>
</div>

<div className="hs-code-panel" id="backend">
```bash
mysql -u root -p < sql/increment/init.sql

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
 <span className="hs-adoption__eyebrow-dot"></span>Batteries included
</div>

<div className="hs-adoption__stats">

<div className="hs-stat">
 <span className="hs-stat__val">3 apps</span> <span className="hs-stat__label">Frontend, admin and API run and build independently</span>
</div>

<div className="hs-stat">
 <span className="hs-stat__val">Full loop</span> <span className="hs-stat__label">Write → review → publish → engage → analyze</span>
</div>

</div>

</div>

<div className="hs-section">

<div className="hs-chip">Core principles</div>

<h2>Built around the whole content lifecycle.</h2>

<p>
BlogLoom brings posts, categories, tags, columns, moments, comments and site configuration into one content model, shared by the public blog and the admin console through a single REST API.
</p>

<ul>
<li><strong>Decoupled</strong> — the public site, admin console and REST API build and run independently.</li>
<li><strong>Markdown first</strong> — Markdown editing, table of contents, code blocks, images and descriptions.</li>
<li><strong>Configurable site</strong> — name, avatar, carousel, social links, friends and about page in one place.</li>
</ul>

<a href="/v1/en/guide/architecture" className="hs-btn hs-btn--secondary">See the architecture →</a>

</div>

<div className="hs-section">

<div className="hs-chip">Capabilities</div>

<h2>Everything a blog platform needs.</h2>

<div className="hs-cards">

<div className="hs-card">
 <h3>Authoring</h3>
 <p>Markdown editor, categories, tags, cover and publishing options.</p>
 <a href="/v1/en/guide/admin/article-editor">Learn more →</a>
</div>

<div className="hs-card">
 <h3>Public blog</h3>
 <p>Home, articles, archive, search, moments and comments.</p>
 <a href="/v1/en/guide/frontend/home">Learn more →</a>
</div>

<div className="hs-card">
 <h3>Admin console</h3>
 <p>Dashboard, content management, site settings and logs.</p>
 <a href="/v1/en/guide/admin/dashboard">Learn more →</a>
</div>

<div className="hs-card">
 <h3>Image hosting</h3>
 <p>Local upload plus GitHub, Upyun and Tencent Cloud storage.</p>
 <a href="/v1/en/integration/storage/local">Learn more →</a>
</div>

<div className="hs-card">
 <h3>Analytics</h3>
 <p>PV, UV, content counts, category and tag distribution, visitor map.</p>
 <a href="/v1/en/guide/admin/dashboard">Learn more →</a>
</div>

<div className="hs-card">
 <h3>Deployment</h3>
 <p>Environment, installation, configuration and troubleshooting.</p>
 <a href="/v1/en/deploy/index">Learn more →</a>
</div>

</div>

</div>

<div className="hs-section">

<div className="hs-chip">Get started</div>

<h2>Ready to launch your blog?</h2>

<p>
Follow the quick start to initialize the database, run the backend, and launch both the public blog and the admin console. All you need is JDK, Maven and Node.js.
</p>

<a href="/v1/en/guide/quickstart" className="hs-btn hs-btn--primary">Start building →</a>

</div>

<div className="hs-faq">

<h2>FAQ</h2>

<Accordion title="What are the runtime requirements?">
`JDK 8+`, `Maven 3.6+`, `Node.js 16+`, plus `MySQL 5.7+` and `Redis 5+`. See [Environment](/v1/en/deploy/environment).
</Accordion>

<Accordion title="What is the default admin account?">
After importing the full SQL, the default account is `admin` with password `123456`. Change it immediately after the first login.
</Accordion>

<Accordion title="Which image storage options are supported?">
Local upload is built in, with GitHub, Upyun and Tencent Cloud integrations. See [Image storage](/v1/en/integration/storage/local).
</Accordion>

<Accordion title="Can I extend it?">
Yes. The backend is clearly layered, and the public site and admin console are independent, making it easy to change themes or add APIs.
</Accordion>

<Accordion title="How is BlogLoom related to NBlog?">
BlogLoom is a derivative of [Naccl/NBlog](https://github.com/Naccl/NBlog), rebranded and reorganized under the original MIT License.
</Accordion>

</div>

</div>
