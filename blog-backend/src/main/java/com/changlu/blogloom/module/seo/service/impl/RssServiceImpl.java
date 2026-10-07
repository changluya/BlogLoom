package com.changlu.blogloom.module.seo.service.impl;

import com.changlu.blogloom.config.properties.BlogProperties;
import com.changlu.blogloom.module.seo.dao.SeoArticleMapper;
import com.changlu.blogloom.module.seo.domain.SeoArticle;
import com.changlu.blogloom.module.seo.service.RssService;
import com.changlu.blogloom.module.seo.support.SeoUrlResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
public class RssServiceImpl implements RssService {
	private static final DateTimeFormatter RFC_1123 = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
	private final SeoArticleMapper articleMapper;
	private final SeoUrlResolver urlResolver;
	private final BlogProperties blogProperties;

	public RssServiceImpl(@Qualifier("seoArticleMapper") SeoArticleMapper articleMapper,
	                      SeoUrlResolver urlResolver, BlogProperties blogProperties) {
		this.articleMapper = articleMapper;
		this.urlResolver = urlResolver;
		this.blogProperties = blogProperties;
	}

	@Override
	public String generate() {
		List<SeoArticle> articles = articleMapper.listRecentPublicArticles();
		String siteName = StringUtils.hasText(blogProperties.getName()) ? blogProperties.getName() : "BlogLoom";
		Date buildDate = articles.isEmpty() || articles.get(0).getUpdateTime() == null
				? new Date() : articles.get(0).getUpdateTime();
		StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
				.append("<rss version=\"2.0\"><channel>\n")
				.append("<title>").append(escape(siteName)).append("</title>\n")
				.append("<link>").append(escape(urlResolver.baseUrl())).append("</link>\n")
				.append("<description>").append(escape(siteName + " 最新文章")).append("</description>\n")
				.append("<language>zh-CN</language>\n<lastBuildDate>").append(date(buildDate)).append("</lastBuildDate>\n");
		for (SeoArticle article : articles) {
			String link = urlResolver.articleUrl(article.getId());
			xml.append("<item>\n<title>").append(escape(article.getTitle())).append("</title>\n")
					.append("<link>").append(escape(link)).append("</link>\n")
					.append("<guid isPermaLink=\"true\">").append(escape(link)).append("</guid>\n")
					.append("<pubDate>").append(date(article.getCreateTime())).append("</pubDate>\n")
					.append("<description>").append(escape(article.getDescription())).append("</description>\n</item>\n");
		}
		return xml.append("</channel></rss>").toString();
	}

	private String date(Date value) {
		return (value == null ? new Date(0) : value).toInstant().atZone(ZoneId.systemDefault()).format(RFC_1123);
	}

	private String escape(String value) {
		if (value == null) return "";
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;").replace("'", "&apos;");
	}
}
