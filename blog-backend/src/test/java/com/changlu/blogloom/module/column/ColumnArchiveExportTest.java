package com.changlu.blogloom.module.column;

import com.changlu.blogloom.config.properties.UploadProperties;
import com.changlu.blogloom.module.column.dao.BlogColumnMapper;
import com.changlu.blogloom.module.column.domain.vo.ColumnTreeVo;
import com.changlu.blogloom.module.column.service.BlogColumnService;
import com.changlu.blogloom.module.column.service.ColumnCoverStorageService;
import com.changlu.blogloom.module.column.service.impl.ColumnArchiveServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ColumnArchiveExportTest {

	@Test
	@SuppressWarnings("unchecked")
	void exportUsesImportStructure() throws Exception {
		Path upload = Files.createTempDirectory("column-export-");
		System.setProperty("user.dir.upload", upload.toString());
		try {
			Path iconDir = upload.resolve("blogColumn").resolve("1");
			Files.createDirectories(iconDir);
			Files.write(iconDir.resolve("logo.png"), new byte[]{(byte) 0x89, 'P', 'N', 'G'});

			ColumnTreeVo root = new ColumnTreeVo();
			root.setId(1L);
			root.setParentId(0L);
			root.setName("根专栏");
			root.setDescription("根专栏简介");
			root.setSort(10);
			root.setCover("http://localhost:8090/static/blogColumn/1/logo.png");
			ColumnTreeVo child = new ColumnTreeVo();
			child.setId(2L);
			child.setParentId(1L);
			child.setName("子专栏");
			child.setDescription("子专栏简介");
			child.setSort(10);
			child.setCover("");
			root.getChildren().add(child);

			BlogColumnService columnService = mock(BlogColumnService.class);
			when(columnService.getAdminTree()).thenReturn(Collections.singletonList(root));
			ColumnArchiveServiceImpl service = new ColumnArchiveServiceImpl(columnService, mock(BlogColumnMapper.class),
					mock(ColumnCoverStorageService.class), new UploadProperties());

			MockHttpServletResponse response = new MockHttpServletResponse();
			service.exportAll(response);

			Map<String, byte[]> entries = new LinkedHashMap<>();
			try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(response.getContentAsByteArray()), StandardCharsets.UTF_8)) {
				ZipEntry entry;
				while ((entry = zip.getNextEntry()) != null) {
					entries.put(entry.getName(), entry.isDirectory() ? new byte[0] : readAll(zip));
				}
			}

			assertTrue(entries.containsKey("columns/"), "缺少 columns/ 根目录");
			assertTrue(entries.containsKey("columns/columns.json"), "缺少 columns/columns.json");
			assertTrue(entries.containsKey("columns/icons/logo.png"), "缺少 columns/icons/logo.png");

			List<Map<String, Object>> json = new ObjectMapper().readValue(entries.get("columns/columns.json"), List.class);
			assertEquals(1, json.size());
			assertEquals("根专栏", json.get(0).get("title"));
			assertEquals("icons/logo.png", json.get(0).get("icon"));
			assertEquals(10, json.get(0).get("order"));
			List<Map<String, Object>> children = (List<Map<String, Object>>) json.get(0).get("children");
			assertEquals(1, children.size());
			assertEquals("子专栏", children.get(0).get("title"));
			assertFalse(children.get(0).containsKey("icon"), "无图标的专栏不应输出 icon");
			assertFalse(children.get(0).containsKey("children"), "叶子专栏不应输出 children");
		} finally {
			System.clearProperty("user.dir.upload");
		}
	}

	private byte[] readAll(InputStream input) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int read;
		while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
		return output.toByteArray();
	}
}
