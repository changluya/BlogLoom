package com.changlu.blogloom.module.column.service;

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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class ColumnCoverStorageService {
	private static final long MAX_SIZE = 5L * 1024 * 1024;
	private static final Set<String> EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "webp"));
	private final UploadProperties uploadProperties;
	private final BlogProperties blogProperties;

	public ColumnCoverStorageService(UploadProperties uploadProperties, BlogProperties blogProperties) {
		this.uploadProperties = uploadProperties;
		this.blogProperties = blogProperties;
	}

	public String save(Long columnId, MultipartFile file) {
		if (file == null || file.isEmpty()) throw new BadRequestException("专栏图片不能为空");
		if (file.getSize() > MAX_SIZE) throw new BadRequestException("专栏图片不能超过 5MB");
		String extension = extension(file.getOriginalFilename());
		if (!EXTENSIONS.contains(extension)) throw new BadRequestException("仅支持 jpg、jpeg、png、webp 图片");
		String fileName = UUID.randomUUID().toString() + "." + extension;
		Path directory = Paths.get(uploadProperties.getBlogColumnPath(columnId)).toAbsolutePath().normalize();
		Path target = directory.resolve(fileName).normalize();
		if (!target.startsWith(directory)) throw new BadRequestException("专栏图片路径不合法");
		try {
			Files.createDirectories(directory);
			file.transferTo(target.toFile());
		} catch (IOException e) {
			throw new PersistenceException("保存专栏图片失败", e);
		}
		String api = blogProperties.getApi() == null ? "" : blogProperties.getApi().replaceAll("/+$", "");
		return api + "/static/blogColumn/" + columnId + "/" + fileName;
	}

	private String extension(String name) {
		if (name == null) return "";
		int dot = name.lastIndexOf('.');
		return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
	}
}
