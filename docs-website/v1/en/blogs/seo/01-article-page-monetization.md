---
title: "BlogLoom SEO 01: Article Page Optimization, Monetization and Data Monitoring"
description: "From article-page SEO and site monetization to data monitoring: why server-side rendering matters, how to prepare, and how to evaluate results."
---

# 1. Understanding search engine optimization

## 1.1 What SEO is

> SEO: making it easier for search engines to discover and understand your articles.

SEO stands for Search Engine Optimization.

Say you write an article, "How to connect Spring Boot to an LLM API?", and you want **people searching for "Spring Boot LLM API" to find your blog**. The content and technical optimization around that **goal** is SEO.

**First, understand roughly how a search engine works:**

| Stage | What the search engine does | What you need to solve |
| --- | --- | --- |
| Discovery | Finds your article's URL | The site has navigation, article links and a Sitemap |
| Crawling | Visits the URL and reads the page | The page is reachable and the body is readable |
| Indexing | Analyzes the page and decides whether to include it in the search database | Clear, valuable content; fewer duplicate pages |
| Ranking & display | Picks relevant pages to show when a user searches | Content matches the search intent; the site offers a good experience |

**Submitting a URL does not guarantee indexing, and indexing does not guarantee a high ranking.**

**For a content-generation platform, SEO can be understood from two angles:**

| Direction | Examples |
| --- | --- |
| Content optimization | The title states the problem clearly; the body has steps, code and verified results; articles link to related ones |
| Technical optimization | Each article has its own URL and title; search engines can read the body; pages load fast; mobile reading works |

**For example, an article's HTML can contain:**

```html
<title>How to connect Spring Boot to an LLM API - Changlu's Blog</title>

<meta
  name="description"
  content="A complete walkthrough of connecting Spring Boot to an LLM API, including configuration, call code and common issues."
/>
```

Here:

+ `title` tells the search engine and the reader **what the page is about**.
+ `description` is the page summary; the search engine may use it for the snippet or pick its own from the body.

Note that **SEO is not about stuffing keywords into the page**. For a technical blog, explaining a real problem clearly is the most important foundation.

## 1.2 Key technical terms and the problems they solve

### Overview of the structure

