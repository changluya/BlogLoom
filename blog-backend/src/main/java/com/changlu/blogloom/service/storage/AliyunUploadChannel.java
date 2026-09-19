package com.changlu.blogloom.service.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.exception.PersistenceException;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

/**
 * 阿里云 OSS 上传渠道。
 *
 * <p>Object Key = 配置 path 前缀 + 渠道内相对路径；访问地址默认为
 * {@code https://{bucket}.{area}.aliyuncs.com/{key}}。</p>
 */
@Component
public class AliyunUploadChannel implements UploadChannel {
	@Autowired
	private ImageHostConfigService imageHostConfigService;

	private static final byte[] PROBE = "blogloom-connectivity-test".getBytes(java.nio.charset.StandardCharsets.UTF_8);

	@Override
	public String name() {
		return ALIYUN;
	}

	/**
	 * 连通性测试：用给定参数在 tmp 目录模拟上传后立即删除。
	 * 参数为空时回退读取已保存的渠道配置。
	 */
	public void testConnection(AliyunOssConfig requestConfig) {
		AliyunOssConfig config = requestConfig != null && !isBlank(requestConfig.getAccessKeyId())
				? requestConfig : requireConfig();
		if (isBlank(config.getAccessKeyId()) || isBlank(config.getAccessKeySecret())
				|| isBlank(config.getBucket()) || isBlank(config.getArea())) {
			throw new BadRequestException("阿里云图床参数不完整，请先补全 accessKeyId、accessKeySecret、bucket、area");
		}
		String relativePath = "tmp/connectivity-test-" + java.util.UUID.randomUUID() + ".png";
		String key = objectKey(config, relativePath);
		OSS client = buildClient(config);
		try {
			client.putObject(config.getBucket(), key, new ByteArrayInputStream(PROBE));
			client.deleteObject(config.getBucket(), key);
		} catch (OSSException | ClientException e) {
			throw new BadRequestException("阿里云 OSS 连通性测试失败: " + e.getMessage(), e);
		} finally {
			client.shutdown();
		}
	}

	@Override
	public UploadedResource upload(byte[] content, String extension, String directory, String fileName) {
		AliyunOssConfig config = requireConfig();
		String relativePath = StoragePaths.join(directory, fileName);
		OSS client = buildClient(config);
		try {
			client.putObject(config.getBucket(), objectKey(config, relativePath), new ByteArrayInputStream(content));
		} catch (OSSException | ClientException e) {
			throw new PersistenceException("上传到阿里云 OSS 失败: " + e.getMessage(), e);
		} finally {
			client.shutdown();
		}
		return new UploadedResource(fileName, relativePath, buildUrl(config, relativePath));
	}

	@Override
	public String copy(String sourceRelativePath, String targetRelativePath) {
		AliyunOssConfig config = requireConfig();
		OSS client = buildClient(config);
		try {
			String sourceKey = objectKey(config, sourceRelativePath);
			if (!client.doesObjectExist(config.getBucket(), sourceKey)) {
				return null;
			}
			client.copyObject(config.getBucket(), sourceKey, config.getBucket(),
					objectKey(config, targetRelativePath));
		} catch (OSSException | ClientException e) {
			throw new PersistenceException("阿里云 OSS 资源归档失败: " + e.getMessage(), e);
		} finally {
			client.shutdown();
		}
		return targetRelativePath.replace('\\', '/');
	}

	@Override
	public void delete(String relativePath) {
		AliyunOssConfig config = requireConfig();
		OSS client = buildClient(config);
		try {
			client.deleteObject(config.getBucket(), objectKey(config, relativePath));
		} catch (OSSException | ClientException e) {
			throw new PersistenceException("删除阿里云 OSS 资源失败: " + e.getMessage(), e);
		} finally {
			client.shutdown();
		}
	}

	@Override
	public String buildUrl(String relativePath) {
		return buildUrl(requireConfig(), relativePath);
	}

	private String buildUrl(AliyunOssConfig config, String relativePath) {
		return publicBase(config) + encodeKey(objectKey(config, relativePath));
	}

	@Override
	public String toRelativePath(String url) {
		if (url == null || url.isEmpty()) {
			return null;
		}
		AliyunOssConfig config;
		try {
			config = requireConfig();
		} catch (RuntimeException e) {
			return null;
		}
		String path;
		try {
			URI uri = new URI(url);
			if (!uri.isAbsolute()) {
				return null;
			}
			String scheme = uri.getScheme();
			if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
				return null;
			}
			path = uri.getPath();
		} catch (URISyntaxException e) {
			return null;
		}
		if (path == null) {
			return null;
		}
		while (path.startsWith("/")) {
			path = path.substring(1);
		}
		String prefix = pathPrefix(config);
		if (!prefix.isEmpty()) {
			if (!path.startsWith(prefix)) {
				return null;
			}
			path = path.substring(prefix.length());
		}
		if (path.startsWith("tmp/") || path.startsWith("blogs/")) {
			return path;
		}
		return null;
	}

	private AliyunOssConfig requireConfig() {
		AliyunOssConfig config = imageHostConfigService.getAliyunConfig();
		if (config == null || isBlank(config.getAccessKeyId()) || isBlank(config.getAccessKeySecret())
				|| isBlank(config.getBucket()) || isBlank(config.getArea())) {
			throw new BadRequestException("阿里云图床参数不完整，请在【图床管理 - 设置图床】中完善配置");
		}
		return config;
	}

	private OSS buildClient(AliyunOssConfig config) {
		return new OSSClientBuilder().build("https://" + endpointHost(config),
				config.getAccessKeyId(), config.getAccessKeySecret());
	}

	private String publicBase(AliyunOssConfig config) {
		return "https://" + config.getBucket().trim() + "." + endpointHost(config) + "/";
	}

	private String endpointHost(AliyunOssConfig config) {
		String area = config.getArea().trim();
		area = area.replaceFirst("(?i)^https?://", "");
		while (area.endsWith("/")) {
			area = area.substring(0, area.length() - 1);
		}
		if (!area.contains(".aliyuncs.com")) {
			area = area + ".aliyuncs.com";
		}
		return area;
	}

	private String objectKey(AliyunOssConfig config, String relativePath) {
		return pathPrefix(config) + relativePath.replace('\\', '/');
	}

	private String pathPrefix(AliyunOssConfig config) {
		String path = config.getPath() == null ? "" : config.getPath().trim().replace('\\', '/');
		while (path.startsWith("/")) {
			path = path.substring(1);
		}
		if (!path.isEmpty() && !path.endsWith("/")) {
			path = path + "/";
		}
		return path;
	}

	private String encodeKey(String key) {
		String[] segments = key.split("/", -1);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < segments.length; i++) {
			if (i > 0) {
				builder.append('/');
			}
			try {
				builder.append(URLEncoder.encode(segments[i], "UTF-8").replace("+", "%20"));
			} catch (UnsupportedEncodingException e) {
				builder.append(segments[i]);
			}
		}
		return builder.toString();
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
