# CSDN 渠道 SOP

> 渠道实现：`scripts/channels/csdn/`（`index.js` 流程 + `selectors.js` 选择器）
> 通用契约：见 [../../00-tool-contract.md](../../00-tool-contract.md)
> 流程参照已跑通的 `auto-sync-blog`，并经真实发布验证。

CSDN 渠道包含：`checkLogin`（检测登录）、`login`（扫码登录）、`publishDraft`（存草稿）、`publish`（发布）、`delete`（删除）、`test publish`（发布→删除链路自测）。
默认**有头模式**（`--mode headed`），发布全过程可见；需要登录的动作在未登录时会自动拉起浏览器登录。

---

## 0. 前置：博客 SOP 校验

发布前，待发布 Markdown 必须符合 [`codes/标准生成发布输出博客sop.md`](../../../../../codes/标准生成发布输出博客sop.md)：

- 文件最顶部一个 ` ```json ` 元数据代码块；
- 元数据字段：`title` / `tags` / `category` / `articleSummary` / `columns`（或 `column`）/ `createTime` / `updateTime` / `knowledgeBasePath`，**额外字段原样保留**（`data.extra`）；
- 封面图在正文中以 `![coverImg](url)` 显式标记（也兼容元数据里的 `cover` / `coverImg` / `firstPicture`）；
- 解析由 `scripts/lib/markdown.js` 实现，**容忍 SOP 样例常见笔误**（全角冒号、尾随逗号、全角引号）。

字段到 CSDN 的映射：

| SOP 字段 | CSDN 目标 |
| --- | --- |
| `title` | 文章标题 |
| `tags`（逗号分隔，取前 10） | 文章标签 |
| `columns`（多个，逐个匹配已有专栏） | 分类专栏 |
| `articleSummary` | 文章摘要 |
| `![coverImg](url)` | 封面/首图（可选，见下） |
| `content`（去掉元数据块后的正文） | 正文（Markdown 源码） |

---

## 1. `csdn checkLogin` —— 检测登录态

```bash
node scripts/publisher.js csdn checkLogin
```

判定逻辑（首页 `https://www.csdn.net/`）：

1. `.toolbar-btn-loginfun` 文本为「登录」→ 未登录；
2. 命中「创作」入口且非登录态，或 Cookie 含 `UserToken` + `UserInfo` → 已登录；
3. 同时返回是否存在滑块 `.verify-move-block`（`data.hasSlider`）。

| 返回 | 含义 | 下一步 |
| --- | --- | --- |
| `ok=true, loggedIn=true` | 已登录 | 直接 `publish` |
| `ok=false, loggedIn=false`（退出码 2） | 未登录 | `publish` 会自动拉起登录，或手动 `csdn login` |

---

## 2. `csdn login` —— 扫码登录

```bash
node scripts/publisher.js csdn login
```

1. 打开登录页 `https://passport.csdn.net/login`（**有头模式**）；
2. 用户扫码 / 输入账号完成登录（含可能的滑块验证）；
3. 脚本每 2s 轮询登录 Cookie，命中即成功（默认最长 **10 分钟**，`--timeout` / `PUBLISHER_LOGIN_TIMEOUT` 可调）；
4. 登录态保存在独立浏览器 profile（`~/.blogloom-publisher/chrome-profile-csdn`）。

---

## 3. `csdn publish` —— 发布博客

```bash
# 干跑：走完所有填写与弹窗配置，但不点最终发布
node scripts/publisher.js csdn publish --file "/abs/blog.md" --dry-run

# 正式发布
node scripts/publisher.js csdn publish --file "/abs/blog.md"
```

### 发布流程（Playwright 主操作 + CDP 增强）

1. 进入编辑器：首页点「创作」`.toolbar-btn-write-new`（失败回退直开 `editor.csdn.net/md/`）；
2. 填标题：先点 `.article-bar__title-display`（【无标题】）唤出隐藏 input `.article-bar__title`，清空后写入；
3. 写正文：点 `.editor__inner.markdown-highlighting`（`<pre contenteditable>`），全选删除默认欢迎内容，再按 `paste → cdp → keyboard` 策略粘贴（剪贴板粘贴优先，CDP `Input.insertText` 兜底）；**正文含图片时，按图片数量估算等待**（默认 2s，每满 5 张多 3s），再继续，确保 CSDN 异步上传/解析图片完成、发布弹窗的「已有图片列表」可用；
4. 点「发布文章」`button.btn-publish` 打开弹窗 `.modal__publish-article`；
5. 弹窗内依次配置：
   - 标签 `.mark_selection`：**先点「添加文章标签」`.tag__btn-tag` 打开选择框**（输入框 `.el-autocomplete input.el-input__inner` 此时才出现），清空草稿残留的已选标签（`.mark_selection_box_el_tag .el-tag__close`），再逐个 `fill + Enter`（最多 10 个），最后点 `.mark_selection_box .modal__close-button` 关闭；**回读 `.mark_selection_box_el_tag` 校验实际生效标签**，写入 `publishConfig.tags / selectedTags / missingTags`；
   - **首图**：直接选择「已有图片列表」的**第一张**（`.img-selection-item img.select-cover` → `.vicp-operate-btn` 确认）；列表为空则跳过；
   - 摘要 `.desc-box .el-textarea__inner`；
   - 分类专栏：定位含「分类专栏」的 `.form-entry`，悬停添加、勾选已有专栏（多个）。匹配按**精确 > 已有包含设置值 > 设置值包含已有**评分取最优，避免误配宽泛子类（实测 `Maven&Gradle` 命中二级项 `# Maven&Gradle`）；勾选项是**隐藏的 `<input type=checkbox>`**，普通点击无效，需触发 DOM `click()` 才会生效，随后回读 `.tag__item-box` 校验；
   - 文章类型 → 原创 `input[value=original]`；
   - 可见范围 → 公开 `input[value=public]`；
   - 创作声明 → 下拉选择「个人观点，仅供参考」（contains 匹配）；
