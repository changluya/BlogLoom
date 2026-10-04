# BlogLoom Publisher Skill —— 公众号（gzh）渠道能力与发布流程

> 面向对象：需要了解 / 维护 / 扩展 BlogLoom 博客分发 Skill 的开发者与 AI Agent。
> 渠道实现目录：[`skills/blogloom-publisher-skill/scripts/channels/gzh/`](../../../skills/blogloom-publisher-skill/scripts/channels/gzh/)
> 渠道 SOP：[`references/channels/gzh/sop.md`](../../../skills/blogloom-publisher-skill/references/channels/gzh/sop.md)
> 通用契约：[`references/00-tool-contract.md`](../../../skills/blogloom-publisher-skill/references/00-tool-contract.md)
> 流程参照已跑通的 `auto-sync-blog` 微信公众号实现，并**经真实公众号后台验证**。

---

## 一、渠道概览

| 项 | 值 |
| --- | --- |
| 渠道标识 | `gzh` |
| 平台入口 | `https://mp.weixin.qq.com/cgi-bin/home` |
| 登录页 | 同上（点击首页「登录」入口进入扫码页） |
| 编辑器 | 内容管理 → 草稿箱 → 新的创作 → 文章（**新标签页**打开 `appmsg_edit_v2`） |
| 内容管理 | 内容管理 → 草稿箱（`appmsg?action=list_card`） |
| 登录凭证 | Cookie `slave_sid` / `slave_user` / `data_ticket` |

**关键点**：直接访问 `cgi-bin/home` 会显示「请重新登录」，必须点击首页的「登录」入口 `#jumpUrl` 才会带 token 进入后台（会话有效则直接进入）。未登录态会下发 `wxuin` / `ua_id` 等 Cookie，**不能用它们判定登录**，否则假阳性。

公众号编辑器不识别 Markdown，正文需先经 **md.doocs.org** 渲染成带内联样式的 HTML，再复制富文本粘贴回编辑器。

---

## 二、能力矩阵

| 动作 | 说明 | 需登录 | 关键返回 |
| --- | --- | --- | --- |
| `gzh checkLogin` | 检测登录态 | 否 | `loggedIn / user / cookieNames` |
| `gzh login` | 打开浏览器扫码登录并持久化 | 否 | `loggedIn / cookieNames` |
| `gzh publishDraft` | 配置好文章后保存为草稿 | 是 | `status=DRAFT_SAVED` |
| `gzh publish` | 发表（需管理员扫码/审核） | 是 | `status=SUBMITTED / PUBLISHED` |
| `gzh delete` | 草稿箱定位后删除（含确认与校验） | 是 | `status=DELETED / DELETE_UNVERIFIED` |
| `gzh test publish` | 组合链路自测：**发布草稿 → 删除** | 是 | `status=DRAFT_SAVED_AND_DELETED` |

> 公众号直接发表需管理员扫码/审核、不适合自动回滚，故 `test publish` 采用 `channel.testMode='draft'`，即 **保存草稿 → 立即删除**。
> 渠道可声明 `successStatuses: ['SUBMITTED']`，核心层按「存在则并入成功态」处理。

---

## 三、统一调用

```bash
node scripts/publisher.js <channel> <action> [options]

# 示例
node scripts/publisher.js gzh checkLogin
node scripts/publisher.js gzh login
node scripts/publisher.js gzh publishDraft --file "/abs/blog.md" --author "长路" --wechat-name "长路Java"
node scripts/publisher.js gzh publish --file "/abs/blog.md" --dry-run
node scripts/publisher.js gzh delete --title "文章标题"
node scripts/publisher.js gzh test publish --file "/abs/blog.md"
```

**渠道专属参数**（从 `--xxx` 原样透传，由 `gzh/index.js` 的 `augmentPayload` 解析，核心 CLI 不感知）：

| 参数 | 含义 |
| --- | --- |
| `--author` | 作者栏（**必填**，缺失会导致「原创」弹窗无法勾选） |
| `--wechat-name` | 正文顶部插入的公众号名片名 |
| `--collection` | 合集名（缺省用 `category`） |
| `--group-send` / `--no-group-send` | 发表时是否群发（默认关闭） |

---

## 四、SOP 字段映射（Markdown → 公众号）

