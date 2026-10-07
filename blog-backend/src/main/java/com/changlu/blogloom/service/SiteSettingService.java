package com.changlu.blogloom.service;

import com.changlu.blogloom.entity.SiteSetting;
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

	void updateSiteSetting(List<LinkedHashMap> siteSettings, List<Integer> deleteIds);
	Map<String, String> uploadImage(Integer id, MultipartFile file);
	Map<String, String> uploadImage(MultipartFile file);
}
