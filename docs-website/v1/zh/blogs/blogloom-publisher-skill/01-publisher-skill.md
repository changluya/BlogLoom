---
title: "BlogLoom 分发 Skill 01：博客一键分发 Skill 从 0 到 1（Playwright + CDP）"
description: 从手动搬运到自动化分发：技术选型、核心设计准则、CSDN 发布链路拆解，以及真实踩坑与优化复盘。
---

# 一、背景：为什么我们需要一个「发布分发 Skill」

## 1.1、场景引入

在维护 BlogLoom 这套开源博客系统的过程中，我逐渐固定了一套「本地 Markdown 创作 → 平台统一管理」的内容工作流：文章先以 Markdown 形式沉淀在本地知识库里，顶部带一段标准元数据，再导入平台。

但很快遇到了一个非常具体的问题：**同一篇文章，我往往还需要分发到 CSDN、掘金、公众号等多个站外渠道**。

一开始我靠手动搬运：打开编辑器、复制正文、填标题、加标签、选专栏、传封面……一篇文章十分钟起步，批量分发更是机械又容易漏。

于是就有了 **BlogLoom Publisher Skill** ——把这些「重复点击」沉淀成一个可被 AI Agent 直接调用的技能包。

## 1.2、手动分发的四大痛点

* **耗时**：一篇长文光是粘贴、等图片上传、配标签就要数分钟；
* **易错**：标签/专栏容易选错，封面忘记设置；
* **不统一**：各渠道字段口径不同，靠人脑映射成本高；
* **难复用**：每篇文章都要「重新点一遍」，没法沉淀为流程。

**重点**：我们要解决的不是「怎么写文章」，而是「写完文章之后，如何用一句话把它分发出去」。

## 1.3、目标与范围

一句话描述目标：

> 把符合统一 SOP 的本地 Markdown 博客，用自然语言即可分发到多个第三方渠道；初次对接渠道为 CSDN。

## 1.4、项目地址与专栏说明

> 本篇是 **BlogLoom 分发 Skill 专栏** 的开篇（`BlogLoom分发Skill` 专栏第 01 篇），后续该专栏会持续记录多渠道分发的设计与实践。

* **BlogLoom 开源仓库**：https://github.com/changluya/BlogLoom
* **本 Skill 目录**：https://github.com/changluya/BlogLoom/tree/master/skills/blogloom-publisher-skill

说明：本 Skill 归属于 BlogLoom 仓库的 `skills/blogloom-publisher-skill` 目录，与主站（`blog-backend` / `blog-cms-ui` / `blog-view-ui`）同仓维护。克隆后进入该目录即可使用：

```bash
git clone https://github.com/changluya/BlogLoom.git
cd BlogLoom/skills/blogloom-publisher-skill
npm install
node scripts/publisher.js csdn checkLogin
```

---

# 二、技术栈选型：为什么是 Playwright + CDP

面对「自动化发布到第三方平台」，常见有三条路。我们逐一对比。

## 2.1、方案一：直接调用平台 HTTP 接口

抓包找到发布接口，直接用代码构造请求。

* **优点**：快、资源占用低、可无头无浏览器。
* **缺点**：接口带签名/加密参数（如 CSDN 的 `x-ca-*` 系列），且随版本频繁变化；一旦改版就要重新逆向。
* **适用**：内部可控、接口稳定的系统。

**弊端说明**：第三方平台的发布接口本质是「私有协议」，跟进成本高、稳定性差，不适合作为长期方案。

## 2.2、方案二：Selenium + WebDriver

传统浏览器自动化方案，生态成熟、资料多。

* **优点**：API 稳定、社区庞大、能复用已有 `chromedriver` 经验。
* **缺点**：与浏览器版本强绑定（driver 版本）、等待与稳定性需大量手工封装、对现代前端（异步渲染）不够友好、调试体验一般。
* **适用**：老项目、已深度投入 Selenium 的场景。

## 2.3、方案三：Playwright + CDP（最终选用）

以 Playwright 作为**主操作**，CDP（Chrome DevTools Protocol）作为**底层增强**。

* **Playwright 主操作**：自动等待元素可交互、跨浏览器、`locator` 语义清晰、有头/无头一键切换，尤其适合「模拟真人操作」；
* **CDP 底层增强**：复用已有 Chrome 的登录态（`connectOverCDP`）、读取 HttpOnly Cookie 判定登录、`Input.insertText` 作为富文本写入兜底。

**重点**：这是「**高层易用 + 底层可控**」的组合。Playwright 负责 90% 的常规操作，CDP 负责 Playwright 够不到的那 10%。

