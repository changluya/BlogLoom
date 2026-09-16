package com.changlu.blogloom.module.knowledge.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KnowledgeImportEntry {
	private String sourceName;
	private String path;
	private boolean directory;
	private long size;
}
