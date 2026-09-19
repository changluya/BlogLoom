package com.changlu.blogloom.module.knowledge.support;

import com.changlu.blogloom.module.knowledge.domain.dto.KnowledgeArticleMetadata;
import com.changlu.blogloom.util.JacksonUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description: 解析 Markdown 顶部 JSON 代码块元数据，并返回移除该代码块后的正文
 * @Author: changlu
 * @Date: 2026-09-19
 */
public final class KnowledgeArticleMetadataParser {
	private static final Pattern FRONT_BLOCK = Pattern.compile(
			"^\\s*```[ \\t]*(?i:json)?[ \\t]*\\r?\\n(.*?)\\r?\\n```[ \\t]*(?:\\r?\\n|$)",
			Pattern.DOTALL);

	private KnowledgeArticleMetadataParser() {
	}

	public static ParsedArticle parse(String rawContent) {
		if (rawContent == null) return new ParsedArticle(null, "");
		String content = stripBom(rawContent);
		Matcher matcher = FRONT_BLOCK.matcher(content);
		if (!matcher.find()) return new ParsedArticle(null, content);
		KnowledgeArticleMetadata metadata = JacksonUtils.readValue(matcher.group(1).trim(), KnowledgeArticleMetadata.class);
		if (metadata == null) return new ParsedArticle(null, content);
		String cleaned = content.substring(matcher.end()).replaceFirst("^(?:\\r?\\n)+", "");
		return new ParsedArticle(metadata, cleaned);
	}

	private static String stripBom(String content) {
		return !content.isEmpty() && content.charAt(0) == '\uFEFF' ? content.substring(1) : content;
	}

	public static final class ParsedArticle {
		private final KnowledgeArticleMetadata metadata;
		private final String content;

		ParsedArticle(KnowledgeArticleMetadata metadata, String content) {
			this.metadata = metadata;
			this.content = content;
		}

		public KnowledgeArticleMetadata getMetadata() { return metadata; }

		public String getContent() { return content; }
	}
}
