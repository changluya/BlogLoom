package com.changlu.blogloom.service;

import com.changlu.blogloom.config.properties.BlogProperties;
import com.changlu.blogloom.config.properties.UploadProperties;
import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.exception.PersistenceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 本地资源存储服务。
 *
 * <p>所有本地资源都以 user.dir.upload 为根目录，临时资源进入 tmp，文章保存
 * 时再复制到 blogs/{blogId}。文件名由服务端生成，避免直接使用客户端文件名造成
 * 路径穿越或重名覆盖。</p>
 */
@Service
public class LocalResourceStorageService {
	@Autowired
	private UploadProperties uploadProperties;
	@Autowired
	private BlogProperties blogProperties;

	/**
	 * 将编辑器上传的资源保存到临时目录。
	 */
	public Map<String, String> uploadTemp(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("上传文件不能为空");
		}

		String extension = getExtension(file);
		String fileName = UUID.randomUUID().toString() + "." + extension;
		Path tempDirectory = Paths.get(uploadProperties.getTempPath()).toAbsolutePath().normalize();
		Path target = tempDirectory.resolve(fileName).normalize();
		try {
			Files.createDirectories(tempDirectory);
			file.transferTo(target.toFile());
		} catch (IOException e) {
			throw new PersistenceException("保存本地上传资源失败", e);
		}

