'use strict';

/**
 * 公众号正文样式转换。
 *
 * 公众号编辑器（ProseMirror）不识别 Markdown 源码，直接粘贴 Markdown 只会得到纯文本。
 * 参照 auto-sync-blog 的做法：先用 Markdown 排版工具 https://md.doocs.org/ 把 Markdown
 * 渲染成带内联样式的 HTML，再把该富文本复制到系统剪贴板，最后粘贴回公众号编辑器。
 *
 * 该函数在**同一浏览器上下文**新开一个转换页，转换完即关闭，不影响主编辑页。
 */

const MD_URL = 'https://md.doocs.org/';

const MD_SELECTORS = {
  editor: '.cm-scroller',
  content: '.cm-content',
  // 右上角「样式」按钮（打开右侧样式面板；中英文两版）
  styleButton: 'button[aria-label="样式"], button[aria-label="Style"]',
  // 样式面板
  stylePanel: '.right-slider-panel',
  // 面板内「图注」区块的「不显示」选项（中英文两版；XPath 先锁定图注标题再取同名按钮，避免误点其他区块的 None）
  figureCaptionNone:
    'xpath=.//h2[normalize-space(.)="图注" or normalize-space(.)="Caption"]' +
    '/parent::div//button[normalize-space(.)="不显示" or normalize-space(.)="None"]',
  // 站点有中英文两版：复制按钮文案为「复制」或「Copy」
  copyButton: 'button:has-text("复制"), button:has-text("Copy")',
};

const MOD = process.platform === 'darwin' ? 'Meta' : 'Control';

async function has(locator) {
  return (await locator.count().catch(() => 0)) > 0;
}

/**
 * 把 md.doocs.org 的「图注」设置为「不显示」。
 *
 * 默认值（alt 优先 / 仅 alt）会在图片下方渲染出图注文字；公众号正文不需要，
 * 因此在复制富文本之前，先点右上角「样式」打开右侧面板，滚动到「图注」区块，
 * 选中「不显示」，然后再执行复制。
 *
 * @param {import('playwright').Page} converter md.doocs 转换页
 * @returns {Promise<boolean>} 是否成功选中「不显示」
 */
async function setFigureCaptionHidden(converter) {
  const panel = converter.locator(MD_SELECTORS.stylePanel).first();
  let hideBtn = panel.locator(MD_SELECTORS.figureCaptionNone).first();

  // 面板默认收起：看不到「图注」时先点「样式」展开
  if (!(await hideBtn.isVisible().catch(() => false))) {
    const styleBtn = converter.locator(MD_SELECTORS.styleButton).first();
    if (await has(styleBtn)) {
      await styleBtn.click({ timeout: 5000 }).catch(() => {});
      await converter.waitForTimeout(800);
    }
    hideBtn = panel.locator(MD_SELECTORS.figureCaptionNone).first();
  }

  if (!(await has(hideBtn))) return false;

  await hideBtn.scrollIntoViewIfNeeded().catch(() => {});
  await hideBtn.click({ timeout: 5000 }).catch(() => {});
  await converter.waitForTimeout(400);

  // 选中态的 class 含 border-primary（未选中为 border-input），据此校验
  const cls = (await hideBtn.getAttribute('class').catch(() => '')) || '';
  return /border-primary/.test(cls);
}

/**
 * 把 Markdown 渲染为公众号富文本，并复制到系统剪贴板。
 * @param {import('playwright').Page} page 当前编辑页（用于取得同一 context）
 * @param {string} markdown Markdown 源码
 * @returns {Promise<{ converted: boolean }>}
 */
async function convertMarkdownToGzhHtml(page, markdown) {
  const context = page.context();
  if (!context || typeof context.newPage !== 'function') {
    return { converted: false, reason: 'context 不支持 newPage' };
  }

  const converter = await context.newPage();
  try {
    await converter.goto(MD_URL, { waitUntil: 'domcontentloaded', timeout: 60000 });
    const editor = converter.locator(MD_SELECTORS.content).first();
    await editor.waitFor({ state: 'visible', timeout: 30000 });

    // 清空转换器默认内容
    await editor.click({ timeout: 5000 }).catch(() => {});
    await converter.keyboard.press(`${MOD}+A`).catch(() => {});
    await converter.keyboard.press('Backspace').catch(() => {});

    // 通过剪贴板粘贴 Markdown，避免超长文本逐字输入带来的性能问题
    await converter
      .evaluate(async (value) => {
        await navigator.clipboard.writeText(value);
      }, markdown)
      .catch(() => {});
    await editor.click({ timeout: 5000 }).catch(() => {});
    await converter.keyboard.press(`${MOD}+V`).catch(() => {});
    // 等待 md.doocs 完成 Markdown 渲染（渲染完成后「复制」才会写入富文本）
    await converter.waitForTimeout(3000);

    // 复制前：把「图注」设为「不显示」，避免图片下方出现图注文字
    const captionHidden = await setFigureCaptionHidden(converter).catch(() => false);

    // 点击「复制」把渲染后的富文本写入剪贴板
    const copy = converter.locator(MD_SELECTORS.copyButton).first();
    if (!(await has(copy))) {
      return { converted: false, reason: '未找到 md.doocs 复制按钮' };
    }
    await copy.click({ timeout: 5000 }).catch(() => {});
    // 剪贴板写入是异步的，关闭转换页前需等待其完成，否则无法粘贴富文本
    await converter.waitForTimeout(2000);
    return { converted: true, captionHidden };
  } catch (err) {
    return { converted: false, reason: err.message };
  } finally {
    await converter.close().catch(() => {});
  }
}

module.exports = { convertMarkdownToGzhHtml, setFigureCaptionHidden, MD_URL, MD_SELECTORS };
