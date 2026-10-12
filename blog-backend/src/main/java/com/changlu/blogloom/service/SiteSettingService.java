package com.changlu.blogloom.service;

import com.changlu.blogloom.entity.SiteSetting;
import com.changlu.blogloom.module.seo.domain.AdsenseConfig;
import com.changlu.blogloom.module.seo.domain.AdSlot;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface SiteSettingService {
	Map<String, List<SiteSetting>> getList();

	Map<String, Object> getSiteInfo();

	String getWebTitleSuffix();

	/** 读取后台配置的 SEO 域名（未配置返回 null） */
	String getSeoDomain();

	/** 保存 SEO 域名配置（空值表示清除后台配置，回退 blog.view） */
	void saveSeoDomain(String value);

	/** 读取各搜索引擎站点验证 content（key：baidu / bing / google，未配置返回空串） */
	java.util.Map<String, String> getSeoVerifications();

	/** 保存各搜索引擎站点验证 content（key：baidu / bing / google） */
	void saveSeoVerifications(java.util.Map<String, String> values);

	/** 读取 AdSense JSON 配置；空值或损坏配置返回空对象 */
	AdsenseConfig getAdsenseConfig();

	/** 只返回总开关开启时的发布商 ID */
	String getAdsenseClient();

	/** 按 position 返回已启用的广告位 */
	Map<String, AdSlot> getAdsenseSlotsByPosition();

	/** 保存 AdSense 原始 JSON */
	void saveAdsenseConfig(String json);

	void updateSiteSetting(List<LinkedHashMap> siteSettings, List<Integer> deleteIds);
	Map<String, String> uploadImage(Integer id, MultipartFile file);
	Map<String, String> uploadImage(MultipartFile file);
}
