package com.changlu.blogloom.module.knowledge.service.impl;

import com.changlu.blogloom.exception.NotFoundException;
import com.changlu.blogloom.module.knowledge.domain.enums.KnowledgeImportTaskStatus;
import com.changlu.blogloom.module.knowledge.domain.vo.KnowledgeImportProgress;
import com.changlu.blogloom.module.knowledge.service.KnowledgeImportTaskService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryKnowledgeImportTaskService implements KnowledgeImportTaskService {
	private final Map<String, KnowledgeImportProgress> tasks = new ConcurrentHashMap<>();

	@Override
	public String create(int total) {
		String id = UUID.randomUUID().toString();
		KnowledgeImportProgress task = new KnowledgeImportProgress();
		task.setTaskId(id);
		task.setStatus(KnowledgeImportTaskStatus.PENDING);
		task.setTotal(total);
		task.setMessage("等待导入");
		tasks.put(id, task);
		return id;
	}

	@Override
	public void start(String taskId) {
		KnowledgeImportProgress task = require(taskId);
		synchronized (task) {
			task.setStatus(KnowledgeImportTaskStatus.RUNNING);
			task.setStartedAt(LocalDateTime.now());
			task.setMessage("开始导入");
		}
	}

	@Override
	public void advance(String taskId, String path, boolean directory, boolean skipped) {
		KnowledgeImportProgress task = require(taskId);
		synchronized (task) {
			task.setProcessed(task.getProcessed() + 1);
			task.setCurrentPath(path);
			if (skipped) task.setSkippedCount(task.getSkippedCount() + 1);
			else if (directory) task.setCreatedDirectoryCount(task.getCreatedDirectoryCount() + 1);
			else task.setCreatedBlogCount(task.getCreatedBlogCount() + 1);
			task.setProgress(task.getTotal() == 0 ? 100 : Math.min(99, task.getProcessed() * 100 / task.getTotal()));
			task.setMessage((skipped ? "已跳过 " : "正在导入 ") + path);
		}
	}

	@Override
	public void succeed(String taskId) {
		KnowledgeImportProgress task = require(taskId);
		synchronized (task) {
			task.setStatus(KnowledgeImportTaskStatus.SUCCESS);
			task.setProgress(100);
			task.setFinishedAt(LocalDateTime.now());
			task.setMessage("导入完成");
		}
	}

	@Override
	public void fail(String taskId, String message, String path) {
		KnowledgeImportProgress task = require(taskId);
		synchronized (task) {
			task.setStatus(KnowledgeImportTaskStatus.FAILED);
			task.setCurrentPath(path);
			task.setFinishedAt(LocalDateTime.now());
			task.setMessage(message == null ? "导入失败" : message);
		}
	}

	@Override
	public KnowledgeImportProgress get(String taskId) {
		KnowledgeImportProgress task = require(taskId);
		synchronized (task) { return copy(task); }
	}

	@Scheduled(fixedDelay = 300000)
	public void cleanExpired() {
		LocalDateTime deadline = LocalDateTime.now().minusMinutes(30);
		tasks.entrySet().removeIf(entry -> entry.getValue().getFinishedAt() != null && entry.getValue().getFinishedAt().isBefore(deadline));
	}

	private KnowledgeImportProgress require(String id) {
		KnowledgeImportProgress task = tasks.get(id);
		if (task == null) throw new NotFoundException("导入任务已过期或服务已重启");
		return task;
	}

	private KnowledgeImportProgress copy(KnowledgeImportProgress source) {
		KnowledgeImportProgress value = new KnowledgeImportProgress();
		value.setTaskId(source.getTaskId()); value.setStatus(source.getStatus()); value.setProgress(source.getProgress());
		value.setTotal(source.getTotal()); value.setProcessed(source.getProcessed()); value.setCurrentPath(source.getCurrentPath());
		value.setCreatedDirectoryCount(source.getCreatedDirectoryCount()); value.setCreatedBlogCount(source.getCreatedBlogCount());
		value.setSkippedCount(source.getSkippedCount()); value.setMessage(source.getMessage());
		value.setStartedAt(source.getStartedAt()); value.setFinishedAt(source.getFinishedAt());
		return value;
	}
}
