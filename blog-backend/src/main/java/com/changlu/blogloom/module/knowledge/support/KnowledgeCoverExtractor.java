package com.changlu.blogloom.module.knowledge.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description: 提取 Markdown 正文中的封面图链接。
 * <p>
 * 封面图不再取正文第一张图，而是**显式标记**：仅 alt 为 coverImg 的图片才视为封面，
 * Markdown 写作 {@code ![coverImg](url)}，HTML 写作 {@code <img alt="coverImg" src="url">}。
 * 两种写法同时存在时取正文中出现位置更靠前的一个；无标记则无封面（返回空串）。
 * @Author: changlu
 * @Date: 2026-09-21
 */
public final class KnowledgeCoverExtractor {
	/** 匹配 Markdown 封面语法 ![coverImg](url)，仅 alt 为 coverImg（大小写不敏感）时才视为封面 */
	private static final Pattern MARKDOWN_COVER = Pattern.compile(
			"!\\[\\s*coverImg\\s*\\]\\s*\\(\\s*<?([^)\\s>]+)>?", Pattern.CASE_INSENSITIVE);
	/** 匹配 HTML 封面语法 <img alt="coverImg" src="url">（属性顺序不限，大小写不敏感） */
	private static final Pattern HTML_COVER = Pattern.compile(
			"<img(?=[^>]*\\balt\\s*=\\s*[\"']\\s*coverImg\\s*[\"'])[^>]*\\bsrc\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
			Pattern.CASE_INSENSITIVE);

	private KnowledgeCoverExtractor() {
	}

	/**
	 * 提取正文中的封面图链接。
	 *
	 * @param content 已移除元数据代码块的 Markdown 正文
	 * @return 封面图链接；无 coverImg 标记时返回空串
	 */
	public static String extract(String content) {
		if (content == null || content.isEmpty()) return "";
		Matcher markdown = MARKDOWN_COVER.matcher(content);
		Matcher html = HTML_COVER.matcher(content);
		boolean hasMarkdown = markdown.find();
		boolean hasHtml = html.find();
		if (!hasMarkdown && !hasHtml) return "";
		if (!hasHtml) return markdown.group(1).trim();
		if (!hasMarkdown) return html.group(1).trim();
		return markdown.start() <= html.start() ? markdown.group(1).trim() : html.group(1).trim();
	}
}