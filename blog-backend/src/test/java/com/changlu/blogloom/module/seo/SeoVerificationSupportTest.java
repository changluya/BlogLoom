package com.changlu.blogloom.module.seo;

import com.changlu.blogloom.module.seo.support.SeoVerificationSupport;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeoVerificationSupportTest {

	@Test
	void shouldBuildOnlyNonEmptyMetaTags() {
		Map<String, String> codes = new LinkedHashMap<>();
		codes.put("baidu", "codeva-xxx");
		codes.put("bing", "");
		codes.put("google", null);

		String tags = SeoVerificationSupport.buildMetaTags(codes);

		assertTrue(tags.contains("baidu-site-verification"));
		assertTrue(tags.contains("codeva-xxx"));
		assertFalse(tags.contains("msvalidate.01"));
		assertFalse(tags.contains("google-site-verification"));
	}

	@Test
	void shouldEscapeContent() {
		Map<String, String> codes = new LinkedHashMap<>();
		codes.put("bing", "a\"b<c>d");

		String tags = SeoVerificationSupport.buildMetaTags(codes);

		assertTrue(tags.contains("content=\"a&quot;b&lt;c&gt;d\""));
	}

	@Test
	void shouldInjectBeforeHeadEnd() {
		String html = "<html><head><title>x</title></head><body></body></html>";
		String tags = "\t<meta name=\"baidu-site-verification\" content=\"codeva-xxx\">\n";

		String injected = SeoVerificationSupport.injectBeforeHeadEnd(html, tags);

		assertTrue(injected.contains(tags + "</head>"));
	}

	@Test
	void shouldKeepHtmlWhenNoHeadEnd() {
		String html = "<html><body>hi</body></html>";
		assertEquals(html, SeoVerificationSupport.injectBeforeHeadEnd(html, "<meta>\n"));
	}
}
