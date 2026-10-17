---
title: "BlogLoom Skill Design and Implementation"
description: "Distilling local Markdown import, update and query into an AI-agent-callable skill package: three-layer separation, unified CLI contract, tool inventory and usage."
---

# BlogLoom Skill Design and Implementation

> Audience: developers involved in BlogLoom's engineering design who need to understand or extend its AI Skill capabilities.
> Implementation directory: `blog-backend/src/main/resources/skill/blogloom-skill/`, download entry `GET /admin/skill/download`.

---

## 1. Background

### 1.1 The problem to solve

BlogLoom is a "local Markdown blog → platform" content loop, but users face lots of repetitive work during migration/maintenance:

- A large amount of Markdown has accumulated locally, needing **batch import** and automatic filing into knowledge-base directories;
- Each article needs its title, tags, category, summary, columns, cover and create/update time filled in;
- Online articles need to **stay in sync with local**, and operations like changing the title/visibility/pinning/columns are scattered and tedious.

The traditional approach is "the user manually pastes each article in the backend and fills each attribute", which is inefficient and error-prone.

### 1.2 Why a Skill

Distill "import/edit/query" into a **skill package directly callable by an AI agent**: the user just needs one natural-language sentence (e.g. "sync this directory of blogs up there"), and the agent reads the skill, selects the SOP and runs the tools to complete the operation.

### 1.3 Design goals

| Goal | Meaning |
| --- | --- |
| Works out of the box | The downloaded ZIP already contains the current user's `BASE_URL` and dedicated token — no manual configuration |
| Consistent with the UI | Directly reuses the platform's existing import/edit APIs, not a parallel set of logic |
| Stably executable by agents | Unified CLI contract, unified JSON output, judgeable errors |
| Extensible | Adding a product capability only requires adding tools + SOP + routing in a fixed order |

---

## 2. Core design

### 2.1 Three-layer separation

The skill is strictly split into three layers with single, non-polluting responsibilities:

```
user natural language
   │
   ▼
SKILL.md          Scenario routing layer: judge intent, point to a specific SOP
   │
   ▼
references/*.md   SOP layer: describe steps, tool names, parameter sources, failure branches
   │
   ▼
scripts/*.py      Tool layer: actually execute (auth + REST calls + orchestration + output)
```

| Layer | File | Responsibility | Does not do |
| --- | --- | --- | --- |
| Routing | `SKILL.md` | Recognize scenarios, dispatch SOPs | Write long flows |
| SOP | `references/0x-*.md` | Business steps and tool orchestration descriptions | Write code |
| Tool | `scripts/*.py` | Execute; atomic layer only maps APIs, orchestration layer composes | Make business judgments |

### 2.2 Two-layer tool model

The tool layer is further split to reflect a progressive "REST atomic tools → business orchestration tools" encapsulation:

| Layer | File | Positioning | Examples |
| --- | --- | --- | --- |
| REST atomic | `scripts/rest_tools.py` | One function = one REST API, doing only auth + request + verbatim return | `get_blog(blog_id)`, `list_columns()` |
| Business orchestration | `scripts/blogloom-skill.py` | One tool = one business action, possibly composing multiple atomic APIs | `import_blog`, `sync_local_blog`, `update_column_relation` |

**Rule of thumb**: calls only 1 API, no business judgment → atomic layer; ≥2 APIs or parameter derivation/result aggregation/retries → orchestration layer.

### 2.3 Unified CLI contract

All tools (including the read-only tools exposed by the atomic layer) share the same invocation:

```bash
python3 scripts/blogloom-skill.py --tool <tool-name> --params '<JSON object>' [--pretty]
```

- `--tool`: select the specific tool;
- `--params`: carries all business parameters (a valid JSON object);
- `--pretty`: only controls output indentation;
- Unified output: `{"ok": true|false, "tool": "...", "data": {...}, "error": null|"..."}`.

### 2.4 Parameters and authentication

| Variable | Source | Description |
| --- | --- | --- |
| `BLOOM_BASE_URL` | Env var / `--params.base_url` | Service address, temporarily overridable via payload (useful for pointing at a test environment) |
| `BLOOM_API_TOKEN` | Env var (**only this one place**) | Authentication; must not be written into `--params` or code |

