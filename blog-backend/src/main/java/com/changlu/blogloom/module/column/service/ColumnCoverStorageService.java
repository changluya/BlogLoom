package com.changlu.blogloom.module.column.service;

import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.exception.PersistenceException;
import com.changlu.blogloom.service.storage.UploadChannelManager;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 专栏封面存储：按当前上传渠道写入 blogColumn/{columnId} 目录。
 */
@Service
public class ColumnCoverStorageService {
	private static final long MAX_SIZE = 5L * 1024 * 1024;
	private static final Set<String> EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "webp"));

	private final UploadChannelManager uploadChannelManager;

	public ColumnCoverStorageService(UploadChannelManager uploadChannelManager) {
		this.uploadChannelManager = uploadChannelManager;
	}

	public String save(Long columnId, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("专栏图片不能为空");
		}
		try {
			return save(columnId, file.getOriginalFilename(), file.getBytes());
		} catch (IOException e) {
			throw new PersistenceException("读取专栏图片失败", e);
		}
	}

	public String save(Long columnId, String originalFilename, byte[] content) {
		if (content == null || content.length == 0) {
			throw new BadRequestException("专栏图片不能为空");
		}
		if (content.length > MAX_SIZE) {
			throw new BadRequestException("专栏图片不能超过 5MB");
		}
		String extension = extension(originalFilename);
		if (!EXTENSIONS.contains(extension)) {
			throw new BadRequestException("仅支持 jpg、jpeg、png、webp 图片");
		}
		String fileName = UUID.randomUUID().toString() + "." + extension;
		return uploadChannelManager.getCurrentChannel()
				.upload(content, extension, "blogColumn/" + columnId, fileName).getUrl();
	}

	private String extension(String name) {
		if (name == null) {
			return "";
		}
		int dot = name.lastIndexOf('.');
		return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
	}
}
