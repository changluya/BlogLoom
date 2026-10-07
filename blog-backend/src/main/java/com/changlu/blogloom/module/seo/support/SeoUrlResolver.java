package com.changlu.blogloom.module.seo.support;

import com.changlu.blogloom.config.properties.BlogProperties;
import com.changlu.blogloom.service.SiteSettingService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SeoUrlResolver {
	private final SiteSettingService siteSettingService;
	private final BlogProperties blogProperties;

	public SeoUrlResolver(SiteSettingService siteSettingService, BlogProperties blogProperties) {
		this.siteSettingService = siteSettingService;
		this.blogProperties = blogProperties;
	}

	public String baseUrl() {
		String configured = siteSettingService.getSeoDomain();
		String value = StringUtils.hasText(configured) ? configured : blogProperties.getView();
		if (!StringUtils.hasText(value)) {
			return "http://localhost:8081";
		}
		return value.trim().replaceAll("/+$", "");
	}

	public String absoluteUrl(String value) {
		if (!StringUtils.hasText(value)) {
			return "";
		}
		String trimmed = value.trim();
		if (trimmed.matches("(?i)^https?://.*")) {
			return trimmed;
		}
		return baseUrl() + (trimmed.startsWith("/") ? trimmed : "/" + trimmed);
	}

	public String articleUrl(Long id) {
		return baseUrl() + "/blog/" + id;
	}
}
