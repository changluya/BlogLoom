# 00 工具契约

> 入口：`node scripts/publisher.js <channel> <action> [options]`
> 统一输出（stdout）：`{"ok": true|false, "channel": "...", "action": "...", "data": {...}|null, "error": null|"..."}`
> 日志走 **stderr**（`[publisher]` 前缀），stdout 只保留结果 JSON，便于 Agent 解析。

## 1. 动作（action）

| 动作 | 说明 | 是否需要登录 |
| --- | --- | --- |
| `checkLogin` | 检测指定渠道是否已登录 | 否 |
| `login` | 打开浏览器，让用户扫码登录并持久化登录态 | 否 |
| `publishDraft` | 按 SOP 元数据保存为**草稿** | 是 |
| `publish` | 按 SOP 元数据**发布**博客 | 是 |
| `delete` | 删除博客（进入内容管理页定位后删除，含二次确认与结果校验；渠道支持时可用） | 是 |
| `test <scenario>` | 组合链路自测，当前支持 `test publish`（csdn：发布后删除；gzh：发布草稿后删除） | 是 |

## 2. 渠道（channel）

渠道按目录维护：`scripts/channels/<channel>/`。当前可用：`csdn`、`gzh`（微信公众号）。

## 3. 参数

| 参数 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- |
| `<channel>` / `--channel` | 是 | — | 渠道标识，如 `csdn` |
| `<action>` / `--action` | 是 | — | `checkLogin` / `login` / `publish` |
| `--file` / `-f` | publish 必填 | — | 待发布 Markdown 路径（含顶部 SOP 元数据代码块） |
| `--title` | 否 | 取元数据 `title` | 覆盖标题 |
| `--tags` | 否 | 取元数据 `tags` | 覆盖标签（逗号分隔，最多 10 个） |
| `--column` | 否 | 取元数据 `columns[0]` | 指定渠道专栏/分类 |
| `--summary` | 否 | 取元数据 `articleSummary` | 覆盖摘要 |
| `--cover` | 否 | 取正文 `![coverImg]` | 覆盖封面图 |
| `--url` | delete 用 | — | 按文章 URL 定位（自动解析出 id） |
| `--id` | delete 用 | — | 按文章 id 定位 |
| `--title` | delete 用 | 取元数据 `title` | 按标题定位 |
| `--dry-run` | 否 | 否 | 填写完成但不点击最终发布 |
| `--auto-login` | 否 | 见下 | 检测到未登录时主动打开浏览器让用户登录 |
| `--no-login` | 否 | 否 | 检测到未登录时不自动登录，直接报错退出 |
| `--cdp <url>` | 否 | 读环境变量 | 连接已有 Chrome，如 `http://127.0.0.1:9222` |
| `--mode <m>` | 否 | `headed` | 浏览器模式：`headed`（可见，默认）/ `headless` |
| `--headed` / `--headless` | 否 | — | `--mode` 的快捷写法 |
| `--timeout <ms>` | 否 | 600000 | 登录等待 / 连接超时（默认 10 分钟） |
| `--params '<json>'` | 否 | — | 以 JSON 传入参数（Agent 调用友好） |
| 渠道专属参数 | 否 | — | 任意 `--key value` 原样透传给渠道，由渠道自行解析（如 gzh 的 `--author` / `--wechat-name` / `--collection` / `--group-send`），核心 CLI 不感知 |

> 默认**有头模式**：发布全过程可见，便于用户确认操作。CI / 无人值守可用 `--mode headless`。

## 3.1 自动登录（本地工作台场景）

本 Skill 定位为**用户本地工作台**，在检测到未登录时会**主动打开浏览器**让用户扫码：

| 动作 | 自动登录默认 | 说明 |
| --- | --- | --- |
| `publish` | **开启** | 未登录 → 自动拉起（有头）浏览器，登录完成后继续发布 |
| `checkLogin` | 关闭 | 仅检测；加 `--auto-login` 才主动拉起登录 |
| `login` | 开启 | 本身就是登录动作 |

- 无头模式（`--headless`）无法扫码，会自动重启为有头浏览器完成登录。
- 加 `--no-login` 可禁用自动登录，未登录时直接报错退出（适合 CI / 批处理）。
- 登录等待默认 **10 分钟**（`PUBLISHER_LOGIN_TIMEOUT` / `--timeout` 可调），给扫码充足时间。

