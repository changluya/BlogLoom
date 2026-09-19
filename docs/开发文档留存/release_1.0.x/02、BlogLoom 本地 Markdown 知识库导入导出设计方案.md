# BlogLoom 本地 Markdown 知识库导入导出设计方案

> 迭代版本：`release_1.0.x`
>
> 文档状态：需求分析与可落地技术方案
>
> 首期范围：独立知识库管理页面、目录树管理、纯 Markdown ZIP 导入与导出

## 1. 背景与目标

当前 `blog` 表已经承载文章标题、Markdown 正文、发布状态、分类、标签等博客核心数据。本需求希望把本地 Markdown 文件夹快速映射为 BlogLoom 中的知识库目录，并支持将整个知识库导出为可以再次导入的 ZIP，同时不能要求修改已有 `blog` 表结构或批量改写历史博客记录。

本方案的核心是新增一张独立的知识库文档树表：

- 文件夹映射为 `DIR` 节点，本身也占一条记录。
- Markdown 文件映射为 `DOC` 节点，并通过 `blog_id` 引用博客文章。
- 所有没有显式目录结构的存量博客，初始化时默认映射为根目录下的一级 `DOC` 节点。
- Markdown 正文仍以 `blog.content` 为唯一业务数据源，树表不重复保存正文。
- 在管理后台新开独立的“知识库”页面管理整棵树，不把树形管理强行放入现有博客列表页。
- 首期只处理目录和 `.md` 文件；图片、附件、Front Matter 高级映射及本地实时监听放到后续版本。

## 2. 首期范围与非目标

### 2.1 首期必须实现

1. 新增独立知识库管理页面，展示并管理目录树。
2. 支持创建、重命名、移动、排序、删除目录节点。
3. 初始化时把现有博客映射为根目录文档节点，之后可移动到任意目录，且不修改该博客记录。
4. 支持上传一个包含目录和 Markdown 文件的 ZIP，并按路径创建目录、博客和映射节点。
5. 支持将当前整棵知识库导出为 ZIP，目录映射为文件夹，文档映射为 `.md` 文件。
6. 导入前完成格式、安全和冲突预检，导入完成后返回逐项结果。
7. 上线初始化后，所有存量博客默认出现在知识库根目录第一级。

### 2.2 首期暂不实现

- 不监听服务端或用户电脑上的本地文件夹变化。
- 不做 Git、语雀、Notion 等外部知识库同步。
- 不导入图片、音视频及其他附件，也不改写 Markdown 内的相对资源链接。
- 不支持一个文档节点同时关联多篇博客。
- 不在公开博客站点直接提供知识库树形导航；首期页面仅位于管理后台。
- 不解决多人同时编辑和复杂版本历史。

## 3. 现状与参考设计结论

### 3.1 BlogLoom 现状

`blog` 表的 `content` 已是 Markdown 正文，适合作为文档内容的数据源。文章新增与更新由 `BlogServiceImpl` 管理，并同步维护标签、缓存和本地资源。因此 ZIP 导入不能只通过 Mapper 裸写 `blog`，应复用或下沉现有博客创建领域逻辑，防止生成“不完整博客”。

现有博客必填字段较多，例如首图、描述、分类、发布开关、字数和阅读时长。ZIP 导入接口不要求用户选择分类或填写首图，由服务端使用知识库导入的内部默认值补齐。

### 3.2 AIChat 参考表的取舍

参考文件 `202507150000_v1.1.x.sql` 中的 `knowledge_yuque_document` / `knowledge_document` 提供了 `DOC`、`DIR` 类型及父节点标识，证明目录和文档使用统一节点模型是可行的。

BlogLoom 首期不直接照搬以下字段：

- 不保存 `markdown_content`，避免它与 `blog.content` 出现双数据源冲突。
- 不使用 `parent_uuid`、`child_uuid` 同时描述关系；只保存 `parent_id`，子节点由查询得到。
- 不引入知识库基础表；个人博客首期只有一棵默认知识库树。
- 不加入向量同步、来源平台、发布审核等当前没有业务消费方的字段。

## 4. 总体架构

```text
管理后台独立知识库页面
  |- 目录树 CRUD / 拖拽移动排序
  |- 存量博客根目录初始化与移动
  |- ZIP 导入预检与执行
  `- 整库 ZIP 导出
             |
             v
