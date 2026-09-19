package com.changlu.blogloom.module.knowledge.service;

import com.changlu.blogloom.module.knowledge.domain.dto.KnowledgeImportOptions;
import com.changlu.blogloom.module.knowledge.domain.vo.KnowledgeImportPreview;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface MarkdownArchiveService {
	KnowledgeImportPreview preview(MultipartFile file, KnowledgeImportOptions options);
	KnowledgeImportPreview previewDocuments(List<MultipartFile> files, KnowledgeImportOptions options);
	String execute(String token);
	void exportAll(HttpServletResponse response) throws IOException;
	void exportDocument(Long nodeId, HttpServletResponse response) throws IOException;
}
