# BlogLoom Publisher Skill —— CSDN 渠道能力与发布流程

> 面向对象：需要了解 / 维护 / 扩展 BlogLoom 博客分发 Skill 的开发者与 AI Agent。
> 渠道实现目录：[`skills/blogloom-publisher-skill/scripts/channels/csdn/`](../../../skills/blogloom-publisher-skill/scripts/channels/csdn/)
> 渠道 SOP：[`references/channels/csdn/sop.md`](../../../skills/blogloom-publisher-skill/references/channels/csdn/sop.md)
> 通用契约：[`references/00-tool-contract.md`](../../../skills/blogloom-publisher-skill/references/00-tool-contract.md)

---

## 一、渠道概览

| 项 | 值 |
| --- | --- |
| 渠道标识 | `csdn` |
| 平台入口 | `https://www.csdn.net/` |
| 登录页 | `https://passport.csdn.net/login` |
| 编辑器 | `https://editor.csdn.net/md/` |
| 内容管理 | `https://mp.csdn.net/mp_blog/manage/article` |
| 登录凭证 | Cookie `UserToken` + `UserInfo` |

CSDN 是 Skill 的**首个对接渠道**，主操作走 Playwright（可见浏览器、真实点击/输入/粘贴），CDP 仅作底层增强（复用登录态、`Input.insertText` 兜底富文本、读取 HttpOnly Cookie 判定登录）。默认**有头模式**，全程可见。

---

## 二、能力矩阵

| 动作 | 说明 | 需登录 | 关键返回 |
| --- | --- | --- | --- |
| `csdn checkLogin` | 检测登录态 | 否 | `loggedIn / user / hasSlider` |
| `csdn login` | 打开浏览器扫码登录并持久化 | 否 | `loggedIn / cookieNames` |
| `csdn publishDraft` | 保存为草稿 | 是 | `status=DRAFT_SAVED` |
| `csdn publish` | 发布博客 | 是 | `status=PUBLISHED, url` |
| `csdn delete` | 内容管理页定位后删除（含二次确认与校验） | 是 | `status=DELETED / DELETE_UNVERIFIED` |
| `csdn test publish` | 组合链路自测：**发布 → 立即删除** | 是 | `status=PUBLISHED_AND_DELETED` |

> `test publish` 用于验证「发布链路可用且不残留内容」。CSDN 支持发布后回滚，故为 `PUBLISHED_AND_DELETED`。

---

## 三、统一调用

```bash
node scripts/publisher.js <channel> <action> [options]

# 示例
node scripts/publisher.js csdn checkLogin
node scripts/publisher.js csdn login
node scripts/publisher.js csdn publishDraft --file "/abs/blog.md"
node scripts/publisher.js csdn publish --file "/abs/blog.md" --dry-run
node scripts/publisher.js csdn publish --file "/abs/blog.md" --mode headless
node scripts/publisher.js csdn delete --url "https://blog.csdn.net/x/article/details/123"
node scripts/publisher.js csdn test publish --file "/abs/blog.md"
```

统一输出（stdout）：

```json
{ "ok": true, "channel": "csdn", "action": "publish", "data": { ... }, "error": null }
```

日志走 stderr（`[publisher]` 前缀），便于 Agent 解析。

---

## 四、SOP 字段映射（Markdown → CSDN）

| SOP 字段 | CSDN 目标 |
| --- | --- |
| `title` | 文章标题 |
| `tags`（逗号分隔，取前 10） | 文章标签 |
| `columns`（多个，逐个匹配已有专栏） | 分类专栏 |
| `articleSummary` | 文章摘要 |
| `![coverImg](url)` | 封面 / 首图（可选） |
| `content`（去掉元数据块后的正文） | 正文（Markdown 源码） |

---

## 五、场景流程详解

### 5.1 `csdn checkLogin` —— 检测登录态

**步骤**

1. 打开首页 `https://www.csdn.net/`；
2. 读取 `.toolbar-btn-loginfun` 文案：为「登录」则未登录；
3. 命中「创作」入口且非登录态，或 Cookie 含 `UserToken` + `UserInfo` → 已登录；
4. 同时返回是否存在滑块 `.verify-move-block`（`data.hasSlider`）。

**返回**

```json
{ "ok": true, "channel": "csdn", "action": "checkLogin",
  "data": { "loggedIn": true, "user": "长路", "reason": "检测到 CSDN 登录态",
            "hasSlider": false, "loginEntryVisible": false }, "error": null }
```

