package com.changlu.blogloom.env;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Data
public class EnvironmentContext {

	@Autowired
	private Environment environment;

	public String getServerPort() {
		return environment.getProperty("server.port", "8090");
	}

	public String getBlogName() {
		return environment.getProperty("blog.name", "BlogLoom");
	}

	public String getBlogApi() {
		return environment.getProperty("blog.api", "http://localhost:" + getServerPort());
	}

	public String getBlogCms() {
		return environment.getProperty("blog.cms", "http://localhost:8080");
	}

	public String getBlogView() {
		return environment.getProperty("blog.view", "http://localhost:8081");
	}

	public String getUploadChannel() {
		return environment.getProperty("upload.channel", "local");
	}

	public String getUploadFilePath() {
		return environment.getProperty("upload.file.path", SystemPropertyUtil.getConfDir() + "/upload/");
	}

	public String getUploadFileAccessPath() {
		return environment.getProperty("upload.file.access-path", "/image/**");
	}

	public String getUploadFileResourcesLocations() {
		return environment.getProperty("upload.file.resources-locations", "file:" + getUploadFilePath());
	}

	public String getCommentNotifyChannel() {
		return environment.getProperty("comment.notify.channel", "mail");
	}

	public boolean isCommentDefaultOpen() {
		return Boolean.parseBoolean(environment.getProperty("comment.default-open", "true"));
	}

	public String getConfigFilePath() {
		return environment.getProperty(
				"blogloom.config.file",
				SystemPropertyUtil.getDefaultConfigFile().toString()
		);
	}

	public String getLogbackFilePath() {
		return environment.getProperty(
				"blogloom.logback.file",
				SystemPropertyUtil.getDefaultLogbackFile().toString()
		);
	}
}
