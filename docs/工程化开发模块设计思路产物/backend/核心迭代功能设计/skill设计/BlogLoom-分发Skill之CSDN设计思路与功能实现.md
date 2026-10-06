---
title: "BlogLoom 分发 Skill 02：CSDN 渠道分发 Skill 设计思路与实现"
description: 从「一句话把本地 Markdown 发到 CSDN」出发：登录态判定、标题/正文写入、发布弹窗逐项配置、删除与链路自测的完整拆解。
---

# 一、背景：为什么单独为 CSDN 做一个分发 Skill

## 1.1、场景引入

在开发 BlogLoom 的多渠道分发能力时，我们遇到了一个非常具体的问题：**同一篇本地 Markdown 博客，除了留在 BlogLoom 平台，还要分发到 CSDN**。

CSDN 是很多 Java / 后端开发者最常打理的站外渠道。它不像自建平台那样提供稳定的发布接口，而是典型的「前端富文本编辑器 + 私有发布协议」。手动搬运的痛点非常集中：

* **字段多且隐蔽**：标题默认是展示态、标签/分类专栏藏在发布弹窗里、首图要等异步加载；
* **改版频繁**：CSDN 前端迭代快，选择器经常变；
* **容易「假成功」**：点了发布/删除，实际没生效，肉眼难发现。

于是，我们把「**发布到 CSDN**」这一整套重复点击，沉淀成 `blogloom-publisher-skill` 里的一个独立渠道分发 Skill。

## 1.2、为什么单独成篇，而不是塞进开篇

开篇《BlogLoom 分发 Skill 01》讲的是**跨渠道通用的骨架**（Playwright + CDP 选型、三层分层、统一 CLI 契约）。而 CSDN 作为**第一个、也是唯一先跑通真实发布的渠道**，其 DOM 结构、弹窗交互、坑点都值得单独成篇：

| 维度 | 01 篇（通用骨架） | 本篇（CSDN 专属） |
| --- | --- | --- |
| 定位 | 所有渠道共用的地基 | CSDN 一个渠道的实现细节 |
| 内容 | 选型、分层、契约 | 选择器、弹窗配置、坑与校验 |
| 复用 | 新增渠道照搬 | 只服务 csdn 这一个 channel |

## 1.3、目标与范围

一句话描述本篇目标：

> 把符合统一 SOP 的本地 Markdown，用一句话分发到 CSDN，且发布/删除结果可被机器校验，绝不「假成功」。

涉及的完整动作：`checkLogin` / `login` / `publishDraft` / `publish` / `delete` / `test publish`。

---

# 二、技术栈：站在通用骨架之上

## 2.1、Playwright 主操作 + CDP 底层增强

CSDN 分发 Skill 复用开篇确立的技术栈，不另起炉灶：

| 层次 | 技术 | 在 CSDN 上的职责 |
| --- | --- | --- |
| 主操作 | Playwright | 可见浏览器、真实点击/输入/粘贴、进入编辑器、打开发布弹窗、逐项配置 |
| 底层增强 | CDP | 复用登录态（`connectOverCDP`）、`Input.insertText` 兜底富文本写入、读取 HttpOnly Cookie 判定登录 |
| 页面结构 | `selectors.js` | 集中维护 CSDN 全部 DOM 选择器，改版只改这里 |

## 2.2、目录结构

CSDN 渠道代码全部收敛在两个文件里，职责单一：

```text
scripts/channels/csdn/
├── index.js       # 业务流程：登录判定、进编辑器、填字段、发布、删除
└── selectors.js   # CSDN DOM 选择器集中维护（改版只改这里）
```

`selectors.js` 里一个关键点：**标签容器 `tagContainer` 已经是 `.mark_selection`，子选择器不要再带 `.mark_selection` 前缀**，否则 `container.locator('.mark_selection …')` 永远匹配不到后代。这是踩出来的坑。

---

# 三、核心功能：CSDN 分发 Skill 能做什么

## 3.1、动作矩阵

