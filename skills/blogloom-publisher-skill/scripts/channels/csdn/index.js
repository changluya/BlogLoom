'use strict';

/**
 * CSDN 渠道适配（初次对接渠道）。
 *
 * 流程参照已跑通的 auto-sync-blog：
 *   1. 首页判定登录态（.toolbar-btn-loginfun 文本是否为「登录」）
 *   2. 首页点「创作」进入编辑器（editor.csdn.net/md）
 *   3. 填标题（先点 .article-bar__title-display 唤出隐藏 input）
 *   4. 清空默认欢迎内容 → 剪贴板粘贴正文
 *   5. 点「发布文章」打开弹窗，在弹窗内配置 标签 / 封面 / 摘要 / 分类专栏 / 类型 / 可见范围
 *   6. 点弹窗底部「发布文章」，从成功弹窗取文章链接
 *
 * 所有 DOM 选择器集中在 ./selectors.js，站点改版时只改那里。
 */

const { writeContent } = require('../../lib/editor');
const { estimateImageWaitMs } = require('../../lib/markdown');
const SELECTORS = require('./selectors');

const MOD = process.platform === 'darwin' ? 'Meta' : 'Control';

async function has(locator) {
  return (await locator.count().catch(() => 0)) > 0;
}

async function clickIf(locator, options = {}) {
  if (await has(locator)) {
    await locator.click({ timeout: options.timeout || 3000, force: options.force }).catch(() => {});
    return true;
  }
  return false;
}

/** 找到包含指定文本的 .form-entry 区块。 */
async function formEntryByText(page, text) {
  const entries = page.locator(SELECTORS.formEntry);
  const count = await entries.count().catch(() => 0);
  for (let i = 0; i < count; i += 1) {
    const entry = entries.nth(i);
    const content = await entry.innerText().catch(() => '');
    if (content.includes(text)) return entry;
  }
  return null;
}

async function checkLogin(page, ctx = {}) {
  await page
    .goto(ctx.homeUrl || 'https://www.csdn.net/', { waitUntil: 'domcontentloaded', timeout: 30000 })
    .catch(() => {});

  const hasSlider = await has(page.locator(SELECTORS.slider).first());
  const loginFun = page.locator(SELECTORS.homeLoginFun).first();
  const loginText = (await has(loginFun)) ? ((await loginFun.innerText().catch(() => '')) || '').trim() : '';
  const writeCount = await page.locator(SELECTORS.homeWriteNew).count().catch(() => 0);

  let loggedIn = !loginText.includes('登录') && (writeCount > 0 || loginText.length > 0);
  if (!loggedIn) {
    const cookies = await ctx.cookies('https://www.csdn.net').catch(() => []);
    const names = cookies.map((c) => c.name);
    loggedIn = names.includes('UserToken') && names.includes('UserInfo');
  }

  return {
    loggedIn,
    user: loggedIn ? loginText : '',
    reason: loggedIn
      ? '检测到 CSDN 登录态'
      : '未检测到 CSDN 登录态，请先执行 csdn login 扫码登录',
    hasSlider,
    loginEntryVisible: loginText.includes('登录'),
  };
}

/** 从首页「创作」进入编辑器。 */
async function enterEditor(page, ctx = {}) {
  if (/editor\.csdn\.net/.test(page.url())) return;

  await page
    .goto(ctx.homeUrl || 'https://www.csdn.net/', { waitUntil: 'domcontentloaded', timeout: 45000 })
    .catch(() => {});

  const write = page.locator(SELECTORS.homeWriteNew).first();
  if (await has(write)) {
    await write.click({ timeout: 5000 }).catch(() => {});
  } else {
    await page
      .goto(ctx.editorUrl || 'https://editor.csdn.net/md/', { waitUntil: 'domcontentloaded', timeout: 45000 })
      .catch(() => {});
  }

  await page.locator(SELECTORS.content).first().waitFor({ state: 'visible', timeout: 30000 });
  await page.waitForTimeout(1500);

  if (/passport\.csdn\.net\/login/i.test(page.url())) {
    const err = new Error(`CSDN 登录态已失效，请重新执行: node scripts/publisher.js csdn login`);
    err.code = 'AUTH_REQUIRED';
    throw err;
  }

  for (const text of ['我知道了', '稍后再说', '取消']) {
    await clickIf(page.locator(`button:has-text("${text}")`).first(), { timeout: 1500 });
  }
}

