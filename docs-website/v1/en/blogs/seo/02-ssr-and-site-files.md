---
title: "BlogLoom SEO 02: Why SPA Blogs Need Server-Side Rendering and Site-Level Files"
description: Phase-one core overhaul: server-side rendering of public pages, robots / sitemap / rss site-level files, indexing control and structured data.
---

# SEO Research 02: Phase-One Core Overhaul, Server-Side Rendering and Site-Level Files

> Series:
> - Previous: [SEO Research 01: Article page optimization, monetization and data monitoring](/v1/en/blogs/seo/01-article-page-monetization)
> - This article: **02, Phase-One Core Overhaul, server-side rendering and site-level files** (the BlogLoom implementation piece)

> **Positioning of this article:** Article 01 is the "textbook + dissection" piece, answering **"what SEO is, how others do it, and where to see the data"**; this article is the "implementation" piece, converging the capabilities from 01 into BlogLoom's phase-one minimal scope and giving executable functional requirements and engineering solutions. So read 01 first for the big picture, then this one to actually make changes; the end of this article also maps its sections to 01's.

When applying search engine optimization (SEO) to the open-source blog platform BlogLoom, we ran into a very concrete problem:

> **You painstakingly finish an article and publish it on your own blog, yet you can't find it on Google or Baidu no matter how you search.**

It's not that the content is bad, nor that nobody visits — it's that **search engines simply can't read our articles**. This article revolves around that problem, walking through BlogLoom's phase-one SEO overhaul from "why do it" to "how to do it" to "how to verify it".

The goal of this article: **roughly sort out the core functional requirements to optimize in the next phase, and give actionable overhaul designs by checking each against BlogLoom's current core features.**

---

# 1. Background: why BlogLoom must do SEO

## 1.1 A real scenario: published but unsearchable

BlogLoom is an open-source blog platform I built:

+ Open source: <https://github.com/changluya/BlogLoom>
+ Stack: Spring Boot 2.2.7 + MyBatis + Vue 2.6 + Vite
+ Positioning: for individual developers / content creators to self-host a blog, and easy to extend
+ Live site: <https://blog.changlu.cloud/>

It consists of three parts:

| Module | Role |
| --- | --- |
| `blog-view-ui` | Blog front end (Vue2 SPA), where readers read articles |
| `blog-cms-ui` | Admin console (Vue SPA), where authors write articles |
| `blog-backend` | Backend service (Spring Boot APIs + MyBatis), providing data |

The feature set is already quite complete: Markdown writing, categories/tags/columns, comments, site config, traffic stats, one-command Docker deployment... **it's just that "search engine indexing" is almost a blank.**

So the question arises: **the article content is all in the database — why can't search engines see it?**

## 1.2 First, understand how search engines work

Before acting, understand roughly how search engines work, so every later step has a basis.

| Stage | What the search engine does | What BlogLoom must solve |
| --- | --- | --- |
| Discovery | Finds your article URL | Navigation, article links and a Sitemap |
| Crawling | Visits the URL and reads the page | The page is reachable and the body readable |
| Indexing | Analyzes the page and decides whether to include it | Clear, valuable content; fewer duplicate pages |
| Ranking & display | Picks relevant pages when a user searches | Content matches search intent; good site experience |

**Key point: submitting a URL does not guarantee indexing, and indexing does not guarantee a high ranking.** In phase one we only solve the first two rings — **being discoverable and crawlable**.

## 1.3 Current state measured: BlogLoom is an empty shell in the crawler's eyes

Saying "there's no SEO" is too vague; let's visit the live site and see the results.

| Request | Response | Note |
| --- | --- | --- |
| `GET https://blog.changlu.cloud/` | 200, but just an empty shell | The source only has `<title>BlogLoom</title>` and an empty `<div id="app">`, with not a single character of body |
| `GET /robots.txt` | 404 | No crawl-rules file |
| `GET /sitemap.xml` | 404 | No sitemap |
| `GET /rss.xml` | 404 | No feed |
| `GET /blog/513` (article link) | 404 | Opening the article link directly returns 404 |
| `GET /blog?id=513` (API) | 200, with the full body | The content is all there; it's just not rendered onto the page |

Two painful points here:

+ **The crawler gets an empty shell.** The body is rendered after the browser runs JS and calls APIs with axios; a crawler doesn't run JS, so it reads nothing.
+ **Even opening the article link directly 404s.** The front end uses history routing, but the server has no fallback — clicking through from the home page works, but copy the link to someone or refresh the page and it 404s.

The request chain is roughly:

```plain
Browser / crawler
   ↓
blog-view-ui's index.html (empty shell, <title>BlogLoom</title>)
   ↓ browser runs JS
axios calls blog-backend's /blog API
   ↓
Vue renders the body in the browser and changes the title with document.title
```

The result is that crawlers and real users see two entirely different worlds:

| Who's looking | What they see |
| --- | --- |
| Crawler (no JS) | `<title>BlogLoom</title>` + an empty `div`, no body, no description |
| User (browser) | Body, title, comments — all displayed normally |

**In one sentence: it's not that the content is bad, it's that search engines simply can't read it.** This is the biggest current problem and the first thing to solve.

## 1.4 Scope and goals of this phase

We need something to look forward to with SEO too. We actually want just three things, in priority order:

| Priority | Goal | How to judge success |
| --- | --- | --- |
| ① | **Be indexable** | Searching `site:blog.changlu.cloud` on Google / Bing / Baidu shows my articles |
| ② | **Do SEO well** | Each article has its own title, summary, canonical and structured data; the site has sitemap / robots / rss / share cards |
| ③ | **See the data** | Search Console for indexing and keywords, GA4 / Umami for traffic sources, forming a weekly review habit |

Around this goal, the article divides scope by phase:

| Phase | Topic | In scope here |
| --- | --- | --- |
| Phase 1 | Server-side rendering of content pages + page-level meta + site-level files (robots / sitemap / rss) + indexing control | ✅ This article |
| Phase 1 | Data monitoring integration (Search Console / GA4 or Umami) | ✅ This article (mostly config, little code) |
| Phase 2 | Reading page performance (Core Web Vitals), deeper on-site interlinking, active push | ⏳ Later |
| Phase 3 | Monetization (AdSense / CPS / sponsorship) | ✅ Previewed here (see 4.4, Google AdSense first) |
| Phase 4 | GEO (`geo:*` meta-information + credibility building) | ⏳ Later |

