package com.changlu.blogloom.service;

import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.exception.PersistenceException;
import com.changlu.blogloom.service.storage.UploadChannelManager;
import com.changlu.blogloom.service.storage.UploadedResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 资源存储服务：所有操作按当前上传渠道委派给对应的 {@link com.changlu.blogloom.service.storage.UploadChannel}。
 *
 * <p>临时资源进入 tmp，文章保存时复制到 blogs/{blogId}；相对路径语义由渠道实现解释，
 * 本地渠道对应 upload 根目录，阿里云渠道对应 Bucket 内去除 path 前缀后的 Object Key。</p>
 */
@Service
public class ResourceStorageService {
	@Autowired
	private UploadChannelManager uploadChannelManager;

	/**
	 * 将编辑器上传的资源保存到临时目录。
	 */
	public Map<String, String> uploadTemp(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("上传文件不能为空");
		}
		String extension = getExtension(file);
		String fileName = UUID.randomUUID().toString() + "." + extension;
		byte[] content;
		try {
			content = file.getBytes();
		} catch (IOException e) {
			throw new PersistenceException("读取上传文件失败", e);
		}
		UploadedResource resource = uploadChannelManager.getCurrentChannel()
				.upload(content, extension, "tmp", fileName);
		Map<String, String> result = new LinkedHashMap<>(4);
		result.put("name", resource.getName());
		result.put("relativePath", resource.getRelativePath());
		result.put("url", resource.getUrl());
		return result;
	}

	/**
	 * 将渠道内的资源复制到指定博客目录。
	 *
	 * <p>目标路径统一为：{@code blogs/{blogId}/{targetRelativePath}}，其中来源
	 * {@code tmp/} 或 {@code blogs/{旧blogId}/} 前缀会被去除，其余层级保留。</p>
	 *
	 * @return 相对路径；源资源不存在时返回 {@code null}
	 */
	public String copyToBlog(Long blogId, String sourceRelativePath) {
		String targetRelativePath = "blogs/" + blogId + "/" + getTargetRelativePath(sourceRelativePath);
		return uploadChannelManager.getCurrentChannel().copy(sourceRelativePath, targetRelativePath);
	}

	/**
	 * 将文章封面复制到 {@code blogs/{blogId}/cover/{fileName}}。
	 */
	public String copyToBlogCover(Long blogId, String sourceRelativePath) {
		String targetRelativePath = "blogs/" + blogId + "/cover/" + getFileName(sourceRelativePath);
		return uploadChannelManager.getCurrentChannel().copy(sourceRelativePath, targetRelativePath);
	}

	/**
	 * 删除已经成功归档的临时资源，仅允许删除 tmp 目录下的资源。
	 */
	public void deleteTemporaryResource(String sourceRelativePath) {
		if (sourceRelativePath == null || !sourceRelativePath.replace('\\', '/').startsWith("tmp/")) {
			return;
		}
		uploadChannelManager.getCurrentChannel().delete(sourceRelativePath.replace('\\', '/'));
	}

	public String buildAccessUrl(String relativePath) {
		return uploadChannelManager.getCurrentChannel().buildUrl(relativePath);
	}

	/**
	 * 将受当前渠道托管的 URL 还原为相对路径，非受管资源返回 null。
	 */
	public String toManagedRelativePath(String url) {
		return uploadChannelManager.getCurrentChannel().toRelativePath(url);
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

	private String getFileName(String path) {
		String normalized = path.replace('\\', '/');
		int slash = normalized.lastIndexOf('/');
		return slash < 0 ? normalized : normalized.substring(slash + 1);
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