KnowledgeNodeController
             |
             v
KnowledgeNodeService + MarkdownArchiveService
       |                         |
       v                         v
knowledge_node              临时 ZIP 工作目录
       |
       `---- DOC.blog_id ----> blog（正文唯一数据源）
```

目录树与博客内容是“结构”和“内容”的分工关系：

| 数据 | 唯一数据源 | 说明 |
| --- | --- | --- |
| 父子关系、排序、节点名 | `knowledge_node` | 不影响博客发布和历史数据 |
| Markdown 正文及文章属性 | `blog` | 沿用现有博客领域逻辑 |
| ZIP 内相对路径 | 导入/导出时计算 | 不作为长期业务真相 |

### 4.1 存量博客的默认根目录映射

知识库功能上线时，已有博客通常没有任何目录信息。默认规则为：每篇尚未建立映射的博客都位于虚拟根节点下，即创建一条 `parent_id=0` 的一级 `DOC` 记录。

```text
虚拟根节点（不入库）
|- 历史博客 A（DOC -> blog.id=A）
|- 历史博客 B（DOC -> blog.id=B）
`- 历史博客 C（DOC -> blog.id=C）
```

这里采用“补建真实映射记录”，不采用查询时临时拼装虚拟文档，原因如下：

- 所有文档都走同一套移动、排序、重命名和导出逻辑。
- Service 层校验博客只在树中出现一次。
- 初始化完成后不会每次树查询都扫描并计算未挂载博客。
- 只新增 `knowledge_node` 数据，不修改任何历史 `blog` 记录。

上线增量 SQL 在建表后执行幂等回填：

```sql
INSERT INTO `knowledge_node`
  (`parent_id`, `blog_id`, `name`, `type`, `sort`, `create_time`, `update_time`)
SELECT
  0, b.id, b.title, 'DOC', 0, NOW(), NOW()
FROM `blog` b
LEFT JOIN `knowledge_node` kn ON kn.blog_id = b.id
WHERE kn.id IS NULL;
```

正式增量脚本应在回填时生成稳定的根目录排序值，例如根据 `blog.create_time ASC, blog.id ASC` 计算顺序；上面的 SQL 仅展示映射原则。

## 5. 数据库设计

### 5.1 表结构

建议在全量初始化脚本 `sql/increment/init.sql` 及对应的 `sql/increment/<版本>/` 增量 SQL 中新增：

```sql
CREATE TABLE `knowledge_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '节点ID',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父节点ID，0表示虚拟根节点',
  `blog_id` bigint DEFAULT NULL COMMENT 'DOC节点关联的博客ID，DIR节点必须为空',
  `name` varchar(255) NOT NULL COMMENT '节点显示名；DOC不包含.md后缀',
  `type` varchar(20) NOT NULL COMMENT '节点类型：DIR目录、DOC文档',
  `sort` int NOT NULL DEFAULT 0 COMMENT '同级排序值，越小越靠前',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='博客知识库文档树';
```

### 5.2 字段约束

- `parent_id=0` 代表后端虚拟根节点，数据库中不保存根节点记录。
- `type=DIR` 时 `blog_id` 必须为 `NULL`；`type=DOC` 时 `blog_id` 必须非空。考虑当前 MySQL 兼容性，首期由 Service 层校验。
- 首期数据库只保留 `id` 主键；博客重复挂载及同级节点重名由 Service 层校验。
- 同一父节点下允许存在同名目录和文档，对应文件系统中 `guide/` 与 `guide.md` 可并存；同类型节点不能重名。
- 不设置数据库外键，以保持现有项目建表风格，并避免历史数据或部署脚本受外键顺序影响；引用完整性由 Service 事务保证。

### 5.3 删除一致性

- 删除博客时同步删除对应 `DOC` 节点，目录不受影响。
- 删除 `DOC` 节点时，同时删除对应博客、博客标签关联和评论数据。
- 删除 `DIR` 节点时递归删除其全部子目录、`DOC` 节点及对应博客；前端确认框必须明确展示受影响文档数量。
- 整个递归删除在同一数据库事务内完成，任一步失败则全部回滚；右键删除属于不可恢复的高风险操作。

## 6. 独立知识库管理页面

### 6.1 页面入口与布局

在 `blog-cms-ui` 左侧菜单新增“知识库”，建议路由为 `/knowledge`，页面文件建议为：