- On missing parameters, **list all missing items at once** to reduce wasted agent rounds;
- Parameter paths are read level by level with `.get()`, taking the missing-parameter branch rather than `KeyError` when a field is absent.

### 2.5 Core decision: reuse platform APIs, don't reinvent the wheel

| Scenario | Reused UI capability | What the skill gains |
| --- | --- | --- |
| Add/import | The "import Markdown into the knowledge base" API | `knowledgeBasePath` filing, creating missing directories level by level, auto-creating tags/categories, matching only existing columns, `SKIP`/`RENAME` conflict strategies |
| Edit | The "edit blog" API | Update by id; unset fields keep their original values |

So the skill **does not implement** directory creation or node moves itself — its behavior is fully consistent with the UI, with minimal maintenance cost.

### 2.6 Cover image recognition rule (key)

The cover is **not the first body image**; it requires an **explicit marker**: only images with `alt` of `coverImg` count as the cover.

```markdown
![coverImg](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/cover.png)
```

| Case | Result |
| --- | --- |
| `![coverImg](url)` | That `url` is the cover |
| `<img alt="coverImg" src="url">` | That `url` is the cover |
| Both forms present | The one appearing earlier in the body |
| Only normal images / no images | No cover |

On the backend this is implemented uniformly by `KnowledgeCoverExtractor.extract(content)`, called by `KnowledgeImportWorker`; the skill side's `pick_first_picture` follows the same rule.

---

## 3. Skill standard and extension conventions

### 3.1 Directory structure (mandatory)

```text
blogloom-skill/
├── SKILL.md                      # Scenario routing layer
├── references/
│   ├── 00-tool-contract.md       # Atomic tool contract (names/params/returns/error codes)
│   ├── 01-import-blog-sop.md     # Add/import SOP
│   ├── 02-update-blog-sop.md     # Update SOP
│   ├── 03-query-blog-sop.md      # Query SOP
│   └── 99-rest-fallback-sop.md   # Fallback SOP for APIs not covered by the contract
├── scripts/
│   ├── blogloom-skill.py         # Business CLI (orchestration layer) + entry
│   ├── rest_tools.py             # REST atomic tools (one-to-one mapping layer)
│   └── default-cover.png         # Built-in default cover
├── test/
│   └── test_blogloom_skill.py    # Unit tests
├── skill.env.yaml                # Env var declaration (platform metadata)
└── skill.env.sh                  # Env var script (token injected on download)
```

### 3.2 Standard extension order

When adding a product capability, proceed in this order:

1. Register one-to-one **read-only REST tools** in `rest_tools.py`;
2. Encapsulate **business tools and multi-API orchestration** in `blogloom-skill.py`'s `build_tool_registry`;
3. Add/update the **SOP** and hook it into `SKILL.md`'s scenario routing table;
4. Add **tests** for the tool inventory, parameter mapping, orchestration and error boundaries;
5. Run unit tests and the skill **structure check**.

### 3.3 Packaging and injection conventions

| Step | Implementation | Description |
| --- | --- | --- |
| Download entry | `GET /admin/skill/download` (`SkillAdminController`) | Specific to the logged-in user |
| Packaging | `SkillPackageService.buildZip(username)` | Scans `skill/blogloom-skill/**` on the classpath and zips it |
| Token injection | Replace `__BLOOM_API_TOKEN__` / `__BLOOM_BASE_URL__` in `skill.env.sh` | Issues a JWT for the current user, working out of the box |
| Filtering | Skip `__MACOSX` / `__pycache__` / `.pyc` | Avoids dirty cache files in the package |

---

## 4. Skill capabilities

### 4.1 Tool inventory

**REST atomic tools** (`rest_tools.py`)

| Tool | Method / path | Description |
| --- | --- | --- |
| `list_categories` | GET `/admin/categories` | Category list |
| `list_tags` | GET `/admin/tags` | Tag list |
| `list_columns` | GET `/admin/columns/options` | Column tree (incl. second level) |
| `get_blog` | GET `/admin/blog` | Blog details |
| `list_blogs` | GET `/admin/blogs` | Article list (fuzzy title match) |
| `update_blog` | PUT `/admin/blog` | Edit article (must include id) |
| `update_visibility` | PUT `/admin/blog/{id}/visibility` | Change visibility |
| `update_top` | PUT `/admin/blog/top` | Pin |
| `update_recommend` | PUT `/admin/blog/recommend` | Recommend |
| `preview_import_files` | POST `/admin/knowledge/import/preview/files` | Import pre-check |
| `execute_import` | POST `/admin/knowledge/import/{token}/execute` | Submit the import task |
| `get_import_progress` | GET `/admin/knowledge/import/tasks/{taskId}` | Poll import progress |
| `upload_resource` | POST `/admin/blog/resources` | Upload image resources |

