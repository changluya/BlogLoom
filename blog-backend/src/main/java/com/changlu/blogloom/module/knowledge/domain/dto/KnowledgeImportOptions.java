package com.changlu.blogloom.module.knowledge.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeImportOptions {
	private Long targetParentId = 0L;
	private Boolean published = true;
	private String conflictPolicy = "SKIP";
}
