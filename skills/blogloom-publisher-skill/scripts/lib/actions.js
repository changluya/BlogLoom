'use strict';

/** 各 action 的具体实现：返回 { ok, action, data, error, code }。 */

const { log } = require('./cli-io');
const { runWithLogin } = require('./runner');
const { buildPublishPayload, resolveDeleteTarget } = require('./payload');

// 跨渠道通用成功态；渠道可用 successStatuses 追加自己的终态
const PUBLISH_OK_STATUSES = ['PUBLISHED', 'DRY_RUN', 'DRAFT_SAVED'];
const okStatusesFor = (channel) => PUBLISH_OK_STATUSES.concat(channel.successStatuses || []);

/** publish / publishDraft 共用：进入编辑器填标题正文，按模式保存草稿或发布。 */
async function runContentAction(channel, opts, payload, mode) {
  return runWithLogin(channel, opts, async (page, ctx) => {
    // 渠道可能在新标签页打开编辑器，enterEditor 返回实际编辑页
    const editorPage = (await channel.enterEditor(page, ctx)) || page;
    // 供渠道在写正文时读取渠道专属参数（如正文顶部插卡）
    ctx.payload = payload;

    await channel.setTitle(editorPage, payload.title);
    if (typeof channel.setAuthor === 'function') {
      await channel.setAuthor(editorPage, payload.author);
    }
    const contentResult = await channel.setContent(editorPage, payload.content, ctx);
    const base = {
      title: payload.title,
      tags: payload.tags,
      column: payload.column,
      contentLength: contentResult.length,
      contentStrategy: contentResult.strategy,
    };

    if (mode === 'draft') {
      if (opts.dryRun) {
        return { ...base, status: 'DRY_RUN', message: '已填写完成，未保存草稿（--dry-run）' };
      }
      // 渠道可声明“草稿也要先配置发布项”（如公众号封面/描述/原创/合集/创作来源）
      let draftConfig = {};
      if (channel.configureOnDraft && typeof channel.prepareDraft === 'function') {
        draftConfig = (await channel.prepareDraft(editorPage, payload)) || {};
      }
      const draft = await channel.saveDraft(editorPage);
      return { ...base, status: draft.status, url: draft.url, publishConfig: draftConfig };
    }

    let publishConfig = {};
    if (typeof channel.preparePublish === 'function') {
      publishConfig = (await channel.preparePublish(editorPage, payload)) || {};
    } else {
      if (typeof channel.setTags === 'function') await channel.setTags(editorPage, payload.tags);
      if (typeof channel.setCategory === 'function') await channel.setCategory(editorPage, payload.column);
      if (typeof channel.setSummary === 'function') await channel.setSummary(editorPage, payload.summary);
    }

    if (opts.dryRun) {
      return { ...base, status: 'DRY_RUN', publishConfig, message: '已填写完成，未点击发布（--dry-run）' };
    }

    const result = await channel.publish(editorPage, { draft: opts.draft, payload });
    return { ...base, status: result.status, url: result.url, publishConfig };
  });
}

async function checkLogin(channel, opts) {
  // 默认只检测；加 --auto-login 时，检测到未登录会主动打开浏览器让用户登录
  const { login } = await runWithLogin(channel, opts, async () => null);
  return { ok: true, action: 'checkLogin', data: login, error: null, code: login.loggedIn ? 0 : 2 };
}

async function login(channel, opts) {
  const data = await runWithLogin(
    channel,
    { ...opts, headless: false, autoLogin: true },
    async () => null
  ).then((r) => r.login);
  return {
    ok: data.loggedIn,
    action: 'login',
    data,
    error: data.loggedIn ? null : data.reason,
    code: data.loggedIn ? 0 : 2,
  };
}

async function publish(channel, opts, mode) {
  const payload = buildPublishPayload(channel, opts);
  log(`渠道=${channel.name} 标题="${payload.title}" 标签=[${payload.tags.join(', ')}] 专栏="${payload.column}"`);
  const { result: data } = await runContentAction(channel, opts, payload, mode);
  const ok = okStatusesFor(channel).includes(data.status);
  return {
    ok,
    action: mode === 'draft' ? 'publishDraft' : 'publish',
    data,
    error: ok ? null : '操作结果未知，请到渠道后台确认',
    code: ok ? 0 : 1,
  };
}