**Key point: first be crawlable, then talk about indexing; first be indexed, then data is worth looking at.** So the phase-one and phase-two technical foundations must be solid first.

---

# 2. Core concepts: which capabilities phase one must add

## 2.1 Overview of the eight basic capabilities

SEO isn't a single-point feature but a "content infrastructure". For a blog platform, the core is these 8 capabilities:

| Capability | Problem solved | What BlogLoom must do |
| --- | --- | --- |
| Independent URL | Each article has a stable address | Backend provides a `/blog/{id}` content page |
| `<title>` | Tells search engines what the page is about | A unique title per page (article title + site name) |
| `description` | Page summary | Output `<meta name="description">` |
| canonical | Declares the preferred URL when the same content has multiple | Output a parameter-free standard URL |
| Structured data | Lets search understand content type, author, time | Output `BlogPosting` JSON-LD |
| Open Graph | Share cards for social platforms | Output `og:title/description/image/url` |
| Sitemap / robots / RSS | Discovery, crawling, subscription | Provide three site-level files |
| Body rendered directly | Crawlers can read the body | Server-render into the first-screen HTML |

## 2.2 Capabilities vs. current state

So which does BlogLoom have and which does it lack? Let's check against the real code.

| Capability required by the research doc | BlogLoom current state | Key code location | Gap |
| --- | --- | --- | --- |
| Independent URL | Has `/blog/:id` (front-end route), but no backend page of the same name | `blog-view-ui/src/router/index.js:30-35` | Backend has no server-rendered page |
| Independent `<title>` | Only changes `document.title` via JS | `views/blog/Blog.vue:180`, `views/Index.vue:134` | The first-screen HTML is fixed to `BlogLoom` |
| `description` | Already returned by the detail API; when empty, generated from the body | `model/vo/BlogDetail.java`; `BlogMapper.xml:293-323` | Detail field mapping completed |
| canonical | None | — | All missing |
| Structured data | None | — | All missing |
| Open Graph | None | — | All missing |
| robots / sitemap / rss | None (all 404) | — | All missing |
| Body rendered directly | No (`v-html` client-side rendering) | `views/blog/Blog.vue:34` | No body in the first screen |
| Aggregation pages crawlable | Tag/category/archive are all SPA | `views/tag/Tag.vue` etc. | Crawler can't read |
| Indexing control | `/cms/`, `/admin/` have no `noindex` | — | May be mistakenly indexed |

## 2.3 Core technical constraints (must know before overhaul)

**Note:** the following constraints directly determine the overhaul approach — read them carefully first.

| No. | Constraint | Description |
| --- | --- | --- |
| 1 | The SPA forward filter "steals" requests | `SpaForwardFilter` intercepts all requests with order `Integer.MIN_VALUE`; a GET HTML request like `/blog/1` **gets forwarded to `index.html` first** and never reaches the backend Controller |
| 2 | `.xml` / `.txt` endpoints are unaffected | The filter doesn't forward requests whose last segment has an extension (`SpaForwardConfig.java:85-87`), so `/sitemap.xml`, `/rss.xml`, `/robots.txt` can be served directly by a Controller |
| 3 | All public APIs are unauthenticated | In `SecurityConfig`, `anyRequest().permitAll()` (`config/SecurityConfig.java:56`); new SEO endpoints are public by default |
| 4 | No `slug` field | Neither the `blog` table nor the entity has a slug; routes use numeric ids, and phase one can keep using ids |
| 5 | Detail API fields must stay in sync with the SEO data flow | `BlogDetail` and `BlogMapper.xml:293-323` explicitly map cover, summary and author info, avoiding the list API having data while the detail API loses it |
| 6 | Same-origin deployment | In production the API uses relative paths (`VITE_API_URL=/`, `/admin/`); meta URLs must be composed with the real domain from the `blog.view` config |

**Key point:** constraint 1 is the most easily overlooked and the most fatal pitfall. Without changing the filter, no amount of Controllers written later will take effect.

---

# 3. Approach: solution selection and overall architecture

## 3.1 Comparing three server-side rendering routes

Since the core problem is "crawlers can't read it", the page must **output the body on the server side**. There are three routes, each with trade-offs.

| Route | Approach | Pros | Cost | Suits |
| --- | --- | --- | --- | --- |
| **Option 1: Backend direct rendering** | The backend renders `/blog/{id}` with Thymeleaf, returning HTML with the body and meta | Best fits the existing Spring Boot (the project already depends on thymeleaf); best and most stable indexing | Detail/list pages must be split from the SPA; backend templates change | **Recommended main route** |
| **Option 2: Pre-rendering / dynamic rendering** | Render with a headless browser at deploy time or for crawler UAs, then return HTML | Front end largely unchanged; can be a transition | Needs an extra service; content updates require re-rendering; cloaking risk | Short-term transition |
| **Option 3: Migrate to an SSR framework** | Switch to Nuxt / Next etc. | Both experience and SEO | Large Vue2 migration cost | Long-term refactor |

**Why not the other two:**

+ Option 2 returns one version to crawlers and another to users, which is "dynamic rendering". Google has clearly stated **dynamic rendering is only a transitional approach**; long-term you should use server-side rendering or static generation, and mishandling can be judged as cloaking.
+ Option 3 is the most elegant but requires rewriting the entire front end; for this project (Vue2) the phase-one cost is too high to be worthwhile.

## 3.2 Why choose backend Thymeleaf direct rendering

Our choice: **adopt Option 1 directly, and do no UA sniffing.**

Three reasons:

1. **Zero new dependencies**: `spring-boot-starter-thymeleaf` is already in `pom.xml:120-124`, currently used only for emails; the capability is ready.
2. **Most stable indexing**: content pages return **the same** server-rendered HTML to readers and crawlers, complying with search-engine specs.
3. **Controllable changes**: only "pages that need indexing" go to the backend; the admin console (`blog-cms-ui`), login, etc. stay SPA.

**Key point: content pages must return the same HTML to readers and crawlers, never two versions, or there's a cloaking risk.**

## 3.3 Target request chain

After the overhaul, a request flows like this:

```plain
Browser / crawler
   │
   ├─ GET /blog/513
   │        ↓
   │   SpaForwardFilter: hits the "server-render allowlist" → no forward
   │        ↓
   │   SeoPageController.getBlogPage(): query DB → inject model
   │        ↓
   │   Thymeleaf renders seo/blog.html (first screen has body + title/description/canonical/OG/JSON-LD)
   │        ↓
   │   Crawler gets full HTML; reader sees the article (can embed comments etc.)
   │
   ├─ GET /sitemap.xml   → SeoFileController (produces=application/xml)
   ├─ GET /rss.xml       → SeoFileController
   └─ GET /robots.txt    → SeoFileController (text/plain)
```

## 3.4 New code structure

**Convention: all SEO-related code goes in a dedicated `seo` module under `blog-backend/src/main/java/com/changlu/blogloom/module`**, consistent with the existing `api / service / domain / support` layering of `module/column` and `module/knowledge`, rather than scattering into the top-level `controller`, `service`, `model` packages.

```plain
blog-backend/src/main/java/com/changlu/blogloom/module/seo/
├── api/                               # Interface layer (Controllers)
│   ├── SeoConfigController.java       # Backend SEO config (GET/POST /admin/seo/config) ✅ done
│   ├── SeoVerificationController.java # Backend SEO platform association ✅ done
│   ├── SpaIndexController.java        # Inject site-verification meta into the SPA entry (option B) ✅ done
│   ├── SeoPageController.java         # Content page rendering (@Controller, returns Thymeleaf views)
│   └── SeoFileController.java         # robots / sitemap / rss (@RestController, text/XML)
├── service/                           # Business layer
│   ├── SeoMetaService.java            # Assemble page SEO metadata (title/description/canonical/og/ldJson)
│   ├── SitemapService.java            # Generate sitemap.xml
│   └── RssService.java                # Generate rss.xml
├── domain/                            # Domain models
│   ├── SeoConfig.java                 # Backend SEO config carrier (seoDomain etc.) ✅ done
│   ├── SeoVerification.java           # Site-verification carrier (baidu/bing/google) ✅ done
│   └── SeoMeta.java                   # Common meta carrier (title/description/canonical/og/ldJson)
├── support/                           # Helpers
│   ├── SeoUrlResolver.java            # Unified canonical / absolute URL building (backend seoDomain, fallback blog.view)
│   └── SeoMetaBuilder.java            # BlogDetail → SeoMeta
└── dao/                               # Optional: independent queries for the plugin

blog-backend/src/main/resources/templates/seo/   # Thymeleaf templates stay under resources
├── layout.html                        # Common skeleton (header/footer)
├── blog.html                          # Article detail page
├── list.html                          # Common list/tag/category/archive page
└── fragments/
    └── seo-head.html                  # title/description/canonical/og/JSON-LD fragment
```

**Why group this way:**

| Reason | Description |
| --- | --- |
| Consistent with the existing architecture | `column` and `knowledge` all use `api/dao/domain/service/support`; SEO follows the same layering, lowering the learning cost |
| Cohesive responsibility | SEO's controllers, business, models and helpers are all under `module/seo`, easy to find and maintain |
| Easy to evolve | Adding backend SEO config (F15), site settings etc. continues in `module/seo`, not polluting other modules |
| Template exception | Thymeleaf resolves from `classpath:/templates/` by convention, so templates go in `resources/templates/seo/`, not inside Java packages |

> In one sentence: **Java code goes into `module/seo`, templates go into `resources/templates/seo`.**

## 3.5 Data flow: model once, reuse site-wide

**Key point:** all meta comes from the single source of article data; templates hardcode no title or description.

```plain
blog table (existing fields)
   ├─ title        → <title> / og:title / JSON-LD headline
   ├─ description  → meta description / og:description
   ├─ first_picture→ og:image / JSON-LD image
   ├─ create_time  → article:published_time / datePublished
   ├─ update_time  → article:modified_time / dateModified / sitemap lastmod
   ├─ category     → article:section / category aggregation page
   ├─ tags         → article:tag / tag aggregation page
   └─ id           → canonical / og:url / sitemap loc
```

**Three principles throughout:**

1. **Data-driven templates**: article data → one common template fragment → generate all meta, no hardcoding, no missing fields.
2. **Server-side rendering**: body and meta are in the first-screen HTML, not dependent on JavaScript.
3. **Model once, reuse site-wide**: `title / summary / cover / time / tags` defined once in the database, shared by the detail page, Sitemap, RSS and OG.

---

# 4. Core functional requirements

## 4.1 Priority overview

The overhaul can't be done all at once; it needs priorities. The 14 functional points are split into 6 groups by optimization type, each listing the **core optimization point** (where the change is), the goal and acceptance criteria.

**Group 1: Content rendering and data foundation (P0)**

| No. | Feature | Priority | Core optimization point | Goal | Acceptance |
| --- | --- | --- | --- | --- | --- |
| **F1** | Data-layer completion | P0 | Add fields to the `BlogDetail` VO + supplement columns and join the author in the `BlogMapper.xml` detail query | Let detail data support all meta | `BlogDetail` returns `firstPicture`, `description`, author |
| **F2** | Server-side rendering of article detail | P0 | Add `SeoPageController` + modify `SpaForwardConfig` allowlist + Thymeleaf template `seo/blog.html` | `curl /blog/{id}` gets body + correct title | Body and meta remain in the source with JS disabled |

**Group 2: Page-level SEO meta-information (P0)**

| No. | Feature | Priority | Core optimization point | Goal | Acceptance |
| --- | --- | --- | --- | --- | --- |
| **F3** | Page-level SEO meta | P0 | Common fragment `templates/seo/fragments/seo-head.html` outputs `title` / `description` | title / description | Unique title per page; non-empty description |
| **F4** | canonical and URL strategy | P0 | `SeoUrlResolver` uniformly builds absolute URLs (reads `blog.view`) | Declare a preferred URL for the same content | canonical points to a parameter-free standard URL |
| **F5** | Structured data JSON-LD | P0 | `SeoMetaBuilder` generates `BlogPosting`, output in `seo-head.html` | `BlogPosting` lets search understand the article | Rich results test tool passes |
| **F6** | Open Graph | P0 | `og:*` and `article:*` tags in `seo-head.html` | Correct share cards | WeChat/QQ/Slack shares show title+summary+cover |