6. `--dry-run`：到此返回 `status=DRY_RUN`，并返回 `publishConfig`（`tags`/`selectedTags`/`missingTags` 标签回读、`resolvedCategories` 命中的专栏、`selectedCategories` 回读的实际已选项、`unresolvedCategories` 未命中、`coverSet`、`creationStatement`）；
7. 否则点弹窗底部 `.modal__button-bar button`（发布文章），从成功弹窗 `a.success-modal-btn[href*=/article/details/]` 取链接（去掉查询参数），返回 `status=PUBLISHED` 与 `url`。

### 3.1 `csdn publishDraft` —— 保存草稿

```bash
node scripts/publisher.js csdn publishDraft --file "/abs/blog.md"
```

- 进入编辑器填标题 / 正文后，点顶部「保存草稿」`button.btn-save`，返回 `status=DRAFT_SAVED`；
- 不走发布弹窗，标签/摘要/分类等字段在草稿阶段不设置（后续可在编辑器或发布时补）。

### 3.2 `csdn delete` —— 删除博客

```bash
node scripts/publisher.js csdn delete --url "https://blog.csdn.net/x/article/details/123"
node scripts/publisher.js csdn delete --id 123
node scripts/publisher.js csdn delete --title "Maven插件—05：..."
```

流程：进入内容管理页 `https://mp.csdn.net/mp_blog/manage` → 按 `--id`（`浏览`链接 `/article/details/<id>`）或 `--title` 定位行 → 悬停行右侧「...」（`.el_mcm-dropdown .el-dropdown-link`）→ 下拉点「删除」（`.el_mcm-dropdown-menu__item`）→ 二次确认弹窗点「确定」（`.btn-msg-confirm`）→ **校验该行已从列表消失**，返回 `status=DELETED`（未确认到则 `DELETE_UNVERIFIED`）。

> 注意：一定要点二次确认弹窗的「确定」，否则不会真正删除。

### 3.3 `csdn test publish` —— 发布→删除 链路自测

```bash
node scripts/publisher.js csdn test publish --file "/abs/blog.md"
```

组合 `publish` + `delete`：发布成功后立即按 URL/id 删除该文章，返回 `status=PUBLISHED_AND_DELETED`，用于验证分发链路是否可用且不残留内容。

### 常见分支

| 情况 | 处理 |
| --- | --- |
| 未登录 | 需登录的动作默认自动拉起（有头）浏览器登录，登录后继续；`--no-login` 则直接报错 |
| 滑块验证 | `data.hasSlider=true`，需人工在浏览器完成；脚本不会绕过 |
| 分类专栏不存在 | 记入 `publishConfig.unresolvedCategories`，跳过不创建，正文仍可发布 |
| 首图列表为空 | `publishConfig.coverSet=false`，跳过首图，正文正常发布 |
| 正文写入失败 | `ok=false`，列出各写入策略失败原因 → 检查 `.editor__inner` 选择器 |
| 删除未确认 | 返回 `DELETE_UNVERIFIED`，检查 `.btn-msg-confirm` 选择器 |

---

## 4. 选择器维护

CSDN 站点可能改版。**所有 DOM 选择器集中在 `scripts/channels/csdn/selectors.js`**，改版时只改这里：

| 键 | 用途 |
| --- | --- |
| `homeWriteNew` / `homeLoginFun` / `slider` | 首页创作入口 / 登录态 / 滑块 |
| `titleDisplay` / `titleInput` | 标题展示与输入框 |
| `content` | 正文 `<pre contenteditable>` |
| `saveDraftButton` | 保存草稿 |
| `publishEntry` / `publishModal` | 打开发布弹窗 |
| `tagContainer` / `tagBox` / `tagSelected` / `tagAddBtn` / `tagDelete` / `tagInput` / `tagModalClose` | 标签容器 / 选择框 / 已选标签 / 「添加文章标签」/ 删除已选 / 输入框 / 关闭按钮（子选择器均相对 `tagContainer`，**不要再带 `.mark_selection` 前缀**） |
| `coverItem` / `coverConfirm` | 首图（已有图片列表第一张） |
| `summaryInput` | 摘要 |
| `formEntry` / `categoryAddBtn` / `categoryOptionCheckbox` / `optionInput` | 分类专栏 / 类型 / 可见范围（按文案定位 `.form-entry`） |
| `creationTrigger` / `creationOption` | 创作声明下拉与选项 |
| `finalPublishButton` / `successLink` | 最终发布 / 成功链接 |
| `manageItem` / `manageMore` / `manageMenuDelete` / `confirmButton` | 内容管理定位 / 「...」/ 下拉「删除」/ 二次确认 |

验证选择器：

```bash
node scripts/publisher.js csdn publish --file "/abs/blog.md" --dry-run
```

`--dry-run` 返回 `contentStrategy`（生效的写入策略）、`contentLength` 与 `publishConfig`，据此判断各字段是否写入成功。