| 动作 | 命令 | 说明 |
| --- | --- | --- |
| 检测登录 | `csdn checkLogin` | 判定登录态，返回是否已登录、是否出现滑块 |
| 扫码登录 | `csdn login` | 打开浏览器扫码，登录态持久化到独立 profile |
| 存草稿 | `csdn publishDraft` | 填标题/正文后保存草稿，不走发布弹窗 |
| 发布 | `csdn publish` | 完整走发布弹窗并最终发布，返回文章链接 |
| 删除 | `csdn delete` | 内容管理页定位后删除，含二次确认与结果校验 |
| 链路自测 | `csdn test publish` | 真实发布 → 立即删除，验证链路且不留残留 |

## 3.2、登录态检测（checkLogin）

| 项 | 内容 |
| --- | --- |
| **核心原理** | 用「首页登录入口文案」做主判定，再用「真正的登录 Cookie」交叉验证，避免把匿名态 Cookie 误当登录凭证 |
| **执行细节流程** | ① 进入首页 `https://www.csdn.net/` → ② 读 `.toolbar-btn-loginfun` 文案，含「登录」则未登录 → ③ 读 Cookie 是否同时含 `UserToken` + `UserInfo` → ④ 探测是否出现滑块 `.verify-move-block` → ⑤ 返回 `{loggedIn, user, hasSlider, loginEntryVisible}` |