		Map<String, String> result = new LinkedHashMap<>(4);
		result.put("name", fileName);
		result.put("relativePath", "tmp/" + fileName);
		result.put("url", buildAccessUrl("tmp/" + fileName));
		return result;
	}

	/**
	 * 将 uploadRoot 下的本地资源复制到指定博客目录。
	 *
	 * <p>目标路径统一为：{@code {uploadRoot}/blogs/{blogId}/{targetRelativePath}}</p>
	 * <pre>
	 * 源相对路径                         目标相对路径（blogId=100）
	 * tmp/a.png                     -> blogs/100/a.png
	 * blogs/20/a.png                -> blogs/100/a.png
	 * tmp/2026/09/a.png             -> blogs/100/2026/09/a.png
	 * other/a.png                   -> blogs/100/other/a.png
	 * </pre>
	 *
	 * <p>即：去掉 {@code tmp/} 或 {@code blogs/{旧blogId}/} 前缀，保留后续文件层级；
	 * 其他路径则完整保留。源文件已在目标位置时不重复复制。</p>
	 *
	 * @param blogId             目标博客 ID
	 * @param sourceRelativePath 相对 uploadRoot 的源资源路径
	 * @return 相对 uploadRoot 的归档路径；源文件不存在时返回 {@code null}
	 */
	public String copyToBlog(Long blogId, String sourceRelativePath) {
		// 1. 将源路径限制在 uploadRoot 内，并确认源文件存在。
		Path source = resolveUnderUploadRoot(sourceRelativePath);
		if (source == null || !Files.isRegularFile(source)) {
			return null;
		}

		// 2. 去掉 tmp/ 或 blogs/{旧blogId}/ 前缀，生成博客目录内的相对路径。
		String targetRelativePath = getTargetRelativePath(sourceRelativePath);
		Path blogDirectory = Paths.get(uploadProperties.getBlogPath(blogId)).toAbsolutePath().normalize();
		Path target = blogDirectory.resolve(targetRelativePath).normalize();
		if (!target.startsWith(blogDirectory)) {
			throw new BadRequestException("资源路径不合法");
		}

		// 3. 创建目标目录并复制文件；同路径时直接复用原文件。
		try {
			Files.createDirectories(target.getParent());
			if (!source.toAbsolutePath().normalize().equals(target)) {
				Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			throw new PersistenceException("归档博客资源失败", e);
		}
		// 4. 返回可供 buildAccessUrl 继续组装的相对路径。
		return "blogs/" + blogId + "/" + targetRelativePath.replace('\\', '/');
	}

	/**
	 * 将文章封面归档到 {@code blogs/{blogId}/cover/{fileName}}。
	 * 文件名沿用上传阶段生成的 UUID，封面与正文资源在目录层面保持隔离。
	 */
	public String copyToBlogCover(Long blogId, String sourceRelativePath) {
		Path source = resolveUnderUploadRoot(sourceRelativePath);
		if (source == null || !Files.isRegularFile(source)) {
			return null;
		}

		Path blogDirectory = Paths.get(uploadProperties.getBlogPath(blogId)).toAbsolutePath().normalize();
		Path coverDirectory = blogDirectory.resolve("cover").normalize();
		Path target = coverDirectory.resolve(source.getFileName().toString()).normalize();
		if (!target.startsWith(coverDirectory)) {
			throw new BadRequestException("资源路径不合法");
		}

		try {
			Files.createDirectories(coverDirectory);
			if (!source.toAbsolutePath().normalize().equals(target)) {
				Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			throw new PersistenceException("归档博客封面失败", e);
		}
		return "blogs/" + blogId + "/cover/" + source.getFileName().toString();
	}

	/**
	 * 删除已经成功归档的临时资源。这里只允许删除 upload/tmp 目录下的文件。
	 */
	public void deleteTemporaryResource(String sourceRelativePath) {
		if (sourceRelativePath == null || !sourceRelativePath.replace('\\', '/').startsWith("tmp/")) {
			return;
		}
		Path source = resolveUnderUploadRoot(sourceRelativePath);
		Path tempDirectory = Paths.get(uploadProperties.getTempPath()).toAbsolutePath().normalize();
		if (source == null || !source.startsWith(tempDirectory)) {
			throw new BadRequestException("临时资源路径不合法");
		}
		try {
			Files.deleteIfExists(source);
		} catch (IOException e) {
			throw new PersistenceException("删除临时资源失败", e);
		}
	}

	public String buildAccessUrl(String relativePath) {
		String api = blogProperties.getApi();
		if (api == null) {
			api = "";
		}
		while (api.endsWith("/")) {
			api = api.substring(0, api.length() - 1);
		}
		return api + "/static/" + relativePath.replace('\\', '/');
	}

	private Path resolveUnderUploadRoot(String relativePath) {
		if (relativePath == null || relativePath.trim().isEmpty()) {
			return null;
		}
		String normalizedRelativePath = relativePath.replace('\\', '/');
		Path root = Paths.get(uploadProperties.getPath()).toAbsolutePath().normalize();
		Path candidate = root.resolve(normalizedRelativePath).normalize();
		return candidate.startsWith(root) ? candidate : null;
	}

	private String getTargetRelativePath(String sourceRelativePath) {
		String normalized = sourceRelativePath.replace('\\', '/');
		if (normalized.startsWith("tmp/")) {
			return normalized.substring("tmp/".length());
		}
		if (normalized.startsWith("blogs/")) {
			int separator = normalized.indexOf('/', "blogs/".length());
			if (separator >= 0 && separator + 1 < normalized.length()) {
				return normalized.substring(separator + 1);
			}
		}
		return normalized;
	}

	private String getExtension(MultipartFile file) {
		String originalFilename = file.getOriginalFilename();
		if (originalFilename != null) {
			int separator = Math.max(originalFilename.lastIndexOf('/'), originalFilename.lastIndexOf('\\'));
			int dot = originalFilename.lastIndexOf('.');
			if (dot > separator && dot + 1 < originalFilename.length()) {
				String extension = originalFilename.substring(dot + 1).toLowerCase();
				if (extension.matches("[a-z0-9]{1,10}")) {
					return extension;
				}
			}
		}
		String contentType = file.getContentType();
		if ("image/jpeg".equalsIgnoreCase(contentType)) {
			return "jpg";
		}
		if ("image/png".equalsIgnoreCase(contentType)) {
			return "png";
		}
		if ("image/gif".equalsIgnoreCase(contentType)) {
			return "gif";
		}
		if ("image/webp".equalsIgnoreCase(contentType)) {
			return "webp";
		}
		return "bin";
	}
}
