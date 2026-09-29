---
name: blogloom-publisher-skill
description: "BlogLoom 博客多渠道分发技能（Playwright 主操作 + CDP 底层增强）。Use when: (1) 把符合「标准生成发布输出博客 SOP」的本地 Markdown 博客分发到第三方渠道（初次对接 csdn），(2) 检测/完成渠道登录 csdn checkLogin / csdn login，(3) 发布或存草稿 csdn publish / csdn publishDraft，(4) 删除博客 csdn delete（内容管理页定位后删除），(5) 组合链路自测 csdn test publish（发布后立即删除）。触发词：分发博客、发布博客、存草稿、删除博客、同步到 CSDN、csdn publish/publishDraft/delete/login/checkLogin、publisher skill。"
---

# BlogLoom Publisher Skill

把**符合统一 SOP 的本地 Markdown 博客**，用「Playwright 主操作 + CDP 底层增强」模拟真人操作，分发到第三方内容渠道。

## 核心原则

- **前置对齐 SOP**：任何发布前，先按 [`codes/标准生成发布输出博客sop.md`](../../../codes/标准生成发布输出博客sop.md) 校验并补齐博客顶部元数据（title / tags / category / articleSummary / columns / createTime / updateTime / knowledgeBasePath），元数据不合规不进入分发。
- **模拟用户操作**：主操作走 Playwright（可见浏览器、真实点击/输入/粘贴）；CDP 仅作底层增强（复用登录态、`Input.insertText` 兜底富文本、读取 HttpOnly Cookie 判定登录），不绕过人机校验。
- **登录态本地持久化 + 主动登录**：面向**用户本地工作台**场景，扫码登录一次即持久保存；`publish` 检测到未登录会**主动拉起浏览器**让用户登录后自动继续，无需手动先执行 `login`。

## 场景路由（按渠道）

**一个渠道一个 SOP**，独立维护在 `references/channels/<channel>/sop.md`：路由 = 选渠道 → 读该渠道 SOP。

| 渠道 | SOP | 说明 |
| --- | --- | --- |
| **csdn**（初次对接） | [`references/channels/csdn/sop.md`](references/channels/csdn/sop.md) | checkLogin / login / publish 三类操作 |

> 新增渠道时，同步在 `references/channels/<channel>/sop.md` 建一份该渠道 SOP，并在此表登记。

通用契约（所有渠道共用，先读）：[`references/00-tool-contract.md`](references/00-tool-contract.md)（CLI、参数、返回结构、错误码、环境变量）。

## 统一调用形式

```bash
node scripts/publisher.js <channel> <action> [options]

node scripts/publisher.js csdn checkLogin
node scripts/publisher.js csdn login
node scripts/publisher.js csdn publish --file "/abs/path/blog.md"
node scripts/publisher.js csdn publish --file "/abs/path/blog.md" --dry-run   # 干跑，可见全过程
node scripts/publisher.js csdn publish --file "/abs/path/blog.md" --mode headless
```

## 环境准备

```bash
cd blogloom-publisher-skill
npm install                 # 安装 playwright（首次）
source ./skill.env.sh       # 载入渠道与浏览器配置（可选）
```

> 浏览器模式：默认用 Playwright 独立 profile（`~/.blogloom-publisher/chrome-profile-<channel>`）。
> 若要复用你已打开的 Chrome 登录态，先带 `--remote-debugging-port=9222` 启动 Chrome，再加 `--cdp http://127.0.0.1:9222`。

## 强制约束

- `publish` 默认在未登录时**主动打开浏览器**让用户登录（本地工作台场景）；CI / 无人值守请加 `--no-login`。
- 首次 `publish` 默认建议先加 `--dry-run` 验证字段填写无误，再正式发布。
- 发布是**对外可见**的写操作，Agent 必须先向用户确认标题与渠道后再执行。