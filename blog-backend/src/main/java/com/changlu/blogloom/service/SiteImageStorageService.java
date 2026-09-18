package com.changlu.blogloom.service;

import com.changlu.blogloom.config.properties.BlogProperties;
import com.changlu.blogloom.config.properties.UploadProperties;
import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.exception.PersistenceException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Service
public class SiteImageStorageService {
	private static final long MAX_SIZE = 5L * 1024 * 1024;
	private final UploadProperties uploadProperties;
	private final BlogProperties blogProperties;

	public SiteImageStorageService(UploadProperties uploadProperties, BlogProperties blogProperties) {
		this.uploadProperties = uploadProperties;
		this.blogProperties = blogProperties;
	}

	public String save(MultipartFile file) {
		if (file == null || file.isEmpty()) throw new BadRequestException("上传图片不能为空");
		if (file.getSize() > MAX_SIZE) throw new BadRequestException("图片不能超过 5MB");
		String extension = extension(file);
		Path directory = Paths.get(uploadProperties.getSitePath()).toAbsolutePath().normalize();
		String fileName = UUID.randomUUID().toString() + "." + extension;
		Path target = directory.resolve(fileName).normalize();
		if (!target.startsWith(directory)) throw new BadRequestException("图片路径不合法");
		try {
			Files.createDirectories(directory);
			file.transferTo(target.toFile());
		} catch (IOException e) {
			throw new PersistenceException("保存站点图片失败", e);
		}
		String api = blogProperties.getApi() == null ? "" : blogProperties.getApi();
		while (api.endsWith("/")) api = api.substring(0, api.length() - 1);
		return api + "/static/site/" + fileName;
	}

	private String extension(MultipartFile file) {
		String name = file.getOriginalFilename();
		String ext = "";
		if (name != null && name.lastIndexOf('.') >= 0) ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
		if (!("jpg".equals(ext) || "jpeg".equals(ext) || "png".equals(ext) || "gif".equals(ext) || "webp".equals(ext))) {
			throw new BadRequestException("仅支持 jpg、jpeg、png、gif、webp 图片");
		}
		return ext;
	}
}