async function setTitle(page, title) {
  // 标题默认显示为 .article-bar__title-display，点击后才显示 input
  await clickIf(page.locator(SELECTORS.titleDisplay).first(), { timeout: 5000 });
  const input = page.locator(SELECTORS.titleInput).first();
  if (!(await has(input))) {
    const box = page.locator('.article-bar__input-box').first();
    await clickIf(box, { timeout: 5000, force: true });
  }
  await input.waitFor({ state: 'visible', timeout: 10000 });
  await input.click({ timeout: 5000 }).catch(() => {});
  await input.fill(title);
  const actual = (await input.inputValue().catch(() => '')) || '';
  if (actual !== title) {
    // 兜底：全选替换
    await input.click({ timeout: 5000 }).catch(() => {});
    await page.keyboard.press(`${MOD}+A`).catch(() => {});
    await page.keyboard.type(title, { delay: 10 }).catch(() => {});
  }
  return { value: (await input.inputValue().catch(() => '')) || '' };
}

async function setContent(page, markdown) {
  const editor = page.locator(SELECTORS.content).first();
  await editor.waitFor({ state: 'visible', timeout: 20000 });
  await editor.click({ timeout: 5000 });

  // 清空默认欢迎内容
  await page.keyboard.press(`${MOD}+A`).catch(() => {});
  await page.keyboard.press('Backspace').catch(() => {});
  await page.waitForTimeout(300);

  const result = await writeContent(page, SELECTORS.content, markdown, ['paste', 'cdp', 'keyboard']);

  // 正文含图片时（SOP 顶部 coverImg 等），CSDN 需异步上传/解析图片；
  // 按图片数量估算等待时间，图片越多等待越久，再点「发布文章」，
  // 发布弹窗的「已有图片列表」才会出现可供选择的首图。
  const waitMs = estimateImageWaitMs(markdown);
  if (waitMs > 0) {
    await page.waitForTimeout(waitMs);
  }
  return result;
}

/** 打开标签选择框（幂等：输入框已可见即视为已打开）。 */
async function openTagBox(page, container) {
  const input = container.locator(SELECTORS.tagInput).first();
  if (await input.isVisible().catch(() => false)) return true;
  await clickIf(container.locator(SELECTORS.tagAddBtn).first(), { timeout: 3000 });
  try {
    await input.waitFor({ state: 'visible', timeout: 5000 });
  } catch (err) {
    return false;
  }
  return true;
}

/** 回读当前已选标签文案（选择框内的 mark_selection_box_el_tag）。 */
async function readSelectedTags(container) {
  const tags = [];
  const items = container.locator(SELECTORS.tagSelected);
  const count = await items.count().catch(() => 0);
  for (let i = 0; i < count; i += 1) {
    const text = ((await items.nth(i).innerText().catch(() => '')) || '').trim();
    if (text) tags.push(text);
  }
  return tags;
}

async function setArticleTags(page, tags) {
  const result = { requested: tags || [], selected: [], added: [], missing: [], cleared: 0 };
  if (!tags || tags.length === 0) return result;
  const container = page.locator(SELECTORS.tagContainer).first();
  if (!(await has(container))) return result;

  // 1. 打开标签选择框（第一次点击「添加文章标签」）
  const opened = await openTagBox(page, container);
  if (!opened) {
    result.missing = [...tags];
    return result;
  }

  // 2. 清空已有的标签（若草稿/上一次运行残留）
  for (let i = 0; i < 30; i += 1) {
    const del = container.locator(SELECTORS.tagDelete).first();
    if (!(await has(del))) break;
    await del.click({ timeout: 1500 }).catch(() => {});
    await page.waitForTimeout(150);
    result.cleared += 1;
  }

  // 3. 逐个输入 + 回车；每次重新解析输入框，避免异步替换导致丢标签
  for (const tag of tags) {
    let input = container.locator(SELECTORS.tagInput).first();
    if (!(await has(input))) {
      await page.waitForTimeout(300);
      input = container.locator(SELECTORS.tagInput).first();
      if (!(await has(input))) continue;
    }
    await input.click({ timeout: 3000 }).catch(() => {});
    await input.fill(tag).catch(() => {});
    await page.waitForTimeout(250);
    await input.press('Enter').catch(() => page.keyboard.press('Enter').catch(() => {}));
    await page.waitForTimeout(350);
  }

  // 4. 关闭选择框（点关闭按钮；失败则忽略）
  await clickIf(container.locator(SELECTORS.tagModalClose).first(), { timeout: 3000 });
  await page.waitForTimeout(300);

  // 5. 回读并核对实际生效的标签
  result.selected = await readSelectedTags(container);
  const selectedSet = new Set(result.selected);
  for (const tag of tags) {
    if (selectedSet.has(tag)) result.added.push(tag);
    else result.missing.push(tag);
  }
  return result;
}

