package com.changlu.blogloom.module.seo;

import com.changlu.blogloom.env.SystemPropertyUtil;
import com.changlu.blogloom.module.seo.api.SpaIndexController;
import com.changlu.blogloom.service.SiteSettingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpaIndexControllerTest {

	@TempDir
	Path tempDir;

	@Test
	void shouldInjectVerificationMetaIntoIndexHtml() throws Exception {
		Path viewDir = tempDir.resolve("static").resolve("view");
		Files.createDirectories(viewDir);
		String original = "<html><head><title>BlogLoom</title></head><body><div id=\"app\"></div></body></html>";
		Files.write(viewDir.resolve("index.html"), original.getBytes(StandardCharsets.UTF_8));

		SiteSettingService settings = mock(SiteSettingService.class);
		Map<String, String> codes = new LinkedHashMap<>();
		codes.put("baidu", "codeva-xxx");
		codes.put("bing", "ABCDEF");
		codes.put("google", "");
		when(settings.getSeoVerifications()).thenReturn(codes);

		String previous = System.getProperty(SystemPropertyUtil.CONF_DIR_PROPERTY);
		System.setProperty(SystemPropertyUtil.CONF_DIR_PROPERTY, tempDir.toString());
		try {
			ResponseEntity<String> response = new SpaIndexController(settings).index();
			assertEquals(200, response.getStatusCodeValue());
			String body = response.getBody();
			assertTrue(body.contains("<meta name=\"baidu-site-verification\" content=\"codeva-xxx\">"));
			assertTrue(body.contains("<meta name=\"msvalidate.01\" content=\"ABCDEF\">"));
			assertTrue(!body.contains("google-site-verification"));
			assertTrue(body.contains("</head>"));
			// 注入的内容位于 </head> 之前
			assertTrue(body.indexOf("baidu-site-verification") < body.indexOf("</head>"));
		} finally {
			if (previous == null) {
				System.clearProperty(SystemPropertyUtil.CONF_DIR_PROPERTY);
			} else {
				System.setProperty(SystemPropertyUtil.CONF_DIR_PROPERTY, previous);
			}
		}
	}
}
