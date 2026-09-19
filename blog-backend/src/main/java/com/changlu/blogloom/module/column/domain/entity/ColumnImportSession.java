package com.changlu.blogloom.module.column.domain.entity;

import com.changlu.blogloom.module.column.domain.dto.ColumnImportItem;
import com.changlu.blogloom.module.column.domain.dto.ColumnImportOptions;
import lombok.Getter;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
public class ColumnImportSession {
	private final String token;
	private final Path zipPath;
	private final List<ColumnImportItem> items;
	private final Map<String, String> entryMap;
	private final String baseDir;
	private final ColumnImportOptions options;
	private final LocalDateTime expiresAt;

	public ColumnImportSession(String token, Path zipPath, List<ColumnImportItem> items, Map<String, String> entryMap,
	                           String baseDir, ColumnImportOptions options, LocalDateTime expiresAt) {
		this.token = token;
		this.zipPath = zipPath;
		this.items = items;
		this.entryMap = entryMap;
		this.baseDir = baseDir;
		this.options = options;
		this.expiresAt = expiresAt;
	}
}
