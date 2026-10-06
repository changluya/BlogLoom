package com.changlu.blogloom.module.seo;

import com.changlu.blogloom.config.properties.BlogProperties;
import com.changlu.blogloom.module.seo.dao.SeoArticleMapper;
import com.changlu.blogloom.module.seo.domain.SeoArticle;
import com.changlu.blogloom.module.seo.service.impl.RssServiceImpl;
import com.changlu.blogloom.module.seo.service.impl.SitemapServiceImpl;
import com.changlu.blogloom.module.seo.support.SeoUrlResolver;
import com.changlu.blogloom.service.SiteSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SeoFeedServiceTest {
	private SeoArticleMapper mapper;
	private SeoUrlResolver resolver;
	private BlogProperties properties;
	private SeoArticle article;

	@BeforeEach
	void setUp() {
		mapper = mock(SeoArticleMapper.class);
		SiteSettingService settings = mock(SiteSettingService.class);
		when(settings.getSeoDomain()).thenReturn("https://blog.example.com");
		properties = new BlogProperties();
		properties.setName("Test & Blog");
		properties.setView("http://localhost");
		resolver = new SeoUrlResolver(settings, properties);
		article = new SeoArticle();
		article.setId(1L);
		article.setTitle("A < B & C");
		article.setDescription("description & more");
		article.setCreateTime(new Date(1000));
		article.setUpdateTime(new Date(2000));
	}

	@Test
	void sitemapShouldContainCanonicalAndValidXml() throws Exception {
		when(mapper.listPublicArticles()).thenReturn(Collections.singletonList(article));
		String xml = new SitemapServiceImpl(mapper, resolver).generate();
		assertTrue(xml.contains("https://blog.example.com/blog/1"));
		assertEquals("urlset", parse(xml).getDocumentElement().getLocalName());
	}

	@Test
	void rssShouldEscapeContentAndBeValidXml() throws Exception {
		when(mapper.listRecentPublicArticles()).thenReturn(Collections.singletonList(article));
		String xml = new RssServiceImpl(mapper, resolver, properties).generate();
		assertTrue(xml.contains("A &lt; B &amp; C"));
		assertEquals("rss", parse(xml).getDocumentElement().getNodeName());
	}

	private org.w3c.dom.Document parse(String xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}
}
