'use strict';

const test = require('node:test');
const assert = require('node:assert');
const { availableChannels, loadChannel } = require('../scripts/lib/channel');

const REQUIRED = ['checkLogin', 'enterEditor', 'setTitle', 'setContent', 'preparePublish', 'publish', 'saveDraft'];

test('availableChannels: 至少包含 csdn 与 gzh', () => {
  const channels = availableChannels();
  assert.ok(channels.includes('csdn'));
  assert.ok(channels.includes('gzh'));
});

test('loadChannel: csdn 适配实现完整（含 delete）', () => {
  const csdn = loadChannel('csdn');
  assert.strictEqual(csdn.name, 'csdn');
  assert.ok(csdn.loginUrl && csdn.editorUrl && csdn.homeUrl);
  assert.ok(Array.isArray(csdn.loginCookieNames) && csdn.loginCookieNames.length > 0);
  for (const fn of REQUIRED) {
    assert.strictEqual(typeof csdn[fn], 'function', `缺少方法: ${fn}`);
  }
  assert.strictEqual(typeof csdn.deleteBlog, 'function');
});

test('loadChannel: gzh 适配实现完整（含 delete）', () => {
  const gzh = loadChannel('gzh');
  assert.strictEqual(gzh.name, 'gzh');
  assert.ok(gzh.loginUrl && gzh.editorUrl && gzh.homeUrl);
  assert.ok(Array.isArray(gzh.loginCookieNames) && gzh.loginCookieNames.length > 0);
  for (const fn of REQUIRED) {
    assert.strictEqual(typeof gzh[fn], 'function', `缺少方法: ${fn}`);
  }
  assert.strictEqual(typeof gzh.deleteBlog, 'function');
});

test('loadChannel: 未知渠道报错并提示可用渠道', () => {
  assert.throws(() => loadChannel('not-exist'), /未知渠道/);
});

test('loadChannel: 空渠道报错', () => {
  assert.throws(() => loadChannel(''), /必须指定渠道/);
});