## 2.4、选型结论对比

| 维度 | 纯 HTTP 接口 | Selenium + WebDriver | Playwright + CDP |
|---|---|---|---|
| 抗改版能力 | 弱（需逆向） | 中 | 强（模拟点击） |
| 登录态复用 | 需自行维护 Token | 一般 | 强（persistent context / CDP） |
| 异步渲染适配 | 不涉及 | 需手工等待 | 内置自动等待 |
| 有头观察调试 | 无 | 支持 | 支持（默认有头） |
| 与 Agent 协作 | 好 | 一般 | 好（统一 CLI） |
| 结论 | 不采用 | 备选 | **采用** |

---

# 三、核心设计准则

## 3.1、三层分离 + 按渠道分文件夹

Skill 内部严格分层，职责单一：

```text
用户自然语言 / 命令行
   │
   ▼
publisher.js        路由层：解析 <channel> <action>，分发
   │
   ▼
channels/<channel>/  渠道层：每个渠道一个文件夹（index.js 流程 + selectors.js 选择器）
   │
   ▼
lib/                通用层：session / editor / markdown / channel 加载
```

**重点**：**选择器永远单独放 `selectors.js`**。站点改版时，只改选择器文件，业务流程代码零改动。

```text
scripts/channels/csdn/
├── index.js       # 业务流程：登录判定、进编辑器、填字段、发布、删除
└── selectors.js   # DOM 选择器集中维护（改版只改这里）
```

## 3.2、一个渠道，一个 SOP

参考 BlogLoom 主站的「场景路由」思想：文档不加倍，**每个渠道维护一份 SOP** 即可。

```text
references/
├── 00-tool-contract.md          # 通用契约：CLI / 参数 / 返回 / 错误码
└── channels/csdn/sop.md         # CSDN 渠道 SOP（含 selectors 对照表）
```

这样新增渠道 = 加 `scripts/channels/<name>/` + `references/channels/<name>/sop.md` + 路由表登记一行。

## 3.3、前置对齐统一博客 SOP

分发之前，先按 `标准生成发布输出博客sop.md` 校验博客顶部元数据：

```JSON
{
   "title": "…",
   "tags": "Maven, spotless, licenseHeader",
   "category": "Maven",
   "articleSummary": "…",
   "columns": "项目管理工具, Maven&Gradle",
   "createTime": "2026-09-21 18:30:00",
   "updateTime": "2026-09-21 18:30:00",
   "knowledgeBasePath": "/0x05、Java后端/…/maven插件"
}
```

解析层做了两件事：

* **字段归一**：兼容 `category`/`categories`、`columns`/`column` 等写法；
* **宽松解析**：容忍 SOP 样例里常见的全角冒号、尾随逗号、全角引号，避免因笔误整段元数据丢失。

**注意**：封面图统一以正文里的 `![coverImg](url)` 显式标记，与 BlogLoom 平台导入规则保持一致。

## 3.4、模拟真人操作 + 登录态持久化

* 主操作全部走 Playwright：真实点击、输入、粘贴，**默认有头浏览器**，发布过程肉眼可见；
* 登录用**独立的持久化 profile**（`~/.blogloom-publisher/chrome-profile-<channel>`），扫码一次后续免登录；
* 也支持 `--cdp` 连接你日常使用的 Chrome，直接复用现成登录态。

## 3.5、统一 CLI 契约与机器可读输出

```bash
node scripts/publisher.js <channel> <action> [options]
```

统一输出到 stdout（日志走 stderr），方便 Agent 解析：

```json
{"ok":true,"channel":"csdn","action":"publish","data":{"status":"PUBLISHED","url":"…"},"error":null}
```

---

# 四、能力全景：目前支持什么

## 4.1、动作矩阵

| 动作 | 说明 |
| --- | --- |
| `csdn checkLogin` | 检测登录态 |
| `csdn login` | 打开浏览器扫码登录并持久化 |
| `csdn publishDraft` | 保存为草稿 |
| `csdn publish` | 发布博客 |
| `csdn delete` | 删除博客（内容管理页定位后删除，含二次确认与校验） |
| `csdn test publish` | 组合链路自测：发布 → 立即删除 |

## 4.2、有头 / 无头可配置

```bash
# 默认有头，便于观察
node scripts/publisher.js csdn publish --file "/abs/blog.md"

# 无头（CI / 无人值守）
node scripts/publisher.js csdn publish --file "/abs/blog.md" --mode headless
```

也可用环境变量 `PUBLISHER_MODE=headed|headless` 全局控制。

## 4.3、主动登录（本地工作台定位）