```text
src/views/knowledge/KnowledgeManage.vue
src/components/knowledge/KnowledgeTree.vue
src/components/knowledge/ImportDialog.vue
src/api/knowledge.js
```

桌面端采用左右布局：

```text
+----------------------+--------------------------------------+
| 知识库目录树         | 当前节点                              |
| [新建目录] [导入]    |                                      |
|                      | DIR：名称、父目录、子项统计           |
| ▾ Java               | DOC：标题、关联博客、更新时间         |
|   ▸ Spring           |      [编辑博客] [删除]                |
|   · JVM.md           |                                      |
| · README.md          | 全局操作：[导出全部]                  |
+----------------------+--------------------------------------+
```

页面职责是管理知识库结构。文档内容编辑首期复用现有 `WriteBlog.vue`，点击“编辑博客”跳转到已有博客编辑路由；后续如体验需要，再把编辑器嵌入右侧详情区。

### 6.2 树节点交互

节点右键菜单或行尾菜单包含：

| 节点 | 操作 |
| --- | --- |
| 空白区域/根节点 | 新建目录、导入 ZIP、修复未映射博客 |
| `DIR` | 新建子目录、重命名、移动、删除 |
| `DOC` | 编辑博客、重命名节点、移动、删除 |

补充规则：

- 树默认一次性加载全部节点。个人博客规模通常较小，首期不增加懒加载复杂度。
- 排序规则为 `sort ASC, id ASC`；拖拽结束后批量提交当前同级顺序。
- 移动目录时必须校验目标不是自身或任一后代，避免形成环。
- 重命名 `DOC` 只修改树中的文件名，不默认修改 `blog.title`；页面可提供“同时更新博客标题”显式选项。
- 树节点展示博客已删除等异常状态，不能因为关联查询使用内连接而悄悄丢失节点。

### 6.3 存量博客与新增博客

初始化完成后，正常情况下不存在“未挂载博客”：所有存量博客已经位于根目录。用户可以直接将根目录文档拖入目标目录，或通过“移动到”弹窗选择目录。

通过现有博客发布页面新建博客时，博客创建事务必须同时插入一条 `parent_id=0` 的 `DOC` 节点。通过 ZIP 导入创建的博客则直接挂到 ZIP 对应目录，不先创建根节点。现有博客列表页可以增加“在知识库中定位”快捷入口，但知识库结构仍由独立页面管理。

## 7. 后端接口设计

统一前缀建议为 `/admin/knowledge`，复用现有 `/admin/**` JWT 权限和 `Result` 返回结构。

### 7.1 目录树接口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/admin/knowledge/tree` | 返回完整知识库树 |
| `POST` | `/admin/knowledge/directories` | 新建目录 |
| `POST` | `/admin/knowledge/documents/link` | 修复场景下为未映射博客补建节点 |
| `PUT` | `/admin/knowledge/nodes/{id}` | 重命名节点 |
| `PUT` | `/admin/knowledge/nodes/{id}/move` | 移动节点 |
| `PUT` | `/admin/knowledge/nodes/reorder` | 批量更新同级排序 |
| `DELETE` | `/admin/knowledge/nodes/{id}` | 删除节点；目录递归删除后代节点和对应博客，文档删除对应博客 |

树返回示例：

```json
[
  {
    "id": 1,
    "parentId": 0,
    "name": "Java",
    "type": "DIR",
    "sort": 0,
    "children": [
      {
        "id": 2,
        "parentId": 1,
        "blogId": 101,
        "name": "JVM 内存模型",
        "type": "DOC",
        "sort": 0,
        "blogTitle": "JVM 内存模型",
        "blogExists": true,
        "children": []
      }
    ]
  }
]
```

移动请求示例：

```json
{
  "targetParentId": 20,
  "targetSort": 3
}
```

Service 必须在一个数据库事务内完成：校验节点存在、校验目标父节点为 `DIR`、检测目录环、调整原父节点和新父节点排序。

### 7.2 导入导出接口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `POST` | `/admin/knowledge/import/preview` | 上传 ZIP 并预检，不写业务数据 |
| `POST` | `/admin/knowledge/import/{token}/execute` | 校验预检会话后提交异步导入任务，返回 `taskId` |
| `GET` | `/admin/knowledge/import/tasks/{taskId}` | 查询导入任务状态、进度和结果 |
| `GET` | `/admin/knowledge/export` | 导出完整知识库 ZIP |
| `GET` | `/admin/knowledge/export/documents/{id}` | 下载指定文档节点对应的 Markdown 原文 |