| SOP 字段 | 公众号目标 |
| --- | --- |
| `title` | 文章标题 |
| `articleSummary` | 文章摘要（**自动裁剪 ≤120 字**） |
| `content`（去掉元数据块后的正文） | 正文（经 md.doocs.org 样式化后粘贴） |
| `![coverImg](url)` | 封面/首图（自动选取正文第一张图） |
| `category`（或渠道参数 `--collection`） | 合集（有匹配则选，无则跳过） |
| 渠道参数 `--author` / 元数据 `author`/`username` | 作者栏 |
| 渠道参数 `--wechat-name` / 元数据 `wechatName` | 正文顶部公众号名片 |
| 渠道参数 `--group-send` / 元数据 `isGroupSend` | 发表是否群发 |

---

## 五、场景流程详解

### 5.1 `gzh checkLogin` —— 检测登录态

**步骤**

1. `enterAdminHome`：打开 `cgi-bin/home`，点击首页「登录」入口 `#jumpUrl`，进入带 token 的后台首页；
2. 出现账号名 `.weui-desktop_name`（如「长路Java」）→ **已登录**；
3. 否则看真正的会话 Cookie `slave_sid` / `slave_user` / `data_ticket`（仅登录后下发）；
4. 都没有 → 未登录。

| 返回 | 下一步 |
| --- | --- |
| `loggedIn=true` | 直接 `publishDraft` / `publish` |
| `loggedIn=false`（退出码 2） | `publish` 会自动拉起登录，或手动 `gzh login` |

> 修复记录：早期用 `wxuin` 判定导致**未登录被误判为已登录**；现白名单只认会话 Cookie，且先点 `#jumpUrl` 才能进入后台。

---

### 5.2 `gzh login` —— 扫码登录

**步骤**

1. 打开 `cgi-bin/home`（**有头模式**），渠道 `prepareLogin` 点击 `#jumpUrl` 进入扫码页；
2. 用**管理员微信**扫码完成登录；
3. 每 2s 轮询登录 Cookie，命中 `slave_sid` 等即成功（默认最长 10 分钟，可调 `--timeout` / `PUBLISHER_LOGIN_TIMEOUT`）；
4. 登录态保存于 `~/.blogloom-publisher/chrome-profile-gzh`，后续可 `--mode headless` 复用。

---

### 5.3 `gzh publishDraft` —— 配置后保存草稿

**步骤**

1. **进入后台首页**：`enterAdminHome` 点 `#jumpUrl`；
2. **进入编辑器**：内容管理 `.weui-desktop-menu_create` → 草稿箱 `.weui-desktop-sub-menu__item a` → 「新的创作」`button:has-text("新的创作")` → 「文章」`.weui-desktop-dropdown__list-ele:has-text("文章")`（**新标签页**打开编辑器，自动切换）；
3. **填标题**：`.title-editor__input .ProseMirror`（contenteditable，**不是隐藏的 `#title` textarea**），用键盘输入；
4. **填作者**：`#js_author_area #author`（**必填**，否则后续原创弹窗无法勾选）；
5. **写正文**（md.doocs 样式化）：
   - 在 md.doocs.org 新开转换页 → 粘贴 Markdown → 等渲染完成 → 点「复制」（`button:has-text("复制"), button:has-text("Copy")`）把富文本 HTML 写入剪贴板；
   - 回编辑页粘贴进内容 ProseMirror `.rich_media_content .ProseMirror`（注意与标题 ProseMirror 区分）；
   - 粘贴后微信弹「**内容结构检测**」确认框 → 点「继续插入」`button:has-text("继续插入")` 内容才真正写入；
   - 转换失败自动回退为原始 Markdown 写入策略（`paste → cdp → keyboard`）；
6. **配置区**（见 5.5 配置项默认策略）：封面 / 描述 / 原创 / 赞赏 / 合集 / 创作来源；
7. **保存草稿**：点「保存为草稿」`button:has-text("保存为草稿")` → `status=DRAFT_SAVED`。

**返回**

```json
{ "ok": true, "channel": "gzh", "action": "publishDraft",
  "data": { "status": "DRAFT_SAVED", "contentStrategy": "md-style-paste",
            "contentLength": 13944, "url": "https://mp.weixin.qq.com/...appmsgid=...",
            "publishConfig": { "coverSet": true, "summarySet": true, "original": true,
              "reward": true, "pay": false, "collection": "", "source": "个人观点，仅供参考" } },
  "error": null }
```

---

### 5.4 `gzh publish` —— 发表

在草稿填写 + 配置基础上，点底部 `.js_bot_bar .mass_send`（文案「发表」）：