**Business orchestration tools** (`blogloom-skill.py`)

| Tool | Purpose | Key parameters |
| --- | --- | --- |
| `import_blog` | Add/import local Markdown (single/multiple/directory) | `file_path` / `file_paths` / `dir_path`, `conflict_policy`, `published`, `target_parent_id` |
| `sync_local_blog` | Smart single-article sync (create if absent, update if present) | `file_path`, `mode` (auto/create/update), `blog_id` |
| `import_markdown_dir` | Directory alias of `import_blog` (legacy compatibility) | `dir_path`, `recursive` |
| `update_blog` | Edit an existing article (unset fields keep originals) | `blog_id` + fields to change |
| `update_column_relation` | Column association maintenance | `blog_id`, `columns`, `mode` (replace/add/remove) |
| `publish_visibility` | Switches for visibility/reward/comments | `blog_id`, `published`, `password` |
| `update_top` / `update_recommend` | Pin / recommend | `blog_id`, `top` / `recommend` |
| `upload_resource` | Upload image resources | `file_path` |
| `get_blog` / `list_blogs` / `list_categories` / `list_tags` / `list_columns` | Query-type | See the table above |

### 4.2 Capability coverage matrix

| Capability | Status | Description |
| --- | --- | --- |
| Single/multiple/directory import | ✅ | Supports `recursive` |
| knowledgeBasePath filing | ✅ | Reuses the platform import API, creating missing directories level by level |
| Auto-create tags/categories | ✅ | Creates and associates when absent |
| Columns matched, not created | ✅ | Includes second-level columns; unmatched ones returned in `unresolved` |
| Cover recognition | ✅ | Only recognizes images with `alt=coverImg` |
| Create/update time writing | ✅ | `YYYY-MM-DD HH:mm:ss` |
| Same-name conflict strategy | ✅ | `SKIP` (default) / `RENAME` |
| Smart sync (idempotent) | ✅ | Decides create/update by title; repeated runs don't duplicate articles |
| Edit body and parameters | ✅ | Unset fields keep originals; no accidental clearing |
| Query and search | ✅ | Article list, categories, tags, columns |
| Auth-failure hint | ✅ | Suggests re-downloading the skill in the CMS to refresh the token |

### 4.3 Error conventions

| Situation | Return |
| --- | --- |
| Missing parameters | `ok=false`, listing all missing fields at once |
| Invalid `--params` JSON | `ok=false`, exit code 2 |
| Unknown tool | `ok=false`, with an `available` list |
| Auth failure (401/403) | `ok=false`, suggesting re-downloading the skill |
| Business error (server code≠200) | `ok=false`, passing through the server's `msg` |
| Import failure | `ok=false`, `progress.status=FAILED`, `message` is the reason |

---

## 5. How to use the skill

### 5.1 Obtaining and preparing

1. In the backend, go to "文章管理" and click **"Skill 下载"** at the top right (or request `GET /admin/skill/download` directly) to get `blogloom-skill.zip`;
2. Unzip, enter the directory, install dependencies and load the environment variables:

```bash
cd blogloom-skill
pip install requests
source ./skill.env.sh      # loads BLOOM_BASE_URL / BLOOM_API_TOKEN (already injected on download)
```

3. Hand the whole `blogloom-skill/` directory to an AI agent that supports skills (the agent reads `SKILL.md` automatically and reads the `references/` SOPs as needed).

> When the token expires, just re-download the skill to refresh it.

### 5.2 Manual command-line verification