**Group 3: Site-level indexing files (P0)**

| No. | Feature | Priority | Core optimization point | Goal | Acceptance |
| --- | --- | --- | --- | --- | --- |
| **F7** | robots.txt | P0 | Add `SeoFileController.robots()` | Provide crawl rules and declare the Sitemap | `/robots.txt` 200, with a Sitemap line |
| **F8** | sitemap.xml | P0 | `SeoFileController.sitemap()` + public article query | List all public articles | `/sitemap.xml` 200, only public published articles |
| **F9** | RSS/Atom subscription | P1 | `SeoFileController.rss()` | Readers can subscribe; readers can discover | `/rss.xml` 200, with `<link rel="alternate">` in HTML |

**Group 4: Crawlable aggregation pages (P1)**

| No. | Feature | Priority | Core optimization point | Goal | Acceptance |
| --- | --- | --- | --- | --- | --- |
| **F10** | Crawlable aggregation pages | P1 | `SeoPageController` reuses the common template `seo/list.html` | List/tag/category/archive pages rendered directly | `curl /tag/{name}` shows the article list |

**Group 5: Indexing control and data monitoring (P1)**

| No. | Feature | Priority | Core optimization point | Goal | Acceptance |
| --- | --- | --- | --- | --- | --- |
| **F11** | Indexing control | P1 | Add `noindex` to `blog-cms-ui/index.html` + `X-Robots-Tag` on `/cms`, `/admin` | Prevent the backend from being indexed | `/cms/` and `/admin/` output `noindex` |
| **F12** | Data monitoring integration | P1 | Search Console + GA4 script injected into the common fragment | See indexing and traffic | Search Console verified; GA4 has data |
| **F15** | Backend SEO config | P1 | Add SEO domain (`seoDomain`), GA4 script, event-reporting switch in site settings; backend injects per config; **plus AdSense publisher ID and ad-slot config** | SEO domain / analytics script / event reporting / ad config all configurable in the backend | Fill the SEO domain and it's used for canonical/sitemap; fill the GA4 ID and it takes effect; fill the AdSense ID and ads are injected; a switch controls event reporting |

**Group 6: Acceleration and performance optimization (P2)**

| No. | Feature | Priority | Core optimization point | Goal | Acceptance |
| --- | --- | --- | --- | --- | --- |
| **F13** | Active push (optional) | P2 | Integrate Baidu `push.js` / Bing IndexNow | Speed up indexing | Baidu/Bing push has success records |
| **F14** | Reading page performance | P2 | Body image lazy loading + async third-party scripts in `index.html` | LCP/CLS on target | Images lazy-load; third-party scripts async |

## 4.2 Phase-one minimal loop

So which must be done first? The answer:

> **F1 + F2 + F3 + F4 + F5 + F6 + F7 + F8**

Doing these eight lets crawlers read the content and submit for indexing. F9–F12 and F15 follow closely, with F13 and F14 as finishing optimizations.

## 4.3 Core implementation rationale and overhaul points

Sections 4.1 and 4.2 cover "what to do"; this section covers "**why do it this way, how it runs underneath, and where the real focus is when doing it**". Once the rationale is understood, the code in Chapter 5 isn't just copy-paste — and when problems arise, you'll know which link to fix.

### 4.3.1 The essence of "crawlers can't read it": the CSR vs SSR rendering difference

**Core rationale:**

A page's body has two production paths:

| Rendering | Where the body is generated | When crawlers can see it |
| --- | --- | --- |
| **CSR (client-side rendering)** | After downloading JS, the browser calls APIs and splices data into the DOM | Only after JS executes |
| **SSR (server-side rendering)** | The server splices data and templates into complete HTML | Visible in the response source |

BlogLoom is a typical CSR. The real flow is:

```plain
Crawler GET /blog/513
   ↓
Returns static/view/index.html (empty shell, <title>BlogLoom</title>, <div id="app"></div>)
   ↓
(Crawlers usually don't run JS, so it stops here → empty shell)
   ↓ if a browser
Downloads and runs JS → Vue mounts → axios calls GET /blog?id=513 → gets JSON → v-html renders → document.title changes the title
```

**Key point:** the problem isn't the data but **which layer the data appears in**. The `GET /blog?id=513` API returns complete body JSON (`BlogController.java:62-97` returns `Result(BlogDetail)`); it's just not spliced into the first-screen HTML.

**How to optimize:**

Switch "pages that need indexing" from CSR to SSR — the backend `@Controller` + Thymeleaf renders `BlogDetail` directly into the first-screen HTML. Then the crawler gets the complete body regardless of whether JS executes.

**Use-case comparison (same input, different output):**

+ **Input:** `GET /blog/513`, header `Accept: text/html`
+ **Before (CSR empty shell):**

```html
<head><title>BlogLoom</title></head>
<body><div id="app"></div></body>
```

+ **After (SSR direct rendering):**

```html
<head>
  <title>Java basics study notes 09, IO streams - BlogLoom</title>
  <meta name="description" content="Starting from the File class, systematically explains IO streams...">
</head>
<body>
  <h1>Java basics study notes 09, IO streams</h1>
  <div class="content">...full body...</div>
</body>
```

> Same input, one output with no body and one with a complete article on the first screen — that's the dividing line for indexing success.

### 4.3.2 Server-side rendering: request lifecycle and the SPA fallback conflict

**Core rationale:**

The same request may pass three gates in Spring Boot, and their order decides success:

```plain
HTTP request
   ↓ ① Servlet Filter (order = Integer.MIN_VALUE, runs first)
SpaForwardFilter: forwards to /index.html if the rule matches
   ↓ ② DispatcherServlet routing
@Controller mapping (SeoPageController)
   ↓ ③ View resolution
ThymeleafViewResolver → renders templates/seo/blog.html
```

**The key conflict (must understand):**

`SpaForwardConfig`'s filter **runs before the Controller** and forwards any "GET + Accept containing text/html + extension-less path" request to `index.html` (`SpaForwardConfig.java:71-88`). That is — **without changing it, `/blog/513` will never reach our new Controller**; `SeoPageController` is just decoration.

**Overhaul points (three):**

