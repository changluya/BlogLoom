# BlogLoom 博客专栏管理与前台展示设计方案

> 迭代版本：`release_1.0.x`
>
> 文档状态：初步设计，可作为后续开发依据
>
> 需求范围：专栏两级管理、博客多选专栏、前台用户卡片下的专栏展示

## 1. 背景与设计结论

BlogLoom 现有 `category` 表表示文章的单一分类，一篇博客只能关联一个分类；本次“专栏”更接近 CSDN 的内容合集，一篇博客可以同时收录到多个专栏，并且专栏需要名称、简介、配图、层级和人工排序。因此不能直接扩展现有 `category`，应新增独立的专栏模型。

首期设计结论如下：

1. 新增 `blog_column` 专栏表和 `blog_column_relation` 博客专栏关系表。
2. 专栏用 `parent_id` 表达层级，`parent_id=0` 表示一级专栏，首期只允许一级和二级。
3. 一篇博客可加入多个专栏；同一篇博客在同一专栏中只能出现一次。
4. 后台增加独立“专栏管理”页面，支持创建、编辑、移动、排序、显隐和删除。
5. 现有博客发布 DTO 增加 `columnIds`，新增和修改博客时在同一事务内维护关系表。
6. 前台在用户介绍卡片下方增加“TA 的专栏”，按两级树展示公开专栏及其公开博客数。
7. 专栏详情页展示专栏介绍及其博客列表；用户点击侧栏专栏后进入该页面。

本方案保留现有分类、标签、知识库能力，三者职责分别为：

| 能力 | 关系 | 用途 |
| --- | --- | --- |
| 分类 `category` | 博客对分类，多对一 | 文章的主分类和传统分类筛选 |
| 标签 `tag` | 博客与标签，多对多 | 横向关键词标记 |
| 专栏 `blog_column` | 博客与专栏，多对多，专栏有两级目录 | 系列内容聚合、运营展示 |
| 知识库 `knowledge_node` | 博客在目录树中唯一挂载 | Markdown 文件目录及导入导出 |

## 2. 需求边界

### 2.1 首期实现

- 专栏最多两级，一级专栏可包含多个二级专栏。
- 创建专栏时维护名称、简介、配图、父专栏、排序和展示状态。
- 支持修改专栏、同级排序、在合法层级间移动专栏。
- 博客编辑和发布时可以选择零个或多个专栏。
- 后台专栏列表展示层级、配图、名称、博客数、状态和操作入口。
- 前台用户卡片下按树形结构展示公开专栏及公开博客数量。
- 提供专栏详情和专栏博客分页接口。

### 2.2 首期不实现

- 不支持三级及更深层级。
- 不支持多作者各自维护一套专栏；当前项目仍按单作者 `user_id=1` 运行。
- 不支持订阅、付费专栏、专栏协作者和审核流。
- 不自动根据分类或标签创建专栏，也不迁移现有分类数据。
- 不在 `blog` 表中保存专栏 ID，避免逗号字符串及重复数据源。
- 不把二级专栏理解为博客；目录与博客仍通过关系表关联。

## 3. 数据模型

### 3.1 关系模型

```text
blog_column（专栏）
  id <---------------- parent_id（最多自关联一层）
   |
   | 1
   |
   | N
blog_column_relation
   | N
   |
   | 1
blog（博客）
```

一篇博客可同时属于“AI 领域”和“调度系统”等多个专栏；一级专栏也允许直接关联博客，不强制所有文章必须放入二级专栏。

### 3.2 专栏表

表名使用 `blog_column`，避免使用含义过于宽泛且可能与数据库关键字冲突的 `column`。

