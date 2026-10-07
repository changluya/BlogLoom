# BlogLoom 分发 Skill 之公众号（gzh）设计思路与功能实现

> 面向对象：参与 BlogLoom 多渠道分发设计、需要理解或扩展第三方渠道 Skill 的开发者。
> 对应实现目录：`skills/blogloom-publisher-skill/scripts/channels/gzh/`、`references/channels/gzh/sop.md`。

---

## 一、背景

### 1.1、要解决的问题

在把 BlogLoom 的本地 Markdown 分发到 CSDN 之后，我们紧接着要打通第二个渠道：**微信公众号**。

公众号是内容分发绕不开的阵地，但它和 CSDN 有着本质不同的编辑器形态：

- CSDN 的编辑器**原生识别 Markdown**，直接粘贴 Markdown 源码即可排版；
- 公众号的编辑器是 **ProseMirror 富文本**，**不识别 Markdown 源码**——直接粘贴 Markdown 只会得到一堆带 `#`、`*`、反引号的纯文本。

除此之外，公众号还有一套极其繁琐的发布配置：首图要「从正文选择」、声明原创要勾「我已阅读」、群发要管理员扫码……每一个环节都是真人才能稳定点对的操作。

### 1.2、为什么单独成篇

上一篇（CSDN）讲的是「编辑器原生吃 Markdown、靠弹窗逐项配置」的模式；公众号这篇讲的是完全不同的三个难点：

| 维度 | CSDN | 公众号（gzh） |
| --- | --- | --- |
| 编辑器内核 | Markdown 原生识别 | ProseMirror 富文本，**不认 Markdown** |
| 正文写入 | 直接粘贴 Markdown 源码 | 先用 md.doocs 转成带样式 HTML 再粘贴 |
| 封面 | 发布弹窗选已有图片第一张 | 编辑页底部「从正文选择」+ 图片上传校验 |
| 发表 | 弹窗点发布即可 | 群发弹窗 + 二次确认 + **管理员扫码** |
| 终态 | `PUBLISHED` | `SUBMITTED`（已提交，需扫码/审核） |

### 1.3、设计目标

| 目标 | 含义 |
| --- | --- |
| 正文排版不丢 | 用 md.doocs 把 Markdown 渲染成公众号样式富文本，粘贴后格式保留 |
| 发布配置齐全 | 首图 / 摘要 / 原创 / 合集 / 创作来源 / 赞赏，一个不少 |
| 能自测不留残留 | `testMode=draft`，自测走「发布草稿 → 删除」，避免自动群发/审核 |
| 机器可判读 | 发表后进入扫码/审核流程，终态定为 `SUBMITTED` 而非 `PUBLISHED` |

---

## 二、技术栈

公众号渠道同样基于「Playwright 主操作 + CDP 底层增强」的通用骨架，但在渠道层多了一个关键文件：

```text
scripts/channels/gzh/
├── index.js       # 业务流程：登录判定、进编辑器、写正文、发布配置、发表、删除
├── selectors.js   # 公众号 DOM 选择器集中维护（改版只改这里）
└── format.js      # 正文样式转换：Markdown → 公众号富文本（md.doocs.org）
```

**核心差异集中在 `format.js`**：它解决了公众号「不认 Markdown」的根本矛盾。

---

## 三、核心功能

### 3.1、动作矩阵

| 动作 | 命令 | 说明 |
| --- | --- | --- |
| 检测登录 | `gzh checkLogin` | 判定登录态（账号名 + 真实会话 Cookie 交叉验证） |
| 扫码登录 | `gzh login` | 打开后台扫码，登录态持久化 |
| 存草稿 | `gzh publishDraft` | 填标题/作者/正文 + 底部配置后保存草稿 |
| 发表 | `gzh publish` | 完整走发布配置并触发发表（群发需扫码） |
| 删除 | `gzh delete` | 草稿箱定位后删除，含确认与结果校验 |
| 链路自测 | `gzh test publish` | 发布草稿 → 删除（`testMode=draft`） |

### 3.2、登录态检测（checkLogin）