## 4. 环境变量

| 变量 | 含义 | 来源 |
| --- | --- | --- |
| `PUBLISHER_HOME` | 数据根目录，默认 `~/.blogloom-publisher` | `skill.env.sh` |
| `PUBLISHER_PROFILE_DIR` | 浏览器 profile 目录（留空按渠道生成） | `skill.env.sh` |
| `PUBLISHER_BROWSER_CHANNEL` | 浏览器渠道，默认 `chrome`（找不到回退 Chromium） | `skill.env.sh` |
| `PUBLISHER_MODE` | 浏览器模式，`headed`（默认）/ `headless` | `skill.env.sh` |
| `PUBLISHER_CDP_URL` / `PUBLISHER_CDP_HOST` / `PUBLISHER_CDP_PORT` | 连接已有 Chrome 的 CDP 地址 | `skill.env.sh` |
| `PUBLISHER_LOGIN_TIMEOUT` | 登录等待上限（毫秒） | `skill.env.sh` |

## 5. 返回结构

`checkLogin`：

```json
{ "ok": true, "channel": "csdn", "action": "checkLogin",
  "data": { "loggedIn": true, "user": "长路", "reason": "检测到 CSDN 登录态", "authCookieNames": ["UserToken", "UserInfo"] },
  "error": null }
```

`login`：

```json
{ "ok": true, "channel": "csdn", "action": "login",
  "data": { "loggedIn": true, "cookieNames": ["UserToken", "UserInfo"] }, "error": null }
```

`publish`：

```json
{ "ok": true, "channel": "csdn", "action": "publish",
  "data": { "status": "PUBLISHED", "url": "https://blog.csdn.net/xxx/article/details/123",
            "title": "Maven插件—05：...", "tags": ["Maven", "spotless"],
            "column": "项目管理工具", "contentLength": 14986, "contentStrategy": "paste",
            "publishConfig": { "categories": ["项目管理工具", "Maven&Gradle"],
              "resolvedCategories": [{"name": "Maven&Gradle", "matched": "# Maven&Gradle"}],
              "selectedCategories": ["项目管理工具", "# Maven&Gradle"],
              "unresolvedCategories": [], "coverSet": true, "creationStatement": "个人观点，仅供参考" } },
  "error": null }
```

`status`：
- publish/publishDraft：`PUBLISHED`（发布成功）/ `DRAFT_SAVED`（草稿已保存）/ `DRY_RUN`（仅填写）/ `UNKNOWN`（未确认）；
  - 渠道可追加自己的终态（`channel.successStatuses`），如 gzh 发表后为 `SUBMITTED`（已提交，需管理员扫码/审核）；
- delete：`DELETED`（已删除并校验）/ `DELETE_UNVERIFIED`（点击后未确认到已删除）。

`test publish`：

```json
// csdn：发布 → 删除
{ "ok": true, "channel": "csdn", "action": "test publish",
  "data": { "status": "PUBLISHED_AND_DELETED",
            "publish": { "status": "PUBLISHED", "url": "https://blog.csdn.net/x/article/details/123", "publishConfig": {} },
            "delete": { "status": "DELETED", "id": "123" } },
  "error": null }

// gzh：发布草稿 → 删除（channel.testMode=draft，避免自动群发/审核）
{ "ok": true, "channel": "gzh", "action": "test publish",
  "data": { "status": "DRAFT_SAVED_AND_DELETED", "mode": "draft",
            "publish": { "status": "DRAFT_SAVED", "url": "https://mp.weixin.qq.com/..." },
            "delete": { "status": "DELETED", "title": "..." } },
  "error": null }
```

## 6. 退出码与错误

| 退出码 | 场景 |
| --- | --- |
| `0` | 成功（含 `DRY_RUN`） |
| `1` | 执行异常（文件不存在、未识别参数拦截、发布失败） |
| `2` | `checkLogin` 未登录 / `login` 超时 |

| 错误 | 处理 |
| --- | --- |
| 未登录直接 `publish` | `ok=false`，提示先执行 `<channel> login` |
| 渠道不存在 | `ok=false`，附 `available` 渠道列表 |
| 缺参 | `ok=false`，明确缺失字段 |
| `--params` 非法 JSON | 进程报错退出（退出码 1） |
| 正文写入失败 | `ok=false`，列出已尝试的写入策略与各自原因 |