async function setArticleSummary(page, summary) {
  if (!summary) return;
  const input = page.locator(SELECTORS.summaryInput).first();
  if (!(await has(input))) return;
  await input.fill(summary).catch(() => {});
}

/** 首图：直接选择「已有图片列表」的第一张并确认。 */
async function setCover(page) {
  const item = page.locator(SELECTORS.coverItem).first();
  // 图片列表可能异步加载，短暂等待
  await item.waitFor({ state: 'visible', timeout: 5000 }).catch(() => {});
  if (!(await has(item))) return { coverSet: false };
  await item.click({ force: true }).catch(() => {});
  await page.waitForTimeout(800);
  const confirm = page.locator(SELECTORS.coverConfirm).first();
  if (await has(confirm)) await confirm.click({ force: true }).catch(() => {});
  await page.waitForTimeout(500);
  return { coverSet: true };
}

async function setArticleCategories(page, categories) {
  const unresolved = [];
  const resolved = [];
  if (!categories || categories.length === 0) return { unresolved, resolved };

  const entry = await formEntryByText(page, '分类专栏');
  if (!entry) return { unresolved: categories, resolved };

  for (let i = 0; i < 10; i += 1) {
    const del = entry.locator(SELECTORS.categoryExistingDelete).first();
    if (!(await has(del))) break;
    await del.click({ timeout: 1500 }).catch(() => {});
    await page.waitForTimeout(150);
  }

  await clickIf(entry.locator(SELECTORS.categoryAddBtn).first(), { timeout: 3000 });
  await page.waitForTimeout(400);

  for (const name of categories) {
    // 匹配已有专栏：精确优先；其次「已有专栏包含设置值」；最后「设置值包含已有专栏」。
    // 用评分取最优，避免用宽泛的子类（如 "Maven"）误配 "Maven&Gradle"。
    const options = page.locator('.tag__options-content .tag__option-label');
    const optionCount = await options.count().catch(() => 0);
    let best = null;
    let bestScore = 0;
    for (let i = 0; i < optionCount; i += 1) {
      const option = options.nth(i);
      const label = ((await option.innerText().catch(() => '')) || '').trim();
      if (!label) continue;
      let score = 0;
      if (label === name) score = 100000;
      else if (label.includes(name)) score = 10000 + name.length;
      else if (name.includes(label)) score = 1000 + label.length;
      if (score > bestScore) {
        bestScore = score;
        best = { option, label };
      }
    }

    if (best) {
      // 分类专栏的勾选项是隐藏的 <input type=checkbox>，普通点击无效，需触发 DOM click
      const chk = best.option.locator(SELECTORS.categoryOptionCheckbox).first();
      if (await has(chk)) {
        const already = await chk.evaluate((el) => !!(el && el.checked)).catch(() => false);
        if (!already) await chk.evaluate((el) => el && el.click()).catch(() => {});
      } else {
        await best.option.evaluate((el) => el && el.click()).catch(() => {});
      }
      await page.waitForTimeout(250);
      resolved.push({ name, matched: best.label });
    } else {
      unresolved.push(name);
    }
  }

  // 点击标题区域关闭下拉
  await clickIf(page.locator(SELECTORS.publishModal).first(), { timeout: 2000, force: true });

  // 回读实际已勾选的分类专栏，便于校验是否真的生效
  const selected = [];
  const entryAfter = await formEntryByText(page, '分类专栏');
  if (entryAfter) {
    const items = entryAfter.locator(SELECTORS.categorySelectedItem);
    const n = await items.count().catch(() => 0);
    for (let i = 0; i < n; i += 1) {
      const text = ((await items.nth(i).innerText().catch(() => '')) || '').trim();
      if (text) selected.push(text);
    }
  }
  return { unresolved, resolved, selected };
}

