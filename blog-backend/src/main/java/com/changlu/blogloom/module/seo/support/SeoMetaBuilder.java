package com.changlu.blogloom.module.seo.support;

import com.changlu.blogloom.config.properties.BlogProperties;
import com.changlu.blogloom.entity.Tag;
import com.changlu.blogloom.model.vo.BlogDetail;
import com.changlu.blogloom.module.seo.domain.SeoMeta;
import com.changlu.blogloom.service.SiteSettingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SeoMetaBuilder {
	private static final DateTimeFormatter ISO_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
	private static final int DESCRIPTION_MAX_LENGTH = 160;

	private final SeoUrlResolver urlResolver;
	private final SiteSettingService siteSettingService;
	private final BlogProperties blogProperties;
	private final ObjectMapper objectMapper;

	public SeoMetaBuilder(SeoUrlResolver urlResolver, SiteSettingService siteSettingService,
	                      BlogProperties blogProperties, ObjectMapper objectMapper) {
		this.urlResolver = urlResolver;
		this.siteSettingService = siteSettingService;
		this.blogProperties = blogProperties;
		this.objectMapper = objectMapper;
	}

	public SeoMeta forArticle(BlogDetail article) {
		SeoMeta meta = baseMeta();
		meta.setType("article");
		meta.setTitle(withSuffix(article.getTitle()));
		meta.setDescription(description(article.getDescription(), article.getContent()));
		meta.setCanonicalUrl(urlResolver.articleUrl(article.getId()));
		meta.setImage(urlResolver.absoluteUrl(article.getFirstPicture()));
		meta.setAuthorName(StringUtils.hasText(article.getAuthorName()) ? article.getAuthorName() : meta.getSiteName());
		meta.setCategory(article.getCategory() == null ? "" : article.getCategory().getName());
		meta.setPublishedTime(toIso(article.getCreateTime()));
		meta.setModifiedTime(toIso(article.getUpdateTime()));
		List<Tag> tags = article.getTags() == null ? Collections.emptyList() : article.getTags();
		meta.setTags(tags.stream().filter(tag -> tag != null && StringUtils.hasText(tag.getName()))
				.map(Tag::getName).collect(Collectors.toList()));
		meta.setLdJson(articleJson(meta, article));
		return meta;
	}

	public SeoMeta forList(String title, String description, String path) {
		SeoMeta meta = baseMeta();
		meta.setTitle(withSuffix(title));
		meta.setDescription(description(description, ""));
		meta.setCanonicalUrl(urlResolver.absoluteUrl(path));
		ObjectNode json = objectMapper.createObjectNode();
		json.put("@context", "https://schema.org");
		json.put("@type", "CollectionPage");
		json.put("name", title);
		json.put("description", meta.getDescription());
		json.put("url", meta.getCanonicalUrl());
		meta.setLdJson(writeJson(json));
		return meta;
	}

	private SeoMeta baseMeta() {
		SeoMeta meta = new SeoMeta();
		meta.setBaseUrl(urlResolver.baseUrl());
		meta.setSiteName(StringUtils.hasText(blogProperties.getName()) ? blogProperties.getName() : "BlogLoom");
		Map<String, String> verifications = siteSettingService.getSeoVerifications();
		if (verifications != null) {
			meta.setBaiduVerification(verifications.get("baidu"));
			meta.setBingVerification(verifications.get("bing"));
			meta.setGoogleVerification(verifications.get("google"));
		}
		return meta;
	}

	private String articleJson(SeoMeta meta, BlogDetail article) {
		ObjectNode json = objectMapper.createObjectNode();
		json.put("@context", "https://schema.org");
		json.put("@type", "BlogPosting");
		json.put("headline", article.getTitle());
		json.put("description", meta.getDescription());
		if (StringUtils.hasText(meta.getImage())) json.put("image", meta.getImage());
		json.put("datePublished", meta.getPublishedTime());
		json.put("dateModified", meta.getModifiedTime());
		json.put("inLanguage", "zh-CN");
		json.put("url", meta.getCanonicalUrl());
		json.put("mainEntityOfPage", meta.getCanonicalUrl());
		ArrayNode keywords = json.putArray("keywords");
		meta.getTags().forEach(keywords::add);
		ObjectNode author = json.putObject("author");
		author.put("@type", "Person");
		author.put("name", meta.getAuthorName());
		ObjectNode publisher = json.putObject("publisher");
		publisher.put("@type", "Organization");
		publisher.put("name", meta.getSiteName());
		if (StringUtils.hasText(article.getAuthorAvatar())) {
			publisher.putObject("logo").put("@type", "ImageObject")
					.put("url", urlResolver.absoluteUrl(article.getAuthorAvatar()));
		}
		return writeJson(json);
	}

	private String withSuffix(String title) {
		String suffix = siteSettingService.getWebTitleSuffix();
		return StringUtils.hasText(suffix) ? title + suffix : title + " - " +
				(StringUtils.hasText(blogProperties.getName()) ? blogProperties.getName() : "BlogLoom");
	}

	private String description(String configured, String html) {
		String text = StringUtils.hasText(configured) ? configured : html;
		text = text == null ? "" : text.replaceAll("<[^>]+>", " ")
				.replaceAll("&[a-zA-Z#0-9]+;", " ").replaceAll("\\s+", " ").trim();
		if (!StringUtils.hasText(text)) text = "BlogLoom 博客内容";
		return text.length() > DESCRIPTION_MAX_LENGTH ? text.substring(0, DESCRIPTION_MAX_LENGTH) : text;
	}

	private String toIso(Date date) {
		Date value = date == null ? new Date(0) : date;
		return value.toInstant().atZone(ZoneId.systemDefault()).format(ISO_TIME);
	}

	private String writeJson(ObjectNode json) {
		try {
			return objectMapper.writeValueAsString(json).replace("</", "<\\/");
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("生成 SEO JSON-LD 失败", e);
		}
	}
}