预检与执行分两步，避免用户上传后在不知道冲突数量等信息的情况下直接写入大量博客。`token` 对应服务端临时目录中的一次性导入会话，建议 30 分钟失效，执行成功或过期后清理。

预检请求使用 `multipart/form-data`：

```text
file              ZIP 文件
targetParentId    导入目标目录，默认 0（虚拟根目录）
published         是否直接发布，默认 false
conflictPolicy    SKIP 或 RENAME，默认 SKIP
```

预检响应至少包含：

```json
{
  "token": "temporary-import-token",
  "directoryCount": 8,
  "documentCount": 32,
  "ignoredCount": 4,
  "conflictCount": 2,
  "totalUncompressedBytes": 183421,
  "items": [
    {"path": "Java/JVM.md", "action": "CREATE", "message": ""},
    {"path": "Java/README.txt", "action": "IGNORE", "message": "首期仅支持.md"}
  ]
}
```

执行接口不阻塞等待全部文件导入，提交成功后立即返回 `taskId` 和初始状态 `PENDING`。前端使用 `taskId` 轮询进度接口，直到状态进入 `SUCCESS` 或 `FAILED`。

进度查询响应至少包含：

```json
{
  "taskId": "knowledge-import-task-id",
  "status": "RUNNING",
  "progress": 43,
  "total": 40,
  "processed": 17,
  "currentPath": "Java/Spring/IOC.md",
  "createdDirectoryCount": 4,
  "createdBlogCount": 12,
  "skippedCount": 1,
  "message": "正在导入 Java/Spring/IOC.md",
  "startedAt": "2026-09-16T10:00:00",
  "finishedAt": null
}
```

`progress` 为 `0-100` 的整数，按已处理的目录和 Markdown 项数计算。任务成功时附带最终导入结果；失败时返回可读错误和失败路径，不向前端暴露堆栈或 SQL 异常。

未指定 `targetParentId` 时，ZIP 内容默认直接导入虚拟根目录，不额外创建 ZIP 包名目录。若所有有效 Entry 共享唯一顶层目录，则导入时剥离该层，将其子项直接挂到虚拟根目录。本次导入的根级节点整体追加在根目录已有节点的最后，并保持稳定的导入顺序。

## 8. ZIP 导入设计

### 8.1 支持的 ZIP 结构

本地目录：

```text
my-knowledge/
|- README.md
|- Java/
|  |- JVM.md
|  `- Spring/
|     `- IOC.md
`- assets/
   `- architecture.png
```

默认导入结果（不保留 ZIP 包名作为额外目录）：

```text
虚拟根目录（不入库）
|- ...已有节点
|- README (DOC -> blog，本次导入)
`- Java (DIR，本次导入)
   |- JVM (DOC -> blog)
   `- Spring (DIR)
      `- IOC (DOC -> blog)
```

`assets/architecture.png` 及空的 `assets` 目录首期忽略。纯空目录是否保留建议为“保留”，因为目录本身属于知识库结构。

### 8.2 Markdown 到博客字段映射

| 博客字段 | 映射规则 |
| --- | --- |
| `title` | 固定取 Markdown 文件名（去掉 `.md` 后缀），不从正文标题推导 |
| `content` | 文件 UTF-8 原文，不移除一级标题 |
| `description` | 去除 Markdown 标记后的首段，截断到约 200 字；为空时使用标题 |
| `first_picture` | 不要求导入参数，首期写入空字符串 |
| `published` | 使用导入参数，默认 `false` |
| `recommend/appreciation/commentEnabled/top` | 使用安全默认值 `false/false/true/false` |
| `category` | 自动使用名为“知识库”的分类；不存在时在导入事务中创建 |
| `tags` | 首期为空，后续可解析 Front Matter |
| `words/readTime` | 复用现有字数和阅读时长计算规则 |
| `user` | 当前登录管理员 |

分类和首图不暴露为 ZIP 导入参数，避免每次导入前重复选择。数据库中的 `first_picture` 仍为非空字段，因此使用空字符串而不是 `NULL` 保持兼容。

### 8.3 导入算法

```text
接收 ZIP
  -> 保存到受控临时目录
  -> 逐 Entry 校验并标准化相对路径
  -> 生成内存目录/文档清单（不立即解压全部内容）
  -> 校验数量、大小、编码、重名及目标目录冲突
  -> 返回 preview token

