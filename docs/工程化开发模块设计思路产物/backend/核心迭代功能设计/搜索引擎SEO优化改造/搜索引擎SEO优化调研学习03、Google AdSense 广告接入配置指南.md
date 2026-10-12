# 搜索引擎SEO优化调研学习03、Google AdSense 广告接入配置指南

> **本文定位：** 把《搜索引擎SEO优化调研学习02》里分散在 **4.4 广告接入需求与原理分析** 与 **5.3 广告接入（Google AdSense）** 两块内容，抽出来整理成一篇**可独立阅读、可直接照做**的 AdSense 配置指南。
>
> 与早期方案相比，本文的操作部分已对齐 **BlogLoom 当前真实实现**：后台是「**总开关 + 四段式表单**」，广告位**直接粘贴 Google 代码**由系统自动识别，**技术位置由系统内部维护**，`ads.txt` 通过「**验证并下载**」按钮生成。

---

# 一、背景引入：流量有了，收益从哪来

## 1.1、一个真实场景：文章被收录了，然后呢？

在 02 篇里，我们花了很大力气让 BlogLoom 从「爬虫眼里的空壳」变成「能被收录的博客」：服务端直出、sitemap、robots、canonical、结构化数据……

文章慢慢被 Google 收录，也来了自然流量。这时候一个很自然的问题出现了：

> **流量来了，怎么让它变成收益？**

对于个人技术博客，最常见、门槛最低的答案就是——**Google AdSense**。

## 1.2、为什么先接 Google AdSense？——四种变现方式对比

广告与变现平台有很多：Google AdSense、国内广告联盟（如百度联盟）、CPS 带货、品牌赞助……为什么第一阶段只选 Google AdSense？

| 选择 | 门槛 | 与博客的匹配度 | 结算 | 结论 |
| --- | --- | --- | --- | --- |
| **Google AdSense** | 有网站即可申请，需内容审核 | 与「原创技术博客」高度契合，自动匹配技术/工具类广告 | 满 100 美元电汇，国内可用 | **首选** |
| 国内广告联盟 | 多要求备案、流量门槛高 | 个人站一般达不到 | 人民币，门槛不一 | 后续可选 |
| CPS / 带货 | 需选品、写推广文 | 会稀释内容调性 | 佣金制 | 不强求 |
| 品牌赞助 | 需议价、接洽 | 依赖流量 | 一单一议 | 有流量后再说 |

**核心判断：** AdSense 是「接入成本最低、对内容破坏最小、又能覆盖全球技术流量」的起步方案。先把 AdSense 跑通，就等于验证了「**内容 → 流量 → 收益**」这条路是通的。

## 1.3、AdSense 与 SEO 的正向循环

很多人以为「做 SEO」和「接广告」是两件事，其实 AdSense 审核恰恰依赖一阶段 SEO 的产出。它们是一个正向循环：

```plain
SEO 改造（可抓取 + sitemap + 隐私政策 + 清晰结构）
        ↓
通过 Search Console 被收录、有自然流量
        ↓
AdSense 申请时「看起来像认真运营的站」
        ↓
过审 → 投放 → 收益
```

**重点：** AdSense 不是「申请了就批」。它有一套**内容与合规红线**（见 3.5），其中很大一部分（隐私政策、可抓取、sitemap、站点结构清晰）**正是 02 篇已经做完的事**。换句话说：**SEO 改造做扎实，AdSense 过审会顺很多。**

## 1.4、本文范围与阅读指引

> **范围说明：** 广告属于变现阶段，不在「收录最小闭环」里。但 BlogLoom 已经在代码层把 AdSense 的**配置、装配、注入、ads.txt** 全部落地，所以本文按「**原理 → 需求 → 配置规则 → 代码 → 实操 → 验证**」的顺序完整讲清。

阅读顺序建议：

| 章节 | 内容 | 适合谁 |
| --- | --- | --- |
| 二、核心概念讲解 | 加载器 / 广告单元 / 发布商 ID / 投放方式 / ads.txt | 第一次接触 AdSense 的人 |
| 三、核心功能需求分析调研 | 需求清单、布局、合规、收益、FAQ | 产品/需求梳理 |
| 四、功能需求与 UI 配置规则 | 后台四段式表单与全部校验规则 | 前端/测试/验收 |
| 五、核心代码设计实现 | 数据模型、装配、注入、ads.txt | 后端/全栈 |
| 六、实战配置步骤 | 申请、建单元、填后台、上线 | 站长本人 |
| 七、验证测试 | 浏览器 / curl / 单元测试 | 测试/上线前 |

---

# 二、核心概念讲解：先搞懂这些再动手

写代码、配后台之前，先把概念理清，否则很容易出现「**代码贴了，广告却不显示**」的情况。

## 2.1、三样东西：加载器、广告单元、自动广告

AdSense 在页面层面的构成，其实只有三样东西：

| 概念 | 英文 | 作用 | 放在哪 |
| --- | --- | --- | --- |
| **AdSense 代码**（加载器） | AdSense code / adsbygoogle.js | 加载 AdSense SDK，是所有广告的基础脚本 | 每个页面的 `<head>` |
| **广告单元** | Ad unit | 一个具体的广告位，带唯一 `data-ad-slot` | 页面正文里，用 `<ins class="adsbygoogle">` 占位 |
| **自动广告** | Auto ads | 由 Google 自动决定在页面哪里插广告 | 只要加载器在，Google 自己插 |