```sql
CREATE TABLE `blog_column` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '专栏ID',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父专栏ID，0表示一级专栏',
  `name` varchar(100) NOT NULL COMMENT '专栏名称',
  `description` varchar(500) NOT NULL DEFAULT '' COMMENT '专栏简介',
  `cover` varchar(500) NOT NULL DEFAULT '' COMMENT '专栏配图URL',
  `sort` int NOT NULL DEFAULT 0 COMMENT '同级排序值，越小越靠前',
  `is_published` bit(1) NOT NULL DEFAULT b'1' COMMENT '前台是否展示',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_sort` (`parent_id`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='博客专栏';
```

字段说明：

- `parent_id=0`：一级专栏。
- `parent_id>0`：二级专栏，父记录必须存在且父记录自身必须是一级专栏。
- `cover` 保存站内相对 URL 或完整 URL，与现有图片字段的使用方式保持一致。
- `sort` 只在同一 `parent_id` 范围内有意义，列表固定按 `sort ASC, id ASC` 返回。
- `is_published=false` 时该专栏不出现在前台；其子专栏和关联关系保留。
- 首期个人博客无需增加 `user_id`。若后续升级为多作者平台，再增加所有者字段并将名称校验、查询和权限收敛到用户维度。

### 3.3 博客专栏关系表

```sql
CREATE TABLE `blog_column_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关系ID',
  `column_id` bigint NOT NULL COMMENT '专栏ID',
  `blog_id` bigint NOT NULL COMMENT '博客ID',
  `sort` int NOT NULL DEFAULT 0 COMMENT '博客在该专栏中的排序值',
  `create_time` datetime NOT NULL COMMENT '加入专栏时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='博客与专栏关系';
```

关系表初版只保留独立主键 `id`，暂不创建联合唯一索引和普通索引，便于保持表结构精简。`column_id + blog_id` 的重复关联由 Service 层对 `columnIds` 去重，并在写入前校验保证；按博客删除关系、编辑回显等查询先直接使用现有字段条件。后续数据规模增大并确认慢查询后，再根据实际执行计划补充索引。

当前项目未统一使用数据库外键，本方案也不增加外键；引用完整性由事务和 Service 层保证。

### 3.4 数据约束

Service 层必须统一校验：

- 专栏名称去除首尾空格后长度为 `1~100`，简介最长 500 字符。
- 同一个父节点下专栏名称不能重复；一级和不同父节点下可同名。
- 二级专栏不能再创建子专栏。
- 移动后层级不能超过二级。
- 移动一级专栏到另一个一级专栏下时，若它已有子专栏，则拒绝移动，否则将产生三级结构。
- 专栏不能移动到自身，也不能移动到自己的子节点。
- `columnIds` 去重后再写入，所有 ID 必须存在；后台保存时可选择隐藏专栏，公开页面只消费公开专栏。

## 4. 后台专栏管理

### 4.1 页面入口与布局

在 `blog-cms-ui/src/router/index.js` 的“博客管理”下新增“专栏管理”：

```text
/blog/column/list
  -> @/views/blog/column/ColumnManage.vue
```

建议页面结构参考 CSDN，但首期使用树形表格更适合两级数据：

```text
+--------------------------------------------------------------------------------+
| 专栏管理                                      [新建一级专栏]                    |
+--------------------------------------------------------------------------------+
| 配图 | 专栏名称/简介       | 层级 | 博客数 | 排序 | 前台展示 | 操作             |
| 图   | ▾ AI领域           | 一级 | 31     | 10   | 开启     | 编辑 新建子栏 删除 |
| 图   |   └ LLM应用        | 二级 | 12     | 20   | 开启     | 编辑 移动 删除     |
| 图   | 技术设计方案        | 一级 | 8      | 30   | 开启     | 编辑 新建子栏 删除 |
+--------------------------------------------------------------------------------+
```

交互细节：

- 创建/编辑使用抽屉或弹窗，字段为名称、简介、配图、父专栏、排序、前台展示。
- 父专栏选择框只列出一级专栏，并提供“无（一级专栏）”。
- 上传成功立即预览图片；取消弹窗时清理未归档的临时文件可作为后续优化。
- 移动可采用“移动到”弹窗；拖拽可在第二阶段补充。首期若直接做拖拽，只允许合法落点。
- 博客数按“直接关联数”展示，不把子专栏文章重复累计到父专栏。可另增 `totalBlogCount` 展示含子栏汇总数，但字段含义必须明确。

### 4.2 创建与编辑表单

请求示例：

```json
{
  "name": "AI领域",
  "description": "记录大模型、Agent 与 AI 工程实践",
  "cover": "/static/blogColumn/12/550e8400-e29b-41d4-a716-446655440000.png",
  "parentId": 0,
  "sort": 10,
  "published": true
}
```

创建时若前端不传 `sort`，后端使用同级最大排序值加 10。不要依赖前端计算最终排序，避免并发或旧页面数据导致重复覆盖。

### 4.3 移动规则

移动接口只接收目标父节点和目标顺序，不允许前端直接更新任意实体字段：

```json
{
  "targetParentId": 0,
  "targetSort": 30
}
```

核心校验伪代码：

```java
public void checkMove(Column source, Long targetParentId) {
    if (source.getId().equals(targetParentId)) {
        throw new IllegalArgumentException("A column cannot be moved under itself");
    }
    if (targetParentId == 0L) {
        return;
    }
    Column parent = requireColumn(targetParentId);
    if (parent.getParentId() != 0L) {
        throw new IllegalArgumentException("Only two levels of columns are supported");
    }
    if (hasChildren(source.getId())) {
        throw new IllegalArgumentException("A column with children cannot be moved to level two");
    }
}
```

移动成功后可对原父节点和新父节点的 `sort` 做紧凑化处理；首期也可接受排序值不连续，只要查询排序稳定。

### 4.4 删除规则

推荐采用“有引用则拒绝”的保守策略：

- 有子专栏的一级专栏不能删除。
- 有博客关联的专栏不能删除，页面提示先移除博客或批量迁移。
- 无子专栏且无博客关联时才物理删除。

这种方式与现有分类删除行为一致，也避免误操作导致整批博客失去专栏。删除博客时只删除关系，不删除专栏。

## 5. 博客发布与编辑改造

### 5.1 DTO 调整

在 `com.changlu.blogloom.model.dto.Blog` 增加：

```java
private List<Long> columnIds = new ArrayList<>();
```

后台获取博客详情时，同时回传当前已选择的专栏 ID。实体 `com.changlu.blogloom.entity.Blog` 是否增加专栏对象不是首期必需，建议使用专门 VO，避免持久化实体不断承载页面字段。

推荐新增：

```java
public class BlogEditVo {
    private Blog blog;
    private List<Long> columnIds;
}
```

考虑现有 `GET /admin/blog?id=...` 已直接返回 `Blog`，为了最小改动，首期也可直接给 DTO/实体补 `columnIds` 并在 Service 查询后填充；但长期推荐拆分 `BlogEditVo`。

### 5.2 前端编辑器

在 `blog-cms-ui/src/views/blog/blog/WriteBlog.vue` 的分类、标签区域增加“所属专栏”多选树：

```html
<el-form-item label="所属专栏">
  <el-cascader
    v-model="form.columnIds"
    :options="columnOptions"
    :props="{ multiple: true, emitPath: false, checkStrictly: true }"
    clearable
    collapse-tags
    placeholder="可选择多个专栏" />
</el-form-item>
```

`checkStrictly: true` 表示一级和二级专栏均可选；`emitPath: false` 确保提交的是专栏 ID 数组而不是完整路径数组。

发布请求示例：

```json
{
  "title": "Spring Boot SSE 实战",
  "cate": 3,
  "tagList": [2, 8],
  "columnIds": [11, 15],
  "content": "...",
  "description": "...",
  "firstPicture": "...",
  "published": true
}
```

`columnIds` 可省略或传空数组，表示博客不属于任何专栏。

### 5.3 保存核心流程

现有 Controller 的 `getResult` 同时处理博客、分类和标签，专栏落地时应优先把完整保存编排下沉到 Service，确保博客、标签、专栏关系处于同一事务。至少应保证以下流程：

```text
校验博客基础字段
  -> 解析/校验分类和标签
  -> columnIds 去重并批量校验专栏存在
  -> 新增或更新 blog
  -> 重建 blog_tag
  -> 重建 blog_column_relation
  -> 归档博客资源
  -> 清理首页、专栏树、专栏详情缓存
  -> 事务提交
```

关系更新采用“全量替换”，逻辑简单且适合单篇博客的少量专栏：

```java
@Transactional(rollbackFor = Exception.class)
public void replaceBlogColumns(Long blogId, List<Long> columnIds) {
    columnRelationMapper.deleteByBlogId(blogId);
    List<Long> distinctIds = columnIds == null
            ? Collections.emptyList()
            : columnIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    if (distinctIds.isEmpty()) {
        return;
    }
    if (columnMapper.countByIds(distinctIds) != distinctIds.size()) {
        throw new NotFoundException("One or more columns do not exist");
    }
    columnRelationMapper.batchInsert(blogId, distinctIds, new Date());
}
```

更新时先删后插必须位于同一事务，任何插入失败都回滚到更新前关系。

### 5.4 删除博客

在 `BlogServiceImpl.deleteBlogById` 内，在删除 `blog` 前调用：

```java
columnRelationMapper.deleteByBlogId(id);
```

 run 不要只在 Controller 中清理关系，否则知识库导入、其他内部服务或未来批处理直接调用 Service 时会残留脏数据。

## 6. 接口设计

接口延续项目当前 `/admin` 和公开根路径风格，并统一使用 `Result` 返回结构。

### 6.1 后台管理接口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/admin/columns/tree` | 获取完整专栏管理树，包含隐藏专栏和直接博客数 |
| `GET` | `/admin/columns/options` | 获取博客编辑器专栏选项树 |
| `GET` | `/admin/column?id={id}` | 获取专栏详情 |
| `POST` | `/admin/column` | 创建专栏 |
| `PUT` | `/admin/column` | 修改名称、简介、配图、排序和展示状态 |
| `PUT` | `/admin/column/{id}/move` | 移动专栏 |
| `PUT` | `/admin/columns/reorder` | 批量更新同级排序 |
| `DELETE` | `/admin/column?id={id}` | 删除空专栏 |
| `POST` | `/admin/column/{id}/cover` | 上传专栏配图并归档到该专栏目录 |

管理树返回示例：

```json
[
  {
    "id": 11,
    "parentId": 0,
    "name": "AI领域",
    "description": "记录 AI 工程实践",
    "cover": "/static/blogColumn/11/550e8400-e29b-41d4-a716-446655440000.png",
    "sort": 10,
    "published": true,
    "blogCount": 31,
    "children": [
      {
        "id": 15,
        "parentId": 11,
        "name": "Agent应用",
        "description": "Agent 实战",
        "cover": "/static/blogColumn/15/7d422728-f50b-4d88-9572-73d42f393758.webp",
        "sort": 10,
        "published": true,
        "blogCount": 8,
        "children": []
      }
    ]
  }
]
```

### 6.2 博客接口调整

| 接口 | 改动 |
| --- | --- |
| `POST /admin/blog` | 请求增加可选 `columnIds` |
| `PUT /admin/blog` | 请求增加可选 `columnIds`，保存时全量替换 |
| `GET /admin/blog?id={id}` | 响应增加 `columnIds`，用于编辑回显 |
| `DELETE /admin/blog?id={id}` | Service 内同步删除专栏关系 |

为兼容旧管理端，后端将未传 `columnIds` 和显式空数组都按空集合处理。若需要兼容“旧客户端编辑不应清空关系”，则 DTO 必须区分 `null` 与空数组：`null` 表示不更新，`[]` 表示清空。首期前后端同步发布时建议采用更简单的“未传即空集合”规则。

### 6.3 前台公开接口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/columns/tree` | 获取用户卡片专栏树，只返回公开专栏及公开博客数 |
| `GET` | `/column/{id}` | 获取公开专栏详情 |
| `GET` | `/column/{id}/blogs?pageNum=1` | 分页获取该专栏直接关联的公开博客 |

前台树建议只返回展示所需字段。每个专栏节点必须同时返回 `cover` 和 `name`，前端分别作为专栏头像/Logo 与专栏标题展示：

```json
[
  {
    "id": 11,
    "name": "AI领域",
    "cover": "/static/blogColumn/11/550e8400-e29b-41d4-a716-446655440000.png",
    "blogCount": 31,
    "children": [
      {
        "id": 15,
        "name": "Agent应用",
        "cover": "/static/blogColumn/15/7d422728-f50b-4d88-9572-73d42f393758.webp",
        "blogCount": 8
      }
    ]
  }
]
```

公开接口查询必须同时满足：

- 专栏 `is_published=1`。
- 如果是二级专栏，其父专栏也必须公开。
- 博客 `is_published=1`。
- 博客数使用 `COUNT(DISTINCT b.id)`，避免异常重复关系放大计数。

## 7. 前台用户卡片与专栏详情

### 7.1 展示位置

当前用户信息卡片为 `blog-view-ui/src/components/sidebar/Introduction.vue`。建议不要继续把所有逻辑堆入该文件，新增：

```text
blog-view-ui/src/components/sidebar/UserColumns.vue
blog-view-ui/src/api/column.js
blog-view-ui/src/views/column/Column.vue
```

在 `Introduction.vue` 个人资料主体之后插入 `<UserColumns />`。每一行均展示“专栏头像/Logo + 专栏标题 + 博客篇数”，二级专栏通过缩进和树线表达所属关系，效果参考“TA 的专栏”：

```text
+------------------------------------+
| 用户头像 / 昵称 / 统计             |
+------------------------------------+
| TA 的专栏                          |
| [Logo] ▾ AI领域               31篇 |
|        ├ [Logo] Agent应用       8篇 |
|        └ [Logo] 模型训练        5篇 |
| [Logo] ▸ 技术设计方案           8篇 |
+------------------------------------+
```

展示细节：

- `cover` 作为专栏头像/Logo，建议一级专栏显示 `32px × 32px`，二级专栏显示 `24px × 24px`，使用圆角方形并保持 `object-fit: cover`。
- `name` 作为专栏标题，单行展示；超长时省略，悬停通过 `title` 查看完整名称。
- `blogCount` 右对齐显示为“`${blogCount}篇`”。
- `cover` 为空或图片加载失败时，展示 BlogLoom 内置的专栏默认 Logo，不能留下破图。
- 整行均可点击进入专栏详情，展开/折叠按钮单独阻止冒泡，避免展开时误跳转。
- 一级专栏默认展开还是折叠可由 UI 决定；建议默认展开前 3 个，其余折叠，避免专栏过多导致侧栏过长。移动端可默认全部折叠。

### 7.2 点击行为

- 点击专栏名称跳转 `/column/{id}`。
- 一级专栏自身有直接关联文章时，详情页默认只展示直接关联文章。
- 页面可提供“包含子专栏文章”开关作为后续能力；首期不要隐式汇总，以免同一文章同时关联父子专栏时重复。
- 无公开博客的专栏：后台仍展示，前台默认不展示；如后续有“空专栏预告”需求再增加配置。

### 7.3 专栏详情页

详情页顶部展示配图、名称、简介、文章数；下方复用现有 `BlogList` 和分页组件。新增前台路由：

```text
/column/:id
  -> @/views/column/Column.vue
```

专栏失效、隐藏或不存在时返回 404 语义，不向前台泄露隐藏专栏信息。

### 7.4 数据加载与缓存

有两种方式：

1. 在现有 `GET /site` 响应中增加 `columnTree`，首页首次加载请求更少。
2. 独立请求 `GET /columns/tree`，组件职责清晰，可单独缓存和刷新。

首期推荐独立接口。专栏树变化频率低，可在后端 Redis 缓存完整公开树；创建、编辑、移动、显隐、删除专栏，以及博客发布状态或专栏关系变化时清理该缓存。

建议新增缓存键：

```java
public static final String COLUMN_PUBLIC_TREE = "columnPublicTree";
```

专栏详情的博客分页首期可不缓存，沿用数据库分页和现有博客列表处理逻辑。

## 8. 图片上传设计

当前已有 `POST /admin/blog/resources` 和 `LocalResourceStorageService`，可复用其文件类型、大小、路径穿越防护等底层能力，但专栏配图不应归档到某一篇博客目录。

专栏配图固定使用独立的 `blogColumn` 目录，与博客正文资源目录隔离。推荐路径：

```text
磁盘目录：${user.dir}/upload/blogColumn/{columnId}/{uuid}.{ext}
公开 URL：/static/blogColumn/{columnId}/{uuid}.{ext}
```

其中 `{columnId}` 为专栏主键，`{uuid}` 为服务端生成的唯一文件名，`{ext}` 保留校验后的真实图片后缀。数据库 `blog_column.cover` 字段只保存可访问 URL，不保存服务器绝对路径。

由于最终目录依赖专栏 ID，创建流程应先插入专栏获得自增 ID，再将图片写入 `${user.dir}/upload/blogColumn/{id}/`，最后更新 `cover` 字段。落地时新增 `ColumnCoverStorageService`，复用公共上传校验与路径解析工具，并在静态资源映射中把 `/static/blogColumn/**` 映射到 `${user.dir}/upload/blogColumn/`。

更新配图时，新图片仍写入当前专栏 ID 对应目录，可以保留任意合法文件名，但推荐统一使用 UUID，避免浏览器缓存和同名覆盖。新图片和数据库字段更新成功后，再异步或尽力删除旧文件。数据库事务不能自动回滚文件系统，因此不应先删除旧文件，避免事务失败后数据库仍指向已经不存在的资源。

接口限制建议：

- 只允许 `jpg/jpeg/png/webp`。
- 单文件最大 5 MB，最终以统一上传配置为准。
- 服务端根据文件内容检测 MIME，不只相信扩展名。
- 文件名由服务端生成，不使用用户原始文件名作为磁盘路径。

## 9. 后端代码结构

建议专栏独立成模块目录，避免继续扩大通用 controller/service 包：

```text
blog-backend/src/main/java/com/changlu/blogloom/module/column/
├── api/
│   ├── ColumnAdminController.java
│   └── ColumnController.java
├── dao/
│   ├── BlogColumnMapper.java
│   └── BlogColumnRelationMapper.java
├── domain/
│   ├── dto/ColumnSaveReq.java
│   ├── dto/ColumnMoveReq.java
│   ├── dto/ColumnReorderReq.java
│   ├── entity/BlogColumn.java
│   └── vo/ColumnTreeVo.java
└── service/
    └── BlogColumnService.java

blog-backend/src/main/resources/mapper/
├── BlogColumnMapper.xml
└── BlogColumnRelationMapper.xml
```

核心职责：

- `ColumnAdminController`：参数接收、管理端路由、操作日志。
- `ColumnController`：公开树、公开详情和文章分页。
- `BlogColumnService`：层级、移动、删除、发布状态、关系维护和缓存失效。
- `BlogColumnMapper`：专栏 CRUD、树查询、公开计数。
- `BlogColumnRelationMapper`：按博客/专栏查询、删除及批量插入。
- `BlogServiceImpl`：在博客保存、更新、删除事务中调用专栏关系服务。

### 9.1 Mapper 核心方法

```java
public interface BlogColumnMapper {
    BlogColumn selectById(Long id);
    List<ColumnTreeVo> selectAdminTreeRows();
    List<ColumnTreeVo> selectPublicTreeRows();
    int insert(BlogColumn column);
    int update(BlogColumn column);
    int updateParentAndSort(Long id, Long parentId, Integer sort);
    int deleteById(Long id);
    int countChildren(Long parentId);
    int countByParentAndName(Long parentId, String name, Long excludeId);
}

public interface BlogColumnRelationMapper {
    List<Long> selectColumnIdsByBlogId(Long blogId);
    int countByColumnId(Long columnId);
    int deleteByBlogId(Long blogId);
    int batchInsert(Long blogId, List<Long> columnIds, Date createTime);
}
```

### 9.2 树组装

由于只有两级，一次查询全部行后在 Java 内组装即可，无需递归 SQL：

```java
Map<Long, ColumnTreeVo> roots = rows.stream()
        .filter(item -> item.getParentId() == 0L)
        .collect(Collectors.toMap(ColumnTreeVo::getId, Function.identity(), (a, b) -> a,
                LinkedHashMap::new));

for (ColumnTreeVo item : rows) {
    if (item.getParentId() != 0L) {
        ColumnTreeVo parent = roots.get(item.getParentId());
        if (parent != null) {
            parent.getChildren().add(item);
        }
    }
}
return new ArrayList<>(roots.values());
```

查询 SQL 必须先按 `parent_id, sort, id` 排序，或者组装完成后分别排序，保证前后端刷新后顺序稳定。

## 10. 核心业务时序

### 10.1 新建专栏

```text
CMS 提交表单
  -> 校验字段与父专栏
  -> 校验同级重名
  -> 计算默认 sort
  -> 插入 blog_column
  -> 将配图保存到 ${user.dir}/upload/blogColumn/{id}/{uuid}.{ext}
  -> 更新 cover
  -> 清理公开专栏树缓存
  -> 返回新专栏 ID
```

### 10.2 发布/更新博客

```text
CMS 提交 Blog + columnIds
  -> 校验 columnIds
  -> 保存 blog
  -> 更新标签关系
  -> 删除该博客旧专栏关系
  -> 批量写入新专栏关系
  -> 清理博客及专栏缓存
  -> 提交事务
```

### 10.3 前台展示

```text
Index.vue 渲染 Introduction.vue
  -> UserColumns.vue 请求 GET /columns/tree
  -> 后端读缓存；未命中则查询公开专栏 + 公开博客数
  -> 组装两级树并缓存
  -> 用户点击专栏
  -> 跳转 /column/:id
  -> 分页请求该专栏公开博客
```

## 11. 异常与一致性处理

- 所有专栏 ID 均使用 `Long`，前端不得假定 JavaScript 数值能安全承载任意 64 位整数；若 ID 将来可能超过安全整数，接口统一输出字符串。
- 博客关系替换、博客删除关系清理必须进入同一事务。
- 管理树查询使用左连接统计博客数，不能因专栏没有博客而丢失专栏。
- 公开树统计只计算公开博客；管理树统计可统计全部博客，并明确字段名为 `blogCount`。
- 修改博客公开状态也要清理专栏公开树缓存，否则篇数会短暂不一致。
- 关系表若出现孤儿数据，公开查询通过连接自然过滤；后台可增加数据巡检 SQL，但不能依赖查询过滤代替写入校验。
- 专栏同级重名和博客专栏重复关联在首期均由 Service 层校验与去重保证，数据库表只保留主键约束。若后续出现并发写入或数据规模问题，再结合实际执行计划评估联合唯一索引和普通索引。

## 12. SQL 与上线步骤

建议在 `sql/increment/1.0/` 新增时间戳增量脚本，同时同步更新全量初始化脚本 `sql/increment/init.sql`。

上线顺序：

1. 执行增量 SQL，新建两张表；此步骤不改历史博客数据。
2. 发布后端，公开接口在无专栏时返回空数组。
3. 发布 CMS，创建专栏并在博客编辑页维护关联。
4. 发布前台用户卡片和专栏详情页。
5. 检查缓存清理、隐藏专栏、私密博客和空专栏等边界。

回滚时先回滚前端，再回滚后端。两张新表不影响原有博客、分类和标签功能，可暂时保留数据；确认不再恢复功能后再人工备份并删除表，不应在普通回滚脚本中直接销毁数据。

## 13. 测试清单

### 13.1 专栏管理

- 创建一级和二级专栏成功。
- 在二级专栏下创建子专栏被拒绝。
- 同级同名被拒绝，不同父级同名允许。
- 无子项一级专栏可移动为二级，有子项一级专栏移动为二级被拒绝。
- 二级专栏可移动到另一个一级专栏或根节点。
- 专栏不能移动到自身或子节点。
- 有子专栏或博客关系时删除被拒绝。
- 修改排序、配图和展示状态后刷新仍正确。

### 13.2 博客关联

- 新博客不选专栏、选择一个、选择多个均保存成功。
- 编辑回显 `columnIds` 正确。
- 更新时增加、移除、清空专栏关系正确。
- 重复 `columnIds` 不产生重复关系。
- 传入不存在的专栏 ID 时整个博客保存回滚。
- 删除博客后关系表无残留，专栏本身仍存在。

### 13.3 前台展示

- 用户卡片只展示公开专栏。
- 隐藏父专栏时，其二级专栏也不展示。
- 篇数只统计公开博客。
- 空专栏按约定不展示。
- 点击一级、二级专栏均可进入对应详情页。
- 私密、草稿博客不会出现在专栏详情列表。
- 移动端折叠、长名称省略和大量专栏滚动体验正常。

## 14. 推荐实施拆分

### 第一阶段：后端与数据

- 新增两张表、实体、Mapper、Service。
- 完成管理接口、公开树接口和专栏博客分页接口。
- 改造博客新增、更新、详情、删除链路。
- 增加事务和层级规则单元测试。

### 第二阶段：CMS

- 新增专栏管理树形表格及创建、编辑、移动、显隐、删除能力。
- 博客编辑器新增专栏多选和回显。
- 接入配图上传。

### 第三阶段：博客前台

- 用户信息卡片下新增 `UserColumns`。
- 增加专栏详情路由和博客分页页面。
- 完成公开树缓存及变更后的缓存失效验证。

## 15. 最终建议

本次功能应保持“分类不动、专栏独立、博客多选、前台树形展示”的边界。两级限制只属于首期业务校验，数据库继续使用通用 `parent_id`，以后若开放更多层级不需要迁表；但当前所有写接口必须严格阻止三级数据。

第一版优先完成可靠的弹窗移动和数字排序，不必一开始就做复杂拖拽。待 CRUD、博客多选和前台展示稳定后，再补充拖拽排序、父子专栏文章聚合、专栏文章单独置顶等增强能力。

## 16. 本次实际落地说明

### 16.1 已落地范围

本次已按上述方案完成第一版端到端实现：

- 新增 `blog_column`、`blog_column_relation` 表及 `release_1.0.x` 增量脚本。
- 关系表严格只保留 `PRIMARY KEY (id)`，重复关系由博客保存逻辑对 `columnIds` 去重保证。
- 新增独立 `module/column` 后端模块，包含管理接口、公开接口、两级树组装、移动与删除校验。
- 博客新增、更新、详情、删除链路已接入专栏关系；编辑详情返回 `columnIds`。
- CMS 已新增“专栏管理”，支持创建一级/二级专栏、编辑、移动、显隐、删除空专栏和上传 Logo。
- 博客发布弹窗已增加多选专栏，并支持编辑回显。
- 博客前台用户卡片已增加“TA 的专栏”，展示 Logo、标题和公开博客篇数。
- 新增专栏详情页，复用现有博客卡片和分页组件。

### 16.2 实际核心代码位置

```text
后端：
blog-backend/src/main/java/com/changlu/blogloom/module/column/
blog-backend/src/main/java/com/changlu/blogloom/service/impl/BlogServiceImpl.java
blog-backend/src/main/resources/mapper/BlogMapper.xml

CMS：
blog-cms-ui/src/views/blog/column/ColumnManage.vue
blog-cms-ui/src/views/blog/blog/WriteBlog.vue
blog-cms-ui/src/api/column.js

博客前台：
blog-view-ui/src/components/sidebar/UserColumns.vue
blog-view-ui/src/views/column/Column.vue
blog-view-ui/src/api/column.js

SQL：
sql/increment/1.0/20260917000000_v1.0.x.sql
sql/increment/init.sql
```

### 16.3 实际图片保存流程

创建专栏时先保存专栏元数据获得自增 ID，随后调用：

```http
POST /admin/column/{id}/cover
Content-Type: multipart/form-data
```

服务端完成格式和 5 MB 大小校验，以 UUID 生成文件名，并保存为：

```text
${user.dir}/upload/blogColumn/{id}/{uuid}.{ext}
```

接口返回的公开 URL 写回 `blog_column.cover`。当前静态资源处理已统一映射 `/static/**` 到 `${user.dir}/upload/`，因此不需要额外增加单独的 MVC 映射。

### 16.4 实际事务边界

`BlogServiceImpl.saveBlog` 和 `updateBlog` 在博客主记录成功后，对 `columnIds` 去重、逐个校验专栏存在，然后删除旧关系并批量写入新关系。整个过程位于博客 Service 的事务中；任一专栏无效或关系写入失败时，博客和专栏关系一起回滚。

`deleteBlogById` 在删除博客前清理 `blog_column_relation`，避免其他内部调用绕过 Controller 后遗留孤儿关系。

### 16.5 本次验证结果

- `mvn clean package -DskipTests`：通过。
- `blog-cms-ui` 执行 `npm run build`：通过。
- `blog-view-ui` 执行 `npm run build`：通过。
- 后端现有 Spring 上下文测试需要外置数据库配置；无数据源环境中无法启动，属于环境依赖，不是本次代码编译失败。