async function setArticleType(page, value) {
  const entry = await formEntryByText(page, '文章类型');
  if (!entry) return;
  const radio = entry.locator(SELECTORS.optionInput(value)).first();
  if (await has(radio)) await radio.click({ force: true }).catch(() => {});
}

async function setArticleVisibility(page, value) {
  const entry = await formEntryByText(page, '可见范围');
  if (!entry) return;
  const radio = entry.locator(SELECTORS.optionInput(value)).first();
  if (await has(radio)) await radio.click({ force: true }).catch(() => {});
}

/** 创作声明：下拉选择「个人观点，仅供参考」（contains 匹配）。 */
async function setCreationStatement(page, text = '个人观点，仅供参考') {
  const entry = await formEntryByText(page, '创作声明');
  if (!entry) return { set: false, reason: '未找到创作声明项' };

  const trigger = entry.locator(SELECTORS.creationTrigger).first();
  if (await has(trigger)) await trigger.click({ force: true }).catch(() => {});
  await page.waitForTimeout(400);

  const options = page.locator(SELECTORS.creationOption);
  const count = await options.count().catch(() => 0);
  for (let i = 0; i < count; i += 1) {
    const option = options.nth(i);
    const label = ((await option.innerText().catch(() => '')) || '').trim();
    if (label && (label.includes(text) || text.includes(label))) {
      await option.click({ force: true }).catch(() => {});
      await page.waitForTimeout(200);
      return { set: true, option: label };
    }
  }
  return { set: false, reason: `未找到创作声明选项：${text}` };
}

/** 点「发布文章」打开弹窗并配置所有发布项（不点最终发布）。 */
async function preparePublish(page, payload = {}) {
  const entryBtn = page.locator(SELECTORS.publishEntry).first();
  await entryBtn.waitFor({ state: 'visible', timeout: 20000 });
  await entryBtn.click();
  await page.locator(SELECTORS.publishModal).first().waitFor({ state: 'visible', timeout: 15000 }).catch(() => {});
  await page.waitForTimeout(1500);

  const tagResult = await setArticleTags(page, payload.tags);
  await setArticleSummary(page, payload.summary);
  await setArticleType(page, 'original');
  await setArticleVisibility(page, 'public');
  const creation = await setCreationStatement(page);
  // 首图列表异步加载，放在最后（分类专栏之前）选取，给缩略图充足加载时间
  const coverResult = await setCover(page);
  // 分类专栏的悬停下拉最后处理，避免其浮层遮挡后续控件
  const categories = (payload.columns && payload.columns.length ? payload.columns : [payload.column]).filter(Boolean);
  const categoryResult = await setArticleCategories(page, categories);

  return {
    categories,
    tags: tagResult.added,
    selectedTags: tagResult.selected,
    missingTags: tagResult.missing,
    resolvedCategories: (categoryResult && categoryResult.resolved) || [],
    unresolvedCategories: (categoryResult && categoryResult.unresolved) || [],
    selectedCategories: (categoryResult && categoryResult.selected) || [],
    coverSet: !!coverResult.coverSet,
    creationStatement: creation.set ? creation.option : null,
  };
}

/** 保存草稿（编辑器顶部「保存草稿」）。 */
async function saveDraft(page) {
  const btn = page.locator(SELECTORS.saveDraftButton).first();
  await btn.waitFor({ state: 'visible', timeout: 15000 });
  await btn.click();
  await page.waitForTimeout(2500);
  return { status: 'DRAFT_SAVED', url: page.url() };
}

/** 在内容管理页定位文章行（按 id 或标题）。 */
function findManageRow(page, target) {
  if (target.id) {
    return page
      .locator(SELECTORS.manageItem)
      .filter({ has: page.locator(`a[href*="/article/details/${target.id}"]`) })
      .first();
  }
  return page.locator(SELECTORS.manageItem).filter({ hasText: target.title }).first();
}

