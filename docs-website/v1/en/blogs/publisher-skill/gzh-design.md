---
title: "BlogLoom Publisher Skill: WeChat Official Account (gzh) Design and Implementation"
description: "Starting from \"the WeChat editor doesn't understand Markdown\": styling conversion, login detection, editor deep-linking, publish configuration and state judgment."
---

# 1. Background

## 1.1 The problem to solve

After publishing BlogLoom's local Markdown to CSDN, we moved on to the second channel: the **WeChat Official Account**.

It is an unavoidable channel for content distribution, but its editor is fundamentally different from CSDN's:

- CSDN's editor **natively understands Markdown** — pasting Markdown source just works;
- The WeChat Official Account editor is **ProseMirror rich text** and **does not understand Markdown** — pasting Markdown only yields plain text full of `#`, `*` and backticks.

Beyond that, WeChat has an extremely tedious publish configuration: the cover must be "selected from the body", declaring original requires checking "我已阅读", and mass sending requires an admin QR scan... each step is something only a human can reliably operate.

## 1.2 Why a separate article

The previous article (CSDN) covered the "editor natively eats Markdown, config everything via dialogs" pattern; this one covers three entirely different difficulties:

| Dimension | CSDN | Official Account (gzh) |
| --- | --- | --- |
| Editor core | Native Markdown | ProseMirror rich text, **no Markdown** |
| Body writing | Paste Markdown source directly | Convert to styled HTML via md.doocs, then paste |
| Cover | Select the first existing image in the publish dialog | Editor bottom "select from body" + image upload verification |
| Publish | Click publish in the dialog | Mass-send dialog + second confirmation + **admin QR scan** |
| Terminal state | `PUBLISHED` | `SUBMITTED` (submitted, pending scan/review) |

## 1.3 Design goals

| Goal | Meaning |
| --- | --- |
| Body formatting preserved | Use md.doocs to render Markdown into WeChat-styled rich text, keeping the format after pasting |
| Complete publish config | Cover / summary / original / collection / creation source / reward — none missing |
| Self-test without residue | `testMode=draft`, self-test does "publish draft → delete" to avoid auto mass-send/review |
| Machine-judgeable | After publishing it enters a scan/review flow, so the terminal state is `SUBMITTED` rather than `PUBLISHED` |

---

## 2. Tech stack

The gzh channel is likewise built on the "Playwright primary + CDP low-level" skeleton, but with one extra key file at the channel layer:

```text
scripts/channels/gzh/
├── index.js       # Business flow: login detection, enter editor, write body, publish config, publish, delete
├── selectors.js   # Centralized WeChat DOM selectors (only edit here on redesign)
└── format.js      # Body styling conversion: Markdown → WeChat rich text (md.doocs.org)
```

**The core differences are concentrated in `format.js`**: it resolves the fundamental contradiction that "WeChat doesn't understand Markdown".

---

## 3. Capabilities

### 3.1 Action matrix

| Action | Command | Description |
| --- | --- | --- |
| Detect login | `gzh checkLogin` | Judge login state (account name + real session cookie cross-verification) |
| QR login | `gzh login` | Open the backend for QR login, persisting state |
| Save draft | `gzh publishDraft` | Fill title/author/body + bottom config, then save draft |
| Publish | `gzh publish` | Complete the publish config and trigger publishing (mass send needs a QR scan) |
| Delete | `gzh delete` | Locate in drafts and delete, with confirmation and verification |
| Flow self-test | `gzh test publish` | Publish draft → delete (`testMode=draft`) |

### 3.2 Login detection (checkLogin)

| Item | Content |
| --- | --- |
| **Core idea** | Use the "backend account name" as the primary judgment, then cross-verify with a "real session cookie" whitelist, avoiding mistaking `wxuin`/`ua_id` issued in the not-logged-in state for credentials |
| **Detailed flow** | ① Open the backend home `mp.weixin.qq.com/cgi-bin/home` → ② if `.weui-desktop_name` (account name) appears, logged in → ③ otherwise read whether cookies contain `slave_sid`/`slave_user`/`data_ticket` → ④ if none, treat as not logged in → ⑤ return `{loggedIn, user, cookieNames, loginEntryVisible, reason}` |