> 详细原理与代码见 [4.1](#41登录态判定文案--cookie-交叉验证)。

## 3.3、扫码登录（login）

| 项 | 内容 |
| --- | --- |
| **核心原理** | 打开有头登录页让用户扫码，脚本轮询登录 Cookie 判定成功，登录态持久化到独立 profile |
| **执行细节流程** | ① 打开 `passport.csdn.net/login`（有头）→ ② 用户扫码/输入账号（可能含滑块）→ ③ 每 2s 轮询 Cookie 是否含 `UserToken`/`UserInfo` → ④ 命中即成功（默认最长 10 分钟）→ ⑤ 登录态存到 `~/.blogloom-publisher/chrome-profile-csdn` |

## 3.4、发布（publish）：完整发布链路

| 项 | 内容 |
| --- | --- |
| **核心原理** | 进入编辑器 → 填标题/正文 → 打开发布弹窗逐项配置 → 点最终发布 → 抓取文章链接 |
| **执行细节流程** | ① 首页点「创作」进 `editor.csdn.net/md`（失败回退直开）→ ② 先点 `.article-bar__title-display` 唤出隐藏 input 填标题 → ③ 清空正文模板，按 `paste → cdp → keyboard` 写正文，含图片时按数量估算等待 → ④ 点「发布文章」`button.btn-publish` 打开弹窗 → ⑤ 依次配置标签/首图/摘要/分类专栏/类型/可见范围/创作声明 → ⑥ `--dry-run` 则返回 `DRY_RUN` 及 `publishConfig` → ⑦ 否则点弹窗底部发布按钮，从成功弹窗 `a.success-modal-btn[href*=/article/details/]` 取链接，返回 `PUBLISHED` |

### 3.4.1、发布弹窗内的逐项配置

| 配置项 | 核心原理 | 执行细节流程 |
| --- | --- | --- |
| **标签** | 标签输入框在点「添加文章标签」后才出现 | ① 幂等打开选择框 → ② 清空残留标签 → ③ 逐个 `fill + Enter`（≤10 个，每次重新解析输入框）→ ④ 关闭选择框 → ⑤ 回读 `.mark_selection_box_el_tag` 校验，写入 `selectedTags/missingTags` |
| **首图** | 首图列表异步加载，需等图片上传完再选 | ① 等 `.img-selection-item img.select-cover` 可见 → ② 取第一张点击 → ③ 点确认 `.vicp-operate-btn` → ④ 返回 `coverSet` |
| **摘要** | 直接填充文本域 | ① 定位 `.desc-box .el-textarea__inner` → ② `fill(summary)` |
| **分类专栏** | 勾选项是隐藏 checkbox，匹配用评分制 | ① 定位「分类专栏」`.form-entry` → ② 清空已选 → ③ 对每个专栏按评分匹配最优项 → ④ 用 DOM `click()` 触发隐藏 checkbox → ⑤ 关闭下拉 → ⑥ 回读 `.tag__item-box`，返回 `resolved/unresolved/selected` |
| **文章类型** | 单选按钮按 value 定位 | 定位 `input[value=original]` 点击（原创） |
| **可见范围** | 单选按钮按 value 定位 | 定位 `input[value=public]` 点击（公开） |
| **创作声明** | 下拉 contains 匹配选项 | ① 点 `.creation-statement-select` 触发 → ② 在选项里按「个人观点，仅供参考」contains 匹配 → ③ 点击并返回选中项 |

## 3.5、存草稿（publishDraft）

| 项 | 内容 |
| --- | --- |
| **核心原理** | 填标题/正文后直接点编辑器顶部「保存草稿」，不走发布弹窗 |
| **执行细节流程** | ① 进编辑器填标题/正文 → ② 点「保存草稿」`button.btn-save` → ③ 返回 `status=DRAFT_SAVED`（标签/摘要/分类等草稿阶段不设置，后续发布时补） |

## 3.6、删除（delete）

| 项 | 内容 |
| --- | --- |
| **核心原理** | 进内容管理页定位文章行 → 二次确认删除 → 回查该行已消失，拒绝「假成功」 |
| **执行细节流程** | ① 进入 `mp.csdn.net/mp_blog/manage` → ② 按 id（浏览链接 `/article/details/<id>`）或 title 定位行 → ③ 悬停行右侧「...」→ ④ 下拉点「删除」→ ⑤ 二次确认弹窗点「确定」→ ⑥ 回查该行已从列表消失 → ⑦ 返回 `DELETED`（未确认则 `DELETE_UNVERIFIED`） |

## 3.7、链路自测（test publish）

| 项 | 内容 |
| --- | --- |
| **核心原理** | 组合「真实发布 + 立即删除」，验证链路可用且不留残留 |
| **执行细节流程** | ① 执行 `publish` → ② 拿到文章 URL/id → ③ 立即执行 `delete` → ④ 返回 `status=PUBLISHED_AND_DELETED` |

## 3.8、CSDN 字段到渠道的映射

| SOP 字段 | CSDN 目标 |
| --- | --- |
| `title` | 文章标题 |
| `tags`（逗号分隔，取前 10） | 文章标签 |
| `columns`（逐个匹配已有专栏） | 分类专栏 |
| `articleSummary` | 文章摘要 |
| `![coverImg](url)` | 封面/首图 |
| `content`（去掉元数据块后的正文） | 正文（Markdown 源码） |

---

# 四、核心功能设计思路与原理

这一章是整篇的重点，逐段拆解 CSDN 分发 Skill 的关键机制。

## 4.1、登录态判定：文案 + Cookie 交叉验证

**问题**：怎么知道当前浏览器是否已登录 CSDN？

**方案**：靠「首页登录入口文案」做主判定，再用「真正的登录 Cookie」做交叉验证。

```js
async function checkLogin(page, ctx = {}) {
  // 1. 找到 .toolbar-btn-loginfun，若文案含「登录」→ 未登录
  const loginFun = page.locator(SELECTORS.homeLoginFun).first();
  const loginText = (await loginFun.innerText() || '').trim();
  let loggedIn = !loginText.includes('登录') && loginText.length > 0;

  // 2. 交叉验证：Cookie 是否含 UserToken + UserInfo
  if (!loggedIn) {
    const cookies = await ctx.cookies('https://www.csdn.net');
    loggedIn = cookies.some(c => c.name === 'UserToken') &&
               cookies.some(c => c.name === 'UserInfo');
  }
  return { loggedIn, hasSlider: /* .verify-move-block 是否存在 */ };
}
```

**重点**：**不能把 `uuid_tt_dd` 这类匿名 Cookie 当作登录凭证**——CSDN 匿名态也会下发它，会导致「假阳性」（详见 5.1 的坑）。

## 4.2、进入编辑器与标题写入

**问题**：CSDN 的标题输入框是隐藏的，怎么填？

**原理**：标题默认显示为 `.article-bar__title-display`（【无标题】），**点击它之后才显示隐藏的 `input.article-bar__title`**。所以要先点展示态、唤出 input、再写入并回读校验：

```js
async function setTitle(page, title) {
  await clickIf(page.locator(SELECTORS.titleDisplay).first());   // 点展示态唤出 input
  const input = page.locator(SELECTORS.titleInput).first();
  await input.waitFor({ state: 'visible' });
  await input.fill(title);
  // 兜底：fill 未生效则全选替换
  if ((await input.inputValue()) !== title) {
    await page.keyboard.press(`${MOD}+A`);
    await page.keyboard.type(title, { delay: 10 });
  }
}
```

## 4.3、正文写入：清空 + 多策略粘贴

**问题**：CSDN 编辑器预置了欢迎模板，且富文本粘贴可能被拦截，怎么把 Markdown 正文可靠写入？

**原理**：四步走——

1. **清空**：`Cmd/Ctrl + A` 全选，`Backspace` 删除默认欢迎内容；
2. **多策略写入**：按 `paste → cdp → keyboard` 依次尝试——剪贴板粘贴最贴近真人（优先），失败用 CDP `Input.insertText` 兜底（绕过 paste 事件拦截），最后键盘逐字；
3. **回读校验**：每尝试一个策略就读回内容，非空才算成功；
4. **图片等待**：正文含图片时按图片数量估算等待时间，给 CSDN 异步上传/解析图片留足时间（否则发布弹窗的首图列表是空的）。

```js
function estimateImageWaitMs(content, { base = 2000, blockSize = 5, perBlock = 3000, max = 60000 } = {}) {
  const count = countImages(content);
  if (count === 0) return 0;
  return Math.min(base + Math.ceil(count / blockSize) * perBlock, max);
}
```

## 4.4、标签配置：先打开选择框再填

**问题**：CSDN 的标签输入框不是一开始就可见的，直接 `fill` 会找不到元素。

**原理**：标签输入框 `.el-autocomplete input.el-input__inner` 只有在**点过「添加文章标签」`.tag__btn-tag` 打开选择框之后才出现**。所以流程是：

1. 幂等打开选择框（输入框可见即视为已打开）；
2. 清空草稿/上一次运行残留的标签；
3. 逐个 `fill + Enter`（每次重新解析输入框，避免异步替换导致丢标签）；
4. 关闭选择框；
5. **回读 `.mark_selection_box_el_tag` 校验实际生效的标签**，写入 `selectedTags / missingTags`。

## 4.5、分类专栏：评分匹配 + DOM click 勾选

**问题**：分类专栏有两层坑——勾选项是隐藏的 checkbox 打不到，且「包含」匹配会误配宽泛子类。

**原理一（匹配用评分制）**：对每个已有专栏选项打分，取最高分——

* 精确相等：100000 分；
* 已有专栏包含设置值：10000 + 名称长度 分；
* 设置值包含已有专栏：1000 + 名称长度 分。

这样 `Maven&Gradle` 不会被宽松地误配到子类 `Maven`。

**原理二（勾选触发 DOM click）**：分类专栏的勾选项是**隐藏的 `<input type=checkbox>`**，普通 `click()` 打不到（看着选中了实际没生效）。需要先判断 `checked` 避免反选，再用 `evaluate((el) => el.click())` 触发 DOM click：

```js
const chk = option.locator(SELECTORS.categoryOptionCheckbox).first();
const already = await chk.evaluate((el) => !!(el && el.checked));
if (!already) await chk.evaluate((el) => el && el.click());   // 关键：DOM click
```

最后**回读 `.tag__item-box`** 校验实际勾选结果，返回 `resolved / unresolved / selected`。

## 4.6、封面/首图：等异步加载再选第一张

**问题**：发布弹窗的「已有图片列表」是异步加载的，点开弹窗马上找 `.img-selection-item` 经常是空的。

**原理**：因为 4.3 已经按图片数量估算过等待时间，首图列表才有内容。取**第一张**作为首图，点击后再点确认按钮 `.vicp-operate-btn`，返回 `coverSet: true/false`。列表为空则跳过，不影响正文发布。

## 4.7、删除：二次确认 + 结果校验（拒绝「假成功」）

**问题**：怎么保证删除真的生效，而不是「看起来删了」？

**原理**：进入内容管理页 `https://mp.csdn.net/mp_blog/manage` → 按 id/title 定位行 → 悬停行右侧「...」→ 下拉点「删除」→ **二次确认弹窗点「确定」** → **回查该行已从列表消失**：

```js
const remaining = await findManageRow(page, target).count();
if (remaining > 0) return { status: 'DELETE_UNVERIFIED' };
return { status: 'DELETED' };
```

**重点**：自动化最危险的错误不是「报错」，而是「**看起来成功**」。所以每个关键动作后都要有**回读校验**。

## 4.8、统一 CLI 契约与机器可读输出

**问题**：Agent / 脚本怎么稳定地调用并解析结果？

**原理**：所有动作统一入口，stdout 只输出一个 JSON（日志走 stderr），便于解析：

```bash
node scripts/publisher.js csdn publish --file "/abs/blog.md"
```

```json
{ "ok": true, "channel": "csdn", "action": "publish",
  "data": { "status": "PUBLISHED", "url": "https://blog.csdn.net/…/details/123",
            "publishConfig": { "tags": ["Maven", "spotless"],
                               "selectedCategories": ["项目管理工具", "# Maven&Gradle"],
                               "coverSet": true,
                               "creationStatement": "个人观点，仅供参考" } },
  "error": null }
```

`status` 约定：`PUBLISHED`（发布）/ `DRAFT_SAVED`（草稿）/ `DRY_RUN`（仅填写）/ `DELETED`（删除并校验）/ `DELETE_UNVERIFIED`（未确认）。

---

# 五、实战：发布与链路自测

## 5.1、干跑验证：`--dry-run`

发布是**对外可见**的写操作，建议先干跑验证字段填写无误：

```bash
node scripts/publisher.js csdn publish --file "/abs/blog.md" --dry-run
```

它走完所有填写与弹窗配置，但**不点最终发布**，返回 `status=DRY_RUN` 与 `publishConfig`（生效的写入策略、标签回读、命中的专栏、封面是否设置等）。据此判断各字段是否写入成功。

## 5.2、正式发布

```bash
node scripts/publisher.js csdn publish --file "/abs/blog.md"
```

默认**有头模式**，发布全过程可见；未登录时自动拉起浏览器让你扫码，登录完成后继续。

## 5.3、链路自测：`csdn test publish`

Mock 能测逻辑，但测不出「页面改版」。提供一个组合自测——**真实发布 → 立即删除**：

```bash
node scripts/publisher.js csdn test publish --file "/abs/blog.md"
```

返回 `status=PUBLISHED_AND_DELETED`，用于验证分发链路是否可用且不残留内容。

## 5.4、选择器维护

CSDN 站点改版频繁。所有 DOM 选择器集中在 `scripts/channels/csdn/selectors.js`，改版时**只改这里**。改动后用 `--dry-run` 验证即可。

---

# 六、踩坑与优化（重点复盘）

以下是 CSDN 渠道真机跑出来的坑，每一条都值得记住。

## 6.1、坑一：登录被「假阳性」

最初用 `UserToken/UserInfo/uuid_tt_dd/cnt_w` 判定登录，结果 CSDN **匿名态也下发 `uuid_tt_dd`**，导致未登录被误判为已登录，随后编辑器直接跳到登录页。

* **优化**：只认真正的登录凭证 `UserToken`/`UserInfo`，并在编辑器阶段判断是否被重定向到 `passport.csdn.net/login`，是则抛 `AUTH_REQUIRED`。

## 6.2、坑二：标签输入框隐藏

`.el-autocomplete input.el-input__inner` 直接 `waitFor(visible)` 超时——它要**先点「添加文章标签」打开选择框后才出现**。

* **优化**：先点 `.tag__btn-tag` 打开选择框，再定位输入框写入。

## 6.3、坑三：分类专栏勾选无效

分类专栏的勾选项是隐藏的 `<input type=checkbox>`，普通 `click()` 打不到。

* **优化**：先判断 `checked` 避免反选，再用 DOM `click()` 触发。

## 6.4、坑四：分类专栏误匹配

设置值 `Maven&Gradle` 用「包含」反向匹配时，被宽松匹配到子类 `Maven`。

* **优化**：匹配改为评分制（精确 > 已有包含设置值 > 设置值包含已有），实测正确命中二级项 `# Maven&Gradle`。

## 6.5、坑五：首图列表异步加载

刚点开发布弹窗就找首图项，经常是空的。

* **优化**：正文含图片时按数量估算等待时间，给足图片上传/解析时间，再点发布。

## 6.6、坑六：删除「假成功」

CSDN 确认弹窗类名是 `.el_mcm-message-box`（**带下划线前缀 `el_mcm`**），而选择器写成 `.el-message-box`，导致「确定」根本没点到，却返回了 `DELETED`。

* **优化**：修正确认按钮为 `.btn-msg-confirm`，并**增加结果校验**——回查列表该行必须消失，否则返回 `DELETE_UNVERIFIED`。

**重点**：回顾这些坑，规律高度一致——**CSDN 的元素要么隐藏、要么异步、要么类名带下划线**。应对手段也只有两条：**先触发展示再定位**、**关键动作后回读校验**。

---

# 七、验证测试：把不稳定关进笼子

## 7.1、mock 单测

用 `test/helpers/mock-page.js` 模拟 Playwright（`locator/click/fill/hover/filter/waitFor/evaluate` 等子集），**无需真实浏览器**即可对每个工具做行为验证：

```bash
npm test              # 全部单测（核心 + 所有渠道）
npm run test:core     # 仅核心：CLI / markdown / channel 加载
npm run test:channels # 仅渠道：csdn / gzh
```

覆盖 CSDN 的 `checkLogin / enterEditor / setTitle / setContent / preparePublish / publish / saveDraft / deleteBlog` 等每一个脚本工具。

## 7.2、真实链路自测

`csdn test publish` 执行「真实发布 → 立即删除」，验证完整链路且不留残留内容。

**重点**：mock 单测守逻辑，真实链路自测守改版，两者缺一不可。

---

# 八、总结

CSDN 分发 Skill 是 BlogLoom 多渠道分发的**第一块、也是踩坑最全的一块拼图**。核心收获可以浓缩为：

* **登录判定要交叉验证**：别把匿名 Cookie 当登录凭证；
* **隐藏元素先触发再定位**：标题、标签输入框都要先点出展示态；
* **异步加载要估算等待**：图片越多等待越久；
* **匹配用评分制**：避免误配宽泛子类；
* **一切关键动作都要回读校验**：拒绝「假成功」。

这套「Playwright 主操作 + CDP 底层增强 + 一渠道一 SOP + 选择器独立 + 回读校验」的 CSDN 方案，可以直接复用到掘金、公众号等其他渠道。

> 项目地址：https://github.com/changluya/BlogLoom ｜ Skill 目录：https://github.com/changluya/BlogLoom/tree/master/skills/blogloom-publisher-skill

---

# 参考资料

[1]. [Playwright 官方文档（Locators、Actions）](https://playwright.dev/docs/api/class-locator)

[2]. [Chrome DevTools Protocol（Input.insertText / Network.getCookies）](https://chromedevtools.github.io/devtools-protocol/)

[3]. [Playwright connectOverCDP：复用已有浏览器登录态](https://playwright.dev/docs/api/class-browsertype#browser-type-connect-over-cdp)

---

整理者：长路 创建时间：2026.10.6 更新时间：2026.10.6