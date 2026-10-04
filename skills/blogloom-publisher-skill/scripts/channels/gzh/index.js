'use strict';

/**
 * 公众号（gzh）渠道适配。
 *
 * 流程参照已跑通的 auto-sync-blog `WechatSyncBlogClientBase`：
 *   1. 首页判定登录态（已登录出现 `.weui-desktop_name`，未登录出现 `#jumpUrl`）
 *   2. 内容管理 → 草稿 → 「写新文章（图文消息）」进入编辑器（可能新开标签页）
 *   3. 填标题 / 作者（`#js_title_main #title` / `#js_author_area #author`）
 *   4. 正文：先用 md.doocs.org 把 Markdown 渲染成公众号样式 HTML，再粘贴进 `.ProseMirror`
 *      （可选：先把公众号名片插入正文顶部）
 *   5. 底部配置：首图封面 → 摘要 → 原创 → 合集 → 创作来源
 *   6. 发布草稿：编辑器顶部「保存」；发布：`.mass_send` → 群发弹窗 → 二次确认
 *   7. 删除：内容管理 → 草稿箱，定位文章 → 右侧「...」→ 删除 → 确认
 *
 * 所有 DOM 选择器集中在 ./selectors.js，站点改版时只改那里。
 */

const { writeContent } = require('../../lib/editor');
const { log } = require('../../lib/cli-io');
const { loadChannelConf } = require('../../lib/channel-conf');
const { convertMarkdownToGzhHtml } = require('./format');
const SELECTORS = require('./selectors');

const CONF = loadChannelConf('gzh');

const MOD = process.platform === 'darwin' ? 'Meta' : 'Control';
const DEFAULT_HOME = 'https://mp.weixin.qq.com/cgi-bin/home';
const SOURCE_TEXT = '个人观点，仅供参考';
// 真正的登录凭证：未登录态会下发 wxuin/ua_id 等，不能用它们判定（否则假阳性）
const LOGIN_COOKIE_NAMES = ['slave_sid', 'slave_user', 'data_ticket'];

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

