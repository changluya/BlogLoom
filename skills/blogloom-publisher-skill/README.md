# BlogLoom Publisher Skill

把符合「[标准生成发布输出博客 SOP](../../codes/标准生成发布输出博客sop.md)」的本地 Markdown 博客，用 **Playwright 主操作 + CDP 底层增强** 模拟真人操作，分发到第三方内容渠道。

初次对接渠道：**csdn**。

## 目录结构

```text
blogloom-publisher-skill/
├── SKILL.md                       # 场景路由（按渠道）
├── references/
│   ├── 00-tool-contract.md        # 通用工具契约：CLI / 参数 / 返回 / 错误码 / 环境变量
│   └── channels/
│       └── csdn/sop.md            # CSDN 渠道 SOP（checkLogin / login / publish）
├── scripts/
│   ├── publisher.js               # CLI 入口（<channel> <action>）
│   ├── lib/
│   │   ├── session.js             # 浏览器会话：Playwright 启动 / CDP 连接
│   │   ├── channel.js             # 渠道加载（按目录）
│   │   ├── editor.js              # 正文写入策略（paste/cdp/keyboard）
│   │   └── markdown.js            # SOP 元数据解析 + coverImg 提取
│   └── channels/
│       └── csdn/
│           ├── index.js           # CSDN 适配实现
│           └── selectors.js       # CSDN DOM 选择器（改版只改这里）
├── test/                          # 单测
├── package.json
└── skill.env.sh
```

## 设计要点

- **Playwright 主操作**：默认**有头浏览器**，真实点击/输入/粘贴，发布全过程可见。
- **CDP 底层增强**：复用已有 Chrome 登录态（`connectOverCDP`）、`Input.insertText` 兜底富文本、读取 HttpOnly Cookie 判定登录。
- **登录态持久化 + 主动登录**：面向本地工作台，扫码一次即持久保存；`publish` 未登录会自动拉起浏览器让你登录后继续。
- **有头/无头可配置**：`--mode headed|headless`（默认 headed），或环境变量 `PUBLISHER_MODE`。
- **按渠道分文件夹**：新增渠道只需加 `scripts/channels/<name>/` 与 `references/channels/<name>/sop.md`。

## 快速开始

```bash
cd blogloom-publisher-skill
npm install                 # 首次安装 playwright
source ./skill.env.sh       # 可选：载入环境变量

node scripts/publisher.js csdn checkLogin                 # 检测登录态
node scripts/publisher.js csdn login                      # 扫码登录
node scripts/publisher.js csdn publishDraft --file "/abs/blog.md"        # 存草稿
node scripts/publisher.js csdn publish --file "/abs/blog.md" --dry-run   # 干跑（可见全过程）
node scripts/publisher.js csdn publish --file "/abs/blog.md"             # 正式发布
node scripts/publisher.js csdn delete --url "https://blog.csdn.net/x/article/details/123"  # 删除
node scripts/publisher.js csdn test publish --file "/abs/blog.md"        # 发布→删除 链路自测
node scripts/publisher.js csdn publish --file "/abs/blog.md" --mode headless  # 无头发布
```

## 新增渠道

1. 新建 `scripts/channels/<name>/index.js`，导出渠道适配对象（契约见 `scripts/lib/channel.js` 注释）；
2. 新建 `scripts/channels/<name>/selectors.js`，集中维护 DOM 选择器；
3. 新建 `references/channels/<name>/sop.md`，写该渠道 SOP；
4. 在 `SKILL.md` 场景路由表登记；
5. 新建 `test/channels/<name>.test.js`，用 `test/helpers/mock-page.js` 对每个工具做单测。

## 测试（统一命令）

```bash
npm test              # 跑全部单测（核心 + 所有渠道）
npm run test:core     # 仅核心（CLI / markdown / channel 加载）
npm run test:channels # 仅渠道（当前 csdn）
```

- 渠道单测用 `test/helpers/mock-page.js` 模拟 Playwright（locator/click/fill/hover/filter…），
  对 `checkLogin / enterEditor / setTitle / setContent / preparePublish / publish / saveDraft / deleteBlog` 等逐一验证，无需真实浏览器。

## 环境变量

| 变量 | 默认 | 说明 |
| --- | --- | --- |
| `PUBLISHER_HOME` | `~/.blogloom-publisher` | 数据根目录 |
| `PUBLISHER_BROWSER_CHANNEL` | `chrome` | 浏览器渠道，找不到回退 Chromium |
| `PUBLISHER_MODE` | `headed` | 浏览器模式：headed / headless |
| `PUBLISHER_CDP_URL` | 空 | 连接已有 Chrome 的 CDP 地址 |
| `PUBLISHER_LOGIN_TIMEOUT` | `600000` | 登录等待上限（毫秒，默认 10 分钟） |