package com.changlu.blogloom.service;

import com.changlu.blogloom.env.EnvironmentContext;
import com.changlu.blogloom.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @Description: Skill 打包服务单测：验证包结构、Token 注入与 pycache 过滤。
 */
class SkillPackageServiceTest {

	private SkillPackageService service;
	private EnvironmentContext environmentContext;

	@BeforeEach
	void setUp() {
		// JwtUtils 的静态配置由 Spring @Value 注入，单测中手动填充
		ReflectionTestUtils.setField(JwtUtils.class, "secretKey", "test-secret-key-for-unit-test-0123456789");
		ReflectionTestUtils.setField(JwtUtils.class, "expireTime", 3600000L);

		environmentContext = mock(EnvironmentContext.class);
		when(environmentContext.getBlogApi()).thenReturn("http://localhost:8090");

		service = new SkillPackageService();
		ReflectionTestUtils.setField(service, "environmentContext", environmentContext);
	}

	@Test
	void shouldPackageSkillWithTokenInjected() throws Exception {
		Map<String, String> entries = unzip(service.buildZip("admin"));

		assertTrue(entries.containsKey("blogloom-skill/SKILL.md"));
		assertTrue(entries.containsKey("blogloom-skill/references/00-tool-contract.md"));
		assertTrue(entries.containsKey("blogloom-skill/scripts/blogloom-skill.py"));
		assertTrue(entries.containsKey("blogloom-skill/scripts/rest_tools.py"));
		assertTrue(entries.containsKey("blogloom-skill/test/test_blogloom_skill.py"));

		String env = entries.get("blogloom-skill/skill.env.sh");
		assertNotNull(env);
		assertTrue(env.contains("http://localhost:8090"), "应注入 BASE_URL");
		assertFalse(env.contains("__BLOOM_API_TOKEN__"), "token 占位符应被替换");
		assertTrue(env.contains("export BLOOM_API_TOKEN=\"ey"), "应注入可用 JWT");
	}

	@Test
	void shouldExcludePycacheArtifacts() throws Exception {
		Map<String, String> entries = unzip(service.buildZip("admin"));
		for (String name : entries.keySet()) {
			assertFalse(name.contains("__pycache__"), "不应打包 __pycache__: " + name);
			assertFalse(name.endsWith(".pyc"), "不应打包 .pyc: " + name);
		}
	}

	private Map<String, String> unzip(byte[] zip) throws Exception {
		Map<String, String> entries = new HashMap<>();
		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				byte[] buffer = new byte[8192];
				StringBuilder content = new StringBuilder();
				int len;
				while ((len = zis.read(buffer)) != -1) {
					content.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
				}
				entries.put(entry.getName(), content.toString());
			}
		}
		assertEquals(entries.size(), entries.keySet().size());
		return entries;
	}
}