1. Add a "server-render allowlist" (`SSR_PREFIXES`); on a hit, `return false` and don't forward, handing the request back to the Controller.
2. The allowlist judgment must still keep the "HTML navigation only" constraint: only allow `Accept: text/html`, extension-less GETs, avoiding affecting `/blogs`, `/blog?id=` and other JSON APIs.
3. Paths with extensions (`sitemap.xml` / `rss.xml` / `robots.txt`) aren't forwarded anyway, so the Controller can hit them directly without being in the allowlist.

**Key point:** this is the "master switch" of the whole overhaul. If the order is wrong, no page rendering later will take effect.

**Use-case comparison (three request types, input → output):**

| Input request | Before | After |
| --- | --- | --- |
| `GET /blog/513` (`Accept: text/html`) | Forwarded to the `index.html` empty shell | The complete article HTML rendered by `SeoPageController` |
| `GET /blog?id=513` (`Accept: application/json`) | `Result(BlogDetail)` JSON | Unchanged, still JSON (APIs unaffected) |
| `GET /sitemap.xml` (with extension) | Not handled by the filter | Directly hits `SeoFileController` outputting XML |

> Key point: the allowlist only takes over "HTML navigation" requests; API JSON and site-level files are unaffected.

### 4.3.3 Meta data flow: from the database to `<head>`

**Core rationale:**

`title` / `description` / `canonical` / OG / JSON-LD are all written in the HTML `<head>`, and search engines **only parse the response's HTML source**, not executing JS (especially Baidu and Bing, with weak JS support). So meta-information must be generated on the server side.

The data flow is (model once, reuse site-wide):

```plain
blog table
  ├─ title / description / first_picture / create_time / update_time
  ├─ category / tags / user_id
  ↓ BlogMapper.xml (detail query, supplement columns + join author)
BlogDetail (VO, supplement firstPicture / description / author)
  ↓ SeoMetaBuilder (+ SeoUrlResolver building absolute URLs, webTitleSuffix)
SeoMeta (title / description / canonicalUrl / image / publishedTime / ...)
  ↓ Thymeleaf common fragment seo-head.html
HTML <head>
```

**The rationale of canonical:** the same article may have multiple URLs (`/blog/513`, `?utm_source=xx`, `?from=home`). `rel="canonical"` tells search engines "which is the preferred version among these addresses", thereby **merging the authority signals of duplicate content**. It's only a "signal"; it doesn't redirect the browser and doesn't guarantee adoption.

**Overhaul points:**

+ The detail API `BlogDetail` must carry `firstPicture`, `description`, `authorName`, `authorAvatar`. Although the SQL queries these columns, the `blogDetail` resultMap must still map them explicitly, otherwise you may get "the article list has a cover but the detail API's cover is null", causing `og:image` not to output.
+ The domain must not be hardcoded; `SeoUrlResolver` uniformly takes the backend SEO domain `seoDomain` (fallback `blog.view`) to build absolute URLs, ensuring canonical / `og:url` / sitemap `loc` are consistent.
+ The `title` suffix reuses the site config `webTitleSuffix`, staying consistent with the existing `document.title` logic.

