#!/usr/bin/env node
'use strict';

/**
 * BlogLoom Publisher Skill CLI —— Playwright 主操作 + CDP 底层增强。
 *
 * 用法：
 *   node scripts/publisher.js <channel> <action> [options]
 *   node scripts/publisher.js --channel csdn --action publish --file <path> [--params '{...}']
 *
 * 示例：
 *   node scripts/publisher.js csdn checkLogin
 *   node scripts/publisher.js csdn login
 *   node scripts/publisher.js csdn publishDraft --file "/abs/blog.md"
 *   node scripts/publisher.js csdn publish --file "/abs/blog.md"
 *   node scripts/publisher.js csdn publish --file "/abs/blog.md" --dry-run
 *   node scripts/publisher.js csdn delete --url "https://blog.csdn.net/x/article/details/123"
 *   node scripts/publisher.js csdn test publish --file "/abs/blog.md"   # 发布后立即删除（验证链路）
 *
 * 统一输出（stdout 末行）：{"ok":bool,"channel":"csdn","action":"...","data":{},"error":null}
 */

const fs = require('fs');
const { BrowserSession } = require('./lib/session');
const { loadChannel, availableChannels } = require('./lib/channel');
const { parseMarkdown } = require('./lib/markdown');

const ACTIONS = ['checkLogin', 'login', 'publishDraft', 'publish', 'delete', 'test'];

function log(msg) {
  process.stderr.write(`[publisher] ${msg}\n`);
}

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
    else if (arg === '--title') opts.title = next();
    else if (arg === '--url') opts.url = next();
    else if (arg.startsWith('--url=')) opts.url = arg.split('=')[1];
    else if (arg === '--id') opts.id = next();
    else if (arg.startsWith('--id=')) opts.id = arg.split('=')[1];
    else if (arg === '--tags') opts.tags = next();
    else if (arg === '--column') opts.column = next();
    else if (arg === '--summary') opts.summary = next();
    else if (arg === '--cover') opts.cover = next();
    else if (arg === '--params') opts.params = JSON.parse(next() || '{}');
    else if (arg.startsWith('--params=')) opts.params = JSON.parse(arg.slice('--params='.length) || '{}');
    else if (arg === '--cdp') opts.cdp = next();
    else if (arg.startsWith('--cdp=')) opts.cdp = arg.split('=')[1];
    else if (arg === '--timeout') opts.timeout = Number(next());
    else if (arg === '--draft') opts.draft = true;
    else if (arg === '--dry-run' || arg === '--dryRun') opts.dryRun = true;
    else if (arg === '--auto-login') opts.autoLogin = true;
    else if (arg === '--no-login') opts.noLogin = true;
    else if (arg === '--mode') opts.mode = String(next() || '').toLowerCase();
    else if (arg.startsWith('--mode=')) opts.mode = arg.split('=')[1].toLowerCase();
    else if (arg === '--headed') opts.headless = false;
    else if (arg === '--headless') opts.headless = true;
    else if (arg === '--json' || arg === '--pretty') opts.pretty = true;
    else if (arg === '--help' || arg === '-h') opts.help = true;
    else if (!arg.startsWith('--')) opts._.push(arg);
    else log(`WARN 未识别参数: ${arg}`);
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
  delete              删除博客（内容管理页定位后彻底删除）
  test <scenario>     组合链路测试，如: test publish（发布后立即删除）

选项:
  -f, --file <path>    待发布 Markdown 文件（含顶部 SOP 元数据代码块）
      --column <name>  指定渠道专栏/分类
      --tags <a,b,c>   覆盖标签（逗号分隔）
      --summary <text> 覆盖摘要
      --url <url>      delete 用：按文章 URL 定位
      --id <id>        delete 用：按文章 id 定位
      --title <text>   delete 用：按标题定位
      --dry-run        填写完成但不点击最终发布
      --auto-login     检测到未登录时主动打开浏览器让用户登录
      --no-login       检测到未登录时不自动登录，直接报错退出
      --mode <m>       浏览器模式：headed（默认，可见）/ headless（无头）
      --headed         等价于 --mode headed
      --headless       等价于 --mode headless
      --cdp <url>      连接已有 Chrome（如 http://127.0.0.1:9222）
      --params <json>  以 JSON 传入参数
      --pretty         美化输出

