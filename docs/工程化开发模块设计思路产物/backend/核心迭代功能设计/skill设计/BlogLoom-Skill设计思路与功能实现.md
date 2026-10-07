# BlogLoom Skill 设计思路与功能实现

> 面向对象：参与 BlogLoom 工程化设计、需要了解或扩展 AI Skill 能力的开发者。
> 对应实现目录：`blog-backend/src/main/resources/skill/blogloom-skill/`，下载入口 `GET /admin/skill/download`。

---

## 一、背景

### 1.1 要解决的问题

BlogLoom 是「本地 Markdown 博客 → 平台」的内容闭环，但用户在迁移/维护时有大量重复动作：

- 本地已积累大量 Markdown，需要**批量导入**并自动归入知识库目录；
- 每篇文章要补全标题、标签、分类、摘要、专栏、封面、创建/更新时间等属性；
- 线上文章需要**保持与本地同步**，改标题/改可见性/置顶/换专栏等操作零散且繁琐。

传统做法是「用户手动在后台逐篇粘贴 + 逐个填属性」，效率低且易错。

### 1.2 为什么用 Skill

把「导入/编辑/查询」沉淀为**可被 AI Agent 直接调用的技能包**：用户只需一句自然语言（如"把这个目录的博客同步上去"），Agent 自动阅读 Skill、选择 SOP、执行工具完成操作。

### 1.3 设计目标

| 目标 | 含义 |
| --- | --- |
| 开箱即用 | 下载 ZIP 即含当前用户的 `BASE_URL` 与专属 Token，无需手工配置 |
| 行为与页面一致 | 直接复用平台既有导入/编辑接口，不另造一套逻辑 |
| 可被 Agent 稳定执行 | 统一 CLI 契约、统一 JSON 输出、错误可判读 |
| 可扩展 | 新增产品能力只需按固定顺序补工具 + SOP + 路由 |

---

## 二、核心设计思路

### 2.1 三层分离

Skill 内部严格分三层，职责单一、互不污染：

```
用户自然语言
   │
   ▼
SKILL.md          场景路由层：判断意图，指向具体 SOP
   │
   ▼
references/*.md   SOP 层：描述步骤、工具名、参数来源、失败分支
   │
   ▼
scripts/*.py      工具层：真正执行（鉴权 + REST 调用 + 编排 + 输出）
```

| 层 | 文件 | 职责 | 不做什么 |
| --- | --- | --- | --- |
| 路由层 | `SKILL.md` | 识别场景、分发 SOP | 不写长流程 |
| SOP 层 | `references/0x-*.md` | 业务步骤与工具编排说明 | 不写代码 |
| 工具层 | `scripts/*.py` | 执行；原子层只映射接口，编排层做组合 | 不做业务判断 |

### 2.2 双层工具模型

工具层再分两层，对应「REST 原子工具 → 业务编排工具」的递进封装：

| 层 | 文件 | 定位 | 示例 |
| --- | --- | --- | --- |
| REST 原子层 | `scripts/rest_tools.py` | 一个函数 = 一个 REST 接口，只做鉴权 + 请求 + 原样返回 | `get_blog(blog_id)`、`list_columns()` |
| 业务编排层 | `scripts/blogloom-skill.py` | 一个工具 = 一个业务动作，可组合多个原子接口 | `import_blog`、`sync_local_blog`、`update_column_relation` |

**判定口诀**：只调 1 个接口、无业务判断 → 原子层；≥2 个接口或含参数推导/结果聚合/重试 → 编排层。

### 2.3 统一 CLI 契约

所有工具（含原子层暴露的只读工具）共用同一调用形式：

```bash
python3 scripts/blogloom-skill.py --tool <工具名> --params '<JSON对象>' [--pretty]
```

- `--tool`：选择具体工具；
- `--params`：承载全部业务参数（合法 JSON 对象）；
- `--pretty`：仅控制输出缩进；
- 统一输出：`{"ok": true|false, "tool": "...", "data": {...}, "error": null|"..."}`。

### 2.4 参数与鉴权

| 变量 | 来源 | 说明 |
| --- | --- | --- |
| `BLOOM_BASE_URL` | 环境变量 / `--params.base_url` | 服务地址，可被 payload 临时覆盖（便于指向测试环境） |
| `BLOOM_API_TOKEN` | 环境变量（**仅此一处**） | 鉴权，禁止写入 `--params` 或代码 |

