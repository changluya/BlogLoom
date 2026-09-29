'use strict';

/**
 * 浏览器会话：Playwright 主操作 + CDP 底层增强。
 *
 * 两种连接模式：
 *  1. 连接已有 Chrome（CDP）：`connectOverCDP`，复用用户真实浏览器与登录态，
 *     不关闭用户浏览器，只断开连接。适合已开调试端口（默认 127.0.0.1:9222）的场景。
 *  2. 启动持久化上下文（Playwright）：独立的 user-data-dir，登录态跨调用保留，
 *     首次 `csdn login` 扫码后，后续 `publish` 无需重复登录。
 */

const fs = require('fs');
const os = require('os');
const path = require('path');
const { chromium } = require('playwright');

const DEFAULT_PUBLISHER_HOME = path.join(os.homedir(), '.blogloom-publisher');
const STEALTH_SCRIPT = `
  Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
  Object.defineProperty(navigator, 'languages', { get: () => ['zh-CN', 'zh', 'en'] });
  Object.defineProperty(navigator, 'plugins', { get: () => [1, 2, 3, 4, 5] });
  window.chrome = window.chrome || { runtime: {} };
`;

function publisherHome() {
  return process.env.PUBLISHER_HOME || DEFAULT_PUBLISHER_HOME;
}

function profileDir(channel) {
  return process.env.PUBLISHER_PROFILE_DIR || path.join(publisherHome(), `chrome-profile-${channel}`);
}

function resolveCdpUrl(options = {}) {
  if (options.cdp) return options.cdp;
  if (process.env.PUBLISHER_CDP_URL) return process.env.PUBLISHER_CDP_URL;
  if (process.env.PUBLISHER_CDP_HOST) {
    const host = process.env.PUBLISHER_CDP_HOST;
    const port = process.env.PUBLISHER_CDP_PORT || '9222';
    return `http://${host}:${port}`;
  }
  return '';
}

class BrowserSession {
  constructor(options = {}) {
    this.options = options;
    this.mode = 'launch';
    this.browser = null;
    this.context = null;
    this._ownsContext = false;
  }

  /** 启动 / 连接浏览器。 */
  async start() {
    const cdpUrl = resolveCdpUrl(this.options);
    if (cdpUrl) {
      await this._connect(cdpUrl);
    } else {
      await this._launch();
    }
    await this.context.addInitScript(STEALTH_SCRIPT).catch(() => {});
    return this;
  }

  async _connect(cdpUrl) {
    this.mode = 'cdp';
    this.cdpUrl = cdpUrl;
    try {
      this.browser = await chromium.connectOverCDP(cdpUrl, { timeout: this.options.timeout || 15000 });
    } catch (err) {
      throw new Error(
        `无法连接 CDP ${cdpUrl}：${err.message}\n` +
          '请确认 Chrome 已带 --remote-debugging-port 启动，或去掉 --cdp 使用独立浏览器模式。'
      );
    }
    this.context = this.browser.contexts()[0] || (await this.browser.newContext());
    this._ownsContext = false;
  }

  async _launch() {
    this.mode = 'launch';
    const channel = this.options.channel || process.env.PUBLISHER_BROWSER_CHANNEL || 'chrome';
    const userDataDir = profileDir(this.options.channelName || 'default');
    fs.mkdirSync(userDataDir, { recursive: true });

    const args = [
      '--disable-blink-features=AutomationControlled',
      '--disable-infobars',
      '--no-first-run',
      '--no-default-browser-check',
    ];

    const launchOptions = {
      headless: !!this.options.headless,
      userDataDir,
      args,
      locale: 'zh-CN',
      viewport: { width: 1440, height: 900 },
    };

    try {
      this.context = await chromium.launchPersistentContext(userDataDir, { ...launchOptions, channel });
    } catch (err) {
      // 未安装系统 Chrome 时回退到 Playwright 自带 Chromium。
      this.context = await chromium.launchPersistentContext(userDataDir, launchOptions);
    }
    this._ownsContext = true;
    this.browser = this.context.browser();
  }

  /** 获取主工作页；优先复用已打开页，否则新建。 */
  async page() {
    const pages = this.context.pages();
    if (pages.length > 0) {
      const page = pages[pages.length - 1];
      await page.bringToFront().catch(() => {});
      return page;
    }
    return this.context.newPage();
  }

  /** 读取 Cookie（含 HttpOnly），用于判定登录态。 */
  async cookies(url) {
    return this.context.cookies(url);
  }

  /** 为当前页开启 CDP 会话（底层增强入口）。 */
  async cdp(page) {
    return this.context.newCDPSession(page);
  }

  async close() {
    try {
      if (this.mode === 'cdp') {
        // 只断开与用户浏览器的连接，不关闭用户浏览器。
        await this.browser?.close();
      } else if (this._ownsContext) {
        await this.context?.close();
      }
    } catch (err) {
      // 关闭阶段错误不外抛
    }
  }
}

module.exports = { BrowserSession, publisherHome, profileDir, resolveCdpUrl, DEFAULT_PUBLISHER_HOME };
