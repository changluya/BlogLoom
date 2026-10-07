'use strict';

/**
 * 微信公众号渠道单测：对每个脚本工具（checkLogin / enterEditor / setTitle / setAuthor /
 * setContent / preparePublish / publish / saveDraft）用 mock page 做行为验证。
 */

const test = require('node:test');
const assert = require('node:assert');

const gzh = require('../../scripts/channels/gzh');
const S = gzh.SELECTORS;
const { MockPage, el } = require('../helpers/mock-page');

function ctxWith({ cookies = [], convertMarkdown = async () => ({ converted: true }), payload = null } = {}) {
  return { ...gzh, cookies: async () => cookies, convertMarkdown, payload };
}
const cookie = (name) => ({ name, value: 'x' });

// ---------------------------------------------------------------------------
// checkLogin
// ---------------------------------------------------------------------------
test('checkLogin: 首页出现账号名时判定已登录', async () => {
  const page = new MockPage([el(S.accountName, { text: '长路Java' })]);
  const result = await gzh.checkLogin(page, ctxWith());
  assert.strictEqual(result.loggedIn, true);
  assert.strictEqual(result.user, '长路Java');
});

test('checkLogin: 未登录（仅登录入口，无关键 Cookie）', async () => {
  const page = new MockPage([el(S.jumpUrl, { text: '登录' })]);
  const result = await gzh.checkLogin(page, ctxWith());
  assert.strictEqual(result.loggedIn, false);
  assert.strictEqual(result.loginEntryVisible, true);
});

test('checkLogin: 出现登录入口时即使有 wxuin 等 Cookie 也判定未登录（防假阳性）', async () => {
  const page = new MockPage([el(S.jumpUrl, { text: '登录' })]);
  const result = await gzh.checkLogin(page, ctxWith({ cookies: [cookie('wxuin'), cookie('ua_id')] }));
  assert.strictEqual(result.loggedIn, false);
  assert.deepStrictEqual(result.cookieNames, []);
});

test('checkLogin: 无账号名但命中真实登录 Cookie（slave_sid）时判定已登录', async () => {
  const page = new MockPage([]);
  const result = await gzh.checkLogin(page, ctxWith({ cookies: [cookie('slave_sid'), cookie('slave_user')] }));
  assert.strictEqual(result.loggedIn, true);
  assert.deepStrictEqual(result.cookieNames.sort(), ['slave_sid', 'slave_user']);
});

// ---------------------------------------------------------------------------
// enterEditor
// ---------------------------------------------------------------------------
test('prepareLogin: 点首页「登录」入口进入扫码页', async () => {
  const page = new MockPage([el(S.jumpUrl, { text: '登录' })]);
  await gzh.prepareLogin(page);
  assert.ok(page.called(S.jumpUrl, 'click'));
});

test('enterEditor: 内容管理 → 草稿箱 → 新的创作 → 文章，返回编辑页', async () => {
  const editor = el(S.editor, { visible: false, text: '编辑区' });
  const addBtn = el(S.createArticleItem, {
    text: '文章',
    onClick: () => {
      editor.visible = true;
    },
  });
  const page = new MockPage([
    el(S.menuCreate),
    el(S.subMenuItem, { children: [el('a')] }),
    el(S.newCreateButton, { text: '新的创作' }),
    addBtn,
    editor,
  ]);
  const editorPage = await gzh.enterEditor(page, gzh);
  assert.strictEqual(editorPage, page);
  assert.ok(page.called(S.menuCreate, 'click'));
  assert.ok(page.called('a', 'click'));
  assert.ok(page.called(S.newCreateButton, 'click'));
  assert.ok(page.called(S.createArticleItem, 'click'));
});

test('enterEditor: 未找到「新的创作」入口时报错', async () => {
  const page = new MockPage([el(S.menuCreate), el(S.subMenuItem, { children: [el('a')] })]);
  await assert.rejects(() => gzh.enterEditor(page, gzh), /新的创作/);
});

// ---------------------------------------------------------------------------
// setTitle / setAuthor / setContent
// ---------------------------------------------------------------------------
test('setTitle: 写入标题输入框', async () => {
  const page = new MockPage([el(S.titleInput, { value: '' })]);
  const result = await gzh.setTitle(page, '我的公众号文章');
  assert.strictEqual(result.value, '我的公众号文章');
  assert.ok(page.called(S.titleInput, 'click'));
});

test('setAuthor: 写入作者栏', async () => {
  const page = new MockPage([el(S.authorInput, { value: '' })]);
  const result = await gzh.setAuthor(page, '长路');
  assert.strictEqual(result.value, '长路');
  assert.ok(page.called(S.authorInput, 'fill'));
});

