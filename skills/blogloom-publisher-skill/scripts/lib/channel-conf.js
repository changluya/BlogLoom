'use strict';

/**
 * 渠道发布配置加载：conf/<channel>.conf（key=value）。
 *
 * 只承载「渠道级发布选项」（如可见范围、创作来源、是否声明原创等），
 * 与「文章自身属性」（来自博客顶部 SOP 元数据）分离：
 *   - 文章属性：由 lib/markdown.js 解析 Markdown 顶部的 ```json 元数据块；
 *   - 渠道选项：本文件按渠道加载，供 channels/<channel> 使用。
 *
 * 文件不存在或解析失败时返回 {}（渠道使用内置默认值）。
 */

const fs = require('fs');
const path = require('path');

const CONF_DIR = path.join(__dirname, '..', '..', 'conf');

/** 解析 conf 文本：忽略空行与 # 注释，支持带引号的值与 true/false。 */
function parseConf(text) {
  const out = {};
  for (const line of String(text).split(/\r?\n/)) {
    const s = line.trim();
    if (!s || s.startsWith('#')) continue;
    const eq = s.indexOf('=');
    if (eq < 0) continue;
    const key = s.slice(0, eq).trim();
    let value = s.slice(eq + 1).trim();
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    if (value === 'true') value = true;
    else if (value === 'false') value = false;
    out[key] = value;
  }
  return out;
}

/** 读取某渠道的发布配置 conf/<channel>.conf；不存在则返回 {}（渠道使用内置默认值）。 */
function loadChannelConf(name) {
  const key = String(name || '').trim().toLowerCase();
  if (!key) return {};
  const file = path.join(CONF_DIR, `${key}.conf`);
  try {
    if (!fs.existsSync(file)) return {};
    return parseConf(fs.readFileSync(file, 'utf8'));
  } catch (err) {
    return {};
  }
}

module.exports = { loadChannelConf, parseConf, CONF_DIR };
