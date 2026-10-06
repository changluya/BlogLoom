package com.changlu.blogloom.module.seo;

import com.changlu.blogloom.config.properties.BlogProperties;
import com.changlu.blogloom.module.seo.support.SeoUrlResolver;
import com.changlu.blogloom.service.SiteSettingService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SeoUrlResolverTest {
	@Test
	void shouldPreferConfiguredDomainAndTrimTrailingSlash() {
		SiteSettingService settings = mock(SiteSettingService.class);
		when(settings.getSeoDomain()).thenReturn("https://blog.example.com///");
		BlogProperties properties = new BlogProperties();
		properties.setView("http://localhost:8081");
		SeoUrlResolver resolver = new SeoUrlResolver(settings, properties);

		assertEquals("https://blog.example.com", resolver.baseUrl());
		assertEquals("https://blog.example.com/blog/8", resolver.articleUrl(8L));
		assertEquals("https://blog.example.com/static/a.png", resolver.absoluteUrl("/static/a.png"));
	}

	@Test
	void shouldKeepExternalAssetUrl() {
		SiteSettingService settings = mock(SiteSettingService.class);
		BlogProperties properties = new BlogProperties();
		properties.setView("https://blog.example.com/");
		SeoUrlResolver resolver = new SeoUrlResolver(settings, properties);
		assertEquals("https://cdn.example.com/a.png", resolver.absoluteUrl("https://cdn.example.com/a.png"));
	}
}