| Term | Problem solved | Is it a formal standard | Standard body | Official entry | What a content platform should follow |
| --- | --- | --- | --- | --- | --- |
| **Sitemap** | **Page discovery**: tells search engines which public pages exist and which content has been updated | Industry common protocol | The Sitemap protocol published by Sitemaps.org | [Sitemaps XML protocol](https://www.sitemaps.org/protocol.html) | Generate XML per spec, covering public pages you want indexed; update on publish/change/removal; respect encoding, URL and file-size limits |
| **robots.txt** | **Crawl management**: tells compliant crawlers which paths may or may not be crawled | IETF standards track | IETF, **RFC 9309: Robots Exclusion Protocol** | [RFC 9309](https://www.rfc-editor.org/rfc/rfc9309.html) | Provide `/robots.txt` at the site root, configuring crawler and path rules correctly; avoid blocking the body and essential resources; don't treat it as access control or an indexing ban |
| **canonical** | **URL consolidation**: declares the preferred version when the same content has multiple URLs, helping merge duplicate signals | IETF standards track | IETF, **RFC 6596: The Canonical Link Relation** | [RFC 6596](https://www.rfc-editor.org/rfc/rfc6596.html) | Declare the correct preferred URL for the page; keep internal links, the Sitemap and canonical consistent; don't point pages with different content to the home page |
| **Structured data** | **Content understanding**: makes content type, title, author and publish time explicit, supporting search-engine understanding and rich display | A combination of specs: vocabularies, expression formats and search-platform rules | **Schema.org** defines types and properties; **W3C** defines formats such as JSON-LD; search engines define their support rules | [Schema.org](https://schema.org/) / [JSON-LD 1.1](https://www.w3.org/TR/json-ld11/) | Choose types that match the actual content, such as `Article`, `BlogPosting`; output in supported formats such as JSON-LD; fields must be truthful and match visible content; validate per target search-engine rules |
| **RSS** | **Subscription updates**: lets readers keep getting new content via a reader; can also help some search engines discover recent URLs | An open content-subscription format spec, usually RSS 2.0 | **RSS Advisory Board** maintains the RSS 2.0 spec | [RSS 2.0 Specification](https://www.rssboard.org/rss-specification) | Provide the feed in `rss`, `channel`, `item` structure; include title, link, date and summary or full text; maintain it on updates and never output private content |
| **Reading page performance** | **Better reading experience**: make the main content appear quickly, interactions respond promptly and layout stay stable during loading | Not a single protocol; Core Web Vitals is a common metric system | **Google** introduced Web Vitals / Core Web Vitals | [Web Vitals](https://web.dev/articles/vitals) | Optimize body rendering, images, scripts, server response and caching; reserve space for images and ads; continuously measure **LCP, INP, CLS** and improve based on real user experience |

**Note:** following the specs helps crawling, understanding and using content, but does not guarantee indexing or ranking.

```plain
SEO
├── Sitemap
├── robots.txt
├── canonical
└── Structured Data

Content subscription
├── RSS
└── Atom

Content propagation
└── Open Graph

User experience
└── Reading page performance
```

### Part 1: SEO optimization

#### 1) Sitemap

> Sitemap: organizing site URLs into a machine-readable directory.

Sitemap usually refers to an XML file, for example:

```plain
https://your-domain/sitemap.xml
```

Its content is roughly as follows (domains are only examples):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    <loc>https://example.com/</loc>
  </url>
  <url>
    <loc>https://example.com/posts/spring-boot-ai</loc>
    <lastmod>2026-10-02</lastmod>
  </url>
</urlset>
```

Knowing two fields is enough:

| Field | Meaning |
| --- | --- |
| `loc` | The page's full URL |
| `lastmod` | The date the page was last substantively modified (optional) |

It's essentially telling search engines proactively:

"These are my public pages; feel free to take a look."

Its role is **to assist page discovery and crawling**, not a shortcut to higher rankings, and it can't guarantee indexing.

**"Configuring a Sitemap" usually involves three things:**

1. **Generate the directory**: extract public URLs from published articles and generate XML.
2. **Keep it updated**: sync the directory when articles are published, modified or deleted.
3. **Tell search engines**: declare it in `robots.txt`, or submit it via a search engine's webmaster tools.

For example, put one line in the site's `robots.txt`:

```plain
Sitemap: https://example.com/sitemap.xml
```

For BlogLoom, it's best to generate this automatically. **The backend, login pages, drafts and private articles should not be in the Sitemap.** But note: not putting something in the Sitemap does not make it secret — private pages still need access control.

#### 2) robots.txt

> Simply put: tell crawlers which paths they may crawl.
>
> Main problem solved: which paths are allowed or disallowed for crawling?

**It's a text file at the site root:**

```plain
https://example.com/robots.txt
```

**For example:**

```plain
User-agent: *
Disallow: /admin/
Disallow: /api/
Disallow: /preview/

Sitemap: https://example.com/sitemap.xml
```

**Meaning:**

| Config | Meaning |
| --- | --- |
| `User-agent: *` | The rules apply to all compliant crawlers |
| `Disallow: /admin/` | Do not crawl the backend path |
| `Disallow: /api/` | Do not crawl the API path |
| `Sitemap: ...` | Tells the Sitemap address |

**It's a crawl rule, not access control.** Others can still visit these URLs directly, so the backend and private content still need authentication.

Also, **disallowing crawling is not the same as disallowing indexing**. If a public page is accessible but you don't want it in search results, you usually use:

```html
<meta name="robots" content="noindex">
```

The search engine must be able to crawl the page to read `noindex`. So don't both disallow crawling and expect it to read the no-index directive.

#### 3) canonical (an optimization strategy)

> Simply put: tell search engines "which URL is authoritative for this page".

The same article may have multiple addresses:

```plain
https://example.com/posts/123
https://example.com/posts/123?utm_source=csdn
https://example.com/posts/123?from=homepage
```

**Since the body is identical, you can declare a single preferred URL:**

```html
<link rel="canonical"
      href="https://example.com/posts/123">
```

**Meaning: the preferred URL** for this content is `/posts/123`.

**In a content platform**: output each article's canonical full URL, and keep internal article links, the Sitemap and canonical as consistent as possible.

**Note two things:**

+ canonical **does not redirect the browser**; a browser redirect needs a redirect.
+ Don't point all articles at the home page, and don't point paginated pages with different content at the same page casually.

It's a signal providing the preferred URL; the final choice of URL is still up to the search engine.

#### 4) Structured data

> A brief introduction: expressing article information in a fixed format.

Readers see the title, avatar and date and can tell who wrote the article. Structured data expresses this information in a machine-friendly format.

A common form is **JSON-LD** embedded in HTML:

```html
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "BlogPosting",
  "headline": "How to connect Spring Boot to an LLM API",
  "author": {
    "@type": "Person",
    "name": "Changlu"
  },
  "datePublished": "2026-10-01T10:00:00+08:00",
  "dateModified": "2026-10-02T12:00:00+08:00",
  "description": "Introduces the connection steps, call code and common issues.",
  "url": "https://example.com/posts/123"
}
</script>
```

**Main fields:**

| Field | Meaning |
| --- | --- |
| `@type` | Content type; here, a blog post |
| `headline` | Article title |
| `author` | Author |
| `datePublished` | First publish time |
| `dateModified` | Last substantive modification time |
| `description` | Content summary |

**In a content platform**: read these fields from the article database and generate them uniformly during Thymeleaf rendering.

It helps search engines understand content and may support richer search display, but **cannot guarantee special display or a ranking boost**. The information must be truthful and consistent with the page; you must not auto-update the date every day to fake an update.

### Part 2: Content subscription formats

#### RSS (reader subscription)

> RSS: lets readers subscribe to your blog updates proactively.

RSS is a **content subscription format**.

Ordinary readers need to open the site periodically to check for updates. With RSS, readers can add multiple blogs to one RSS reader and see new articles in one place.

For example: a reader subscribes to Changlu's blog → you publish a new article → the reader periodically checks the feed → the reader sees the update.

It's usually an XML file too, commonly at:

```plain
https://your-domain/rss.xml
```

The file usually contains:

| Information | Example |
| --- | --- |
| Blog name | Changlu's Blog |
| Article title | How to connect Spring Boot to an LLM API |
| Original link | The article's full URL |
| Publish time | October 2, 2026 |
| Content | Article summary or full text |

**RSS itself is usually not email notification or instant push. It's subscription data provided by the site; the reader checks and displays updates.**

You can declare the subscription address in HTML:

```html
<link rel="alternate"
  type="application/rss+xml"
  title="Changlu's Blog"
  href="https://example.com/rss.xml">
```

**RSS mainly serves reader subscriptions, not a required SEO ranking config.** It differs from Sitemap in that **Sitemap focuses on listing URLs; RSS focuses on providing recent content updates**.

#### Atom Feed subscription

**Atom Feed: another standardized content subscription format.**

Atom and RSS serve very similar purposes, both letting blogs and news sites provide the latest content to RSS/Feed readers in a machine-readable way.

Common addresses, for example:

```plain
https://example.com/atom.xml
https://example.com/feed.xml
```

Users can add this address to readers like Feedly, Inoreader and NetNewsWire. After the site publishes a new article, the reader periodically checks the Atom Feed to discover new content.

An Atom file looks roughly like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom">

  <title>Changlu's Blog</title>
  <link href="https://example.com/"/>
  <link href="https://example.com/atom.xml" rel="self"/>

  <updated>2026-10-02T12:00:00+08:00</updated>

  <entry>
    <title>How to connect Spring Boot to an LLM API</title>

    <link href="https://example.com/posts/123"/>

    <id>https://example.com/posts/123</id>

    <published>2026-10-01T10:00:00+08:00</published>
    <updated>2026-10-02T12:00:00+08:00</updated>

    <summary>
      Introduces how to connect Spring Boot to an LLM API.
    </summary>
  </entry>

</feed>
```

Main fields:

| Field | Meaning |
| --- | --- |
| `title` | Blog or article title |
| `link` | Site or article address |
| `id` | Unique content identifier |
| `published` | First publish time |
| `updated` | Last updated time |
| `summary` | Article summary |
| `content` | Article body (optional) |

You can declare an Atom Feed in the page HTML:

```html
<link
  rel="alternate"
  type="application/atom+xml"
  title="Changlu's Blog"
  href="https://example.com/atom.xml">
```

For a **content distribution platform**, you can update the Atom Feed automatically when publishing, usually outputting only the most recent few dozen public articles rather than the entire blog history.

**What's the difference between RSS and Atom?**

Simply put:

| Compare | RSS | Atom |
| --- | --- | --- |
| Purpose | Content subscription | Content subscription |
| Format | XML | XML |
| Common address | `/rss.xml` | `/atom.xml` |
| Publish time | `pubDate` etc. | `published` |
| Modified time | Handled differently per RSS version | Natively provides `updated` |
| Spec | Many historical versions | More unified and standardized |
| Reader support | Very wide | Very wide |

For a blog system, **RSS and Atom are not both mandatory**. If a content distribution platform already supports:

```plain
/rss.xml
```

that already satisfies most subscription scenarios.

If you want the blog platform to be more complete, you can provide both:

```plain
/rss.xml
/atom.xml
```

Atom and RSS also differ from Sitemap in positioning:

```plain
Sitemap
    ↓
Mainly serves search engines
    ↓
Tells search engines: which URLs the site has

RSS / Atom
    ↓
Mainly serves subscribing readers and content aggregators
    ↓
Tells readers: what content was recently published
```

Neither is itself **a feature that directly improves SEO ranking**, but they help with **content discovery, subscription and distribution**.

### Part 3: Content propagation

#### Open Graph tags

**Open Graph: controls what is displayed when a web page is shared to social platforms.**

When a blog article is shared to WeChat, Facebook, LinkedIn, Discord, Slack or another platform supporting link cards, the platform usually tries to read the page's title, description and image.

If nothing is configured, the platform may guess from the HTML:

```plain
What's the title?
What's the summary?
Which image should be shown?
```

This may result in:

```plain
Wrong image
Truncated title
Unsuitable summary
No cover image
```

Open Graph uses a set of `<meta>` tags to explicitly tell the platform:

When this article is shared to other platforms, please use these titles, summaries, images and links.

For example:

```html
<meta
  property="og:title"
  content="How to connect Spring Boot to an LLM API">

<meta
  property="og:description"
  content="A complete walkthrough of connecting Spring Boot to an LLM API.">

<meta
  property="og:image"
  content="https://example.com/images/spring-boot-ai-cover.png">

<meta
  property="og:url"
  content="https://example.com/posts/123">

<meta
  property="og:type"
  content="article">

<meta
  property="og:site_name"
  content="Changlu's Blog">
```

Main fields:

| Tag | Meaning |
| --- | --- |
| `og:title` | Share card title |
| `og:description` | Share card summary |
| `og:image` | Cover image shown when shared |
| `og:url` | Page canonical URL |
| `og:type` | Content type |
| `og:site_name` | Site name |

For the **BlogLoom** case, this information can mostly come straight from the article database:

```plain
Article title
    ↓
og:title

Article summary
    ↓
og:description

Article first image / cover
    ↓
og:image

Article canonical URL
    ↓
og:url

Site name
    ↓
og:site_name
```

So it can be output uniformly in the Thymeleaf article template:

```html
<meta property="og:title"
      th:content="${article.title}">

<meta property="og:description"
      th:content="${article.description}">

<meta property="og:image"
      th:content="${article.cover}">

<meta property="og:url"
      th:content="${article.url}">

<meta property="og:type"
      content="article">

<meta property="og:site_name"
      content="BlogLoom">
```

For the article type, you can add further:

```html
<meta
  property="article:published_time"
  content="2026-10-01T10:00:00+08:00">

<meta
  property="article:modified_time"
  content="2026-10-02T12:00:00+08:00">

<meta
  property="article:author"
  content="Changlu">
```

A few points to note:

**First, `og:image` is best as a full URL.**

Recommended:

```plain
https://example.com/images/article-cover.png
```

Rather than:

```plain
/images/article-cover.png
```

Because it's third-party platforms that fetch the share card.

**Second, Open Graph images should be publicly accessible.**

If the image requires login or carries a cookie, third-party platforms usually can't fetch it.

**Third, Open Graph and SEO are not the same thing.**

Open Graph mainly solves:

```plain
What a link looks like after being shared to an external platform
```

Whereas ordinary SEO tags:

```plain
<title>...</title>

<meta name="description" content="...">
```

mainly serve search results and the page itself.

Usually the two can share the same data:

```plain
article.title
├── <title>
└── og:title

article.description
├── meta description
└── og:description

article.cover
└── og:image
```

So for a content platform, it's recommended to understand it as:

```plain
SEO
├── Sitemap
├── robots.txt
├── canonical
└── Structured Data

Content subscription
├── RSS
└── Atom

Content propagation
└── Open Graph

User experience
└── Reading page performance
```

This way, the **8 capabilities above already form a fairly complete blog content infrastructure**: search engines handle discovery and understanding, feeds handle subscription, Open Graph handles external propagation, and performance optimization handles the actual reading experience.

### Part 4: User experience

#### Reading page performance

> Introduction: make articles open fast, interact smoothly and lay out stably.

It's not just server response speed; it also includes **the experience when readers actually see and operate the page**.

| Experience | Common problem | How BlogLoom could improve |
| --- | --- | --- |
| Main content appears fast | The body only appears after JavaScript and APIs load | Include the body directly in the first HTML |
| Prompt click response | Too many scripts make button clicks laggy | Reduce heavy scripts; load features on demand |
| Page doesn't jump | Images and ads load and push the body down | Reserve dimensions for images and ads |
| Reasonable image loading | The cover is oversized and article images all download at once | Compress images; lazy-load images below the body |
| Body not dragged down by extras | Comments, analytics and ads block reading | Load these features asynchronously |

The commonly heard **Core Web Vitals** mainly focus on:

| Metric | How to understand it |
| --- | --- |
| **LCP** | How soon the main content is displayed |
| **INP** | Whether the page responds promptly after user actions |
| **CLS** | Whether content shifts unexpectedly during loading |

## 1.3 How can a site run commercial ads?

The commonly supported types include: **cloud service referral commissions, direct brand sponsorship, and Google AdSense**.

+ With **Google AdSense**, Google AdSense matches the ads; it doesn't necessarily show Google's own products.

### Three monetization approaches

| Approach | What you need to do | How you earn | Suitability for BlogLoom |
| --- | --- | --- | --- |
| **Ad network** | Apply to the platform; embed ad code after approval | Settled per platform-recognized ad display data | Easy to integrate, but content and earnings depend on ad demand |
| **Referral commission, CPS** | Recommend products using a dedicated referral link | Commission after the reader makes a qualifying purchase | **Fits deployment tutorials, cloud servers and dev-tool content** |
| **Direct sponsorship** | Negotiate ad placement, period and creatives with a brand | Charged monthly, per campaign, or per agreed performance | **Can control brand and display quality, but you must negotiate yourself** |

"Ad quality" depends not only on the platform's fame but also on whether the ads relate to your readers.

### Related platforms and channels

| Platform / channel | Positioning and eligibility | Recommendation |
| --- | --- | --- |
| [Google AdSense](https://adsense.google.com/start/) | For website monetization; requires quality original content, policy compliance and site control; applicants must be 18+ | **A good first test target for a site ad network** |
| [Baidu Union / Baiqingteng](https://union.baidu.com/bqt/) | Official traffic-cooperation entry for sites and apps; the public page alone isn't enough to confirm the full eligibility conditions for an individual blog | A domestic union candidate; confirm individual-entity and site-filing requirements first |
| [Tencent Cloud Promoter](https://cloud.tencent.com/act/partner/cps) | Individuals can apply after real-name verification; establishes customer association via a dedicated link, with commission on qualifying orders | **A great match for server, Docker and project-deployment tutorials** |
| [EthicalAds](https://www.ethicalads.io/publishers/) | Focused on developer sites; officially currently seeking sites with **50k+ monthly views** | Consider after building up technical content and traffic |
| [Tencent Marketing Alliance](https://e.qq.com/dev/index.html), [Pangle](https://www.csjplatform.com/) | Public products and integration material focus mainly on app ads and SDKs | **Not a first choice until plain blog web integration is confirmed** |

Among these, EthicalAds also constrains ad positions and the number of ads per page, while Carbon requires exclusive ads. So these platforms can't all be mixed in by default.

### Estimating ad revenue

For display ads, you can observe via **Page RPM: revenue per thousand page views**:

```plain
Monthly ad revenue ≈ monthly page views ÷ 1000 × measured Page RPM
```

The following is only a calculation example, **not a platform quote or a revenue forecast for BlogLoom**:

| Monthly views | Assuming Page RPM of ¥2 | Assuming Page RPM of ¥10 |
| --- | --- | --- |
| 10,000 | ¥20 | ¥100 |
| 50,000 | ¥100 | ¥500 |
| 100,000 | ¥200 | ¥1,000 |

Actual earnings are affected by visitor region, article topic, ad demand, ad blockers, display success rate and other factors. **Site PV is not equal to ad impressions**, so you also can't just multiply a platform's published ad CPM by all site PV.

**You need to look at three separate data sets:**

| Data | Where to look |
| --- | --- |
| Site visits, sources, reading | GA4 or Umami |
| Ad impressions, estimated earnings, final settlement | The ad network backend |
| Referral association, valid orders, commission | The referral platform backend |

Don't click ads yourself or ask friends to "help click"; Google explicitly prohibits artificially inflating impressions, clicks and other invalid traffic.

# 2. SEO optimization for the article detail page

## 2.1 Reference: dissecting a CSDN blog article

This section fetches a real CSDN blog detail page and dissects, from its **HTTP response and HTML source**, which SEO optimizations it does and which GEO (optimization for AI Q&A engines) optimizations it does, mapping them one by one to the capabilities in Chapter 1.

**The conclusion first: CSDN's article detail page implements almost all 8 capabilities mentioned in Chapter 1, and additionally adds a whole set of GEO meta-information.**

### 2.1.1 Collection method (curl)

The following request was used, carrying a browser UA, `Referer` and full cookies to emulate a real visitor as much as possible and avoid risk-control blocking:

```bash
curl --url 'https://blog.csdn.net/cl939974883/article/details/167174108?spm=1001.2014.3001.5501' \
  -H 'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8' \
  -H 'Accept-Language: zh-CN,zh;q=0.9,en;q=0.8' \
  -H 'User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36' \
  -b 'uuid_tt_dd=...; UserName=cl939974883; UserToken=...'
```

> Note: cookie values are private credentials, abbreviated with `...` here; their role is mainly to let CSDN return the full page as a "logged-in visitor". The actual response is full HTML (~350KB here); this article only excerpts the SEO/GEO-related source fragments.

Response status: `HTTP 200`.

### 2.1.2 Overview of the page's SEO capabilities (mapped to Chapter 1)

| Chapter 1 capability | Implemented by CSDN? | How CSDN does it |
| --- | --- | --- |
| Independent page title `<title>` | ✅ | `Java basics study notes 09, IO streams — File class and IO streams - CSDN blog` |
| Article summary `description` | ✅ | `<meta name="description">` + `description:direct_answer` |
| canonical | ✅ | Subdomains and tracking params all point to `blog.csdn.net/cl939974883/article/details/167174108` |
| Structured data | ✅✅ | Schema.org `Article` + Baidu Cambrian, two JSON-LD blocks |
| RSS autodiscovery | ✅ | `<link rel="alternate" type="application/rss+xml">` |
| Open Graph | ✅ | Full `og:*` + `article:*` + ByteDance `bytedance:*` |
| Body rendered directly | ✅ | The body is inside `#content_views`, server-rendered |
| Category and tag entries | ✅ | `article:section`, tags linking to on-site search pages |
| robots / index control | Site level | The article page itself has no `noindex`; managed site-wide |
| Sitemap | Site level | Not exposed directly on the detail page; via the site Sitemap + active push |
| Reading page performance | ⚠️ Partial | Images via CDN compression, but many third-party scripts |
| **GEO (for AI engines)** | ✅✅ | A whole set of `geo:*` meta-information + a `GEO check` entry |

### 2.1.3 Page basics (`<head>`)

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <title>Java basics study notes 09, IO streams — File class and IO streams - CSDN blog</title>
  <meta name="description"
        content="Starting from the File class, systematically explains the IO stream system and usage, covering byte streams and character streams...">
  <meta name="keywords" content="Java,IO stream,File,byte stream,character stream">
  <meta name="viewport"
        content="width=device-width, initial-scale=1.0, minimum-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <meta name="applicable-device" content="pc">
  <meta name="referrer" content="always">
  <link rel="shortcut icon" href="https://g.csdnimg.cn/static/logo/favicon32.ico" type="image/x-icon">
</head>
```

Key points:

+ `title` uses the "article title + site name" structure; the site name helps with brand keywords.
+ `description` is a readable summary, reusing the same copy as the OG and GEO summaries.
+ Although `keywords` is no longer a mainstream ranking factor, CSDN keeps it, consistent with the tags.
+ `applicable-device: pc` declares this is the PC page (there's a separate mobile version).

### 2.1.4 canonical: consolidating subdomains + tracking params

The visited address carries a tracking param `?spm=...`, and `blog_address` is set to the personalized subdomain `https://changlu.blog.csdn.net`:

```html
<link rel="canonical" href="https://blog.csdn.net/cl939974883/article/details/167174108"/>
```

```javascript
var blog_address = "https://changlu.blog.csdn.net";
var articleDetailUrl = "https://changlu.blog.csdn.net/article/details/167174108";
```

That is: **the personalized `changlu.blog.csdn.net` and the tracking link with `?spm=` all converge on the same standard address `blog.csdn.net/cl939974883/article/details/167174108`.** This is exactly the canonical usage from Chapter 1 — declaring the preferred URL to search engines, avoiding the same article being split into multiple URLs that dilute authority.

### 2.1.5 Structured data: two JSON-LD blocks

CSDN places two JSON-LD blocks in `<head>`, one for general search engines and one for Baidu:

**Block 1: Schema.org `Article` (with GEO extension fields)**

```html
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "Article",
  "headline": "Java basics study notes 09, IO streams — File class and IO streams",
  "url": "https://blog.csdn.net/cl939974883/article/details/167174108",
  "image": "https://i-blog.csdnimg.cn/direct/998cac412100451991df30879630bd9b.png",
  "keywords": "Java,IO stream,File,byte stream,character stream",
  "inLanguage": "zh-CN",
  "datePublished": "2026-10-06T15:09:17+08:00",
  "dateModified": "2026-10-06T15:09:34+08:00",
  "description": "Starting from the File class, systematically explains the IO stream system and usage...",
  "articleSection": "Java",
  "author": [{
    "@type": "Person",
    "name": "Changlu",
    "url": "https://blog.csdn.net/cl939974883"
  }],
  "publisher": {
    "@type": "Organization",
    "name": "CSDN",
    "logo": { "@type": "ImageObject", "url": "https://g.csdnimg.cn/static/logo/favicon32.ico" }
  },
  "geoSummary": {
    "heading": "Content summary",
    "items": {
      "Topic": "Java basics study notes 09, IO streams — File class and IO streams",
      "Type": "article",
      "Use case": "Starting from the File class, systematically explains the IO stream system and usage...",
      "Core features": "Java,IO stream,File,byte stream,character stream",
      "Updated": "2026-10-06T15:09:34+08:00"
    },
    "keyPoints": ["Java", "IO stream", "File", "byte stream", "character stream"],
    "citationContext": {
      "authoritySource": "Changlu",
      "factChecked": true,
      "lastVerified": "2026-10-06T15:09:34+08:00"
    }
  }
}
</script>
```

As seen, besides the standard `Article` fields (title, author, publish/modified time, `publisher`), it stuffs a custom `geoSummary` into the JSON-LD, structuring "topic / type / use case / core features / updated / key points / citability" — **fields fed specifically to AI Q&A engines**.

**Block 2: Baidu Cambrian structured data**

```html
<script type="application/ld+json">
{"@context":"https://ziyuan.baidu.com/contexts/cambrian.jsonld",
 "@id":"https://blog.csdn.net/cl939974883/article/details/167174108",
 "appid":"1638831770136827",
 "pubDate":"2026-10-06T15:09:17",
 "title":"Java basics study notes 09, IO streams — File class and IO streams - CSDN blog",
 "upDate":"2026-10-06T15:09:17"}
</script>
```

This is the Cambrian format prescribed by Baidu's resource platform (ziyuan.baidu.com), used to tell Baidu an article's publish and update time, so Baidu can show time information in results. **Outputting structured data separately for different search engines** is a clear strategy of theirs.

### 2.1.6 Open Graph and sharing meta-information

CSDN writes the tags used when sharing to social/IM platforms very completely:

```html
<meta property="og:locale" content="zh_CN">
<meta property="og:type" content="article">
<meta property="og:title" content="Java basics study notes 09, IO streams — File class and IO streams - CSDN blog">
<meta property="og:description" content="Starting from the File class, systematically explains the IO stream system and usage...">
<meta property="og:url" content="https://blog.csdn.net/cl939974883/article/details/167174108">
<meta property="og:site_name" content="CSDN Blog">
<meta property="og:image" content="https://i-blog.csdnimg.cn/direct/998cac412100451991df30879630bd9b.png">
<meta property="og:image:width" content="1200">
<meta property="og:image:height" content="630">
<meta property="og:image:alt" content="Java basics study notes 09, IO streams — File class and IO streams">
<meta property="article:published_time" content="2026-10-06T15:09:17+08:00">
<meta property="article:modified_time" content="2026-10-06T15:09:34+08:00">
<meta property="article:author" content="Changlu">
<meta property="article:section" content="Java">
<meta property="article:tag" content="Java,IO stream,File,byte stream,character stream">
```

| Tag | Role |
| --- | --- |
| `og:*` | Share card title, summary, cover, canonical URL, site name |
| `og:image:width/height` | Explicit cover dimensions, so platforms can lay out at 1200×630 |
| `og:image:alt` | Cover alt text, helping accessibility and image understanding |
| `article:published_time` / `modified_time` | Article publish and update times |
| `article:author` / `section` / `tag` | Author, category, tags |

**Notably, `og:url` is fully consistent with canonical, both pointing to the standard address** — the two data sets share the same source, matching Chapter 1's advice to "share the same article data between SEO and Open Graph". It also outputs extra tags for the ByteDance family (Toutiao), such as `bytedance:published_time` and `bytedance:updated_time`.

### 2.1.7 GEO optimization: meta-information for AI search engines (key)

This is a set of optimizations CSDN has adopted recently that is very typical and worth learning. It uses a batch of custom `<meta>` tags to organize the article into "**facts an AI can directly cite**". GEO can be understood as **Generative Engine Optimization** — making Q&A engines like ChatGPT, Doubao and Wenxin Yiyan more willing to cite your content.

```html
<meta name="geo:keywords" content="Java,IO stream,File,byte stream,character stream">
<meta name="geo:summary" content="Starting from the File class, systematically explains the IO stream system and usage...">
<meta name="geo:entity_type" content="Article">
<meta name="geo:subject" content="Java basics study notes 09, IO streams — File class and IO streams">
<meta name="geo:key_points" content="Java|IO stream|File|byte stream|character stream">
<meta name="geo:citation_ready" content="true">

<meta name="description:direct_answer" content="Starting from the File class, systematically explains the IO stream system and usage...">
<meta name="author:credentials" content="Changlu">
<meta name="author:affiliation" content="csdn">
<meta name="geo:source_type" content="original">
<meta name="geo:fact_checked" content="true">
<meta name="geo:editorial_policy" content="https://passport.csdn.net/service">
<meta name="geo:update_frequency" content="daily">
<meta name="geo:maintenance_status" content="active">
```

Explained one by one:

| Meta-information | Meaning | Effect on AI engines |
| --- | --- | --- |
| `geo:summary` | Content summary | Gives the AI a directly extractable overview |
| `description:direct_answer` | Direct answer | Explicitly tells the AI "this passage is the answer" |
| `geo:entity_type` | Entity type `Article` | Helps the AI judge this is an article, not Q&A/product |
| `geo:subject` | Subject | The content's gist |
| `geo:key_points` | Key points (`|`-separated) | Distills points for easy AI extraction |
| `geo:citation_ready` | Whether "citation ready" | Declares it can be cited directly |
| `geo:source_type` | Source type `original` | Emphasizes originality, improving credibility |
| `geo:fact_checked` | Whether fact-checked | Declares the content has been fact-checked |
| `author:credentials` / `author:affiliation` | Author identity and affiliation | Provides author authority (E-E-A-T) |
| `geo:editorial_policy` | Editorial policy link | Lets the AI know the source of editorial standards |
| `geo:update_frequency` | Update frequency `daily` | Declares the content stays updated |
| `geo:maintenance_status` | Maintenance status `active` | Declares the content is still maintained |

In addition, the article title area has a visible **"GEO check" button** linking to `https://mp.csdn.net/geo`, indicating CSDN has turned GEO into a product feature for authors:

```html
<a class="geo-detection-btn"
   href="https://mp.csdn.net/geo?title=...&url=https%3A%2F%2Fchanglu.blog.csdn.net%2Farticle%2Fdetails%2F167174108&utm_source=blog_geo"
   data-report-view='{"spm":"3001.11779"}'>
  <span class="geo-detection-text">GEO check</span>
</a>
```

> Summary: CSDN's GEO approach = **on top of ordinary SEO meta-information, use fields like `geo:*`, `description:direct_answer`, `author:credentials` and `citation_ready` to proactively declare to AI engines "what this content is, whether it's trustworthy, and whether it can be cited directly".** This idea can be migrated to BlogLoom.

#### 2.1.7.1 Investigation: is there an "official standard" for GEO? Is CSDN copying some platform?

The conclusion first: **CSDN's `geo:*` is not a tag defined by a formal standard like W3C, IETF or Schema.org, but a naming scheme developed in-house at CSDN.** It aligns with the GEO industry concept that rose rapidly in 2026 (especially the "trustworthy corpus" principles of China's CAAC group standards), but **no search engine officially acknowledges reading `geo:*` meta-information**. Dividing GEO's sources into four layers makes it clearer:

| Layer | Source | Time | Nature | Relationship to CSDN's `geo:*` |
| --- | --- | --- | --- | --- |
| Academic origin | Aggarwal et al., "GEO: Generative Engine Optimization" (Princeton, KDD 2024, arXiv:2311.09735) | 2023 paper / 2024 conference | Academic paper, first proposing the term and definition of GEO | Provides the "GEO" concept itself, but defines no HTML tags |
| Domestic industry group standard | China Advertising Association (CAAC) "Generative Engine Optimization (GEO)" five group standards `T/CAACCHINA 001~005-2026` | Published 2026-09-14 | Group standard (recommended, not national) | CSDN's `original / fact_checked / update_frequency / author:credentials / editorial_policy` etc. clearly align with its "trustworthy corpus" requirements |
| Open proposals | `llms.txt` (proposed by Jeremy Howard / Answer.AI), `llms-meta-tags` (community, using the `llms:` prefix) | Since 2024 | Community proposals, not widely adopted | CSDN didn't use `llms:` but coined the `geo:` prefix |
| Search engine official | Google's "Optimizing for generative AI features" guide (2026), Bing Webmaster guidelines, Microsoft Advertising guidelines | 2026 | Official documentation | **Google explicitly states such special markup is unnecessary**; Bing documents GEO but emphasizes SEO fundamentals |

**Several key facts:**

1. **The CAAC five group standards are currently the closest thing to an "official basis" for GEO in China**: general theory and terminology, service-provider evaluation, pricing and measurement, trustworthy corpus, and service-process compliance and security. The "trustworthy corpus" standard proposes six principles — **truthful, authoritative, professional, traceable, dynamically updated and neutral**. CSDN's `geo:source_type=original`, `geo:fact_checked`, `geo:update_frequency=daily`, `geo:maintenance_status=active`, `author:credentials` and `geo:editorial_policy` are exactly this set of principles mapped onto page meta-information. But note: **this is a "service and corpus" standard, not an "HTML meta-tag" standard.**

2. **Google's 2026 AI optimization guide explicitly "pours cold water"**: terms like AEO/GEO are hot online, but many so-called shortcuts don't work; Google directly writes that **Google Search does not use "LLMS.txt files and other special markup"**, and generative AI search **does not need structured data** or **content specially rewritten or chunked for AI**. That is, Google does not adopt "markup specifically fed to AI" such as `llms.txt` and `geo:*`.

3. **The `geo:` prefix itself has another meaning**: in HTML, `geo.position`, `geo.placename` and `geo.region` are historically meta-tag conventions for **geographic location**, sharing the name `geo:` with CSDN's "generative engine optimization" but with a **completely different meaning**. This is naming reuse, not standard inheritance.

4. **CSDN has made it a product of its own**: the article page's "GEO check" button jumps to `https://mp.csdn.net/geo`, indicating `geo:*` is more about **serving CSDN's own GEO detection/optimization feature** than a general protocol for external AI engines.

**So the conclusion for BlogLoom:**

```plain
GEO meta-information (geo:*)
├── Not a formal standard: neither W3C / IETF / Schema.org defines it
├── Has industry reference: CAAC five group standards (trustworthy corpus: truthful/authoritative/professional/traceable/dynamically updated/neutral)
├── Google's official stance: no special markup needed, no content specially written for AI (still SEO)
└── Platform in-house: CSDN uses it to serve its own GEO check feature

BlogLoom implementation advice
├── The mainline is still standard SEO: title / description / canonical / Schema.org / Sitemap / body rendered directly
├── geo:* meta-information is optional: can be added as an "enhancement signal", but adoption by any AI engine isn't guaranteed
└── What really works is the content itself: original, with authoritative sources, cited data and continuous updates
    (which happens to align with the six CAAC trustworthy-corpus principles)
```

> In one sentence: **CSDN's GEO is not copying some official protocol, but a platform implementation of "industry GEO concepts + CAAC trustworthy-corpus principles"; its value lies not in the tags themselves but in the content credibility behind them. BlogLoom should prioritize solid standard SEO and content quality, with `geo:*` as a nice-to-have.**

Sources:

+ Aggarwal, P. et al. *GEO: Generative Engine Optimization*, KDD 2024, arXiv:2311.09735 — https://arxiv.org/abs/2311.09735
+ China Advertising Association (CAAC) "Generative Engine Optimization (GEO)" five group standards T/CAACCHINA 001~005-2026 (2026-09-14)
+ Google Search Central, "Optimizing for generative AI features in Google Search" — https://developers.google.com/search/docs/fundamentals/ai-optimization-guide
+ Wikipedia, *Generative engine optimization* — https://en.wikipedia.org/wiki/Generative_engine_optimization
+ The `llms.txt` proposal — https://llmstxt.org

### 2.1.8 Body rendered directly (server-side rendering)

The body is not loaded asynchronously via JavaScript but is **written directly into the returned HTML**:

```html
<article class="baidu_pl">
  <div id="article_content" class="article_content clearfix">
    <div id="content_views" class="markdown_views prism-tomorrow-night">
      <div class="toc">
        <h4>Table of contents</h4>
        <ul>
          <li><a href="#_4">Preface</a></li>
          <li><a href="#File_16">1. File class</a></li>
          ...
        </ul>
      </div>

      <h2><a id="_4"></a>Preface</h2>
      ...
      <h2><a id="File_16"></a>1. File class</h2>
      ...
    </div>
  </div>
</article>
```

Key points:

+ **The title uses a single `<h1>` and sections use `<h2>`/`<h3>`**, with clear hierarchy (this fetch captured 1 `<h1>`, 2 `<h2>` and 9 `<h3>`).
+ **Every heading has an `id` anchor** (e.g. `id="File_16"`) and a top TOC is generated, which helps reader navigation and lets search engines understand the structure.
+ The body, code blocks and images are all in the first-screen HTML, matching Chapter 1's performance/indexing requirement of "the first HTML directly contains the body".

### 2.1.9 Category and tag entries

The article declares its category via `article:section`, with tags at the bottom and top, and tag links pointing to on-site search aggregation pages:

```html
<meta property="article:section" content="Java">
<meta property="article:tag" content="Java,IO stream,File,byte stream,character stream">

<a class="tag-link-new" rel="nofollow"
   href="https://so.csdn.net/so/search/s.do?q=IO%E6%B5%81&...">#IO stream</a>

<a class="item-target"
   href="https://blog.csdn.net/cl939974883/category_13215351.html"
   title="Java backend development from beginner to advanced">Column: Java backend development from beginner to advanced</a>
```

| Entry | Role |
| --- | --- |
| Tag page `so.csdn.net/...?q=IO stream` | Aggregates same-topic articles into a crawlable list page, forming internal links |
| Column/category page `category_13215351.html` | Article categorization entry, helping crawlers discover other articles in the same series |
| `rel="nofollow"` | Declares that aggregate links like tags don't pass authority, avoiding abuse |

### 2.1.10 On-site interlinking and content discovery

CSDN lays out many related links on the detail page to help crawlers keep discovering content and to increase reader dwell time:

| Position | Form | Source example |
| --- | --- | --- |
| Previous/next | Same-series sequential links | `<a href=".../167174099">Java basics study notes 08, exception handling</a>` |
| Related recommendations | ElasticSearch semantic relevance | `recommend-item-box type_blog`, with `distribute.pc_relevant...` |
| People are reading | On-site popular articles | `hotArticle-list` under `#asideHotArticle` |
| Column entry | Series aggregation page | `category_13215351.html` |

Among these, the "related recommendations" links are **rendered server-side after relevance sorting**, not purely client-side, which is very crawler-friendly.

### 2.1.11 Crawl submission and site verification (site level, but traces visible on article pages)

In the detail page source you can see the push and verification scripts CSDN configures for various search engines:

| Platform | Trace | Role |
| --- | --- | --- |
| Shenma Search (UC) | `<meta name="shenma-site-verification" content="...">` | Site ownership verification |
| Baidu | `<script src="https://zz.bdstatic.com/linksubmit/push.js">` | Baidu **active push** of new articles |
| Toutiao/ByteDance | `lf1-cdn-tos.bytegoofy.com/goofy/ttzz/push.js` | Toutiao Search active push |
| Baidu | The Cambrian JSON-LD mentioned above | Tells Baidu time information |

**This is exactly the engineering implementation of "telling search engines" from Chapter 1's "three things in configuring a Sitemap"**: beyond the Sitemap, CSDN also stacks active push APIs from several search engines to speed up indexing.

### 2.1.12 RSS autodiscovery

The page declares the blog's RSS address, auto-discoverable by readers:

```html
<link rel="alternate" type="application/rss+xml"
      title="Changlu"
      href="https://blog.csdn.net/cl939974883/rss/map">
```

This corresponds to Chapter 1's "RSS autodiscovery" — the article page only declares the feed address; the feed itself is maintained by the blog home page.

### 2.1.13 Reading page performance (partially implemented)

| Chapter 1 requirement | CSDN's approach | Assessment |
| --- | --- | --- |
| Body rendered directly | `#content_views` server-rendered | ✅ Good |
| Image optimization | Images via `i-blog.csdnimg.cn`, compressible/resizable with `?x-oss-process=image/resize,...` | ✅ Good |
| Image lazy loading | No `loading="lazy"` / `data-src` lazy attributes seen on body images | ⚠️ Room for improvement |
| Reduce blocking scripts | References jQuery, MathJax and several third-party analytics/push scripts | ⚠️ Many third-party scripts, mitigated by async loading |

> For a self-hosted platform like BlogLoom, CSDN's performance isn't perfect — **image lazy loading + controlling third-party scripts** are areas we can do better.

### 2.1.14 Summary: CSDN article detail page optimization checklist

```plain
Basics
├── <html lang="zh-CN">
├── Independent <title> (article title + site name)
├── <meta description> / keywords
└── viewport / referrer / favicon

Search engines
├── canonical (consolidating subdomains + tracking params)
├── Structured data: Schema.org Article
├── Structured data: Baidu Cambrian
├── Active push: Baidu push.js / Toutiao push.js
└── Site verification: shenma-site-verification

Content propagation
├── Open Graph (og:* + article:*)
├── ByteDance meta-information (bytedance:*)
└── RSS autodiscovery (<link rel="alternate" type="application/rss+xml">)

GEO (for AI engines)
├── geo:summary / geo:subject / geo:key_points
├── geo:entity_type / geo:source_type / geo:citation_ready
├── description:direct_answer
├── author:credentials / author:affiliation
├── geo:fact_checked / geo:update_frequency / geo:maintenance_status
└── geoSummary (written inside JSON-LD) + GEO check entry

Page and content
├── Body rendered server-side (#content_views)
├── h1/h2/h3 + heading id anchors + TOC
├── Category / tag aggregation entries
└── On-site interlinking (previous, related, popular, columns)

Performance
├── Image CDN + resize params
└── Body rendered directly (many third-party scripts; lazy loading missing)
```

**In one sentence: on top of "standard SEO (title/description/canonical/structured data/OG/RSS)", CSDN stacks a whole set of "GEO (geo:* meta-information + direct answer + citability declaration)", and implements almost all 8 capabilities. BlogLoom can align item by item against this checklist, where the `geo:*` meta-information and the `geoSummary` in structured data are the most worthwhile differentiation points to fill in first.**

# 3. Data metric monitoring

**A website's data monitoring is divided into**: traffic data, indexing status and revenue, across different platforms.

## 3.1 Google platforms overview

| What you want to know | Official platform and entry | Data you can see |
| --- | --- | --- |
| **Is Google indexing it? Which keywords bring traffic?** | [Google Search Console](https://search.google.com/search-console/) | Index status, crawl issues, search keywords, impressions, clicks, average position |
| **How many people came? Which articles did they read? Where from?** | [Google Analytics (GA4)](https://analytics.google.com/) | Users, page views, traffic sources, popular pages, engagement and events |
| **How much did the site earn showing Google ads?** | [Google AdSense](https://adsense.google.com/start/) | Estimated earnings, balance, payments, plus ad impression and click data |

---

## Next steps

- [BlogLoom SEO 02: Why SPA blogs need server-side rendering and site-level files](/v1/en/blogs/seo/02-ssr-and-site-files)
- [BlogLoom SEO 03: Google AdSense integration guide](/v1/en/blogs/seo/03-adsense-guide)
- [SEO, subscription and site monetization configuration](/v1/en/blogs/features/seo-and-monetization)
