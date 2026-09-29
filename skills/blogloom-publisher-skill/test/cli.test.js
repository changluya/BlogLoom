'use strict';

/**
 * CLI 表面单测：验证参数解析 / 渠道与动作路由 / 缺参拦截（不启动浏览器）。
 */

const test = require('node:test');
const assert = require('node:assert');
const path = require('path');
const { execFileSync } = require('child_process');

const CLI = path.join(__dirname, '..', 'scripts', 'publisher.js');

function run(args) {
  try {
    const stdout = execFileSync('node', [CLI, ...args], { encoding: 'utf8' });
    return { code: 0, stdout };
  } catch (err) {
    return { code: err.status, stdout: err.stdout || '', stderr: err.stderr || '' };
  }
}

test('CLI --help: 列出渠道与动作', () => {
  const { code, stdout } = run(['--help']);
  assert.strictEqual(code, 0);
  assert.match(stdout, /csdn/);
  assert.match(stdout, /publishDraft/);
  assert.match(stdout, /delete/);
});

test('CLI: 未知渠道报错', () => {
  const { code, stdout } = run(['unknown-channel', 'checkLogin']);
  assert.strictEqual(code, 1);
  assert.match(stdout, /未知渠道/);
});

test('CLI: 未知动作报错', () => {
  const { code, stdout } = run(['csdn', 'not-an-action']);
  assert.strictEqual(code, 1);
  assert.match(stdout, /未知动作/);
});

test('CLI: publish 缺少 --file 被拦截（不启动浏览器）', () => {
  const { code, stdout } = run(['csdn', 'publish']);
  assert.strictEqual(code, 1);
  assert.match(stdout, /缺少 --file/);
});

test('CLI: publishDraft 缺少 --file 被拦截', () => {
  const { code, stdout } = run(['csdn', 'publishDraft']);
  assert.strictEqual(code, 1);
  assert.match(stdout, /缺少 --file/);
});

test('CLI: delete 缺少定位参数被拦截', () => {
  const { code, stdout } = run(['csdn', 'delete']);
  assert.strictEqual(code, 1);
  assert.match(stdout, /删除需要提供/);
});

test('CLI: test 未知场景报错', () => {
  const { code, stdout } = run(['csdn', 'test', 'unknown']);
  assert.strictEqual(code, 1);
  assert.match(stdout, /未知测试场景/);
});