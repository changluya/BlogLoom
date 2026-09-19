package com.changlu.blogloom.service.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.changlu.blogloom.config.properties.BlogProperties;
import com.changlu.blogloom.config.properties.UploadProperties;
import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.exception.PersistenceException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地上传渠道：资源写入 upload 根目录，通过 /static/** 访问。
 */
@Component
public class LocalUploadChannel implements UploadChannel {
	@Autowired
	private UploadProperties uploadProperties;
	@Autowired
	private BlogProperties blogProperties;

	private static final byte[] PROBE = "blogloom-connectivity-test".getBytes(java.nio.charset.StandardCharsets.UTF_8);

	@Override
	public String name() {
		return LOCAL;
	}

	/**
	 * 连通性测试：在 tmp 目录模拟上传后立即删除。
	 */
	public void testConnection() {
		String relativePath = "tmp/connectivity-test-" + java.util.UUID.randomUUID() + ".png";
		Path target = resolveUnderRoot(relativePath);
		try {
			Files.createDirectories(target.getParent());
			Files.write(target, PROBE);
			Files.deleteIfExists(target);
		} catch (IOException e) {
			throw new PersistenceException("本地上传连通性测试失败", e);
		}
	}

	@Override
	public UploadedResource upload(byte[] content, String extension, String directory, String fileName) {
		String relativePath = StoragePaths.join(directory, fileName);
		Path target = resolveUnderRoot(relativePath);
		try {
			Files.createDirectories(target.getParent());
			Files.write(target, content);
		} catch (IOException e) {
			throw new PersistenceException("保存本地资源失败", e);
		}
		return new UploadedResource(fileName, relativePath, buildUrl(relativePath));
	}

	@Override
	public String copy(String sourceRelativePath, String targetRelativePath) {
		Path source = resolveUnderRoot(sourceRelativePath);
		if (!Files.isRegularFile(source)) {
			return null;
		}
		Path target = resolveUnderRoot(targetRelativePath);
		try {
			Files.createDirectories(target.getParent());
			if (!source.toAbsolutePath().normalize().equals(target.toAbsolutePath().normalize())) {
				Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			throw new PersistenceException("归档本地资源失败", e);
		}
		return targetRelativePath.replace('\\', '/');
	}

	@Override
	public void delete(String relativePath) {
		Path target = resolveUnderRoot(relativePath);
		try {
			Files.deleteIfExists(target);
		} catch (IOException e) {
			throw new PersistenceException("删除本地资源失败", e);
		}
	}

	@Override
	public String buildUrl(String relativePath) {
		String api = blogProperties.getApi() == null ? "" : blogProperties.getApi();
		while (api.endsWith("/")) {
			api = api.substring(0, api.length() - 1);
		}
		return api + "/static/" + relativePath.replace('\\', '/');
	}

	@Override
	public String toRelativePath(String url) {
		if (url == null || url.isEmpty()) {
			return null;
		}
		String path = url;
		try {
			URI uri = new URI(url);
			if (uri.isAbsolute()) {
				String scheme = uri.getScheme();
				if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
					return null;
				}
				path = uri.getPath();
			}
		} catch (URISyntaxException e) {
			return null;
		}
		if (path == null) {
			return null;
		}
		while (path.startsWith("/")) {
			path = path.substring(1);
		}
		if (path.startsWith("static/")) {
			return path.substring("static/".length());
		}
		if (path.startsWith("image/")) {
			return path.substring("image/".length());
		}
		if (path.startsWith("upload/tmp/")) {
			return path.substring("upload/".length());
		}
		if (path.startsWith("upload/blogs/")) {
			return path.substring("upload/".length());
		}
		if (path.startsWith("tmp/") || path.startsWith("blogs/")) {
			return path;
		}
		return null;
	}

	private Path resolveUnderRoot(String relativePath) {
		Path root = Paths.get(uploadProperties.getPath()).toAbsolutePath().normalize();
		Path target = root.resolve(relativePath.replace('\\', '/')).normalize();
		if (!target.startsWith(root)) {
			throw new BadRequestException("资源路径不合法");
		}
		return target;
	}
}