async function del(channel, opts) {
  if (typeof channel.deleteBlog !== 'function') {
    throw new Error(`渠道 ${channel.displayName} 暂不支持 delete（请到渠道后台手动删除）`);
  }
  const target = resolveDeleteTarget(opts, opts.params);
  log(`渠道=${channel.name} 删除文章：${target.title || target.id || target.url}`);
  const { result: data } = await runWithLogin(channel, opts, async (page, ctx) =>
    channel.deleteBlog(page, target, ctx)
  );
  const ok = data.status === 'DELETED';
  return { ok, action: 'delete', data, error: null, code: ok ? 0 : 1 };
}

async function testPublish(channel, opts) {
  if (typeof channel.deleteBlog !== 'function') {
    throw new Error(`渠道 ${channel.displayName} 暂不支持 test publish（无 delete 能力，发布不可自动回滚）`);
  }
  const scenario = String(opts.testScenario || 'publish').toLowerCase();
  if (scenario !== 'publish') {
    throw new Error(`未知测试场景: ${scenario}，当前支持: publish`);
  }

  const payload = buildPublishPayload(channel, opts);
  const mode = channel.testMode === 'draft' ? 'draft' : 'publish';
  log(`[test publish] 渠道=${channel.name} 标题="${payload.title}"：${mode === 'draft' ? '发布草稿' : '发布'} → 删除`);
  const { result: data } = await runWithLogin(channel, opts, async (page, ctx) => {
    const editorPage = (await channel.enterEditor(page, ctx)) || page;
    ctx.payload = payload;
    await channel.setTitle(editorPage, payload.title);
    if (typeof channel.setAuthor === 'function') {
      await channel.setAuthor(editorPage, payload.author);
    }
    const contentResult = await channel.setContent(editorPage, payload.content, ctx);

    let publishConfig = {};
    let published;
    let expectedStatus;
    let successStatus;
    if (mode === 'draft') {
      if (channel.configureOnDraft && typeof channel.prepareDraft === 'function') {
        publishConfig = (await channel.prepareDraft(editorPage, payload)) || {};
      }
      const draft = await channel.saveDraft(editorPage);
      published = { status: draft.status, url: draft.url };
      expectedStatus = 'DRAFT_SAVED';
      successStatus = 'DRAFT_SAVED_AND_DELETED';
    } else {
      if (typeof channel.preparePublish === 'function') {
        publishConfig = (await channel.preparePublish(editorPage, payload)) || {};
      }
      published = await channel.publish(editorPage, { payload });
      expectedStatus = 'PUBLISHED';
      successStatus = 'PUBLISHED_AND_DELETED';
    }

    const target = { url: published.url, title: payload.title };
    const match = String(published.url || '').match(/details\/(\d+)/);
    if (match) target.id = match[1];
    const deleted = await channel.deleteBlog(page, target, ctx);
    return {
      status: published.status === expectedStatus && deleted.status === 'DELETED' ? successStatus : 'UNKNOWN',
      mode,
      publish: {
        status: published.status,
        url: published.url,
        contentLength: contentResult.length,
        contentStrategy: contentResult.strategy,
        publishConfig,
      },
      delete: deleted,
    };
  });
  const ok = data.status === 'PUBLISHED_AND_DELETED' || data.status === 'DRAFT_SAVED_AND_DELETED';
  return { ok, action: 'test publish', data, error: ok ? null : '测试链路未完整成功', code: ok ? 0 : 1 };
}

/** 按 actionKey 分发。 */
async function dispatch(channel, opts, actionKey) {
  switch (actionKey) {
    case 'checkLogin':
      return checkLogin(channel, opts);
    case 'login':
      return login(channel, opts);
    case 'publish':
      return publish(channel, opts, 'publish');
    case 'publishDraft':
      return publish(channel, opts, 'draft');
    case 'delete':
      return del(channel, opts);
    case 'test':
      return testPublish(channel, opts);
    default:
      throw new Error(`未知动作: ${actionKey}`);
  }
}

module.exports = { dispatch, runContentAction, PUBLISH_OK_STATUSES };
