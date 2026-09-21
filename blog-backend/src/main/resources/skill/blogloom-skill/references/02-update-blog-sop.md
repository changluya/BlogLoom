# 02 修改已有博客 SOP

## 适用场景

用户想修改 BlogLoom 上**已存在**的博客：

- 用本地新版 Markdown 覆盖线上文章正文（"这篇我改过了，同步一下"）；
- 只改参数：标题、描述、分类、标签、专栏、可见性、置顶、推荐、赞赏、评论。

## 核心：复用页面「编辑博客」接口

修改统一走 `PUT /admin/blog`（页面编辑接口）：按 `id` 更新，**未传字段沿用原文**，不会误清空。

## 前置参数

| 参数 | 来源 | 说明 |
| --- | --- | --- |
| `blog_id` | 用户提供 / 上一步查询 | 目标文章 id |
| `file_path` | 覆盖正文时提供 | 本地新版 Markdown 路径 |

## 步骤

### A. 用本地 Markdown 覆盖线上正文（含元数据）

1. 确认目标文章 id（可直接用标题查询）：

```bash
python3 scripts/blogloom-skill.py --tool list_blogs --params '{"title":"文章标题"}' --pretty
```

2. 调用 `sync_local_blog` 并显式传 `blog_id`（强制走更新）：

```bash
python3 scripts/blogloom-skill.py --tool sync_local_blog \
  --params '{"file_path":"/abs/path/blog.md","blog_id":11}' --pretty
```

3. 工具会回填原 `createTime`/`views`，用文件正文与元数据更新文章，并维护分类/标签/专栏。

> 不传 `blog_id` 时 `sync_local_blog` 默认按标题匹配，命中同样走更新，因此重复执行是幂等的。

### B. 只改参数（不换正文）

| 需求 | 工具 | 示例 |
| --- | --- | --- |
| 改可见性（公开/私密/密码） | `publish_visibility` | `{"blog_id":11,"published":false}` |
| 置顶 | `update_top` | `{"blog_id":11,"top":true}` |
| 推荐 | `update_recommend` | `{"blog_id":11,"recommend":true}` |
| 改标题/描述/分类/标签/专栏 | `update_blog` | `{"blog_id":11,"title":"新标题","category":"Maven"}` |
| 改专栏关联 | `update_column_relation` | `{"blog_id":11,"columns":["Maven&Gradle"],"mode":"replace"}` |

`update_blog` 只更新传入字段，其余沿用原文；返回 `updated` 列出实际变更的字段。

## 分支与异常

| 情况 | 处理 |
| --- | --- |
| `blog_id` 不存在 | 服务端返回业务错误，提示用户确认 id |
| 只改可见性却传空 `password` | 公开/私密模式会清空密码，符合预期 |
| 更新专栏时专栏不存在 | `unresolved` 中列出，不会新建、不报错 |
| 原文无封面 | 自动补默认封面（编辑接口要求 `firstPicture` 非空） |
| 需要改知识库目录位置 | 编辑接口不改节点位置；如需移动，请删除后重新导入，或在知识库页面拖拽 |

## 输出要求

一句话结论 + 变更项，例如：

> 已更新文章 #11「Maven插件—05…」：正文与标签已同步，并设为置顶。