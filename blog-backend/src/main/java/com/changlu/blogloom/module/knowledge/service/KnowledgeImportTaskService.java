package com.changlu.blogloom.module.knowledge.service;

import com.changlu.blogloom.module.knowledge.domain.vo.KnowledgeImportProgress;

public interface KnowledgeImportTaskService {
	String create(int total);
	void start(String taskId);
	void advance(String taskId, String path, boolean directory, boolean skipped);
	void succeed(String taskId);
	void fail(String taskId, String message, String path);
	KnowledgeImportProgress get(String taskId);
}
