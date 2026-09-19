package com.changlu.blogloom.module.knowledge.domain.entity;

import com.changlu.blogloom.module.knowledge.domain.dto.KnowledgeImportOptions;
import lombok.Getter;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @Description: 导入会话。支持两种来源：
 * <ul>
 *     <li>ZIP 归档：持有临时 {@code zipPath}，文档内容从 ZIP Entry 读取；</li>
 *     <li>Markdown 文件：持有 {@code documents}（Entry 路径 -&gt; 文件字节），路径已按 knowledgeBasePath 归一化。</li>
 * </ul>
 */
@Getter
public class KnowledgeImportSession {
	private final String token;
	/** ZIP 模式下的临时压缩包路径；文件模式下为 null */
	private final Path zipPath;
	/** 文件模式下的文档内容（Entry 路径 -> UTF-8 字节）；ZIP 模式下为 null */
	private final Map<String, byte[]> documents;
	private final List<KnowledgeImportEntry> entries;
	private final KnowledgeImportOptions options;
	private final LocalDateTime expiresAt;

	/** ZIP 归档导入会话 */
	public KnowledgeImportSession(String token, Path zipPath, List<KnowledgeImportEntry> entries,
	                              KnowledgeImportOptions options, LocalDateTime expiresAt) {
		this(token, zipPath, null, entries, options, expiresAt);
	}

	/** Markdown 文件导入会话 */
	public KnowledgeImportSession(String token, Map<String, byte[]> documents, List<KnowledgeImportEntry> entries,
	                              KnowledgeImportOptions options, LocalDateTime expiresAt) {
		this(token, null, documents, entries, options, expiresAt);
	}

	private KnowledgeImportSession(String token, Path zipPath, Map<String, byte[]> documents,
	                               List<KnowledgeImportEntry> entries, KnowledgeImportOptions options,
	                               LocalDateTime expiresAt) {
		this.token = token;
		this.zipPath = zipPath;
		this.documents = documents;
		this.entries = entries;
		this.options = options;
		this.expiresAt = expiresAt;
	}

	/** 是否为 ZIP 归档导入 */
	public boolean isArchive() { return zipPath != null; }
}
