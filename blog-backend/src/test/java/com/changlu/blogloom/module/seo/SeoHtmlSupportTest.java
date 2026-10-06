package com.changlu.blogloom.module.seo;

import com.changlu.blogloom.module.seo.support.SeoHtmlSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeoHtmlSupportTest {
	@Test
	void shouldDowngradeArticleH1AndLazyLoadImages() {
		String result = SeoHtmlSupport.optimizeArticleHtml("<h1>title</h1><p>x</p><img src=\"/a.png\">");
		assertTrue(result.contains("<h2>title</h2>"));
		assertTrue(result.contains("loading=\"lazy\" decoding=\"async\""));
	}

	@Test
	void shouldKeepExistingImageLoadingStrategy() {
		String image = "<img loading=\"eager\" src=\"/cover.png\">";
		assertEquals(image, SeoHtmlSupport.optimizeArticleHtml(image));
	}

	@Test
	void shouldRestoreDataSrcForServerRenderedImages() {
		String result = SeoHtmlSupport.optimizeArticleHtml("<img alt=\"cover\" data-src=\"https://cdn/a.png\">");
		assertTrue(result.contains("src=\"https://cdn/a.png\""));
		assertTrue(!result.contains("data-src="));
	}

	@Test
	void shouldExtractServerRenderedTableOfContents() {
		String html = "<h2 id=\"overview\">Overview <code>API</code></h2><h3 id='details'>Details</h3>";
		assertEquals(2, SeoHtmlSupport.extractToc(html).size());
		assertEquals("overview", SeoHtmlSupport.extractToc(html).get(0).getId());
		assertEquals("Overview API", SeoHtmlSupport.extractToc(html).get(0).getTitle());
		assertEquals(3, SeoHtmlSupport.extractToc(html).get(1).getLevel());
	}

	@Test
	void shouldRenderInlineTocPlaceholderLikeClientPage() {
		String html = "<p>[toc]</p><h2 id=\"one\">One</h2><h3 id=\"two\">Two</h3>";
		String result = SeoHtmlSupport.renderInlineToc(html);
		assertTrue(result.contains("article-inline-toc-title\">文章目录"));
		assertTrue(result.contains("href=\"#one\""));
		assertTrue(!result.contains("[toc]"));
	}
}
