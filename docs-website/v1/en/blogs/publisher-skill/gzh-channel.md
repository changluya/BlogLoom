---
title: "BlogLoom Publisher Skill — WeChat Official Account (gzh) Channel Capabilities and Publishing Flow"
description: The gzh channel's action matrix, Markdown-to-rich-text conversion, publish configuration, selector reference and error branches.
---

# BlogLoom Publisher Skill — WeChat Official Account (gzh) Channel Capabilities and Publishing Flow

> Audience: developers and AI agents who need to understand, maintain or extend the BlogLoom publishing skill.
> Channel implementation: [`skills/blogloom-publisher-skill/scripts/channels/gzh/`](https://github.com/changluya/BlogLoom/tree/master/skills/blogloom-publisher-skill/scripts/channels/gzh/)
> Channel SOP: [`references/channels/gzh/sop.md`](https://github.com/changluya/BlogLoom/blob/master/skills/blogloom-publisher-skill/references/channels/gzh/sop.md)
> Common contract: [`references/00-tool-contract.md`](https://github.com/changluya/BlogLoom/blob/master/skills/blogloom-publisher-skill/references/00-tool-contract.md)
> The flow follows the proven `auto-sync-blog` WeChat implementation and has been **verified against a real Official Account backend**.

---

## 1. Channel overview

| Item | Value |
| --- | --- |
| Channel id | `gzh` |
| Platform | `https://mp.weixin.qq.com/cgi-bin/home` |
| Login page | Same as above (click the "登录" entry on the home page to reach the QR page) |
| Editor | Content management → Drafts → New creation → Article (opens `appmsg_edit_v2` in a **new tab**) |
| Content management | Content management → Drafts (`appmsg?action=list_card`) |
| Login credentials | Cookies `slave_sid` / `slave_user` / `data_ticket` |

**Key point**: visiting `cgi-bin/home` directly shows "请重新登录" (please log in again). You must click the home page's "登录" entry `#jumpUrl` to enter the backend with a token (if the session is valid, it enters directly). The not-logged-in state also issues cookies such as `wxuin` / `ua_id`, and **these must not be used to detect login**, or you get false positives.

The WeChat editor does not understand Markdown, so the body must first be rendered into HTML with inline styles via **md.doocs.org**, then copied as rich text back into the editor.

---

## 2. Action matrix

| Action | Description | Login required | Key return |
| --- | --- | --- | --- |
| `gzh checkLogin` | Detect login state | No | `loggedIn / user / cookieNames` |
| `gzh login` | Open browser for QR login and persist | No | `loggedIn / cookieNames` |
| `gzh publishDraft` | Configure the article then save as draft | Yes | `status=DRAFT_SAVED` |
| `gzh publish` | Publish (admin QR scan / review required) | Yes | `status=SUBMITTED / PUBLISHED` |
| `gzh delete` | Locate in drafts and delete (with confirmation and verification) | Yes | `status=DELETED / DELETE_UNVERIFIED` |
| `gzh test publish` | Combined self-test: **publish draft → delete** | Yes | `status=DRAFT_SAVED_AND_DELETED` |

> Publishing an Official Account article directly requires admin QR scan / review and is not suitable for automatic rollback, so `test publish` uses `channel.testMode='draft'`, i.e. **save draft → delete immediately**.
> A channel may declare `successStatuses: ['SUBMITTED']`; the core layer merges it into the success states if present.

---

## 3. Unified invocation

```bash
node scripts/publisher.js <channel> <action> [options]

# Examples
node scripts/publisher.js gzh checkLogin
node scripts/publisher.js gzh login
node scripts/publisher.js gzh publishDraft --file "/abs/blog.md" --author "长路" --wechat-name "长路Java"
node scripts/publisher.js gzh publish --file "/abs/blog.md" --dry-run
node scripts/publisher.js gzh delete --title "Article title"
node scripts/publisher.js gzh test publish --file "/abs/blog.md"
```

**Channel-specific parameters** (passed through verbatim as `--xxx`, parsed by `gzh/index.js`'s `augmentPayload`; the core CLI is unaware of them):

| Parameter | Meaning |
| --- | --- |
| `--author` | Author field (**required**; missing it prevents checking the "Original" dialog) |
| `--wechat-name` | Official Account name card inserted at the top of the body |
| `--collection` | Collection name (falls back to `category`) |
| `--group-send` / `--no-group-send` | Whether to mass-send on publish (off by default) |

---

## 4. SOP field mapping (Markdown → Official Account)

| SOP field | Official Account target |
| --- | --- |
| `title` | Article title |
| `articleSummary` | Article summary (**auto-truncated to ≤120 chars**) |
| `content` (body after removing the metadata block) | Body (styled via md.doocs.org, then pasted) |
| `![coverImg](url)` | Cover/first image (auto-selects the first image in the body) |
| `category` (or channel param `--collection`) | Collection (selected if matched, otherwise skipped) |
| Channel param `--author` / metadata `author`/`username` | Author field |
| Channel param `--wechat-name` / metadata `wechatName` | Official Account name card at the top of the body |
| Channel param `--group-send` / metadata `isGroupSend` | Whether to mass-send on publish |

---

## 5. Scenario flows in detail

### 5.1 `gzh checkLogin` — detect login state

**Steps**

1. `enterAdminHome`: open `cgi-bin/home`, click the home page's "登录" entry `#jumpUrl` to reach the token-bearing backend home;
2. If the account name `.weui-desktop_name` appears (e.g. "长路Java") → **logged in**;
3. Otherwise check the real session cookies `slave_sid` / `slave_user` / `data_ticket` (only issued after login);
4. If none → not logged in.

| Return | Next step |
| --- | --- |
| `loggedIn=true` | `publishDraft` / `publish` directly |
| `loggedIn=false` (exit code 2) | `publish` will auto-launch login, or run `gzh login` manually |

> Fix history: early versions used `wxuin`, which caused **not-logged-in to be misjudged as logged in**; the whitelist now only accepts session cookies, and you must click `#jumpUrl` first to reach the backend.

---

### 5.2 `gzh login` — QR login

**Steps**

1. Open `cgi-bin/home` (**headed mode**); the channel's `prepareLogin` clicks `#jumpUrl` to reach the QR page;
2. Scan with the **admin WeChat** to complete login;
3. Poll the login cookies every 2s; success when `slave_sid` etc. appear (10 minutes max by default; adjustable via `--timeout` / `PUBLISHER_LOGIN_TIMEOUT`);
4. Login state is saved to `~/.blogloom-publisher/chrome-profile-gzh` and can later be reused with `--mode headless`.

---

### 5.3 `gzh publishDraft` — configure then save as draft

**Steps**

1. **Enter the backend home**: `enterAdminHome` clicks `#jumpUrl`;
2. **Enter the editor**: Content management `.weui-desktop-menu_create` → Drafts `.weui-desktop-sub-menu__item a` → "新的创作" `button:has-text("新的创作")` → "文章" `.weui-desktop-dropdown__list-ele:has-text("文章")` (**opens the editor in a new tab**, switching automatically);
3. **Fill the title**: `.title-editor__input .ProseMirror` (contenteditable, **not the hidden `#title` textarea**), typed with the keyboard;
4. **Fill the author**: `#js_author_area #author` (**required**, otherwise the later Original dialog cannot be checked);
5. **Write the body** (md.doocs styling):
   - Open a conversion page at md.doocs.org → paste the Markdown → wait for rendering → click "复制" (`button:has-text("复制"), button:has-text("Copy")`) to write the rich-text HTML to the clipboard;
   - Paste back into the content ProseMirror `.rich_media_content .ProseMirror` (note this differs from the title ProseMirror);
   - After pasting, WeChat shows a "**内容结构检测**" (content structure check) dialog → click "继续插入" `button:has-text("继续插入")` for the content to actually be written;
   - If conversion fails, automatically falls back to writing the raw Markdown (`paste → cdp → keyboard`);
6. **Config area** (see 5.5 for the default strategies): cover / description / original / reward / collection / creation source;
7. **Save draft**: click "保存为草稿" `button:has-text("保存为草稿")` → `status=DRAFT_SAVED`.

**Return**

```json
{ "ok": true, "channel": "gzh", "action": "publishDraft",
  "data": { "status": "DRAFT_SAVED", "contentStrategy": "md-style-paste",
            "contentLength": 13944, "url": "https://mp.weixin.qq.com/...appmsgid=...",
            "publishConfig": { "coverSet": true, "summarySet": true, "original": true,
              "reward": true, "pay": false, "collection": "", "source": "个人观点，仅供参考" } },
  "error": null }
```

---

### 5.4 `gzh publish` — publish

On top of the draft filling + configuration, click `.js_bot_bar .mass_send` (labeled "发表") at the bottom:

1. Mass-send dialog `.new_mass_send_dialog`: if `--group-send` is not enabled, turn the mass-send switch off;
2. Click the dialog's primary button `.weui-desktop-btn_primary`;
3. Second confirmation `.double_check_dialog` → continue publishing;
4. Mass sending requires admin QR scan, returning `status=SUBMITTED` (or `PUBLISHED` when a success toast is detected).

> `--dry-run`: completes all filling and configuration but does not click final publish.

---

### 5.5 Default configuration strategies (`preparePublish`, completed automatically before saving the draft)

| Item | Default strategy | Key selector / logic |
| --- | --- | --- |
| Cover | Pick the **first** body image | Hover `#js_cover_area` → `.js_selectCoverFromContent` (only visible on hover) → first `.img_crop_panel .appmsg_content_img_item` → `button:has-text("下一步")` → on the cover-edit page `button:has-text("确认")` (note: not "完成/确定") |
| Description | From `articleSummary`, **auto-truncated to ≤120 chars** | `#js_description_area textarea` |
| Original | Text original → check "我已阅读" → confirm | `#js_original` opens the dialog; select `.weui-desktop-form__check-label:has-text("文字原创")`; the agreement checkbox is at `.original_agreement` (clicking the text opens the agreement page, so trigger a DOM click on the `<label>` instead); **the first dialog often doesn't respond → close and reopen** (implemented in `chooseOrigin`) |
| Reward | Enabled by default (requires declaring original first) | `#js_reward_setting_area .js_reward_open` → "赞赏设置" dialog → reward author + reward account + check agreement → confirm (**best-effort**, with a timeout guard) |
| Paid | **Not enabled** (product requirement) | —— |
| Comments | Keep the editor default | —— |
| Collection | Match by **category** `category`; skip if no match / no category | `#js_article_tags_area` → `.select-opt-li` with the same name |
| Creation source | 个人观点，仅供参考 | `#js_claim_source_area .js_claim_source_desc` → dialog select `.weui-desktop-form__check-label:has-text("个人观点，仅供参考")` → `button:has-text("确认")` |

> **The Original/Reward dialogs are custom WeChat components**: they respond unreliably to synthetic automation clicks. `chooseOrigin` uses a "fill author → close and reopen with retries" strategy; `enableRewardAndPay` has an overall timeout (it won't stall the main flow). Paid is disabled per the requirement.

#### 5.5.1 Channel-level publishing config (`conf/gzh.conf`)

The article title/tags/category/summary/body come from the blog's top SOP metadata; the **channel-level options** in the table above come from `conf/gzh.conf`, read by default at runtime:

| conf key | Value | Default | Purpose |
| --- | --- | --- | --- |
| `author` | Text | `长路` | Author field (required; CLI `--author` overrides) |
| `wechatName` | Text | empty | Official Account name card at the top of the body (CLI `--wechat-name` overrides) |
| `collection` | Text | empty | Collection name; empty falls back to `category` matching (CLI `--collection` overrides) |
| `groupSend` | `true` / `false` | `false` | Whether to mass-send on publish (CLI `--group-send` overrides) |
| `cover` | `first` / `none` | `first` | Cover strategy |
| `summaryMaxLength` | integer | `120` | Max description length |
| `enableOriginal` | `true` / `false` | `true` | Whether to declare original |
| `enableReward` | `true` / `false` | `true` | Whether to enable reward |
| `enablePay` | `true` / `false` | `false` | Whether to enable paid (off by default) |
| `creationSource` | Text | `个人观点，仅供参考` | Creation source |
| `mdStyleUrl` | URL | `https://md.doocs.org/` | Body styling tool URL |

> Priority: CLI args > metadata extra > conf default. See `scripts/lib/channel-conf.js` for the loader.

---

### 5.6 `gzh delete` — delete a post

**Steps**

1. Go to Content management → Drafts;
2. Locate the card `.weui-desktop-card` by `--title` (or by `data-appid`);
3. Hover the card → the inline "删除" `.weui-desktop-link:has-text("删除")`;
4. In the confirmation popover `.weui-desktop-popover`, click the primary button `button.weui-desktop-btn_primary:has-text("删除")`;
5. **Poll and verify the card has disappeared from the list** → `DELETED`, otherwise `DELETE_UNVERIFIED`.

> Fix history: deletion is not in a "..." menu but an inline "编辑 / 删除"; the confirm button must be the actual `button` (not the `.weui-desktop-btn_wrp` outer div).

---

### 5.7 `gzh test publish` — publish draft → delete flow self-test

```bash
node scripts/publisher.js gzh test publish --file "/abs/blog.md"
```

`channel.testMode='draft'`: **save draft → delete the draft immediately**, returning `status=DRAFT_SAVED_AND_DELETED` to verify the flow works and leaves no residual content.

---

## 6. Selector reference (`channels/gzh/selectors.js`)

| Key | Purpose |
| --- | --- |
| `accountName` / `jumpUrl` | Login account name / home login entry |
| `menuCreate` / `subMenuItem` | Content management / drafts |
| `newCreateButton` / `createArticleItem` | "新的创作" / dropdown "文章" |
| `titleInput` / `titleFallbackInput` / `authorInput` | Title ProseMirror / fallback title / author |
| `editor` / `continueInsert` / `insertProfileBtn` | Content ProseMirror / "继续插入" / Official Account name card |
| `afterArea` / `coverArea` / `selectCoverFromContent` / `imgCropPanel` / `contentImgItem` / `coverNext` / `coverDone` / `coverPreview` | Cover selection / next / confirm / verify |
| `descTextarea` | Description (≤120) |
| `originalBox` / `original` / `originalTypeText` / `originalAgreementInput` / `originalAgreementLabel` / `dialogConfirm` / `dialogCancel` | Original dialog |
| `rewardArea` / `rewardSwitch` / `rewardTypeAuthor` / `rewardAccountItem` / `rewardAgreementInput` / `rewardDialogConfirm` | Reward settings |
| `payArea` / `paySwitch` | Paid (no action by default) |
| `articleTagsArea` / `articleTagsLabel` / `collectionOption` | Collection |
| `claimSourceArea` / `claimSourceDesc` / `claimSourceOption` / `claimConfirm` | Creation source |
| `bottomBar` / `massSend` / `massSendDialog` / `massSendSwitch` / `doubleCheckDialog` / `btnPrimary` | Publish and mass-send dialogs |
| `saveButton` / `saveFallback` | Save as draft |
| `draftItem` / `draftDeleteLink` / `confirmDelete` | Draft card / inline delete / confirm popover |

The md.doocs conversion selectors are centralized in `channels/gzh/format.js` (`editable: .cm-content`, `copyButton`).

> When the site changes, **only edit the selector files**; business-flow code stays untouched.

---

## 7. Error codes and branches

| Situation | Handling |
| --- | --- |
| Not logged in | Login-required actions auto-launch a (headed) login by default; `--no-login` errors out directly |
| md.doocs conversion failure | Falls back to writing raw Markdown, `contentStrategy=raw-*` |
| "内容结构检测" appears | Automatically clicks "继续插入" |
| Original dialog not responding | Close and reopen with retries; if still failing, skip (does not affect saving) |
| Reward dialog stuck | Skipped after a timeout guard (does not affect saving) |
| Collection not matched | Skipped; the body can still be saved |
| Delete unconfirmed | Returns `DELETE_UNVERIFIED` |

Exit codes: `0` success / `1` execution error / `2` `checkLogin` not logged in or `login` timed out.

---

## 8. Verification and maintenance

```bash
npm run test:channels
node scripts/publisher.js gzh publishDraft --file "/abs/blog.md" --dry-run
node scripts/publisher.js gzh test publish --file "/abs/blog.md"
```

- Unit tests cover: `checkLogin / prepareLogin / enterEditor / setTitle / setAuthor / setContent (incl. continue-insert) / preparePublish / publish / saveDraft / deleteBlog / augmentPayload`;
- `contentStrategy=md-style-paste` means the md.doocs styling conversion succeeded;
- **Real-device verification conclusion**: title / author / body styling / cover / description(≤120) / original / collection / creation source / save draft / delete / `test publish` all pass; **reward is best-effort**, **paid is disabled**.

---

## Next steps

- [BlogLoom Publisher Skill 01: One-click publishing skill from 0 to 1](/v1/en/blogs/blogloom-publisher-skill/01-publisher-skill)
- [WeChat (gzh) design and implementation](/v1/en/blogs/publisher-skill/gzh-design)
- [CSDN channel capabilities and publishing flow](/v1/en/blogs/publisher-skill/csdn-channel)