| 返回 | 下一步 |
| --- | --- |
| `loggedIn=true` | 直接 `publish` / `publishDraft` |
| `loggedIn=false`（退出码 2） | `publish` 会自动拉起登录，或手动 `csdn login` |

---

### 5.2 `csdn login` —— 扫码登录

**步骤**

1. 打开 `https://passport.csdn.net/login`（**有头模式**）；
2. 用户扫码 / 输入账号完成登录（含可能的滑块验证，脚本不绕过）；
3. 每 2s 轮询登录 Cookie，命中 `UserToken` / `UserInfo` 即成功（默认最长 10 分钟）；
4. 登录态保存在独立 profile：`~/.blogloom-publisher/chrome-profile-csdn`。

---

### 5.3 `csdn publishDraft` —— 保存草稿

**步骤**

1. **进入编辑器**：首页点「创作」`.toolbar-btn-write-new`（失败回退直开 `editor.csdn.net/md/`）；
2. **填标题**：先点展示态 `.article-bar__title-display`（【无标题】）唤出隐藏 `input.article-bar__title`，清空后写入，回读校验；
3. **写正文**：
   - 点 `.editor__inner.markdown-highlighting`（`<pre contenteditable>`）；
   - 全选删除默认欢迎内容；
   - 按 `paste → cdp → keyboard` 策略粘贴（剪贴板粘贴优先，CDP `Input.insertText` 兜底）；
   - **正文含图片时按图片数量估算等待**（默认 2s，每满 5 张多 3s），确保图片异步上传完成；
4. 点顶部「保存草稿」`button.btn-save` → `status=DRAFT_SAVED`。

> 草稿阶段不配置标签/摘要/分类等（后续可在编辑器或发布时补）。

---

### 5.4 `csdn publish` —— 发布

在草稿填写基础上，点「发布文章」`button.btn-publish` 打开弹窗 `.modal__publish-article`，依次配置：

| 配置 | 选择器 / 逻辑 |
| --- | --- |
| 标签 | 容器 `.mark_selection`；先点 `.tag__btn-tag` 打开选择框，清空草稿残留标签（`.mark_selection_box_el_tag .el-tag__close`），逐个 `fill + Enter`（最多 10 个），点 `.mark_selection_box .modal__close-button` 关闭；**回读 `.mark_selection_box_el_tag` 校验** |
| 首图 | `.img-selection-item img.select-cover` 第一张 → `.vicp-operate-btn` 确认；列表为空则跳过 |
| 摘要 | `.desc-box .el-textarea__inner` |
| 分类专栏 | 定位含「分类专栏」的 `.form-entry`，勾选已有专栏；匹配按**精确 > 已有包含设置值 > 设置值包含已有**评分取最优；勾选项是**隐藏 checkbox**，需触发 DOM `click()`；回读 `.tag__item-box` 校验 |
| 文章类型 | 原创 `input[value=original]` |
| 可见范围 | 公开 `input[value=public]` |
| 创作声明 | 下拉选「个人观点，仅供参考」（contains 匹配） |

**`--dry-run`**：完成所有填写与弹窗配置，但不点最终发布，返回 `status=DRY_RUN` 与 `publishConfig`。

**正式发布**：点弹窗底部 `.modal__button-bar button`（发布文章），从成功弹窗 `a.success-modal-btn[href*="/article/details/"]` 取链接（去查询参数），返回 `status=PUBLISHED` 与 `url`。

#### 5.4.1 渠道级发布配置（`conf/csdn.conf`）

文章的标题/标签/分类/摘要/专栏/封面来自博客顶部 SOP 元数据；下列**渠道级选项**来自 `conf/csdn.conf`，脚本执行时默认读取：

| conf 键 | 取值 | 默认 | 作用 |
| --- | --- | --- | --- |
| `blogType` | `original` / `repost` / `translated` | `original` | 文章类型 |
| `visibleRange` | `public` / `private` / `read_need_fans` / `read_need_vip` | `public` | 可见范围 |
| `creationStatement` | 文案（留空则不设置） | `个人观点，仅供参考` | 创作声明 |
| `cover` | `first` / `none` | `first` | 首图策略 |
| `imageWaitBase` / `imageWaitPerBlock` | 毫秒 | `2000` / `3000` | 正文图片异步上传等待估算 |

> 优先级：CLI 参数 > 元数据 > conf 默认。加载器见 `scripts/lib/channel-conf.js`。

