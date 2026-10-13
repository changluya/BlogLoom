---
title: "BlogLoom Publisher Skill 02: CSDN Channel Design and Implementation"
description: Starting from "publish local Markdown to CSDN with one sentence": login detection, title/body writing, per-item publish dialog configuration, deletion and flow self-test.
---

# 1. Background: why a dedicated CSDN publishing skill

## 1.1 Scenario

While building BlogLoom's multi-channel publishing capability, we ran into a very concrete problem: **the same local Markdown post needs to be published not only on the BlogLoom platform but also on CSDN**.

CSDN is one of the most commonly maintained off-site channels for Java / backend developers. Unlike a self-hosted platform, it doesn't offer a stable publishing API; it's a classic "front-end rich-text editor + private publishing protocol". The manual pain points are highly concentrated:

* **Many hidden fields**: the title defaults to a display state, tags/category columns hide inside the publish dialog, and the cover waits on async loading;
* **Frequent redesigns**: CSDN's front end iterates quickly, so selectors often change;
* **Prone to "false success"**: you click publish/delete but nothing actually happens, and it's hard to notice visually.

So we distilled the entire set of "**publish to CSDN**" repetitive clicks into a dedicated channel skill inside `blogloom-publisher-skill`.

## 1.2 Why a separate article instead of folding it into the intro

The intro, "BlogLoom Publisher Skill 01", covers the **cross-channel skeleton** (Playwright + CDP selection, three-layer separation, unified CLI contract). But CSDN is the **first and only channel to have real publishing working end-to-end**, so its DOM structure, dialog interactions and pitfalls deserve their own piece:

| Dimension | Article 01 (common skeleton) | This article (CSDN-specific) |
| --- | --- | --- |
| Focus | The shared foundation for all channels | Implementation details of one channel |
| Content | Selection, layering, contract | Selectors, dialog config, pitfalls and verification |
| Reuse | Copied when adding a channel | Serves only the `csdn` channel |

## 1.3 Goal and scope

The goal in one sentence:

> Publish SOP-compliant local Markdown to CSDN with one sentence, with publish/delete results that are machine-verifiable and never "falsely successful".

The full set of actions involved: `checkLogin` / `login` / `publishDraft` / `publish` / `delete` / `test publish`.

---

# 2. Tech stack: standing on the common skeleton

## 2.1 Playwright primary + CDP low-level enhancement

The CSDN skill reuses the stack established in the intro rather than starting over:

| Layer | Tech | Role on CSDN |
| --- | --- | --- |
| Primary | Playwright | Visible browser, real clicks/typing/pasting, entering the editor, opening the publish dialog, configuring each item |
| Low-level | CDP | Reusing login state (`connectOverCDP`), `Input.insertText` as a rich-text fallback, reading HttpOnly cookies to detect login |
| Page structure | `selectors.js` | Centralized CSDN DOM selectors; when the site changes, only this file changes |

## 2.2 Directory structure

All CSDN channel code is condensed into two files with single responsibilities:

```text
scripts/channels/csdn/
├── index.js       # Business flow: login detection, enter editor, fill fields, publish, delete
└── selectors.js   # Centralized CSDN DOM selectors (only edit here on redesign)
```

One key point in `selectors.js`: **the tag container `tagContainer` is already `.mark_selection`, so child selectors must not repeat the `.mark_selection` prefix**, otherwise `container.locator('.mark_selection …')` will never match descendants. This was learned the hard way.

---

# 3. Capabilities: what the CSDN skill can do

## 3.1 Action matrix

| Action | Command | Description |
| --- | --- | --- |
| Detect login | `csdn checkLogin` | Judge login state, returning whether logged in and whether a slider appears |
| QR login | `csdn login` | Open the browser for QR login, persisting state to a dedicated profile |
| Save draft | `csdn publishDraft` | Fill title/body then save as draft, without the publish dialog |
| Publish | `csdn publish` | Go through the full publish dialog and publish, returning the article link |
| Delete | `csdn delete` | Locate in content management and delete, with confirmation and verification |
| Flow self-test | `csdn test publish` | Publish → delete immediately, verifying the flow and leaving nothing behind |

## 3.2 Login detection (checkLogin)

