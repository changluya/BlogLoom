package com.changlu.blogloom.service;

import com.changlu.blogloom.constant.JwtConstants;
import com.changlu.blogloom.env.EnvironmentContext;
import com.changlu.blogloom.exception.PersistenceException;
import com.changlu.blogloom.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @Description: BlogLoom Skill 打包下载服务。
 *
 * <p>将内置在 classpath 下的 {@code skill/blogloom-skill} 目录打包为 ZIP，并在打包时把
 * 当前登录用户的 {@code BASE_URL} 与专属 {@code API_TOKEN} 注入 {@code skill.env.sh}，
 * 使用户下载后开箱即用。</p>
 *
 * @Author: changlu
 * @Date: 2026-09-21
 */
@Service
public class SkillPackageService {

	private static final String SKILL_CLASSPATH_PATTERN = "classpath*:skill/blogloom-skill/**";
	private static final String SKILL_ROOT = "skill/blogloom-skill/";
	private static final String ZIP_ROOT_DIR = "blogloom-skill/";
	private static final String ENV_FILE = "skill.env.sh";
	private static final String TOKEN_PLACEHOLDER = "__BLOOM_API_TOKEN__";

	@Autowired
	private EnvironmentContext environmentContext;

	/**
	 * 将 Skill 打包并写入响应流，文件名形如 {@code blogloom-skill-<timestamp>.zip}。
	 *
	 * @param username 当前登录用户名，用于签发 token
	 * @param response HTTP 响应
	 */
	public void download(String username, HttpServletResponse response) throws IOException {
		byte[] zip = buildZip(username);

		String fileName = "blogloom-skill.zip";
		String encoded = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
		// 注意：不能调用 response.reset()，否则会清掉 CORS 过滤器已写入的跨域响应头
		response.setContentType("application/zip");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Content-Disposition",
				"attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encoded);
		response.setContentLength(zip.length);
		try (OutputStream out = response.getOutputStream()) {
			out.write(zip);
			out.flush();
		}
	}

	/**
	 * 组装 Skill ZIP 字节流（便于测试）。
	 */
	public byte[] buildZip(String username) {
		String baseUrl = environmentContext.getBlogApi();
		String token = JwtUtils.generateToken(JwtConstants.ADMIN_PREFIX + username,
				AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_admin"));

		List<Resource> resources = resolveResources();
		if (resources.isEmpty()) {
			throw new PersistenceException("未找到内置的 BlogLoom Skill 模板");
		}

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
			for (Resource resource : resources) {
				String relativePath = resolveRelativePath(resource);
				if (relativePath == null || relativePath.isEmpty()) {
					continue;
				}
				byte[] content = readResource(resource, relativePath, baseUrl, token);
				ZipEntry entry = new ZipEntry(ZIP_ROOT_DIR + relativePath);
				entry.setTime(System.currentTimeMillis());
				zos.putNextEntry(entry);
				zos.write(content);
				zos.closeEntry();
			}
		} catch (IOException e) {
			throw new PersistenceException("打包 BlogLoom Skill 失败", e);
		}
		return buffer.toByteArray();
	}

	private List<Resource> resolveResources() {
		try {
			PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
			Resource[] found = resolver.getResources(SKILL_CLASSPATH_PATTERN);
			List<Resource> result = new ArrayList<>();
			for (Resource resource : found) {
				if (resource.isReadable()) {
					result.add(resource);
				}
			}
			return result;
		} catch (IOException e) {
			throw new PersistenceException("读取 BlogLoom Skill 模板失败", e);
		}
	}

	private String resolveRelativePath(Resource resource) throws IOException {
		String url = resource.getURL().toString().replace('\\', '/');
		int index = url.indexOf(SKILL_ROOT);
		if (index < 0) {
			return null;
		}
		String relative = url.substring(index + SKILL_ROOT.length());
		// 过滤目录占位、Mac 元数据与构建缓存文件
		if (relative.isEmpty() || relative.endsWith("/") || relative.startsWith("__MACOSX")
				|| relative.contains("__MACOSX/") || relative.contains("__pycache__")
				|| relative.endsWith(".pyc")) {
			return null;
		}
		return relative;
	}

	private byte[] readResource(Resource resource, String relativePath, String baseUrl, String token)
			throws IOException {
		try (InputStream in = resource.getInputStream()) {
			byte[] raw = readAll(in);
			if (ENV_FILE.equals(relativePath)) {
				String text = new String(raw, StandardCharsets.UTF_8)
						.replace(TOKEN_PLACEHOLDER, token)
						.replace("__BLOOM_BASE_URL__", baseUrl);
				return text.getBytes(StandardCharsets.UTF_8);
			}
			return raw;
		}
	}

	private byte[] readAll(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] chunk = new byte[8192];
		int len;
		while ((len = in.read(chunk)) != -1) {
			out.write(chunk, 0, len);
		}
		return out.toByteArray();
	}
}