/**
 * 删除博客：进入内容管理页 https://mp.csdn.net/mp_blog/manage，
 * 悬停该行右侧「...」→ 下拉点「删除」→ 弹窗点「确定」，并**校验确实删除成功**。
 */
async function deleteBlog(page, target = {}) {
  const manageUrl = 'https://mp.csdn.net/mp_blog/manage/article';
  await page.goto(manageUrl, { waitUntil: 'domcontentloaded', timeout: 45000 });
  await page.waitForTimeout(6000);

  const row = findManageRow(page, target);
  if (!(await has(row))) {
    throw new Error(`未在内容管理中找到文章：${target.title || target.id || target.url || '(未提供)'}`);
  }

  await row.scrollIntoViewIfNeeded().catch(() => {});
  await row.hover().catch(() => {});
  await page.waitForTimeout(500);

  // 行右侧「...」悬停展开，下拉里有「删除」
  const more = row.locator(SELECTORS.manageMore).first();
  if (!(await has(more))) {
    throw new Error('未找到行右侧「...」入口，页面可能改版，请检查 selectors.js');
  }
  await more.hover().catch(() => {});
  await page.waitForTimeout(400);
  await more.click({ force: true }).catch(() => {});

  // 下拉菜单项渲染在 body 级 portal，可能每行都预渲染一份，需取可见的那个
  const delItem = page.locator(`${SELECTORS.manageMenuDelete}:visible`).filter({ hasText: '删除' }).first();
  try {
    await delItem.waitFor({ state: 'visible', timeout: 5000 });
  } catch (err) {
    await more.click({ force: true }).catch(() => {});
    await delItem.waitFor({ state: 'visible', timeout: 5000 });
  }
  await delItem.click({ force: true, timeout: 5000 });
  await page.waitForTimeout(800);

  // 二次确认弹窗：必须点「确定」才会真正删除
  const confirm = page.locator(SELECTORS.confirmButton).filter({ hasText: /确\s*定|确认/ }).first();
  await confirm.waitFor({ state: 'visible', timeout: 8000 });
  await confirm.click({ timeout: 5000 });
  await page.locator(SELECTORS.confirmBox).first().waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {});
  await page.waitForTimeout(1500);

  // 校验删除结果：该行应已从列表消失
  const remaining = await findManageRow(page, target).count().catch(() => 0);
  if (remaining > 0) {
    return { status: 'DELETE_UNVERIFIED', title: target.title || '', id: target.id || '' };
  }
  return { status: 'DELETED', title: target.title || '', id: target.id || '' };
}

/** 点弹窗底部「发布文章」并抓取文章链接。 */
async function publish(page) {
  const btn = page.locator(SELECTORS.finalPublishButton).filter({ hasText: '发布文章' }).first();
  await btn.waitFor({ state: 'visible', timeout: 15000 });
  await btn.scrollIntoViewIfNeeded().catch(() => {});
  await btn.click({ force: true }).catch(async () => {
    await btn.evaluate((el) => el.click()).catch(() => {});
  });

  const link = page.locator(SELECTORS.successLink).first();
  await link.waitFor({ state: 'attached', timeout: 60000 }).catch(() => {});

  let url = (await link.getAttribute('href').catch(() => '')) || '';
  if (!url) {
    await page.waitForURL(/article\/details\/\d+/, { timeout: 30000 }).catch(() => {});
    url = page.url();
  }
  const clean = url.split('?')[0];
  return { url: clean, status: /article\/details\/\d+/.test(clean) ? 'PUBLISHED' : 'UNKNOWN' };
}

module.exports = {
  name: 'csdn',
  displayName: 'CSDN',
  homeUrl: 'https://www.csdn.net/',
  loginUrl: 'https://passport.csdn.net/login',
  editorUrl: 'https://editor.csdn.net/md/',
  loginCookieNames: ['UserToken', 'UserInfo'],
  checkLogin,
  enterEditor,
  setTitle,
  setContent,
  preparePublish,
  publish,
  saveDraft,
  deleteBlog,
  setCover,
  setCreationStatement,
  setArticleTags,
  SELECTORS,
};