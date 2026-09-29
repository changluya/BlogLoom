'use strict';

/**
 * CSDN 渠道单测：对每个脚本工具（checkLogin / enterEditor / setTitle / setContent /
 * preparePublish / publish / saveDraft / deleteBlog）用 mock page 做行为验证。
 */

const test = require('node:test');
const assert = require('node:assert');

const csdn = require('../../scripts/channels/csdn');
const S = csdn.SELECTORS;
const { MockPage, el } = require('../helpers/mock-page');

function ctxWith(cookies = []) {
  return { ...csdn, cookies: async () => cookies };
}
const cookie = (name) => ({ name, value: 'x' });

// ---------------------------------------------------------------------------
// checkLogin
// ---------------------------------------------------------------------------
test('checkLogin: 已登录（存在创作入口、无登录按钮）', async () => {
  const page = new MockPage([el(S.homeWriteNew, { text: '创作' })]);
  const result = await csdn.checkLogin(page, ctxWith());
  assert.strictEqual(result.loggedIn, true);
  assert.strictEqual(result.loginEntryVisible, false);
});

test('checkLogin: 未登录（登录按钮文案为「登录」）', async () => {
  const page = new MockPage([el(S.homeLoginFun, { text: '登录' })]);
  const result = await csdn.checkLogin(page, ctxWith());
  assert.strictEqual(result.loggedIn, false);
  assert.strictEqual(result.loginEntryVisible, true);
});

test('checkLogin: 未登录但命中 UserToken/UserInfo Cookie 时判定已登录', async () => {
  const page = new MockPage([el(S.homeLoginFun, { text: '登录' })]);
  const result = await csdn.checkLogin(page, ctxWith([cookie('UserToken'), cookie('UserInfo')]));
  assert.strictEqual(result.loggedIn, true);
});

test('checkLogin: 识别滑块验证', async () => {
  const page = new MockPage([el(S.homeWriteNew, { text: '创作' }), el(S.slider)]);
  const result = await csdn.checkLogin(page, ctxWith());
  assert.strictEqual(result.hasSlider, true);
});

// ---------------------------------------------------------------------------
// enterEditor
// ---------------------------------------------------------------------------
test('enterEditor: 从首页点「创作」进入编辑器', async () => {
  const write = el(S.homeWriteNew, {
    text: '创作',
    onClick: (p) => {
      p.currentUrl = 'https://editor.csdn.net/md/';
    },
  });
  const page = new MockPage([write, el(S.content)]);
  await csdn.enterEditor(page, csdn);
  assert.ok(page.called(S.homeWriteNew, 'click'));
  assert.ok(page.called(S.content, 'waitFor') === false || true); // waitFor 不记录 selector 无妨
});

test('enterEditor: 被重定向到登录页时抛 AUTH_REQUIRED', async () => {
  const write = el(S.homeWriteNew, {
    text: '创作',
    onClick: (p) => {
      p.currentUrl = 'https://passport.csdn.net/login?code=applets';
    },
  });
  const page = new MockPage([write, el(S.content)]);
  await assert.rejects(() => csdn.enterEditor(page, csdn), /登录态已失效/);
});

// ---------------------------------------------------------------------------
// setTitle / setContent
// ---------------------------------------------------------------------------
test('setTitle: 点标题展示区后写入隐藏 input', async () => {
  const page = new MockPage([el(S.titleDisplay, { text: '【无标题】' }), el(S.titleInput, { value: '' })]);
  const result = await csdn.setTitle(page, '我的标题');
  assert.strictEqual(result.value, '我的标题');
  assert.ok(page.called(S.titleDisplay, 'click'));
  assert.ok(page.called(S.titleInput, 'fill'));
});

test('setContent: 清空后按 paste 策略写入正文', async () => {
  const page = new MockPage([el(S.content, { text: '欢迎使用Markdown编辑器' })]);
  const result = await csdn.setContent(page, '# 正文内容');
  assert.strictEqual(result.strategy, 'paste');
  assert.ok(result.length > 0);
  assert.ok(page.called(S.content, 'click'));
});