用户确认
  -> 创建内存导入任务并返回 taskId
  -> 异步执行器将任务状态更新为 RUNNING
  -> 读取目标目录当前最大 sort，为导入的根级节点分配末尾排序值
  -> 按路径深度升序创建 DIR
  -> 对每个 .md 构造博客 DTO
  -> 调用可复用的博客创建领域服务创建 blog
  -> 插入 DOC 节点并关联 blog_id
  -> 每处理一项即更新内存任务进度
  -> 汇总结果
  -> 提交事务
  -> 将任务标记为 SUCCESS
  -> 清理临时文件
```

目录必须先于其子项创建。不能依赖 ZIP Entry 一定按父目录在前排列，因为合法 ZIP 不保证顺序，且 ZIP 可能不显式包含目录 Entry。导入节点的同级顺序应根据规范化后的 ZIP 路径稳定计算；目标目录已有节点不重排，新节点从当前最大 `sort` 之后追加。

首期异步执行不改变数据库事务边界：一次导入仍作为一个数据库事务，任一 Markdown 创建失败则回滚本次新建的博客和树节点，并将内存任务标记为 `FAILED`。为防止超大事务，上传限制应保持保守；持久化任务和分批导入放到后续版本。

### 8.4 冲突规则

首期提供两种明确策略：

- `SKIP`：目标父目录下存在同名同类型节点时跳过该节点；目录冲突时复用已有目录并继续处理子项。
- `RENAME`：自动生成 `名称 (1)`、`名称 (2)`；文档创建新博客，不覆盖原博客。

首期不提供 `OVERWRITE`。仅凭文件路径覆盖已有博客可能误改人工编辑的历史内容，且无法可靠判断本地文件和线上博客谁更新。后续若实现增量同步，应增加来源标识、内容哈希、上次同步基线和明确的三方冲突策略。

ZIP 内部的路径重名在预检阶段直接报错，例如大小写不敏感环境中的 `Java/a.md` 与 `Java/A.md`。同一目录中的 `foo/` 和 `foo.md` 可同时存在。

### 8.5 ZIP 安全约束

必须防护 Zip Slip 和 ZIP 炸弹：

1. 拒绝绝对路径、盘符路径及含 `..` 的 Entry。
2. 路径统一将 `\` 转为 `/`，Unicode 正规化后再次校验。
3. 即使首期不落盘解压，也要确保标准化后的目标路径位于本次临时目录内。
4. 限制 ZIP 压缩文件大小、Entry 数量、单文件解压大小、总解压大小和目录深度。
5. 初始默认值：ZIP 500 MB、单 Markdown 5 MB、最多 10,000 个原始 Entry、过滤后最多 2,000 个可导入目录和 Markdown、Markdown 解压总量 2 GB、最大 20 层；Spring multipart 的单文件和请求上限同步设置为 500 MB，最终做成配置项。扫描基于 ZIP 中央目录，只打开 `.md` 正文；`.git`、图片、xmind 等非 Markdown 文件不解压、不解析。
6. 拒绝加密 ZIP、符号链接和无法识别为 UTF-8 的 Markdown，并在预检结果中给出具体路径。
7. 文件扩展名以大小写不敏感方式判断，仅接受 `.md`；`__MACOSX/` 以及任一路径层级以 `.` 开头的隐藏文件或目录（如 `.idea/`、`.git/`、`.vscode/`、`.DS_Store`）静默忽略。
8. 原始文件名仅用于显示和逻辑路径，不用于拼接任意系统路径。

### 8.6 内存导入任务与进度

首期使用应用内存维护导入任务，不新增任务表。可使用 `ConcurrentHashMap<String, KnowledgeImportTask>` 作为任务容器，任务状态为 `PENDING`、`RUNNING`、`SUCCESS` 和 `FAILED`。进度更新需保证内存可见性，对外只返回不可变的任务快照，避免查询线程读到更新一半的状态。

- 预检成功只生成临时 `token`，用户确认后才创建导入任务。
- 同一个 `token` 只能成功提交一次，防止重复点击生成多个任务。
- 导入线程在处理每个目录或 Markdown 文件后更新 `processed`、`progress`、`currentPath` 和累计结果。
- `SUCCESS` 或 `FAILED` 任务保留 30 分钟供前端查询，之后由定时清理任务删除；ZIP 临时文件在任务结束后立即清理。
- 应用重启后内存任务状态会丢失，进度接口对不存在的 `taskId` 返回“任务已过期或服务已重启”。这是首期内存方案的明确边界。
- 首期使用有界异步线程池，不使用通用默认线程池；超出容量时返回“当前导入任务较多”。

## 9. ZIP 导出设计

### 9.1 导出规则

- `DIR` 节点生成同名文件夹。
- `DOC` 节点读取关联 `blog.content`，生成 `{节点名}.md`。
- 空目录也写入 ZIP Entry，保证往返导入后目录不丢失。
- 文件名中的 `/`、`\`、控制字符替换为安全字符，去除尾随空格和点。
- 同一导出目录发生文件系统名称冲突时追加稳定后缀，例如 `名称 (2).md`，并在导出报告中记录。
- 文档内容统一以 UTF-8 编码，换行符保持原正文，不额外插入或删除一级标题。
- 已丢失关联博客的异常 `DOC` 节点不输出空 Markdown；写入导出报告并继续其他节点。

响应建议：

```http
HTTP/1.1 200 OK
Content-Type: application/zip
Content-Disposition: attachment; filename="blogloom-knowledge-20260915.zip"
```

后端应使用 `ZipOutputStream` 直接流式写入 HTTP 响应，避免先把完整 ZIP 放进内存。导出查询可一次取回扁平节点列表和对应博客正文，再在内存构树，避免递归 N+1 查询。

### 9.2 往返一致性

首期承诺以下往返能力：

```text
知识库树 -> 导出 ZIP -> 导入到空知识库
```

目录层级、节点名和 Markdown 正文保持一致；博客 ID、创建时间、分类、标签、浏览量等站点属性不保证一致。若未来要求完整站点迁移，需要额外提供版本化 `blogloom-manifest.json`，这不属于“纯 Markdown 导入”首期。

## 10. 代码落点建议

### 10.1 后端

知识库功能按独立业务 module 组织，避免将 API、Domain、DAO 和 Service 分散到现有公共目录。首期在 `blog-backend` 内使用 `module.knowledge` 顶层包完成模块化隔离，不额外拆分 Maven 子工程；后续如需独立部署，可再整体迁移该包。

```text
blog-backend/src/main/java/com/changlu/blogloom/module/knowledge/
|- api/
|  `- KnowledgeNodeAdminController.java
|- domain/
|  |- entity/KnowledgeNode.java
|  |- enums/
|  |  |- KnowledgeNodeType.java
|  |  `- KnowledgeImportTaskStatus.java
|  |- dto/
|  |  |- KnowledgeDirectoryCreate.java
|  |  |- KnowledgeNodeLink.java
|  |  |- KnowledgeNodeMove.java
|  |  `- KnowledgeImportOptions.java
|  `- vo/
|     |- KnowledgeTreeNode.java
|     |- KnowledgeImportPreview.java
|     |- KnowledgeImportResult.java
|     `- KnowledgeImportProgress.java
|- dao/
|  `- KnowledgeNodeMapper.java
`- service/
   |- KnowledgeNodeService.java
   |- MarkdownArchiveService.java
   |- KnowledgeImportTaskService.java
   `- impl/
      |- KnowledgeNodeServiceImpl.java
      |- MarkdownArchiveServiceImpl.java
      `- InMemoryKnowledgeImportTaskService.java

blog-backend/src/main/resources/mapper/knowledge/
`- KnowledgeNodeMapper.xml
```

职责边界：

- `KnowledgeNodeService` 负责树节点的校验、查询、增删改、移动及排序。
- `MarkdownArchiveService` 负责 ZIP 解析、预检、Markdown 映射、导入编排与导出流。
- `KnowledgeImportTaskService` 负责导入任务的内存创建、进度更新、快照查询、终态保留和过期清理。
- 博客创建逻辑应从 Controller 参数适配中抽出可复用服务方法，让后台手工发布和 ZIP 导入共享核心规则；不要从导入服务反向调用 Controller。
- `KnowledgeNodeMapper` 返回扁平记录；Service 使用 `parent_id -> children` 映射一次构树，时间复杂度为 O(n)。

### 10.2 前端

```text
blog-cms-ui/src/
|- api/knowledge.js
|- views/knowledge/KnowledgeManage.vue
`- components/knowledge/
   |- KnowledgeTree.vue
   |- KnowledgeNodeDetail.vue
   `- ImportDialog.vue
