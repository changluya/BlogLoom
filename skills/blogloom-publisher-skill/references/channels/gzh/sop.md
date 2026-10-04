# 公众号（gzh）渠道 SOP

> 渠道实现：`scripts/channels/gzh/`（`index.js` 流程 + `selectors.js` 选择器 + `format.js` 正文样式转换）
> 通用契约：见 [../../00-tool-contract.md](../../00-tool-contract.md)
> 流程参照已跑通的 `auto-sync-blog` 微信公众号实现，并**经真机（真实公众号后台）验证**。

公众号渠道包含：`checkLogin`（检测登录）、`login`（扫码登录）、`publishDraft`（保存草稿）、`publish`（发表，需管理员扫码/审核）、`delete`（草稿箱定位后删除）、`test publish`（**发布草稿 → 删除** 链路自测）。
默认**有头模式**（`--mode headed`），全过程可见；需要登录的动作在未登录时会自动拉起浏览器登录。已登录后可全程 `--mode headless`。

> 平台入口：`https://mp.weixin.qq.com/cgi-bin/home`。
> **关键点**：直接访问 `cgi-bin/home` 会显示「请重新登录」，需点击首页的「登录」入口 `#jumpUrl` 才会带 token 进入后台（会话有效则直接进入）。

---

## 0. 前置：博客 SOP 校验

发布前，待发布 Markdown 必须符合 [`codes/标准生成发布输出博客sop.md`](../../../../../codes/标准生成发布输出博客sop.md)：
顶部一个 ` ```json ` 元数据代码块（`title` / `tags` / `category` / `articleSummary` / `columns` / `createTime` / `updateTime` / `knowledgeBasePath`），封面图以 `![coverImg](url)` 标记。

字段到公众号的映射：

| SOP 字段 | 公众号目标 |
| --- | --- |
| `title` | 文章标题 |
| `articleSummary` | 文章摘要 |
| `content`（去掉元数据块后的正文） | 正文（经 md.doocs.org 样式化后粘贴） |
| `![coverImg](url)` | 封面/首图（自动选取正文第一张图） |
| `columns[0]` 或渠道参数 `--collection` | 合集 |
| 渠道参数 `--author` / 元数据 `author`/`username` | 作者栏 |
| 渠道参数 `--wechat-name` / 元数据 `wechatName` | 正文顶部插入的公众号名片 |
| 渠道参数 `--group-send` / 元数据 `isGroupSend` | 是否群发（默认关闭） |

> 渠道专属参数从 `--xxx` 原样透传给渠道，由 `gzh/index.js` 的 `augmentPayload` 解析；核心 payload 层不感知。

---

## 1. `gzh checkLogin` —— 检测登录态

```bash
node scripts/publisher.js gzh checkLogin
```

判定逻辑（进入后台首页后）：

1. 出现账号名 `.weui-desktop_name`（如「长路Java」）→ **已登录**；
2. 否则看真正的会话 Cookie `slave_sid` / `slave_user` / `data_ticket`——它们**仅登录后下发**；未登录时的 `wxuin`/`ua_id` 不在白名单，因此不会假阳性；
3. 都没有则视为未登录。

| 返回 | 含义 | 下一步 |
| --- | --- | --- |
| `ok=true, loggedIn=true` | 已登录 | 直接 `publishDraft` / `publish` |
| `ok=false, loggedIn=false`（退出码 2） | 未登录 | `publish` 会自动拉起登录，或手动 `gzh login` |

---

## 2. `gzh login` —— 扫码登录

```bash
node scripts/publisher.js gzh login
```

1. 打开 `https://mp.weixin.qq.com/cgi-bin/home`（**有头模式**），点击首页「登录」入口 `#jumpUrl` 进入扫码页；
2. 用**管理员微信**扫码完成登录；
3. 脚本每 2s 轮询登录 Cookie，命中即成功（默认最长 **10 分钟**，`--timeout` / `PUBLISHER_LOGIN_TIMEOUT` 可调）；
4. 登录态保存在独立浏览器 profile（`~/.blogloom-publisher/chrome-profile-gzh`），后续可 `--mode headless` 复用。

```bash
node scripts/publisher.js gzh login --mode headless   # 已登录时快速复用校验
```

---

## 3. `gzh publishDraft` —— 保存草稿

```bash
node scripts/publisher.js gzh publishDraft --file "/abs/blog.md" --author "长路" --wechat-name "长路Java"
```

