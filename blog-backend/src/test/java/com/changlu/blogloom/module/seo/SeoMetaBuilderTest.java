package com.changlu.blogloom.module.seo;

import com.changlu.blogloom.config.properties.BlogProperties;
import com.changlu.blogloom.entity.Category;
import com.changlu.blogloom.entity.Tag;
import com.changlu.blogloom.model.vo.BlogDetail;
import com.changlu.blogloom.module.seo.domain.SeoMeta;
import com.changlu.blogloom.module.seo.support.SeoMetaBuilder;
import com.changlu.blogloom.module.seo.support.SeoUrlResolver;
import com.changlu.blogloom.service.SiteSettingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SeoMetaBuilderTest {
	@Test
	void shouldBuildCanonicalOpenGraphAndValidJsonLd() throws Exception {
		SiteSettingService settings = mock(SiteSettingService.class);
		when(settings.getSeoDomain()).thenReturn("https://blog.example.com/");
		when(settings.getWebTitleSuffix()).thenReturn(" - Test");
		BlogProperties properties = new BlogProperties();
		properties.setName("Test Blog");
		properties.setView("http://localhost");
		ObjectMapper mapper = new ObjectMapper();
		SeoMetaBuilder builder = new SeoMetaBuilder(new SeoUrlResolver(settings, properties), settings, properties, mapper);
		BlogDetail article = new BlogDetail();
		article.setId(12L);
		article.setTitle("SEO title");
		article.setContent("<p>fallback description</p>");
		article.setFirstPicture("/static/cover.png");
		article.setAuthorName("Author");
		article.setCreateTime(new Date(1000));
		article.setUpdateTime(new Date(2000));
		Category category = new Category(); category.setName("Java"); article.setCategory(category);
		Tag tag = new Tag(); tag.setName("SEO"); article.setTags(Arrays.asList(tag));

		SeoMeta meta = builder.forArticle(article);
		assertEquals("SEO title - Test", meta.getTitle());
		assertEquals("https://blog.example.com/blog/12", meta.getCanonicalUrl());
		assertEquals("https://blog.example.com/static/cover.png", meta.getImage());
		assertEquals("fallback description", meta.getDescription());
		JsonNode json = mapper.readTree(meta.getLdJson());
		assertEquals("BlogPosting", json.get("@type").asText());
		assertEquals(meta.getCanonicalUrl(), json.get("mainEntityOfPage").asText());
		assertTrue(json.get("keywords").isArray());
		assertFalse(meta.getPublishedTime().isEmpty());
	}
}
