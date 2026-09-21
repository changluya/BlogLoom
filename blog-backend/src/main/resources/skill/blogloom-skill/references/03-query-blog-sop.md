# 03 查询 / 搜索博客 SOP

## 适用场景

- "帮我看看平台上有没有标题含 xxx 的文章"；
- "列出所有文章 / 某分类下的文章"；
- "看看有哪些分类、标签、专栏，方便我导入时对齐"。

## 前置参数

`BLOOM_BASE_URL` / `BLOOM_API_TOKEN` 来自环境变量，无需用户提供。

## 步骤

| 目标 | 工具 | 参数 |
| --- | --- | --- |
| 按标题模糊搜索文章 | `list_blogs` | `title`（模糊）, `categoryId`, `pageNum`, `pageSize` |
| 查看文章详情 | `get_blog` | `id` |
| 列出全部分类 | `list_categories` | — |
| 列出全部标签 | `list_tags` | — |
| 列出专栏树 | `list_columns` | — |

示例：

```bash
python3 scripts/blogloom-skill.py --tool list_blogs \
  --params '{"title":"Maven","page_size":20}' --pretty

python3 scripts/blogloom-skill.py --tool get_blog --params '{"id":4}' --pretty
```

## 分支与异常

| 情况 | 处理 |
| --- | --- |
| 搜索无结果 | `total=0`，如实告知并建议更换关键词或先导入 |
| 详情返回 `content=null` | 列表接口不返回正文，用 `get_blog` 取详情 |
| 鉴权失败 | 提示重新下载 Skill 刷新 token |

## 输出要求

以「结论 + 证据字段」汇报：文章用 `id` / `title` / `category`，列表用 `total` 与 `list[]`，不要编造结果。

> 建议在导入前先执行 `list_columns`，确认目标专栏是否存在，避免导入后专栏关联为空。