```

首期优先复用项目现有 UI 组件和请求封装。导入弹窗分为“选择 ZIP 与默认属性”“预检结果”“导入进度”“执行结果”四个状态。用户确认预检结果后提交任务，立即进入进度页并禁止重复提交。

前端默认每 1 秒轮询一次任务进度，页面展示进度条、已处理数/总数、当前文件、已创建博客数和跳过数。任务进入终态或弹窗销毁时停止轮询；成功后刷新知识库树，失败时保留错误信息和失败路径。

## 11. 事务、并发与异常处理

### 11.1 事务边界

- 单节点 CRUD 使用短事务。
- 移动和排序在同一事务中完成。
- 通过普通博客管理页面新建博客时，博客和根目录 `DOC` 节点在同一事务中创建；删除博客时同步删除节点。
- 首期正式导入由异步任务执行，仍使用单事务写入博客、标签关系和知识库节点。
- ZIP 临时文件不属于数据库事务：数据库提交成功后清理；回滚后同样清理。定时任务兜底删除过期导入会话。
- 导出是只读操作，不创建业务数据。

### 11.2 并发控制

当前阶段不依赖数据库唯一索引，同级重名和博客重复挂载由 Service 层在事务内预检，并返回明确的业务错误。

首期通过应用内互斥锁限制同一实例一次只执行一个知识库导入，其他任务保持 `PENDING` 或直接拒绝，首期建议直接拒绝以避免临时 ZIP 长时间占用空间。若未来支持多实例部署，再引入 Redis 分布式锁和持久化导入任务。

### 11.3 异常节点修复

虽然正常流程会维护引用一致性，管理员手工改库仍可能产生 `DOC.blog_id` 不存在的情况。树查询应使用左连接并返回 `blogExists=false`。管理页面允许删除异常节点或重新绑定博客。

## 12. 性能预期

- 树查询：一次节点与博客概要左连接查询，内存 O(n) 构树。
- 导入预检：流式读取 ZIP Entry，限制总量；不得调用 `readAllBytes()` 读取无上限内容。
- 导入写入：首期上限 2,000 Entry 可接受逐文档创建；后续可对目录和节点做批量插入。
- 导出：一次查询、流式压缩、流式响应；客户端断开时及时关闭流和数据库资源。

## 13. 测试方案

### 13.1 单元测试

1. 扁平节点正确构造成多层树，排序稳定。
2. 移动目录到自身或后代时报错。
3. `DIR/blog_id`、`DOC/blog_id` 类型约束正确。
4. Markdown 文件名到博客标题的映射正确，并覆盖空文件、中文文件名和包含多个点的文件名；描述、字数映射正确。
5. 文件名清洗和导出重名处理稳定。
6. 路径标准化拒绝 `../`、绝对路径、Windows 盘符和反斜杠绕过。
7. ZIP 数量、深度、单文件和总解压大小限制生效。
8. 内存任务状态流转、进度计算、终态快照及过期清理正确。

### 13.2 集成测试

1. 上传嵌套 ZIP 后，目录和 `.md` 均正确映射，非 Markdown 文件被忽略。
2. ZIP 不含显式目录 Entry 时仍能补建父目录。
3. `SKIP` 与 `RENAME` 冲突策略符合预检结果。
4. 任一博客创建失败时，本次导入的博客和节点全部回滚。
5. 删除 DOC 时同步删除对应博客、标签关联和评论；删除博客时同步删除对应 DOC。
6. 删除非空目录时递归删除全部后代节点和对应博客，任一步失败时整体回滚。
7. 导出后再导入空库，树层级、节点名和正文一致。
8. 私有文章不会因为移动目录或导出操作被意外改为公开。
9. 升级后每篇存量博客都生成且只生成一个根目录 DOC，重复执行回填不产生重复节点。
10. 从普通博客页面新建文章后自动出现在知识库根目录。
11. 不传 `targetParentId` 时，ZIP 根级内容直接追加到虚拟根目录末尾，不创建 ZIP 包名目录，且不改变已有节点顺序。
12. 同一预检 `token` 不能重复提交；导入期间可持续查询递增进度，成功和回滚失败都能进入正确终态。

### 13.3 前端验收

1. 独立知识库菜单和页面可访问。
2. 新建、重命名、移动、排序、删除后树无需整页刷新即可正确更新。
3. 文档节点能跳转已有博客编辑页。
4. 导入预检清楚展示创建、忽略、冲突和错误项。
5. 预检通过后才可提交导入；执行期间可实时查看进度条、处理数量和当前文件，并避免重复点击。
6. 导入成功后停止轮询并刷新树，失败后停止轮询并保留可读错误。
7. DOC 和 DIR 节点右键均展示删除；目录删除确认框显示其下文档数量并明确级联删除范围。

## 14. 分阶段实施计划

### 阶段一：树模型与独立页面

1. 增加增量 SQL、实体、Mapper 和枚举。
2. 完成树查询、目录 CRUD、移动排序和环检测。
3. 完成存量博客根目录回填、新建博客自动建节点以及文档移动。
4. 增加管理后台独立知识库页面。

阶段一完成后，即使没有 ZIP，也可以人工组织历史博客的知识库目录。

### 阶段二：纯 Markdown ZIP 导入

1. 增加上传限制和临时会话管理。
2. 实现安全预检、字段映射和冲突策略。
3. 抽取可复用博客创建领域逻辑。
4. 实现内存导入任务、异步事务化导入、进度查询与前端实时展示。

### 阶段三：纯 Markdown ZIP 导出

1. 实现节点/整库流式导出。
2. 实现文件名清洗、空目录保留和异常报告。
3. 完成导出再导入的往返测试。

### 后续增强

- 图片与附件一起打包，导入后接入现有 `blogs/{blogId}` 资源归档能力并重写相对链接。
- 支持 Front Matter 映射分类、标签、发布时间、封面和发布状态。
- 增加来源路径、内容哈希、同步基线和导入任务表，实现本地目录增量/双向同步。
- 增加版本化 manifest，实现跨 BlogLoom 实例的完整博客元数据迁移。
- 在公开站点增加知识库目录导航和面包屑。

## 15. 风险与关键决策

| 风险/问题 | 首期决策 |
| --- | --- |
| 树表与博客表重复正文 | 不重复，正文只读写 `blog.content` |
| 删除树导致历史博客丢失 | DOC 与目录均支持删除，但必须二次确认并明确提示会级联删除对应博客 |
| 导入覆盖人工编辑内容 | 首期不提供覆盖，仅跳过或重命名 |
| ZIP 路径穿越/炸弹 | 预检、规范化和多维度硬限制 |
| 博客必填属性无法由 Markdown 得到 | 服务端自动创建/复用“知识库”分类并使用受控默认值，不增加导入表单负担 |
| 大 ZIP 长事务 | 首期限量并放入异步任务；分批提交后续实现 |
| 应用重启导致进度丢失 | 首期明确提示任务过期或服务重启；后续改为持久化任务 |
| 一篇博客多处出现 | 首期唯一挂载，后续有明确场景再改中间表 |
| 节点重命名是否改文章标题 | 默认不改，显式勾选才同步标题 |

## 16. 首期验收标准

满足以下条件即可判定本需求首期落地：

1. 不修改 `blog` 表结构和既有博客数据，即可通过新表组织完整目录树。
2. 管理后台存在独立知识库页面，可完成目录和文档节点的基本管理。
3. 任意合法的纯 Markdown 文件夹 ZIP 可经预检后导入，文件夹和文档均生成节点，文档正文写入关联博客。
4. 当前知识库可导出为保持目录层级的纯 Markdown ZIP。
5. 导出 ZIP 可再次导入到空知识库，目录、节点名称和 Markdown 正文一致。
6. 导入具备路径穿越、ZIP 炸弹、非法编码和冲突防护。
7. 删除文档或目录前有明确的级联范围提示，递归删除与失败导入都具备事务一致性，不留下半成品数据。
8. ZIP 经预检通过后异步执行，管理页可持续查看导入进度、当前文件和最终结果。