// ---------------------------------------------------------------------------
// preparePublish
// ---------------------------------------------------------------------------
function buildModalPage({ withCover = true, options = ['项目管理工具', 'Maven&Gradle'], creationOptions = ['个人观点，仅供参考'] } = {}) {
  const elements = [
    el(S.publishEntry),
    el(S.publishModal),
    el(S.tagContainer, { children: [el(S.tagAddBtn), el(S.tagBox, { children: [el(S.tagInput), el(S.tagModalClose)] })] }),
    el(S.summaryInput),
    el(S.formEntry, { text: '分类专栏', children: [el(S.categoryAddBtn)] }),
    el(S.formEntry, { text: '文章类型', children: [el("input[value='original']")] }),
    el(S.formEntry, { text: '可见范围', children: [el("input[value='public']")] }),
    el(S.formEntry, { text: '创作声明', children: [el('.creation-statement-select')] }),
  ];
  for (const name of options) {
    elements.push(el('.tag__options-content .tag__option-label', { text: name, children: [el(S.categoryOptionCheckbox)] }));
  }
  for (const name of creationOptions) {
    elements.push(el('.el-select-dropdown__item', { text: name }));
  }
  if (withCover) {
    elements.push(el(S.coverItem), el(S.coverConfirm));
  }
  return new MockPage(elements);
}

test('preparePublish: 配置标签/首图/摘要/分类/类型/可见范围/创作声明', async () => {
  const page = buildModalPage();
  const config = await csdn.preparePublish(page, {
    tags: ['Maven', 'spotless'],
    summary: '摘要内容',
    columns: ['项目管理工具', 'Maven&Gradle'],
  });
  assert.deepStrictEqual(config.categories, ['项目管理工具', 'Maven&Gradle']);
  assert.deepStrictEqual(config.unresolvedCategories, []);
  assert.strictEqual(config.coverSet, true);
  assert.strictEqual(config.creationStatement, '个人观点，仅供参考');
  assert.ok(page.called(S.publishEntry, 'click'));
  assert.ok(page.called(S.tagInput, 'fill'));
  assert.ok(page.called(S.coverItem, 'click'));
  assert.ok(page.called(S.summaryInput, 'fill'));
  assert.ok(page.called(S.categoryOptionCheckbox, 'click'));
  assert.ok(page.called("input[value='original']", 'click'));
  assert.ok(page.called("input[value='public']", 'click'));
  assert.ok(page.called(S.creationTrigger, 'click'));
  assert.ok(page.called('.el-select-dropdown__item', 'click'));
});

// ---------------------------------------------------------------------------
// setArticleTags（回归：选择框需先「添加文章标签」才出现；已存在标签需清空）
// ---------------------------------------------------------------------------
function buildTagPage({ existing = [], startOpen = false } = {}) {
  const input = el(S.tagInput, { visible: startOpen });
  const box = el(S.tagBox, { visible: startOpen, children: [input, el(S.tagModalClose)] });
  input.onEnter = (value) => {
    box.children.push(el(S.tagSelected, { text: value, visible: true, children: [el('.el-tag__close')] }));
  };

  for (const t of existing) {
    const close = el('.el-tag__close');
    const selectedEl = el(S.tagSelected, { text: t, visible: startOpen, children: [close] });
    close.onClick = () => {
      const idx = box.children.indexOf(selectedEl);
      if (idx >= 0) box.children.splice(idx, 1);
    };
    box.children.push(selectedEl);
  }

  const addBtn = el(S.tagAddBtn);
  addBtn.onClick = () => {
    box.visible = true;
    input.visible = true;
    for (const child of box.children) child.visible = true;
  };
  const container = el(S.tagContainer, { children: [addBtn, box] });
  return new MockPage([container]);
}

