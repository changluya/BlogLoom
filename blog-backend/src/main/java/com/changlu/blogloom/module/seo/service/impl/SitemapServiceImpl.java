package com.changlu.blogloom.module.seo.service.impl;

import com.changlu.blogloom.module.seo.dao.SeoArticleMapper;
import com.changlu.blogloom.module.seo.domain.SeoArticle;
import com.changlu.blogloom.module.seo.service.SitemapService;
import com.changlu.blogloom.module.seo.support.SeoUrlResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class SitemapServiceImpl implements SitemapService {
	private final SeoArticleMapper articleMapper;
	private final SeoUrlResolver urlResolver;

	public SitemapServiceImpl(@Qualifier("seoArticleMapper") SeoArticleMapper articleMapper, SeoUrlResolver urlResolver) {
		this.articleMapper = articleMapper;
		this.urlResolver = urlResolver;
	}

	@Override
	public String generate() {
		StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
				.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n")
				.append(url("/home", null)).append(url("/archives", null));
		for (SeoArticle article : articleMapper.listPublicArticles()) {
			xml.append("  <url>\n    <loc>").append(escape(urlResolver.articleUrl(article.getId())))
					.append("</loc>\n");
			if (article.getUpdateTime() != null) {
				xml.append("    <lastmod>").append(article.getUpdateTime().toInstant()
						.atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ISO_DATE))
						.append("</lastmod>\n");
			}
			xml.append("  </url>\n");
		}
		return xml.append("</urlset>").toString();
	}

	private String url(String path, String lastmod) {
		return "  <url>\n    <loc>" + escape(urlResolver.absoluteUrl(path)) + "</loc>\n" +
				(lastmod == null ? "" : "    <lastmod>" + lastmod + "</lastmod>\n") + "  </url>\n";
	}

	private String escape(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;").replace("'", "&apos;");
	}
}
