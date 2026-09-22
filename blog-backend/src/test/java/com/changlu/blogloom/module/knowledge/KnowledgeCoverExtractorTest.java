package com.changlu.blogloom.module.knowledge;

import com.changlu.blogloom.module.knowledge.support.KnowledgeCoverExtractor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 封面图提取规则单测：仅 alt 为 coverImg 的图片才作为封面。
 */
class KnowledgeCoverExtractorTest {

	@Test
	void shouldIgnorePlainMarkdownImage() {
		assertEquals("", KnowledgeCoverExtractor.extract("正文\n![pic](http://a.png)\n结尾"));
	}

	@Test
	void shouldExtractMarkdownCoverImg() {
		assertEquals("http://cover.png", KnowledgeCoverExtractor.extract("正文\n![coverImg](http://cover.png)\n结尾"));
	}

	@Test
	void shouldExtractHtmlCoverImg() {
		assertEquals("http://cover.png",
				KnowledgeCoverExtractor.extract("<img alt=\"coverImg\" src=\"http://cover.png\">"));
	}

	@Test
	void shouldExtractHtmlCoverImgWithReversedAttributeOrder() {
		assertEquals("http://cover.png",
				KnowledgeCoverExtractor.extract("<img src=\"http://cover.png\" alt=\"coverImg\">"));
	}

	@Test
	void shouldIgnoreHtmlImageWithoutCoverAlt() {
		assertEquals("", KnowledgeCoverExtractor.extract("<img alt=\"banner\" src=\"http://a.png\">"));
	}

	@Test
	void shouldTakeEarlierCoverWhenBothStylesExist() {
		String content = "<img alt=\"coverImg\" src=\"http://first.png\">\n![coverImg](http://second.png)";
		assertEquals("http://first.png", KnowledgeCoverExtractor.extract(content));
	}

	@Test
	void shouldBeCaseInsensitive() {
		assertEquals("http://cover.png", KnowledgeCoverExtractor.extract("![COVERIMG](http://cover.png)"));
	}

	@Test
	void shouldReturnEmptyWhenNoCoverMarked() {
		assertEquals("", KnowledgeCoverExtractor.extract("纯文本，没有图片"));
		assertEquals("", KnowledgeCoverExtractor.extract(null));
		assertEquals("", KnowledgeCoverExtractor.extract(""));
	}
}