package com.changlu.blogloom.service.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.util.JacksonUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 图床连通性测试：按渠道标识将配置字符串转换为对应渠道参数，模拟上传一张探针图片后立即删除。
 */
@Service
public class ImageHostConnectivityService {
	@Autowired
	private LocalUploadChannel localUploadChannel;
	@Autowired
	private AliyunUploadChannel aliyunUploadChannel;
	@Autowired
	private ImageHostConfigService imageHostConfigService;

	/**
	 * @param channel 渠道标识
	 * @param value   渠道配置字符串，为空时使用已保存配置
	 */
	public Map<String, String> test(String channel, String value) {
		String name = channel == null || channel.trim().isEmpty()
				? UploadChannel.LOCAL : channel.trim().toLowerCase();
		Map<String, String> result = new LinkedHashMap<>();
		if (UploadChannel.ALIYUN.equals(name)) {
			AliyunOssConfig config = isBlank(value)
					? imageHostConfigService.getAliyunConfig()
					: JacksonUtils.readValue(value, AliyunOssConfig.class);
			aliyunUploadChannel.testConnection(config);
			result.put("channel", UploadChannel.ALIYUN);
			result.put("message", "阿里云 OSS 连通性正常");
			return result;
		}
		if (!UploadChannel.LOCAL.equals(name)) {
			throw new BadRequestException("不支持的上传渠道: " + name);
		}
		localUploadChannel.testConnection();
		result.put("channel", UploadChannel.LOCAL);
		result.put("message", "本地上传连通性正常");
		return result;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
