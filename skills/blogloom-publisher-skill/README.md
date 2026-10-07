# BlogLoom Publisher Skill

把符合「[标准生成发布输出博客 SOP](../../codes/标准生成发布输出博客sop.md)」的本地 Markdown 博客，用 **Playwright 主操作 + CDP 底层增强** 模拟真人操作，分发到第三方内容渠道。

已对接渠道：**csdn**、**gzh**（微信公众号）。

## 目录结构

```text
blogloom-publisher-skill/
├── SKILL.md                       # 场景路由（按渠道）
├── references/
│   ├── 00-tool-contract.md        # 通用工具契约：CLI / 参数 / 返回 / 错误码 / 环境变量
│   └── channels/
│       ├── csdn/sop.md            # CSDN 渠道 SOP
│       └── gzh/sop.md             # 公众号渠道 SOP（含 md.doocs 正文样式化）
├── scripts/
│   ├── publisher.js               # CLI 入口（解析 → 路由 → 输出）
│   ├── lib/                       # 通用层（不含任何渠道业务）
│   │   ├── args.js                # 参数解析（通用透传渠道专属参数）/ 帮助文本
│   │   ├── cli-io.js              # stdout(JSON) / stderr(日志)
│   │   ├── payload.js             # SOP 元数据 → 通用发布参数、删除目标解析
│   │   ├── runner.js              # 浏览器会话与登录编排
│   │   ├── actions.js             # 各 action 实现（checkLogin/login/publish/delete/test）
│   │   ├── session.js             # Playwright 启动 / CDP 连接
│   │   ├── channel.js             # 渠道加载（按目录）
│   │   ├── editor.js              # 正文写入策略（paste/cdp/keyboard）
│   │   └── markdown.js            # SOP 元数据解析 + coverImg 提取
│   └── channels/
│       ├── csdn/
│       │   ├── index.js           # CSDN 适配实现
│       │   └── selectors.js       # CSDN DOM 选择器（改版只改这里）
│       └── gzh/
│           ├── index.js           # 公众号适配实现（含 augmentPayload/testMode/deleteBlog）
│           ├── selectors.js       # 公众号 DOM 选择器
│           └── format.js          # 正文 Markdown → 公众号样式（md.doocs.org）
├── test/                          # 单测
├── package.json
└── skill.env.sh
```

## 设计要点

- **Playwright 主操作**：默认**有头浏览器**，真实点击/输入/粘贴，发布全过程可见。
- **CDP 底层增强**：复用已有 Chrome 登录态（`connectOverCDP`）、`Input.insertText` 兜底富文本、读取 HttpOnly Cookie 判定登录。
- **分层与渠道隔离**：入口只做「解析 → 路由 → 输出」；`lib/` 仅放跨渠道通用能力，**渠道专属业务（含 CLI 参数解析、发布字段、正文转换、删除流程）全部收敛在 `channels/<channel>/`**。
- **登录态持久化 + 主动登录**：面向本地工作台，扫码一次即持久保存；`publish` 未登录会自动拉起浏览器让你登录后继续。
- **有头/无头可配置**：`--mode headed|headless`（默认 headed），或环境变量 `PUBLISHER_MODE`。
- **按渠道分文件夹**：新增渠道只需加 `scripts/channels/<name>/` 与 `references/channels/<name>/sop.md`。

## 快速开始

```bash
cd blogloom-publisher-skill
npm install                 # 首次安装 playwright
source ./skill.env.sh       # 可选：载入环境变量

# CSDN
node scripts/publisher.js csdn checkLogin
node scripts/publisher.js csdn login
node scripts/publisher.js csdn publishDraft --file "/abs/blog.md"
node scripts/publisher.js csdn publish --file "/abs/blog.md" --dry-run
node scripts/publisher.js csdn delete --url "https://blog.csdn.net/x/article/details/123"
node scripts/publisher.js csdn test publish --file "/abs/blog.md"        # 发布→删除

# 公众号 gzh（正文经 md.doocs.org 样式化后粘贴）
node scripts/publisher.js gzh checkLogin
node scripts/publisher.js gzh login
node scripts/publisher.js gzh publishDraft --file "/abs/blog.md" --author "长路" --wechat-name "长路Java"
node scripts/publisher.js gzh delete --title "文章标题"
node scripts/publisher.js gzh test publish --file "/abs/blog.md"         # 发布草稿→删除
```

