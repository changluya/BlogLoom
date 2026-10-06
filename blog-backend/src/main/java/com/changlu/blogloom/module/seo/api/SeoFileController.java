package com.changlu.blogloom.module.seo.api;

import com.changlu.blogloom.module.seo.service.SeoFileService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeoFileController {
	private final SeoFileService seoFileService;

	public SeoFileController(SeoFileService seoFileService) {
		this.seoFileService = seoFileService;
	}

	@GetMapping(value = "/robots.txt", produces = "text/plain;charset=UTF-8")
	public String robots() {
		return seoFileService.robots();
	}

	@GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
	public String sitemap() {
		return seoFileService.sitemap();
	}

	@GetMapping(value = "/rss.xml", produces = "application/rss+xml;charset=UTF-8")
	public String rss() {
		return seoFileService.rss();
	}
}
