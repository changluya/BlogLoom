'use strict';

/** CLI 参数解析与帮助文本。 */

const { availableChannels } = require('./channel');

const ACTIONS = ['checkLogin', 'login', 'publishDraft', 'publish', 'delete', 'test'];

/** `foo-bar` → `fooBar`。 */
function toCamel(key) {
  return key.replace(/-([a-z0-9])/g, (_, c) => c.toUpperCase());
}

/**
 * 通用参数解析：
 * - 核心参数（channel/action/file/params/timeout/mode/布尔开关）在此显式归一；
 * - 其余 `--key value` / `--key=value` / `--flag` / `--no-key` 一律解析为 opts[key]，
 *   渠道专属参数（如 gzh 的 --author/--wechat-name/--collection/--group-send）由各渠道自行消费，
 *   **核心解析层不感知任何渠道业务**。
 */
function parseArgs(argv) {
  const opts = { _: [], params: {} };
  let i = 0;
  const next = () => argv[++i];
  for (i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === '--channel') opts.channel = next();
    else if (arg.startsWith('--channel=')) opts.channel = arg.split('=')[1];
    else if (arg === '--action') opts.action = next();
    else if (arg.startsWith('--action=')) opts.action = arg.split('=')[1];
    else if (arg === '--file' || arg === '-f') opts.file = next();
    else if (arg.startsWith('--file=')) opts.file = arg.split('=')[1];
    else if (arg === '--params') opts.params = JSON.parse(next() || '{}');
    else if (arg.startsWith('--params=')) opts.params = JSON.parse(arg.slice('--params='.length) || '{}');
    else if (arg === '--timeout') opts.timeout = Number(next());
    else if (arg.startsWith('--timeout=')) opts.timeout = Number(arg.split('=')[1]);
    // 核心布尔开关（跨渠道统一语义）
    else if (arg === '--draft') opts.draft = true;
    else if (arg === '--dry-run' || arg === '--dryRun') opts.dryRun = true;
    else if (arg === '--auto-login') opts.autoLogin = true;
    else if (arg === '--no-login') opts.noLogin = true;
    else if (arg === '--headed') opts.headless = false;
    else if (arg === '--headless') opts.headless = true;
    else if (arg === '--json' || arg === '--pretty') opts.pretty = true;
    else if (arg === '--help' || arg === '-h') opts.help = true;
    // 通用透传：渠道专属参数
    else if (arg.startsWith('--no-')) opts[toCamel(arg.slice('--no-'.length))] = false;
    else if (arg.startsWith('--')) {
      const body = arg.slice(2);
      const eq = body.indexOf('=');
      if (eq >= 0) {
        opts[toCamel(body.slice(0, eq))] = body.slice(eq + 1);
      } else {
        const value = argv[i + 1];
        if (value !== undefined && !value.startsWith('--')) {
          opts[toCamel(body)] = value;
          i += 1;
        } else {
          opts[toCamel(body)] = true;
        }
      }
    } else opts._.push(arg);
  }
  opts.channel = opts.channel || opts._[0];
  opts.action = opts.action || opts._[1];
  opts.testScenario = opts.testScenario || opts._[2];
  return opts;
}

function printHelp() {
  process.stdout.write(`BlogLoom Publisher Skill

功能: 按「标准生成发布输出博客 SOP」把本地 Markdown 分发到第三方渠道。

用法: node scripts/publisher.js <channel> <action> [options]

渠道: ${availableChannels().join(', ') || '(无)'}
动作:
  checkLogin          检测渠道登录态
  login               打开浏览器扫码登录
  publishDraft        保存为草稿
  publish             发布博客
  delete              删除博客（内容管理页定位后彻底删除，渠道支持时可用）
  test <scenario>     组合链路测试，如: test publish（发布后立即删除）

选项:
  -f, --file <path>    待发布 Markdown 文件（含顶部 SOP 元数据代码块）
      --title <text>   delete 用：按标题定位；publish 用：覆盖标题
      --tags <a,b,c>   覆盖标签（逗号分隔）
      --column <name>  指定渠道专栏/分类
      --summary <text> 覆盖摘要
      --cover <url>    覆盖封面图
      --url <url>      delete 用：按文章 URL 定位
      --id <id>        delete 用：按文章 id 定位
      --dry-run        填写完成但不点击最终发布
      --auto-login     检测到未登录时主动打开浏览器让用户登录
      --no-login       检测到未登录时不自动登录，直接报错退出
      --mode <m>       浏览器模式：headed（默认，可见）/ headless（无头）
      --headed         等价于 --mode headed
      --headless       等价于 --mode headless
      --cdp <url>      连接已有 Chrome（如 http://127.0.0.1:9222）
      --params <json>  以 JSON 传入参数
      --pretty         美化输出

  渠道专属参数（如 gzh 的 --author / --wechat-name / --collection / --group-send）
  会按 --key value 原样透传给对应渠道，具体见各渠道 SOP。

示例:
  node scripts/publisher.js csdn checkLogin
  node scripts/publisher.js csdn publish --file "/abs/blog.md"
  node scripts/publisher.js csdn test publish --file "/abs/blog.md"
  node scripts/publisher.js gzh publishDraft --file "/abs/blog.md" --wechat-name "长路Java"
  node scripts/publisher.js gzh test publish --file "/abs/blog.md"
`);
}

module.exports = { parseArgs, printHelp, ACTIONS };
