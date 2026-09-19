package com.changlu.blogloom.module.column.service;

import com.changlu.blogloom.module.column.domain.dto.ColumnImportOptions;
import com.changlu.blogloom.module.column.domain.vo.ColumnImportPreview;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public interface ColumnArchiveService {
	ColumnImportPreview preview(MultipartFile file, ColumnImportOptions options);
	Map<String, Object> execute(String token);
	void exportAll(HttpServletResponse response) throws IOException;
}
