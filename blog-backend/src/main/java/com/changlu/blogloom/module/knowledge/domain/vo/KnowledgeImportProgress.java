package com.changlu.blogloom.module.knowledge.domain.vo;

import com.changlu.blogloom.module.knowledge.domain.enums.KnowledgeImportTaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class KnowledgeImportProgress {
	private String taskId;
	private KnowledgeImportTaskStatus status;
	private int progress;
	private int total;
	private int processed;
	private String currentPath;
	private int createdDirectoryCount;
	private int createdBlogCount;
	private int skippedCount;
	private String message;
	private LocalDateTime startedAt;
	private LocalDateTime finishedAt;
}
