package com.changlu.blogloom.service.storage;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 阿里云 OSS 渠道配置，对应 site_setting.uploadChannelAliyun 的 JSON 值。
 */
@NoArgsConstructor
@Getter
@Setter
@ToString(exclude = "accessKeySecret")
public class AliyunOssConfig {
	private String accessKeyId;
	private String accessKeySecret;
	private String bucket;
	private String area;
	private String path;
}