这个 Skill 的定位是**用户本地工作台**：需要登录的动作检测到未登录时，会**主动拉起浏览器**让用户扫码，登录完成后自动继续。

> 无头模式无法扫码，会自动重启为有头浏览器；CI 场景可用 `--no-login` 直接报错退出。

---

# 五、实战：CSDN 发布链路拆解

## 5.1、登录态判定

在首页通过登录入口文案判定，再用 Cookie 交叉验证：

* 找到 `.toolbar-btn-loginfun` 且文案为「登录」→ 未登录；
* 命中「创作」入口且非登录态，或 Cookie 含 `UserToken` + `UserInfo` → 已登录。

## 5.2、进入编辑器与标题/正文写入

1. 首页点「创作」（`.toolbar-btn-write-new`）进入 `editor.csdn.net/md`；
2. **标题默认是展示态**（`.article-bar__title-display`，显示「【无标题】」），点击后才显示隐藏的 `input.article-bar__title`；
3. 正文是 `<pre class="editor__inner markdown-highlighting" contenteditable>`，需先清空默认欢迎内容，再按 `paste → cdp → keyboard` 策略粘贴：

```js
async function setContent(page, markdown) {
  const editor = page.locator('.editor__inner.markdown-highlighting').first();
  await editor.click();
  await page.keyboard.press(`${MOD}+A`);
  await page.keyboard.press('Backspace');           // 清空欢迎内容
  const r = await writeContent(page, '.editor__inner.markdown-highlighting', markdown,
                               ['paste', 'cdp', 'keyboard']);
  return r;                                          // { strategy, length }
}
```

## 5.3、发布弹窗配置

点「发布文章」（`button.btn-publish`）打开弹窗 `.modal__publish-article`，在里面配置：

* 标签 `.mark_selection`
* **首图**：已有图片列表第一张（`.img-selection-item img.select-cover`）
* 摘要 `.desc-box .el-textarea__inner`
* 分类专栏（含二级 `# xxx`）
* 文章类型 → 原创；可见范围 → 公开
* **创作声明** → 个人观点，仅供参考

## 5.4、删除与二次确认

`delete` 进入内容管理页 `https://mp.csdn.net/mp_blog/manage`，定位文章行 → 悬停右侧「...」→ 下拉「删除」→ 弹窗「确定」，随后**回查该行已消失**才算成功。

---

# 六、踩坑与优化（重点复盘）

这部分是整篇最真实的部分：**每一条都是真机跑出来的坑**。

## 6.1、坑一：登录被「假阳性」

最初用 Cookie 里是否有 `UserToken/UserInfo/uuid_tt_dd/cnt_w` 判定登录。结果发现 **CSDN 在匿名态也会下发 `uuid_tt_dd`**，导致「未登录」被误判为「已登录」，随后编辑器直接跳到登录页。

* **现象**：`checkLogin` 说已登录，`publish` 打开编辑器却是登录页。
* **优化**：只认真正的登录凭证 `UserToken`/`UserInfo`，并在编辑器阶段判断是否被重定向到 `passport.csdn.net/login`，是则抛 `AUTH_REQUIRED`。

## 6.2、坑二：标题输入框是隐藏的

`.article-bar__title` 直接 `waitFor(visible)` 永远超时——因为它 **`display:none`**，真正显示的是 `.article-bar__title-display`。

* **优化**：先点展示态，唤出隐藏 input，再填入并回读校验：

```js
await page.locator('.article-bar__title-display').first().click();
const input = page.locator('input.article-bar__title').first();
await input.waitFor({ state: 'visible' });
await input.fill(title);
```

## 6.3、坑三：正文默认内容与富文本写入

CSDN 编辑器会预置一段「欢迎使用 Markdown 编辑器」的模板。如果不清空，发布出去的文章会多出无关内容。

* **优化**：写入前 `Cmd/Ctrl + A` + `Backspace` 清空；写入采用**剪贴板粘贴优先**（最贴近真人），失败再用 CDP `Input.insertText`、最后键盘逐字兜底；写入后回读内容长度校验。

## 6.4、坑四：首图列表异步加载

发布弹窗里的「已有图片列表」是**异步加载**的。刚点开弹窗就找 `.img-selection-item`，经常是空的，导致首图没选上。

* **优化**：正文含图片时，**按图片数量估算粘贴后的等待时间**——默认 2s，每满 5 张图再多等 3s，给足图片上传/解析时间；再点发布，首图列表才可用。

```js
function estimateImageWaitMs(content, { base = 2000, blockSize = 5, perBlock = 3000, max = 60000 } = {}) {
  const count = countImages(content);
  if (count === 0) return 0;
  return Math.min(base + Math.ceil(count / blockSize) * perBlock, max);
}
```