- 缺参时**一次性列出**所有缺失项，减少 Agent 无效轮次；
- 参数路径用 `.get()` 逐级取，接口少字段时走缺参分支而非 `KeyError`。

### 2.5 核心决策：复用平台接口，不重复造轮子

| 场景 | 复用的页面能力 | Skill 因此获得 |
| --- | --- | --- |
| 新增/导入 | 「导入 Markdown 到知识库」接口 | `knowledgeBasePath` 目录归位、缺失目录逐级创建、标签/分类自动创建、专栏只匹配已存在、`SKIP`/`RENAME` 冲突策略 |
| 修改 | 「编辑博客」接口 | 按 id 更新，未传字段沿用原文 |

因此 Skill **不自行实现**目录创建或节点搬移，行为与页面完全一致，维护成本最低。

### 2.6 封面图识别规则（重点）

封面图**不取正文第一张图**，而是要求**显式标记**：只有 `alt` 为 `coverImg` 的图片才作为封面。

```markdown
![coverImg](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/cover.png)
```

| 情形 | 结果 |
| --- | --- |
| `![coverImg](url)` | 该 `url` 作为封面 |
| `<img alt="coverImg" src="url">` | 该 `url` 作为封面 |
| 两种写法同时存在 | 取正文中更靠前的一个 |
| 只有普通图片 / 无图片 | 无封面 |

后端由 `KnowledgeCoverExtractor.extract(content)` 统一实现，`KnowledgeImportWorker` 调用；Skill 侧 `pick_first_picture` 保持同一规则。

---

## 三、Skill 标准及扩展规范

### 3.1 目录结构（强制）

```text
blogloom-skill/
├── SKILL.md                      # 场景路由层
├── references/
│   ├── 00-tool-contract.md       # 原子工具契约（工具名/参数/返回/错误码）
│   ├── 01-import-blog-sop.md     # 新增/导入 SOP
│   ├── 02-update-blog-sop.md     # 修改 SOP
│   ├── 03-query-blog-sop.md      # 查询 SOP
│   └── 99-rest-fallback-sop.md   # 契约未覆盖接口兜底 SOP
├── scripts/
│   ├── blogloom-skill.py         # 业务 CLI（编排层）+ 入口
│   ├── rest_tools.py             # REST 原子工具（一一映射层）
│   └── default-cover.png         # 内置默认封面
├── test/
│   └── test_blogloom_skill.py    # 单测
├── skill.env.yaml                # 环境变量声明（平台元数据）
└── skill.env.sh                  # 环境变量脚本（下载时注入 token）
```

### 3.2 标准扩展顺序

新增一项产品能力时，按以下顺序落地：

1. 在 `rest_tools.py` 登记一一映射的**只读 REST 工具**；
2. 在 `blogloom-skill.py` 的 `build_tool_registry` 封装**业务工具及多接口编排**；
3. 新增/更新 **SOP**，并挂载到 `SKILL.md` 场景路由表；
4. 补充工具清单、参数映射、编排及错误边界**测试**；
5. 运行单测与 Skill **结构校验**。

### 3.3 打包与注入规范

| 环节 | 实现 | 说明 |
| --- | --- | --- |
| 下载入口 | `GET /admin/skill/download`（`SkillAdminController`） | 当前登录用户专属 |
| 打包 | `SkillPackageService.buildZip(username)` | 扫描 classpath 下 `skill/blogloom-skill/**` 打成 ZIP |
| Token 注入 | `skill.env.sh` 中替换 `__BLOOM_API_TOKEN__` / `__BLOOM_BASE_URL__` | 按当前用户签发 JWT，开箱即用 |
| 过滤 | 跳过 `__MACOSX` / `__pycache__` / `.pyc` | 避免打包缓存脏文件 |

---

## 四、Skill 功能情况

### 4.1 工具清单

**REST 原子工具**（`rest_tools.py`）

| 工具 | 方法 / 路径 | 说明 |
| --- | --- | --- |
| `list_categories` | GET `/admin/categories` | 分类列表 |
| `list_tags` | GET `/admin/tags` | 标签列表 |
| `list_columns` | GET `/admin/columns/options` | 专栏树（含二级） |
| `get_blog` | GET `/admin/blog` | 博客详情 |
| `list_blogs` | GET `/admin/blogs` | 文章列表（标题模糊匹配） |
| `update_blog` | PUT `/admin/blog` | 编辑文章（须含 id） |
| `update_visibility` | PUT `/admin/blog/{id}/visibility` | 改可见性 |
| `update_top` | PUT `/admin/blog/top` | 置顶 |
| `update_recommend` | PUT `/admin/blog/recommend` | 推荐 |
| `preview_import_files` | POST `/admin/knowledge/import/preview/files` | 导入预检 |
| `execute_import` | POST `/admin/knowledge/import/{token}/execute` | 提交导入任务 |
| `get_import_progress` | GET `/admin/knowledge/import/tasks/{taskId}` | 轮询导入进度 |
| `upload_resource` | POST `/admin/blog/resources` | 上传图片资源 |

