package com.changlu.blogloom.service;

import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.exception.PersistenceException;
import com.changlu.blogloom.service.storage.UploadChannelManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

/**
 * 站点设置图片存储：按当前上传渠道写入 site 目录。
 */
@Service
public class SiteImageStorageService {
	private static final long MAX_SIZE = 5L * 1024 * 1024;

	@Autowired
	private UploadChannelManager uploadChannelManager;

	public String save(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("上传图片不能为空");
		}
		if (file.getSize() > MAX_SIZE) {
			throw new BadRequestException("图片不能超过 5MB");
		}
		String extension = extension(file);
		byte[] content;
		try {
			content = file.getBytes();
		} catch (IOException e) {
			throw new PersistenceException("读取上传图片失败", e);
		}
		String fileName = UUID.randomUUID().toString() + "." + extension;
		return uploadChannelManager.getCurrentChannel()
				.upload(content, extension, "site", fileName).getUrl();
	}

	private String extension(MultipartFile file) {
		String name = file.getOriginalFilename();
		String ext = "";
		if (name != null && name.lastIndexOf('.') >= 0) {
			ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
		}
		if (!("jpg".equals(ext) || "jpeg".equals(ext) || "png".equals(ext) || "gif".equals(ext) || "webp".equals(ext))) {
			throw new BadRequestException("仅支持 jpg、jpeg、png、gif、webp 图片");
		}
		return ext;
	}
}
