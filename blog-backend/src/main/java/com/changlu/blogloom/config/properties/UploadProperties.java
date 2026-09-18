package com.changlu.blogloom.config.properties;

import org.springframework.context.annotation.Configuration;
import com.changlu.blogloom.env.SystemPropertyUtil;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静态文件上传访问路径配置(目前用于评论中QQ头像的本地存储)
 *
 * @author: changlu
 * @date: 2026-09-13
 */
@Configuration
public class UploadProperties {
	public String getPath() {
		return SystemPropertyUtil.getLocalUploadFileDir();
	}

	public String getTempPath() {
		return getPath("tmp").toString();
	}

	public String getBlogPath(Long blogId) {
		return getPath("blogs", String.valueOf(blogId)).toString();
	}

	public String getBlogColumnPath(Long columnId) {
		return getPath("blogColumn", String.valueOf(columnId)).toString();
	}

	public String getSitePath() {
		return getPath("site").toString();
	}

	public String getAccessPath() {
		return "/static/**";
	}

	public String getResourcesLocations() {
		return "file:" + getPath() + "/";
	}

	private Path getPath(String... children) {
		Path path = Paths.get(getPath());
		for (String child : children) {
			path = path.resolve(child);
		}
		return path.normalize();
	}
}
