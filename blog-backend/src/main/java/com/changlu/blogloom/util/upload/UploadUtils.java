package com.changlu.blogloom.util.upload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.changlu.blogloom.constant.UploadConstants;
import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.service.storage.UploadChannelManager;
import com.changlu.blogloom.service.storage.UploadedResource;

import java.util.UUID;

/**
 * @Description: 图片下载保存工具类
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Component
public class UploadUtils {
	private static RestTemplate restTemplate;

	private static UploadChannelManager uploadChannelManager;

	@Autowired
	public void setRestTemplate(RestTemplate restTemplate) {
		UploadUtils.restTemplate = restTemplate;
	}

	@Autowired
	public void setUploadChannelManager(UploadChannelManager uploadChannelManager) {
		UploadUtils.uploadChannelManager = uploadChannelManager;
	}

	@AllArgsConstructor
	@Getter
	public static class ImageResource {
		byte[] data;
		//图片拓展名 jpg png
		String type;
	}

	/**
	 * 按当前上传渠道保存图片
	 *
	 * @param image 需要保存的图片
	 * @throws Exception
	 */
	public static String upload(ImageResource image) throws Exception {
		UploadedResource resource = uploadChannelManager.getCurrentChannel()
				.upload(image.getData(), image.getType(), "", UUID.randomUUID() + "." + image.getType());
		return resource.getUrl();
	}

	/**
	 * 从网络获取图片数据
	 *
	 * @param url 图片URL
	 * @return
	 */
	public static ImageResource getImageByRequest(String url) {
		ResponseEntity<byte[]> responseEntity = restTemplate.getForEntity(url, byte[].class);
		if (UploadConstants.IMAGE.equals(responseEntity.getHeaders().getContentType().getType())) {
			return new ImageResource(responseEntity.getBody(), responseEntity.getHeaders().getContentType().getSubtype());
		}
		throw new BadRequestException("response contentType unlike image");
	}
}
