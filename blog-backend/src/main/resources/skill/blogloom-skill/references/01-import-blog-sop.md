# 01 本地新增博客 / 批量导入 SOP

## 适用场景

用户想把**本地 Markdown 博客**新增到 BlogLoom：

- 单篇："把 `xxx.md` 导入/发布到博客"；
- 批量："把这个目录下的博客全部同步上去"。

## 前置参数

| 参数 | 来源 | 说明 |
| --- | --- | --- |
| `BLOOM_BASE_URL` / `BLOOM_API_TOKEN` | 环境变量（下载包已内置） | 无需用户提供 |
| `file_path` / `file_paths` / `dir_path` | 用户提供 | 单篇 / 多篇 / 目录 |
| `published` | 可选 | 是否公开，默认 `true` |
| `conflict_policy` | 可选 | `SKIP`（默认）/ `RENAME` |
| `target_parent_id` | 可选 | 导入根目录节点 id，默认 `0`（知识库根） |

## 核心：完全复用页面「导入 Markdown 到知识库」接口

`import_blog` / `sync_local_blog`（新增分支）调用平台导入接口，因此与页面导入行为**完全一致**：

1. 每篇 Markdown 顶部的 ```json 元数据代码块被解析并从正文移除；
2. **目录由 `knowledgeBasePath` 决定**（格式 `/a/bb/cc`），缺失目录**逐级自动创建**，文章挂载到对应知识库目录；
3. 标题取 `title`（缺省文件名）、描述取 `articleSummary`（缺省截取正文）；
4. **封面图（首图）只识别 alt 为 `coverImg` 的图片**：`![coverImg](url)` 或 `<img alt="coverImg" src="url">`（取更靠前者）；正文中其他普通图片**不再**被当作封面，无 `coverImg` 标记则**无封面**；
5. 标签、分类不存在**自动创建**并关联；专栏**只匹配已存在的**（含二级），不创建；
6. `createTime` / `updateTime` 按 `YYYY-MM-DD HH:mm:ss` 写入；
7. 同名文档按 `conflict_policy` 处理（SKIP 跳过 / RENAME 自动 `(n)` 重命名）。

> `knowledgeBasePath` 为空时文章落在知识库根目录。

### 封面图（首图）约定

封面图**不再取正文第一张图**，而是**显式标记**：只有 alt 为 `coverImg` 的图片才作为封面。

```markdown
![coverImg](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202609212201801.png)
```

| 情形 | 结果 |
| --- | --- |
| 正文含 `![coverImg](url)` | 该 `url` 作为封面 |
| 正文含 `<img alt="coverImg" src="url">` | 该 `url` 作为封面 |
| 同时存在两种写法 | 取正文中**出现位置更靠前**的一个 |
| 正文只有普通图片（alt 非 `coverImg`） | **无封面**（首图为空） |
| 无任何图片 | **无封面** |

> 建议在为文章生成/整理内容时，把封面图写成 `![coverImg](...)`，与正文插图区分开；正文中该图仍保留显示。

## 步骤

### A. 单篇新增（推荐）

```bash
python3 scripts/blogloom-skill.py --tool sync_local_blog \
  --params '{"file_path":"/abs/path/blog.md"}' --pretty
```

- `sync_local_blog` 自动判断：平台无同标题文章 → **导入接口新增**；已有 → **编辑接口更新**（见 02 SOP）。

### B. 强制新增 / 批量导入

```bash
# 单篇强制新增
python3 scripts/blogloom-skill.py --tool sync_local_blog \
  --params '{"file_path":"/abs/path/blog.md","mode":"create"}' --pretty

# 目录批量导入
python3 scripts/blogloom-skill.py --tool import_blog \
  --params '{"dir_path":"/abs/dir","recursive":true,"conflict_policy":"SKIP"}' --pretty

# 多文件批量导入
python3 scripts/blogloom-skill.py --tool import_blog \
  --params '{"file_paths":["/a.md","/b.md"],"published":false}' --pretty
```

## 结果判读

依据返回字段汇报：

| 字段 | 含义 |
| --- | --- |
| `preview.directoryCount` / `documentCount` / `paths` | 预检到的目录数、文档数、路径列表 |
| `progress.status` | `SUCCESS` / `FAILED` |
| `progress.createdDirectoryCount` / `createdBlogCount` / `skippedCount` | 新建目录数 / 新建博客数 / 跳过数 |
| `importedBlogs[]` | 每篇的 `title`、回查到的 `blog_id`、`knowledgeBasePath` |

> 注意：目录已存在时会计入 `skippedCount`（目录复用），不代表文章被跳过；文章是否新建看 `createdBlogCount` 与 `importedBlogs[].blog_id`。

## 分支与异常

| 情况 | 处理 |
| --- | --- |
| 文件/目录不存在 | 工具报「文件不存在 / 目录不存在」，让用户确认路径 |
| 顶部无元数据代码块 | 标题回退文件名，分类默认「知识库」，摘要自动截取正文 |
| 元数据非法 JSON | 视为无元数据，代码块保留在正文 |
| 正文无 `![coverImg](...)` 标记 | 无封面（首图空），不影响导入，由平台补默认封面 |
| `knowledgeBasePath` 格式非法 | 预检直接报错（必须以 `/` 开头、末尾不带 `/`、无空段等） |
| 同名文档 | `conflict_policy=SKIP` 跳过；`RENAME` 生成 `(n)` |
| 鉴权失败 | 提示在 CMS「文章管理」右上角重新下载 Skill 刷新 token |

## 输出要求

一句话结论 + 证据字段，例如：

> 已导入 1 篇：`Maven插件—05…`（#11），已归入知识库目录 `0x05、Java后端/01、Java基础知识点/项目管理工具/maven/maven插件`，新建 5 个目录。