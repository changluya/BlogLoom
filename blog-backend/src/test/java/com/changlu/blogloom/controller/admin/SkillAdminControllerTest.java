package com.changlu.blogloom.controller.admin;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillAdminControllerTest {

	private final SkillAdminController controller = new SkillAdminController();

	@Test
	void shouldResolvePublicBaseUrlFromForwardedHeaders() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("blog-backend");
		request.setServerPort(8090);
		request.addHeader("Host", "blog-backend:8090");
		request.addHeader("X-Forwarded-Proto", "https");
		request.addHeader("X-Forwarded-Host", "blog.example.com");
		request.addHeader("X-Forwarded-Prefix", "/api/");

		assertEquals("https://blog.example.com/api", controller.resolveBaseUrl(request));
	}

	@Test
	void shouldFallBackToCurrentRequestHost() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("https");
		request.addHeader("Host", "cms.example.com:8443");

		assertEquals("https://cms.example.com:8443", controller.resolveBaseUrl(request));
	}
}
