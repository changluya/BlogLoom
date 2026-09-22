---
title: "AI Skill 同步"
description: 下载专属 AI 技能包，用自然语言把本地 Markdown 文章同步到 BlogLoom。
---

BlogLoom 提供一份开箱即用的 **AI Skill 技能包**：在管理后台「文章管理」页右上角点击「Skill 下载」即可获得。技能包内置当前账号的站点地址与登录令牌，交给支持 Skill 的 AI 助手后，用一句自然语言就能完成本地文章与站点的同步。

## 获取与准备

1. 登录管理后台，进入「文章管理」，点击右上角 **「Skill 下载」**，得到 `blogloom-skill.zip`；
2. 解压后安装依赖并载入环境变量：

```bash
cd blogloom-skill
pip install requests
source ./skill.env.sh      # 载入 BLOOM_BASE_URL / BLOOM_API_TOKEN（下载时已注入）
```

3. 把整个 `blogloom-skill/` 目录交给支持 Skill 的 AI 助手即可。

> 令牌过期后，重新下载一次技能包即可刷新。

## 能做什么

| 能力 | 说明 |
| --- | --- |
| 新增 / 批量导入 | 把单篇或整个目录的 Markdown 导入博客，自动归入知识库目录、自动创建并关联标签与分类、匹配已存在的专栏 |
| 智能同步 | 同标题文章已存在则更新、不存在则新增，重复执行不会产生重复文章 |
| 修改已有文章 | 同步本地新版正文，或单独修改标题、描述、分类、标签、专栏、可见性、置顶与推荐 |
| 查询文章 | 按标题搜索文章、查看详情，列出全部分类、标签与专栏 |

技能包与后台「迁移本地文章」使用的是同一套导入接口，因此行为与页面操作完全一致。

## Markdown 文章格式

每篇文章就是一个普通的 `.md` 文件：**最顶部一个 JSON 代码块**声明属性，其后是正常 Markdown 正文。封面图不是元数据字段，而是在正文里用 `![coverImg](图片链接)` 显式标记。

````markdown
```json
{
  "title": "SqlParser解析器快速入门",
  "tags": "AI技术,SEO优化,内容营销",
  "category": "SqlParser",
  "articleSummary": "本文介绍 SqlParser 的快速上手方式。",
  "columns": "SqlParser, AI",
  "createTime": "2026-09-19 14:30:00",
  "updateTime": "2026-09-19 14:30:00",
  "knowledgeBasePath": "/0x05、Java后端/SqlParser"
}
```

# SqlParser解析器快速入门

![coverImg](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/cover.png)

正文内容……
````

| 部分 | 说明 |
| --- | --- |
| 元数据代码块 | 必须在文件最顶部，围栏为 ` ```json `；导入后该代码块会从正文移除 |
| 字段 | `title`、`tags`、`category`、`articleSummary`、`columns`、`createTime`、`updateTime`、`knowledgeBasePath` 均可选，缺省时有各自兜底 |
| 封面图 | 正文中 `![coverImg](url)`，只认 `coverImg` 这个标记，普通图片不会当作封面 |
| 专栏 | 只匹配数据库中已存在的专栏（含二级专栏），不会自动新建 |

## 快捷提示语

用自然语言对 AI 助手说一句即可，助手会自动选择对应流程：

| 场景 | 快捷提示语 |
| --- | --- |
| 单篇导入 | 「把 `/Users/me/blogs/maven.md` 这篇导入到博客」 |
| 批量导入目录 | 「把这个目录下的所有博客同步到平台：`/Users/me/blogs/Java`」 |
| 目录归位导入 | 「把这篇文章导入到知识库的 `SqlParser` 目录下」 |
| 同步更新 | 「`maven.md` 我改过了，把它同步更新到线上」 |
| 搜索文章 | 「帮我找一下平台上标题带 `Maven` 的文章」 |
| 改可见性 | 「把文章 11 设为私密」 |
| 置顶 / 推荐 | 「把文章 11 置顶」 |
| 换专栏 | 「把文章 11 的专栏改成 `Maven&Gradle`」 |

## 下一步

- [内容管理](/v1/zh/guide/admin/content)
- [Markdown 与内容](/v1/zh/guide/concepts/markdown)
- [集成概览](/v1/zh/integration/overview)
