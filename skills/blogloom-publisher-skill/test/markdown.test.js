'use strict';

const test = require('node:test');
const assert = require('node:assert');
const path = require('path');
const { parseMarkdown, pickCoverImage, normalizeList, lenientJsonParse, countImages, estimateImageWaitMs } = require('../scripts/lib/markdown');

const FIXTURE = '/Users/edy/changlu_workspace/mymd/我的开源项目/BlogLoom/codes/Maven插件—05、批量添加License头声明spotless-maven-plugin.md';

test('parseMarkdown: 解析顶部 SOP 元数据并按映射取值', () => {
  const blog = parseMarkdown(FIXTURE);
  assert.strictEqual(blog.title, 'Maven插件—05：批量添加License头声明spotless-maven-plugin');
  assert.deepStrictEqual(blog.tags, ['Maven', 'spotless', 'licenseHeader', 'license-maven-plugin', '代码规范', 'License']);
  assert.strictEqual(blog.category, 'Maven');
  assert.deepStrictEqual(blog.columns, ['项目管理工具', 'Maven&Gradle']);
  assert.strictEqual(blog.createTime, '2026-09-21 18:30:00');
  assert.strictEqual(blog.knowledgeBasePath, '/0x05、Java后端/01、Java基础知识点/项目管理工具/maven/maven插件');
  assert.ok(blog.summary.startsWith('介绍如何批量为项目中的每个Java类'));
});

test('parseMarkdown: 元数据代码块从正文移除', () => {
  const blog = parseMarkdown(FIXTURE);
  assert.ok(!blog.content.includes('"knowledgeBasePath"'));
  assert.ok(blog.content.includes('# 一、认识License头声明'));
});

test('parseMarkdown: 提取 coverImg 封面', () => {
  const blog = parseMarkdown(FIXTURE);
  assert.strictEqual(blog.cover, 'https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202609212201801.png');
});

test('pickCoverImage: 只认 alt=coverImg，取更靠前者', () => {
  assert.strictEqual(pickCoverImage('![普通](a.png)\n![coverImg](b.png)'), 'b.png');
  assert.strictEqual(pickCoverImage('<img alt="coverImg" src="c.png">'), 'c.png');
  assert.strictEqual(pickCoverImage('![普通](a.png)'), '');
});

test('normalizeList: 支持中英文逗号分隔', () => {
  assert.deepStrictEqual(normalizeList('a, b，c、d'), ['a', 'b', 'c', 'd']);
  assert.deepStrictEqual(normalizeList(['x', ' y ']), ['x', 'y']);
  assert.deepStrictEqual(normalizeList(null), []);
});

test('parseMarkdown: 文件不存在时报错', () => {
  assert.throws(() => parseMarkdown('/no/such/file.md'), /文件不存在/);
});

test('lenientJsonParse: 容忍 SOP 样例的全角冒号与尾随逗号', () => {
  const raw = '{\n "title": "T",\n "columns"\uff1a "A, B",\n "knowledgeBasePath": "/x",\n}';
  const parsed = lenientJsonParse(raw);
  assert.strictEqual(parsed.title, 'T');
  assert.strictEqual(parsed.columns, 'A, B');
  assert.strictEqual(parsed.knowledgeBasePath, '/x');
});

test('parseMarkdown: 保留额外字段并支持 category/categories 与 metadata 封面', () => {
  const os = require('os');
  const fs = require('fs');
  const path = require('path');
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'blogloom-'));
  const file = path.join(dir, 'x.md');
  fs.writeFileSync(
    file,
    '```json\n{"title":"X","categories":"C1,C2","column":"Col","cover":"https://x/cover.png","customField":"keep"}\n```\n\n正文\n'
  );
  const blog = parseMarkdown(file);
  assert.deepStrictEqual(blog.categories, ['C1', 'C2']);
  assert.deepStrictEqual(blog.columns, ['Col']);
  assert.strictEqual(blog.cover, 'https://x/cover.png');
  assert.strictEqual(blog.extra.customField, 'keep');
});

test('countImages: 统计 Markdown 与 HTML 图片', () => {
  assert.strictEqual(countImages('![a](1.png) ![b](2.png) <img src="3.png">'), 3);
  assert.strictEqual(countImages('纯文本'), 0);
});

test('estimateImageWaitMs: 默认 2s，每满 5 张多 3s', () => {
  assert.strictEqual(estimateImageWaitMs('无图'), 0);
  assert.strictEqual(estimateImageWaitMs('![a](1.png)'), 5000); // 1 张 → 2s + 3s
  const five = Array.from({ length: 5 }, (_, i) => `![a](x${i}.png)`).join('\n');
  assert.strictEqual(estimateImageWaitMs(five), 5000); // 5 张 → 2s + 3s
  const six = Array.from({ length: 6 }, (_, i) => `![a](x${i}.png)`).join('\n');
  assert.strictEqual(estimateImageWaitMs(six), 8000); // 6 张 → 2s + 2*3s
});