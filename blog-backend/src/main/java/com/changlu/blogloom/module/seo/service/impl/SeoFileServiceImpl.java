package com.changlu.blogloom.module.seo.service.impl;

import com.changlu.blogloom.module.seo.service.RssService;
import com.changlu.blogloom.module.seo.service.SeoFileService;
import com.changlu.blogloom.module.seo.service.SitemapService;
import com.changlu.blogloom.module.seo.support.SeoUrlResolver;
import com.changlu.blogloom.module.seo.domain.AdsenseConfig;
import com.changlu.blogloom.service.SiteSettingService;
import org.springframework.stereotype.Service;

/**
 * @Description: 站点级 SEO 文件内容实现
 * @Author: changlu
 * @Date: 2026-10-06
 */
@Service
public class SeoFileServiceImpl implements SeoFileService {
	private final SeoUrlResolver urlResolver;
	private final SitemapService sitemapService;
	private final RssService rssService;
	private final SiteSettingService siteSettingService;

	public SeoFileServiceImpl(SeoUrlResolver urlResolver, SitemapService sitemapService, RssService rssService,
			SiteSettingService siteSettingService) {
		this.urlResolver = urlResolver;
		this.sitemapService = sitemapService;
		this.rssService = rssService;
		this.siteSettingService = siteSettingService;
	}

	@Override
	public String robots() {
		return "User-agent: *\nDisallow: /admin/\nDisallow: /cms/\nDisallow: /login\n" +
				"Allow: /\n\nSitemap: " + urlResolver.baseUrl() + "/sitemap.xml\n";
	}

	@Override
	public String sitemap() {
		return sitemapService.generate();
	}

	@Override
	public String rss() {
		return rssService.generate();
	}

	@Override
	public String adsTxt() {
		AdsenseConfig config = siteSettingService.getAdsenseConfig();
		if (Boolean.FALSE.equals(config.getEnabled())) return "";
		String custom = config.getAdsTxt() == null ? "" : config.getAdsTxt().trim();
		if (!custom.isEmpty()) return custom.endsWith("\n") ? custom : custom + "\n";
		String client = config.getClient() == null ? "" : config.getClient().trim();
		if (client.isEmpty()) return "";
		return "google.com, " + client.replaceFirst("^ca-", "") +
				", DIRECT, f08c47fec0942fa0\n";
	}
}