1. 群发弹窗 `.new_mass_send_dialog`：`--group-send` 未开启则关闭群发开关；
2. 点弹窗主按钮 `.weui-desktop-btn_primary`；
3. 二次确认 `.double_check_dialog` → 继续发表；
4. 群发需管理员扫码，返回 `status=SUBMITTED`（检测到成功提示时 `PUBLISHED`）。

> `--dry-run`：完成所有填写与配置，但不点最终发表。

---

### 5.5 配置项默认策略（`preparePublish`，保存草稿前自动完成）

| 配置项 | 默认策略 | 关键选择器 / 逻辑 |
| --- | --- | --- |
| 封面 | 从正文图片选**第一张** | 悬停 `#js_cover_area` → `.js_selectCoverFromContent`（悬停后才可见）→ `.img_crop_panel .appmsg_content_img_item` 第一张 → `button:has-text("下一步")` → 编辑封面页 `button:has-text("确认")`（注意不是「完成/确定」） |
| 描述 | 取 `articleSummary`，**自动裁剪 ≤120 字** | `#js_description_area textarea` |
| 原创 | 文字原创 → 勾「我已阅读」→ 确定 | `#js_original` 打开弹窗；选 `.weui-desktop-form__check-label:has-text("文字原创")`；协议复选框在 `.original_agreement`（点文字会打开协议页，改为对 `<label>` 触发 DOM click）；**首次弹窗常不响应 → 关闭重开再试**（`chooseOrigin` 已实现） |
| 赞赏 | 默认开启（需先声明原创） | `#js_reward_setting_area .js_reward_open` → 弹「赞赏设置」→ 赞赏作者 + 赞赏账户 + 勾协议 → 确定（**best-effort**，有超时保护） |
| 付费 | **不开启**（产品要求） | —— |
| 留言 | 保留编辑器默认 | —— |
| 合集 | 按**分类** `category` 匹配，无匹配/无分类则跳过 | `#js_article_tags_area` → `.select-opt-li` 同名选中 |
| 创作来源 | 个人观点，仅供参考 | `#js_claim_source_area .js_claim_source_desc` → 弹窗选 `.weui-desktop-form__check-label:has-text("个人观点，仅供参考")` → `button:has-text("确认")` |

> **原创/赞赏弹窗为微信自定义组件**：对自动化合成点击响应不稳定。`chooseOrigin` 采用「填作者 → 关闭重开重试」策略；`enableRewardAndPay` 加整体超时（不会卡住主流程）。付费按需求不开启。

#### 5.5.1 渠道级发布配置（`conf/gzh.conf`）

文章标题/标签/分类/摘要/正文来自博客顶部 SOP 元数据；上表这些**渠道级选项**来自 `conf/gzh.conf`，脚本执行时默认读取：

| conf 键 | 取值 | 默认 | 作用 |
| --- | --- | --- | --- |
| `author` | 文本 | `长路` | 作者栏（必填；CLI `--author` 覆盖） |
| `wechatName` | 文本 | 空 | 正文顶部公众号名片（CLI `--wechat-name` 覆盖） |
| `collection` | 文本 | 空 | 合集名；空则按 `category` 匹配（CLI `--collection` 覆盖） |
| `groupSend` | `true` / `false` | `false` | 发表是否群发（CLI `--group-send` 覆盖） |
| `cover` | `first` / `none` | `first` | 封面策略 |
| `summaryMaxLength` | 整数 | `120` | 描述最大字数 |
| `enableOriginal` | `true` / `false` | `true` | 是否声明原创 |
| `enableReward` | `true` / `false` | `true` | 是否开启赞赏 |
| `enablePay` | `true` / `false` | `false` | 是否开启付费（默认不开启） |
| `creationSource` | 文案 | `个人观点，仅供参考` | 创作来源 |
| `mdStyleUrl` | URL | `https://md.doocs.org/` | 正文样式化工具地址 |

> 优先级：CLI 参数 > 元数据 extra > conf 默认。加载器见 `scripts/lib/channel-conf.js`。

---

### 5.6 `gzh delete` —— 删除推文

**步骤**

1. 进入内容管理 → 草稿箱；
2. 按 `--title` 定位卡片 `.weui-desktop-card`（或按 `data-appid`）；
3. 悬停卡片 → 行内「删除」`.weui-desktop-link:has-text("删除")`；
4. 确认浮层 `.weui-desktop-popover` 内主按钮 `button.weui-desktop-btn_primary:has-text("删除")`；
5. **轮询回查该卡片已从列表消失** → `DELETED`，否则 `DELETE_UNVERIFIED`。

