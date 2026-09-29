'use strict';

const test = require('node:test');
const assert = require('node:assert');
const { availableChannels, loadChannel } = require('../scripts/lib/channel');

const REQUIRED = ['checkLogin', 'enterEditor', 'setTitle', 'setContent', 'preparePublish', 'publish', 'saveDraft', 'deleteBlog'];

test('availableChannels: 至少包含 csdn', () => {
  assert.ok(availableChannels().includes('csdn'));
});

test('loadChannel: csdn 适配实现完整', () => {
  const csdn = loadChannel('csdn');
  assert.strictEqual(csdn.name, 'csdn');
  assert.ok(csdn.loginUrl && csdn.editorUrl && csdn.homeUrl);
  assert.ok(Array.isArray(csdn.loginCookieNames) && csdn.loginCookieNames.length > 0);
  for (const fn of REQUIRED) {
    assert.strictEqual(typeof csdn[fn], 'function', `缺少方法: ${fn}`);
  }
});

test('loadChannel: 未知渠道报错并提示可用渠道', () => {
  assert.throws(() => loadChannel('not-exist'), /未知渠道/);
});

test('loadChannel: 空渠道报错', () => {
  assert.throws(() => loadChannel(''), /必须指定渠道/);
});