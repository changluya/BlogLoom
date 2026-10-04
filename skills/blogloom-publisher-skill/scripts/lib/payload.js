'use strict';

/** 发布参数构建与删除目标解析。 */

const { parseMarkdown } = require('./markdown');

/**
 * 从 SOP 元数据 + CLI 覆盖值推导发布参数（对齐 SOP：tags/category/columns/articleSummary）。
 * 仅负责**跨渠道通用字段**；渠道专属字段通过 `channel.augmentPayload(base, ctx)` 由其自行补充，
 * 核心层不感知任何渠道业务。
 */
function buildPublishPayload(channel, opts) {
  const params = opts.params || {};
  const file = opts.file || params.file || params.file_path;
  if (!file) throw new Error('缺少 --file（待发布 Markdown 路径）');

  const blog = parseMarkdown(file);
  const tags = (opts.tags || params.tags || blog.tags.join(',')).toString();
  const tagList = tags
    .split(/[,，、|;；]/)
    .map((t) => t.trim())
    .filter(Boolean)
    .slice(0, 10);

  const column = opts.column || params.column || blog.columns[0] || blog.category || '';

  const base = {
    channel: channel.name,
    file: blog.filePath,
    title: opts.title || params.title || blog.title,
    tags: tagList,
    column,
    columns: blog.columns.length ? blog.columns : column ? [column] : [],
    category: blog.category,
    summary: opts.summary || params.summary || blog.summary,
    cover: opts.cover || params.cover || blog.cover,
    createTime: blog.createTime,
    updateTime: blog.updateTime,
    knowledgeBasePath: blog.knowledgeBasePath,
    extra: blog.extra || {},
    content: blog.content,
  };

  if (typeof channel.augmentPayload === 'function') {
    Object.assign(base, channel.augmentPayload(base, { opts, params, blog }) || {});
  }
  return base;
}

/** 解析删除目标：title / url / id（可来自 --file 的元数据标题）。 */
function resolveDeleteTarget(opts, params = {}) {
  const target = {
    url: opts.url || params.url || '',
    id: opts.id || params.id || '',
    title: opts.title || params.title || '',
  };
  const file = opts.file || params.file || params.file_path;
  if (!target.title && file) target.title = parseMarkdown(file).title;
  if (!target.id && target.url) {
    const match = String(target.url).match(/details\/(\d+)/);
    if (match) target.id = match[1];
  }
  if (!target.title && !target.id && !target.url) {
    throw new Error('删除需要提供 --url / --id / --title（或 --file 以标题定位）');
  }
  return target;
}

module.exports = { buildPublishPayload, resolveDeleteTarget };
