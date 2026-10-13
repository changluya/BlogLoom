---
title: "SEO、订阅与变现"
description: 站点级 SEO 配置、robots / sitemap / rss 站点级文件与 Google AdSense 广告接入。
---

BlogLoom 内置站点级 SEO 能力：公开页面服务端直出，自动生成 robots / sitemap / rss，并支持接入站点验证与 Google AdSense 广告。

## SEO 配置

在后台「SEO 优化 → SEO 配置」中设置站点对外的 SEO 域名（canonical、sitemap、RSS 均以它为准），并提供核心文件的快捷查看与下载：

| 文件 | 作用 |
| --- | --- |
| `robots.txt` | 抓取规则并声明 Sitemap |
| `sitemap.xml` | 公开文章地图 |
| `rss.xml` | 订阅源 |
| `ads.txt` | 广告授权售卖声明 |

## 服务端直出

文章详情、首页、标签、分类、专栏、归档等公开页面由后端 Thymeleaf 服务端直出，补齐页面级 title / description / canonical / Open Graph / JSON-LD 结构化数据，让搜索引擎可直接读取正文。

## 搜索平台站点验证

后台「SEO 优化 → SEO 平台关联」支持百度 / Bing / Google 站点验证：把平台给出的验证代码整段粘贴进来（自动提取其中的 content 值），保存后站点首页与聚合页源码即输出对应验证 meta；留空则不输出。

## 广告变现（Google AdSense）

后台「SEO 优化 → 广告平台关联」可配置：

- 发布商 ID
- 自动 / 手动广告位（顶部、文内、底部、右侧目录下方）
- `ads.txt` 内容

可粘贴广告单元代码自动识别 `client` / `slot`；启用后前台与 SEO 直出页按位置注入广告加载器与广告位，留空则全站不投放。

## 下一步

- [SEO 原理 01：文章页优化、站点变现与数据监测](/v1/zh/blogs/seo/01-article-page-monetization)
- [SEO 原理 02：服务端直出与站点级文件](/v1/zh/blogs/seo/02-ssr-and-site-files)
- [SEO 原理 03：Google AdSense 广告接入](/v1/zh/blogs/seo/03-adsense-guide)