> See [4.2](#42-login-detection-account-name--real-session-cookie) for the full rationale and code.

### 3.3 QR login (login)

| Item | Content |
| --- | --- |
| **Core idea** | When not logged in, the home page shows the `#jumpUrl` login entry; clicking it leads to the QR page, and on success the session cookie is persisted to a dedicated profile |
| **Detailed flow** | ① Open the backend home → ② if `#jumpUrl` exists, click it to reach the QR login page → ③ the user scans → ④ the script polls `slave_sid`/`slave_user`/`data_ticket` to judge success → ⑤ save state to `~/.blogloom-publisher/chrome-profile-gzh` |

### 3.4 Body styling conversion (format.js)

| Item | Content |
| --- | --- |
| **Core idea** | The WeChat editor (ProseMirror) doesn't understand Markdown, so first use md.doocs.org to render Markdown into styled HTML rich text, then paste it back into the editor |
| **Detailed flow** | ① Open a conversion page at `md.doocs.org` in the same browser context → ② clear default content, paste the Markdown into `.cm-content` → ③ first set "图注" (caption) to "不显示" (hidden) to avoid caption text under images → ④ click "复制" to write rich text to the clipboard → ⑤ close the conversion page → ⑥ paste back on the main editor with `Mod+V` |

> See [4.1](#41-body-styling-conversion-markdown--wechat-rich-text-key) for the full rationale and code.

### 3.5 Entering the editor (enterEditor)

| Item | Content |
| --- | --- |
| **Core idea** | The WeChat editor is deeply nested, and clicking "文章" may open a new tab, so we must listen for the new page and switch context |
| **Detailed flow** | ① Open the backend home → ② click "内容管理" → "草稿箱" → ③ click "新的创作" → "文章" → ④ use `context.waitForEvent('page')` to listen; if a new tab opens, switch to it → ⑤ wait for the `.ProseMirror` editor to be ready → ⑥ detect redirect to `/loginpage/` and throw `AUTH_REQUIRED` if so |

### 3.6 Title / author writing (setTitle / setAuthor)

| Item | Content |
| --- | --- |
| **Core idea** | The title/author are contenteditable/ProseMirror, where `fill` isn't always reliable — keyboard typing is most robust; the author must be filled first, otherwise the Original dialog's "我已阅读" cannot be checked |
| **Detailed flow** | ① Locate the title `.title-editor__input .ProseMirror` (fallback `#title`) → ② select-all and clear → ③ `keyboard.type` character by character → ④ read back to verify; likewise write the author into `#js_author_area #author` → ⑤ read back as a verification fallback |

### 3.7 Body writing (setContent)

| Item | Content |
| --- | --- |
| **Core idea** | Prefer md.doocs styled paste; handle WeChat's "内容结构检测" (content structure check) dialog after pasting; if the read-back is empty, fall back to the raw write strategy |
| **Detailed flow** | ① Optional: insert the Official Account name card at the top of the body → ② invoke the 3.4 styling conversion → ③ paste into the body editor with `Mod+V` → ④ repeatedly click "继续插入" to handle the content-structure-check dialog → ⑤ read back the body length → ⑥ if 0, fall back to `writeContent(['paste','cdp','keyboard'])` |

### 3.8 Publish configuration items

| Item | Core idea | Detailed flow |
| --- | --- | --- |
| **Cover** | The cover panel renders incrementally in body order, so wait for uploads before taking the first image, and verify the upload succeeded | ① Locate `#js_cover_area` at the bottom → ② hover "从正文选择" → ③ poll the panel's `.appmsg_content_img_item` count until it reaches the total body images → ④ take the first → ⑤ "下一步" "完成" → ⑥ verify a `background-image` appears in the preview (retry once on failure) |
| **Summary** | Fill the textarea directly, auto-truncating over-length | ① Locate `#js_description_area textarea` → ② `slice` to the conf limit (120 chars by default) → ③ `fill(text)` |
| **Original** | WeChat's custom component often doesn't respond the first time, requiring retries + a DOM click on the agreement | ① Click "原创" → ② click "文字原创" → ③ repeatedly trigger a DOM click on the `<label>` until "我已阅读" `isChecked()` → ④ "确定" → ⑤ verify the dialog closed; on failure, close and retry (max 4 rounds) |
| **Reward** | Requires declaring original first; clicking the switch shows a "赞赏设置" dialog to pick an account + check the agreement + confirm | ① Wait for `#js_reward_setting_area` to be visible → ② click the switch → ③ in the dialog select "赞赏作者" + the first reward account + check the agreement → ④ confirm → ⑤ verify `isRewardOn` (with an overall timeout guard) |
| **Paid** | Disabled per product requirement | Skip (keep the default off) |
| **Collection** | Match a same-named collection; skip (don't create) if absent | ① Click the `#js_article_tags_area` label → ② in the dialog iterate `.select-opt-li` for a same-name match → ③ click "确认" |
| **Creation source** | Dropdown contains-match the text | ① Click the `#js_claim_source_area` description → ② match "个人观点，仅供参考" among the options → ③ click "确认" |

### 3.9 Publish (publish)

| Item | Content |
| --- | --- |
| **Core idea** | Publishing may trigger a mass send (requiring an admin QR scan), so the terminal state is `SUBMITTED` rather than `PUBLISHED` |
| **Detailed flow** | ① Click "群发" `.mass_send` at the bottom → ② dialog 1: turn off the mass-send switch for a non-mass send, then confirm → ③ dialog 2: second confirmation "继续发表" → ④ wait for the success toast → ⑤ on hit return `PUBLISHED`, otherwise return `SUBMITTED` and prompt the user to complete the QR scan in the browser |

### 3.10 Save draft / delete

| Feature | Core idea | Detailed flow |
| --- | --- | --- |
| **Save draft** | The draft scenario also configures cover/summary/original/collection/creation source before saving | ① Close residual dialogs → ② invoke the publish config → ③ click "保存为草稿" `button:has-text("保存为草稿")` → ④ return `DRAFT_SAVED` |
| **Delete** | Go to drafts, locate the article card, delete, confirm the popover, then verify the card is gone | ① Go to "内容管理 → 草稿箱" → ② locate `.weui-desktop-card` by appmsgid/title → ③ hover → ④ click the inline "删除" → ⑤ click "删除" in the confirm popover → ⑥ poll to verify the card is gone → ⑦ return `DELETED` (otherwise `DELETE_UNVERIFIED`) |

### 3.11 Channel-specific parameter pass-through (augmentPayload)

| Item | Content |
| --- | --- |
| **Core idea** | The core CLI is unaware of channel business; `--key value` is passed through verbatim and assembled by the channel's `augmentPayload`, with priority "CLI > metadata extra > conf" |
| **Detailed flow** | ① Parse `--author`/`--wechat-name`/`--collection`/`--group-send` → ② take the priority value step by step against `blog.extra` and `conf` → ③ return `{author, wechatName, collection, groupSend}` for publishing/config |

---

## 4. Core design rationale

This chapter breaks down the gzh channel's most critical mechanisms.

### 4.1 Body styling conversion: Markdown → WeChat rich text (key)

**Problem**: the WeChat editor doesn't understand Markdown; pasting directly only yields plain text and loses all formatting.

**Approach**: following the proven `auto-sync-blog`, use the online typesetting tool **md.doocs.org** to first render Markdown into HTML with inline styles, then copy that rich text and paste it back into the WeChat editor.

```js
async function convertMarkdownToGzhHtml(page, markdown) {
  // 1. Open a conversion page in the same browser context (closed after conversion, no impact on the main editor)
  const converter = await context.newPage();
  await converter.goto('https://md.doocs.org/', { waitUntil: 'domcontentloaded' });
  const editor = converter.locator('.cm-content');
  await editor.waitFor({ state: 'visible' });

  // 2. Clear default content and paste the Markdown (via clipboard, avoiding the performance cost of typing long text)
  await converter.keyboard.press(`${MOD}+A`);
  await converter.keyboard.press('Backspace');
  await converter.evaluate((v) => navigator.clipboard.writeText(v), markdown);
  await converter.keyboard.press(`${MOD}+V`);

  // 3. Click "复制" to write the rendered rich text to the clipboard
  await converter.locator('button:has-text("复制")').click();
  return { converted: true };
}
```

**Key points**:

- **New page in the same context**: the conversion page shares cookies/login state with the main editor and is `close()`d afterward, not polluting the main flow;
- **Clipboard rather than typing**: typing long text character by character is slow; a clipboard paste is done in one go;
- **Set "图注" (caption) to "不显示" before copying**: md.doocs renders caption text below images by default, which the WeChat body doesn't need, so select "不显示" in the right-side style panel first.

**Implementation detail for hiding captions**: the style panel is collapsed by default; when the "图注" block isn't visible, first click "样式" at the top right to expand it; use XPath to first target the "图注/Caption" heading, then take the "不显示/None" button inside its parent block, avoiding clicking same-named buttons in other blocks; verify the selected state with the `border-primary` class:

```js
const cls = (await hideBtn.getAttribute('class')) || '';
return /border-primary/.test(cls);
```

### 4.2 Login detection: account name + real session cookie

**Problem**: WeChat's login credentials are complex — how do we avoid "false positives"?

**Rationale**: double protection —

1. On the backend home, if the account name `.weui-desktop_name` appears → logged in;
2. Otherwise check the **real session cookies** (`slave_sid` / `slave_user` / `data_ticket`) — they are **only issued after login**;
3. If none, treat as not logged in.

**Key point**: `wxuin` / `ua_id` must not be used — they are **also issued when not logged in**, causing false positives. The credential whitelist contains only `LOGIN_COOKIE_NAMES = ['slave_sid', 'slave_user', 'data_ticket']`.

### 4.3 Entering the editor: content management → drafts → new creation

**Problem**: the WeChat editor is deeply nested, and "文章" may open a new tab.

**Rationale**: the route chain is backend home → content management → drafts → "新的创作" → "文章". When clicking "文章", **listen for a new tab with `context.waitForEvent('page')`**, and if one opens, switch to it:

```js
const newPage = await Promise.all([
  context.waitForEvent('page', { timeout: 15000 }).catch(() => null),
  addBtn.click({ timeout: 8000 }).catch(() => {}),
]).then(([np]) => np);
if (newPage) editorPage = newPage;
```

At the same time, detect whether it's redirected to the login page (`/loginpage/`) and throw `AUTH_REQUIRED` if so.

### 4.4 Title / author writing

The title editor is contenteditable/ProseMirror, where `fill` isn't always reliable — **keyboard typing is most robust**: select-all and clear → `keyboard.type` character by character → read back to verify.

The author field (`#js_author_area #author`) **must be filled first** — without an author, the later Original dialog's "我已阅读" cannot be checked. Likewise use select-all replace + read-back verification.

### 4.5 Body writing: styled paste + fallback

```js
async function setContent(page, markdown, ctx) {
  // 1. Optional: insert the Official Account name card at the top of the body
  if (profileName) await insertProfileCard(page, profileName);

  // 2. Prefer the md.doocs styling conversion
  const converted = await convert(page, markdown);

  // 3. Paste into ProseMirror
  await page.keyboard.press(`${MOD}+V`);

  // 4. Handle WeChat's "内容结构检测" dialog → click "继续插入" for it to actually be written
  await confirmInsertDialog(page);

  // 5. Read back the length; if 0, fall back to the raw Markdown write strategy
  if (length === 0) return await writeContent(page, editor, markdown, ['paste', 'cdp', 'keyboard']);
}
```

**Key point**: after pasting external rich text, WeChat shows a "内容结构检测" (content structure check) dialog — **you must click "继续插入"**, or the content won't actually be written into the body. The raw `writeContent` fallback is kept so that publishing still works if styling conversion fails.

### 4.6 Cover: select from body + upload verification

**Problem**: the WeChat cover panel renders incrementally in body order, so before images finish uploading, the first one isn't the body's first image.

**Rationale**: `setCover(page, firstBox, expectedImages)` — pass the total body image count, poll the panel's item count until it reaches the expected value (or stops growing for several consecutive seconds), then take the first:

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

Then "下一步" → "完成", and finally **verify the cover actually uploaded** (a `background-image` with a non-empty URL appears in the preview); retry once on failure:

```js
async function isCoverUploaded(page) {
  const style = (await preview.getAttribute('style')) || '';
  return style.includes('background-image') && !style.includes('url("")');
}
```

### 4.7 Original: declare type + check agreement (multiple retries)

**Problem**: WeChat's custom components often don't respond on first open, and a failed "我已阅读" check causes the declaration to not take effect.

**Rationale**: `chooseOrigin` uses **multiple retries** — up to 4 rounds, each opening the dialog → clicking "文字原创" → repeatedly checking "我已阅读" until `isChecked()` is true → confirm → verify the dialog closed; on failure, close and retry:

```js
for (let i = 0; i < 6 && !checked; i++) {
  await agreeLabel.evaluate((el) => el.click());
  checked = await agreeInput.isChecked();
}
```

The check triggers a DOM click on the `<label>`, retrying until it's really checked.

### 4.8 Publish: mass-send dialog + second confirmation + QR scan

**Problem**: publishing may trigger a mass send (requiring an admin QR scan) — how do we define the terminal state?

**Rationale**: the `publish` flow — click "群发" at the bottom → dialog 1 (turn off mass-send for a non-mass send) → confirm → dialog 2 (second confirmation "继续发表") → wait for the success toast. Mass sending requires a QR scan, so **a confirmation window is provided**, returning:

```js
return published
  ? { status: 'PUBLISHED', url: '' }
  : { status: 'SUBMITTED', url: '', message: 'submitted for publishing; to mass-send, complete the admin QR scan in the browser' };
```

**Key point**: after publishing, WeChat may enter a scan/review flow, so the **terminal state is `SUBMITTED`, not `PUBLISHED`** (declared via `channel.successStatuses = ['SUBMITTED']`), avoiding false failure by machines.

### 4.9 Channel-specific parameter pass-through

WeChat's `--author` / `--wechat-name` / `--collection` / `--group-send` parameters are **not known** to the core CLI; they're passed through verbatim as `--key value`. The channel assembles them in `augmentPayload`, with priority **CLI args > metadata extra > conf default**:

```js
author: opts.author || params.author || extra.author || extra.username || CONF.author || '',
wechatName: opts.wechatName || extra.wechatName || CONF.wechatName || '',
groupSend: /* opts → params → extra → conf in order */
```

---

## 5. In practice: publish and flow self-test

### 5.1 Save draft (recommended first test)

```bash
node scripts/publisher.js gzh publishDraft --file "/abs/blog.md" --author "长路" --wechat-name "长路Java"
```

The draft scenario also configures cover/summary/original/collection/creation source (`configureOnDraft: true`) before saving.

### 5.2 Actual publish

```bash
node scripts/publisher.js gzh publish --file "/abs/blog.md" --wechat-name "长路Java"
```

Defaults to headed mode with the whole process visible; auto-launches login if not logged in; after publishing, complete the admin QR scan in the browser if mass sending is needed.

### 5.3 Flow self-test: gzh test publish

The gzh self-test uses **draft mode** to avoid auto mass-send/review:

```bash
node scripts/publisher.js gzh test publish --file "/abs/blog.md"
```

Returns `status=DRAFT_SAVED_AND_DELETED` (controlled by `channel.testMode='draft'`).

---

## 6. Pitfalls and optimizations (key retrospective)

### 6.1 Pitfall 1: pasting Markdown becomes plain text

The WeChat editor doesn't understand Markdown; pasting directly yields only plain text with `#`, `*` and backticks exposed.

* **Fix**: use md.doocs to first render Markdown into styled HTML, then paste.

### 6.2 Pitfall 2: content doesn't get in after pasting (content structure check)

After pasting external rich text, WeChat shows a "内容结构检测" dialog; without clicking "继续插入" the content won't be written.

* **Fix**: `confirmInsertDialog` repeatedly clicks "继续插入", and reads back the body length, falling back to the raw write strategy if 0.

### 6.3 Pitfall 3: login "false positive"

`wxuin` / `ua_id` are also issued when not logged in, so using them misjudges "not logged in" as "logged in".

* **Fix**: the credential whitelist contains only `slave_sid` / `slave_user` / `data_ticket`.

### 6.4 Pitfall 4: cover selected wrong/empty

The cover panel renders incrementally in body order, so before images finish uploading, the first is not the body's first image.

* **Fix**: pass the total image count, poll the panel's item count until it reaches the expected value before taking the first, and verify the upload succeeded (retry once on failure).

### 6.5 Pitfall 5: original declaration ineffective

WeChat's custom component doesn't respond on first open, and a failed "我已阅读" check causes the declaration to be ineffective.

* **Fix**: multiple retries + DOM clicks on the `<label>`, repeatedly verifying `isChecked()` until it's really checked.

### 6.6 Pitfall 6: terminal state judgment

After publishing, WeChat may enter a scan/review flow; returning `PUBLISHED` would cause false failure by machines.

* **Fix**: set the terminal state to `SUBMITTED` (`successStatuses: ['SUBMITTED']`), providing a QR-scan confirmation window after publishing.

**Key point**: the gzh pitfalls concentrate into two categories — "**content doesn't get in**" and "**false success state**" — and the countermeasures are "read-back verification" + "retry" + "fallback".

---

## 7. Verification testing

### 7.1 Mock unit tests

Use `test/helpers/mock-page.js` to simulate Playwright, verifying **without a real browser**:

```bash
npm test              # all unit tests (core + all channels)
npm run test:channels # channels only: csdn / gzh
```

Covers every gzh script tool: `checkLogin / enterEditor / setTitle / setAuthor / setContent / preparePublish / publish / saveDraft / deleteBlog / augmentPayload`.

### 7.2 Real-flow self-test

`gzh test publish` performs "publish draft → delete", verifying the full flow without triggering a mass send or leaving residual content.

---

## 8. Summary

The introduction of the gzh channel validated the **extensibility** of the "per-channel layering" skeleton: reusing common capabilities (Playwright + CDP, login detection, CLI contract) and adding channel-specific logic only in `channels/gzh/` (`format.js` styling conversion, `augmentPayload`, `testMode`, `successStatuses`).

Core takeaways:

* **If the editor doesn't understand Markdown, convert to HTML**: md.doocs rendering + clipboard rich text;
* **If content doesn't get in, fall back**: read-back verification + raw write strategy;
* **If the state doesn't fit, use SUBMITTED**: scan/review flows shouldn't be treated as failures;
* **Read back to verify every key action**: reject "false success".

> Project: https://github.com/changluya/BlogLoom | Skill directory: https://github.com/changluya/BlogLoom/tree/master/skills/blogloom-publisher-skill

---

# References

[1]. [md.doocs.org: Markdown typesetting tool for WeChat](https://md.doocs.org/)

[2]. [Playwright docs (Locators, Actions, Page Context)](https://playwright.dev/docs/api/class-locator)

[3]. [Chrome DevTools Protocol (Input.insertText / Network.getCookies)](https://chromedevtools.github.io/devtools-protocol/)

[4]. [Playwright connectOverCDP: reuse an existing browser's login state](https://playwright.dev/docs/api/class-browsertype#browser-type-connect-over-cdp)

---

Author: 长路 | Created: 2026.10.6 | Updated: 2026.10.6

---

## Next steps

- [BlogLoom Publisher Skill 01: One-click publishing skill from 0 to 1](/v1/en/blogs/blogloom-publisher-skill/01-publisher-skill)
- [WeChat (gzh) channel capabilities and publishing flow](/v1/en/blogs/publisher-skill/gzh-channel)
- [CSDN channel design and implementation](/v1/en/blogs/publisher-skill/csdn-design)
