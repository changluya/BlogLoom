package com.changlu.blogloom.module.knowledge;

import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.module.knowledge.support.KnowledgeBasePathSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KnowledgeBasePathSupportTest {
	@Test
	void shouldReturnEmptyForNullAndBlank() {
		assertEquals("", KnowledgeBasePathSupport.normalize(null));
		assertEquals("", KnowledgeBasePathSupport.normalize(""));
		assertEquals("", KnowledgeBasePathSupport.normalize("   "));
	}

	@Test
	void shouldNormalizeValidPath() {
		assertEquals("a/bb/cc", KnowledgeBasePathSupport.normalize("/a/bb/cc"));
		assertEquals("a", KnowledgeBasePathSupport.normalize("/a"));
	}

	@Test
	void shouldRejectPathWithoutLeadingSlash() {
		assertThrows(BadRequestException.class, () -> KnowledgeBasePathSupport.normalize("a/bb"));
	}

	@Test
	void shouldRejectTrailingSlash() {
		assertThrows(BadRequestException.class, () -> KnowledgeBasePathSupport.normalize("/a/bb/"));
	}

	@Test
	void shouldRejectEmptySegment() {
		assertThrows(BadRequestException.class, () -> KnowledgeBasePathSupport.normalize("/a//bb"));
	}

	@Test
	void shouldRejectDotSegments() {
		assertThrows(BadRequestException.class, () -> KnowledgeBasePathSupport.normalize("/a/./bb"));
		assertThrows(BadRequestException.class, () -> KnowledgeBasePathSupport.normalize("/a/../bb"));
	}

	@Test
	void shouldRejectBackslashAndNullChar() {
		assertThrows(BadRequestException.class, () -> KnowledgeBasePathSupport.normalize("/a\\bb"));
		assertThrows(BadRequestException.class, () -> KnowledgeBasePathSupport.normalize("/a\0bb"));
	}
}