```json
{ "ok": true, "channel": "csdn", "action": "publish",
  "data": { "status": "PUBLISHED", "url": "https://blog.csdn.net/xxx/article/details/123",
            "publishConfig": { "selectedCategories": ["项目管理工具", "# Maven&Gradle"],
              "resolvedCategories": [{ "name": "Maven&Gradle", "matched": "# Maven&Gradle" }],
              "unresolvedCategories": [], "coverSet": true, "creationStatement": "个人观点，仅供参考" } },
  "error": null }
```

---

### 5.5 `csdn delete` —— 删除博客

**步骤**

1. 进入内容管理页 `https://mp.csdn.net/mp_blog/manage/article`；
2. 按 `--id`（`a[href*="/article/details/<id>"]`）或 `--title` 定位文章行 `.article-list-item-mp`；
3. 悬停该行 → 行右侧「...」`.el_mcm-dropdown .el-dropdown-link`；
4. 下拉点「删除」`.el_mcm-dropdown-menu__item`；
5. 二次确认弹窗点「确定」`.btn-msg-confirm`；
6. **回查该行已从列表消失** → `DELETED`，否则 `DELETE_UNVERIFIED`。

> 关键：一定要点二次确认弹窗的「确定」，否则不会真正删除。

---

### 5.6 `csdn test publish` —— 发布 → 删除 链路自测

```bash
node scripts/publisher.js csdn test publish --file "/abs/blog.md"
```

组合 `publish` + `delete`：发布成功后立即按 URL/id 删除，返回 `PUBLISHED_AND_DELETED`，用于验证分发链路可用且不残留内容。

---

## 六、选择器对照表（`channels/csdn/selectors.js`）

| 键 | 用途 |
| --- | --- |
| `homeWriteNew` / `homeLoginFun` / `slider` | 首页创作入口 / 登录态 / 滑块 |
| `titleDisplay` / `titleInput` | 标题展示态 / 隐藏 input |
| `content` | 正文 `<pre contenteditable>` |
| `saveDraftButton` | 保存草稿 |
| `publishEntry` / `publishModal` | 打开发布弹窗 |
| `tagContainer` / `tagBox` / `tagSelected` / `tagAddBtn` / `tagDelete` / `tagInput` / `tagModalClose` | 标签容器 / 选择框 / 已选 / 添加 / 删除 / 输入 / 关闭 |
| `coverItem` / `coverConfirm` | 首图（已有图片第一张）/ 确认 |
| `summaryInput` | 摘要 |
| `formEntry` / `categoryAddBtn` / `categoryOptionCheckbox` / `optionInput` | 分类专栏 / 类型 / 可见范围 |
| `creationTrigger` / `creationOption` | 创作声明下拉与选项 |
| `finalPublishButton` / `successLink` | 最终发布 / 成功链接 |
| `manageItem` / `manageMore` / `manageMenuDelete` / `confirmButton` | 内容管理定位 / 「...」/ 下拉删除 / 二次确认 |

> 站点改版时**只改本文件**，业务流程代码零改动。

---

## 七、错误码与分支

| 情况 | 处理 |
| --- | --- |
| 未登录 | 需登录的动作默认自动拉起（有头）浏览器登录；`--no-login` 则直接报错 |
| 滑块验证 | `data.hasSlider=true`，需人工在浏览器完成；脚本不绕过 |
| 分类专栏不存在 | 记入 `publishConfig.unresolvedCategories`，跳过不创建，正文仍可发布 |
| 首图列表为空 | `publishConfig.coverSet=false`，跳过首图，正文正常发布 |
| 正文写入失败 | `ok=false`，列出各写入策略失败原因 → 检查 `.editor__inner` 选择器 |
| 删除未确认 | 返回 `DELETE_UNVERIFIED`，检查 `.btn-msg-confirm` 选择器 |

退出码：`0` 成功（含 `DRY_RUN`）/ `1` 执行异常 / `2` `checkLogin` 未登录或 `login` 超时。

---

## 八、验证与维护

```bash
npm run test:channels                    # 渠道单测（csdn / gzh），mock 模拟 Playwright
node scripts/publisher.js csdn publish --file "/abs/blog.md" --dry-run
node scripts/publisher.js csdn test publish --file "/abs/blog.md"
```

- `--dry-run` 返回 `contentStrategy`（生效的写入策略）、`contentLength` 与 `publishConfig`，据此判断各字段是否写入成功；
- `test publish` 验证完整链路且不留残留内容；
- 单测覆盖：`checkLogin / enterEditor / setTitle / setContent / preparePublish / publish / saveDraft / deleteBlog`。