**Use case (input one article's data → output head meta-information):**

Input (a row in the `blog` table):

```plain
id=513
title=Java basics study notes 09, IO streams
description=Starting from the File class, systematically explains IO streams...
first_picture=https://pictured-bed.oss-cn-beijing.aliyuncs.com/cover/513.png
create_time=2026-10-01 10:00:00
update_time=2026-10-02 12:00:00
category=Java
tags=[Java, IO streams]
author=Changlu
```

Output (rendered into `<head>`):

```html
<title>Java basics study notes 09, IO streams - BlogLoom</title>
<meta name="description" content="Starting from the File class, systematically explains IO streams...">
<link rel="canonical" href="https://blog.changlu.cloud/blog/513">
<meta property="og:image" content="https://pictured-bed.oss-cn-beijing.aliyuncs.com/cover/513.png">
<meta property="og:url" content="https://blog.changlu.cloud/blog/513">
```

**canonical use case:** visiting `/blog/513?utm_source=csdn&from=home`, the canonical in the output is still the clean `https://blog.changlu.cloud/blog/513`.

### 4.3.4 How structured data (JSON-LD) is generated

**Core rationale:**

Structured data = describing content with a shared vocabulary search engines understand (Schema.org). JSON-LD is the most recommended carrier, placed in `<script type="application/ld+json">`. After parsing, the search engine knows "this is a `BlogPosting`, who the author is, when published/modified, what it's about", understanding the content better and possibly showing rich results.

Generation flow:

```plain
BlogDetail
  ↓ SeoMetaBuilder assembles an ObjectNode with Jackson ObjectMapper
{ "@context": "...", "@type": "BlogPosting", "headline": ..., "author": ..., "datePublished": ... }
  ↓ ObjectMapper.writeValueAsString serialization (avoids manual escaping)
  ↓ injected into <script type="application/ld+json"> in seo-head.html
```

**Key points:**

+ Use `ObjectMapper` to serialize; **don't manually concatenate** JSON strings (quotes/Chinese/HTML escaping is very error-prone).
+ `dateModified` must take the real `update_time`; **don't refresh the time each load to fake an update**; information must also match visible content.
+ Choose `BlogPosting` for `@type` (`BlogPosting` is a subtype of `Article` in schema.org).

**Use case (input `BlogDetail` → output JSON-LD):**

```html
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "BlogPosting",
  "headline": "Java basics study notes 09, IO streams",
  "description": "Starting from the File class, systematically explains IO streams...",
  "image": "https://pictured-bed.oss-cn-beijing.aliyuncs.com/cover/513.png",
  "datePublished": "2026-10-01T10:00:00+08:00",
  "dateModified": "2026-10-02T12:00:00+08:00",
  "inLanguage": "zh-CN",
  "keywords": ["Java", "IO streams"],
  "author": { "@type": "Person", "name": "Changlu" },
  "mainEntityOfPage": "https://blog.changlu.cloud/blog/513"
}
</script>
```

> Validation: paste this into Google's Rich Results Test / a Schema validator; it should have no errors.

### 4.3.5 Protocol rationale of site-level files (robots / sitemap / rss)

**Core rationale and flow:**

| File | Based on | Role | Generation/update timing |
| --- | --- | --- | --- |
| `robots.txt` | RFC 9309 | Tells crawlers which paths are crawlable | Site level, basically fixed |
| `sitemap.xml` | Sitemaps.org protocol | Lists public pages, aiding discovery/crawling | Sync after article publish/modify/delete |
| `rss.xml` | RSS 2.0 | For readers/aggregators to subscribe | Update on new articles (output the latest ~20) |

Key mechanisms on BlogLoom's side:

+ All three are **extension-bearing paths**, which `SpaForwardFilter` doesn't forward, so `SeoFileController` can produce them directly (`produces` set to `text/plain` / `application/xml` / `application/rss+xml`).
+ `sitemap.xml` must be **dynamically generated**: query the `blog` table for `is_published=1 and is_deleted=0 and password empty`, using the same URL rule as canonical.
+ `rss.xml` is auto-discovered via the article page's `<link rel="alternate" type="application/rss+xml">` (write it into the common fragment).

**Overhaul points:**

+ **Do not** `Disallow: /blog` in robots — BlogLoom's APIs and content pages are both at the root, which would mistakenly block articles.
+ Private/draft/password articles **must not** enter sitemap or rss, but this is only "not proactively exposing"; real protection still relies on authentication.
+ `lastmod` uses the real `update_time`; the sitemap `loc` is fully consistent with canonical.

**Use case (each of the three endpoints, input → output):**

+ **`GET /sitemap.xml`** (input: none):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    <loc>https://blog.changlu.cloud/blog/513</loc>
    <lastmod>2026-10-02</lastmod>
  </url>
  <!-- ...other public articles; drafts/private/password articles don't appear... -->
</urlset>
```

+ **`GET /robots.txt`** (input: none):

```plain
User-agent: *
Disallow: /admin/
Disallow: /cms/
Disallow: /login
Allow: /

Sitemap: https://blog.changlu.cloud/sitemap.xml
```

+ **`GET /rss.xml`** (input: none, taking the latest ~20 public articles):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
  <channel>
    <title>Changlu's Blog</title>
    <link>https://blog.changlu.cloud</link>
    <item>
      <title>Java basics study notes 09, IO streams</title>
      <link>https://blog.changlu.cloud/blog/513</link>
      <guid>https://blog.changlu.cloud/blog/513</guid>
      <pubDate>Thu, 01 Oct 2026 10:00:00 +0800</pubDate>
      <description>Starting from the File class, systematically explains IO streams...</description>
    </item>
  </channel>
</rss>
```

**Sitemap size and limits (no concern for BlogLoom's several hundred articles):**

The Sitemaps protocol has two hard limits on **a single sitemap file**:

| Limit | Cap | Not exceeded | When exceeded |
| --- | --- | --- | --- |
| URL count | 50,000 | One `sitemap.xml` lists everything | Split into multiple child sitemaps, summarized with a `sitemapindex` |
| File size | 50MB uncompressed | Same | Same |

BlogLoom's current scale (five to six hundred articles):

+ **Count**: about 500–600 URLs, far below the 50k cap.
+ **Size**: each `<url>` is about 100 bytes; the whole file is tens of KB, far below 50MB.
+ **Conclusion**: a single `sitemap.xml` with full output suffices, **no pagination and no `sitemapindex` needed**.

Current scope (consistent with 5.1.3):

+ **Included**: the two static pages `/home`, `/archives` + all public articles (`is_published=1 and is_deleted=0 and password empty`).
+ **Excluded**: drafts, recycle bin, password-protected articles.
+ **Not yet added (optional enhancement)**: aggregation pages like `/tag/{name}`, `/category/{name}`, `/column/{id}`.

**Optimization suggestion (optional):** the sitemap actually only uses `id` and `update_time`, so a lighter dedicated query could be used (currently `SeoArticleMapper` also `select`s `title/description/first_picture`); negligible at a few hundred articles, consider trimming columns or adding caching at tens of thousands.

### 4.3.6 Crawling and indexing: indexing-control rationale and the search engine flow

**First distinguish two concepts (most important):**

| Directive | Role | Where | Prerequisite |
| --- | --- | --- | --- |
| `Disallow` | Blocks **crawling** | `robots.txt` | — |
| `noindex` | Blocks **indexing** | The page's `<meta name="robots">` or the `X-Robots-Tag` response header | **Crawling must be allowed first** for the engine to read it |

**Rationale:** whether a search engine can read `noindex` depends on **whether it can crawl** the page. So for a page that is crawlable but shouldn't be indexed (like the `/cms/` backend), the correct approach is "allow crawling + output noindex on the page"; doing both Disallow and expecting it to read noindex is logically contradictory.

**The full search-engine indexing flow (Google-based):**

```plain
① Discovery: on-site links / external links / Sitemap / manual URL submission
   ↓
② Crawling: Googlebot fetches HTML per crawl budget
   ↓
③ Rendering: Googlebot renders JS with Chromium (queued, not real-time)
   ↓
④ Indexing: content quality, duplication, canonical, noindex judged together
   ↓
⑤ Display/ranking: sorted and shown when a user searches
```

**Key point:** Google does render JS, but **rendering is delayed and budgeted**; Baidu and Bing support JS rendering more weakly. So relying on CSR "waiting for Googlebot to render" is unreliable — **server-side rendering lets the flow get the body at step ②**, making indexing faster and more stable.

**How to promote and monitor indexing (by search engine):**

The official entries and submission methods of the three mainstream engines:

| Search engine | Official platform | Submission / push | Key notes |
| --- | --- | --- | --- |
| **Google** | [Search Console](https://search.google.com/search-console/) | ① Submit `sitemap.xml`; ② "URL Inspection → Request Indexing" | **No open active-push API**, and it doesn't use IndexNow; rely on Sitemap + internal links + manual requests |
| **Baidu** | [Baidu Search Resource Platform](https://ziyuan.baidu.com/) | ① Manual submit; ② Active-push API; ③ `push.js` auto push; ④ Submit Sitemap | The main domestic indexing battlefield; `push.js` auto-pushes when visitors browse |
| **Bing** | [Bing Webmaster Tools](https://www.bing.com/webmasters/) | ① Submit `sitemap.xml`; ② **IndexNow** active push; ③ Manual URL submit | IndexNow is shared by Bing / Yandex and others with one submission |

**① Google:**
+ Verify via DNS TXT in Search Console → submit `sitemap.xml` → request indexing for new articles via "URL Inspection".
+ **Note:** Google has retired the sitemap ping endpoint and **doesn't support IndexNow**, so don't count on a "push API"; Sitemap + on-site links + Search Console manual requests are the right way.

**② Baidu:**
+ After verifying the site in the Baidu Search Resource Platform, three submission methods are available:
  + **Manual submit**: paste URLs one by one in the backend (good for small numbers);
  + **Active-push API**: the server POSTs to the push endpoint when publishing (fastest, most real-time);
  + **Auto push `push.js`**: auto-push when the page is visited (easiest).

```html
<!-- Baidu auto push: put into the common head fragment or blog-view-ui/index.html -->
<script src="https://zz.bdstatic.com/linksubmit/push.js"></script>
```

**③ Bing:**
+ Verify in Bing Webmaster Tools → submit `sitemap.xml`.
+ Integrate **IndexNow**: place a `<your-key>.txt` (content is the key) at the site root, and POST to the IndexNow endpoint when publishing:

```json
POST https://api.indexnow.org/indexnow
{
  "host": "blog.changlu.cloud",
  "key": "<your-key>",
  "keyLocation": "https://blog.changlu.cloud/<your-key>.txt"
}
```

(For the full IndexNow payload and verification details, see the original research; the essence is that a single submission is shared by multiple engines.)

### 4.3.7 Data monitoring rationale

**Core rationale:** monitoring answers three questions with three kinds of tools:

| Question | Tool | Data |
| --- | --- | --- |
| Is it indexed? Which keywords? | Google Search Console | Index status, crawl issues, keywords, impressions, clicks, average position |
| Who came? Which articles? From where? | GA4 / Umami | Users, page views, sources, popular pages |
| How much did ads earn? | Google AdSense | Estimated earnings, balance, payments, impressions, clicks |

For BlogLoom, the Search Console verification and the GA4/Umami script are injected into the public head fragment, controlled by backend config (F15), so no code change is needed to switch them on or off.

### 4.3.8 Backend SEO config (analytics script / event reporting) rationale

**Core rationale:** the SEO domain, GA4 script and event-reporting switch are stored as site settings (same `site_setting` table, a new group), editable in the backend "site settings" page. The backend reads them when rendering the head fragment (`seo-head.html`), so the same configuration drives all pages. This keeps SEO knobs out of code and in the hands of the site owner.

### 4.3.9 Reading page performance optimization rationale

**Core rationale:** the body is now in the first-screen HTML (SSR), so LCP improves automatically; remaining work is lazy-loading body images and deferring third-party scripts (analytics, ads) so they don't block reading. Reserved dimensions for images and ads keep CLS low.

## 4.4 Ad integration requirements and rationale (Google AdSense)

### 4.4.1 Why Google AdSense first

AdSense is the most mature and easiest-to-integrate ad network for a self-hosted blog: application is straightforward, it matches content automatically, and it imposes relatively light integration requirements (one publisher ID + ad slots). For phase one, it's the sensible first test target.

### 4.4.2 How AdSense works and key concepts

| Concept | Meaning |
| --- | --- |
| Publisher ID | `pub-XXXXXXXXXXXXXXXX`, your account identifier in AdSense |
| Ad unit | A single ad slot with its own `slot` id |
| Auto ads | AdSense decides placements automatically; needs only the loader script |
| Manual ads | You place `<ins class="adsbygoogle">` at chosen positions |
| `ads.txt` | A file declaring authorized sellers, for brand-safety and fraud prevention |
| Page RPM | Revenue per thousand page views |

The loader script (`adsbygoogle.js`) plus the publisher ID is injected into the page head; each manual ad unit is an `<ins>` tag configured with its `data-ad-client` and `data-ad-slot`.

### 4.4.3 One publisher ID, N ad units

A single publisher ID can drive many ad units (top, in-article, bottom, below the right-hand table of contents, etc.), each with a distinct slot. The backend stores the publisher ID once and each position's slot, and injects them per position.

### 4.4.4 Ad placement planning

| Position | Rationale |
| --- | --- |
| Top | High visibility but must not push the body down; reserve height |
| In-article | Good relevance to reading context |
| Bottom | Low interference, good for post-read impressions |
| Below the right TOC | On wide screens, fills otherwise-empty sidebar space |

### 4.4.5 Ad position ↔ page mapping (requirement view)

| Page | Ads served? |
| --- | --- |
| Article detail | Yes (all positions) |
| Home / list | Yes (top/bottom) |
| Backend `/cms`, `/admin` | **No** — the backend isn't public and needs no ads |

### 4.4.6 Compliance and review points

+ Content must be original, policy-compliant and have a clear privacy policy.
+ No self-clicking or asking friends to click (invalid traffic).
+ `ads.txt` should declare Google as an authorized seller.
+ Don't place ads to obscure content or mislead clicks.

### 4.4.7 Revenue and metrics: what to watch and how to compute

```plain
Monthly ad revenue ≈ monthly page views ÷ 1000 × measured Page RPM
```

Page PV ≠ ad impressions; watch site traffic (GA4/Umami), ad impressions/estimated earnings (AdSense) and final settlement separately.

### 4.4.8 Troubleshooting (ads not showing)

| Symptom | Checks |
| --- | --- |
| No ads at all | Publisher ID empty? Loader script injected? |
| Some positions empty | Slot id wrong? Ad blocker on? |
| Review rejected | Content/policy issues; check AdSense messages |

### 4.4.9 Reference index

See the AdSense help center and the `ads.txt` specification.

---

# 5. Practical code: item-by-item overhaul

## 5.1 Functional code optimization

### 5.1.1 Content rendering and data foundation

1. **Complete `BlogDetail`**: add `firstPicture`, `description`, `authorName`, `authorAvatar`, and map them explicitly in the `blogDetail` resultMap.
2. **Add `SeoPageController`**: a `@Controller` mapping `GET /blog/{id}` that queries the article, injects it into the model and returns the `seo/blog` view.
3. **Modify `SpaForwardConfig`**: add `SSR_PREFIXES` and return `false` on a hit so `/blog/{id}` reaches the Controller.

### 5.1.2 Page-level SEO meta-information

Create `templates/seo/fragments/seo-head.html` outputting `title`, `description`, `canonical`, `og:*`, `article:*` and the `BlogPosting` JSON-LD, all from `SeoMeta` populated by `SeoMetaBuilder`, with URLs built by `SeoUrlResolver`.

### 5.1.3 Site-level indexing files

Implement `SeoFileController` with `robots()`, `sitemap()` and `rss()` methods (`produces` set appropriately), backed by `SitemapService` and `RssService`, querying only public published non-password articles.

### 5.1.4 Crawlable aggregation pages

`SeoPageController` handles `/home`, `/tag/{name}`, `/category/{name}`, `/archive` etc., reusing the common `seo/list.html` template so crawlers can read the lists.

### 5.1.5 Indexing control and backend config

Add `noindex` to `blog-cms-ui/index.html`, and set `X-Robots-Tag: noindex, nofollow` for `/cms/**` and `/admin/**` via a `WebConfig` interceptor/filter. Add the backend SEO config page and the `seoDomain` setting.

### 5.1.6 Reading page performance optimization

Lazy-load body images and load third-party scripts (analytics, ads) asynchronously; reserve dimensions for images and ads.

## 5.2 Search engine platform indexing

### 5.2.1 Data monitoring integration

Inject the Search Console verification meta and the GA4/Umami script into the common fragment, controlled by backend config.

### 5.2.2 Google indexing (overseas)

Verify site ownership via DNS TXT in Search Console, submit `sitemap.xml`, and request indexing for new URLs.

### 5.2.3 Baidu indexing (domestic)

Verify in the Baidu Search Resource Platform, submit the Sitemap, and optionally add the active-push API or `push.js`.

### 5.2.4 Bing indexing (overseas)

Verify in Bing Webmaster Tools, submit `sitemap.xml`, and optionally integrate IndexNow.

### 5.2.5 Reading indexing status and common issues

Common states: "Discovered – currently not indexed", "Crawled – currently not indexed", "Excluded by noindex", etc. Understand each before worrying.

### 5.2.6 Indexing monitoring cadence

Aim for a weekly review: new articles' index status, coverage errors, and keyword impressions.

## 5.3 Ad integration (Google AdSense)

### 5.3.1 Core implementation

Store the publisher ID and each position's slot in backend config; inject the loader script in the head and the ad-unit `<ins>` tags per position; generate `/ads.txt` from a configurable content field.

```
# Article page loader + ad units
# ads.txt
# Expected: google.com, pub-9876543210987654, DIRECT, f08c47fec0942fa0
# When unconfigured, nothing is injected (clear the publisher ID in the backend first)
```

### 5.3.2 Core application steps

Apply at AdSense → add the site → place the verification/loader code → wait for review → create ad units → fill the publisher ID and slots in the BlogLoom backend.

---

# 6. Verification testing

## 6.1 Functional acceptance tests

### 6.1.1 Functional verification (browser / backend)

Visit `/blog/{id}` with JS disabled and confirm the body and meta are present; open the backend SEO pages and confirm the config saves.

### 6.1.2 curl verification

```bash
curl -s https://blog.changlu.cloud/robots.txt
curl -s https://blog.changlu.cloud/sitemap.xml
curl -s https://blog.changlu.cloud/rss.xml
curl -s https://blog.changlu.cloud/blog/513 | grep -E "<title>|canonical|og:"
```

### 6.1.3 Fine-grained checks (unit tests)

Unit-test `SeoMetaBuilder`, `SeoUrlResolver`, `SitemapService` and `RssService`.

## 6.2 Phase-one launch checklist

- [ ] `/blog/{id}` returns SSR HTML with body + meta
- [ ] Each page has a unique title and a non-empty description
- [ ] canonical points to a parameter-free standard URL
- [ ] JSON-LD validates with no errors on the rich results test
- [ ] Share cards show title + summary + cover
- [ ] `/robots.txt`, `/sitemap.xml`, `/rss.xml` all return 200
- [ ] The sitemap contains only public published non-password articles
- [ ] `/cms/` and `/admin/` output noindex
- [ ] Search Console and GA4 verified and reporting
- [ ] AdSense config injects on the correct positions (when configured)

## 6.3 Implementation milestones

| Milestone | Content |
| --- | --- |
| M1 | Capture the gap and define scope (this article, Chapters 1–4) |
| M2 | Content rendering + page-level meta (F1–F6) |
| M3 | Site-level files + aggregation pages (F7–F10) |
| M4 | Indexing control + monitoring + backend config (F11, F12, F15) |
| M5 | AdSense integration (5.3) |
| M6 | Performance and active push (F13, F14) |

## 6.4 Risks and cautions

+ Don't split content into two versions for readers and crawlers (cloaking).
+ Don't put private/password pages into sitemap/rss.
+ Keep canonical/og:url/sitemap loc consistent.
+ Don't fake `dateModified`.

---

# 7. Summary

BlogLoom's phase-one SEO overhaul converges to: **server-side render the pages that need indexing with backend Thymeleaf, keep meta-information model-once-reuse-site-wide, provide robots/sitemap/rss as site-level files, control indexing for the backend, and add monitoring and optional AdSense.** The technical prerequisite is the `SpaForwardFilter` allowlist — without it, nothing else takes effect. After this phase, crawlers can read the content and the site is ready for indexing and data review.

# References

[1]. [Sitemaps XML protocol](https://www.sitemaps.org/protocol.html)

[2]. [RFC 9309: Robots Exclusion Protocol](https://www.rfc-editor.org/rfc/rfc9309.html)

[3]. [RFC 6596: The Canonical Link Relation](https://www.rfc-editor.org/rfc/rfc6596.html)

[4]. [Schema.org](https://schema.org/) / [JSON-LD 1.1](https://www.w3.org/TR/json-ld11/)

[5]. [RSS 2.0 Specification](https://www.rssboard.org/rss-specification)

[6]. [Web Vitals](https://web.dev/articles/vitals)

[7]. [Google Search Console](https://search.google.com/search-console/)

[8]. [Google AdSense](https://adsense.google.com/start/)

---

## Next steps

- [BlogLoom SEO 01: Article page optimization, monetization and data monitoring](/v1/en/blogs/seo/01-article-page-monetization)
- [BlogLoom SEO 03: Google AdSense integration guide](/v1/en/blogs/seo/03-adsense-guide)
- [SEO, subscription and site monetization configuration](/v1/en/blogs/features/seo-and-monetization)
