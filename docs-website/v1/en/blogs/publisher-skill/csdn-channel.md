---
title: "BlogLoom Publisher Skill — CSDN Channel Capabilities and Publishing Flow"
description: The CSDN channel's full action matrix, SOP field mapping, scenario flows, selector reference and error branches.
---

# BlogLoom Publisher Skill — CSDN Channel Capabilities and Publishing Flow

> Audience: developers and AI agents who need to understand, maintain or extend the BlogLoom publishing skill.
> Channel implementation: [`skills/blogloom-publisher-skill/scripts/channels/csdn/`](https://github.com/changluya/BlogLoom/tree/master/skills/blogloom-publisher-skill/scripts/channels/csdn/)
> Channel SOP: [`references/channels/csdn/sop.md`](https://github.com/changluya/BlogLoom/blob/master/skills/blogloom-publisher-skill/references/channels/csdn/sop.md)
> Common contract: [`references/00-tool-contract.md`](https://github.com/changluya/BlogLoom/blob/master/skills/blogloom-publisher-skill/references/00-tool-contract.md)

---

## 1. Channel overview

| Item | Value |
| --- | --- |
| Channel id | `csdn` |
| Platform | `https://www.csdn.net/` |
| Login page | `https://passport.csdn.net/login` |
| Editor | `https://editor.csdn.net/md/` |
| Content management | `https://mp.csdn.net/mp_blog/manage/article` |
| Login credentials | Cookies `UserToken` + `UserInfo` |

CSDN is the skill's **first integrated channel**. Primary operations go through Playwright (visible browser, real clicks/typing/pasting), while CDP is only used for low-level enhancement (reusing login state, `Input.insertText` as a rich-text fallback, reading HttpOnly cookies to detect login). It defaults to **headed mode**, visible throughout.

---

## 2. Action matrix

| Action | Description | Login required | Key return |
| --- | --- | --- | --- |
| `csdn checkLogin` | Detect login state | No | `loggedIn / user / hasSlider` |
| `csdn login` | Open browser for QR login and persist | No | `loggedIn / cookieNames` |
| `csdn publishDraft` | Save as draft | Yes | `status=DRAFT_SAVED` |
| `csdn publish` | Publish the post | Yes | `status=PUBLISHED, url` |
| `csdn delete` | Locate in content management and delete (with confirmation and verification) | Yes | `status=DELETED / DELETE_UNVERIFIED` |
| `csdn test publish` | Combined self-test: **publish → delete immediately** | Yes | `status=PUBLISHED_AND_DELETED` |

> `test publish` verifies "the publishing flow works and leaves no residual content". CSDN supports rollback after publishing, hence `PUBLISHED_AND_DELETED`.

---

## 3. Unified invocation

```bash
node scripts/publisher.js <channel> <action> [options]

# Examples
node scripts/publisher.js csdn checkLogin
node scripts/publisher.js csdn login
node scripts/publisher.js csdn publishDraft --file "/abs/blog.md"
node scripts/publisher.js csdn publish --file "/abs/blog.md" --dry-run
node scripts/publisher.js csdn publish --file "/abs/blog.md" --mode headless
node scripts/publisher.js csdn delete --url "https://blog.csdn.net/x/article/details/123"
node scripts/publisher.js csdn test publish --file "/abs/blog.md"
```

Unified output (stdout):

```json
{ "ok": true, "channel": "csdn", "action": "publish", "data": { ... }, "error": null }
```

Logs go to stderr (prefixed `[publisher]`), which makes parsing by agents easy.

---

## 4. SOP field mapping (Markdown → CSDN)

| SOP field | CSDN target |
| --- | --- |
| `title` | Article title |
| `tags` (comma-separated, first 10) | Article tags |
| `columns` (multiple, matched against existing columns) | Category columns |
| `articleSummary` | Article summary |
| `![coverImg](url)` | Cover / first image (optional) |
| `content` (body after removing the metadata block) | Body (Markdown source) |

---

## 5. Scenario flows in detail

### 5.1 `csdn checkLogin` — detect login state

**Steps**

1. Open the home page `https://www.csdn.net/`;
2. Read the `.toolbar-btn-loginfun` text: if it says "登录" (login), the user is not logged in;
3. If the "创作" (create) entry is present and not in a login state, or the cookies contain `UserToken` + `UserInfo` → logged in;
4. Also return whether a slider `.verify-move-block` exists (`data.hasSlider`).

**Return**

```json
{ "ok": true, "channel": "csdn", "action": "checkLogin",
  "data": { "loggedIn": true, "user": "长路", "reason": "detected CSDN login state",
            "hasSlider": false, "loginEntryVisible": false }, "error": null }
```

| Return | Next step |
| --- | --- |
| `loggedIn=true` | `publish` / `publishDraft` directly |
| `loggedIn=false` (exit code 2) | `publish` will auto-launch login, or run `csdn login` manually |

---

### 5.2 `csdn login` — QR login

**Steps**

1. Open `https://passport.csdn.net/login` (**headed mode**);
2. The user scans the QR code / types credentials (including any slider verification, which the script does not bypass);
3. Poll the login cookies every 2s; success when `UserToken` / `UserInfo` appear (10 minutes max by default);
4. Login state is saved in a dedicated profile: `~/.blogloom-publisher/chrome-profile-csdn`.

---

### 5.3 `csdn publishDraft` — save draft

**Steps**

1. **Enter the editor**: click "创作" `.toolbar-btn-write-new` on the home page (fall back to opening `editor.csdn.net/md/` directly);
2. **Fill the title**: click the display state `.article-bar__title-display` ("【无标题】") to reveal the hidden `input.article-bar__title`, clear it, type the title, then read back to verify;
3. **Write the body**:
   - Click `.editor__inner.markdown-highlighting` (`<pre contenteditable>`);
   - Select all and delete the default welcome content;
   - Paste using the `paste → cdp → keyboard` strategy (clipboard paste first, CDP `Input.insertText` as a fallback);
   - **When the body contains images, estimate a wait based on image count** (2s base, +3s per 5 images) to ensure async upload completes;
4. Click "保存草稿" `button.btn-save` at the top → `status=DRAFT_SAVED`.

> Tags/summary/category are not configured during the draft stage (they can be added later in the editor or when publishing).

---

### 5.4 `csdn publish` — publish

On top of the draft flow, click "发布文章" `button.btn-publish` to open the `.modal__publish-article` dialog, and configure each item:

| Item | Selector / logic |
| --- | --- |
| Tags | Container `.mark_selection`; first click `.tag__btn-tag` to open the selection box, clear leftover draft tags (`.mark_selection_box_el_tag .el-tag__close`), then `fill + Enter` each (max 10), click `.mark_selection_box .modal__close-button` to close; **read back `.mark_selection_box_el_tag` to verify** |
| Cover | First `.img-selection-item img.select-cover` → confirm with `.vicp-operate-btn`; skip if the list is empty |
| Summary | `.desc-box .el-textarea__inner` |
| Category columns | Locate the `.form-entry` containing "分类专栏", check existing columns; matching scores by **exact > existing contains configured > configured contains existing**; checkboxes are **hidden**, so trigger a DOM `click()`; read back `.tag__item-box` to verify |
| Article type | Original `input[value=original]` |
| Visibility | Public `input[value=public]` |
| Creation statement | Select "个人观点，仅供参考" from the dropdown (contains match) |

**`--dry-run`**: completes all filling and dialog configuration but does not click final publish, returning `status=DRY_RUN` and `publishConfig`.

**Actual publish**: click `.modal__button-bar button` (publish) at the bottom of the dialog, take the link from the success dialog `a.success-modal-btn[href*="/article/details/"]` (strip query params), and return `status=PUBLISHED` with `url`.

#### 5.4.1 Channel-level publishing config (`conf/csdn.conf`)

The article's title/tags/category/summary/columns/cover come from the blog's top SOP metadata; the following **channel-level options** come from `conf/csdn.conf`, read by default at runtime:

| conf key | Value | Default | Purpose |
| --- | --- | --- | --- |
| `blogType` | `original` / `repost` / `translated` | `original` | Article type |
| `visibleRange` | `public` / `private` / `read_need_fans` / `read_need_vip` | `public` | Visibility |
| `creationStatement` | Text (empty = not set) | `个人观点，仅供参考` | Creation statement |
| `cover` | `first` / `none` | `first` | Cover strategy |
| `imageWaitBase` / `imageWaitPerBlock` | milliseconds | `2000` / `3000` | Async image upload wait estimation |

> Priority: CLI args > metadata > conf default. See `scripts/lib/channel-conf.js` for the loader.

```json
{ "ok": true, "channel": "csdn", "action": "publish",
  "data": { "status": "PUBLISHED", "url": "https://blog.csdn.net/xxx/article/details/123",
            "publishConfig": { "selectedCategories": ["项目管理工具", "# Maven&Gradle"],
              "resolvedCategories": [{ "name": "Maven&Gradle", "matched": "# Maven&Gradle" }],
              "unresolvedCategories": [], "coverSet": true, "creationStatement": "个人观点，仅供参考" } },
  "error": null }
```

---

### 5.5 `csdn delete` — delete a post

**Steps**

1. Go to the content management page `https://mp.csdn.net/mp_blog/manage/article`;
2. Locate the post row `.article-list-item-mp` by `--id` (`a[href*="/article/details/<id>"]`) or `--title`;
3. Hover the row → the "..." on the right `.el_mcm-dropdown .el-dropdown-link`;
4. Click "删除" in the dropdown `.el_mcm-dropdown-menu__item`;
5. Click "确定" in the confirmation dialog `.btn-msg-confirm`;
6. **Verify the row has disappeared from the list** → `DELETED`, otherwise `DELETE_UNVERIFIED`.

> Key point: you must click "确定" in the confirmation dialog, or nothing is actually deleted.

---

### 5.6 `csdn test publish` — publish → delete flow self-test

```bash
node scripts/publisher.js csdn test publish --file "/abs/blog.md"
```

Combines `publish` + `delete`: after a successful publish, delete immediately by URL/id, returning `PUBLISHED_AND_DELETED` to verify the publishing flow works and leaves no residual content.

---

## 6. Selector reference (`channels/csdn/selectors.js`)

| Key | Purpose |
| --- | --- |
| `homeWriteNew` / `homeLoginFun` / `slider` | Home create entry / login state / slider |
| `titleDisplay` / `titleInput` | Title display state / hidden input |
| `content` | Body `<pre contenteditable>` |
| `saveDraftButton` | Save draft |
| `publishEntry` / `publishModal` | Open publish dialog |
| `tagContainer` / `tagBox` / `tagSelected` / `tagAddBtn` / `tagDelete` / `tagInput` / `tagModalClose` | Tag container / select box / selected / add / delete / input / close |
| `coverItem` / `coverConfirm` | Cover (first existing image) / confirm |
| `summaryInput` | Summary |
| `formEntry` / `categoryAddBtn` / `categoryOptionCheckbox` / `optionInput` | Category column / type / visibility |
| `creationTrigger` / `creationOption` | Creation statement dropdown and options |
| `finalPublishButton` / `successLink` | Final publish / success link |
| `manageItem` / `manageMore` / `manageMenuDelete` / `confirmButton` | Content management locate / "..." / dropdown delete / confirm |

> When the site changes, **only edit this file**; business-flow code stays untouched.

---

## 7. Error codes and branches

| Situation | Handling |
| --- | --- |
| Not logged in | Actions that require login auto-launch a (headed) browser login by default; with `--no-login` they error out directly |
| Slider verification | `data.hasSlider=true`; the user must complete it in the browser; the script does not bypass it |
| Category column missing | Recorded in `publishConfig.unresolvedCategories`, skipped (not created); the body can still publish |
| Cover list empty | `publishConfig.coverSet=false`, cover skipped, body publishes normally |
| Body write failure | `ok=false`, listing the failure reason of each write strategy → check the `.editor__inner` selector |
| Delete unconfirmed | Returns `DELETE_UNVERIFIED`; check the `.btn-msg-confirm` selector |

Exit codes: `0` success (including `DRY_RUN`) / `1` execution error / `2` `checkLogin` not logged in or `login` timed out.

---

## 8. Verification and maintenance

```bash
npm run test:channels                    # channel unit tests (csdn / gzh), Playwright mocked
node scripts/publisher.js csdn publish --file "/abs/blog.md" --dry-run
node scripts/publisher.js csdn test publish --file "/abs/blog.md"
```

- `--dry-run` returns `contentStrategy` (the effective write strategy), `contentLength` and `publishConfig`, useful to confirm each field was written;
- `test publish` verifies the full flow and leaves no residual content;
- Unit tests cover: `checkLogin / enterEditor / setTitle / setContent / preparePublish / publish / saveDraft / deleteBlog`.

---

## Next steps

- [BlogLoom Publisher Skill 01: One-click publishing skill from 0 to 1](/v1/en/blogs/blogloom-publisher-skill/01-publisher-skill)
- [CSDN channel design and implementation](/v1/en/blogs/publisher-skill/csdn-design)
- [WeChat Official Account (gzh) channel capabilities and publishing flow](/v1/en/blogs/publisher-skill/gzh-channel)