async function scrollToBottom(page) {
  await page.keyboard.press('End').catch(() => {});
  await page
    .evaluate(() => window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' }))
    .catch(() => {});
  await page.waitForTimeout(600);
}

/**
 * 进入公众号后台首页：带 token 的 `/cgi-bin/home` 需要通过首页的「登录」入口 `#jumpUrl` 进入
 * （会话有效则直接进入后台，无效则跳登录页）。后台首页会出现 `.weui-desktop-menu_create`。
 */
async function enterAdminHome(page, ctx = {}) {
  if (await page.locator(SELECTORS.menuCreate).first().isVisible().catch(() => false)) return;

  log('打开公众号首页，准备进入后台...');
  await page
    .goto(ctx.homeUrl || DEFAULT_HOME, { waitUntil: 'domcontentloaded', timeout: 45000 })
    .catch(() => {});
  await page.waitForTimeout(1500);

  const jump = page.locator(SELECTORS.jumpUrl).first();
  if (await has(jump)) {
    log('点击首页「登录」入口，进入带 token 的后台首页');
    await jump.click({ timeout: 8000 }).catch(() => {});
    await page.waitForTimeout(4000);
  }
  await page
    .locator(SELECTORS.menuCreate)
    .first()
    .waitFor({ state: 'visible', timeout: 15000 })
    .catch(() => {});
}

/**
 * 扫码登录前置：未登录时首页显示「请重新登录 / 登录」入口，点击 `#jumpUrl` 才会进入扫码登录页。
 * 供通用 performLogin 调用。
 */
async function prepareLogin(page) {
  const jump = page.locator(SELECTORS.jumpUrl).first();
  if (await jump.isVisible().catch(() => false)) {
    await jump.click({ timeout: 8000 }).catch(() => {});
    await page.waitForTimeout(3000);
  }
}

/**
 * 检测登录态：
 * 1. 进入后台首页后出现账号名 `.weui-desktop_name` → 已登录；
 * 2. 否则看真正的会话 Cookie（slave_sid / slave_user / data_ticket）——它们仅在登录后下发，
 *    未登录时的 wxuin/ua_id 不在白名单，因此不会假阳性；
 * 3. 都没有则视为未登录。
 */
async function checkLogin(page, ctx = {}) {
  await enterAdminHome(page, ctx);
  await page.waitForTimeout(1500);

  let user = '';
  const nameLoc = page.locator(SELECTORS.accountName).first();
  if (await has(nameLoc)) user = ((await nameLoc.innerText().catch(() => '')) || '').trim();

  const jumpLoc = page.locator(SELECTORS.jumpUrl).first();
  const jumpText = (await has(jumpLoc)) ? ((await jumpLoc.innerText().catch(() => '')) || '').trim() : '';
  const loginEntryVisible = jumpText.includes('登录');

  const cookies = await ctx.cookies(ctx.homeUrl || DEFAULT_HOME).catch(() => []);
  const cookieNames = cookies.map((c) => c.name).filter((n) => LOGIN_COOKIE_NAMES.includes(n));
  const loggedIn = !!user || cookieNames.length > 0;

  return {
    loggedIn,
    user: user || '',
    cookieNames,
    loginEntryVisible,
    reason: loggedIn
      ? '检测到公众号登录态'
      : '未检测到公众号登录态，请先执行 gzh login 扫码登录',
  };
}

/**
 * 进入图文消息编辑器：后台首页 → 内容管理 → 草稿箱 → 新的创作 → 文章。
 * 「文章」在新标签页打开编辑器，返回实际编辑页。
 */
async function enterEditor(page, ctx = {}) {
  // 已在编辑器（内容 ProseMirror 已就绪）则直接复用
  if (await page.locator(SELECTORS.editor).first().isVisible().catch(() => false)) return page;

  await enterAdminHome(page, ctx);

  // 内容管理 → 草稿箱
  log('进入「内容管理 → 草稿箱」');
  await clickIf(page.locator(SELECTORS.menuCreate).first(), { timeout: 5000 });
  await page.waitForTimeout(1000);
  const subItem = page.locator(SELECTORS.subMenuItem).first();
  if (await has(subItem)) {
    await clickIf(subItem.locator('a').first(), { timeout: 5000 });
  }
  await page.waitForTimeout(3000);

  // 新的创作 → 文章
  log('点击「新的创作 → 文章」，打开图文编辑器');
  if (!(await clickIf(page.locator(SELECTORS.newCreateButton).first(), { timeout: 6000, force: true }))) {
    const err = new Error('未找到「新的创作」入口，页面可能改版，请检查 selectors.js');
    err.code = 'EDITOR_NOT_FOUND';
    throw err;
  }
  await page.waitForTimeout(1500);
  const addBtn = page.locator(SELECTORS.createArticleItem).first();
  if (!(await has(addBtn))) {
    const err = new Error('未找到「文章」入口，页面可能改版，请检查 selectors.js');
    err.code = 'EDITOR_NOT_FOUND';
    throw err;
  }

  // 点击「文章」可能在新标签页打开编辑器
  let editorPage = page;
  const context = page.context();
  if (context && typeof context.waitForEvent === 'function') {
    const newPage = await Promise.all([
      context.waitForEvent('page', { timeout: 15000 }).catch(() => null),
      addBtn.click({ timeout: 8000 }).catch(() => {}),
    ]).then(([np]) => np);
    if (newPage) {
      editorPage = newPage;
      await editorPage.waitForLoadState?.('domcontentloaded').catch(() => {});
    }
  } else {
    await addBtn.click({ timeout: 8000 }).catch(() => {});
  }

  await editorPage
    .locator(SELECTORS.editor)
    .first()
    .waitFor({ state: 'visible', timeout: 30000 })
    .catch(() => {});

  if (/loginpage/.test(editorPage.url())) {
    const err = new Error('公众号登录态已失效，请重新执行: node scripts/publisher.js gzh login');
    err.code = 'AUTH_REQUIRED';
    throw err;
  }
  await editorPage.waitForTimeout(2000);
  return editorPage;
}

async function setTitle(page, title) {
  log(`填写标题：${title}`);
  let input = page.locator(SELECTORS.titleInput).first();
  if (!(await has(input))) input = page.locator(SELECTORS.titleFallbackInput).first();
  await input.waitFor({ state: 'visible', timeout: 15000 });
  await input.click({ timeout: 5000 }).catch(() => {});
  await page.keyboard.press(`${MOD}+A`).catch(() => {});
  await page.keyboard.press('Backspace').catch(() => {});
  // 标题编辑器是 contenteditable/ProseMirror，用键盘输入最稳
  await page.keyboard.type(title, { delay: 5 }).catch(() => {});
  await page.waitForTimeout(300);
  let actual = ((await input.innerText().catch(() => '')) || '').trim();
  if (!actual) actual = (await input.inputValue().catch(() => '')) || '';
  return { value: actual };
}

/** 作者栏（可选，来自 --author / 元数据 author / username）。必须先填作者，原创弹窗的「我已阅读」才可勾选。 */
async function setAuthor(page, author) {
  if (!author) return { value: '' };
  const input = page.locator(SELECTORS.authorInput).first();
  if (!(await has(input))) return { value: '' };
  await input.scrollIntoViewIfNeeded().catch(() => {});
  await input.click({ timeout: 5000 }).catch(() => {});
  await input.fill('').catch(() => {});
  await input.fill(author).catch(() => {});
  let value = (await input.inputValue().catch(() => '')) || '';
  if (value !== author) {
    await input.click({ timeout: 5000 }).catch(() => {});
    await page.keyboard.press(`${MOD}+A`).catch(() => {});
    await page.keyboard.press('Backspace').catch(() => {});
    await page.keyboard.type(author, { delay: 10 }).catch(() => {});
    value = (await input.inputValue().catch(() => '')) || value;
  }
  log(`填写作者：${value || '(空)'}`);
  return { value };
}

/** 插入公众号名片（可选，来自 --wechat-name / 元数据 wechatName）。 */
async function insertProfileCard(page, wechatName) {
  if (!wechatName) return false;
  const btn = page.locator(SELECTORS.insertProfileBtn).first();
  if (!(await clickIf(btn, { timeout: 5000 }))) return false;

  const dialog = page.locator(SELECTORS.profileDialog).first();
  await dialog.waitFor({ state: 'visible', timeout: 8000 }).catch(() => {});

  const search = dialog.locator(SELECTORS.profileSearch).first();
  const input = search.locator('input').first();
  if (await has(input)) {
    await input.fill(wechatName).catch(() => {});
    await clickIf(search.locator(SELECTORS.profileSearchBtn).first(), { timeout: 3000 });
  }
  await page.waitForTimeout(2500);

  const card = page.locator(SELECTORS.profileGridCol).first().locator(SELECTORS.profileCardBody).first();
  await clickIf(card, { timeout: 5000 });
  await clickIf(dialog.locator(SELECTORS.btnWrp).first(), { timeout: 3000 });
  await page.waitForTimeout(800);
  return true;
}

/**
 * 微信粘贴外部富文本后会弹「内容结构检测」确认框（取消 / 继续插入），
 * 点「继续插入」才会把内容真正写进正文。
 */
async function confirmInsertDialog(page) {
  for (let i = 0; i < 3; i += 1) {
    const btn = page.locator(SELECTORS.continueInsert).last();
    if (!(await btn.isVisible().catch(() => false))) break;
    await btn.click({ timeout: 5000 }).catch(() => {});
    await page.waitForTimeout(2500);
  }
}

/**
 * 写入正文：把 Markdown 渲染成公众号样式后粘贴进 ProseMirror。
 * 转换能力通过 `ctx.convertMarkdown` 注入（默认走 md.doocs.org），便于单测替换。
 */
async function setContent(page, markdown, ctx = {}) {
  const editor = page.locator(SELECTORS.editor).first();
  await editor.waitFor({ state: 'visible', timeout: 30000 });
  await editor.click({ timeout: 5000 }).catch(() => {});
  await page.waitForTimeout(400);

  const profileName = (ctx.payload && (ctx.payload.wechatName || ctx.payload.wechat_name)) || '';
  if (profileName) await insertProfileCard(page, profileName).catch(() => {});

  const convert = ctx.convertMarkdown || convertMarkdownToGzhHtml;
  log('正文样式转换：md.doocs.org 渲染 Markdown → 复制富文本');
  const converted = await convert(page, markdown).catch(() => ({ converted: false }));
  if (!converted || !converted.converted) log(`  样式转换未完成（${(converted && converted.reason) || '未知'}），将回退原始写入`);

  log('粘贴正文到编辑器内容区');
  await editor.click({ timeout: 5000 }).catch(() => {});
  await page.keyboard.press(`${MOD}+V`).catch(() => {});
  await page.waitForTimeout(2500);

  // 粘贴外部富文本后，微信会弹出「内容结构检测」确认框，需点「继续插入」才会真正写入
  log('处理「内容结构检测」确认框（继续插入）');
  await confirmInsertDialog(page);

  let length = 0;
  try {
    length = (((await editor.innerText({ timeout: 3000 })) || '')).length;
  } catch (err) {
    length = 0;
  }

  // 样式转换失败 / 粘贴为空：回退为原始 Markdown 写入策略
  if (length === 0) {
    const fallback = await writeContent(page, SELECTORS.editor, markdown, ['paste', 'cdp', 'keyboard']);
    return { strategy: `raw-${fallback.strategy}`, length: fallback.length, converted: false };
  }

  await scrollToBottom(page);
  return {
    strategy: converted && converted.converted ? 'md-style-paste' : 'md-style-paste-fallback',
    length,
    converted: !!(converted && converted.converted),
  };
}

/** 首图：从正文图片中选第一张作为封面，并做上传校验（失败重试一次）。 */
async function setCover(page, firstBox) {
  const pick = async () => {
    const coverArea = firstBox.locator(SELECTORS.coverArea).first();
    if (!(await has(coverArea))) return false;

    await coverArea.hover().catch(() => {});
    await page.waitForTimeout(800);

    // 「从正文选择」按钮悬停后才可见，且可能渲染在多个预置浮层中，需取可见的那个
    let trigger = page.locator(`${SELECTORS.selectCoverFromContent}:visible`).first();
    if (!(await has(trigger))) {
      const pop = page.locator(SELECTORS.coverNullPop).first();
      if (await has(pop)) await pop.hover().catch(() => {});
      await page.waitForTimeout(600);
      trigger = page.locator(`${SELECTORS.selectCoverFromContent}:visible`).first();
    }
    if (!(await clickIf(trigger, { timeout: 4000 }))) return false;

    const panel = page.locator(SELECTORS.imgCropPanel).first();
    await panel.waitFor({ state: 'visible', timeout: 8000 }).catch(() => {});
    const pic = panel.locator(SELECTORS.contentImgItem).first();
    if (!(await has(pic))) return false;
    log('封面：选中正文第一张图');
    await pic.click({ timeout: 5000 }).catch(() => {});
    await page.waitForTimeout(600);

    log('封面：下一步');
    await clickIf(page.locator(SELECTORS.coverNext).first(), { timeout: 5000 });
    await page.waitForTimeout(1800);

    log('封面：完成');
    await clickIf(page.locator(SELECTORS.coverDone).last(), { timeout: 5000 });
    await page.waitForTimeout(1500);
    return true;
  };

  if (!(await pick())) return false;
  if (!(await isCoverUploaded(page))) {
    await page.waitForTimeout(2000);
    await pick();
  }
  return isCoverUploaded(page);
}

/** 校验封面是否上传成功（预览区出现 background-image）。 */
async function isCoverUploaded(page) {
  const preview = page.locator(SELECTORS.coverPreview).first();
  if (!(await has(preview))) return false;
  const style = (await preview.getAttribute('style').catch(() => '')) || '';
  return style.includes('background-image') && !style.includes('url("")');
}

/** 文章摘要（描述上限来自 conf，默认 120 字，超出自动裁剪）。 */
async function inputArticleSummary(page, firstBox, summary) {
  if (!summary) return false;
  const textarea = firstBox.locator(SELECTORS.descTextarea).first();
  if (!(await has(textarea))) return false;
  const max = Number(CONF.summaryMaxLength) || 120;
  const text = String(summary).slice(0, max);
  await textarea.click({ timeout: 3000 }).catch(() => {});
  await textarea.fill(text).catch(() => {});
  log(`填写描述（${text.length}/${max}）`);
  return true;
}

/** 原创：点原创 → 选「文字原创」→ 勾「我已阅读」→ 确定。
 *  微信自定义组件首次打开常不响应，需关闭后重新打开再试；作者未填写时无法勾选。 */
async function chooseOrigin(page) {
  const box = page.locator(SELECTORS.originalBox).first();
  if (!(await has(box))) return false;
  const origin = box.locator(SELECTORS.original).first();
  const originDialog = page.locator(SELECTORS.dialog).filter({ hasText: '声明类型' }).first();

  let opened = false;
  for (let round = 0; round < 4; round += 1) {
    if (!opened) {
      if (!(await clickIf(origin, { timeout: 5000 }))) return false;
      opened = true;
      await page.waitForTimeout(1600);
    }
    const dialog = page.locator(SELECTORS.dialog).first();
    if (!(await dialog.isVisible().catch(() => false))) break;

    // 声明类型：主动点选「文字原创」
    await clickIf(dialog.locator(SELECTORS.originalTypeText).first(), { timeout: 4000 });
    await page.waitForTimeout(300);

    // 勾选「我已阅读并同意…」：对 <label> 触发 DOM click，重试直到勾上
    const agreeInput = dialog.locator(SELECTORS.originalAgreementInput).first();
    const agreeLabel = dialog.locator(SELECTORS.originalAgreementLabel).first();
    let checked = (await has(agreeInput)) ? await agreeInput.isChecked().catch(() => false) : false;
    for (let i = 0; i < 6 && !checked; i += 1) {
      if (await has(agreeLabel)) await agreeLabel.evaluate((el) => el && el.click()).catch(() => {});
      await page.waitForTimeout(350);
      checked = (await has(agreeInput)) ? await agreeInput.isChecked().catch(() => false) : false;
    }
    log(`原创：文字原创 + 我已阅读(${checked ? '已勾选' : '未勾选'})`);

    // 确定
    await clickIf(dialog.locator(SELECTORS.dialogConfirm).first(), { timeout: 5000 });
    await page.waitForTimeout(1600);
    if (!(await originDialog.isVisible().catch(() => false))) {
      log('原创：声明成功');
      return true;
    }

    // 失败：关闭弹窗，下一轮重新打开再试
    await clickIf(originDialog.locator(SELECTORS.dialogCancel).first(), { timeout: 4000 });
    await page.waitForTimeout(900);
    opened = false;
  }

  log('原创：多次尝试仍未声明成功，已跳过（不影响后续步骤）');
  await closeDialogs(page);
  return false;
}

/** 合集：选择与设置值同名的合集（不存在则不创建，跳过）。 */
async function chooseCollection(page, name) {
  if (!name) return '';
  const area = page.locator(SELECTORS.articleTagsArea).first();
  if (!(await has(area))) return '';
  if (!(await clickIf(area.locator(SELECTORS.articleTagsLabel).first(), { timeout: 4000 }))) return '';

  const dialog = page.locator(SELECTORS.dialog).first();
  await dialog.waitFor({ state: 'visible', timeout: 5000 }).catch(() => {});
  const items = dialog.locator(SELECTORS.collectionOption);
  const count = await items.count().catch(() => 0);
  let matched = '';
  for (let i = 0; i < count; i += 1) {
    const text = ((await items.nth(i).innerText().catch(() => '')) || '').trim();
    if (text === name) {
      await items.nth(i).click({ timeout: 3000 }).catch(() => {});
      matched = text;
      break;
    }
  }
  await clickIf(dialog.locator(SELECTORS.btnWrp).first(), { timeout: 3000 });
  await page.waitForTimeout(600);
  return matched;
}

/** 创作来源：弹窗选择指定文案（默认取自 conf，个人观点仅供参考）→ 确认。 */
async function chooseSource(page, text = CONF.creationSource || SOURCE_TEXT) {
  const area = page.locator(SELECTORS.claimSourceArea).first();
  if (!(await has(area))) return '';
  if (!(await clickIf(area.locator(SELECTORS.claimSourceDesc).first(), { timeout: 4000 }))) return '';

  const dialog = page.locator(SELECTORS.dialog).last();
  await dialog.waitFor({ state: 'visible', timeout: 5000 }).catch(() => {});
  const option = dialog
    .locator(`.weui-desktop-form__check-label:has-text("${text}"), label:has-text("${text}")`)
    .first();
  if (!(await clickIf(option, { timeout: 4000 }))) return '';
  await clickIf(dialog.locator(SELECTORS.claimConfirm).first(), { timeout: 4000 });
  await page.waitForTimeout(800);
  return text;
}

/** 把某个开关打开（幂等 + 重试；用区域文案判断是否真的开启）。 */
async function turnOnSwitch(page, areaSelector, inputSelector, switchSelector, onPattern) {
  const area = page.locator(areaSelector).first();
  for (let i = 0; i < 10 && !(await area.isVisible().catch(() => false)); i += 1) {
    await page.waitForTimeout(300);
  }
  const isOn = async () => {
    if (onPattern) {
      const txt = (await area.innerText().catch(() => '')) || '';
      if (onPattern.test(txt)) return true;
    }
    return page.locator(inputSelector).first().isChecked().catch(() => false);
  };
  for (let i = 0; i < 3; i += 1) {
    if (await isOn()) return true;
    const sw = page.locator(switchSelector).first();
    if (await sw.isVisible().catch(() => false)) {
      await sw.click({ timeout: 4000 }).catch(() => {});
    } else {
      await page.locator(inputSelector).first().evaluate((el) => el && el.click()).catch(() => {});
    }
    await page.waitForTimeout(1200);
  }
  return isOn();
}

/** 处理「赞赏设置」弹窗：赞赏类型选赞赏作者、选赞赏账户、勾协议、确定。 */
async function configureRewardDialog(page) {
  const dlg = page.locator(SELECTORS.dialog).filter({ hasText: '赞赏类型' }).first();
  if (!(await dlg.isVisible().catch(() => false))) return false;

  // 赞赏类型：赞赏作者
  await clickIf(dlg.locator(SELECTORS.rewardTypeAuthor).first(), { timeout: 3000 });
  // 赞赏账户：选最近使用的第一个
  await clickIf(dlg.locator(SELECTORS.rewardAccountItem).first(), { timeout: 3000 });
  // 勾选「我已阅读并同意…」
  const agree = dlg.locator(SELECTORS.rewardAgreementInput).first();
  const agreeLabel = dlg.locator(SELECTORS.rewardAgreementLabel).first();
  let checked = (await has(agree)) ? await agree.isChecked().catch(() => false) : false;
  for (let i = 0; i < 6 && !checked; i += 1) {
    if (await has(agreeLabel)) await agreeLabel.evaluate((el) => el && el.click()).catch(() => {});
    await page.waitForTimeout(350);
    checked = (await has(agree)) ? await agree.isChecked().catch(() => false) : false;
  }
  await clickIf(dlg.locator(SELECTORS.rewardDialogConfirm).first(), { timeout: 4000 });
  await page.waitForTimeout(1500);
  return !(await dlg.isVisible().catch(() => false));
}

/** 赞赏是否已开启（界面出现「账户」提示）。 */
async function isRewardOn(page) {
  const txt = (await page.locator(SELECTORS.rewardArea).first().innerText().catch(() => '')) || '';
  return /账户|已开启/.test(txt);
}

/**
 * 赞赏：默认开启（需先声明原创）；点开关会弹「赞赏设置」，选赞赏账户+勾协议+确定。
 * 付费按产品要求**不开启**。整体加超时保护，避免自定义组件卡住主流程。
 */
async function enableRewardAndPay(page) {
  const doEnable = async () => {
    await page.waitForTimeout(1200);
    let reward = await isRewardOn(page);
    for (let i = 0; i < 3 && !reward; i += 1) {
      await clickIf(page.locator(SELECTORS.rewardSwitch).first(), { timeout: 4000 });
      await page.waitForTimeout(2000);
      await configureRewardDialog(page);
      await page.waitForTimeout(700);
      reward = await isRewardOn(page);
    }
    return reward;
  };
  let reward = false;
  try {
    reward = await Promise.race([
      doEnable(),
      new Promise((resolve) => setTimeout(() => resolve(false), 45000)),
    ]);
  } catch (err) {
    reward = false;
  }
  log(`赞赏：${reward ? '已开启' : '未开启'}；付费：默认不开启`);
  await closeDialogs(page).catch(() => {});
  return { reward, pay: false };
}

/** 配置发布项（封面/描述/原创/赞赏付费/合集/创作来源），不点最终发表。 */
async function preparePublish(page, payload = {}) {
  const result = {
    coverSet: false,
    summarySet: false,
    original: false,
    reward: false,
    pay: false,
    collection: '',
    source: '',
  };

  const bottomBox = page.locator(SELECTORS.afterArea).first();
  if (await has(bottomBox)) {
    const firstBox = bottomBox.locator(SELECTORS.coverDescriptionArea).first();
    if (await has(firstBox)) {
      if (CONF.cover !== 'none') {
        result.coverSet = await setCover(page, firstBox).catch(() => false);
      }
      result.summarySet = await inputArticleSummary(page, firstBox, payload.summary).catch(() => false);
    }
  }

  // 是否声明原创（conf 控制）
  result.original = CONF.enableOriginal !== false ? await chooseOrigin(page).catch(() => false) : false;

  // 是否开启赞赏（conf 控制；需先声明原创）。付费默认不开启。
  if (CONF.enableReward !== false) {
    const rp = await enableRewardAndPay(page).catch(() => ({ reward: false, pay: false }));
    result.reward = rp.reward;
    result.pay = rp.pay;
  }

  // 合集：按「分类」选择，没有分类或没有匹配项则不选
  const collection = payload.category || payload.collection || '';
  result.collection = await chooseCollection(page, collection).catch(() => '');

  result.source = await chooseSource(page, CONF.creationSource || SOURCE_TEXT).catch(() => '');
  return result;
}

/**
 * 保存草稿前的发布项配置（封面/描述/原创/合集/创作来源）。
 * 赞赏/付费/留言保留编辑器默认（正常状态，不需要额外操作）。
 * 草稿场景由 actions 通过 channel.configureOnDraft 识别后调用。
 */
async function prepareDraft(page, payload = {}) {
  return preparePublish(page, payload);
}

/** 触发发表：群发弹窗（可关闭群发）→ 二次确认 → 等待扫码/提交。 */
async function publish(page, opts = {}) {
  await closeDialogs(page);
  const payload = opts.payload || {};
  const groupSend = payload.groupSend === true || payload.isGroupSend === true;

  const bar = page.locator(SELECTORS.bottomBar).first();
  await bar.waitFor({ state: 'visible', timeout: 15000 }).catch(() => {});
  const send = bar.locator(SELECTORS.massSend).first();
  await send.click({ timeout: 8000 }).catch(() => {});
  await page.waitForTimeout(2000);

  // 弹窗 1：是否群发（默认开启，未要求群发则关闭）
  const dialog = page.locator(SELECTORS.massSendDialog).first();
  if (await dialog.waitFor({ state: 'visible', timeout: 8000 }).then(() => true).catch(() => false)) {
    if (!groupSend) {
      await clickIf(dialog.locator(SELECTORS.massSendSwitch).first(), { timeout: 3000 });
    }
    const ft = dialog.locator(SELECTORS.dialogFt).first();
    await clickIf(ft.locator(SELECTORS.btnPrimary).first(), { timeout: 5000 });
  } else {
    // 兼容无弹窗（如「保存并群发」入口）的场景
    await clickIf(page.locator(SELECTORS.massSend).first(), { timeout: 3000 });
  }
  await page.waitForTimeout(2000);

  // 弹窗 2：二次确认「继续发表」
  const doubleCheck = page.locator(SELECTORS.doubleCheckDialog).first();
  if (await doubleCheck.waitFor({ state: 'visible', timeout: 6000 }).then(() => true).catch(() => false)) {
    await clickIf(doubleCheck.locator(SELECTORS.btnPrimary).first(), { timeout: 5000 });
  }
  await page.waitForTimeout(3000);

  // 群发需要管理员扫码，此处给出一段确认窗口
  const toast = page.locator(SELECTORS.successToast).first();
  const published = await toast
    .waitFor({ state: 'visible', timeout: 8000 })
    .then(() => true)
    .catch(() => false);

  return published
    ? { status: 'PUBLISHED', url: '' }
    : { status: 'SUBMITTED', url: '', message: '已提交发表，如需群发请在浏览器中完成管理员扫码确认' };
}

/** 关闭当前所有可见弹窗（兜底，避免自定义组件残留弹窗挡住后续按钮）。 */
async function closeDialogs(page) {
  for (let i = 0; i < 5; i += 1) {
    const dlg = page.locator(SELECTORS.dialog).last();
    if (!(await dlg.isVisible().catch(() => false))) break;
    await clickIf(dlg.locator(SELECTORS.dialogCancel).first(), { timeout: 2000 });
    await page.waitForTimeout(400);
  }
}

/** 保存草稿（编辑器顶部「保存为草稿」，即 gzh publishDraft 的核心动作）。 */
async function saveDraft(page) {
  await closeDialogs(page);
  let btn = page.locator(SELECTORS.saveButton).first();
  if (!(await btn.isVisible().catch(() => false))) {
    btn = page.locator(SELECTORS.saveFallback).first();
  }
  await btn.waitFor({ state: 'visible', timeout: 15000 });
  log('点击「保存为草稿」');
  await btn.click({ timeout: 5000 });
  await page.waitForTimeout(3000);
  return { status: 'DRAFT_SAVED', url: page.url() };
}

/** 进入「内容管理 → 草稿箱」列表页。 */
async function enterDraftList(page, ctx = {}) {
  await enterAdminHome(page, ctx);
  await clickIf(page.locator(SELECTORS.menuCreate).first(), { timeout: 5000 });
  await page.waitForTimeout(1000);
  const subItem = page.locator(SELECTORS.subMenuItem).first();
  if (await has(subItem)) {
    await clickIf(subItem.locator('a').first(), { timeout: 5000 });
  }
  await page.waitForTimeout(3500);
}

/** 在草稿箱中按 appmsgid / 标题定位文章卡片。 */
function findDraftItem(page, target = {}) {
  if (target.id) return page.locator(`${SELECTORS.draftItem}[data-appid="${target.id}"]`).first();
  if (target.title) return page.locator(SELECTORS.draftItem).filter({ hasText: target.title }).first();
  return page.locator(SELECTORS.draftItem).first();
}

/**
 * 删除公众号推文：内容管理 → 草稿箱 → 悬停卡片 → 行内「删除」→ 确认浮层「删除」，
 * 并回查该卡片是否已从列表消失（避免假成功）。
 */
async function deleteBlog(page, target = {}, ctx = {}) {
  await enterDraftList(page, ctx);

  const item = findDraftItem(page, target);
  if (!(await has(item))) {
    throw new Error(`未在草稿箱中找到文章：${target.title || target.id || target.url || '(未提供)'}`);
  }
  await item.scrollIntoViewIfNeeded().catch(() => {});
  await item.hover().catch(() => {});
  await page.waitForTimeout(800);

  const delLink = item.locator(SELECTORS.draftDeleteLink).first();
  if (!(await has(delLink))) {
    throw new Error('未找到行内「删除」入口，页面可能改版，请检查 selectors.js');
  }
  await delLink.click({ force: true, timeout: 5000 }).catch(() => {});
  await page.waitForTimeout(800);

  const confirm = page.locator(SELECTORS.confirmDelete).first();
  await confirm.waitFor({ state: 'visible', timeout: 8000 });
  await confirm.click({ timeout: 5000 });
  await page.waitForTimeout(1500);

  // 轮询等待列表刷新（删除是异步的）
  let remaining = 1;
  for (let i = 0; i < 10; i += 1) {
    remaining = await findDraftItem(page, target).count().catch(() => 0);
    if (remaining === 0) break;
    await page.waitForTimeout(800);
  }
  if (remaining > 0) return { status: 'DELETE_UNVERIFIED', title: target.title || '', id: target.id || '' };
  return { status: 'DELETED', title: target.title || '', id: target.id || '' };
}

/**
 * 渠道专属发布参数（核心 payload 层不感知）：
 * author（作者栏）/ wechatName（公众号名片）/ collection（合集）/ groupSend（是否群发）。
 */
function augmentPayload(base, { opts = {}, params = {}, blog = {} } = {}) {
  const extra = (blog && blog.extra) || base.extra || {};
  const confGroup = CONF.groupSend === true || CONF.groupSend === 'true';
  const groupSend =
    opts.groupSend !== undefined
      ? opts.groupSend === true || opts.groupSend === 'true'
      : params.groupSend !== undefined
        ? params.groupSend === true || params.groupSend === 'true'
        : extra.isGroupSend !== undefined
          ? extra.isGroupSend === true || extra.isGroupSend === 'true'
          : confGroup;

  return {
    author: opts.author || params.author || extra.author || extra.username || CONF.author || '',
    wechatName: opts.wechatName || opts.wechat || params.wechatName || extra.wechatName || CONF.wechatName || '',
    collection: opts.collection || params.collection || extra.collection || CONF.collection || '',
    groupSend,
  };
}

module.exports = {
  name: 'gzh',
  displayName: '公众号',
  homeUrl: DEFAULT_HOME,
  loginUrl: DEFAULT_HOME,
  editorUrl: DEFAULT_HOME,
  loginCookieNames: LOGIN_COOKIE_NAMES,
  // 发布链路需管理员扫码/审核，自测采用「发布草稿 → 删除」而非直接群发
  testMode: 'draft',
  // 草稿场景也要先配置封面/描述/原创/合集/创作来源，再保存草稿
  configureOnDraft: true,
  // 发表后进入平台/扫码流程，最终态为 SUBMITTED
  successStatuses: ['SUBMITTED'],
  prepareLogin,
  checkLogin,
  enterEditor,
  setTitle,
  setAuthor,
  setContent,
  preparePublish,
  prepareDraft,
  publish,
  saveDraft,
  deleteBlog,
  augmentPayload,
  insertProfileCard,
  setCover,
  isCoverUploaded,
  inputArticleSummary,
  chooseOrigin,
  enableRewardAndPay,
  chooseCollection,
  chooseSource,
  SELECTORS,
};
