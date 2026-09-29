'use strict';

/**
 * Markdown SOP 解析：与 `codes/标准生成发布输出博客sop.md` 保持一致。
 *
 * 每篇文章 = 文件最顶部一个 ```json 元数据代码块 + 正常 Markdown 正文。
 * 封面图（首图）在正文中用 ![coverImg](url) / <img alt="coverImg" src="url"> 显式标记。
 */

const fs = require('fs');
const path = require('path');

const META_BLOCK = /^\uFEFF?\s*```[ \t]*json[ \t]*\r?\n([\s\S]*?)\r?\n```[ \t]*\r?\n?/i;

/**
 * 宽松解析元数据 JSON：
 * 1. 先按标准 JSON 解析；
 * 2. 失败时容忍 SOP 样例常见笔误（全角冒号、尾随逗号、全角引号）后再解析；
 * 3. 仍失败则返回 null（视为无元数据，代码块保留在正文）。
 */
function lenientJsonParse(text) {
  try {
    const parsed = JSON.parse(text);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : null;
  } catch (err) {
    // fallthrough
  }
  try {
    const cleaned = text
      .replace(/[\u201c\u201d]/g, '"') // 全角双引号
      .replace(/：/g, ':') // 全角冒号
      .replace(/,\s*([}\]])/g, '$1'); // 尾随逗号
    const parsed = JSON.parse(cleaned);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : null;
  } catch (err) {
    return null;
  }
}

/** 把数组 / 逗号分隔字符串统一为去空字符串数组（兼容中英文标点）。 */
function normalizeList(value) {
  if (value === undefined || value === null) return [];
  const parts = Array.isArray(value) ? value : String(value).split(/[,，、|;；]/);
  return parts.map((item) => String(item).trim()).filter(Boolean);
}

function pickField(metadata, ...keys) {
  for (const key of keys) {
    const value = metadata[key];
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      return String(value).trim();
    }
  }
  return '';
}

/** 只认 alt=coverImg 的图片作为封面（Markdown / HTML，取正文中更靠前者）。 */
function pickCoverImage(content) {
  const text = content || '';
  const candidates = [];

  const mdRe = /!\[\s*coverImg\s*\]\s*\(\s*<?([^)\s>]+)>?/gi;
  let match;
  while ((match = mdRe.exec(text)) !== null) {
    candidates.push({ index: match.index, url: match[1] });
  }

  const htmlRe = /<img(?=[^>]*\balt\s*=\s*["']\s*coverImg\s*["'])[^>]*\bsrc\s*=\s*["']([^"']+)["'][^>]*>/gi;
  while ((match = htmlRe.exec(text)) !== null) {
    candidates.push({ index: match.index, url: match[1] });
  }

  if (candidates.length === 0) return '';
  candidates.sort((a, b) => a.index - b.index);
  return candidates[0].url;
}

/** 无摘要时从正文截取一段纯文本（去掉图片/HTML/标记符号）。 */
function deriveSummary(content, limit = 150) {
  const plain = String(content || '')
    .replace(/!\[[^\]]*\]\([^)]*\)/g, '')
    .replace(/<[^>]+>/g, '')
    .replace(/[#>*`~\-[\]()!]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
  return plain.slice(0, limit);
}

/**
 * 解析单篇 Markdown。
 * @returns {{
 *   filePath: string, title: string, tags: string[], category: string,
 *   columns: string[], summary: string, createTime: string, updateTime: string,
 *   knowledgeBasePath: string, cover: string, content: string, metadata: object
 * }}
 */
function parseMarkdown(filePath) {
  const abs = path.resolve(filePath);
  if (!fs.existsSync(abs) || !fs.statSync(abs).isFile()) {
    throw new Error(`文件不存在: ${filePath}`);
  }

  const raw = fs.readFileSync(abs, 'utf8');
  const match = META_BLOCK.exec(raw);
  let metadata = {};
  let content = raw;

  if (match) {
    const parsed = lenientJsonParse(match[1]);
    if (parsed) {
      metadata = parsed;
      content = raw.slice(match[0].length);
    }
    // 解析失败：视为无元数据，代码块保留在正文（与平台导入规则一致）
  }

  const fallbackTitle = path.basename(abs, path.extname(abs));
  const title = pickField(metadata, 'title') || fallbackTitle;
  const coverFromMeta = pickField(metadata, 'cover', 'coverImg', 'firstPicture', 'coverImage');

  return {
    filePath: abs,
    title,
    tags: normalizeList(pickField(metadata, 'tags')),
    category: pickField(metadata, 'category'),
    categories: normalizeList(pickField(metadata, 'categories', 'category')),
    columns: normalizeList(pickField(metadata, 'columns', 'column')),
    summary: pickField(metadata, 'articleSummary', 'summary', 'description') || deriveSummary(content),
    createTime: pickField(metadata, 'createTime'),
    updateTime: pickField(metadata, 'updateTime'),
    knowledgeBasePath: pickField(metadata, 'knowledgeBasePath'),
    cover: pickCoverImage(content) || coverFromMeta,
    content,
    metadata,
    extra: metadata,
  };
}

/** 统计正文中的图片数量（Markdown + HTML）。 */
function countImages(content) {
  const text = content || '';
  const md = (text.match(/!\[[^\]]*\]\([^)]*\)/g) || []).length;
  const html = (text.match(/<img\b[^>]*>/gi) || []).length;
  return md + html;
}

/**
 * 按图片数量估算「粘贴正文后」的等待时间：默认 2s，每满 5 张图片再多 3s（不足 5 张按一组算）。
 * 无图片返回 0。
 */
function estimateImageWaitMs(content, { base = 2000, blockSize = 5, perBlock = 3000, max = 60000 } = {}) {
  const count = countImages(content);
  if (count === 0) return 0;
  const blocks = Math.ceil(count / blockSize);
  return Math.min(base + blocks * perBlock, max);
}

module.exports = {
  parseMarkdown,
  normalizeList,
  pickCoverImage,
  deriveSummary,
  lenientJsonParse,
  countImages,
  estimateImageWaitMs,
  META_BLOCK,
};
