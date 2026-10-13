---
title: "BlogLoom SEO 03: Google AdSense Integration Guide"
description: From publisher ID to auto/manual ad slots and ads.txt: how Google AdSense is integrated and configured in BlogLoom.
---

# Google AdSense Integration and Configuration Guide

> This article is part of the BlogLoom SEO series, focusing specifically on how to integrate and configure Google AdSense.
> Related: [BlogLoom SEO 01](/v1/en/blogs/seo/01-article-page-monetization) · [BlogLoom SEO 02](/v1/en/blogs/seo/02-ssr-and-site-files)

---

## 1. Why integrate AdSense

For a self-hosted blog, Google AdSense is the lowest-friction way to start monetizing: apply with a site, place a loader script, create ad units, and AdSense matches relevant ads automatically. Unlike CPS referral programs that need specific product content, or direct sponsorship that needs negotiation, AdSense works with any content and requires only configuration.

**Key point: AdSense is a "content monetization" channel, not a "content distribution" channel.** Its revenue depends on page views, visitor region, article topic and ad demand; the site owner doesn't manage the ads directly.

---

## 2. Key concepts

| Concept | Meaning |
| --- | --- |
| Publisher ID | `pub-XXXXXXXXXXXXXXXX`, your AdSense account identifier |
| Ad unit | A single ad slot with its own numeric `slot` id |
| Auto ads | AdSense places ads automatically; needs only the loader script + publisher ID |
| Manual ads | You place `<ins class="adsbygoogle">` tags at chosen positions |
| `ads.txt` | The Authorized Digital Sellers file, declaring who may sell your inventory |
| Page RPM | Revenue per thousand page views |
| CPM | Cost per thousand impressions (advertiser-side) |

The AdSense loader script:

```html
<script async
  src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-XXXXXXXXXXXXXXXX"
  crossorigin="anonymous"></script>
```

A manual ad unit:

```html
<ins class="adsbygoogle"
     style="display:block"
     data-ad-client="ca-pub-XXXXXXXXXXXXXXXX"
     data-ad-slot="1234567890"
     data-ad-format="auto"
     data-full-width-responsive="true"></ins>
<script>
     (adsbygoogle = window.adsbygoogle || []).push({});
</script>
```

---

## 3. One publisher ID, N ad units

A single publisher ID can drive many ad units. Each unit has a distinct `slot`. In BlogLoom, the backend stores:

+ The **publisher ID** (once);
+ Each **position's slot** (top, in-article, bottom, below the right TOC).

The loader script is injected once into the head; each position injects its own `<ins>` unit. This design means you can enable, disable or reposition ads from the backend without code changes.

---

## 4. Ad position planning

| Position | Rationale | Notes |
| --- | --- | --- |
| Top | High visibility | Must not push the body down; reserve height to avoid CLS |
| In-article | Highly relevant to reading context | Usually the best-performing manual slot |
| Bottom | Low interference | Good post-read impressions |
| Below the right TOC | Fills otherwise-empty wide-screen sidebar | Only on wide screens |

**Design principles:**

+ Reserve dimensions for every ad slot to avoid layout shift (CLS).
+ Don't stack too many ads; it hurts experience and can violate policy.
+ Keep ads clearly distinguishable from content.

---

## 5. Ad position ↔ page mapping

| Page | Ads served? | Reason |
| --- | --- | --- |
| Article detail | Yes (all positions) | The primary reading page and the best monetization surface |
| Home / list / aggregation | Yes (top/bottom) | High traffic, but avoid overloading |
| Backend `/cms`, `/admin` | **No** | The backend isn't public and doesn't need ads |

---

## 6. Configuration in BlogLoom

1. **Maintain the publisher ID** in the backend "Ad platform association" page.
2. **Fill each position's slot** (top, in-article, bottom, below the right TOC), or paste the ad-unit code and let BlogLoom extract `client`/`slot` automatically.
3. **Edit `ads.txt` content** (see section 7).
4. **Enable/disable**: if the publisher ID is empty, no ads are injected anywhere.

The backend injects:

+ The loader script into the head of the SPA entry and the SSR pages;
+ Each manual ad unit at its configured position;
+ `/ads.txt` generated from the configured content (or served empty/404 when unconfigured).

> Priority and behavior: the SSR pages and the SPA pages both read the same config, so ads appear consistently on both.

---

## 7. ads.txt

`ads.txt` (Authorized Digital Sellers) is a plain-text file at the site root declaring which companies are authorized to sell your ad inventory. It helps prevent unauthorized reselling and protects brand safety.

A typical AdSense line:

```plain
google.com, pub-XXXXXXXXXXXXXXXX, DIRECT, f08c47fec0942fa0
```

Where:

+ `google.com` — the advertising system domain;
+ `pub-XXXXXXXXXXXXXXXX` — your publisher ID;
+ `DIRECT` — you have a direct relationship with the seller;
+ `f08c47fec0942fa0` — Google's certification authority id (TAG ID).

BlogLoom serves `/ads.txt` from the backend-configured content, so you can paste the line provided by AdSense directly.

---

## 8. Compliance and review points

+ **Original, policy-compliant content** with a privacy policy page.
+ **No invalid traffic**: never click your own ads or ask others to, which risks account termination.
+ **No misleading placement**: don't obscure content or make ads look like navigation.
+ **`ads.txt` correct**, declaring Google as an authorized seller.
+ **Age and eligibility**: AdSense applicants must be 18+ and have site control.

---

## 9. Revenue and metrics

For display ads, observe via **Page RPM**:

```plain
Monthly ad revenue ≈ monthly page views ÷ 1000 × measured Page RPM
```

Example only (not a platform quote or a BlogLoom forecast):

| Monthly views | Page RPM ¥2 | Page RPM ¥10 |
| --- | --- | --- |
| 10,000 | ¥20 | ¥100 |
| 50,000 | ¥100 | ¥500 |
| 100,000 | ¥200 | ¥1,000 |

Watch three data sets separately:

| Data | Where |
| --- | --- |
| Site visits, sources, reading | GA4 / Umami |
| Ad impressions, estimated earnings, settlement | AdSense backend |
| Referral association, valid orders, commission | The referral platform backend |

**Page PV ≠ ad impressions**, so don't multiply a published CPM by all site PV.

---

## 10. Troubleshooting (ads not showing)

| Symptom | Checks |
| --- | --- |
| No ads anywhere | Publisher ID empty? Loader script injected? |
| Some positions empty | Slot id wrong? Ad blocker on? Ad inventory low for your region? |
| Review rejected or limited | Content/policy issues; check AdSense messages |
| Earnings drop suddenly | Policy issue, seasonal demand, traffic-source change |

---

## 11. Reference index

+ [Google AdSense](https://adsense.google.com/start/)
+ [AdSense Help: eligibility requirements](https://support.google.com/adsense/answer/9724)
+ [AdSense Help: invalid traffic](https://support.google.com/adsense/answer/48182)
+ [IAB Tech Lab ads.txt specification](https://iabtechlab.com/ads-txt/)

---

## Next steps

- [BlogLoom SEO 01: Article page optimization, monetization and data monitoring](/v1/en/blogs/seo/01-article-page-monetization)
- [BlogLoom SEO 02: Why SPA blogs need server-side rendering and site-level files](/v1/en/blogs/seo/02-ssr-and-site-files)
- [SEO, subscription and site monetization configuration](/v1/en/blogs/features/seo-and-monetization)
