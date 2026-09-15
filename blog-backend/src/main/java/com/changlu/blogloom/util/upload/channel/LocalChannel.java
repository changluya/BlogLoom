package com.changlu.blogloom.util.upload.channel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import com.changlu.blogloom.config.properties.BlogProperties;
import com.changlu.blogloom.config.properties.UploadProperties;
import com.changlu.blogloom.util.upload.UploadUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 本地存储方式
 *
 * @author: changlu
 * @date: 2026-09-13
 */
@Lazy
@Component
public class LocalChannel implements FileUploadChannel {
	@Autowired
	private BlogProperties blogProperties;
	@Autowired
	private UploadProperties uploadProperties;

	/**
	 * 将图片保存到本地，并返回访问本地图片的URL
	 *
	 * @param image 需要保存的图片
	 * @return 访问图片的URL
	 * @throws Exception
	 */
	@Override
	public String upload(UploadUtils.ImageResource image) throws Exception {
		Path folder = Paths.get(uploadProperties.getPath());
		Files.createDirectories(folder);
		String fileName = UUID.randomUUID() + "." + image.getType();
		Files.write(folder.resolve(fileName), image.getData());
		return blogProperties.getApi() + "/static/" + fileName;
	}
}
