# BlogLoom 前台外部图床图片防盗链优化方案

> 迭代版本：`release_1.0.x`
> 文档状态：已落地
> 涉及范围：`blog-view-ui/index.html`、`blog-view-ui/src/views/blog/Blog.vue`、`blog-view-ui/src/views/moments/Moments.vue`、`blog-view-ui/src/views/about/About.vue`、`blog-view-ui/src/views/friends/Friends.vue`

## 一、背景

### 1.1 问题现象

文章正文中引用的部分外部图床图片（例如语雀 CDN `cdn.nlark.com`）在博客前台不展示，控制台表现为图片请求失败；但把同一 URL 复制到浏览器地址栏可以直接打开并下载。

示例地址：

```text
https://cdn.nlark.com/yuque/0/2025/png/2464036/1760006042970-1252a85f-ba2e-41c4-947b-bd4423ab68ec.png
```

### 1.2 影响范围

前台所有通过 `v-html` 渲染富文本的位置都可能命中该问题：

| 页面 | 渲染位置 |
| --- | --- |
| 文章详情 | `views/blog/Blog.vue` |
| 动态 | `views/moments/Moments.vue` |
| 关于我 | `views/about/About.vue` |
| 友人帐 | `views/friends/Friends.vue` |

只要图片来自「按 Referer 做防盗链」的第三方图床，就会出现「直链能开、页面不显示」。

### 1.3 优化目标

- 前台能够正常展示外部图床图片，无需用户手动下载再上传；
- 不引入代理服务器、不额外消耗后端带宽；
- 尽量不影响同源请求的 Referer（保留站点自身的请求语义）；
- 方案全局生效，后续新增 `v-html` 渲染位置无需重复改造。

## 二、分析调研

### 2.1 复现与验证

使用 `curl` 对比不同 Referer 下的响应：

```bash
URL="https://cdn.nlark.com/yuque/0/2025/png/2464036/1760006042970-1252a85f-ba2e-41c4-947b-bd4423ab68ec.png"

# 1) 不携带 Referer
curl -s -o /dev/null -w "http=%{http_code} type=%{content_type}\n" -A "Mozilla/5.0" "$URL"
# -> http=200 type=image/png

# 2) 携带外部站点 Referer
curl -s -o /dev/null -w "http=%{http_code} type=%{content_type}\n" -A "Mozilla/5.0" -e "https://example.com/" "$URL"
# -> http=403 type=text/html

# 3) 携带语雀自身 Referer
curl -s -o /dev/null -w "http=%{http_code} type=%{content_type}\n" -A "Mozilla/5.0" -e "https://www.yuque.com/" "$URL"
# -> http=200 type=image/png
```

结论：该 CDN 依据 `Referer` 判定来源，外部站点引用返回 `403`，无 Referer 或同站 Referer 返回 `200`。

### 2.2 根因定位

浏览器在页面内发起图片请求时，默认会携带当前页面地址作为 `Referer`（现代浏览器默认策略为 `strict-origin-when-cross-origin`，跨域时至少携带来源 origin）。因此：

- 地址栏直接访问：不产生 `Referer` → CDN 放行 → 可下载；
- 前台 `v-html` 内嵌图片：携带本站 `Referer` → CDN 判定为盗链 → `403` → 图片空白。

问题不在图片 URL，也不在 `v-html` 渲染，而在于**跨域图片请求携带了被图床拒绝的 Referer**。

### 2.3 方案对比

| 方案 | 思路 | 优点 | 问题 | 结论 |
| --- | --- | --- | --- | --- |
| 文档级 Referrer Policy | `index.html` 设置 `<meta name="referrer" content="same-origin">` | 一行生效、全局覆盖、无需改业务代码 | 跨域请求统一不带 Referer | **采用** |
| 逐张图片属性 | 给每个 `<img>` 加 `referrerpolicy="no-referrer"` | 粒度最细 | 图片由 `v-html` 动态插入，需遍历或改写 HTML，维护成本高 | 不采用 |
| 后端代理转发 | 前端经后端拉取图片再回源 | 可统一缓存、可控 | 增加带宽与实现成本，失去 CDN 直连优势 | 不采用 |
| MutationObserver 动态打标 | JS 监听 DOM 给新图片加属性 | 只影响图片 | 需处理插入时序，代码量大于一行 meta | 不采用 |

最终选择文档级 `same-origin`：跨域图片不再携带 Referer 即可绕过防盗链，同源请求（如本站静态资源）仍保留 Referer，站点级副作用最小。

## 三、设计落地实现步骤

### 3.1 设置文档级 Referrer Policy

在 `blog-view-ui/index.html` 的 `<head>` 中新增：

```html
<meta name="viewport" content="width=device-width,initial-scale=1.0">
<!-- 跨域图片（如语雀 cdn.nlark.com 等启用防盗链的图床）不携带 Referer，避免被 403 拦截；同源请求仍保留 Referer -->
<meta name="referrer" content="same-origin">
<link rel="icon" href="/img/site-favicon.png">
```

### 3.2 渲染链路无需改动

图片仍由各页面的 `v-html` 渲染，`Blog.vue` 等无需感知 Referer 策略：

```html
<div class="typo article-content" v-lazy-container="{selector: 'img'}" v-viewer v-html="blog.content"></div>
```

`<meta name="referrer">` 作用于整个文档，所有子资源请求（含 `v-html` 动态插入的 `<img>`）都会按该策略决定是否携带 Referer。

### 3.3 构建与验证

```bash
cd blog-view-ui
npm run build
```

构建产物校验：

```bash
grep -n "referrer" dist/index.html
# -> <meta name="referrer" content="same-origin">
```

手工验收项：

1. 打开引用语雀 CDN 图片的文章详情页，确认图片正常展示；
2. 打开动态 / 关于我 / 友人帐中引用外部图床的页面，确认图片正常展示；
3. 打开浏览器 Network，确认图片请求 `200`，且请求头不含 `Referer`；
4. 确认本站 `/static/**` 等同源图片仍正常展示。

## 四、原理介绍

### 4.1 Referer 与防盗链

`Referer` 是浏览器在发起请求时附带的上游页面地址。图床常以它判断资源是否被外部站点引用：来自白名单域名放行，其他来源拒绝。这既能节省 CDN 流量，也会误伤合法的内容嵌入。

### 4.2 Referrer Policy 的作用

`<meta name="referrer">` 设置文档的默认 Referrer Policy，浏览器据此决定每个请求是否发送 `Referer`。常见取值：

| 取值 | 同源请求 | 跨域请求 |
| --- | --- | --- |
| `no-referrer` | 不发送 | 不发送 |
| `same-origin` | 发送 | 不发送 |
| `strict-origin-when-cross-origin`（浏览器默认） | 发送完整 URL | 仅发送 origin |

`same-origin` 让跨域图片请求不带 Referer，从而通过防盗链校验，同时保留同源请求的 Referer 语义。

### 4.3 适用边界与注意事项

- 该策略只解决「按 Referer 防盗链」的图床；若图床按签名/时效 URL 校验，仍需业务侧重新上传或换源；
- 跨域请求（含后端 API）不再携带 Referer，本方案下后端未使用 Referer 做鉴权或统计，因此无影响；
- 若后台 CMS 的编辑器预览同样需要展示这类图片，可在 `blog-cms-ui/index.html` 同步加入相同的 `meta`；
- 图片本身仍直连第三方 CDN，不经过博客服务器，不增加后端带宽。