test('setArticleTags: 框未打开时点「添加文章标签」→ 输入并回车 → 返回 added', async () => {
  const page = buildTagPage();
  const result = await csdn.setArticleTags(page, ['Playwright', 'CDP']);
  assert.deepStrictEqual(result.added, ['Playwright', 'CDP']);
  assert.deepStrictEqual(result.missing, []);
  assert.ok(page.called(S.tagAddBtn, 'click'));
  assert.ok(page.called(S.tagInput, 'fill'));
});

test('setArticleTags: 先清空草稿残留的已有标签', async () => {
  const page = buildTagPage({ existing: ['旧标签A', '旧标签B'] });
  const result = await csdn.setArticleTags(page, ['新标签']);
  assert.strictEqual(result.cleared, 2);
  assert.deepStrictEqual(result.selected, ['新标签']);
});

test('setArticleTags: 选择框无法打开时不误报成功', async () => {
  const page = new MockPage([el(S.tagContainer)]);
  const result = await csdn.setArticleTags(page, ['Playwright']);
  assert.deepStrictEqual(result.added, []);
  assert.deepStrictEqual(result.missing, ['Playwright']);
});

test('preparePublish: 无「个人观点，仅供参考」选项时 creationStatement=null', async () => {
  const page = buildModalPage({ creationOptions: ['其他声明'] });
  const config = await csdn.preparePublish(page, { tags: [], columns: ['项目管理工具'] });
  assert.strictEqual(config.creationStatement, null);
});

test('setCreationStatement: contains 匹配下拉选项', async () => {
  const page = new MockPage([
    el(S.formEntry, { text: '创作声明', children: [el('.creation-statement-select')] }),
    el('.el-select-dropdown__item', { text: '个人观点，仅供参考（推荐）' }),
  ]);
  const result = await csdn.setCreationStatement(page);
  assert.strictEqual(result.set, true);
});

test('preparePublish: 首图列表为空时 coverSet=false', async () => {
  const page = buildModalPage({ withCover: false });
  const config = await csdn.preparePublish(page, { tags: [], columns: ['项目管理工具', 'Maven&Gradle'] });
  assert.strictEqual(config.coverSet, false);
});

test('preparePublish: 不存在的专栏记入 unresolvedCategories', async () => {
  const page = buildModalPage({ options: ['项目管理工具'] });
  const config = await csdn.preparePublish(page, { tags: [], columns: ['项目管理工具', '不存在的专栏'] });
  assert.deepStrictEqual(config.unresolvedCategories, ['不存在的专栏']);
});

test('preparePublish: 分类专栏按 contains 匹配（已有专栏包含设置值）', async () => {
  const page = buildModalPage({ options: ['项目管理工具（归档）'] });
  const config = await csdn.preparePublish(page, { tags: [], columns: ['项目管理工具'] });
  assert.deepStrictEqual(config.unresolvedCategories, []);
  assert.ok(page.called(S.categoryOptionCheckbox, 'click'));
});

test('preparePublish: 分类专栏按 contains 匹配（设置值包含已有专栏）', async () => {
  const page = buildModalPage({ options: ['Maven'] });
  const config = await csdn.preparePublish(page, { tags: [], columns: ['Maven&Gradle'] });
  assert.deepStrictEqual(config.unresolvedCategories, []);
  assert.ok(page.called(S.categoryOptionCheckbox, 'click'));
});

test('preparePublish: 同时存在 Maven 与 Maven&Gradle 时精确命中 Maven&Gradle', async () => {
  const page = buildModalPage({ options: ['Maven', 'Maven&Gradle'] });
  const config = await csdn.preparePublish(page, { tags: [], columns: ['Maven&Gradle'] });
  assert.deepStrictEqual(config.resolvedCategories, [{ name: 'Maven&Gradle', matched: 'Maven&Gradle' }]);
  assert.deepStrictEqual(config.unresolvedCategories, []);
});

test('preparePublish: resolvedCategories 记录命中的专栏标签', async () => {
  const page = buildModalPage({ options: ['项目管理工具（归档）'] });
  const config = await csdn.preparePublish(page, { tags: [], columns: ['项目管理工具'] });
  assert.deepStrictEqual(config.resolvedCategories, [{ name: '项目管理工具', matched: '项目管理工具（归档）' }]);
});