> 修复记录：删除不是「...」菜单，而是行内「编辑 / 删除」；确认按钮必须点真正的 `button`（不是 `.weui-desktop-btn_wrp` 外层 div）。

---

### 5.7 `gzh test publish` —— 发布草稿 → 删除 链路自测

```bash
node scripts/publisher.js gzh test publish --file "/abs/blog.md"
```

`channel.testMode='draft'`：**保存草稿 → 立即删除该草稿**，返回 `status=DRAFT_SAVED_AND_DELETED`，验证链路可用且不残留内容。

---

## 六、选择器对照表（`channels/gzh/selectors.js`）

| 键 | 用途 |
| --- | --- |
| `accountName` / `jumpUrl` | 登录态账号名 / 首页登录入口 |
| `menuCreate` / `subMenuItem` | 内容管理 / 草稿箱 |
| `newCreateButton` / `createArticleItem` | 「新的创作」/ 下拉「文章」 |
| `titleInput` / `titleFallbackInput` / `authorInput` | 标题 ProseMirror / 兜底标题 / 作者 |
| `editor` / `continueInsert` / `insertProfileBtn` | 内容 ProseMirror / 「继续插入」/ 公众号名片 |
| `afterArea` / `coverArea` / `selectCoverFromContent` / `imgCropPanel` / `contentImgItem` / `coverNext` / `coverDone` / `coverPreview` | 封面选取 / 下一步 / 确认 / 校验 |
| `descTextarea` | 描述（≤120） |
| `originalBox` / `original` / `originalTypeText` / `originalAgreementInput` / `originalAgreementLabel` / `dialogConfirm` / `dialogCancel` | 原创弹窗 |
| `rewardArea` / `rewardSwitch` / `rewardTypeAuthor` / `rewardAccountItem` / `rewardAgreementInput` / `rewardDialogConfirm` | 赞赏设置 |
| `payArea` / `paySwitch` | 付费（默认不操作） |
| `articleTagsArea` / `articleTagsLabel` / `collectionOption` | 合集 |
| `claimSourceArea` / `claimSourceDesc` / `claimSourceOption` / `claimConfirm` | 创作来源 |
| `bottomBar` / `massSend` / `massSendDialog` / `massSendSwitch` / `doubleCheckDialog` / `btnPrimary` | 发表与群发弹窗 |
| `saveButton` / `saveFallback` | 保存为草稿 |
| `draftItem` / `draftDeleteLink` / `confirmDelete` | 草稿箱卡片 / 行内删除 / 确认浮层 |

md.doocs 转换选择器集中在 `channels/gzh/format.js`（`editable: .cm-content`、`copyButton`）。

> 站点改版时**只改选择器文件**，业务流程代码零改动。

---

## 七、错误码与分支

| 情况 | 处理 |
| --- | --- |
| 未登录 | 需登录动作默认自动拉起（有头）登录；`--no-login` 直接报错 |
| md.doocs 转换失败 | 回退原始 Markdown 写入，`contentStrategy=raw-*` |
| 弹出「内容结构检测」 | 自动点「继续插入」 |
| 原创弹窗不响应 | 关闭重开重试；仍失败则跳过（不影响保存） |
| 赞赏弹窗卡住 | 超时保护后跳过（不影响保存） |
| 合集无匹配 | 跳过，正文仍可保存 |
| 删除未确认 | 返回 `DELETE_UNVERIFIED` |

退出码：`0` 成功 / `1` 执行异常 / `2` `checkLogin` 未登录或 `login` 超时。

---

## 八、验证与维护

```bash
npm run test:channels
node scripts/publisher.js gzh publishDraft --file "/abs/blog.md" --dry-run
node scripts/publisher.js gzh test publish --file "/abs/blog.md"
```

- 单测覆盖：`checkLogin / prepareLogin / enterEditor / setTitle / setAuthor / setContent(含继续插入) / preparePublish / publish / saveDraft / deleteBlog / augmentPayload`；
- `contentStrategy=md-style-paste` 表示走通 md.doocs 样式转换；
- **真机验证结论**：标题 / 作者 / 正文样式化 / 封面 / 描述(≤120) / 原创 / 合集 / 创作来源 / 保存草稿 / 删除 / `test publish` 均已跑通；**赞赏为 best-effort**，**付费不开启**。