test('setContent: 经样式转换后粘贴进 ProseMirror', async () => {
  const page = new MockPage([el(S.editor, { text: '渲染后的公众号正文' })]);
  const result = await gzh.setContent(page, '# 正文', ctxWith());
  assert.strictEqual(result.strategy, 'md-style-paste');
  assert.ok(result.length > 0);
  assert.ok(page.called(S.editor, 'click'));
  assert.ok(page.calls.some((c) => c.type === 'keyPress'));
});

test('setContent: 粘贴后点「继续插入」确认框并写入正文', async () => {
  let clicked = 0;
  const dialog = el(S.continueInsert, { text: '继续插入', onClick: () => { clicked += 1; dialog.removed = true; } });
  const page = new MockPage([el(S.editor, { text: '渲染后的公众号正文' }), dialog]);
  const result = await gzh.setContent(page, '# 正文', ctxWith());
  assert.strictEqual(result.strategy, 'md-style-paste');
  assert.ok(clicked >= 1);
});

// ---------------------------------------------------------------------------
// preparePublish
// ---------------------------------------------------------------------------
function buildPublishPage() {
  const originalDialog = el('.weui-desktop-dialog', {
    visible: true,
    children: [
      el(S.formCheckbox),
      el('label', { text: '文字原创' }),
      el('label', { text: '我已阅读' }),
      el('button', {
        text: '确定',
        onClick: () => {
          originalDialog.visible = false;
        },
      }),
    ],
  });
  const collectionDialog = el('.weui-desktop-dialog', {
    visible: true,
    children: [
      el(S.collectionOption, { text: '项目管理工具' }),
      el(S.collectionOption, { text: 'SQLParser解析器' }),
      el(S.btnWrp, {
        onClick: () => {
          collectionDialog.visible = false;
        },
      }),
    ],
  });
  const claimDialog = el('.weui-desktop-dialog', {
    visible: true,
    children: [
      el('.weui-desktop-form__check-label', { text: '个人观点，仅供参考' }),
      el('button', {
        text: '确认',
        onClick: () => {
          claimDialog.visible = false;
        },
      }),
    ],
  });
  const elements = [
    el(S.afterArea, {
      children: [
        el(S.coverDescriptionArea, {
          children: [el(S.coverArea), el(S.descTextarea, { value: '' })],
        }),
      ],
    }),
    el(S.selectCoverFromContent),
    el(S.imgCropPanel, { children: [el(S.contentImgItem)] }),
    el(S.dialogFt, { children: [el(S.btnWrp), el(S.btnWrp)] }),
    el(S.coverPreview, { attrs: { style: 'background-image: url("https://x/cover.png")' } }),
    el(S.originalBox, { children: [el(S.original)] }),
    originalDialog,
    el(S.articleTagsArea, { children: [el(S.articleTagsLabel)] }),
    collectionDialog,
    claimDialog,
    el(S.claimSourceArea, { children: [el(S.claimSourceDesc)] }),
    el(S.claimSourceOption, { text: '其他' }),
    el(S.claimSourceOption, { text: '个人观点，仅供参考' }),
  ];
  return new MockPage(elements);
}

test('preparePublish: 配置封面/摘要/原创/合集/创作来源', async () => {
  const page = buildPublishPage();
  const config = await gzh.preparePublish(page, {
    summary: '这是一段摘要',
    collection: '项目管理工具',
  });
  assert.strictEqual(config.coverSet, true);
  assert.strictEqual(config.summarySet, true);
  assert.strictEqual(config.original, true);
  assert.strictEqual(config.collection, '项目管理工具');
  assert.strictEqual(config.source, '个人观点，仅供参考');
  assert.ok(page.called(S.selectCoverFromContent, 'click'));
  assert.ok(page.called(S.original, 'click'));
  assert.ok(page.called(S.articleTagsLabel, 'click'));
  assert.ok(page.called(S.claimSourceDesc, 'click'));
});

test('chooseCollection: 合集不存在时返回空字符串（不创建）', async () => {
  const page = buildPublishPage();
  const matched = await gzh.chooseCollection(page, '不存在的合集');
  assert.strictEqual(matched, '');
});

test('chooseSource: 默认选中「个人观点，仅供参考」', async () => {
  const page = buildPublishPage();
  const source = await gzh.chooseSource(page);
  assert.strictEqual(source, '个人观点，仅供参考');
});

// ---------------------------------------------------------------------------
// publish / saveDraft
// ---------------------------------------------------------------------------
function buildMassSendPage() {
  const elements = [
    el(S.bottomBar, { children: [el(S.massSend)] }),
    el(S.massSendDialog, {
      visible: true,
      children: [el(S.massSendSwitch), el(S.dialogFt, { children: [el(S.btnPrimary)] })],
    }),
    el(S.doubleCheckDialog, { visible: true, children: [el(S.btnPrimary)] }),
  ];
  return new MockPage(elements);
}

