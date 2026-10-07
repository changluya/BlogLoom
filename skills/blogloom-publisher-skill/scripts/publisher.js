#!/usr/bin/env node
'use strict';

/**
 * BlogLoom Publisher Skill CLI —— Playwright 主操作 + CDP 底层增强。
 *
 * 入口只负责「解析 → 路由 → 输出」；具体职责已拆分到 lib/：
 *   lib/args.js     参数解析与帮助文本
 *   lib/payload.js  SOP 元数据 → 发布参数、删除目标解析
 *   lib/runner.js   浏览器会话与登录编排
 *   lib/actions.js  各 action 实现（checkLogin/login/publish/delete/test）
 *   lib/channel.js  渠道加载（按目录）
 *   lib/session.js  Playwright/CDP 会话
 *
 * 用法：
 *   node scripts/publisher.js <channel> <action> [options]
 *
 * 统一输出（stdout）：{"ok":bool,"channel":"...","action":"...","data":{},"error":null}
 */

const { parseArgs, printHelp, ACTIONS } = require('./lib/args');
const { loadChannel } = require('./lib/channel');
const { emit } = require('./lib/cli-io');
const { dispatch } = require('./lib/actions');

const ACTION_ALIASES = { checklogin: 'checkLogin', publishdraft: 'publishDraft' };

async function run() {
  const opts = parseArgs(process.argv.slice(2));
  if (opts.help || (!opts.channel && !opts.action)) {
    printHelp();
    return 0;
  }

  const channel = loadChannel(opts.channel);
  const action = String(opts.action || '').trim();
  const actionKey = ACTION_ALIASES[action.toLowerCase()] || action;

  if (!ACTIONS.includes(actionKey)) {
    throw new Error(`未知动作: ${action || '(空)'}，可用: ${ACTIONS.join(', ')}`);
  }

  // 需要登录的动作默认允许自动登录（本地工作台场景）
  if (actionKey !== 'checkLogin' && actionKey !== 'login' && opts.autoLogin === undefined) {
    opts.autoLogin = true;
  }

  const outcome = await dispatch(channel, opts, actionKey);
  emit({
    ok: outcome.ok,
    channel: channel.name,
    action: outcome.action || actionKey,
    data: outcome.data,
    error: outcome.error,
  });
  return outcome.code;
}

run()
  .then((code) => process.exit(code || 0))
  .catch((err) => {
    emit({
      ok: false,
      channel: process.argv[2] || '',
      action: process.argv.slice(3).join(' ') || '',
      data: null,
      error: err.message,
    });
    process.exit(1);
  });
