'use strict';

/**
 * 轻量 Playwright 模拟层：用于在无浏览器环境下对渠道脚本做单测。
 *
 * 支持渠道脚本实际用到的 API 子集：
 *   page.locator / goto / url / waitForTimeout / waitForURL / keyboard / context / evaluate
 *   locator.first / nth / filter({hasText,has}) / locator(child) / count / click / fill /
 *   innerText / inputValue / getAttribute / waitFor / hover / press / scrollIntoViewIfNeeded
 *
 * 元素注册：el(selector, { text, value, href, visible, children, onClick })
 * - children 支持容器内子元素（container.locator(child)）
 * - onClick(page, element) 可制造副作用（如切换 url、删除元素）
 */

function stripState(part) {
  return part.replace(/:visible/g, '').replace(/:first/g, '').trim();
}

function parsePart(part) {
  const hasTexts = [...part.matchAll(/:has-text\(["']?([^"')]+)["']?\)/g)].map((m) => m[1]);
  const cleaned = stripState(part.replace(/:has-text\([^)]*\)/g, ''));
  const tokens = cleaned.split(/\s+/).filter(Boolean);
  return { cleaned, last: tokens[tokens.length - 1] || '', hasTexts };
}

/** 纯 class 选择器 → class 列表；含组合符/属性/标签则返回 null。 */
function classTokens(selector) {
  if (!selector.startsWith('.') || /[\s[\]()>+~,]/.test(selector)) return null;
  return selector.slice(1).split('.').filter(Boolean);
}

function el(selector, options = {}) {
  return {
    selector,
    text: options.text || '',
    value: options.value || '',
    href: options.href || '',
    visible: options.visible !== false,
    attrs: options.attrs || {},
    children: options.children || [],
    onClick: options.onClick || null,
    removed: false,
  };
}

function descendants(element) {
  const out = [];
  for (const child of element.children || []) {
    out.push(child, ...descendants(child));
  }
  return out;
}

function partMatches(element, part) {
  const { cleaned, last, hasTexts } = parsePart(part);
  if (hasTexts.some((t) => !element.text.includes(t))) return false;
  if (!cleaned) return hasTexts.length > 0;
  const sel = element.selector;
  if (sel === cleaned) return true;
  // 纯 class 选择器按 class token 匹配，避免 `.a` 误配 `.a__b`
  const qTokens = classTokens(cleaned);
  const eTokens = classTokens(sel);
  if (qTokens && eTokens) {
    const set = new Set(eTokens);
    return qTokens.every((t) => set.has(t));
  }
  if (last && (sel === last || sel.includes(last) || last.includes(sel))) return true;
  return false;
}

class MockLocator {
  constructor(page, selector, { scope = null, nth = null, text = null, has = null } = {}) {
    this.page = page;
    this.selector = selector;
    this.scope = scope; // 父元素数组（用于 container.locator(child)）
    this.nthIndex = nth;
    this.text = text; // string | RegExp
    this.has = has; // MockLocator
  }

  _candidates() {
    if (this.scope) {
      return this.scope.flatMap((parent) => descendants(parent));
    }
    return this.page.elements;
  }

  _resolve() {
    const parts = this.selector.split(',').map((p) => p.trim()).filter(Boolean);
    let matched = this._candidates().filter((element) => {
      if (element.removed) return false;
      if (this.selector.includes(':visible') && !element.visible) return false;
      return parts.some((part) => partMatches(element, part));
    });

    if (this.text !== null) {
      matched = matched.filter((element) =>
        this.text instanceof RegExp ? this.text.test(element.text) : element.text.includes(this.text)
      );
    }
    if (this.has) {
      const hasParts = this.has.selector.split(',').map((p) => p.trim()).filter(Boolean);
      matched = matched.filter((element) =>
        descendants(element).some((child) => hasParts.some((part) => partMatches(child, part)))
      );
    }
    if (this.nthIndex !== null) {
      matched = matched.slice(this.nthIndex, this.nthIndex + 1);
    }
    return matched;
  }

  _one() {
    const list = this._resolve();
    if (list.length === 0) {
      const err = new Error(`mock: no element for "${this.selector}"`);
      err.code = 'MOCK_EMPTY';
      throw err;
    }
    return list[0];
  }

  first() {
    return new MockLocator(this.page, this.selector, { ...this._opts(), nth: 0 });
  }

  nth(index) {
    return new MockLocator(this.page, this.selector, { ...this._opts(), nth: index });
  }

  last() {
    const list = this._resolve();
    return new MockLocator(this.page, this.selector, { ...this._opts(), nth: Math.max(0, list.length - 1) });
  }

  _opts() {
    return { scope: this.scope, text: this.text, has: this.has };
  }

