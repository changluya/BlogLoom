package com.changlu.blogloom.module.knowledge;

import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.module.knowledge.domain.dto.KnowledgeImportOptions;
import com.changlu.blogloom.module.knowledge.domain.vo.KnowledgeImportPreview;
import com.changlu.blogloom.module.knowledge.service.impl.MarkdownArchiveServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownArchiveServiceImplTest {
	private final MarkdownArchiveServiceImpl service = new MarkdownArchiveServiceImpl(null, null, null);

	@Test
	void shouldPlaceDocumentsByKnowledgeBasePath() {
		MockMultipartFile nested = md("note.md", "```json\n{\"title\":\"嵌套\",\"knowledgeBasePath\":\"/a/bb/cc\"}\n```\n正文");
		MockMultipartFile root = md("root.md", "```json\n{\"title\":\"根目录\"}\n```\n正文");
		KnowledgeImportPreview preview = service.previewDocuments(Arrays.asList(nested, root), options());
		assertEquals(2, preview.getDocumentCount());
		assertEquals(3, preview.getDirectoryCount());
		assertTrue(preview.getPaths().contains("a/bb/cc/note.md"));
		assertTrue(preview.getPaths().contains("root.md"));
	}

	@Test
	void shouldDeduplicateSameFileNameInSameDirectory() {
		MockMultipartFile first = md("note.md", "```json\n{\"knowledgeBasePath\":\"/a\"}\n```\n正文");
		MockMultipartFile second = md("note.md", "```json\n{\"knowledgeBasePath\":\"/a\"}\n```\n正文");
		KnowledgeImportPreview preview = service.previewDocuments(Arrays.asList(first, second), options());
		assertTrue(preview.getPaths().contains("a/note.md"));
		assertTrue(preview.getPaths().contains("a/note (1).md"));
	}

	@Test
	void shouldRejectInvalidKnowledgeBasePath() {
		MockMultipartFile file = md("bad.md", "```json\n{\"knowledgeBasePath\":\"/a/bb/\"}\n```\n正文");
		assertThrows(BadRequestException.class, () -> service.previewDocuments(Collections.singletonList(file), options()));
	}

	@Test
	void shouldRejectNonMarkdownFile() {
		MockMultipartFile file = new MockMultipartFile("files", "note.txt", "text/plain", "正文".getBytes(StandardCharsets.UTF_8));
		assertThrows(BadRequestException.class, () -> service.previewDocuments(Collections.singletonList(file), options()));
	}

	private MockMultipartFile md(String name, String content) {
		return new MockMultipartFile("files", name, "text/markdown", content.getBytes(StandardCharsets.UTF_8));
	}

	private KnowledgeImportOptions options() {
		KnowledgeImportOptions options = new KnowledgeImportOptions();
		options.setTargetParentId(0L);
		options.setPublished(true);
		options.setConflictPolicy("SKIP");
		return options;
	}
}