```bash
# Query: search articles by title
python3 scripts/blogloom-skill.py --tool list_blogs --params '{"title":"Maven","page_size":20}' --pretty

# Import: single article
python3 scripts/blogloom-skill.py --tool sync_local_blog --params '{"file_path":"/abs/path/blog.md"}' --pretty

# Import: whole directory
python3 scripts/blogloom-skill.py --tool import_blog --params '{"dir_path":"/abs/dir","recursive":true,"conflict_policy":"SKIP"}' --pretty

# Edit: change visibility
python3 scripts/blogloom-skill.py --tool publish_visibility --params '{"blog_id":11,"published":false}' --pretty
```

### 5.3 Markdown article format conventions

Each article is an ordinary `.md` file in two parts: **a ` ```json ` metadata code block at the very top** (declaring article attributes), **followed by normal Markdown body**. The cover is not a metadata field — it's explicitly marked in the body with `![coverImg](image-URL)`.

````markdown
```json
{
  "title": "SqlParser Parser Quick Start",
  "tags": "AI,SEO,content marketing",
  "category": "SqlParser",
  "articleSummary": "This article explores how to optimize blog content to improve search rankings.",
  "columns": "SqlParser, AI",
  "createTime": "2026-09-19 14:30:00",
  "updateTime": "2026-09-19 14:30:00",
  "knowledgeBasePath": "/0x05、Java后端/SqlParser"
}
```

# SqlParser Parser Quick Start

![coverImg](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/cover.png)

Body content here……

## 1. Background

The body supports **bold**, `inline code`, lists, code blocks and other standard Markdown syntax, preserved as-is.
````

Format rules:

| Part | Requirement |
| --- | --- |
| Metadata code block | Must be at the **very top** of the file, fenced with ` ```json ` (`json` is case-insensitive); removed from the body after import |
| `title` / `tags` / `category` / `articleSummary` / `columns` / `createTime` / `updateTime` / `knowledgeBasePath` | All optional fields, each with its own fallback |
| Cover | `![coverImg](url)` in the body, **only the `coverImg` alt is recognized**; normal images aren't covers |
| Body | Everything after the code block, with standard Markdown syntax preserved as-is |
| No metadata block | Treated as plain Markdown, with the title falling back to the filename; an invalid-JSON code block is treated as no metadata |

### 5.4 Quick prompt examples

Just say one natural-language sentence to the agent, and it hits the right SOP automatically:

| Scenario | Quick prompt |
| --- | --- |
| Single import | "Import `/Users/me/blogs/maven.md` into the blog" |
| Batch directory import | "Sync all blogs in this directory to the platform: `/Users/me/blogs/Java`" |
| Filed import | "Import this article into the knowledge base's `SqlParser` directory" |
| Resume/avoid duplicates | "Skip same-named ones when importing this directory" |
| Sync update | "I've changed `maven.md`; sync it online" |
| Search articles | "Find articles on the platform whose title contains `Maven`" |
| View details | "Show the details of article id 11" |
| Change visibility | "Set article 11 to private" |
| Pin/recommend | "Pin article 11" / "Recommend article 11" |
| Change columns | "Change article 11's columns to `Maven&Gradle`" |
| Preparation | "List all columns and categories so I can confirm before importing" |
| Combined troubleshooting | "Check article 11's tags and columns, then add anything missing" |

---

## 6. Related implementation index

| Component | Path |
| --- | --- |
| Download API | `blog-backend/.../controller/admin/SkillAdminController.java` |
| Packaging/injection service | `blog-backend/.../service/SkillPackageService.java` |
| Skill resource root | `blog-backend/src/main/resources/skill/blogloom-skill/` |
| Cover extractor | `blog-backend/.../module/knowledge/support/KnowledgeCoverExtractor.java` |
| Import executor | `blog-backend/.../module/knowledge/service/impl/KnowledgeImportWorker.java` |
| Front-end download entry | `blog-cms-ui/src/views/blog/blog/BlogList.vue`, `src/api/blog.js` |
| Import rule PRD | `docs/prd/核心功能业务逻辑/后台快速迁移导入逻辑/知识库-博客迁移导入标准逻辑.md` |

---

## Next steps

- [AI Skill sync](/v1/en/integration/ai-skill)
- [BlogLoom Publisher Skill 01: One-click publishing skill from 0 to 1](/v1/en/blogs/blogloom-publisher-skill/01-publisher-skill)
- [Content management](/v1/en/guide/admin/content)