| 项 | 内容 |
| --- | --- |
| **核心原理** | 用「后台账号名」做主判定，再用「真实会话 Cookie」白名单交叉验证，避免把未登录态下发的 `wxuin`/`ua_id` 误当登录凭证 |
| **执行细节流程** | ① 进入后台首页 `mp.weixin.qq.com/cgi-bin/home` → ② 出现 `.weui-desktop_name`（账号名）则已登录 → ③ 否则读 Cookie 是否含 `slave_sid`/`slave_user`/`data_ticket` → ④ 都没有则视为未登录 → ⑤ 返回 `{loggedIn, user, cookieNames, loginEntryVisible, reason}` |

> 详细原理与代码见 [4.2](#42登录态判定账号名--真实会话-cookie)。

### 3.3、扫码登录（login）

| 项 | 内容 |
| --- | --- |
| **核心原理** | 未登录时首页出现 `#jumpUrl` 登录入口，点击进入扫码页；扫码成功后会话 Cookie 持久化到独立 profile |
| **执行细节流程** | ① 进入后台首页 → ② 有 `#jumpUrl` 则点击进入扫码登录页 → ③ 用户扫码 → ④ 脚本轮询 `slave_sid`/`slave_user`/`data_ticket` 判定成功 → ⑤ 登录态存到 `~/.blogloom-publisher/chrome-profile-gzh` |

### 3.4、正文样式转换（format.js）

| 项 | 内容 |
| --- | --- |
| **核心原理** | 公众号编辑器（ProseMirror）不识别 Markdown，先用 md.doocs.org 把 Markdown 渲染成带样式的 HTML 富文本，再粘贴回编辑器 |
| **执行细节流程** | ① 同一浏览器上下文新开转换页 `md.doocs.org` → ② 清空默认内容，把 Markdown 粘贴进 `.cm-content` → ③ 先把「图注」设为「不显示」（避免图片下方渲染图注文字）→ ④ 点「复制」把富文本写入剪贴板 → ⑤ 关闭转换页 → ⑥ 回主编辑页 `Mod+V` 粘贴 |

> 详细原理与代码见 [4.1](#41正文样式转换markdown--公众号富文本重点)。

### 3.5、进入编辑器（enterEditor）

| 项 | 内容 |
| --- | --- |
| **核心原理** | 公众号编辑器藏得深，且点「文章」可能新开标签页，需监听新页面并切换上下文 |
| **执行细节流程** | ① 进入后台首页 → ② 点「内容管理」→「草稿箱」→ ③ 点「新的创作」→「文章」→ ④ 用 `context.waitForEvent('page')` 监听，若新开标签页则切到新页面 → ⑤ 等 `.ProseMirror` 编辑器就绪 → ⑥ 判断是否被重定向到 `/loginpage/`，是则抛 `AUTH_REQUIRED` |

### 3.6、标题 / 作者写入（setTitle / setAuthor）

| 项 | 内容 |
| --- | --- |
| **核心原理** | 标题/作者是 contenteditable/ProseMirror，`fill` 不一定可靠，用键盘输入最稳；作者必须先填，否则原创弹窗「我已阅读」无法勾选 |
| **执行细节流程** | ① 定位标题 `.title-editor__input .ProseMirror`（兜底 `#title`）→ ② 全选清空 → ③ `keyboard.type` 逐字输入 → ④ 回读校验；作者同理写 `#js_author_area #author` → ⑤ 回读校验兜底 |

### 3.7、正文写入（setContent）

| 项 | 内容 |
| --- | --- |
| **核心原理** | 优先用 md.doocs 样式粘贴；粘贴后处理微信「内容结构检测」确认框；回读为空则回退原始写入策略 |
| **执行细节流程** | ① 可选：把公众号名片插入正文顶部 → ② 调 3.4 样式转换 → ③ 点正文编辑器 `Mod+V` 粘贴 → ④ 循环点「继续插入」处理内容结构检测框 → ⑤ 回读正文长度 → ⑥ 为 0 则回退 `writeContent(['paste','cdp','keyboard'])` |

### 3.8、发布配置项

| 配置 | 核心原理 | 执行细节流程 |
| --- | --- | --- |
| **首图** | 封面面板按正文顺序增量渲染，需等图片上传完再取第一张，并校验上传成功 | ① 底部定位 `#js_cover_area` → ② 悬停「从正文选择」→ ③ 轮询面板 `.appmsg_content_img_item` 数量达正文图总数 → ④ 取第一张 → ⑤ 「下一步」「完成」→ ⑥ 校验预览区出现 `background-image`（失败重试一次） |
| **摘要** | 直接填充文本域，超长自动裁剪 | ① 定位 `#js_description_area textarea` → ② 按 conf 上限（默认 120 字）`slice` → ③ `fill(text)` |
| **原创** | 微信自定义组件首次常不响应，需多轮重试 + 触发 DOM click 勾协议 | ① 点「原创」→ ② 点「文字原创」→ ③ 反复对 `<label>` 触发 DOM click 直到「我已阅读」`isChecked()` → ④ 「确定」→ ⑤ 校验弹窗关闭，失败则关弹窗下一轮再试（最多 4 轮） |
| **赞赏** | 需先声明原创；点开关会弹「赞赏设置」，选账户+勾协议+确定 | ① 等 `#js_reward_setting_area` 可见 → ② 点开关 → ③ 弹窗选「赞赏作者」+ 首个赞赏账户 + 勾协议 → ④ 确定 → ⑤ 校验 `isRewardOn`（整体加超时保护） |
| **付费** | 按产品要求不开启 | 跳过（保留默认关闭） |
| **合集** | 匹配同名合集，不存在则跳过不创建 | ① 点 `#js_article_tags_area` 标签 → ② 弹窗遍历 `.select-opt-li` 匹配同名 → ③ 点「确认」 |
| **创作来源** | 下拉 contains 匹配文案 | ① 点 `#js_claim_source_area` 描述 → ② 在选项里按「个人观点，仅供参考」匹配 → ③ 点「确认」 |

### 3.9、发表（publish）

| 项 | 内容 |
| --- | --- |
| **核心原理** | 发表可能触发群发（需管理员扫码），因此终态定为 `SUBMITTED` 而非 `PUBLISHED` |
| **执行细节流程** | ① 点底部「群发」`.mass_send` → ② 弹窗 1：非群发则关闭群发开关，点确认 → ③ 弹窗 2：二次确认「继续发表」→ ④ 等待成功 toast → ⑤ 命中返回 `PUBLISHED`，否则返回 `SUBMITTED` 并提示在浏览器完成扫码确认 |

### 3.10、存草稿 / 删除

| 功能 | 核心原理 | 执行细节流程 |
| --- | --- | --- |
| **存草稿** | 草稿场景也要先配封面/摘要/原创/合集/创作来源，再保存 | ① 关闭残留弹窗 → ② 调发布配置 → ③ 点「保存为草稿」`button:has-text("保存为草稿")` → ④ 返回 `DRAFT_SAVED` |
| **删除** | 进草稿箱定位文章卡片，删除后确认浮层，回查卡片消失 | ① 进「内容管理 → 草稿箱」→ ② 按 appmsgid/标题定位 `.weui-desktop-card` → ③ 悬停 → ④ 点行内「删除」→ ⑤ 确认浮层点「删除」→ ⑥ 轮询回查卡片消失 → ⑦ 返回 `DELETED`（未确认则 `DELETE_UNVERIFIED`） |

### 3.11、渠道专属参数透传（augmentPayload）

| 项 | 内容 |
| --- | --- |
| **核心原理** | 核心 CLI 不感知渠道业务，`--key value` 原样透传，由渠道 `augmentPayload` 组装，优先级「CLI > 元数据 extra > conf」 |
| **执行细节流程** | ① 解析 `--author`/`--wechat-name`/`--collection`/`--group-send` → ② 与 `blog.extra`、`conf` 逐级取优先值 → ③ 返回 `{author, wechatName, collection, groupSend}` 供发布/配置使用 |

---

## 四、核心功能设计思路与原理

这一章逐段拆解公众号渠道最核心的机制。

### 4.1、正文样式转换：Markdown → 公众号富文本（重点）

**问题**：公众号编辑器不识别 Markdown，直接粘贴只会得到纯文本，排版全丢。

**方案**：参照已跑通的 `auto-sync-blog`，用在线排版工具 **md.doocs.org** 先把 Markdown 渲染成带内联样式的 HTML，复制该富文本，再粘贴回公众号编辑器。

```js
async function convertMarkdownToGzhHtml(page, markdown) {
  // 1. 在同一浏览器上下文新开一个转换页（转换完即关闭，不影响主编辑页）
  const converter = await context.newPage();
  await converter.goto('https://md.doocs.org/', { waitUntil: 'domcontentloaded' });
  const editor = converter.locator('.cm-content');
  await editor.waitFor({ state: 'visible' });

  // 2. 清空默认内容，粘贴 Markdown（走剪贴板，避免超长文本逐字输入的性能问题）
  await converter.keyboard.press(`${MOD}+A`);
  await converter.keyboard.press('Backspace');
  await converter.evaluate((v) => navigator.clipboard.writeText(v), markdown);
  await converter.keyboard.press(`${MOD}+V`);

  // 3. 点「复制」，把渲染后的富文本写入剪贴板
  await converter.locator('button:has-text("复制")').click();
  return { converted: true };
}
```

**关键点**：

- **同上下文新开页**：转换页与主编辑页共享 Cookie/登录态，且转换完 `close()`，不污染主流程；
- **走剪贴板而非逐字输入**：长文逐字输入有性能问题，剪贴板粘贴一次到位；
- **复制前先把「图注」设为「不显示」**：md.doocs 默认会在图片下方渲染图注文字，公众号正文不需要，需先在右侧样式面板选中「不显示」。

**图注隐藏的实现细节**：样式面板默认收起，看不到「图注」区块时先点右上角「样式」展开；用 XPath 先锁定「图注/Caption」标题，再取其父区块内的「不显示/None」按钮，避免误点其他区块的同名按钮；选中态用 `border-primary` class 校验：

```js
const cls = (await hideBtn.getAttribute('class')) || '';
return /border-primary/.test(cls);
```

### 4.2、登录态判定：账号名 + 真实会话 Cookie

**问题**：公众号登录凭证复杂，怎么避免「假阳性」？

**原理**：双保险判定——

1. 进入后台首页，出现账号名 `.weui-desktop_name` → 已登录；
2. 否则看**真正的会话 Cookie**（`slave_sid` / `slave_user` / `data_ticket`）——它们**仅在登录后下发**；
3. 都没有则视为未登录。

**重点**：不能用 `wxuin` / `ua_id` 判定——**未登录态也会下发**，会假阳性。登录凭证白名单只放 `LOGIN_COOKIE_NAMES = ['slave_sid', 'slave_user', 'data_ticket']`。

### 4.3、进入编辑器：内容管理 → 草稿箱 → 新的创作

**问题**：公众号编辑器藏得深，且「文章」可能新开标签页打开。

**原理**：路由链路——后台首页 → 内容管理 → 草稿箱 → 「新的创作」→ 「文章」。点「文章」时**用 `context.waitForEvent('page')` 监听新标签页**，若新开则切换到新页面继续操作：

```js
const newPage = await Promise.all([
  context.waitForEvent('page', { timeout: 15000 }).catch(() => null),
  addBtn.click({ timeout: 8000 }).catch(() => {}),
]).then(([np]) => np);
if (newPage) editorPage = newPage;
```

同时判断是否被重定向到登录页（`/loginpage/`），是则抛 `AUTH_REQUIRED`。

### 4.4、标题 / 作者写入

标题编辑器是 contenteditable/ProseMirror，`fill` 不一定可靠，**用键盘输入最稳**：全选清空 → `keyboard.type` 逐字输入 → 回读校验。

作者栏（`#js_author_area #author`）是**必须先填的**——不填作者，后续原创弹窗的「我已阅读」就无法勾选。同样全选替换 + 回读校验兜底。

### 4.5、正文写入：样式粘贴 + 回退兜底

```js
async function setContent(page, markdown, ctx) {
  // 1. 可选：把公众号名片插入正文顶部
  if (profileName) await insertProfileCard(page, profileName);

  // 2. 优先用 md.doocs 样式转换
  const converted = await convert(page, markdown);

  // 3. 粘贴进 ProseMirror
  await page.keyboard.press(`${MOD}+V`);

  // 4. 处理微信的「内容结构检测」确认框 → 点「继续插入」才真正写入
  await confirmInsertDialog(page);

  // 5. 回读长度；为 0 则回退为原始 Markdown 写入策略
  if (length === 0) return await writeContent(page, editor, markdown, ['paste', 'cdp', 'keyboard']);
}
```

**重点**：微信粘贴外部富文本后会弹「内容结构检测」确认框，**必须点「继续插入」**，否则内容不会真正写进正文。同时保留原始 `writeContent` 兜底，样式转换失败时仍能发布。

### 4.6、首图：从正文选择 + 上传校验

**问题**：公众号封面面板按正文顺序增量渲染，图片没传完时第一张并不是正文首图。

**原理**：`setCover(page, firstBox, expectedImages)`——传正文图片总数，轮询面板 item 数量直到达到期望值（或连续若干秒不再增长），再取第一张：

```js
async function waitForContentImages(page, panel, expected, timeoutMs = 45000) {
  while (Date.now() < deadline) {
    const count = await panel.locator('.appmsg_content_img_item').count();
    if (count >= expected) return count;
    if (count === last) { stable++; if (stable >= 4 && count > 0) return count; }
    else stable = 0;
    last = count;
    await page.waitForTimeout(1000);
  }
}
```

然后「下一步」→「完成」，最后**校验封面是否真上传成功**（预览区出现 `background-image` 且非空 url）；失败重试一次：

```js
async function isCoverUploaded(page) {
  const style = (await preview.getAttribute('style')) || '';
  return style.includes('background-image') && !style.includes('url("")');
}
```

### 4.7、原创：声明类型 + 勾协议（多重试）

**问题**：微信的自定义组件首次打开常不响应，且「我已阅读」勾选失败会导致声明不生效。

**原理**：`chooseOrigin` 采用**多轮重试**——最多 4 轮，每轮打开弹窗 → 点「文字原创」→ 反复勾「我已阅读」直到 `isChecked()` 为真 → 确定 → 校验弹窗是否已关闭；失败则关弹窗下一轮再试：

```js
for (let i = 0; i < 6 && !checked; i++) {
  await agreeLabel.evaluate((el) => el.click());
  checked = await agreeInput.isChecked();
}
```

勾选对 `<label>` 触发 DOM click，重试直到真勾上。

### 4.8、发表：群发弹窗 + 二次确认 + 扫码

**问题**：公众号发表可能触发群发（需管理员扫码），终态如何界定？

**原理**：`publish` 流程——点底部「群发」→ 弹窗 1（非群发则关闭群发开关）→ 确认 → 弹窗 2（二次确认「继续发表」）→ 等待成功 toast。群发需要扫码，**给出一段确认窗口**，返回：

```js
return published
  ? { status: 'PUBLISHED', url: '' }
  : { status: 'SUBMITTED', url: '', message: '已提交发表，如需群发请在浏览器中完成管理员扫码确认' };
```

**重点**：公众号发表后可能进入扫码/审核，所以**终态是 `SUBMITTED` 而不是 `PUBLISHED`**（通过 `channel.successStatuses = ['SUBMITTED']` 声明），避免机器误判失败。

### 4.9、渠道专属参数透传

公众号的 `--author` / `--wechat-name` / `--collection` / `--group-send` 等参数，核心 CLI **不感知**，统一 `--key value` 原样透传给渠道。渠道用 `augmentPayload` 组装，优先级：**CLI 参数 > 元数据 extra > conf 默认**：

```js
author: opts.author || params.author || extra.author || extra.username || CONF.author || '',
wechatName: opts.wechatName || extra.wechatName || CONF.wechatName || '',
groupSend: /* opts → params → extra → conf 逐级 */
```

---

## 五、实战：发布与链路自测

### 5.1、存草稿（推荐首测）

```bash
node scripts/publisher.js gzh publishDraft --file "/abs/blog.md" --author "长路" --wechat-name "长路Java"
```

草稿场景也会先配置封面/摘要/原创/合集/创作来源（`configureOnDraft: true`），再保存草稿。

### 5.2、正式发表

```bash
node scripts/publisher.js gzh publish --file "/abs/blog.md" --wechat-name "长路Java"
```

默认有头模式可见全过程；未登录自动拉起登录；发表后若需群发则在浏览器完成管理员扫码。

### 5.3、链路自测：gzh test publish

公众号自测用**草稿模式**，避免自动群发/审核：

```bash
node scripts/publisher.js gzh test publish --file "/abs/blog.md"
```

返回 `status=DRAFT_SAVED_AND_DELETED`（`channel.testMode='draft'` 控制）。

---

## 六、踩坑与优化（重点复盘）

### 6.1、坑一：直接粘贴 Markdown 变纯文本

公众号编辑器不识别 Markdown，直接粘贴只得到纯文本，`#`、`*`、反引号全暴露。

* **优化**：用 md.doocs 先把 Markdown 渲染成带样式的 HTML 再粘贴。

### 6.2、坑二：粘贴后内容没进去（内容结构检测）

微信粘贴外部富文本后弹「内容结构检测」确认框，不点「继续插入」内容不会写入正文。

* **优化**：`confirmInsertDialog` 循环点「继续插入」，并回读正文长度，为 0 则回退原始写入策略。

### 6.3、坑三：登录「假阳性」

未登录态也会下发 `wxuin` / `ua_id`，用它们判定会把「未登录」误判为「已登录」。

* **优化**：登录凭证白名单只放 `slave_sid` / `slave_user` / `data_ticket`。

### 6.4、坑四：首图选错/选空

封面面板按正文顺序增量渲染，图片没传完时第一张不是正文首图。

* **优化**：传图片总数，轮询面板 item 数量直到达到期望值再取第一张，并校验上传成功（失败重试一次）。

### 6.5、坑五：原创声明不生效

微信自定义组件首次打开不响应，「我已阅读」勾选失败导致声明不生效。

* **优化**：多轮重试 + 对 `<label>` 触发 DOM click，反复校验 `isChecked()` 直到真勾上。

### 6.6、坑六：发表终态判断

发表后可能进入扫码/审核，返回 `PUBLISHED` 会导致机器误判失败。

* **优化**：终态定为 `SUBMITTED`（`successStatuses: ['SUBMITTED']`），发表后给出扫码确认窗口。

**重点**：公众号的坑集中在「**内容不进去**」和「**状态假成功**」两类，应对手段是「回读校验」+「重试」+「兜底回退」。

---

## 七、验证测试

### 7.1、mock 单测

用 `test/helpers/mock-page.js` 模拟 Playwright，**无需真实浏览器**即可验证：

```bash
npm test              # 全部单测（核心 + 所有渠道）
npm run test:channels # 仅渠道：csdn / gzh
```

覆盖 gzh 的 `checkLogin / enterEditor / setTitle / setAuthor / setContent / preparePublish / publish / saveDraft / deleteBlog / augmentPayload` 等每一个脚本工具。

### 7.2、真实链路自测

`gzh test publish` 走「发布草稿 → 删除」，验证完整链路且不触发群发、不留残留。

---

## 八、总结

公众号渠道的引入，验证了「按渠道分层」这套骨架的**可扩展性**：复用 Playwright + CDP、登录判定、CLI 契约等通用能力，只在 `channels/gzh/` 里新增渠道专属逻辑（`format.js` 样式转换、`augmentPayload`、`testMode`、`successStatuses`）。

核心收获：

* **编辑器不认 Markdown 就转 HTML**：md.doocs 渲染 + 剪贴板富文本；
* **内容不进就回退兜底**：回读校验 + 原始写入策略；
* **状态不进就定 SUBMITTED**：扫码/审核流程不该被当成失败；
* **一切关键动作都要回读校验**：拒绝「假成功」。

> 项目地址：https://github.com/changluya/BlogLoom ｜ Skill 目录：https://github.com/changluya/BlogLoom/tree/master/skills/blogloom-publisher-skill

---

# 参考资料

[1]. [md.doocs.org：Markdown 公众号排版工具](https://md.doocs.org/)

[2]. [Playwright 官方文档（Locators、Actions、Page Context）](https://playwright.dev/docs/api/class-locator)

[3]. [Chrome DevTools Protocol（Input.insertText / Network.getCookies）](https://chromedevtools.github.io/devtools-protocol/)

[4]. [Playwright connectOverCDP：复用已有浏览器登录态](https://playwright.dev/docs/api/class-browsertype#browser-type-connect-over-cdp)

---

整理者：长路 创建时间：2026.10.6 更新时间：2026.10.6