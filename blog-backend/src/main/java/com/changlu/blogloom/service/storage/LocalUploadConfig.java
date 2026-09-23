package com.changlu.blogloom.service.storage;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 本地上传渠道配置，对应 site_setting.uploadChannelLocal 的 JSON 值。
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
public class LocalUploadConfig {
	/**
	 * 本地资源对外访问的地址前缀，例如 http://localhost:8090。
	 */
	private String address;
}