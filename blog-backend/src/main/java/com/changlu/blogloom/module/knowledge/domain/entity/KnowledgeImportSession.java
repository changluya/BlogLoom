package com.changlu.blogloom.module.knowledge.domain.entity;

import com.changlu.blogloom.module.knowledge.domain.dto.KnowledgeImportOptions;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class KnowledgeImportSession {
	private String token;
	private Path zipPath;
	private List<KnowledgeImportEntry> entries;
	private KnowledgeImportOptions options;
	private LocalDateTime expiresAt;
}