| Item | Content |
| --- | --- |
| **Core idea** | Use the "home login entry text" as the primary judgment, then cross-verify with "real login cookies" to avoid mistaking anonymous cookies for credentials |
| **Detailed flow** | ① Open the home page `https://www.csdn.net/` → ② read `.toolbar-btn-loginfun` text; if it contains "登录", not logged in → ③ read whether cookies contain both `UserToken` + `UserInfo` → ④ probe for the slider `.verify-move-block` → ⑤ return `{loggedIn, user, hasSlider, loginEntryVisible}` |

> See [4.1](#41-login-detection-text--cookie-cross-verification) for the full rationale and code.

## 3.3 QR login (login)

| Item | Content |
| --- | --- |
| **Core idea** | Open a headed login page for the user to scan, poll login cookies to judge success, and persist state to a dedicated profile |
| **Detailed flow** | ① Open `passport.csdn.net/login` (headed) → ② user scans/types credentials (possibly with a slider) → ③ poll cookies every 2s for `UserToken`/`UserInfo` → ④ success on hit (10 minutes max by default) → ⑤ save state to `~/.blogloom-publisher/chrome-profile-csdn` |

## 3.4 Publish (publish): the full flow

| Item | Content |
| --- | --- |
| **Core idea** | Enter editor → fill title/body → open the publish dialog and configure each item → click final publish → capture the article link |
| **Detailed flow** | ① On the home page click "创作" to enter `editor.csdn.net/md` (fall back to direct open) → ② first click `.article-bar__title-display` to reveal the hidden input and fill the title → ③ clear the body template, write the body with `paste → cdp → keyboard`, estimating a wait by image count → ④ click "发布文章" `button.btn-publish` to open the dialog → ⑤ configure tags/cover/summary/category columns/type/visibility/creation statement in order → ⑥ with `--dry-run`, return `DRY_RUN` and `publishConfig` → ⑦ otherwise click the dialog's bottom publish button, take the link from `a.success-modal-btn[href*=/article/details/]`, returning `PUBLISHED` |

### 3.4.1 Per-item configuration in the publish dialog

| Item | Core idea | Detailed flow |
| --- | --- | --- |
| **Tags** | The tag input only appears after clicking "添加文章标签" | ① Idempotently open the select box → ② clear leftover tags → ③ `fill + Enter` each (≤10, re-resolving the input each time) → ④ close the select box → ⑤ read back `.mark_selection_box_el_tag` to verify, writing `selectedTags/missingTags` |
| **Cover** | The cover list loads asynchronously; wait for uploads before selecting | ① Wait for `.img-selection-item img.select-cover` to be visible → ② click the first one → ③ confirm with `.vicp-operate-btn` → ④ return `coverSet` |
| **Summary** | Just fill the textarea | ① Locate `.desc-box .el-textarea__inner` → ② `fill(summary)` |
| **Category columns** | Checkboxes are hidden; matching uses scoring | ① Locate the "分类专栏" `.form-entry` → ② clear selections → ③ score and pick the best option per column → ④ trigger the hidden checkbox with a DOM `click()` → ⑤ close the dropdown → ⑥ read back `.tag__item-box`, returning `resolved/unresolved/selected` |
| **Article type** | Locate the radio by value | Click `input[value=original]` (original) |
| **Visibility** | Locate the radio by value | Click `input[value=public]` (public) |
| **Creation statement** | Dropdown contains-match the option | ① Click `.creation-statement-select` → ② contains-match "个人观点，仅供参考" among the options → ③ click and return the selected item |

## 3.5 Save draft (publishDraft)

| Item | Content |
| --- | --- |
| **Core idea** | After filling title/body, click "保存草稿" at the top of the editor, without the publish dialog |
| **Detailed flow** | ① Enter the editor and fill title/body → ② click "保存草稿" `button.btn-save` → ③ return `status=DRAFT_SAVED` (tags/summary/category are not set at the draft stage; add them when publishing) |

## 3.6 Delete (delete)

| Item | Content |
| --- | --- |
| **Core idea** | Go to content management, locate the row, confirm the deletion, then verify the row is gone — rejecting "false success" |
| **Detailed flow** | ① Go to `mp.csdn.net/mp_blog/manage` → ② locate the row by id (browse link `/article/details/<id>`) or title → ③ hover the "..." on the right → ④ click "删除" in the dropdown → ⑤ click "确定" in the confirmation dialog → ⑥ verify the row is gone from the list → ⑦ return `DELETED` (otherwise `DELETE_UNVERIFIED`) |

## 3.7 Flow self-test (test publish)

| Item | Content |
| --- | --- |
| **Core idea** | Combine "publish for real + delete immediately" to verify the flow works and leaves nothing behind |
| **Detailed flow** | ① Run `publish` → ② get the article URL/id → ③ immediately run `delete` → ④ return `status=PUBLISHED_AND_DELETED` |

## 3.8 CSDN field mapping

| SOP field | CSDN target |
| --- | --- |
| `title` | Article title |
| `tags` (comma-separated, first 10) | Article tags |
| `columns` (matched against existing columns) | Category columns |
| `articleSummary` | Article summary |
| `![coverImg](url)` | Cover/first image |
| `content` (body after removing the metadata block) | Body (Markdown source) |

---

# 4. Core design rationale

This chapter is the heart of the article, breaking down the CSDN skill's key mechanisms.

## 4.1 Login detection: text + cookie cross-verification

**Problem**: how do we know whether the browser is logged into CSDN?

**Approach**: use the "home login entry text" as the primary judgment, then cross-verify with "real login cookies".

```js
async function checkLogin(page, ctx = {}) {
  // 1. Find .toolbar-btn-loginfun; if the text contains "登录" → not logged in
  const loginFun = page.locator(SELECTORS.homeLoginFun).first();
  const loginText = (await loginFun.innerText() || '').trim();
  let loggedIn = !loginText.includes('登录') && loginText.length > 0;

  // 2. Cross-verify: do the cookies contain UserToken + UserInfo?
  if (!loggedIn) {
    const cookies = await ctx.cookies('https://www.csdn.net');
    loggedIn = cookies.some(c => c.name === 'UserToken') &&
               cookies.some(c => c.name === 'UserInfo');
  }
  return { loggedIn, hasSlider: /* whether .verify-move-block exists */ };
}
```

**Key point**: **anonymous cookies such as `uuid_tt_dd` must not be treated as login credentials** — CSDN issues them in the anonymous state too, causing "false positives" (see the pitfall in 5.1).

## 4.2 Entering the editor and writing the title

**Problem**: CSDN's title input is hidden — how do we fill it?

**Rationale**: the title defaults to the display state `.article-bar__title-display` ("【无标题】"), and **the hidden `input.article-bar__title` only appears after you click it**. So you click the display state, reveal the input, write, then read back to verify:

```js
async function setTitle(page, title) {
  await clickIf(page.locator(SELECTORS.titleDisplay).first());   // click display state to reveal input
  const input = page.locator(SELECTORS.titleInput).first();
  await input.waitFor({ state: 'visible' });
  await input.fill(title);
  // Fallback: if fill didn't take effect, select-all and replace
  if ((await input.inputValue()) !== title) {
    await page.keyboard.press(`${MOD}+A`);
    await page.keyboard.type(title, { delay: 10 });
  }
}
```

## 4.3 Writing the body: clear + multi-strategy paste

**Problem**: CSDN's editor presets a welcome template, and rich-text paste may be intercepted — how do we reliably write the Markdown body?

**Rationale**: four steps —

1. **Clear**: `Cmd/Ctrl + A` to select all, `Backspace` to delete the default welcome content;
2. **Multi-strategy write**: try `paste → cdp → keyboard` in order — clipboard paste is closest to a human (first), CDP `Input.insertText` as a fallback (bypassing paste-event interception), keyboard typing last;
3. **Read-back verification**: after each strategy, read the content back; only non-empty counts as success;
4. **Image wait**: when the body contains images, estimate a wait time by image count, giving CSDN's async image upload/parsing enough time (otherwise the cover list in the publish dialog is empty).

```js
function estimateImageWaitMs(content, { base = 2000, blockSize = 5, perBlock = 3000, max = 60000 } = {}) {
  const count = countImages(content);
  if (count === 0) return 0;
  return Math.min(base + Math.ceil(count / blockSize) * perBlock, max);
}
```

## 4.4 Tag configuration: open the select box first

**Problem**: CSDN's tag input is not visible from the start, so a direct `fill` won't find the element.

**Rationale**: the tag input `.el-autocomplete input.el-input__inner` only appears **after clicking "添加文章标签" `.tag__btn-tag` to open the select box**. So the flow is:

1. Idempotently open the select box (if the input is visible, it's open);
2. Clear leftover tags from the draft / previous run;
3. `fill + Enter` each (re-resolving the input each time to avoid dropped tags from async replacement);
4. Close the select box;
5. **Read back `.mark_selection_box_el_tag` to verify the effective tags**, writing `selectedTags / missingTags`.

## 4.5 Category columns: scoring match + DOM click

**Problem**: category columns have two pitfalls — the checkbox is hidden and can't be clicked, and "contains" matching mis-targets broad subcategories.

**Rationale 1 (scoring match)**: score each existing column option and take the highest —

* Exact equality: 100000 points;
* Existing column contains the configured value: 10000 + name length;
* Configured value contains the existing column: 1000 + name length.

This way `Maven&Gradle` won't be loosely mis-matched to the subcategory `Maven`.

**Rationale 2 (trigger a DOM click)**: the category column checkbox is a **hidden `<input type=checkbox>`**, and a normal `click()` can't reach it (it looks selected but isn't). Check `checked` first to avoid toggling off, then trigger a DOM click with `evaluate((el) => el.click())`:

```js
const chk = option.locator(SELECTORS.categoryOptionCheckbox).first();
const already = await chk.evaluate((el) => !!(el && el.checked));
if (!already) await chk.evaluate((el) => el && el.click());   // key: DOM click
```

Finally, **read back `.tag__item-box`** to verify the actual selections, returning `resolved / unresolved / selected`.

## 4.6 Cover: wait for async loading, then pick the first

**Problem**: the publish dialog's "existing image list" loads asynchronously; searching for `.img-selection-item` right after opening is often empty.

**Rationale**: because 4.3 already estimated a wait by image count, the cover list has content by then. Take the **first** image as the cover, click it, then click the confirm button `.vicp-operate-btn`, returning `coverSet: true/false`. If the list is empty, skip without affecting the body publish.

## 4.7 Delete: confirmation + result verification (rejecting "false success")

**Problem**: how do we ensure the delete actually happened, rather than "looked deleted"?

**Rationale**: go to content management `https://mp.csdn.net/mp_blog/manage` → locate the row by id/title → hover the "..." on the right → click "删除" in the dropdown → **click "确定" in the confirmation dialog** → **verify the row is gone from the list**:

```js
const remaining = await findManageRow(page, target).count();
if (remaining > 0) return { status: 'DELETE_UNVERIFIED' };
return { status: 'DELETED' };
```

**Key point**: the most dangerous error in automation isn't "an error", it's "**looking successful**". So every key action must have a **read-back verification**.

## 4.8 Unified CLI contract and machine-readable output

**Problem**: how do agents/scripts call and parse results reliably?

**Rationale**: all actions share one entry point, stdout outputs a single JSON (logs go to stderr), making parsing easy:

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

`status` conventions: `PUBLISHED` (published) / `DRAFT_SAVED` (draft) / `DRY_RUN` (fill only) / `DELETED` (deleted & verified) / `DELETE_UNVERIFIED` (unconfirmed).

---

# 5. In practice: publish and flow self-test

## 5.1 Dry run: `--dry-run`

Publishing is a **publicly visible** write operation, so a dry run first is recommended to verify the fields:

```bash
node scripts/publisher.js csdn publish --file "/abs/blog.md" --dry-run
```

It completes all filling and dialog configuration but **does not click final publish**, returning `status=DRY_RUN` and `publishConfig` (effective write strategy, tag read-back, matched columns, whether the cover was set, etc.), which shows whether each field was written.

## 5.2 Actual publish

```bash
node scripts/publisher.js csdn publish --file "/abs/blog.md"
```

Defaults to **headed mode**, with the whole process visible; if not logged in, it auto-launches the browser for you to scan, then continues.

## 5.3 Flow self-test: `csdn test publish`

Mocks can test logic but not "page redesigns". A combined self-test is provided — **publish for real → delete immediately**:

```bash
node scripts/publisher.js csdn test publish --file "/abs/blog.md"
```

Returns `status=PUBLISHED_AND_DELETED` to verify the publishing flow works and leaves no residual content.

## 5.4 Selector maintenance

CSDN's site redesigns frequently. All DOM selectors are centralized in `scripts/channels/csdn/selectors.js`, and on redesign you **only edit here**. After changes, verify with `--dry-run`.

---

# 6. Pitfalls and optimizations (key retrospective)

The following are CSDN channel pitfalls hit on real devices — each worth remembering.

## 6.1 Pitfall 1: login "false positive"

Initially login was detected via `UserToken/UserInfo/uuid_tt_dd/cnt_w`, but CSDN **issues `uuid_tt_dd` even in the anonymous state**, so not-logged-in was misjudged as logged in, and the editor jumped straight to the login page.

* **Fix**: accept only the real credentials `UserToken`/`UserInfo`, and in the editor stage detect whether it's redirected to `passport.csdn.net/login`, throwing `AUTH_REQUIRED` if so.

## 6.2 Pitfall 2: hidden tag input

`.el-autocomplete input.el-input__inner` times out on `waitFor(visible)` — it **only appears after clicking "添加文章标签" to open the select box**.

* **Fix**: click `.tag__btn-tag` first to open the select box, then locate the input.

## 6.3 Pitfall 3: category column checkbox ineffective

The category column checkbox is a hidden `<input type=checkbox>`, and a normal `click()` can't reach it.

* **Fix**: check `checked` first to avoid toggling off, then trigger a DOM `click()`.

## 6.4 Pitfall 4: category column mis-match

When reverse-matching the configured value `Maven&Gradle` with "contains", it was loosely matched to the subcategory `Maven`.

* **Fix**: scoring match (exact > existing contains configured > configured contains existing), which correctly hits the second-level item `# Maven&Gradle` in practice.

## 6.5 Pitfall 5: async cover list

Searching for cover items right after opening the publish dialog was often empty.

* **Fix**: when the body contains images, estimate a wait by count, giving enough time for upload/parsing, then publish.

## 6.6 Pitfall 6: delete "false success"

CSDN's confirmation dialog class is `.el_mcm-message-box` (**with the underscore prefix `el_mcm`**), while the selector was written as `.el-message-box`, so "确定" was never clicked — yet `DELETED` was returned.

* **Fix**: correct the confirm button to `.btn-msg-confirm` and **add result verification** — the row must disappear from the list, otherwise return `DELETE_UNVERIFIED`.

**Key point**: the pattern across these pitfalls is highly consistent — **CSDN's elements are either hidden, async, or have underscored class names**. The only two countermeasures are: **trigger the display state before locating**, and **read back to verify after key actions**.

---

# 7. Verification testing: caging the instability

## 7.1 Mock unit tests

Use `test/helpers/mock-page.js` to simulate Playwright (a subset of `locator/click/fill/hover/filter/waitFor/evaluate`), verifying each tool's behavior **without a real browser**:

```bash
npm test              # all unit tests (core + all channels)
npm run test:core     # core only: CLI / markdown / channel loading
npm run test:channels # channels only: csdn / gzh
```

Covers every CSDN script tool: `checkLogin / enterEditor / setTitle / setContent / preparePublish / publish / saveDraft / deleteBlog`.

## 7.2 Real-flow self-test

`csdn test publish` performs "publish for real → delete immediately", verifying the full flow and leaving no residual content.

**Key point**: mock unit tests guard the logic, real-flow self-tests guard against redesigns — both are indispensable.

---

# 8. Summary

The CSDN publishing skill is BlogLoom's **first and most pitfall-laden piece** of multi-channel publishing. The core takeaways can be condensed to:

* **Cross-verify login**: don't treat anonymous cookies as credentials;
* **Trigger hidden elements before locating**: the title and tag input both need a display-state click first;
* **Estimate waits for async loading**: the more images, the longer the wait;
* **Use scoring matches**: avoid mis-targeting broad subcategories;
* **Read back to verify every key action**: reject "false success".

This CSDN approach — "Playwright primary + CDP low-level + one SOP per channel + independent selectors + read-back verification" — can be reused directly for Juejin, the Official Account and other channels.

> Project: https://github.com/changluya/BlogLoom | Skill directory: https://github.com/changluya/BlogLoom/tree/master/skills/blogloom-publisher-skill

---

# References

[1]. [Playwright docs (Locators, Actions)](https://playwright.dev/docs/api/class-locator)

[2]. [Chrome DevTools Protocol (Input.insertText / Network.getCookies)](https://chromedevtools.github.io/devtools-protocol/)

[3]. [Playwright connectOverCDP: reuse an existing browser's login state](https://playwright.dev/docs/api/class-browsertype#browser-type-connect-over-cdp)

---

Author: 长路 | Created: 2026.10.6 | Updated: 2026.10.6

---

## Next steps

- [BlogLoom Publisher Skill 01: One-click publishing skill from 0 to 1](/v1/en/blogs/blogloom-publisher-skill/01-publisher-skill)
- [CSDN channel capabilities and publishing flow](/v1/en/blogs/publisher-skill/csdn-channel)
- [WeChat (gzh) channel design and implementation](/v1/en/blogs/publisher-skill/gzh-design)
