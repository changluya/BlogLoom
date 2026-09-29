---
title: "BlogLoom Publisher Skill 01: One-Click Blog Publishing from 0 to 1 (Playwright + CDP)"
description: From manual copy-paste to automated publishing — tech selection, core design principles, the CSDN publishing pipeline, and real-world pitfalls and fixes.
---

# 1. Background: Why Do We Need a "Publishing Skill"?

## 1.1 The Scenario

While maintaining the open-source blogging system BlogLoom, I settled into a stable content workflow: write in locally stored Markdown, keep a standard metadata block at the top, then import it into the platform.

But a very concrete problem soon appeared: **the same article often needs to be distributed to multiple external channels such as CSDN, Juejin, and WeChat Official Accounts**.

At first I moved things by hand: open the editor, paste the body, fill the title, add tags, pick a column, upload a cover — ten minutes per article, and batch publishing was both mechanical and error-prone.

That is how the **BlogLoom Publisher Skill** was born: turning all this "repeated clicking" into a skill package that an AI Agent can call directly.

## 1.2 Four Pain Points of Manual Distribution

* **Time-consuming**: pasting, waiting for image uploads, and configuring tags takes minutes per long article.
* **Error-prone**: tags and columns are easy to pick wrongly, and covers are easily forgotten.
* **Inconsistent**: each channel uses different field conventions, and mapping them by hand is costly.
* **Hard to reuse**: every article means "clicking through everything again" — nothing accumulates into a process.

**Key point**: what we want to solve is not *how to write an article*, but *how to distribute it with a single sentence once it is written*.

## 1.3 Goal and Scope

The goal in one sentence:

> Distribute SOP-compliant local Markdown blogs to multiple third-party channels using natural language; the first integrated channel is CSDN.

## 1.4 Repository and Column Notes

> This is the opening article of the **BlogLoom Publisher Skill column** (`BlogLoom分发Skill` No. 01). Future articles will keep documenting the design and practice of multi-channel publishing.

* **BlogLoom repository**: https://github.com/changluya/BlogLoom
* **This Skill directory**: https://github.com/changluya/BlogLoom/tree/master/skills/blogloom-publisher-skill

The Skill lives under the `skills/blogloom-publisher-skill` directory of the BlogLoom repository, maintained alongside the main site (`blog-backend` / `blog-cms-ui` / `blog-view-ui`). Clone and use it directly:

```bash
git clone https://github.com/changluya/BlogLoom.git
cd BlogLoom/skills/blogloom-publisher-skill
npm install
node scripts/publisher.js csdn checkLogin
```

---

# 2. Tech Selection: Why Playwright + CDP

For "automated publishing to third-party platforms", there are three common paths. Let's compare them one by one.

## 2.1 Option 1: Call the Platform HTTP API Directly

Capture the network traffic, find the publish API, and construct requests directly.

