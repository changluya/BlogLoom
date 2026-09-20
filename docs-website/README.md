# BlogLoom 官网与文档库

BlogLoom 开源项目官网，基于 **Mintlify + Markdown/MDX** 构建，采用「多语言（en/zh）+ 配置驱动导航」的标准开源官网结构。

> 方法论参考同目录的《开源项目官网结构与文档库拆解.md》。

---

## 1. 快速开始

```bash
npm ci
npm run dev        # 打开 http://localhost:3000
```

提交前质量门禁：

```bash
npm test             # 校验脚本单元测试
npm run check        # 导航 / title / 页面存在性
npm run validate     # check + Mintlify 严格构建校验
npm run broken-links # 坏链检测
```

---

## 2. 目录结构

```text
docs-website/
├── docs.json          # 单一事实来源：导航 / 版本 / 语言 / 品牌 / 重定向
├── package.json       # 脚本：dev / check / test / validate / broken-links
├── style.css          # 首页 Landing 自定义样式（.blogloom-landing 作用域）
├── landing.js         # 首页交互（Tab 切换 / 复制按钮）
├── logo.svg           # 品牌 Logo（矢量）
├── blogloom-logo.png  # 品牌 Logo（位图，与仓库根 assets/ 同源）
├── site-favicon.png   # 站点图标 / favicon
├── .mintignore        # 忽略文件
├── scripts/
│   ├── check-docs.mjs # 校验：每个导航路由有文件、每页有 title、无孤儿页
│   └── tests/         # 校验逻辑单元测试
├── v1/                # 当前版本 1.x
│   ├── en/            #   英文（默认语言）
│   └── zh/            #   中文（与 en 平行对齐）
└── 开源项目官网结构与文档库拆解.md   # 官网建设方法论
```

> CI 工作流位于仓库根目录 `.github/workflows/website.yml`，会在 `docs-website/` 下执行 `npm test`、`npm run validate` 与 `npm run broken-links`。

**导航模型**（五层树，全部由 `docs.json` 驱动）：

```text
language (en 默认 / zh) → version (v1) → tab (Home/Guide/Deploy/Integration/Blog/Community)
→ group (侧边栏) → page (.md 路由)
```

---

## 3. 栏目结构

| Tab | 内容定位 | 对应模块 |
| --- | --- | --- |
| **Home / 首页** | 项目介绍、Landing 页 | 首页 |
| **Guide / 指南** | 快速开始、核心概念、系统模块、博客前台、管理后台、参考 | 快速开始 + 指南 + 参考 |
| **Deploy / 部署与运维** | 环境、安装部署、配置、安全、运维与故障排查 | 使用手册 |
| **Integration / 集成** | 图床存储、通知、数据存储、工具 | 集成/生态 |
| **Blog / 博客** | 版本发布与使用场景 | 博客 |
| **Community / 社区** | 参与贡献、贡献者、路线图 | 社区 |

每个文档页遵循「概览 → 上手 → 进阶 → 下一步」的递进结构，并在页尾通过链接延续阅读路径。

---

## 4. 品牌定制

使用 Mintlify 的 **`mint` 主题**，全局品牌全部由 `docs.json` 配置驱动：

| 字段 | 说明 |
| --- | --- |
| `name` | 站点 / 项目名 `BlogLoom` |
| `colors` | 品牌色 primary `#5B5BD6` / light / dark |
| `logo` / `favicon` | 品牌 Logo |
| `topbarCtaButton` | 顶栏 CTA（Star on GitHub） |
| `anchors` / `navbar` / `footer` | GitHub 与页脚社交链接 |

三层定制方式：

1. **全局品牌**：改 `docs.json` 品牌字段。
2. **内容交互**：正文使用 Mintlify 内置组件 `<CardGroup>`/`<Card>`/`<Accordion>`/`<Note>`/`<Tabs>` 等。
3. **Landing 定制**：`v1/{en,zh}/intro.md` + `style.css`（`.blogloom-landing` 作用域）+ `landing.js`。

---

## 5. 扩展清单

| 目标 | 改哪里 |
| --- | --- |
| 增删栏目 / Tab | `docs.json` 的 `navigation` |
| 新增页面 | 建 `.md` + 加路由到 `docs.json` |
| 改品牌 / 颜色 / Logo | `docs.json` 的 `name`/`colors`/`logo` |
| 加语言 | `docs.json` `languages` + 建 `v1/{lang}/` 目录 |
| 加版本 | `docs.json` `versions` + 建目录（当前为单版本 `v1`） |
| 定制首页 | `intro.md` + `style.css` + `landing.js` |
| 重定向旧链接 | `docs.json` 的 `redirects` |

> 约束：`docs.json` 里的每个路由必须有对应文件，每个文件必须在导航中 —— `npm run check` 强制校验。

---

## 6. 校验与发布

`check-docs.mjs` 核心校验点：

- `docs.json` 中每个导航路由都有对应文件；
- 每个 `.md` 文件都在导航中（无孤儿页）；
- 每个页面有 `title` front-matter。

已内置 CI 工作流：仓库根目录 [`.github/workflows/website.yml`](../.github/workflows/website.yml)，在 `docs-website/` 目录下执行：

```yaml
name: Validate Docs
on:
  pull_request:
    paths: ['docs-website/**']
  push:
    branches: [master, main]
defaults:
  run:
    working-directory: docs-website
jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: npm
          cache-dependency-path: docs-website/package-lock.json
      - run: npm ci
      - run: npm test
      - run: npm run validate
      - run: npm run broken-links
```

发布托管：在 Mintlify 后台连接 GitHub，选择仓库 / 分支 / `docs-website` 目录，配置自定义域名后自动发布。

---

## 7. 搜索与深色模式

- **站内搜索**：Mintlify 内置全文搜索，本地 `mint dev` 需先 `mint login`；线上托管默认开启。
- **深色模式**：首页 Landing 已适配 `.dark` class，自动切换。
- 注意索引排除规则：`hidden: true`、`noindex`、`searchable: false` 或未列入导航的页面会被排除。
