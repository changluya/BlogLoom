package com.changlu.blogloom.util.upload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.changlu.blogloom.constant.UploadConstants;
import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.util.upload.channel.LocalChannel;

/**
 * @Description: 图片下载保存工具类
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Component
public class UploadUtils {
	private static RestTemplate restTemplate;

	private static LocalChannel uploadChannel;

	@Autowired
	public void setRestTemplate(RestTemplate restTemplate) {
		UploadUtils.restTemplate = restTemplate;
	}

	@Autowired
	public void setUploadChannel(LocalChannel uploadChannel) {
		UploadUtils.uploadChannel = uploadChannel;
	}

	@AllArgsConstructor
	@Getter
	public static class ImageResource {
		byte[] data;
		//图片拓展名 jpg png
		String type;
	}

	/**
	 * 保存图片到本地
	 *
	 * @param image 需要保存的图片
	 * @throws Exception
	 */
	public static String upload(ImageResource image) throws Exception {
		return uploadChannel.upload(image);
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