test('publish: 关闭群发 → 群发弹窗发表 → 二次确认 → SUBMITTED', async () => {
  const page = buildMassSendPage();
  const result = await gzh.publish(page, { payload: { groupSend: false } });
  assert.strictEqual(result.status, 'SUBMITTED');
  assert.ok(page.called(S.massSend, 'click'));
  assert.ok(page.called(S.massSendSwitch, 'click'));
  assert.ok(page.called(S.btnPrimary, 'click'));
});

test('publish: 群发时保留默认开关（不点关闭）', async () => {
  const page = buildMassSendPage();
  const result = await gzh.publish(page, { payload: { groupSend: true } });
  assert.strictEqual(result.status, 'SUBMITTED');
  assert.strictEqual(page.called(S.massSendSwitch, 'click'), false);
});

test('saveDraft: 点「保存为草稿」返回 DRAFT_SAVED', async () => {
  const page = new MockPage([el(S.saveButton, { text: '保存为草稿' })]);
  const result = await gzh.saveDraft(page);
  assert.strictEqual(result.status, 'DRAFT_SAVED');
  assert.ok(page.called(S.saveButton, 'click'));
});

// ---------------------------------------------------------------------------
// deleteBlog
// ---------------------------------------------------------------------------
function buildDraftListPage({ removeOnConfirm = true } = {}) {
  const row = el(S.draftItem, {
    text: 'Maven插件—05：批量添加License头声明spotless-maven-plugin',
    children: [el('.weui-desktop-link', { text: '删除' })],
  });
  const confirm = el('.weui-desktop-popover button.weui-desktop-btn_primary', {
    text: '删除',
    onClick: () => {
      if (removeOnConfirm) row.removed = true;
    },
  });
  const page = new MockPage([
    el(S.menuCreate),
    el(S.subMenuItem, { children: [el('a')] }),
    row,
    confirm,
  ]);
  return { page, row };
}

test('deleteBlog: 草稿箱定位卡片 → 行内「删除」→ 确认后校验已删除', async () => {
  const { page } = buildDraftListPage();
  const result = await gzh.deleteBlog(page, { title: 'Maven插件—05' });
  assert.strictEqual(result.status, 'DELETED');
  assert.ok(page.called(S.draftDeleteLink, 'click'));
  assert.ok(page.called('.weui-desktop-popover button.weui-desktop-btn_primary', 'click'));
});

test('deleteBlog: 按 appmsgid 定位卡片', async () => {
  const { page } = buildDraftListPage();
  const result = await gzh.deleteBlog(page, { id: '100008709' });
  assert.strictEqual(result.status, 'DELETED');
});

test('deleteBlog: 未确认删除（卡片仍在）返回 DELETE_UNVERIFIED', async () => {
  const { page } = buildDraftListPage({ removeOnConfirm: false });
  const result = await gzh.deleteBlog(page, { title: 'Maven插件—05' });
  assert.strictEqual(result.status, 'DELETE_UNVERIFIED');
});

test('deleteBlog: 草稿箱中找不到文章时报错', async () => {
  const page = new MockPage([el(S.menuCreate), el(S.subMenuItem, { children: [el('a')] })]);
  await assert.rejects(() => gzh.deleteBlog(page, { title: '不存在的文章' }), /未在草稿箱中找到/);
});

// ---------------------------------------------------------------------------
// augmentPayload：渠道专属字段（核心 payload 层不感知）
// ---------------------------------------------------------------------------
test('augmentPayload: 解析 gzh 专属参数与元数据兜底', () => {
  const base = { column: '项目管理工具', extra: {} };
  const out = gzh.augmentPayload(base, {
    opts: { author: '长路', wechatName: '长路Java', collection: 'SQLParser解析器', groupSend: true },
    params: {},
    blog: { extra: {} },
  });
  assert.deepStrictEqual(out, {
    author: '长路',
    wechatName: '长路Java',
    collection: 'SQLParser解析器',
    groupSend: true,
  });
});

test('augmentPayload: 缺省时回落到 conf/gzh.conf 默认值', () => {
  const { loadChannelConf } = require('../../scripts/lib/channel-conf');
  const CONF = loadChannelConf('gzh');
  const out = gzh.augmentPayload({ column: 'SQLParser', extra: {} }, { opts: {}, params: {}, blog: { extra: {} } });
  assert.strictEqual(out.groupSend, CONF.groupSend === true);
  assert.strictEqual(out.collection, CONF.collection || '');
  assert.strictEqual(out.author, CONF.author || '');
});

test('augmentPayload: 元数据 extra 提供 username/wechatName/collection', () => {
  const blog = { extra: { username: '长路', wechatName: '长路Java', collection: 'SQLParser', isGroupSend: 'true' } };
  const out = gzh.augmentPayload({ column: 'X', extra: blog.extra }, { opts: {}, params: {}, blog });
  assert.strictEqual(out.author, '长路');
  assert.strictEqual(out.wechatName, '长路Java');
  assert.strictEqual(out.collection, 'SQLParser');
  assert.strictEqual(out.groupSend, true);
});