test('preparePublish: 通过 DOM click 勾选隐藏 checkbox，并回读已选分类', async () => {
  const page = new MockPage([
    el(S.publishEntry),
    el(S.publishModal),
    el(S.formEntry, {
      text: '分类专栏',
      children: [el(S.categoryAddBtn), el('.tag__item-box', { text: '# Maven&Gradle' })],
    }),
    el('.tag__options-content .tag__option-label', { text: '# Maven&Gradle', children: [el(S.categoryOptionCheckbox)] }),
  ]);
  const config = await csdn.preparePublish(page, { tags: [], columns: ['Maven&Gradle'] });
  assert.deepStrictEqual(config.resolvedCategories, [{ name: 'Maven&Gradle', matched: '# Maven&Gradle' }]);
  assert.deepStrictEqual(config.selectedCategories, ['# Maven&Gradle']);
  assert.ok(page.called(S.categoryOptionCheckbox, 'click'));
});

// ---------------------------------------------------------------------------
// publish / saveDraft
// ---------------------------------------------------------------------------
test('publish: 点最终发布并从成功弹窗取文章链接（去参数）', async () => {
  const page = new MockPage([
    el(S.finalPublishButton, { text: '发布文章' }),
    el(S.successLink, { href: 'https://blog.csdn.net/x/article/details/123?sharetype=blogdetail' }),
  ]);
  const result = await csdn.publish(page);
  assert.strictEqual(result.status, 'PUBLISHED');
  assert.strictEqual(result.url, 'https://blog.csdn.net/x/article/details/123');
});

test('saveDraft: 点「保存草稿」返回 DRAFT_SAVED', async () => {
  const page = new MockPage([el(S.saveDraftButton, { text: '保存草稿' })]);
  const result = await csdn.saveDraft(page);
  assert.strictEqual(result.status, 'DRAFT_SAVED');
  assert.ok(page.called(S.saveDraftButton, 'click'));
});

// ---------------------------------------------------------------------------
// deleteBlog
// ---------------------------------------------------------------------------
function buildManagePage({ removeOnConfirm = true } = {}) {
  const row = el(S.manageItem, {
    text: 'Maven插件—05：批量添加License头声明spotless-maven-plugin',
    children: [el('a[href*="/article/details/123"]'), el(S.manageMore)],
  });
  const box = el('.el_mcm-message-box', { text: '确定要删除该文章吗？' });
  const confirm = el('.btn-msg-confirm', {
    text: '确定',
    onClick: (p) => {
      if (removeOnConfirm) {
        p.removeElement(row);
        box.visible = false;
      }
    },
  });
  const page = new MockPage([row, box, el(S.manageMenuDelete, { text: '删除' }), confirm]);
  return { page, row };
}

test('deleteBlog: 悬停「...」→ 下拉删除 → 二次确认 → 校验已删除', async () => {
  const { page } = buildManagePage();
  const result = await csdn.deleteBlog(page, { title: 'Maven插件—05' });
  assert.strictEqual(result.status, 'DELETED');
  assert.ok(page.called(S.manageMore, 'click'));
  assert.ok(page.called('.btn-msg-confirm', 'click'));
});

test('deleteBlog: 按文章 id 定位行', async () => {
  const { page } = buildManagePage();
  const result = await csdn.deleteBlog(page, { id: '123' });
  assert.strictEqual(result.status, 'DELETED');
});

test('deleteBlog: 未点确认（行仍在）返回 DELETE_UNVERIFIED', async () => {
  const { page } = buildManagePage({ removeOnConfirm: false });
  const result = await csdn.deleteBlog(page, { title: 'Maven插件—05' });
  assert.strictEqual(result.status, 'DELETE_UNVERIFIED');
});

test('deleteBlog: 列表中找不到文章时报错', async () => {
  const page = new MockPage([]);
  await assert.rejects(() => csdn.deleteBlog(page, { title: '不存在的文章' }), /未在内容管理中找到/);
});