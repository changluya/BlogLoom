# 00 原子工具契约

> 工具层统一入口：`python3 scripts/blogloom-skill.py --tool <工具名> --params '<JSON>' [--pretty]`
> 统一输出：`{"ok": true|false, "tool": "...", "data": {...}|null, "error": null|"..."}`
> 环境变量：`BLOOM_BASE_URL`（服务地址，可用 `--params.base_url` 覆盖）、`BLOOM_API_TOKEN`（鉴权，仅环境变量）。

## 1. 核心环境变量

| 变量 | 含义 | 来源 |
| --- | --- | --- |
| `BLOOM_BASE_URL` | BlogLoom 服务地址，如 `http://localhost:8090` | `skill.env.sh` / `--params.base_url` |
| `BLOOM_API_TOKEN` | 登录 Token（JWT），请求头 `Authorization: <token>` | `skill.env.sh`，**禁止写入 --params** |

## 2. 设计原则：新增走导入接口，修改走编辑接口

| 场景 | 复用平台能力 | 说明 |
| --- | --- | --- |
| 新增 / 导入 | 页面「导入 Markdown 到知识库」接口 | 原生支持 `knowledgeBasePath` 目录归位、缺失目录逐级创建、标签/分类自动创建、专栏只匹配已存在、`SKIP`/`RENAME` 冲突策略 |
| 修改 | 页面「编辑博客」接口 | 按 id 更新正文与参数，正文/字段未传则沿用原文 |

> 因此 Skill **不自行实现**目录创建或节点搬移，全部复用平台接口，行为与页面完全一致。

## 3. 原子工具（REST 一一映射）

| 工具名 | 方法 / 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| `list_categories` | GET `/admin/categories` | `pageNum`, `pageSize` | 分类列表 |
| `list_tags` | GET `/admin/tags` | `pageNum`, `pageSize` | 标签列表 |
| `list_columns` | GET `/admin/columns/options` | 无 | 专栏树（含 `id`/`name`/`children`） |
| `get_blog` | GET `/admin/blog` | `id` | 博客详情 |
| `list_blogs` | GET `/admin/blogs` | `title`, `categoryId`, `pageNum`, `pageSize` | 文章列表（标题模糊匹配） |
| `update_blog` | PUT `/admin/blog` | `blog`（**必须含 id**） | 编辑文章 |
| `update_visibility` | PUT `/admin/blog/{id}/visibility` | `id` + `visibility` | 改可见性 |
| `update_top` | PUT `/admin/blog/top` | `id`, `top` | 置顶 |
| `update_recommend` | PUT `/admin/blog/recommend` | `id`, `recommend` | 推荐 |
| `preview_import_files` | POST `/admin/knowledge/import/preview/files` | `files`, `targetParentId`, `published`, `conflictPolicy` | 导入预检 |
| `execute_import` | POST `/admin/knowledge/import/{token}/execute` | `token` | 提交导入任务 |
| `get_import_progress` | GET `/admin/knowledge/import/tasks/{taskId}` | `taskId` | 轮询导入进度 |
| `upload_resource` | POST `/admin/blog/resources` | `file_path` | 上传图片资源 |

## 4. 业务编排工具

### 4.1 `import_blog` —— 新增/导入本地 Markdown（复用页面导入接口）

| 参数 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- |
| `file_path` | 三选一 | — | 单个 `.md` 文件 |
| `file_paths` | 三选一 | — | 多个 `.md` 文件数组 |
| `dir_path` | 三选一 | — | 目录（配合 `recursive`） |
| `recursive` | 否 | `true` | 目录是否递归 |
| `target_parent_id` | 否 | `0` | 导入根目录节点 id（0=知识库根） |
| `published` | 否 | `true` | 导入后是否公开 |
| `conflict_policy` | 否 | `SKIP` | `SKIP` 跳过同名 / `RENAME` 自动重命名 |
| `timeout` / `poll_interval` | 否 | `300` / `1.0` | 轮询超时与间隔（秒） |

返回：`preview`（目录数/文档数/路径列表）、`progress`（最终进度与计数）、`importedBlogs`（每篇标题与回查到的 `blog_id`、`knowledgeBasePath`）。

### 4.2 `sync_local_blog` —— 单篇智能同步

| 参数 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- |
| `file_path` | 是 | — | 本地 `.md` 路径 |
| `mode` | 否 | `auto` | `auto` 按标题判断 / `create` 强制新增 / `update` 强制更新 |
| `blog_id` | 否 | — | 指定目标文章（强制更新） |
| `title`/`category`/`tags`/`columns`/`description`/`knowledgeBasePath` | 否 | 取元数据 | 新增时通过改写顶部元数据覆盖；更新时直接覆盖字段 |

- `auto`：平台已有同标题文章 → 走**编辑接口**更新；否则 → 走**导入接口**新增。
- 幂等：重复执行不会产生重复文章。

### 4.3 `import_markdown_dir` —— `import_blog` 的目录别名（兼容旧调用）

### 4.4 `update_blog` —— 编辑已有文章（复用页面编辑接口）

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `blog_id` | 是 | 目标文章 id |
| `file_path` / `content` | 否 | 覆盖正文（`file_path` 会自动去掉顶部元数据代码块） |
| `title`/`description`/`firstPicture`/`category`/`tags`/`columns` | 否 | 覆盖对应字段（导入首图只认正文 alt 为 `coverImg` 的图片，用 `firstPicture` 可显式指定封面） |
| `published`/`top`/`recommend`/`appreciation`/`commentEnabled`/`password` | 否 | 覆盖对应开关 |

未传的字段一律沿用原文，避免误清空。返回 `updated` 列出实际变更的字段。

### 4.5 `update_column_relation` —— 专栏关联维护

`blog_id`（必填）、`columns`（必填，名称或 id 数组）、`mode`（`replace`/`add`/`remove`，默认 `replace`）。
专栏**只匹配已存在**（含二级），不存在则不关联、不创建，并在 `unresolved` 中返回。

### 4.6 `publish_visibility` / `update_top` / `update_recommend` / `upload_resource`

见 §3 对应原子工具；`publish_visibility` 支持 `blog_id` + `published` + `password` + 各开关。

## 5. 错误约定

| 场景 | 返回 |
| --- | --- |
| 缺参 | `ok=false`，`error` 一次性列出所有缺失字段 |
| `--params` 非法 JSON | `ok=false`，进程退出码 2 |
| 未知工具 | `ok=false`，附 `available` 列表 |
| 鉴权失效（401/403） | `ok=false`，提示重新下载 Skill 刷新 token |
| 业务错误（服务端 code≠200） | `ok=false`，`error` 透传服务端 `msg` |
| 导入失败 | `ok=false`，`progress.status=FAILED` 且 `message` 为失败原因 |