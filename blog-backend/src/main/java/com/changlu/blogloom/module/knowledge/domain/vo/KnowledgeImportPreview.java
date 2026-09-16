package com.changlu.blogloom.module.knowledge.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class KnowledgeImportPreview {
	private String token;
	private int directoryCount;
	private int documentCount;
	private int ignoredCount;
	private long totalUncompressedBytes;
	private List<String> paths;
}
