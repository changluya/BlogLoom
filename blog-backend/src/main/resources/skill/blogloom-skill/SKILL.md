---
name: blogloom-skill
description: "BlogLoom 本地博客同步技能。Use when: (1) 把本地 Markdown 博客新增/导入到 BlogLoom（单篇或批量，按 knowledgeBasePath 自动归入知识库目录、自动建分类标签、关联专栏），(2) 修改 BlogLoom 上已有博客的正文或参数（标题/描述/分类/标签/专栏/可见性/置顶/推荐/赞赏/评论），(3) 查询或搜索 BlogLoom 上的文章列表与详情。"
---

# BlogLoom Skill

把本地 Markdown 博客快速同步到 BlogLoom 平台：新增（导入）与修改（编辑），并支持查询文章。
本 Skill 已内置当前登录用户的 `BASE_URL` 与 `API_TOKEN`，开箱即用。

## 设计要点

- **新增/导入**：直接复用页面「导入 Markdown 到知识库」接口 —— 原生支持 `knowledgeBasePath` 目录归位、缺失目录逐级创建、标签/分类自动创建、专栏只匹配已存在、冲突策略。
- **修改**：直接复用页面「编辑博客」接口 —— 按 id 更新，未传字段沿用原文。
- 因此 Skill 不自行实现目录创建/节点搬移，行为与页面完全一致。

## 工具契约

- 先读 [`references/00-tool-contract.md`](references/00-tool-contract.md)：工具名、参数、返回结构、错误码。

## 场景路由

| 场景 | 进入 SOP |
| --- | --- |
| 本地新增博客 / 批量导入 Markdown | [`references/01-import-blog-sop.md`](references/01-import-blog-sop.md) |
| 修改已有博客内容与参数 | [`references/02-update-blog-sop.md`](references/02-update-blog-sop.md) |
| 查询 / 搜索文章、看分类与标签 | [`references/03-query-blog-sop.md`](references/03-query-blog-sop.md) |
| 契约未覆盖的临时接口调用 | [`references/99-rest-fallback-sop.md`](references/99-rest-fallback-sop.md) |

## 统一调用形式

```bash
python3 scripts/blogloom-skill.py --tool <工具名> --params '{...}' [--pretty]
```

## 环境准备

```bash
pip install requests
source ./skill.env.sh      # 载入 BASE_URL / API_TOKEN（下载包时已写入你的专属 token）
```

> 若 `skill.env.sh` 中的 token 过期，在 CMS「文章管理」右上角重新下载 Skill 即可刷新。