## 新增渠道

1. 新建 `scripts/channels/<name>/index.js`，导出渠道适配对象（契约见 `scripts/lib/channel.js` 注释）；渠道专属逻辑（如专属参数解析 `augmentPayload`、正文转换、删除流程）都放这里；
2. 新建 `scripts/channels/<name>/selectors.js`，集中维护 DOM 选择器；
3. （可选）新建 `scripts/channels/<name>/format.js` 存放该渠道的正文格式化逻辑；
4. 新建 `references/channels/<name>/sop.md`，写该渠道 SOP；
5. 在 `SKILL.md` 场景路由表登记；
6. 新建 `test/channels/<name>.test.js`，用 `test/helpers/mock-page.js` 对该渠道**每个脚本工具**做单测。

## 测试（统一命令）

```bash
npm test              # 跑全部单测（核心 + 所有渠道）
npm run test:core     # 仅核心（CLI / markdown / channel 加载）
npm run test:channels # 仅渠道（csdn / gzh）
```

- 渠道单测用 `test/helpers/mock-page.js` 模拟 Playwright（locator/click/fill/hover/filter…），
  对每个渠道的脚本工具逐一验证，无需真实浏览器：
  - csdn：`checkLogin / enterEditor / setTitle / setContent / preparePublish / publish / saveDraft / deleteBlog`；
  - gzh：`checkLogin / enterEditor / setTitle / setAuthor / setContent / preparePublish / publish / saveDraft / deleteBlog / augmentPayload`。

## 渠道发布配置（conf/）

**文章自身属性**（标题/标签/分类/摘要/专栏/封面等）来自博客顶部 SOP 元数据；**渠道级发布选项**（可见范围、创作来源、是否声明原创等）放在：

```text
conf/
├── csdn.conf.template   # 模板（提交）：复制为 csdn.conf 后修改
├── gzh.conf.template    # 模板（提交）：复制为 gzh.conf 后修改
├── csdn.conf            # 实际配置（已 .gitignore，不入库）
└── gzh.conf
```

```bash
# 复制（或 mv）模板为实际配置，再按需修改
mv conf/csdn.conf.template conf/csdn.conf
mv conf/gzh.conf.template conf/gzh.conf
```

- 语法：`key=value`，一行一项，`#` 注释，布尔用 `true/false`；
- 脚本执行对应渠道时**默认读取 `conf/<channel>.conf`** 并用于选项选择；若该文件不存在，则回退读取同名的 `conf/<channel>.conf.template`；
- 优先级：**CLI 参数 > 元数据 extra > conf 默认**（如 gzh 的 `--author` 覆盖 `gzh.conf` 的 `author`）；
- `conf/*.conf` 已在 `.gitignore` 忽略（含个人默认值，勿提交）；模板 `conf/*.conf.template` 提交入库供其他开发者复制、修改；
- `lib/channel-conf.js` 提供解析与加载，无任何配置文件时回退内置默认值。

## 环境变量

| 变量 | 默认 | 说明 |
| --- | --- | --- |
| `PUBLISHER_HOME` | `~/.blogloom-publisher` | 数据根目录 |
| `PUBLISHER_BROWSER_CHANNEL` | `chrome` | 浏览器渠道，找不到回退 Chromium |
| `PUBLISHER_MODE` | `headed` | 浏览器模式：headed / headless |
| `PUBLISHER_CDP_URL` | 空 | 连接已有 Chrome 的 CDP 地址 |
| `PUBLISHER_LOGIN_TIMEOUT` | `600000` | 登录等待上限（毫秒，默认 10 分钟） |