示例:
  node scripts/publisher.js csdn checkLogin
  node scripts/publisher.js csdn login
  node scripts/publisher.js csdn publish --file "/abs/blog.md"
  node scripts/publisher.js csdn publishDraft --file "/abs/blog.md"
  node scripts/publisher.js csdn delete --url "https://blog.csdn.net/x/article/details/123"
  node scripts/publisher.js csdn test publish --file "/abs/blog.md"
`);
}

function emit(payload) {
  process.stdout.write(`${JSON.stringify(payload, null, 2)}\n`);
}

/** 从元数据 + CLI 覆盖值推导发布参数（对齐 SOP：tags/category/columns/articleSummary）。 */
function buildPublishPayload(channel, opts) {
  const params = opts.params || {};
  const file = opts.file || params.file || params.file_path;
  if (!file) throw new Error('缺少 --file（待发布 Markdown 路径）');

  const blog = parseMarkdown(file);
  const tags = (opts.tags || params.tags || blog.tags.join(',')).toString();
  const tagList = tags
    .split(/[,，、|;；]/)
    .map((t) => t.trim())
    .filter(Boolean)
    .slice(0, 10);

  const column = opts.column || params.column || blog.columns[0] || blog.category || '';

  return {
    channel: channel.name,
    file: blog.filePath,
    title: opts.title || params.title || blog.title,
    tags: tagList,
    column,
    columns: blog.columns.length ? blog.columns : column ? [column] : [],
    category: blog.category,
    summary: opts.summary || params.summary || blog.summary,
    cover: opts.cover || params.cover || blog.cover,
    createTime: blog.createTime,
    updateTime: blog.updateTime,
    knowledgeBasePath: blog.knowledgeBasePath,
    extra: blog.extra || {},
    content: blog.content,
  };
}

async function withSession(opts, fn) {
  const session = new BrowserSession({
    headless: opts.headless,
    cdp: opts.cdp,
    timeout: opts.timeout,
    channelName: opts.channel,
  });
  await session.start();
  try {
    return await fn(session);
  } finally {
    await session.close().catch(() => {});
  }
}

/** 渠道方法上下文：暴露渠道配置 + 会话 Cookie 读取能力。 */
function makeChannelCtx(channel, session) {
  return {
    cookies: (url) => session.cookies(url),
    ...channel,
  };
}

function loginTimeout(opts) {
  return opts.timeout || Number(process.env.PUBLISHER_LOGIN_TIMEOUT || 600000);
}

/** 解析有头 / 无头：显式 --headed/--headless 优先，其次 --mode，再取环境变量，默认有头。 */
function resolveHeadless(opts) {
  if (opts.headless === true) return true;
  if (opts.headless === false) return false;
  const mode = String(opts.mode || process.env.PUBLISHER_MODE || 'headed').toLowerCase();
  return mode === 'headless' || mode === 'none';
}

async function openSession(opts, channel, headlessOverride) {
  const headless = headlessOverride === undefined ? resolveHeadless(opts) : headlessOverride;
  const session = new BrowserSession({
    headless,
    cdp: opts.cdp,
    timeout: opts.timeout,
    channelName: channel.name,
  });
  await session.start();
  return session;
}

/** 轮询 Cookie 判定登录完成（登录页内不跳转，避免打断扫码流程）。 */
async function waitForLogin(session, channel, timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const cookies = await session.cookies(channel.homeUrl).catch(() => []);
    const names = cookies.map((c) => c.name);
    if (names.some((n) => channel.loginCookieNames.includes(n))) {
      return { loggedIn: true, cookieNames: names.filter((n) => channel.loginCookieNames.includes(n)) };
    }
    await new Promise((r) => setTimeout(r, 2000));
  }
  return { loggedIn: false, cookieNames: [] };
}

/** 打开登录页并等待用户完成登录（交互式）。 */
async function performLogin(session, channel, timeoutMs) {
  const page = await session.page();
  log(`打开登录页: ${channel.loginUrl}`);
  await page.goto(channel.loginUrl, { waitUntil: 'domcontentloaded', timeout: 45000 }).catch(() => {});
  log(`请在浏览器中完成扫码登录（最多等待 ${Math.round(timeoutMs / 1000)} 秒）...`);
  const result = await waitForLogin(session, channel, timeoutMs);
  if (!result.loggedIn) {
    return { loggedIn: false, reason: '等待登录超时，请重试并完成扫码' };
  }
  await page.goto(channel.homeUrl, { waitUntil: 'domcontentloaded', timeout: 30000 }).catch(() => {});
  log('登录成功，登录态已保存到本地浏览器 profile。');
  return { loggedIn: true, cookieNames: result.cookieNames };
}

/**
 * 统一「确保登录 + 执行 handler」：
 * 检测到未登录且允许自动登录时，主动打开（有头）浏览器让用户完成登录，再继续后续动作。
 */
async function runWithLogin(channel, opts, handler) {
  let session = await openSession(opts, channel);
  try {
    let page = await session.page();
    let ctx = makeChannelCtx(channel, session);
    let login = await channel.checkLogin(page, ctx);

    if (!login.loggedIn && opts.noLogin) {
      const err = new Error(
        `未登录 ${channel.displayName}，已禁用自动登录（--no-login）。请先执行: node scripts/publisher.js ${channel.name} login`
      );
      err.code = 'AUTH_REQUIRED';
      throw err;
    }

    if (!login.loggedIn && opts.autoLogin && !opts.noLogin) {
      // 无头模式无法扫码，重启为有头（CDP 复用用户浏览器时本身可见）
      if (resolveHeadless(opts) && !opts.cdp) {
        await session.close().catch(() => {});
        session = await openSession(opts, channel, false);
        page = await session.page();
        ctx = makeChannelCtx(channel, session);
      }
      log(`检测到未登录 ${channel.displayName}，已为你打开浏览器，请完成扫码登录...`);
      const result = await performLogin(session, channel, loginTimeout(opts));
      if (!result.loggedIn) {
        const err = new Error('登录未完成或超时');
        err.code = 'AUTH_REQUIRED';
        throw err;
      }
      login = await channel.checkLogin(page, ctx);
      if (!login.loggedIn) {
        const err = new Error(`登录后仍未识别到 ${channel.displayName} 登录态`);
        err.code = 'AUTH_REQUIRED';
        throw err;
      }
    }

    return { login, result: await handler(page, ctx, login) };
  } finally {
    await session.close().catch(() => {});
  }
}

/** 解析删除目标：title / url / id（可来自 --file 的元数据标题）。 */
function resolveDeleteTarget(opts, params = {}) {
  const target = {
    url: opts.url || params.url || '',
    id: opts.id || params.id || '',
    title: opts.title || params.title || '',
  };
  const file = opts.file || params.file || params.file_path;
  if (!target.title && file) target.title = parseMarkdown(file).title;
  if (!target.id && target.url) {
    const match = String(target.url).match(/details\/(\d+)/);
    if (match) target.id = match[1];
  }
  if (!target.title && !target.id && !target.url) {
    throw new Error('删除需要提供 --url / --id / --title（或 --file 以标题定位）');
  }
  return target;
}

/** publish / publishDraft 共用：进入编辑器填标题正文，按模式保存草稿或发布。 */
async function runContentAction(channel, opts, payload, mode) {
  return runWithLogin(channel, opts, async (page, ctx) => {
    await channel.enterEditor(page, ctx);
    await channel.setTitle(page, payload.title);
    const contentResult = await channel.setContent(page, payload.content);
    const base = {
      title: payload.title,
      tags: payload.tags,
      column: payload.column,
      contentLength: contentResult.length,
      contentStrategy: contentResult.strategy,
    };

    if (mode === 'draft') {
      if (opts.dryRun) {
        return { ...base, status: 'DRY_RUN', message: '已填写完成，未保存草稿（--dry-run）' };
      }
      const draft = await channel.saveDraft(page);
      return { ...base, status: draft.status, url: draft.url };
    }

    let publishConfig = {};
    if (typeof channel.preparePublish === 'function') {
      publishConfig = (await channel.preparePublish(page, payload)) || {};
    } else {
      if (typeof channel.setTags === 'function') await channel.setTags(page, payload.tags);
      if (typeof channel.setCategory === 'function') await channel.setCategory(page, payload.column);
      if (typeof channel.setSummary === 'function') await channel.setSummary(page, payload.summary);
    }

    if (opts.dryRun) {
      return { ...base, status: 'DRY_RUN', publishConfig, message: '已填写完成，未点击发布（--dry-run）' };
    }

    const result = await channel.publish(page, { draft: opts.draft });
    return { ...base, status: result.status, url: result.url, publishConfig };
  });
}

async function run() {
  const opts = parseArgs(process.argv.slice(2));
  if (opts.help || (!opts.channel && !opts.action)) {
    printHelp();
    return 0;
  }

  const channel = loadChannel(opts.channel);
  const action = String(opts.action || '').trim();
  const normalize = {
    checklogin: 'checkLogin',
    publishdraft: 'publishDraft',
  };
  const actionKey = normalize[action.toLowerCase()] || action;

  if (!ACTIONS.includes(actionKey)) {
    throw new Error(`未知动作: ${action || '(空)'}，可用: ${ACTIONS.join(', ')}`);
  }

  if (actionKey === 'checkLogin') {
    // 默认只检测；加 --auto-login 时，检测到未登录会主动打开浏览器让用户登录
    const { login } = await runWithLogin(channel, opts, async () => null);
    emit({ ok: true, channel: channel.name, action: 'checkLogin', data: login, error: null });
    return login.loggedIn ? 0 : 2;
  }

  if (actionKey === 'login') {
    const data = await runWithLogin(
      channel,
      { ...opts, headless: false, autoLogin: true },
      async () => null
    ).then((r) => r.login);
    emit({ ok: data.loggedIn, channel: channel.name, action: 'login', data, error: data.loggedIn ? null : data.reason });
    return data.loggedIn ? 0 : 2;
  }

  // 需要登录的动作默认允许自动登录（本地工作台场景）
  if (opts.autoLogin === undefined) opts.autoLogin = true;

  if (actionKey === 'publishDraft' || actionKey === 'publish') {
    const mode = actionKey === 'publishDraft' ? 'draft' : 'publish';
    const payload = buildPublishPayload(channel, opts);
    log(`渠道=${channel.name} 标题="${payload.title}" 标签=[${payload.tags.join(', ')}] 专栏="${payload.column}"`);
    const { result: data } = await runContentAction(channel, opts, payload, mode);
    const ok = data.status === 'PUBLISHED' || data.status === 'DRY_RUN' || data.status === 'DRAFT_SAVED';
    emit({ ok, channel: channel.name, action: actionKey, data, error: ok ? null : '操作结果未知，请到 CSDN 后台确认' });
    return ok ? 0 : 1;
  }

  if (actionKey === 'delete') {
    const target = resolveDeleteTarget(opts, opts.params);
    log(`渠道=${channel.name} 删除文章：${target.title || target.id || target.url}`);
    const { result: data } = await runWithLogin(channel, opts, async (page, ctx) =>
      channel.deleteBlog(page, target, ctx)
    );
    emit({ ok: data.status === 'DELETED', channel: channel.name, action: 'delete', data, error: null });
    return data.status === 'DELETED' ? 0 : 1;
  }

  // test <scenario>：组合链路，当前支持 test publish（发布后立即删除）
  const scenario = String(opts.testScenario || 'publish').toLowerCase();
  if (scenario !== 'publish') {
    throw new Error(`未知测试场景: ${scenario}，当前支持: publish`);
  }
  const payload = buildPublishPayload(channel, opts);
  log(`[test publish] 渠道=${channel.name} 标题="${payload.title}"：发布 → 删除`);
  const { result: data } = await runWithLogin(channel, opts, async (page, ctx) => {
    await channel.enterEditor(page, ctx);
    await channel.setTitle(page, payload.title);
    const contentResult = await channel.setContent(page, payload.content);
    let publishConfig = {};
    if (typeof channel.preparePublish === 'function') {
      publishConfig = (await channel.preparePublish(page, payload)) || {};
    }
    const published = await channel.publish(page, {});
    const target = { url: published.url };
    const match = String(published.url || '').match(/details\/(\d+)/);
    if (match) target.id = match[1];
    target.title = payload.title;
    const deleted = await channel.deleteBlog(page, target, ctx);
    return {
      status: published.status === 'PUBLISHED' && deleted.status === 'DELETED' ? 'PUBLISHED_AND_DELETED' : 'UNKNOWN',
      publish: { status: published.status, url: published.url, contentLength: contentResult.length, contentStrategy: contentResult.strategy, publishConfig },
      delete: deleted,
    };
  });
  const ok = data.status === 'PUBLISHED_AND_DELETED';
  emit({ ok, channel: channel.name, action: 'test publish', data, error: ok ? null : '测试链路未完整成功' });
  return ok ? 0 : 1;
}

run()
  .then((code) => process.exit(code || 0))
  .catch((err) => {
    emit({ ok: false, channel: process.argv[2] || '', action: process.argv.slice(3).join(' ') || '', data: null, error: err.message });
    process.exit(1);
  });