流程：

1. 进入后台首页（点 `#jumpUrl`）→ 内容管理 `.weui-desktop-menu_create` → 草稿箱 `.weui-desktop-sub-menu__item a`；
2. 点「新的创作」`button:has-text("新的创作")` → 点下拉「文章」`.weui-desktop-dropdown__list-ele:has-text("文章")`（**新标签页打开编辑器**，自动切换）；
3. 填标题：`.title-editor__input .ProseMirror`（contenteditable，**注意不是隐藏的 `#title` textarea**）；
4. 填作者：`#js_author_area #author`（可选）；
5. **正文样式转换**：在 md.doocs.org 新开转换页 → 粘贴 Markdown → 等渲染完成 → 点「复制」（`button:has-text("复制"), button:has-text("Copy")`）把**富文本 HTML** 写入剪贴板 → 回编辑页粘贴进内容 ProseMirror `.rich_media_content .ProseMirror`；
   - 粘贴后微信弹出「**内容结构检测**」确认框，需点「继续插入」`button:has-text("继续插入")` 内容才真正写入；
   - 可选：正文顶部插入公众号名片（`#js_editor_insertProfile` 搜索 `--wechat-name` 选第一个卡片）；
   - 转换失败自动回退为原始 Markdown 写入策略（`paste → cdp → keyboard`）；
6. 点编辑器工具栏「保存为草稿」`button:has-text("保存为草稿")` → 返回 `status=DRAFT_SAVED`（含带 `appmsgid` 的编辑页 URL）。

### 3.1 配置项默认策略（保存草稿前自动完成）

| 配置项 | 默认策略 |
| --- | --- |
| 标题 / 作者 | 标题必填；**作者必须先填**（否则「原创」弹窗无法勾选） |
| 封面 | 从正文图片选第一张（从正文选择 → 下一步 → 编辑封面「确认」） |
| 描述 | 取 `articleSummary`，**自动裁剪至 ≤120 字** |
| 原创 | 文字原创 → 勾「我已阅读」→ 确定（该弹窗为自定义组件，首次可能不响应，会自动关闭重开再试） |
| 赞赏 | 默认开启（需先声明原创）；弹「赞赏设置」→ 赞赏作者 + 赞赏账户 → 勾协议 → 确定（best-effort） |
| 付费 | **不开启**（产品要求） |
| 留言 | 保留默认 |
| 合集 | 按**分类**（`category`）匹配，无匹配/无分类则跳过 |
| 创作来源 | 个人观点，仅供参考 |

> 原创/赞赏弹窗为微信自定义组件，自动化下偶发不响应；`chooseOrigin` 已做「关闭重开」重试，`enableRewardAndPay` 有超时保护（不会卡住主流程）。

> 正文写入策略返回 `contentStrategy=md-style-paste` 表示走通 md.doocs 样式转换。

---

## 4. `gzh publish` —— 发表

```bash
node scripts/publisher.js gzh publish --file "/abs/blog.md" --wechat-name "长路Java"
node scripts/publisher.js gzh publish --file "/abs/blog.md" --dry-run
```

在 `publishDraft` 的填写基础上，继续配置并按顺序发表：

1. 底部 `.appsmg-editor__after-area` → `#js_cover_description_area`：
   - 封面：悬停 `#js_cover_area` → `.js_selectCoverFromContent` → 图片面板 `.appmsg_content_img_item` 第一张 → 下一步/确认；回读预览 `background-image` 校验，失败重试一次；
   - 摘要：`#js_description_area textarea`；
2. 原创：`#js_original_box #js_original` → 勾选同意 → 确认；
3. 合集：`#js_article_tags_area` → 选择与设置值同名的 `.select-opt-li`（不存在则不创建）；
4. 创作来源：`#js_claim_source_area` → 默认「个人观点，仅供参考」（第 4 项）；
5. 发表：底部 `.js_bot_bar .mass_send`（文案「发表」）→ 群发弹窗 `.new_mass_send_dialog`（`--group-send` 未开启则关闭开关）→ 主按钮 → 二次确认 `.double_check_dialog`；
6. 群发需管理员扫码，返回 `status=SUBMITTED`（或检测到成功提示时 `PUBLISHED`）。

> `--dry-run` 完成所有填写，但不点最终发表。

---

## 5. `gzh delete` —— 删除推文

