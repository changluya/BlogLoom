'use strict';

/**
 * 会话与登录编排：
 * - openSession / makeChannelCtx：创建浏览器会话并构造渠道上下文；
 * - runWithLogin：统一「确保登录 → 执行 handler」，未登录时可主动拉起浏览器扫码。
 */

const { BrowserSession } = require('./session');
const { loadChannelConf } = require('./channel-conf');
const { log } = require('./cli-io');

/** 渠道方法上下文：暴露渠道配置 + 会话 Cookie 读取能力 + 渠道发布配置(conf)。 */
function makeChannelCtx(channel, session) {
  return {
    cookies: (url) => session.cookies(url),
    conf: loadChannelConf(channel.name),
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
  // 渠道可在此把页面推进到真正的扫码登录页（如公众号需先点首页「登录」入口）
  if (typeof channel.prepareLogin === 'function') {
    await channel.prepareLogin(page).catch(() => {});
  }
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

module.exports = {
  runWithLogin,
  openSession,
  makeChannelCtx,
  loginTimeout,
  resolveHeadless,
};