**业务编排工具**（`blogloom-skill.py`）

| 工具 | 作用 | 关键参数 |
| --- | --- | --- |
| `import_blog` | 新增/导入本地 Markdown（单篇/多篇/目录） | `file_path` / `file_paths` / `dir_path`、`conflict_policy`、`published`、`target_parent_id` |
| `sync_local_blog` | 单篇智能同步（无则新增、有则更新） | `file_path`、`mode`（auto/create/update）、`blog_id` |
| `import_markdown_dir` | `import_blog` 的目录别名（兼容旧调用） | `dir_path`、`recursive` |
| `update_blog` | 编辑已有文章（未传字段沿用原文） | `blog_id` + 待改字段 |
| `update_column_relation` | 专栏关联维护 | `blog_id`、`columns`、`mode`（replace/add/remove） |
| `publish_visibility` | 可见性/赞赏/评论等开关 | `blog_id`、`published`、`password` |
| `update_top` / `update_recommend` | 置顶 / 推荐 | `blog_id`、`top` / `recommend` |
| `upload_resource` | 上传图片资源 | `file_path` |
| `get_blog` / `list_blogs` / `list_categories` / `list_tags` / `list_columns` | 查询类 | 见上表 |

### 4.2 能力覆盖矩阵

| 能力 | 状态 | 说明 |
| --- | --- | --- |
| 单篇/多篇/目录导入 | ✅ | 支持 `recursive` 递归 |
| knowledgeBasePath 目录归位 | ✅ | 复用平台导入接口，缺失目录逐级创建 |
| 标签/分类自动创建 | ✅ | 不存在时新建并关联 |
| 专栏只匹配不创建 | ✅ | 含二级专栏，未匹配项在 `unresolved` 返回 |
| 封面图识别 | ✅ | 仅识别 `alt=coverImg` 的图片 |
| 创建/更新时间写入 | ✅ | `YYYY-MM-DD HH:mm:ss` |
| 同名冲突策略 | ✅ | `SKIP`（默认）/ `RENAME` |
| 智能同步（幂等） | ✅ | 按标题判断新增/更新，重复执行不产生重复文章 |
| 编辑正文与参数 | ✅ | 未传字段沿用原文，不误清空 |
| 查询与搜索 | ✅ | 文章列表、分类、标签、专栏 |
| 鉴权失败提示 | ✅ | 提示在 CMS 重新下载 Skill 刷新 token |

### 4.3 错误约定

| 场景 | 返回 |
| --- | --- |
| 缺参 | `ok=false`，一次性列出所有缺失字段 |
| `--params` 非法 JSON | `ok=false`，退出码 2 |
| 未知工具 | `ok=false`，附 `available` 列表 |
| 鉴权失效（401/403） | `ok=false`，提示重新下载 Skill |
| 业务错误（服务端 code≠200） | `ok=false`，透传服务端 `msg` |
| 导入失败 | `ok=false`，`progress.status=FAILED`，`message` 为原因 |

---

## 五、如何使用 Skill

### 5.1 获取与准备

1. 后台进入「文章管理」，点击右上角 **「Skill 下载」**（或直接请求 `GET /admin/skill/download`），得到 `blogloom-skill.zip`；
2. 解压后进入目录，安装依赖并载入环境变量：

```bash
cd blogloom-skill
pip install requests
source ./skill.env.sh      # 载入 BLOOM_BASE_URL / BLOOM_API_TOKEN（下载时已注入）
```

3. 把整个 `blogloom-skill/` 目录交给支持 Skill 的 AI Agent（Agent 会自动读 `SKILL.md` 并按需读取 `references/` SOP）。

> Token 过期时，重新下载 Skill 即可刷新。

### 5.2 手工命令行验证

