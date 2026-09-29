'use strict';

/**
 * 编辑器正文写入适配层。
 *
 * 各渠道编辑器内核不同（CSDN 用 ProseMirror/自研富文本，有的用 textarea/CodeMirror），
 * 这里提供一组 strategy，channel 侧按能力选择：
 *   - pasteStrategy      : 剪贴板粘贴（最贴近人工，优先）
 *   - fillStrategy       : 直接 fill textarea / contenteditable
 *   - keyboardStrategy   : focus 后 keyboard.insertText（兜底，慢）
 *   - cdpInsertStrategy  : CDP Input.insertText（底层增强，绕过部分富文本拦截）
 */

/** 把 Markdown 文本经系统剪贴板粘贴到目标元素（保留换行与代码块格式）。 */
async function pasteStrategy(page, selector, text) {
  const locator = page.locator(selector).first();
  await locator.waitFor({ state: 'visible', timeout: 15000 });
  await locator.click();
  await page.evaluate(async (value) => {
    await navigator.clipboard.writeText(value);
  }, text);
  const mod = process.platform === 'darwin' ? 'Meta' : 'Control';
  await page.keyboard.press(`${mod}+V`);
}

/** 直接填充输入控件（textarea / input / contenteditable 原生支持 fill）。 */
async function fillStrategy(page, selector, text) {
  const locator = page.locator(selector).first();
  await locator.waitFor({ state: 'visible', timeout: 15000 });
  await locator.fill(text);
}

/** 聚焦后逐段 insertText。 */
async function keyboardStrategy(page, selector, text) {
  const locator = page.locator(selector).first();
  await locator.waitFor({ state: 'visible', timeout: 15000 });
  await locator.click();
  await page.keyboard.insertText(text);
}

/** CDP 底层增强：通过 Input.insertText 注入，绕过富文本的 paste 事件拦截。 */
async function cdpInsertStrategy(page, selector, text) {
  const locator = page.locator(selector).first();
  await locator.waitFor({ state: 'visible', timeout: 15000 });
  await locator.click();
  const session = await page.context().newCDPSession(page);
  await session.send('Input.insertText', { text });
}

const STRATEGIES = {
  paste: pasteStrategy,
  fill: fillStrategy,
  keyboard: keyboardStrategy,
  cdp: cdpInsertStrategy,
};

/**
 * 按优先级依次尝试写入。
 * @param {string[]} order 例如 ['paste', 'cdp', 'keyboard']
 */
async function writeContent(page, selector, text, order = ['paste', 'cdp', 'keyboard']) {
  const errors = [];
  for (const name of order) {
    const fn = STRATEGIES[name];
    if (!fn) continue;
    try {
      await fn(page, selector, text);
      const actual = await readBackText(page, selector);
      if (actual && actual.trim().length > 0) {
        return { strategy: name, length: actual.length };
      }
      errors.push(`${name}: 写入后内容为空`);
    } catch (err) {
      errors.push(`${name}: ${err.message}`);
    }
  }
  throw new Error(`正文写入失败（已尝试 ${order.join(' → ')}）：\n${errors.join('\n')}`);
}

async function readBackText(page, selector) {
  try {
    const locator = page.locator(selector).first();
    return (await locator.innerText({ timeout: 3000 })) || '';
  } catch (err) {
    return '';
  }
}

module.exports = { writeContent, STRATEGIES, readBackText };