## 6.5、坑五：分类专栏的隐藏复选框与误匹配

两个连环坑：

1. **勾选无效**：分类专栏的勾选项其实是一个**隐藏的 `<input type="checkbox" class="tag__option-chk">`**，普通 `click()` 打不到，看着「选中了」，实际没生效；
2. **匹配错误**：用「包含」反向匹配时，设置值 `Maven&Gradle` 会被宽松地匹配到子类 `Maven`。

* **优化 1**：改为触发 DOM `click()` 勾选，并先判断 `checked` 避免反选，最后回读已选项：

```js
const chk = option.locator('.tag__option-chk').first();
const already = await chk.evaluate((el) => !!(el && el.checked));
if (!already) await chk.evaluate((el) => el && el.click());   // 关键：DOM click
```

* **优化 2**：匹配改为**评分制**——精确 `> ` 已有包含设置值 `> ` 设置值包含已有，取最高分，实测正确命中二级项 `# Maven&Gradle`。

## 6.6、坑六：删除「假成功」

`delete` 第一次跑返回了 `DELETED`，但文章其实还在。

* **根因**：CSDN 的确认弹窗类名是 `.el_mcm-message-box`（**带下划线前缀 `el_mcm`**），而我们的选择器写的是 `.el-message-box`，导致「确定」按钮根本没点到。
* **优化**：
  1. 修正确认按钮选择器为 `.btn-msg-confirm`；
  2. **增加结果校验**——点完确认后回查内容管理列表，该行必须消失，否则返回 `DELETE_UNVERIFIED`，绝不「假成功」。

**重点**：自动化最危险的错误不是「报错」，而是「**看起来成功**」。所以每个关键动作后都要有**回读校验**。

---

# 七、验证测试：把不稳定关进笼子

浏览器自动化天然依赖外部页面，怎么保证质量？我们的做法是 **mock 单测 + 真实链路自测**双层。

## 7.1、mock 单测

实现了一个轻量 Playwright 模拟层（`test/helpers/mock-page.js`），支持 `locator/click/fill/hover/filter/waitFor/evaluate` 等子集，**无需真实浏览器**即可对每个工具做行为验证：

```bash
npm test              # 全部单测（核心 + 所有渠道），当前 45 项全绿
npm run test:core     # 仅核心：CLI / markdown / 渠道加载
npm run test:channels # 仅渠道：当前 csdn
```

覆盖 `checkLogin / enterEditor / setTitle / setContent / preparePublish / publish / saveDraft / deleteBlog` 等每一个脚本工具。

## 7.2、真实链路自测：test publish

Mock 能测逻辑，但测不出「页面改版」。所以提供一个组合自测：

```bash
node scripts/publisher.js csdn test publish --file "/abs/blog.md"
```

它执行「真实发布 → 立即删除」，返回：

```json
{"status":"PUBLISHED_AND_DELETED",
 "publish":{"url":"https://blog.csdn.net/…/details/166846562",
            "publishConfig":{"selectedCategories":["项目管理工具","# Maven&Gradle"],
                             "coverSet":true,"creationStatement":"个人观点，仅供参考"}}}
```

**重点**：`--dry-run` 只填写不点发布，用来快速验证字段；`test publish` 用来验证完整链路且**不留残留内容**。

---

# 八、总结与展望

回顾这次从 0 到 1 的实践，可以浓缩为几条准则：

* **背景驱动**：先想清楚「是谁、在什么场景、反复做什么」，再动手；
* **技术选型要匹配问题**：第三方平台发布，选「模拟真人操作」的 Playwright + CDP，而不是硬刚私有接口；
* **结构决定可维护性**：按渠道分文件夹、选择器独立、一渠道一 SOP；
* **一切关键动作都要回读校验**：警惕「假成功」；
* **测试双层**：mock 单测守逻辑，真实链路自测守改版。

后续计划：

1. 增加更多渠道（掘金、公众号等），验证「按渠道分层」的可扩展性；
2. 支持封面图自动上传（远程 `coverImg` → 下载 → 上传）；
3. 为 Agent 提供更丰富的自然语言路由示例，让「一句话分发」更顺滑。

> 如果你也在做内容多渠道分发，希望这套「Playwright 主操作 + CDP 底层增强」的思路能给你一点参考。
>
> 项目地址：https://github.com/changluya/BlogLoom ｜ Skill 目录：https://github.com/changluya/BlogLoom/tree/master/skills/blogloom-publisher-skill

---

整理者:长路 时间:2026.9.29
