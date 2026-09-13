package com.changlu.blogloom.config.properties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 又拍云配置(目前用于评论中QQ头像的图床)
 *
 * @author: changlu
 * @date: 2026-09-13
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
@Configuration
@ConfigurationProperties(prefix = "upload.upyun")
public class UpyunProperties {
	/**
	 * 又拍云存储空间名称
	 */
	private String bucketName;
	/**
	 * 操作员名称
	 */
	private String username;
	/**
	 * 操作员密码
	 */
	private String password;
	/**
	 * 存储路径
	 */
	private String path;
	/**
	 * CDN访问域名
	 */
	private String domain;
}