官方说明：**「要把 AdSense 代码放在每个页面的 `<head>` 和 `</head>` 之间」**，才能覆盖全站、让 Auto ads 生效（[About the AdSense code](https://support.google.com/adsense/answer/9274634)）。

**关键更新：** 官方已说明 **Auto ads 现在可以通过任意 AdSense 广告单元代码生效**，不再强制要求单独粘贴 Auto ads 代码——**只放一个「广告单元」代码，也能吃到 Auto ads 的自动投放**。

**① 加载器代码形态：**

```html
<!-- 加载器：放在 <head> 里，ca-pub-XXXXXXXXXXXXXXXX 换成你的发布商 ID -->
<script async
  src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-XXXXXXXXXXXXXXXX"
  crossorigin="anonymous"></script>
```

**② 广告单元代码形态：**

```html
<!-- 自适应广告单元：data-ad-client 是发布商 ID，data-ad-slot 是该广告位 ID -->
<ins class="adsbygoogle"
     style="display:block"
     data-ad-client="ca-pub-XXXXXXXXXXXXXXXX"
     data-ad-slot="1234567890"
     data-ad-format="auto"
     data-full-width-responsive="true"></ins>
<script>(adsbygoogle = window.adsbygoogle || []).push({});</script>
```

参数含义（官方 [自适应广告代码参数](https://support.google.com/adsense/answer/9183460)）：

| 参数 | 含义 | 取值 |
| --- | --- | --- |
| `data-ad-client` | 发布商 ID | `ca-pub-…` |
| `data-ad-slot` | 广告单元 ID | 数字串 |
| `data-ad-format` | 大体形状（桌面） | `auto` / `rectangle` / `horizontal` / `vertical` |
| `data-ad-layout` | 内容内嵌布局 | 如 `in-article` |
| `data-full-width-responsive` | 移动端是否全宽展开 | `true`（官方推荐）/ `false` |

**重点：**

+ **加载器只需一次**（`<head>`），**每出现一个 `<ins>` 广告位，就要紧跟一次 `push({})`**；少 push 不显示，多 push 会报错。
+ 自适应广告的父容器要**有稳定宽度**；父容器为 `width:auto` 或运行时才算宽，可能出现移动端布局异常。

## 2.2、两级 ID：发布商 ID 与广告位 ID

这是最容易搞混、也最该先搞清的一点。AdSense 里的标识分**两级**：

| 标识 | 数量 | 是什么 | 出现在哪 |
| --- | --- | --- | --- |
| **发布商 ID**（`ca-pub-XXXX`） | **全站唯一** | 整个 AdSense 账号的 ID | 加载器脚本、每个广告位的 `data-ad-client` |
| **广告位 ID**（`data-ad-slot`） | **可以有多个** | 一个「广告单元」一个 ID | 每个 `<ins>` 广告位 |

**「是否有多个」要分开看：**

| 参数 | 单/多 | 说明 |
| --- | --- | --- |
| `client`（发布商 ID） | **单** | 一个账号一个，全站所有广告位共用同一个 |
| `slots[].slot`（广告位 ID） | **多** | 顶部、底部各放一个 → 建 2 个「广告单元」 → 拿 2 个 slot ID |
| `adsTxt` | 单文件、多行 | 只接 Google 通常 1 行；接多家联盟才多行 |

**结论：**

+ **发布商 ID 永远只配一个**，它是总开关。
+ **广告位想放几个就建几个**，数量完全由你定。
+ 多个 slot 不是「同一广告单元的多个参数」，而是**互相独立的广告单元**（各自独立报表、独立样式）。

> **BlogLoom 的设计取舍：** 后台只维护**一个配置项** `adsense`（一组 JSON），发布商 ID、广告位列表、ads.txt 全在这一个 JSON 里——**不新增表、不新增字段**。

## 2.3、投放方式：按网站、按广告单元、混合投放

在 AdSense 里，「按网站」和「按广告单元」**不是二选一**，它们可以独立使用，也可以同时使用：

| 方式 | AdSense 后台入口 | 位置由谁决定 | BlogLoom 对应配置 |
| --- | --- | --- | --- |
| **按网站（自动广告）** | Ads → By site | Google 根据页面自动选择 | `autoAds=true`，可以不配置 `slots` |
| **按广告单元（手动广告）** | Ads → By ad unit | BlogLoom 根据 `position` 精确渲染 | `autoAds=false`，配置一个或多个 `slots` |
| **混合投放** | 两处都配置 | Google 自动补位 + BlogLoom 手动位 | `autoAds=true`，同时配置多个 `slots` |

| `autoAds` | 是否存在已启用 slot | BlogLoom 是否加载全局脚本 | 实际投放方式 |
| --- | --- | --- | --- |
| `false` | 否 | 否 | 不投放 |
| `true` | 否 | 是 | 仅自动广告 |
| `false` | 是 | 是 | 仅手动广告单元 |
| `true` | 是 | 是 | 自动广告 + 手动广告单元 |

> **关键边界：** `adsbygoogle.js` 是自动广告和手动广告**共用的加载器**，脚本 URL 本身**没有** `autoAds=true/false` 参数。`autoAds` 表示 BlogLoom 的投放意图，**Google 是否真正自动插入广告，最终仍由 AdSense 后台「按网站」开关决定**。

因此需要同步完成以下操作：

1. 选择「仅自动广告」或「混合投放」时，需在 AdSense 后台 `Ads → By site` 中**开启**该站点的自动广告。
2. 选择「仅手动广告单元」时，需在 AdSense 后台**关闭**该站点的自动广告；否则即使 BlogLoom 配置 `autoAds=false`，Google 仍可能通过同一加载器自动插入广告。
3. `slots` 可以为空，也可以配置多个；每个 slot 对应一个独立广告单元。

## 2.4、ads.txt：站点级「授权售卖」声明

`ads.txt` 是 IAB Tech Lab 规范（[官方说明](https://developers.google.com/adsense/platforms/transparent/ads-txt)）：**非强制，但不放会损失部分预算**——很多广告主只对「已验证 ads.txt」的广告请求出价。文件放**域名根目录**，只接 Google 时内容一行：

```plain
google.com, pub-0000000000000000, DIRECT, f08c47fec0942fa0
```

> `pub-0000000000000000` 是发布商 ID（**去掉 `ca-` 前缀**）；`f08c47fec0942fa0` 是 Google 固定认证标识。这与本项目 `robots/sitemap/rss` 的站点级文件机制同构（见 02 篇 5.1.3）。

## 2.5、站点验证：AdSense 代码 或 meta 标签

| 方式 | 做法 | 适用 |
| --- | --- | --- |
| **AdSense 代码** | 把加载器代码放进首页 `<head>` | 首页就要展示广告的常规站 |
| **meta 标签** | 在首页 `<head>` 放一段验证 meta | **不想在首页放广告**时（官方新增） |

官方（[站点管理改进](https://support.google.com/adsense/answer/13996652)）：新增 `meta tag` 验证，**「不想在首页展示广告时，可用 meta 标签替代 AdSense 代码片段验证」**。

BlogLoom 首页是 SPA，已有 `SpaIndexController` 向 SPA 入口注入 meta 的能力，可直接沿用 meta 标签方案：

```html
<meta name="google-adsense-account" content="ca-pub-1234567890123456">
```

## 2.6、关键概念对照表

| 概念 | 一句话记忆 | BlogLoom 落点 |
| --- | --- | --- |
| 加载器 `client` | 全站总开关，Auto ads 靠它 | `<head>` 一份 |
| 广告位 `slot` | 一个手动广告单元 | `<body>` 里一段 `<ins>` |
| `autoAds` | 投放意图开关 | 后台第 2 段配置 |
| `ads.txt` | 授权售卖声明 | `/ads.txt` |
| `position` | 模板里的位置标记 | 系统内部维护，用户无感 |

---

# 三、核心功能需求分析调研

本章回答「**要做什么、为什么这么做、有哪些约束**」，是后面「配置规则」与「代码实现」的出发点。

## 3.1、需求总览（功能点清单）

| 编号 | 功能点 | 说明 | 优先级 |
| --- | --- | --- | --- |
| A1 | 广告总开关 | 一键启停全站广告，配置保留 | P0 |
| A2 | 发布商 ID 配置 | 全站唯一 `ca-pub-…` | P0 |
| A3 | 自动广告开关 | 控制「按网站」投放意图 | P0 |
| A4 | 手动广告位配置 | 粘贴 Google 代码，自动识别 slot | P0 |
| A5 | 广告位启停 | 单个广告位可单独开关 | P0 |
| A6 | SSR 文章页注入 | 加载器 + 手动广告位直出 | P0 |
| A7 | SPA 页面注入 | 首页/列表加载器 + 路由切换 push | P1 |
| A8 | ads.txt 生成 | 自动生成 / 自定义 + 验证并下载 | P0 |
| A9 | 站点验证 meta | 首页注入验证 meta（方案 B） | P1 |
| A10 | 配置解析兜底 | 配置损坏时不影响页面 | P0 |

## 3.2、后台如何承载需求：为什么是「一个 JSON + 一个开关」

需求方最容易想到的是「发布商 ID、自动广告、ads.txt、广告位」拆成四个独立配置项。但这样会带来一连串问题：**数据库要加字段、接口要加参数、增删广告位要改代码**。

于是 BlogLoom 选择**单一配置项**方案：

| 配置键（nameEn） | 中文名 | 类型 | 说明 |
| --- | --- | --- | --- |
| `adsense` | AdSense 广告配置 | JSON（字符串） | 后台唯一入口；发布商 ID、自动广告、ads.txt、广告位列表全在这一个 JSON 里 |

```json
{
  "version": 1,
  "enabled": true,
  "client": "ca-pub-1234567890123456",
  "autoAds": true,
  "adsTxt": "",
  "slots": [
    { "name": "", "slot": "1111111111", "position": "articleTop",    "enabled": true, "format": "auto", "layout": "", "fullWidth": null, "alignment": "center" },
    { "name": "", "slot": "2222222222", "position": "articleBottom", "enabled": true, "format": "auto", "layout": "", "fullWidth": null, "alignment": "center" }
  ]
}
```

**设计理由：** 后台只维护一个 JSON，前端表单本质是「一组表单 + 一段校验」，后端只当字符串存取、解析时做兜底；新增/删除位置、临时关广告、改发布商 ID 都只动这一处，**不新增表、不新增字段、不改代码**。

## 3.3、广告位规划与布局原则

广告放哪，直接决定「体验」和「收益」。参考眼动 F 型浏览路径，**放在读者自然经过的阅读动线上**，比「多塞几个格子」有效得多。

| 位置 | 说明 | BlogLoom 落点 | 建议 |
| --- | --- | --- | --- |
| **首屏之下** | 所有人进门都会经过 | 文章标题后 / 正文前 | 响应式横幅，控制高度，**别让首屏只有广告没正文** |
| **文内第一条** | 第 2~3 段之后，读者已确认「值得读」 | 正文中部 | CTR 通常最高；二阶段做 |
| **正文结束处** | 读者处于「接下来看什么」的过渡期 | 正文末尾、相关推荐前 | 矩形 / 信息流样式 |
| **桌面侧栏** | 宽屏才有，可 sticky | 文章页左右栏 | 仅桌面；移动端隐藏 |
| **页脚 / 侧栏最底** | 长期 CTR 极低还伤体验 | — | **果断砍掉** |

**布局原则：**

1. **数量克制：** 3~4 个高质量位置，通常优于 6~7 个鸡肋位。
2. **预留尺寸防 CLS：** 广告容器给 `min-height` 或 `aspect-ratio`，避免加载后正文乱跳。
3. **融入而非伪装：** 配色接近站点主题可以；**绝不能**做成「下载按钮」「播放键」或伪装成正文（见 3.5）。
4. **移动端优先：** 用自适应单元（`data-full-width-responsive="true"`），少用手动固定大尺寸撑爆首屏。

## 3.4、广告位与页面的对应关系

| 页面 | 渲染方式 | 适合的广告位 | 接入方式 |
| --- | --- | --- | --- |
| 文章详情页 `/blog/{id}` | SSR 直出 | `articleTop` / `articleBottom` / `articleSidebarToc`（`articleInArticle` 预留） | 后端模板输出 `<ins>` |
| 首页 / 列表 / 归档 | Vue SPA | `listTop`（预留） | 前端注入 + 路由 push |
| 后台 `/cms`、`/admin` | SPA | **不投** | 后台不对外开放，无需广告 |

**核心场景是文章页**：它既是收录主体，也是读者停留最久的地方；SPA 页广告属于锦上添花。

> **现状说明：** 目前实际渲染的位置为 `articleTop`、`articleBottom`（正文内），以及桌面端右栏「本文目录」下方的 `articleSidebarToc`（SSR 与 SPA 均已接入）；`articleInArticle`、`listTop` 已在**校验白名单**中预留，属于进阶项，一阶段可先不做。

## 3.5、合规与审核要点（红线）

AdSense 审核的本质是判断「**你是不是在做对用户有用的网站**」。

**① 申请前的站点条件（与 SEO 改造强相关）：**

| 要求 | BlogLoom 现状 | 对应 |
| --- | --- | --- |
| 内容可抓取（非空壳） | 已完成服务端直出 | 02 篇 5.1.1.2 |
| 有 sitemap 并提交 Search Console | 已完成 | 02 篇 5.1.3.2 |
| 有隐私政策页（含 Cookie / 第三方说明） | **需确认/补齐** | 新增 |
| 有关于 / 联系页面 | 站点已有 About | 现有 |
| 全站 HTTPS + 自有顶级域名 | 已满足（changlu.cloud） | 现有 |
| 导航清晰、无死链、移动端可用 | 基本满足 | 现有 |

> **注意：** 隐私政策页是审核里最容易漏的一项，需写明：使用了 Cookie、接入了 Google AdSense 等第三方、第三方可能基于用户兴趣投放广告。

**② 广告实现红线（官方 [广告投放政策](https://support.google.com/adsense/answer/1346295)）：**

| 红线 | 说明 | 对 BlogLoom 的约束 |
| --- | --- | --- |
| 诱导点击 | 禁止「点广告支持本站」等文案或布局 | 广告旁不放诱导性文字/箭头 |
| 伪装成内容 | 禁止做成导航、下载按钮、正文样式 | 广告位有清晰边界与留白 |
| 误导性标题 | 禁止用「资源」「有用链接」等标题盖在广告上 | 广告上方不放大标题 |
| 未经请求的自动刷新 | 禁止定时器自动刷新广告「刷曝光」 | SPA 只允许「用户切路由后 push 新广告位」 |
| 图片与广告对齐 | 禁止让站内图片像广告配图 | 广告与正文图片保持视觉区隔 |
| 放在登录墙后 | Google 无法评估的内容不投放 | 私密/密码文章页**不投广告**（与 sitemap 排除一致） |

**③ 内容质量（决定能不能过审）：**

+ **原创、垂直、有信息量**：同一细分领域 8~15 篇能解决具体问题的原创长文，比「50 篇模板短文」更稳。
+ **不是机翻/洗稿**：多平台搬运、机翻当原创，属于高危重复内容。
+ **有稳定更新节奏**：几周内持续更新，比「同一天批量发完」更像认真运营。
+ **被拒后**：读懂拒信关键词 → 做**可见的整改**（补隐私政策、删违规、修 404、改导航）→ **等 7~14 天**再申请，**不要连续刷申请**。

## 3.6、收益与指标：看什么、怎么算

| 指标 | 全称 | 含义 | 用途 |
| --- | --- | --- | --- |
| **Page RPM** | 每千次网页浏览收益 | 1000 次页面浏览能赚多少 | 衡量页面整体变现效率 |
| **Impressions RPM** | 每千次广告展示收益 | 1000 次广告展示能赚多少 | 衡量广告位本身质量 |
| **CTR** | 点击率 | 点击 ÷ 展示 | 判断位置/样式是否「被看到」 |
| **CPC** | 每次点击成本 | 广告主为一次点击付的钱 | 受内容主题（技术类偏高）影响 |

**经验参考（非承诺）：** 内容站 CTR 常见 1~3%，长期低于 0.5% 通常意味着位置或匹配有问题；RPM 波动大，**看周趋势而非单日**。

**结算门槛：** 账户余额累计满 **100 美元**才触发支付；需完成身份验证（PIN 码）与收款方式（国内电汇需 SWIFT/BIC）。

> **重要：广告费的主要变量不是「广告位数量」，而是「内容主题 × 流量质量 × 页面浏览深度」。** 技术博客 CPC 通常不低，把性能与收录做扎实、让文章被更多人看到，比多塞几个广告位更划算。

## 3.7、常见问题排查（广告不显示）

| 现象 | 可能原因 | 排查/解决 |
| --- | --- | --- |
| 源码里有 `<ins>`，但页面空白 | 加载器没注入成功 / 顺序不对 | 确认 `<head>` 有 `adsbygoogle.js`；加载器必须在 `<ins>` 之前 |
| 有加载器，但广告不出现 | 账号/站点还在审核中 | Sites 状态是否 Ready；审核通过前不投放 |
| 只显示一次，切路由后消失 | SPA 路由切换后没重新 push | 见 5.6 的 `pushAdsense`；确认是「新的 `<ins>`」 |
| 控制台报 `All ins elements already have ads` | 同一 `<ins>` 被 push 多次 | 每个元素只 push 一次；代码已用 `data-pushed` 去重 |
| 控制台报 `No slot size` | 父容器宽度为 0 或未定义 | 给广告容器固定/最小宽度 |
| 移动端撑破布局 | 自适应父容器宽度不稳 | 用 `data-full-width-responsive="true"` + 容器 `width:100%` |
| 本机看到广告、线上看不到 | 广告拦截插件 / 地区无库存 | 换无插件浏览器、清缓存；部分地区库存本就少 |
| ads.txt 一直 Not found | 路径/内容/缓存问题 | `curl` 确认根目录可访问；内容格式一行；点 Check for updates |
| 被拒：「内容不足 / 低价值」 | 篇数或质量不够 | 补垂直原创长文、清理模板文，7~14 天后再申 |

> **调试：** 浏览器 **Network** 过滤 `adsbygoogle` 看 SDK 是否加载；**Console** 看 `push()` 报错。广告竞价有延迟（通常几秒内），刚打开空白属正常。

---

# 四、功能需求与 UI 配置规则

本章是**开发与验收的统一口径**：把「后台长什么样、每个字段什么规则、开关怎么组合、粘贴代码怎么识别」写成明确规则。

## 4.1、配置总模型

后台配置最终落成一份 JSON，字段定义如下：

| 字段 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `version` | int | 1 | 配置版本号（当前保留，便于后续兼容） |
| `enabled` | boolean | `false` | **总开关**；关闭后前台不加载、不渲染，配置保留 |
| `client` | string | `""` | 发布商 ID，格式 `ca-pub-数字`，**全站唯一** |
| `autoAds` | boolean | `false` | 自动广告投放意图；需与 AdSense 后台「按网站」一致 |
| `adsTxt` | string | `""` | 自定义 ads.txt 内容；留空则按 `client` 自动生成 |
| `slots` | array | `[]` | 广告位列表，每项对应一个手动广告单元 |

广告位对象 `AdSlot`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `name` | string | 备注名（可选，便于识别） |
| `slot` | string | 广告位 ID，**纯数字**，来自 Google 代码 `data-ad-slot` |
| `position` | string | 技术位置，**系统内部维护**，用户不直接编辑 |
| `enabled` | boolean | 单个广告位启停 |
| `format` | string | `auto` / `fluid` / `rectangle` / `horizontal` / `vertical`，默认 `auto` |
| `layout` | string | `data-ad-layout`，如 `in-article` |
| `fullWidth` | boolean | `data-full-width-responsive`，可空 |
| `alignment` | string | `left` / `center` / `right`，默认 `center` |

## 4.2、后台四段式表单（UI 结构）

后台页面：**SEO优化 → 广告平台关联**，组件 `AdvertisingPlatform.vue`。整体由「一个总开关 + 四个编号区块」组成：

```plain
┌───────────────────────────────────────────────────────────────┐
│ 广告平台关联                    [保存配置]                       │
├───────────────────────────────────────────────────────────────┤
│ 启用 AdSense                                          [开关]    │  ← 总开关
│                                                                │
│ ① 发布商配置                                                   │
│    发布商 ID        [ ca-pub-1234567890123456            ]      │
│                                                                │
│ ② 自动广告                                                     │
│    开启自动广告     [开关]（总开关关闭时禁用）                   │
│                                                                │
│ ③ ads.txt 配置                              [验证并下载]         │
│    ads.txt 内容     [ 文本域，可留空自动生成            ]        │
│                                                                │
│ ④ 广告位配置                                [新增广告位]         │
│    ┌ 广告位 1   [启用开关] [删除] ─────────────────────────┐   │
│    │ 投放位置     [ 下拉选择：文章详情·标题下方 ▾       ]  │   │
│    │ Google 代码  [ 粘贴 Google 广告单元代码            ]  │   │
│    │ （失焦自动解析）已识别：slot=… format=… layout=…     │   │
│    └──────────────────────────────────────────────────────┘   │
│    新增广告位会按顺序自动分配默认位置，可在广告位内改选。        │
└───────────────────────────────────────────────────────────────┘
```

**四段与需求映射：**

| 区块 | 标题 | 承载字段 | 需求编号 |
| --- | --- | --- | --- |
| 总开关 | 启用 AdSense | `enabled` | A1 |
| ① | 发布商配置 | `client` | A2 |
| ② | 自动广告 | `autoAds` | A3 |
| ③ | ads.txt 配置 | `adsTxt` + 验证并下载 | A8 |
| ④ | 广告位配置 | `slots[]` | A4 / A5 |

## 4.3、字段与规则明细（逐字段）

| 字段 | 必填 | 前端规则 | 后端规则 | 提示文案 |
| --- | --- | --- | --- | --- |
| `client` | 启用广告时必填 | 正则 `^ca-pub-\d+$` | 正则 `^ca-pub-\d+$`，空串放行 | 「发布商 ID 格式应为 ca-pub-数字」 |
| `autoAds` | 否 | 总开关关闭时禁用 | 总开关为 `true` 且 `autoAds=true` 时要求 `client` 非空 | — |
| `adsTxt` | 否 | 无 | 无 | 留空自动生成；填写后原样输出 |
| `slots[].slot` | 启用时必填 | 正则 `^\d+$` | 正则 `^\d+$` | 「slot 必须为数字」 |
| `slots[].position` | 是 | 下拉选择（新增时自动分配默认值），不可重复 | 必须在白名单内，不可重复 | 「请为每个广告位选择位置」/「广告位置不能重复」 |
| `slots[].format` | 否 | 默认 `auto` | 白名单：`auto/fluid/autorelaxed/rectangle/horizontal/vertical` | 「不支持的广告格式」 |
| `slots[].alignment` | 否 | 默认 `center` | 白名单：`left/center/right` | 「不支持的对齐方式」 |

**位置白名单（后端单一数据源，经 `GET /admin/seo/adsense/positions` 下发）：**

| position | 中文名 | 渲染点 | 适合广告 |
| --- | --- | --- | --- |
| `articleTop` | 文章详情·标题下方 | 文章头之后、正文前 | 展示广告（横幅） |
| `articleInArticle` | 文章详情·正文内嵌 | 正文段落之间（进阶，暂未渲染） | 文章内嵌 |
| `articleBottom` | 文章详情·正文结束后 | 正文末尾、标签前 | 展示 / 多重广告 |
| `articleSidebarToc` | 文章详情·右侧目录下方 | 右栏「本文目录」下方（桌面端） | **纵向广告** |
| `listTop` | 列表页·顶部 | 列表页顶部（预留） | 信息流 |

**保存前置校验（前端 `validate`，与后端一致）：**

1. `client` 非空时必须匹配 `^ca-pub-\d+$`；
2. 若「启用 + （自动广告 或 至少一个启用的广告位）」，则 `client` 不能为空 → 否则提示「启用自动广告或手动广告位前请填写发布商 ID」；
3. 每个广告位必须有 `position`；
4. `position` 不能重复；
5. 启用的广告位必须能成功解析出 `slot`。

> **重点：** 前端校验与后端 `AdsenseConfigController.validate()` **规则同构**，后端做最终兜底——即使绕过前端，非法配置也会被拒绝，不会写库。

## 4.4、开关组合行为矩阵

这是配置逻辑的核心，验收时可直接对照：

| `enabled` | `client` | `autoAds` | 是否存在启用 slot | `<head>` 加载器 | `<body>` 手动位 | `/ads.txt` |
| --- | --- | --- | --- | --- | --- | --- |
| `false` | 任意 | 任意 | 任意 | **不注入** | **不渲染** | **空** |
| `true` | 空 | 任意 | 任意 | 不注入 | 不渲染 | 空（保存时通常已被校验拦截） |
| `true` | 非空 | `true` | 否 | **注入** | 不渲染 | 自动生成一行 |
| `true` | 非空 | `true` | 是 | **注入** | **渲染** | 自动生成一行 |
| `true` | 非空 | `false` | 是 | **注入** | **渲染** | 自动生成一行 |
| `true` | 非空 | `false` | 否 | 不注入 | 不渲染 | 自动生成一行 |

**加载器注入条件（三条件同时满足）：**

```plain
enabled != false
并且 client 非空
并且（autoAds == true 或 至少存在一个 enabled != false 且 slot 非空的广告位）
```

> **注意 `enabled` 的空值语义：** 后端多处以 `Boolean.FALSE.equals(enabled)` 判断——即 `enabled == null` 视为**启用**，只有显式 `false` 才关闭。前端初始值固定为 `false`，避免误开。

## 4.5、广告位代码解析规则（粘贴即识别）

用户在「④ 广告位配置」只需**粘贴 Google 提供的完整广告单元代码**，系统在 `blur`（失焦）时自动解析，**不会原样执行粘贴的脚本**。

**解析流程（`parseSlotCode`）：**

```plain
粘贴代码
  ↓ DOMParser 解析为 DOM
查找 <ins class="adsbygoogle">
  ├─ 找不到 → 提示「未识别到 <ins class="adsbygoogle"> 广告单元」
  ↓
提取 client：
  ├─ 优先取 ins 的 data-ad-client
  └─ 找不到 → 从 <script src="..adsbygoogle.js?client=..."> 的查询参数取
校验 client ^ca-pub-\d+$ 且 slot ^\d+$
  ├─ 不通过 → 提示「代码中的 client 或 slot 格式不正确」
  ↓
与全局 client 比对：
  ├─ 全局已填且不一致 → 提示「广告位的发布商 ID 与全局配置不一致」
  └─ 全局为空 → 用代码里的 client 自动回填
  ↓
写入字段：
  slot      = data-ad-slot
  format    = data-ad-format（默认 auto，含 autorelaxed）
  layout    = data-ad-layout（默认空）
  layoutKey = data-ad-layout-key（信息流广告的布局 key，默认空）
  fullWidth = data-full-width-responsive（缺省 null，否则 true/false）
  alignment = 从 style 的 text-align 解析（默认 center）
```

**解析结果反馈：** 页面显示 `已识别：slot=… format=… layout=…`，让用户即时确认。

| 提取字段 | 来源属性 | 缺省 |
| --- | --- | --- |
| `client` | `data-ad-client` 或加载器 `src?client=` | 全局回填 |
| `slot` | `data-ad-slot` | 无（必填） |
| `format` | `data-ad-format` | `auto` |
| `layout` | `data-ad-layout` | `""` |
| `layoutKey` | `data-ad-layout-key` | `""` |
| `fullWidth` | `data-full-width-responsive` | `null` |
| `alignment` | `ins.style` 的 `text-align` | `center` |

> **设计理由：** 用户面对的是 AdSense 后台复制出来的**完整代码**，不需要理解 `slot/format/layout/alignment` 分别是什么。系统替他解析成结构化字段，既能精确渲染，又不会执行来路不明的脚本——**安全与易用兼得**。

## 4.6、位置选择规则

位置不再「用户看不到、选不到」，而是**后台可下拉选择**；新增广告位时系统仍会按顺序给一个默认位置，用户可以改。

**位置元数据由后端统一维护**，通过 `GET /admin/seo/adsense/positions` 下发给后台下拉，保证「前端选项 = 后端白名单」，不会出现「后台能选、后端拒绝」的错位。

| position | 中文名 | 渲染点 | 适合广告 |
| --- | --- | --- | --- |
| `articleTop` | 文章详情·标题下方 | 文章头之后、正文前 | 展示广告（横幅） |
| `articleInArticle` | 文章详情·正文内嵌 | 正文段落之间（进阶，暂未渲染） | 文章内嵌 |
| `articleBottom` | 文章详情·正文结束后 | 正文末尾、标签前 | 展示 / 多重广告 |
| `articleSidebarToc` | 文章详情·右侧目录下方 | 右栏「本文目录」下方（**仅桌面端**） | **纵向广告** |
| `listTop` | 列表页·顶部 | 列表页顶部（预留） | 信息流 |

**规则：**

1. 点击「新增广告位」时，从 `['articleTop','articleBottom','articleInArticle','articleSidebarToc','listTop']` 取**第一个尚未使用**的位置作为默认值；
2. 最多 **5 个**广告位；全部用完后提示「已达到当前支持的广告位数量上限」；
3. 每个广告位可在「投放位置」下拉中**改为任意未占用的位置**；
4. 删除某个广告位后，该位置释放，可再次被分配；
5. 位置**不可重复**，前端与后端双重校验。

> **注意：** `articleSidebarToc` 只在**桌面端右栏**渲染（移动端右栏整体隐藏），建议配合 `vertical`（纵向）或 `autorelaxed`（多重）形态的广告单元。
>
> **设计说明：** 位置选择是「有约束的自由」——用户能选，但选项是固定的、且与后端白名单同源；这样既满足「在目录下方挂纵向广告」这类精确需求，又不会因为乱填位置导致渲染不到。

## 4.7、ads.txt「验证并下载」规则

「③ ads.txt 配置」右侧的「**验证并下载**」按钮，逻辑如下：

```plain
点击「验证并下载」
  ↓ GET /admin/seo/file/ads（responseType: blob）
  ↓ 后端按 ads.txt 规则生成内容
判断返回内容：
  ├─ 内容为空 → 提示「ads.txt 文件为空，请先配置发布商 ID 或 ads.txt 内容」
  └─ 内容非空 → 浏览器下载文件名 ads.txt + 提示「ads.txt 验证成功，已开始下载」
```

**后端生成规则（`SeoFileServiceImpl.adsTxt()`）：**

| 条件 | 生成结果 |
| --- | --- |
| 总开关 `enabled=false` | 返回空 |
| `adsTxt` 自定义非空 | **原样输出**（自动补尾换行） |
| `adsTxt` 为空且 `client` 非空 | `google.com, pub-XXXX, DIRECT, f08c47fec0942fa0`（去掉 `ca-` 前缀） |
| `adsTxt` 为空且 `client` 为空 | 返回空 |

> **注意：** 「验证并下载」是**给你本地核对 + 上传托管环境**用的。若站点本身由 BlogLoom 提供 `/ads.txt` 端点，内容会随配置自动生效，无需手动上传；按钮的价值在于「所见即所得」地确认格式正确。

## 4.8、前后端一致性规则

为保证「后台所见 = 前台所得」，两端共用同一套语义：

| 规则 | 后端 | 前端（view / cms） |
| --- | --- | --- |
| `enabled == null` 视为启用 | `Boolean.FALSE.equals(enabled)` | `config.enabled === false` 才关闭 |
| 加载器注入条件 | `getAdsenseClient()` | `initAdsense()` |
| slot 过滤 | `getAdsenseSlotsByPosition()` | `AdsenseSlot` 的 `slot` 计算属性 |
| push 去重 | — | `data-pushed` / `dataset.pushed` |
| 保存校验 | `AdsenseConfigController.validate()` | `AdvertisingPlatform.vue` 的 `validate()` |

> **重点：** 后端 `/site` 接口把 `adsense` 作为**已解析的对象**返回（而非字符串），前端无需再 `JSON.parse`，两端天然共享同一份配置。

---

# 五、核心代码设计实现

本章讲「**代码长什么样、数据怎么流**」。整体分后端与前端两部分。

## 5.1、总体结构

```plain
配置入口（后台四段式表单）
        ↓ GET/POST /admin/seo/adsense
AdsenseConfigController（校验）
        ↓
site_setting.adsense（一个 JSON 字符串）
        ↓ getAdsenseConfig() 解析
┌───────────────┬───────────────┬────────────────────┐
│ SSR 模板       │ /site 接口     │ SeoFileService      │
│ seo-head.html │ → SPA 注入     │ → /ads.txt          │
│ seo/blog.html │ initAdsense()  │ → /admin/seo/file/ads│
└───────────────┴───────────────┴────────────────────┘
```

## 5.2、数据模型与存储

**常量（`SiteSettingConstants.java`）：**

```java
/** Google AdSense 广告配置（JSON） */
public static final String ADSENSE = "adsense";
```

**配置载体（`module/seo/domain/AdsenseConfig.java`）：**

```java
@Getter @Setter
public class AdsenseConfig {
    private Integer version;
    private Boolean enabled;          // 总开关
    private String client;            // ca-pub-…
    private Boolean autoAds;          // 自动广告投放意图
    private String adsTxt;            // 可选，自定义 ads.txt
    private List<AdSlot> slots = new ArrayList<>();
}
```

**广告位（`module/seo/domain/AdSlot.java`）：**

```java
@Getter @Setter
public class AdSlot {
    private String name;        // 备注名
    private String slot;        // data-ad-slot
    private String position;    // articleTop / articleBottom / articleSidebarToc / …（后台可选）
    private Boolean enabled;
    private String format;      // auto / fluid / rectangle / horizontal / vertical
    private String layout;      // data-ad-layout
    private String layoutKey;   // data-ad-layout-key（信息流布局 key）
    private Boolean fullWidth;  // data-full-width-responsive
    private String alignment;   // left / center / right
}
```

**存储：** 复用 `site_setting` 表（`name_en='adsense'`、`type=1`、`value=<JSON>`），**无需建表、无需新增字段**；该行在首次保存时由 `upsertBasicSetting` 自动创建。

**解析兜底（关键）：** `getAdsenseConfig()` 必须容错——JSON 为空、格式错、`client` 为空时都返回「空配置」，这样下游「**拿不到就不输出**」：

```java
@Override
public AdsenseConfig getAdsenseConfig() {
    String json = settingValue(SiteSettingConstants.ADSENSE);
    if (json.isEmpty()) return new AdsenseConfig();
    AdsenseConfig config = JacksonUtils.readValue(json, AdsenseConfig.class);
    return config == null ? new AdsenseConfig() : config;  // 损坏 = 不注入，绝不中断页面
}
```

**读取语义（`SiteSettingServiceImpl`）：**

```java
@Override
public String getAdsenseClient() {
    AdsenseConfig config = getAdsenseConfig();
    if (Boolean.FALSE.equals(config.getEnabled())) return "";
    boolean hasEnabledSlot = config.getSlots() != null && config.getSlots().stream()
        .anyMatch(item -> item != null && !Boolean.FALSE.equals(item.getEnabled())
            && item.getSlot() != null && !item.getSlot().trim().isEmpty());
    if (!Boolean.TRUE.equals(config.getAutoAds()) && !hasEnabledSlot) return "";
    return config.getClient() == null ? "" : config.getClient().trim();
}

@Override
public Map<String, AdSlot> getAdsenseSlotsByPosition() {
    Map<String, AdSlot> result = new LinkedHashMap<>();
    AdsenseConfig config = getAdsenseConfig();
    if (Boolean.FALSE.equals(config.getEnabled()) || config.getSlots() == null) return result;
    for (AdSlot item : config.getSlots()) {
        if (item == null || Boolean.FALSE.equals(item.getEnabled())) continue;
        String position = item.getPosition() == null ? "" : item.getPosition().trim();
        String slot = item.getSlot() == null ? "" : item.getSlot().trim();
        if (!position.isEmpty() && !slot.isEmpty()) result.put(position, item);
    }
    return result;
}
```

## 5.3、后端装配：SeoMetaBuilder

每个 SSR 页面模型都会从 `SeoMetaBuilder.baseMeta()` 拿到广告信息：

```java
meta.setAdsenseClient(siteSettingService.getAdsenseClient());
Map<String, AdSlot> adsenseSlots = siteSettingService.getAdsenseSlotsByPosition();
if (adsenseSlots != null) meta.setAdsenseSlots(adsenseSlots);
```

`SeoMeta` 对应字段：

```java
private String adsenseClient;                              // 空则不注入加载器
private Map<String, AdSlot> adsenseSlots = new LinkedHashMap<>(); // 位置 → AdSlot
```

> **只读一次：** 装配时读一次配置，把 `client` 作为加载器开关、把过滤后的 `slots` 转成 `position → AdSlot` 的 map；模板只消费这个 map，**不感知原始 JSON**。

## 5.4、后台接口：AdsenseConfigController

**路由：** `/admin/seo/adsense`

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| GET | `/admin/seo/adsense` | 返回当前配置对象 |
| POST | `/admin/seo/adsense` | 校验并保存配置 |

**白名单与校验：**

```java
POSITIONS  = { articleTop, articleInArticle, articleBottom, articleSidebarToc, listTop }
FORMATS    = { auto, fluid, autorelaxed, rectangle, horizontal, vertical }
ALIGNMENTS = { left, center, right }
```

```java
private String validate(AdsenseConfig config) {
    if (config == null) return "广告配置不能为空";
    String client = trim(config.getClient());
    if (!client.isEmpty() && !client.matches("^ca-pub-\\d+$"))
        return "client 格式应为 ca-pub-数字";
    Set<String> positions = new HashSet<>();
    boolean hasEnabledSlot = false;
    if (config.getSlots() == null) {
        if (Boolean.TRUE.equals(config.getEnabled()) && Boolean.TRUE.equals(config.getAutoAds()) && client.isEmpty())
            return "启用自动广告前请填写发布商 ID";
        return null;
    }
    for (AdSlot item : config.getSlots()) {
        if (item == null) return "slots 中不能包含空项";
        String position = trim(item.getPosition());
        String slot = trim(item.getSlot());
        if (!POSITIONS.contains(position)) return "不支持的广告位置：" + position;
        if (!positions.add(position))       return "广告位置不能重复：" + position;
        if (slot.isEmpty() && (Boolean.FALSE.equals(config.getEnabled()) || Boolean.FALSE.equals(item.getEnabled()))) continue;
        if (!slot.matches("^\\d+$"))        return "slot 必须为数字：" + position;
        String format = trim(item.getFormat());
        if (!format.isEmpty() && !FORMATS.contains(format)) return "不支持的广告格式：" + format;
        String alignment = trim(item.getAlignment());
        if (!alignment.isEmpty() && !ALIGNMENTS.contains(alignment)) return "不支持的对齐方式：" + alignment;
        if (!Boolean.FALSE.equals(item.getEnabled())) hasEnabledSlot = true;
    }
    if (Boolean.TRUE.equals(config.getEnabled()) && (Boolean.TRUE.equals(config.getAutoAds()) || hasEnabledSlot)
            && client.isEmpty()) return "启用广告前请填写发布商 ID";
    return null;
}
```

**保存（`saveAdsenseConfig`）：** `upsertBasicSetting(ADSENSE, "AdSense 广告配置", json)` → `deleteSiteInfoCache()`，保证下一次 `/site` 与页面渲染立即生效。

## 5.5、SSR 页面注入（Thymeleaf）

**① 加载器注入公共 head**（`templates/seo/fragments/seo-head.html`，配了 ID 才输出）：

```html
<script th:if="${seo.adsenseClient != null and !seo.adsenseClient.isEmpty()}" async
        th:src="'https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=' + ${seo.adsenseClient}"
        crossorigin="anonymous"></script>
```

**② 文章模板按位置渲染广告位**（`templates/seo/blog.html`，取不到 slot 就不渲染）：

```html
<!-- articleTop -->
<div class="article-ad" th:if="${seo.adsenseClient != null and !seo.adsenseClient.isEmpty() and seo.adsenseSlots['articleTop'] != null}">
  <ins class="adsbygoogle"
       th:style="'display:block;text-align:' + (${seo.adsenseSlots['articleTop'].alignment} ?: 'center')"
       th:attr="data-ad-client=${seo.adsenseClient},
                data-ad-slot=${seo.adsenseSlots['articleTop'].slot},
                data-ad-format=${seo.adsenseSlots['articleTop'].format} ?: 'auto',
                data-ad-layout=${seo.adsenseSlots['articleTop'].layout},
                data-full-width-responsive=${seo.adsenseSlots['articleTop'].fullWidth}"></ins>
  <script>(adsbygoogle = window.adsbygoogle || []).push({});</script>
</div>
```

`articleBottom` 同构。**注意：** `th:if` 里先判断 `client` 非空 **且** 该位置有 slot，二者缺一不渲染——这正是「**取不到就不输出**」的兜底。

**右栏位（`articleSidebarToc`）** 渲染在 `seo/blog.html` 的右侧目录 `<aside class="article-toc-column">` 内、「本文目录」盒子**之后**：

```html
<!-- 右侧目录之后：纵向广告位 -->
<div class="article-ad article-ad-sidebar" th:if="${... and seo.adsenseSlots['articleSidebarToc'] != null}">
  <ins class="adsbygoogle" th:attr="data-ad-client=${seo.adsenseClient},data-ad-slot=${seo.adsenseSlots['articleSidebarToc'].slot},…"></ins>
  <script>(adsbygoogle = window.adsbygoogle || []).push({});</script>
</div>
```

> 该位只在桌面端可见（整列 `m-mobile-hide`），建议配 `format=vertical`（纵向展示）或 `autorelaxed`（多重）广告单元。

## 5.6、SPA 页面注入（Vue）

**① 加载器工具（`blog-view-ui/src/util/adsense.js`）：**

```js
export function initAdsense(config) {
    if (!config || config.enabled === false || !config.client) return
    const hasEnabledSlot = (config.slots || []).some(item => item && item.enabled !== false && item.slot)
    if (config.autoAds !== true && !hasEnabledSlot) return
    window.__adsenseClient = config.client
    if (document.querySelector('script[data-blogloom-adsense], script[src*="pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"]')) return
    const script = document.createElement('script')
    script.async = true
    script.crossOrigin = 'anonymous'
    script.dataset.blogloomAdsense = 'true'
    script.src = `https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${encodeURIComponent(config.client)}`
    document.head.appendChild(script)
}

export function pushAdsense(element) {
    if (!window.__adsenseClient || !element || element.dataset.pushed) return
    element.dataset.pushed = 'true'
    try { (window.adsbygoogle = window.adsbygoogle || []).push({}) } catch (e) { /* 被拦截或未就绪时不影响正文阅读 */ }
}
```

**② 注入时机（`Index.vue`）：** 拉取 `/site` 后调用 `initAdsense(this.siteInfo.adsense)`，配置一经保存即动态生效，**无需重新构建前端**。

**③ 广告位组件（`AdsenseSlot.vue`）：** 按 `position` 从 `slots` 中找到启用的广告位再渲染 `<ins>`，`mounted`/`watch` 时调用 `pushAdsense`。

**④ 用于文章页（`Blog.vue`）：**

```html
<AdsenseSlot :config="adsenseConfig" position="articleTop"/>
...
<AdsenseSlot :config="adsenseConfig" position="articleBottom"/>
```

**右栏位** 在布局组件 `Index.vue` 的右侧目录之后、仅文章页渲染：

```html
<Tocbot v-if="$route.name === 'blog'"/>
<AdsenseSlot v-if="$route.name === 'blog'" :config="siteInfo.adsense || {}" position="articleSidebarToc"/>
```

**⑤ SPA 根 HTML 兜底（`SpaIndexController`）：** 当 `getAdsenseClient()` 非空时，服务端还会往 SPA 入口 `<head>` 注入一次加载器，避免 JS 尚未执行时缺少脚本。

> **合规底线：** 只允许「用户切换路由后 push 新的广告位」；代码已用 `data-pushed` 去重，**禁止定时器反复刷同一广告位**，否则违反 3.5 的自动刷新政策。

## 5.7、ads.txt 站点级文件

**对外端点（`SeoFileController`）：**

```java
@GetMapping(value = "/ads.txt", produces = "text/plain;charset=UTF-8")
public String adsTxt() { return seoFileService.adsTxt(); }
```

**生成逻辑（`SeoFileServiceImpl`）：**

```java
@Override
public String adsTxt() {
    AdsenseConfig config = siteSettingService.getAdsenseConfig();
    if (Boolean.FALSE.equals(config.getEnabled())) return "";
    String custom = config.getAdsTxt() == null ? "" : config.getAdsTxt().trim();
    if (!custom.isEmpty()) return custom.endsWith("\n") ? custom : custom + "\n";
    String client = config.getClient() == null ? "" : config.getClient().trim();
    if (client.isEmpty()) return "";
    return "google.com, " + client.replaceFirst("^ca-", "") + ", DIRECT, f08c47fec0942fa0\n";
}
```

**后台查看 / 下载（`SeoConfigController`）：** `contentOf("ads")` → `seoFileService.adsTxt()`；`fileNameOf("ads")` → `ads.txt`；`contentTypeOf("ads")` → `text/plain`。前端「验证并下载」即调用其中的下载端点。

## 5.8、配置 → 成品对照（文章页）

**后台配置（唯一入口 `site_setting.adsense`）：**

```json
{
  "version": 1,
  "enabled": true,
  "client": "ca-pub-9876543210987654",
  "autoAds": true,
  "adsTxt": "",
  "slots": [
    { "slot": "1111111111", "position": "articleTop",    "enabled": true, "format": "auto", "layout": "", "fullWidth": null, "alignment": "center" },
    { "slot": "2222222222", "position": "articleBottom", "enabled": true, "format": "auto", "layout": "", "fullWidth": null, "alignment": "center" }
  ]
}
```

**后端组装：**

```plain
getAdsenseClient()        → "ca-pub-9876543210987654"
getAdsenseSlotsByPosition() → { articleTop: 1111111111, articleBottom: 2222222222 }
```

**渲染出的 `/blog/513` HTML（精简）：**

```html
<head>
  <title>Java基础学习笔记 09、IO流 - BlogLoom</title>
  <link rel="canonical" href="https://changlu.cloud/blog/513">
  <script async
    src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-9876543210987654"
    crossorigin="anonymous"></script>
</head>
<body>
  <div class="article-ad">
    <ins class="adsbygoogle" style="display:block;text-align:center"
         data-ad-client="ca-pub-9876543210987654" data-ad-slot="1111111111"
         data-ad-format="auto" data-full-width-responsive="null"></ins>
    <script>(adsbygoogle = window.adsbygoogle || []).push({});</script>
  </div>
  <article>
    <h1>Java基础学习笔记 09、IO流</h1>
    <div class="content">……完整正文……</div>
    <div class="article-ad">
      <ins class="adsbygoogle" style="display:block;text-align:center"
           data-ad-client="ca-pub-9876543210987654" data-ad-slot="2222222222"
           data-ad-format="auto" data-full-width-responsive="null"></ins>
      <script>(adsbygoogle = window.adsbygoogle || []).push({});</script>
    </div>
  </article>
</body>
```

**页面实际看到的样子：**

```plain
┌──────────────────────────────────────────────────────────────┐
│  BlogLoom  首页  归档  标签  分类                      🔍       │
├──────────────────────────────────────────────────────────────┤
│              Java基础学习笔记 09、IO流                        │
│   ┌──────────────────────────────────────────────────────┐   │
│   │           广告位 articleTop / slot 1111111111         │   │  ← 标题下横幅
│   └──────────────────────────────────────────────────────┘   │
│   从 File 类入手……（正文段落）……                               │
│   ┌──────────────────────────────────────────────────────┐   │
│   │        广告位 articleBottom / slot 2222222222         │   │  ← 正文结束、推荐前
│   └──────────────────────────────────────────────────────┘   │
│   #Java  #IO流                                               │
└──────────────────────────────────────────────────────────────┘
```

| 后台 JSON 字段 | 生成/展示位置 | 页面上看到 |
| --- | --- | --- |
| `client = ca-pub-9876…` | `<head>` 的 `adsbygoogle.js` | 全页广告能加载 |
| `slots[].position=articleTop` / `slot=1111…` | `<h1>` 之后、正文之前 | 标题下横幅 |
| `slots[].position=articleBottom` / `slot=2222…` | 正文末尾、推荐前 | 结尾信息流 |
| `adsTxt` 留空 | `/ads.txt` | 一行 `google.com, pub-9876543210987654, DIRECT, …` |

**变体：**

+ 只配 `articleTop` → 页面只有标题下一个广告位，其它位置不渲染（`th:if` 兜底）。
+ 某位 `enabled:false` → 该位在 `getAdsenseSlotsByPosition()` 组装时被过滤，**源码里不出现**。
+ `client` 留空（或整段 JSON 清空）→ `<head>` 无加载器、所有广告位不渲染、`ads.txt` 为空 = **全站关广告**，无需改代码。

---

# 六、实战配置步骤

> **本章顺序：** 先规划「自动 / 手动怎么挂」，再按真实落地顺序走：**① AdSense 开启自动广告 → ② 配置并验证 ads.txt → ③ 创建手动广告单元（展示广告·纵向）→ ④ 回到平台完成广告位配置**，最后看结算与监控。文中已预留截图位，可直接补齐。

## 6.1、投放策略规划：自动广告与手动广告怎么挂

正式配置前，先想清楚一个问题：**自动广告和手动广告，到底怎么搭配，既不浪费收益，又不打扰阅读？**

### 6.1.1、先明确一个底线：阅读优先

技术博客的读者是来「读内容」的，广告一旦压过内容，掉的是留存，最终也会掉收益。因此先立三条底线：

1. **首屏留白给内容**：标题下第一个广告位不得挤占首屏正文，保证「进门先看到文章」。
2. **阅读动线上克制**：一篇文章最多 **2~3 个**手动位，够用即止，宁缺毋滥。
3. **防跳动（CLS）**：广告容器预留 `min-height`（BlogLoom 默认 90px / 移动端 72px），避免广告加载后正文乱跳。

### 6.1.2、自动 vs 手动：怎么分工

| 维度 | 纯自动（Auto ads） | 纯手动（按广告单元） | 混合（推荐） |
| --- | --- | --- | --- |
| 位置控制 | Google 决定，不可控 | 完全可控 | 关键位可控，其余交由 Google |
| 起步成本 | 极低 | 需建单元、配 slot | 低（1 个手动位即可） |
| 对阅读的影响 | 位置不可控，有风险 | 自己把关，风险低 | 高价值位把关，其余兜底 |
| 收益上限 | 中 | 高（精准位 CTR 高） | 高 |

**核心判断：** 文章页既是收录主体，也是读者停留最久的地方，值得用手动位守好「标题下 + 正文结束」两个高价值动线；其余位置交给 Auto ads 兜底。**这样既拿到覆盖，又保住可控与阅读体验。**

### 6.1.3、分阶段挂载

| 阶段 | 后台配置 | AdSense 后台 | 效果 |
| --- | --- | --- | --- |
| **一阶段（起步）** | `autoAds=true` + 仅 `articleTop` | 开启「按网站」自动广告 | 1 个手动位 + Google 自动补位，几乎零调优 |
| **二阶段（有数据）** | 加 `articleBottom` | 维持自动广告 | 守好首尾两条动线，按 CTR 决定是否继续加 |
| **三阶段（精细化）** | 按报表调整/关闭某位，必要时关 `autoAds` | 如需纯手动则关「按网站」 | 纯手动控版，只留高 CTR 位 |

> **注意：** 新增时会依次给默认位置：`articleTop → articleBottom → articleInArticle → articleSidebarToc → listTop`，最多 5 个；可在每个广告位的「投放位置」下拉里改选。起步**只加 1 个**，别急着填满。

### 6.1.4、不影响阅读的三条红线

1. **意图一致**：`autoAds=true` 时，AdSense 后台「按网站」必须同步开启；配置只表示意图，最终由 Google 裁决。
2. **位置不重叠**：手动位已覆盖的位置，不要再依赖 Auto ads 在同区插入，避免同屏重复。
3. **总量不过载**：`autoAds=true` 叠加多个手动位时，手动位控制在 **2~3 个**；过密会伤阅读、增 CLS，甚至触碰审核红线（见 3.5）。

### 6.1.5、推荐的起步配置

```json
{
  "version": 1,
  "enabled": true,
  "client": "ca-pub-1234567890123456",
  "autoAds": true,
  "adsTxt": "",
  "slots": [
    { "slot": "1111111111", "position": "articleTop", "enabled": true, "format": "auto", "alignment": "center" }
  ]
}
```

**一句话：** 起步「Auto ads + 一个 `articleTop` 手动位」，跑出数据后再用手动位逐步接管高价值位置——**先保证能读，再谈收益**。

## 6.2、申请前自查（8 项）

| # | 项 | 怎么查 | 达标 |
| --- | --- | --- | --- |
| 1 | 内容可抓取 | `curl -H 'Accept: text/html' …/blog/513 \| grep '<h1>'` | 源码有正文 |
| 2 | sitemap 可访问 | `curl -s …/sitemap.xml` | 200 |
| 3 | 被 Google 收录 | Search Console → 页面 | 部分已编入索引 |
| 4 | 隐私政策页 | 打开 `/privacy` | 含 Cookie + AdSense + 第三方 |
| 5 | 关于/联系页 | `/about`、有邮箱 | 页脚可进入 |
| 6 | 全站 HTTPS + 自有域名 | 浏览器地址栏 | 小锁 |
| 7 | 移动端可用 | 手机 / PageSpeed | 无红色硬伤 |
| 8 | 无死链 | 抽查导航/内链 | 无 404 |

## 6.3、申请与站点验证

① https://adsense.google.com/ 登录注册

② 填账号信息（国家/时区/收款姓名地址，须真实）

![img](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120217965.png)  

③ Sites → 添加站点 changlu.cloud（只填域名）

注意：这里只能够填写主域名，对应的话我是blog.changlu.cloud，这里只能够去配置主域名了：

![image-20261012021915652](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120219711.png)  

④ 选验证方式来进行网站审核，这里我选择了：ads.txt

![image-20261012021832898](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120218040.png)

此时我们进入blogloom后台来进行配置即可，其中发布商ID需要补充ca-：

![image-20261012023941460](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120239611.png)      

⑤ 点 Verify（实时校验）

![image-20261012024026965](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120240098.png)  

此时已通过认证：

![image-20261012024039343](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120240560.png)      

⑥ 进入审核（数天~两周），期间勿频繁改结构

![image-20261012024051832](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120240879.png)  

等待吧：

![image-20261012024106995](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120241130.png)    

⑦ 通过 → Ready → 广告开始展示（≤1h）；被拒 → 按拒信整改，等 7~14 天再申

**为什么选 ads.txt：** 三种验证方式（AdSense 代码段 / Ads.txt 代码段 / 元标记）任选其一即可。ads.txt 反正后面投放也必须有，一次配置两处到位，且**不依赖首页是否展示广告**。BlogLoom 后台把发布商 ID 填成 `ca-pub-8054136469344898`、「ads.txt 配置」**留空**、总开关**开启**，保存后 `/ads.txt` 即自动输出：

```plain
google.com, pub-8054136469344898, DIRECT, f08c47fec0942fa0
```

回 AdSense 点 **Verify**，Google 抓取到该行即为通过。

> **注意：** 总开关不能关（否则 `/ads.txt` 为空）；必须公网 HTTPS 根路径可访问；内容**留空自动生成**，别手改。

## 6.4、核心步骤一：在 Google AdSense 开启自动广告

> **前提：** 站点已通过审核（Sites 状态 Ready）。自动广告在 **Google 侧**开启，BlogLoom 侧只是「投放意图」开关，两边必须一致。

**操作：**

1. 登录 [Google AdSense](https://adsense.google.com/)；
2. 左侧进入 **Ads → By site**；
3. 找到你的站点 `changlu.cloud`，打开 **Auto ads（自动广告）** 开关；
4. （可选）进入 Auto ads 设置，按需开关具体格式（In-page / Anchor / Vignette 等）；
5. 完成后再回到 BlogLoom 后台，把第 2 段「自动广告」开关一起打开（见 6.7）——**两边一致**。

> **重点：** Google 侧的自动广告开关才是**最终裁决**，BlogLoom 的 `autoAds` 只是声明意图。不一致会出现「平台开了、实际没投」或「平台关了、Google 还插」两种情况。

> **📷 截图位 ①：** AdSense → Ads → By site，开启自动广告的界面

![image-20261012024140444](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120241599.png)  

右上角开启，点击应用到网站：

![image-20261012024253695](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120242864.png)  

同时自动优化也开启：

![image-20261012024515004](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120245199.png)  

保存：

![image-20261012024309484](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120243754.png)     

## 6.5、核心步骤二：配置并验证 ads.txt

`ads.txt` 建议**尽早配置**：它声明站点授权 Google 销售广告库存，越早被 Google 抓到，流失的广告预算越少。

**BlogLoom 侧：**

1. 后台「广告平台关联 → ③ ads.txt 配置」，**留空**即按发布商 ID 自动生成；需自定义再填写；

   ![image-20261012023941460](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120239611.png) 

2. 点「**验证并下载**」：内容非空则下载 `ads.txt` 并提示成功；为空则提示「请先配置发布商 ID」；

3. 若站点由 BlogLoom 托管，`/ads.txt` 会随配置**自动生效**，无需手动上传；否则把下载的文件传到域名根目录。

**Google 侧确认：**

```plain
① 打开 https://changlu.cloud/ads.txt 确认一行内容
② AdSense → Sites → Ads.txt 查看 "last crawled" 状态
③ 异常 → 点 Check for updates 重新抓取
④ 状态变为 Authorized → 正常投放
```

见上面6.3步骤即可：



![image-20261012024026965](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120240098.png)  

此时已通过认证：

![image-20261012024039343](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120240560.png)      

同时在blogloom中也要进行开启：

![image-20261012024538975](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120245027.png)  

## 6.6、核心步骤三：创建手动广告单元（展示广告 · 纵向）

这里以「在文章**右侧目录下方**挂一个**纵向广告**」为示范。

**操作：**

```plain
AdSense → Ads → By ad unit → 新建广告单元
  → 选择「展示广告（Display）」   ← 适用范围广、任何空间都能投
  → 命名（如 blog-article-sidebar-toc）
  → 选择纵向（Vertical）形态（或交给 responsive 自适应）
  → 保存 → Get code → 复制整段 <ins> 代码
```

| 建议单元名 | 目标位置 | 形态 |
| --- | --- | --- |
| `blog-article-sidebar-toc` | `articleSidebarToc`（右侧目录下方） | 展示广告 · **纵向** |
| `blog-article-top` | `articleTop` | 展示广告 · 自适应 / 横幅 |
| `blog-article-bottom` | `articleBottom` | 展示广告 / 多重广告 |

> **注意：** 复制的是**完整代码**，不用自己找 `data-ad-slot`——下一步粘贴后系统会自动识别。若想尝试「信息流 / 文章内嵌 / 多重」，分别选对应类型即可（见 2.1）。

选择展示广告：

![image-20261012024602310](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120246427.png)  

选择纵向保存即可：

![image-20261012024635842](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120246894.png)  

点击进行复制代码：

![image-20261012024702793](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120247987.png)

点击复制代码：

![image-20261012024732445](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120247673.png)  

## 6.7、核心步骤四：回到平台完成广告位配置

把上一步拿到的代码填进 BlogLoom 后台「广告平台关联」：

1. **总开关：** 打开「启用 AdSense」；
2. **① 发布商配置：** 填 `ca-pub-数字`（粘贴广告位代码时若为空会自动回填）；
3. **② 自动广告：** 与 Google 侧保持一致（见 6.4）；
4. **③ ads.txt 配置：** 一般留空（见 6.5）；
5. **④ 广告位配置：**
   - 点「**新增广告位**」；
   - **投放位置** 选「**文章详情·右侧目录下方**」（挂纵向广告）；
   - 把上一步的完整代码粘进「Google 代码」框，**失焦**后出现「已识别：slot=…」；
   - 需要更多位置就继续「新增广告位」，并在下拉里改选对应位置（不可重复，最多 5 个）；
6. **保存配置** → 前台刷新即生效。

> **注意：** 位置由「投放位置」下拉决定；新增时会自动分配一个默认位置，可随时改选。「右侧目录下方」在**移动端右栏隐藏**，只桌面端可见。

在blogloom中进行新增广告位：

![image-20261012024808794](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120248033.png) 

默认这个文章详情右侧目录下面为：

![image-20261012024846173](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120248258.png)  

随便找篇文章打开源代码即可：

![image-20261012025103267](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610120251459.png)  

## 6.8、广告位生效

todo

## 6.9、结算与监控

### 6.9.1、结算门槛与前置验证（缺一不可）

| 项 | 说明 | 入口 |
| --- | --- | --- |
| **结算门槛** | 余额累计满 **100 美元**（或等值）才触发支付 | — |
| **身份验证** | 上传证件核验身份，通常 1~数日出结果 | Payments → 验证身份 |
| **地址 PIN 验证** | Google 邮寄含 6 位 PIN 的明信片到收款地址，需在 AdSense 输入；中国一般 2~4 周收到 | Payments → 验证地址 |
| **税务信息** | 填 W-8BEN；中美税收协定下，仅对**美国来源**收入按协定税率预扣（常见 **10%**） | Payments → 管理税务信息 |

> **重点：** 四项**缺一不可**，任一项未完成，AdSense 会暂停付款并在付款页提示「需要操作」，核对无误后才会继续打款。

### 6.9.2、国内提现（电汇）配置

国内 AdSense 收益通过 **银行转账（电汇 / Wire Transfer）**、以 **美元（USD）** 打款。配置步骤：

```plain
AdSense → Payments → 管理付款方式 → 添加付款方式
  → 选择「银行转账（电汇）」
  → 填写：
      收款人姓名（Account holder name） —— 必须与银行开户名完全一致
      银行名称（Bank name）
      SWIFT / BIC 代码 —— 填开户行的 SWIFT
      账号（Account number）
      收款人详细地址
  → 保存，等待 Google 验证付款方式
```

| 项 | 要求 |
| --- | --- |
| **账户类型** | 需能接收**外币电汇**的账户：多币种账户 / 外币账户（中行、招行、工行等均可） |
| **收款人姓名** | 与银行账户**开户名完全一致**，否则电汇会被退回 |
| **SWIFT/BIC** | 填**开户行**的 SWIFT 代码（不确定直接问银行网点） |
| **账户国家** | 付款方式所在国家/地区必须是**中国**，且为**本人名下**账户 |

> **注意：** 不要用他人账户或不明第三方「代收」，易触发风控、冻结甚至无法入账。

> **📷 截图位 ⑧：** AdSense「Payments → 管理付款方式」添加银行转账（电汇）界面

### 6.9.3、到账、结汇与税务

- **打款时间：** 达标后，Google 通常在每月 **21~26 日**发起付款，电汇到账一般 **3~7 个工作日**。
- **手续费：** Google 可能扣电汇费，**中转行 / 收款行**可能再扣一笔；金额小的时候不划算。
- **结汇：** 个人每年有 **5 万美元**便利化结汇额度；银行可能要求说明资金用途（如「广告服务费/稿费」）并出示 AdSense 付款凭证。
- **合规申报：** 广告收入属应税所得，按当地规定申报；保留 AdSense 付款记录与相关协议备查。

> **📷 截图位 ⑨：** 收款银行到账 / 结汇界面（可选）

### 6.8.4、收益指标与监控节奏

+ **核心指标：** Page RPM / Impressions RPM / CTR / CPC（含义见 3.6），**看周趋势而非单日**。
+ **监控节奏：** 每周固定看广告展示、点击、RPM；出现「无效流量」「政策提醒」邮件立即处理（见 3.5）。

---

# 七、验证测试

## 7.1、功能验收（浏览器 / 后台）

| 序号 | 功能点 | 操作 | 预期结果 |
| --- | --- | --- | --- |
| 1 | 总开关关闭 | 关闭后刷新文章页 | 源码无 `adsbygoogle`，无广告位 |
| 2 | 发布商 ID 注入 | 填 `client` 保存 | `<head>` 出现 `adsbygoogle.js?client=…` |
| 3 | 广告位粘贴识别 | 粘贴 Google 代码失焦 | 显示「已识别：slot=… format=…」 |
| 4 | 位置选择 | 新增广告位后改「投放位置」 | 默认按顺序分配；可改选，含「右侧目录下方」；最多 5 个 |
| 5 | 广告位渲染 | 打开 `/blog/{id}` | 文章页出现对应 `<ins>` + `push` |
| 6 | 单广告位关闭 | 关闭某位保存 | 源码中该位置 `<ins>` 消失 |
| 7 | ads.txt 验证并下载 | 点「验证并下载」 | 内容非空则下载 `ads.txt`；为空则报错提示 |
| 8 | SPA 注入 | 打开首页 | 加载器注入；切路由后新广告位被 push |

## 7.2、curl 验证

```bash
# 文章页加载器 + 广告位
curl -s -H 'Accept: text/html' https://changlu.cloud/blog/513 \
  | grep -Eo 'adsbygoogle[^"]*|data-ad-slot="[0-9]+"'

# ads.txt
curl -s https://changlu.cloud/ads.txt
# 期望：google.com, pub-9876543210987654, DIRECT, f08c47fec0942fa0

# 未配置时不注入（后台先清空发布商 ID 或关闭总开关）
curl -s -H 'Accept: text/html' https://changlu.cloud/blog/513 | grep -c 'adsbygoogle'  # 期望 0
```

## 7.3、单元测试

集成测试 `SeoEndpointIntegrationTest#shouldRenderAdsenseAndExposeAdsTxtWhenConfigured()` 覆盖：

+ 文章页 HTML 含加载器、`data-ad-slot="1111111111"`、`data-ad-slot="2222222222"`；
+ `/ads.txt` 等于 `google.com, pub-1234567890123456, DIRECT, f08c47fec0942fa0\n`。

```bash
cd blog-backend
mvn -Dtest='Seo*Test' test
```

---

# 八、总结

回到最开始那个场景——**流量来了，怎么变现**。本文把这条链路完整走了一遍：

```plain
问题：流量有了，收益从哪来
  ↓
方案：先接 Google AdSense（门槛最低、匹配度高）
  ↓
概念：加载器(client) + 广告单元(slot) + 自动广告(autoAds) + ads.txt
  ↓
需求：一个 JSON 承载全部；总开关 + 发布商ID + 自动广告 + ads.txt + 广告位
  ↓
配置规则：四段式表单；粘贴代码自动识别；位置内部维护；组合行为矩阵明确
  ↓
代码：AdsenseConfig 存储 → Controller 校验 → Builder 装配 → 模板/SPA 注入 → ads.txt
  ↓
上线：定策略 → 申请 → 开自动广告 → 配 ads.txt → 建手动单元（展示广告·纵向）→ 平台广告位配置 → 监控收益
```

**一句话总结：BlogLoom 把 AdSense 做成了「后台一个总开关 + 四段式表单」——用户只粘贴 Google 代码、决定启停，技术位置由系统内部维护；配置存成一份 JSON，后端解析兜底、SSR/SPA 双端注入、ads.txt 自动生成。这样既满足 AdSense 的合规与投放要求，又做到「改配置不改代码」。**

**重点回顾：**

+ 投放策略：**阅读优先**，起步「Auto ads + 一个 `articleTop` 手动位」，跑出数据再逐步加位（见 6.1）。
+ 发布商 ID **全站唯一**，广告位**可以多个**；`autoAds` 只是意图，AdSense 后台「按网站」才是最终裁决。
+ 加载器注入三条件：`enabled != false` **且** `client` 非空 **且**（`autoAds=true` 或存在启用 slot）。
+ 位置后台可下拉选择（含「右侧目录下方」纵向位），与后端白名单同源，最多 5 个。
+ 配置损坏 = 不注入、不报错，**绝不影响正文阅读**。

---

# 参考资料

[1]. [About the AdSense code（AdSense 代码说明）](https://support.google.com/adsense/answer/9274634)

[2]. [代码放置位置（`<head>`）](https://support.google.com/adsense/answer/9274516)

[3]. [站点管理改进（meta 验证 / ads.txt）](https://support.google.com/adsense/answer/13996652)

[4]. [自适应广告代码参数](https://support.google.com/adsense/answer/9183460)

[5]. [广告投放政策](https://support.google.com/adsense/answer/1346295)

[6]. [ads.txt 指南](https://support.google.com/adsense/answer/12171612)

[7]. [ads.txt 技术规范（Google for Developers）](https://developers.google.com/adsense/platforms/transparent/ads-txt)

[8]. [广告代码（AFP 参数参考）](https://developers.google.cn/adsense/platforms/direct/ad-tags)

[9]. [SPA 接入 AdSense 实践](https://jasonwatmore.com/add-google-adsense-to-a-single-page-app-react-angular-vue-next-etc)

[10]. [申请与审核经验（2026）](https://eastondev.com/blog/zh/posts/media/20260108-google-adsense-approval-guide/)

[11]. [布局与 CTR 优化（2026）](https://eastondev.com/blog/zh/posts/media/20260108-adsense-placement-optimization/)

[12]. [BlogLoom 开源项目（GitHub）](https://github.com/changluya/BlogLoom)

---

整理者：长路　创建时间：2026.10.12　更新时间：2026.10.12