  filter({ hasText = null, has = null } = {}) {
    return new MockLocator(this.page, this.selector, {
      ...this._opts(),
      text: hasText !== null ? hasText : this.text,
      has: has || this.has,
    });
  }

  locator(childSelector) {
    return new MockLocator(this.page, childSelector, { scope: this._resolve() });
  }

  async count() {
    return this._resolve().length;
  }

  async click(options = {}) {
    const element = this._one();
    this.page.calls.push({ type: 'click', selector: this.selector, opts: options });
    this.page.lastClickedElement = element;
    if (typeof element.onClick === 'function') element.onClick(this.page, element);
  }

  async fill(value) {
    const element = this._one();
    element.value = value;
    this.page.calls.push({ type: 'fill', selector: this.selector, value });
  }

  async press(key) {
    const element = this._one();
    this.page.calls.push({ type: 'press', selector: this.selector, key });
    if (key === 'Enter' && typeof element.onEnter === 'function') element.onEnter(element.value);
  }

  async isVisible() {
    const list = this._resolve();
    return list.length > 0 && list.some((element) => element.visible);
  }

  async isChecked() {
    const element = this._resolve()[0];
    return !!(element && element.checked);
  }

  async hover() {
    this._one();
    this.page.calls.push({ type: 'hover', selector: this.selector });
  }

  async scrollIntoViewIfNeeded() {
    this.page.calls.push({ type: 'scroll', selector: this.selector });
  }

  async waitFor(options = {}) {
    const state = options.state || 'visible';
    const list = this._resolve();
    if (state === 'hidden') {
      if (list.some((element) => element.visible)) {
        throw new Error(`mock: element still visible for "${this.selector}"`);
      }
      return;
    }
    if (list.length === 0) {
      throw new Error(`mock: waitFor(${state}) timeout for "${this.selector}"`);
    }
    if (state === 'visible' && !list.some((element) => element.visible)) {
      throw new Error(`mock: element not visible for "${this.selector}"`);
    }
  }

  async innerText() {
    const list = this._resolve();
    return list.map((element) => element.text).join('\n');
  }

  async inputValue() {
    return this._one().value;
  }

  async getAttribute(name) {
    const element = this._resolve()[0];
    if (!element) return null;
    if (name === 'href') return element.href;
    return element.attrs[name] !== undefined ? element.attrs[name] : null;
  }

  async evaluate(fn) {
    const element = this._resolve()[0];
    if (!element) return undefined;
    if (typeof fn === 'function') {
      // 模拟浏览器 DOM 元素：支持 .checked / .click()
      const proxy = {
        checked: !!element.checked,
        click: () => {
          this.page.calls.push({ type: 'click', selector: this.selector });
          if (typeof element.onClick === 'function') element.onClick(this.page, element);
        },
      };
      try {
        return fn(proxy);
      } catch (err) {
        return undefined;
      }
    }
    return element.value;
  }
}

class MockPage {
  constructor(elements = []) {
    this.elements = elements;
    this.currentUrl = 'about:blank';
    this.calls = [];
    this.keyboard = {
      press: async (key) => {
        this.calls.push({ type: 'keyPress', key });
        if (key === 'Backspace' && this.lastClickedElement) this.lastClickedElement.value = '';
      },
      type: async (text) => {
        this.calls.push({ type: 'keyType', text });
        if (this.lastClickedElement) this.lastClickedElement.value = (this.lastClickedElement.value || '') + text;
      },
      insertText: async (text) => {
        this.calls.push({ type: 'insertText', text });
        if (this.lastClickedElement) this.lastClickedElement.value = (this.lastClickedElement.value || '') + text;
      },
    };
    this.contextObj = {
      newCDPSession: async () => ({ send: async (method, params) => this.calls.push({ type: 'cdp', method, params }) }),
      addInitScript: async () => {},
      cookies: async () => [],
    };
  }

  removeElement(element) {
    element.removed = true;
  }

  locator(selector) {
    return new MockLocator(this, selector);
  }

  async goto(url) {
    this.currentUrl = url;
    this.calls.push({ type: 'goto', url });
  }

  url() {
    return this.currentUrl;
  }

  async waitForTimeout(ms) {
    this.calls.push({ type: 'waitForTimeout', ms });
  }

  async waitForURL(pattern) {
    if (!pattern.test(this.currentUrl)) {
      throw new Error(`mock: waitForURL timeout (${this.currentUrl})`);
    }
  }

  async evaluate() {
    return undefined;
  }

  context() {
    return this.contextObj;
  }

  called(selector, type) {
    return this.calls.some(
      (c) => c.selector && (c.selector === selector || c.selector.includes(selector)) && (!type || c.type === type)
    );
  }
}

module.exports = { MockPage, MockLocator, el };
