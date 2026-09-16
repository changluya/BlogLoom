package com.changlu.blogloom.module.knowledge;

import com.changlu.blogloom.module.knowledge.domain.enums.KnowledgeImportTaskStatus;
import com.changlu.blogloom.module.knowledge.domain.vo.KnowledgeImportProgress;
import com.changlu.blogloom.module.knowledge.service.impl.InMemoryKnowledgeImportTaskService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InMemoryKnowledgeImportTaskServiceTest {
	@Test
	void shouldTrackProgressAndSuccess() {
		InMemoryKnowledgeImportTaskService service = new InMemoryKnowledgeImportTaskService();
		String id = service.create(2);
		service.start(id);
		service.advance(id, "Java", true, false);
		KnowledgeImportProgress running = service.get(id);
		assertEquals(KnowledgeImportTaskStatus.RUNNING, running.getStatus());
		assertEquals(50, running.getProgress());
		assertEquals(1, running.getCreatedDirectoryCount());

		service.advance(id, "Java/IOC.md", false, false);
		service.succeed(id);
		KnowledgeImportProgress success = service.get(id);
		assertEquals(KnowledgeImportTaskStatus.SUCCESS, success.getStatus());
		assertEquals(100, success.getProgress());
		assertEquals(1, success.getCreatedBlogCount());
		assertNotNull(success.getFinishedAt());
	}

	@Test
	void shouldExposeFailureMessage() {
		InMemoryKnowledgeImportTaskService service = new InMemoryKnowledgeImportTaskService();
		String id = service.create(1);
		service.start(id);
		service.fail(id, "解析失败", "bad.md");
		KnowledgeImportProgress failed = service.get(id);
		assertEquals(KnowledgeImportTaskStatus.FAILED, failed.getStatus());
		assertEquals("解析失败", failed.getMessage());
		assertEquals("bad.md", failed.getCurrentPath());
	}
}
