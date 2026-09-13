package com.changlu.blogloom.util.upload.channel;

import com.changlu.blogloom.constant.UploadConstants;
import com.changlu.blogloom.util.common.SpringContextUtils;

/**
 * 文件上传方式
 *
 * @author: changlu
 * @date: 2026-09-13
 */
public class ChannelFactory {
	/**
	 * 创建文件上传方式
	 *
	 * @param channelName 方式名称
	 * @return 文件上传Channel
	 */
	public static FileUploadChannel getChannel(String channelName) {
		switch (channelName.toLowerCase()) {
			case UploadConstants.LOCAL:
				return SpringContextUtils.getBean(LocalChannel.class);
			case UploadConstants.GITHUB:
				return SpringContextUtils.getBean(GithubChannel.class);
			case UploadConstants.UPYUN:
				return SpringContextUtils.getBean(UpyunChannel.class);
		}
		throw new RuntimeException("Unsupported value in [application.properties]: [upload.channel]");
	}
}
