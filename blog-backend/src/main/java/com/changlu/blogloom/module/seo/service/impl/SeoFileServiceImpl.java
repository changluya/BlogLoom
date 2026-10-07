package com.changlu.blogloom.module.seo.service.impl;

import com.changlu.blogloom.module.seo.service.RssService;
import com.changlu.blogloom.module.seo.service.SeoFileService;
import com.changlu.blogloom.module.seo.service.SitemapService;
import com.changlu.blogloom.module.seo.support.SeoUrlResolver;
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

	public SeoFileServiceImpl(SeoUrlResolver urlResolver, SitemapService sitemapService, RssService rssService) {
		this.urlResolver = urlResolver;
		this.sitemapService = sitemapService;
		this.rssService = rssService;
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
}