```bash
node scripts/publisher.js gzh delete --title "Maven插件—05：批量添加License头声明spotless-maven-plugin"
```

流程：进入内容管理 → 草稿箱 → 定位卡片 `.weui-desktop-card`（可按 `data-appid` 或标题）→ 悬停卡片 → 行内「删除」`.weui-desktop-link:has-text("删除")` → 确认浮层 `.weui-desktop-popover` 内主按钮 `button.weui-desktop-btn_primary:has-text("删除")` → **轮询回查该卡片已从列表消失**，返回 `status=DELETED`（未确认到则 `DELETE_UNVERIFIED`）。

---

## 6. `gzh test publish` —— 发布草稿 → 删除 链路自测

```bash
node scripts/publisher.js gzh test publish --file "/abs/blog.md"
```

因公众号直接发表需管理员扫码/审核、不适合自动回滚，公众号自测采用 `testMode=draft`：
**保存草稿 → 立即删除该草稿**，返回 `status=DRAFT_SAVED_AND_DELETED`，用于验证分发链路可用且不残留内容。
（CSDN 等支持直接发布回滚的渠道为 `PUBLISHED_AND_DELETED`。）

真实运行示例（headless）：

```json
{"ok":true,"channel":"gzh","action":"test publish",
 "data":{"status":"DRAFT_SAVED_AND_DELETED","mode":"draft",
   "publish":{"status":"DRAFT_SAVED","url":"https://mp.weixin.qq.com/...appmsgid=100008711","contentLength":13944,"contentStrategy":"md-style-paste"},
   "delete":{"status":"DELETED"}}}
```

### 常见分支

| 情况 | 处理 |
| --- | --- |
| 未登录 | 需登录的动作默认自动拉起（有头）浏览器登录，登录后继续；`--no-login` 则直接报错 |
| md.doocs 转换失败 | 回退为原始 Markdown 写入，`contentStrategy=raw-*` |
| 弹出「内容结构检测」 | 自动点「继续插入」；若内容样式有行高告警，属第三方样式提示，不阻断保存 |
| 封面列表为空 | `coverSet=false`，跳过封面，正文正常保存 |
| 合集不存在 | 不创建，跳过，正文仍可保存 |
| 删除未确认 | 返回 `DELETE_UNVERIFIED`，检查 `.weui-desktop-popover button.weui-desktop-btn_primary` 选择器 |

---

## 7. 选择器维护

**所有 DOM 选择器集中在 `scripts/channels/gzh/selectors.js`**，改版时只改这里；md.doocs 的转换选择器集中在 `scripts/channels/gzh/format.js`。

| 键 | 用途 |
| --- | --- |
| `accountName` / `jumpUrl` | 登录态账号名 / 首页登录入口（进后台、扫码都靠它） |
| `menuCreate` / `subMenuItem` | 内容管理 / 草稿箱 |
| `newCreateButton` / `createArticleItem` | 「新的创作」按钮 / 下拉「文章」 |
| `titleInput` / `titleFallbackInput` / `authorInput` | 标题 ProseMirror / 兜底标题输入 / 作者 |
| `editor` / `continueInsert` / `insertProfileBtn` | 内容 ProseMirror / 「继续插入」/ 公众号名片 |
| `afterArea` / `coverArea` / `selectCoverFromContent` / `imgCropPanel` / `contentImgItem` / `coverPreview` | 封面选取与校验 |
| `descTextarea` | 摘要 |
| `originalBox` / `original` / `formCheckbox` / `originalAgreement` | 原创 |
| `articleTagsArea` / `articleTagsLabel` / `collectionOption` | 合集 |
| `claimSourceArea` / `claimSourceDesc` / `claimSourceOption` | 创作来源 |
| `bottomBar` / `massSend` / `massSendDialog` / `massSendSwitch` / `doubleCheckDialog` / `btnPrimary` | 发表与群发弹窗 |
| `saveButton` / `saveFallback` | 保存为草稿 |
| `draftItem` / `draftDeleteLink` / `confirmDelete` | 草稿箱卡片 / 行内删除 / 确认浮层 |

验证选择器：

```bash
node scripts/publisher.js gzh publishDraft --file "/abs/blog.md" --dry-run
```

返回 `contentStrategy`（`md-style-paste` 表示走通 md.doocs 样式转换）、`contentLength` 与字段填写结果，据此判断各步骤是否生效。
