'use strict';

/**
 * 渠道适配层（按渠道分文件夹维护）。
 *
 * 目录约定：
 *   scripts/channels/<channel>/index.js      渠道适配实现（必需）
 *   scripts/channels/<channel>/selectors.js  渠道 DOM 选择器（推荐，独立维护）
 *   scripts/channels/<channel>/README.md     渠道维护说明（可选）
 *
 * `<channel>/index.js` 需导出：
 * {
 *   name,                       // 渠道标识（目录名 / CLI --channel）
 *   displayName,                // 展示名
 *   editorUrl,                  // 发布页（编辑器）地址
 *   loginUrl,                   // 登录页地址
 *   homeUrl,                    // 站点首页（用于登录态判定）
 *   loginCookieNames: [...],    // 判定已登录的关键 Cookie（任一命中即视为已登录）
 *   checkLogin(page, ctx)       // 返回 { loggedIn, reason, ... }
 *   ensureEditor(page, ctx)     // 确保处于编辑器页
 *   setTitle(page, title)
 *   setContent(page, markdown)  // 写入正文（Markdown 源码）
 *   setTags(page, tags)
 *   setCategory(page, column)
 *   setSummary(page, summary)
 *   publish(page, opts)         // 触发发布，返回 { url, status }
 * }
 */

const fs = require('fs');
const path = require('path');

const CHANNELS_DIR = path.join(__dirname, '..', 'channels');

/** 列出所有可用渠道（目录形式，或兼容单文件 <name>.js）。 */
function availableChannels() {
  if (!fs.existsSync(CHANNELS_DIR)) return [];
  return fs
    .readdirSync(CHANNELS_DIR, { withFileTypes: true })
    .filter((entry) => {
      if (entry.isDirectory()) return fs.existsSync(path.join(CHANNELS_DIR, entry.name, 'index.js'));
      return entry.isFile() && entry.name.endsWith('.js');
    })
    .map((entry) => (entry.isDirectory() ? entry.name : entry.name.replace(/\.js$/, '')))
    .sort();
}

/** 载入指定渠道适配。 */
function loadChannel(name) {
  const key = String(name || '').trim().toLowerCase();
  if (!key) {
    throw new Error(`必须指定渠道（--channel），可用: ${availableChannels().join(', ') || '无'}`);
  }

  const dirIndex = path.join(CHANNELS_DIR, key, 'index.js');
  const flatFile = path.join(CHANNELS_DIR, `${key}.js`);
  const file = fs.existsSync(dirIndex) ? dirIndex : fs.existsSync(flatFile) ? flatFile : '';

  if (!file) {
    throw new Error(`未知渠道: ${name}，可用: ${availableChannels().join(', ') || '无'}`);
  }

  const channel = require(file);
  if (!channel || !channel.name) throw new Error(`渠道定义非法: ${name}`);
  return channel;
}

module.exports = { loadChannel, availableChannels, CHANNELS_DIR };