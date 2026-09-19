package com.changlu.blogloom.service.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 上传结果。
 */
@Getter
@AllArgsConstructor
public class UploadedResource {
	private final String name;
	private final String relativePath;
	private final String url;
}
