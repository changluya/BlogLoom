package com.changlu.blogloom.service.storage;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 上传渠道注册中心：按站点设置选择当前渠道实现。
 */
@Component
public class UploadChannelManager {
	private final Map<String, UploadChannel> channels = new LinkedHashMap<>();
	private final ImageHostConfigService imageHostConfigService;

	public UploadChannelManager(List<UploadChannel> channelList, ImageHostConfigService imageHostConfigService) {
		for (UploadChannel channel : channelList) {
			channels.put(channel.name().toLowerCase(), channel);
		}
		this.imageHostConfigService = imageHostConfigService;
	}

	/**
	 * 获取当前上传渠道，配置缺失或未知时回退本地。
	 */
	public UploadChannel getCurrentChannel() {
		String name = imageHostConfigService.getCurrentChannelName();
		UploadChannel channel = name == null ? null : channels.get(name.toLowerCase());
		if (channel == null) {
			channel = channels.get(UploadChannel.LOCAL);
		}
		if (channel == null) {
			throw new IllegalStateException("本地上传渠道未注册");
		}
		return channel;
	}
}