```bash
# 查询：按标题搜索文章
python3 scripts/blogloom-skill.py --tool list_blogs --params '{"title":"Maven","page_size":20}' --pretty

# 导入：单篇
python3 scripts/blogloom-skill.py --tool sync_local_blog --params '{"file_path":"/abs/path/blog.md"}' --pretty

# 导入：整个目录
python3 scripts/blogloom-skill.py --tool import_blog --params '{"dir_path":"/abs/dir","recursive":true,"conflict_policy":"SKIP"}' --pretty

# 修改：改可见性
python3 scripts/blogloom-skill.py --tool publish_visibility --params '{"blog_id":11,"published":false}' --pretty
```

### 5.3 Markdown 文章格式约定

每篇文章就是一个普通的 `.md` 文件，格式分两部分：**文件最顶部一个 ` ```json ` 元数据代码块**（声明文章属性），**其后是正常 Markdown 正文**。封面图不是元数据字段，而是在正文里用 `![coverImg](图片URL)` 显式标记。

````markdown
```json
{
  "title": "SqlParser解析器快速入门",
  "tags": "AI技术,SEO优化,内容营销",
  "category": "SqlParser",
  "articleSummary": "本文探讨了如何优化博客内容，帮助提升搜索引擎排名。",
  "columns": "SqlParser, AI",
  "createTime": "2026-09-19 14:30:00",
  "updateTime": "2026-09-19 14:30:00",
  "knowledgeBasePath": "/0x05、Java后端/SqlParser"
}
```

# SqlParser解析器快速入门

![coverImg](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/cover.png)

这里是正文内容……

## 一、背景

正文支持**加粗**、`行内代码`、列表、代码块等标准 Markdown 语法，按原样保留。
````

格式规则：

| 部分 | 要求 |
| --- | --- |
| 元数据代码块 | 必须在文件**最顶部**，围栏为 ` ```json `（`json` 大小写不敏感）；导入后该代码块会从正文移除 |
| `title` / `tags` / `category` / `articleSummary` / `columns` / `createTime` / `updateTime` / `knowledgeBasePath` | 均为可选字段，缺省时有各自兜底 |
| 封面图 | 正文中 `![coverImg](url)`，**只认 `coverImg` 这个 alt**；普通图片不作为封面 |
| 正文 | 代码块之后的全部内容，标准 Markdown 语法原样保留 |
| 无元数据代码块 | 按纯 Markdown 处理，标题回退文件名，代码块内容非法 JSON 时视为无元数据 |

### 5.4 快捷提示语示范

直接对 Agent 说一句自然语言即可，Agent 会自动命中对应 SOP：

| 场景 | 快捷提示语 |
| --- | --- |
| 单篇导入 | 「把 `/Users/me/blogs/maven.md` 这篇导入到博客」 |
| 批量导入目录 | 「把这个目录下的所有博客同步到平台：`/Users/me/blogs/Java`」 |
| 目录归位导入 | 「把这篇文章导入到知识库的 `SqlParser` 目录下」 |
| 断点续传/避免重复 | 「这个目录导入时遇到同名的就跳过」 |
| 同步更新 | 「`maven.md` 我改过了，把它同步更新到线上」 |
| 搜索文章 | 「帮我找一下平台上标题带 `Maven` 的文章」 |
| 查看详情 | 「看一下 id 为 11 的文章详情」 |
| 改可见性 | 「把文章 11 设为私密」 |
| 置顶/推荐 | 「把文章 11 置顶」「把文章 11 设为推荐」 |
| 换专栏 | 「把文章 11 的专栏改成 `Maven&Gradle`」 |
| 对齐准备 | 「列出所有专栏和分类，我导入前确认下」 |
| 综合排查 | 「查一下文章 11 的标签和专栏，然后补上缺失的」 |

---

## 六、相关实现索引

| 组件 | 路径 |
| --- | --- |
| 下载接口 | `blog-backend/.../controller/admin/SkillAdminController.java` |
| 打包注入服务 | `blog-backend/.../service/SkillPackageService.java` |
| Skill 资源根 | `blog-backend/src/main/resources/skill/blogloom-skill/` |
| 封面提取器 | `blog-backend/.../module/knowledge/support/KnowledgeCoverExtractor.java` |
| 导入执行器 | `blog-backend/.../module/knowledge/service/impl/KnowledgeImportWorker.java` |
| 前端下载入口 | `blog-cms-ui/src/views/blog/blog/BlogList.vue`、`src/api/blog.js` |
| 导入规则 PRD | `docs/prd/核心功能业务逻辑/后台快速迁移导入逻辑/知识库-博客迁移导入标准逻辑.md` |