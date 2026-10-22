package com.changlu.blogloom.service;

import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.exception.PersistenceException;
import com.changlu.blogloom.service.storage.UploadChannelManager;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 友链头像存储：按当前上传渠道写入 friend/{friendId} 目录。
 */
@Service
public class FriendAvatarStorageService {
	private static final long MAX_SIZE = 5L * 1024 * 1024;
	private static final Set<String> EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "webp"));

	private final UploadChannelManager uploadChannelManager;

	public FriendAvatarStorageService(UploadChannelManager uploadChannelManager) {
		this.uploadChannelManager = uploadChannelManager;
	}

	public String save(Long friendId, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("友链头像不能为空");
		}
		if (file.getSize() > MAX_SIZE) {
			throw new BadRequestException("友链头像不能超过 5MB");
		}
		String extension = extension(file.getOriginalFilename());
		if (!EXTENSIONS.contains(extension)) {
			throw new BadRequestException("仅支持 jpg、jpeg、png、gif、webp 图片");
		}
		try {
			String fileName = UUID.randomUUID().toString() + "." + extension;
			return uploadChannelManager.getCurrentChannel()
					.upload(file.getBytes(), extension, "friend/" + friendId, fileName).getUrl();
		} catch (IOException e) {
			throw new PersistenceException("读取友链头像失败", e);
		}
	}

	private String extension(String name) {
		if (name == null || name.lastIndexOf('.') < 0) {
			return "";
		}
		return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
	}
}
