package com.changlu.blogloom.service.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.changlu.blogloom.constant.SiteSettingConstants;
import com.changlu.blogloom.entity.SiteSetting;
import com.changlu.blogloom.mapper.SiteSettingMapper;
import com.changlu.blogloom.util.JacksonUtils;

/**
 * 从 site_setting 读取图床配置（上传渠道选择、阿里云参数）。
 */
@Service
public class ImageHostConfigService {
	@Autowired
	private SiteSettingMapper siteSettingMapper;

	/**
	 * 当前上传渠道标识，未配置时回退为本地。
	 */
	public String getCurrentChannelName() {
		SiteSetting setting = siteSettingMapper.getByNameEn(SiteSettingConstants.UPLOAD_CHANNEL_CHOOSE);
		if (setting == null || setting.getValue() == null || setting.getValue().trim().isEmpty()) {
			return UploadChannel.LOCAL;
		}
		return setting.getValue().trim().toLowerCase();
	}

	/**
	 * 本地渠道参数，未配置时返回 null。
	 */
	public LocalUploadConfig getLocalConfig() {
		SiteSetting setting = siteSettingMapper.getByNameEn(SiteSettingConstants.UPLOAD_CHANNEL_LOCAL);
		if (setting == null || setting.getValue() == null || setting.getValue().trim().isEmpty()) {
			return null;
		}
		return JacksonUtils.readValue(setting.getValue(), LocalUploadConfig.class);
	}

	/**
	 * 阿里云渠道参数，未配置时返回 null。
	 */
	public AliyunOssConfig getAliyunConfig() {
		SiteSetting setting = siteSettingMapper.getByNameEn(SiteSettingConstants.UPLOAD_CHANNEL_ALIYUN);
		if (setting == null || setting.getValue() == null || setting.getValue().trim().isEmpty()) {
			return null;
		}
		return JacksonUtils.readValue(setting.getValue(), AliyunOssConfig.class);
	}
}
