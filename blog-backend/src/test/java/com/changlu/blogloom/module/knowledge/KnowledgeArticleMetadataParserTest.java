package com.changlu.blogloom.module.knowledge;

import com.changlu.blogloom.module.knowledge.domain.dto.KnowledgeArticleMetadata;
import com.changlu.blogloom.module.knowledge.support.KnowledgeArticleMetadataParser;
import com.changlu.blogloom.module.knowledge.support.KnowledgeArticleMetadataParser.ParsedArticle;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeArticleMetadataParserTest {
	@Test
	void shouldParseJsonBlockAndRemoveItFromContent() {
		String markdown = "```json\n" +
				"{\"title\":\"JVM 内存模型\",\"articleSummary\":\"堆与栈\",\"tags\":[\"Java\",\"JVM\"],\"category\":\"后端\",\"columns\":\"Java 进阶\"}\n" +
				"```\n" +
				"# JVM 内存模型\n\n![cover](https://cdn.example.com/cover.png)\n正文";
		ParsedArticle parsed = KnowledgeArticleMetadataParser.parse(markdown);
		KnowledgeArticleMetadata metadata = parsed.getMetadata();
		assertNotNull(metadata);
		assertEquals("JVM 内存模型", metadata.getTitle());
		assertEquals("堆与栈", metadata.getArticleSummary());
		assertEquals(Arrays.asList("Java", "JVM"), metadata.getTags());
		assertEquals("后端", metadata.getCategory());
		assertEquals(Arrays.asList("Java 进阶"), metadata.getColumns());
		assertTrue(parsed.getContent().startsWith("# JVM 内存模型"));
	}

	@Test
	void shouldSupportCommaSeparatedTags() {
		String markdown = "```json\n{\"tags\":\"Java，JVM、并发\"}\n```\n正文";
		ParsedArticle parsed = KnowledgeArticleMetadataParser.parse(markdown);
		assertEquals(Arrays.asList("Java", "JVM", "并发"), parsed.getMetadata().getTags());
	}

	@Test
	void shouldSupportMultipleColumns() {
		String markdown = "```json\n{\"title\":\"SqlParser解析器快速入门\",\"tags\":\"AI技术,SEO优化,内容营销\"," +
				"\"category\":\"SqlParser\",\"articleSummary\":\"摘要\",\"columns\":\"SqlParser, AI\"}\n```\n正文";
		ParsedArticle parsed = KnowledgeArticleMetadataParser.parse(markdown);
		KnowledgeArticleMetadata metadata = parsed.getMetadata();
		assertNotNull(metadata);
		assertEquals("SqlParser", metadata.getCategory());
		assertEquals(Arrays.asList("SqlParser", "AI"), metadata.getColumns());
		assertEquals(Arrays.asList("SqlParser", "AI"), metadata.allColumns());
	}

	@Test
	void shouldDeduplicateColumns() {
		String markdown = "```json\n{\"columns\":\"AI,Java 进阶,AI\"}\n```\n正文";
		ParsedArticle parsed = KnowledgeArticleMetadataParser.parse(markdown);
		assertEquals(Arrays.asList("AI", "Java 进阶"), parsed.getMetadata().allColumns());
	}

	@Test
	void shouldIgnoreNonJsonFirstBlock() {
		String markdown = "```java\nSystem.out.println(1);\n```\n正文";
		ParsedArticle parsed = KnowledgeArticleMetadataParser.parse(markdown);
		assertNull(parsed.getMetadata());
		assertEquals(markdown, parsed.getContent());
	}

	@Test
	void shouldKeepContentWhenJsonIsInvalid() {
		String markdown = "```json\n{not-json}\n```\n正文";
		ParsedArticle parsed = KnowledgeArticleMetadataParser.parse(markdown);
		assertNull(parsed.getMetadata());
		assertEquals(markdown, parsed.getContent());
	}

	@Test
	void shouldReturnOriginalContentWhenNoBlock() {
		String markdown = "# 标题\n正文";
		ParsedArticle parsed = KnowledgeArticleMetadataParser.parse(markdown);
		assertNull(parsed.getMetadata());
		assertEquals(markdown, parsed.getContent());
	}
}