* **Pros**: fast, low resource usage, no browser required.
* **Cons**: the API carries signed/encrypted parameters (e.g., CSDN's `x-ca-*` family) and changes frequently; any redesign means reverse-engineering again.
* **Best for**: internal systems with controllable, stable APIs.

**Drawback**: a third-party publish API is essentially a *private protocol*. The maintenance cost is high and stability is poor — not suitable as a long-term solution.

## 2.2 Option 2: Selenium + WebDriver

The traditional browser-automation approach, with a mature ecosystem and plenty of resources.

* **Pros**: stable API, large community, reusable `chromedriver` experience.
* **Cons**: tightly coupled to the browser version (driver version), lots of manual wrapping for waits and stability, unfriendly to modern async rendering, and mediocre debugging.
* **Best for**: legacy projects already deeply invested in Selenium.

## 2.3 Option 3: Playwright + CDP (Chosen)

Use Playwright as the **primary driver** and CDP (Chrome DevTools Protocol) as a **low-level enhancement**.

* **Playwright as primary**: auto-waiting for actionable elements, cross-browser, clear `locator` semantics, one-switch headed/headless — ideal for "simulating a real user".
* **CDP as enhancement**: reuse an existing Chrome login state (`connectOverCDP`), read HttpOnly cookies to detect login, and use `Input.insertText` as a fallback for rich-text input.

**Key point**: this is a combination of **high-level ergonomics + low-level control**. Playwright handles 90% of routine operations; CDP handles the 10% Playwright cannot reach.

## 2.4 Comparison

| Dimension | Pure HTTP API | Selenium + WebDriver | Playwright + CDP |
|---|---|---|---|
| Resilience to redesign | Weak (needs RE) | Medium | Strong (simulates clicks) |
| Login reuse | Maintain token yourself | Fair | Strong (persistent context / CDP) |
| Async rendering | N/A | Manual waits | Built-in auto-wait |
| Headed debugging | No | Yes | Yes (headed by default) |
| Agent friendliness | Good | Fair | Good (unified CLI) |
| Verdict | Not used | Fallback | **Chosen** |

---

# 3. Core Design Principles

## 3.1 Three-Layer Separation + Per-Channel Folders

The Skill is strictly layered, each with a single responsibility:

```text
Natural language / CLI
   │
   ▼
publisher.js        Router: parse <channel> <action>, dispatch
   │
   ▼
channels/<channel>/  Channel layer: one folder per channel (index.js flow + selectors.js)
   │
   ▼
lib/                Common layer: session / editor / markdown / channel loading
```

**Key point**: **selectors always live alone in `selectors.js`**. When a site redesigns, only the selectors file changes; the business flow stays untouched.

```text
scripts/channels/csdn/
├── index.js       # Business flow: login, editor, fields, publish, delete
└── selectors.js   # Centralized DOM selectors (only change here on redesign)
```

## 3.2 One Channel, One SOP

Borrowing BlogLoom's "scenario routing" idea: documents should not be duplicated — **one SOP per channel**.

```text
references/
├── 00-tool-contract.md          # Shared contract: CLI / params / returns / errors
└── channels/csdn/sop.md         # CSDN channel SOP (with a selectors table)
```

Adding a channel = add `scripts/channels/<name>/` + `references/channels/<name>/sop.md` + one routing table row.

## 3.3 Align with the Unified Blog SOP First

Before publishing, validate the top metadata against `标准生成发布输出博客sop.md`:

```JSON
{
   "title": "…",
   "tags": "Maven, spotless, licenseHeader",
   "category": "Maven",
   "articleSummary": "…",
   "columns": "项目管理工具, Maven&Gradle",
   "createTime": "2026-09-21 18:30:00",
   "updateTime": "2026-09-21 18:30:00",
   "knowledgeBasePath": "/0x05、Java后端/…/maven插件"
}
```

The parser does two things:

* **Field normalization**: accepts `category`/`categories`, `columns`/`column`, etc.
* **Lenient parsing**: tolerates common mistakes in SOP samples (full-width colons, trailing commas, full-width quotes) so a typo doesn't lose the whole metadata block.

**Note**: covers are always marked explicitly in the body via `![coverImg](url)`, consistent with BlogLoom's import rules.

## 3.4 Simulate a Real User + Persist Login

* All primary operations go through Playwright: real clicks, typing, and pasting, **headed by default**, so the publishing process is visible.
* Login uses a **dedicated persistent profile** (`~/.blogloom-publisher/chrome-profile-<channel>`): scan the QR code once and stay logged in.
* `--cdp` can connect to your everyday Chrome to reuse its existing login state.

## 3.5 Unified CLI Contract and Machine-Readable Output

```bash
node scripts/publisher.js <channel> <action> [options]
```

Output goes to stdout (logs to stderr), easy for an Agent to parse:

```json
{"ok":true,"channel":"csdn","action":"publish","data":{"status":"PUBLISHED","url":"…"},"error":null}
```

---

# 4. Capabilities at a Glance

## 4.1 Action Matrix

| Action | Description |
| --- | --- |
| `csdn checkLogin` | Detect login state |
| `csdn login` | Open the browser for QR login and persist it |
| `csdn publishDraft` | Save as a draft |
| `csdn publish` | Publish the blog |
| `csdn delete` | Delete a blog (locate it in the content manager, with confirmation and verification) |
| `csdn test publish` | Composite self-test: publish → delete immediately |

## 4.2 Configurable Headed / Headless

```bash
# Headed by default, easy to observe
node scripts/publisher.js csdn publish --file "/abs/blog.md"

# Headless (CI / unattended)
node scripts/publisher.js csdn publish --file "/abs/blog.md" --mode headless
```

You can also control it globally with `PUBLISHER_MODE=headed|headless`.

## 4.3 Proactive Login (Local Workbench)

This Skill targets the **local workbench**: when an action needs login and detects it is not logged in, it **proactively launches the browser** for the user to scan, then continues automatically.

> Headless mode cannot scan, so it relaunches headed; for CI, use `--no-login` to fail fast.

---

# 5. Hands-On: The CSDN Publishing Pipeline

## 5.1 Login Detection

Detect via the login entry text on the homepage, cross-checked with cookies:

* If `.toolbar-btn-loginfun` exists with text "登录" → not logged in.
* If the "创作" entry is present and not in a login state, or cookies contain `UserToken` + `UserInfo` → logged in.

## 5.2 Entering the Editor and Writing Title/Body

1. Click "创作" (`.toolbar-btn-write-new`) on the homepage to enter `editor.csdn.net/md`.
2. **The title is in display state by default** (`.article-bar__title-display`, showing "【无标题】"); clicking it reveals the hidden `input.article-bar__title`.
3. The body is a `<pre class="editor__inner markdown-highlighting" contenteditable>`. Clear the default welcome content first, then paste using the `paste → cdp → keyboard` strategy:

```js
async function setContent(page, markdown) {
  const editor = page.locator('.editor__inner.markdown-highlighting').first();
  await editor.click();
  await page.keyboard.press(`${MOD}+A`);
  await page.keyboard.press('Backspace');           // clear welcome content
  const r = await writeContent(page, '.editor__inner.markdown-highlighting', markdown,
                               ['paste', 'cdp', 'keyboard']);
  return r;                                          // { strategy, length }
}
```

## 5.3 Configuring the Publish Dialog

Click "发布文章" (`button.btn-publish`) to open `.modal__publish-article`, then configure:

* Tags `.mark_selection`
* **Cover**: the first item in the existing image list (`.img-selection-item img.select-cover`)
* Summary `.desc-box .el-textarea__inner`
* Category columns (including second-level `# xxx`)
* Article type → Original; Visibility → Public
* **Creation statement** → "个人观点，仅供参考" (personal opinion, for reference only)

## 5.4 Deleting with Confirmation

`delete` goes to the content manager `https://mp.csdn.net/mp_blog/manage`, locates the article row → hovers "..." on the right → clicks "删除" in the dropdown → clicks "确定" in the dialog, then **verifies the row is gone** before declaring success.

---

# 6. Pitfalls and Fixes

This section is the most honest part: **every item is a pit hit on the real site**.

## 6.1 Pit 1: False-Positive Login

Initially, login was detected by whether cookies contained `UserToken/UserInfo/uuid_tt_dd/cnt_w`. It turned out **CSDN also sets `uuid_tt_dd` in an anonymous state**, so "not logged in" was misjudged as "logged in", and the editor jumped straight to the login page.

* **Symptom**: `checkLogin` says logged in, but `publish` opens the editor at the login page.
* **Fix**: only trust the real credentials `UserToken`/`UserInfo`, and during the editor stage check whether the page was redirected to `passport.csdn.net/login`; if so, throw `AUTH_REQUIRED`.

## 6.2 Pit 2: The Title Input Is Hidden

`waitFor(visible)` on `.article-bar__title` times out forever — because it is **`display:none`**; what is actually shown is `.article-bar__title-display`.

* **Fix**: click the display state first to reveal the hidden input, then fill it and read it back:

```js
await page.locator('.article-bar__title-display').first().click();
const input = page.locator('input.article-bar__title').first();
await input.waitFor({ state: 'visible' });
await input.fill(title);
```

## 6.3 Pit 3: Default Body Content and Rich-Text Input

The CSDN editor pre-loads a "Welcome to the Markdown editor" template. If it is not cleared, irrelevant content ends up in the published article.

* **Fix**: clear with `Cmd/Ctrl + A` + `Backspace` before writing; use **clipboard paste first** (closest to a real user), then fall back to CDP `Input.insertText`, and finally per-character keyboard input; read back the content length to verify.

## 6.4 Pit 4: Cover List Loads Asynchronously

The "existing image list" in the publish dialog **loads asynchronously**. Looking for `.img-selection-item` right after opening the dialog often finds nothing, so the cover is missed.

* **Fix**: when the body contains images, **estimate the post-paste wait by image count** — 2s by default, plus 3s for every full 5 images — giving image upload/parsing enough time before opening the publish dialog.

```js
function estimateImageWaitMs(content, { base = 2000, blockSize = 5, perBlock = 3000, max = 60000 } = {}) {
  const count = countImages(content);
  if (count === 0) return 0;
  return Math.min(base + Math.ceil(count / blockSize) * perBlock, max);
}
```

## 6.5 Pit 5: Hidden Checkbox and Wrong Category Match

Two chained pitfalls:

1. **Ineffective checking**: the category checkbox is actually a **hidden `<input type="checkbox" class="tag__option-chk">`**. An ordinary `click()` cannot reach it — it *looks* selected but is not.
2. **Wrong match**: with loose reverse "contains" matching, the setting `Maven&Gradle` was matched to the broader child `Maven`.

* **Fix 1**: trigger a DOM `click()` on the checkbox, first checking `checked` to avoid unchecking, then read back the selected items:

```js
const chk = option.locator('.tag__option-chk').first();
const already = await chk.evaluate((el) => !!(el && el.checked));
if (!already) await chk.evaluate((el) => el && el.click());   // key: DOM click
```

* **Fix 2**: switch to **scored matching** — exact `> ` existing-contains-setting `> ` setting-contains-existing — taking the highest score; verified to correctly hit the second-level item `# Maven&Gradle`.

## 6.6 Pit 6: "Fake Success" on Delete

The first `delete` run returned `DELETED`, but the article was still there.

* **Root cause**: CSDN's confirmation dialog class is `.el_mcm-message-box` (**with the underscore prefix `el_mcm`**), while our selector was `.el-message-box`, so the "确定" button was never clicked.
* **Fix**:
  1. Correct the confirm button selector to `.btn-msg-confirm`.
  2. **Add result verification** — after confirming, re-check the content list; the row must be gone, otherwise return `DELETE_UNVERIFIED`. Never report "fake success".

**Key point**: the most dangerous automation bug is not an *error*, but *looking like success*. So every critical action must be followed by a **read-back verification**.

---

# 7. Testing: Putting Instability in a Cage

Browser automation inherently depends on external pages. How do we ensure quality? We use two layers: **mock unit tests + real pipeline self-test**.

## 7.1 Mock Unit Tests

We implemented a lightweight Playwright simulation layer (`test/helpers/mock-page.js`) supporting a subset of `locator/click/fill/hover/filter/waitFor/evaluate`, so every tool can be behaviorally verified **without a real browser**:

```bash
npm test              # all unit tests (core + all channels); currently 45 green
npm run test:core     # core only: CLI / markdown / channel loading
npm run test:channels # channels only: currently csdn
```

Coverage includes every script tool: `checkLogin / enterEditor / setTitle / setContent / preparePublish / publish / saveDraft / deleteBlog`.

## 7.2 Real Pipeline Self-Test: test publish

Mocks can test logic but cannot catch a site redesign. So we provide a composite self-test:

```bash
node scripts/publisher.js csdn test publish --file "/abs/blog.md"
```

It performs "real publish → immediate delete" and returns:

```json
{"status":"PUBLISHED_AND_DELETED",
 "publish":{"url":"https://blog.csdn.net/…/details/166846562",
            "publishConfig":{"selectedCategories":["项目管理工具","# Maven&Gradle"],
                             "coverSet":true,"creationStatement":"个人观点，仅供参考"}}}
```

**Key point**: `--dry-run` fills everything without clicking publish, for quick field verification; `test publish` validates the full pipeline **without leaving residue**.

---

# 8. Summary and Outlook

Looking back at this from-scratch practice, it boils down to a few principles:

* **Scenario-driven**: first clarify *who, in what scenario, repeatedly does what*, then start.
* **Tech selection must match the problem**: for third-party publishing, choose the "simulate a real user" Playwright + CDP, not a head-on fight with private APIs.
* **Structure determines maintainability**: per-channel folders, isolated selectors, one SOP per channel.
* **Verify every critical action**: beware of "fake success".
* **Two test layers**: mock unit tests guard the logic, real pipeline self-tests guard against redesigns.

Next steps:

1. Add more channels (Juejin, WeChat Official Accounts, etc.) to validate the per-channel extensibility.
2. Support automatic cover upload (remote `coverImg` → download → upload).
3. Provide richer natural-language routing examples for the Agent, making "one-sentence publishing" smoother.

> If you are also working on multi-channel content distribution, I hope this "Playwright primary + CDP enhancement" approach offers some reference.
>
> Repository: https://github.com/changluya/BlogLoom ｜ Skill directory: https://github.com/changluya/BlogLoom/tree/master/skills/blogloom-publisher-skill

---

Compiled by ChangLu · 2026.9.29
