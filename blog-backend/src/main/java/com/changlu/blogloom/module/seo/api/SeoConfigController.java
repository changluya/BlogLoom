package com.changlu.blogloom.module.seo.api;

import com.changlu.blogloom.config.properties.BlogProperties;
import com.changlu.blogloom.model.vo.Result;
import com.changlu.blogloom.module.seo.domain.SeoConfig;
import com.changlu.blogloom.module.seo.service.SeoFileService;
import com.changlu.blogloom.service.SiteSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * @Description: SEO 配置（后台 SEO 配置页接口）
 * <p>SEO 域名取值优先级：后台配置 site_setting.seoDomain &gt; 配置参数 blog.view。</p>
 * @Author: changlu
 * @Date: 2026-10-06
 */
@RestController
@RequestMapping("/admin/seo")
public class SeoConfigController {

	@Autowired
	private SiteSettingService siteSettingService;
	@Autowired
	private BlogProperties blogProperties;
	@Autowired
	private SeoFileService seoFileService;

	/**
	 * 获取 SEO 配置：后台值优先，兜底配置参数 blog.view。
	 */
	@GetMapping("/config")
	public Result getConfig() {
		String dbValue = siteSettingService.getSeoDomain();
		String defaultDomain = trimEndSlash(blogProperties.getView());
		boolean configured = dbValue != null && !dbValue.trim().isEmpty();
		SeoConfig config = new SeoConfig();
		config.setSeoDomain(configured ? trimEndSlash(dbValue) : defaultDomain);
		config.setDefaultSeoDomain(defaultDomain);
		config.setConfigured(configured);
		return Result.ok("获取成功", config);
	}

	/**
	 * 保存 SEO 域名；留空表示清除后台配置，回退 blog.view。末尾斜杠会被自动去除。
	 */
	@PostMapping("/config")
	public Result updateConfig(@RequestBody SeoConfig config) {
		String domain = trimEndSlash(config.getSeoDomain());
		if (!domain.isEmpty() && !domain.matches("^https?://.+")) {
			return Result.error("SEO 域名需以 http:// 或 https:// 开头");
		}
		siteSettingService.saveSeoDomain(domain);
		return Result.ok("保存成功");
	}

	/** 去除首尾空白与末尾斜杠（如 https://blog.changlu.cloud/ → https://blog.changlu.cloud） */
	private String trimEndSlash(String value) {
		return value == null ? "" : value.trim().replaceAll("/+$", "");
	}

	/**
	 * 快捷查看：返回 robots / sitemap / rss 的文本内容。
	 */
	@GetMapping("/file/{name}/content")
	public Result getFileContent(@PathVariable String name) {
		String content = contentOf(name);
		if (content == null) {
			return Result.create(404, "不支持的文件：" + name);
		}
		return Result.ok("获取成功", content);
	}

	/**
	 * 快捷下载：robots / sitemap / rss 作为附件下载。
	 */
	@GetMapping("/file/{name}")
	public ResponseEntity<byte[]> downloadFile(@PathVariable String name) {
		String content = contentOf(name);
		if (content == null) {
			return ResponseEntity.notFound().build();
		}
		byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType(contentTypeOf(name) + ";charset=UTF-8"));
		headers.setContentDisposition(ContentDisposition.builder("attachment").filename(fileNameOf(name)).build());
		headers.setContentLength(bytes.length);
		return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
	}

	private String contentOf(String name) {
		switch (name) {
			case "robots":
				return seoFileService.robots();
			case "sitemap":
				return seoFileService.sitemap();
			case "rss":
				return seoFileService.rss();
			default:
				return null;
		}
	}

	private String fileNameOf(String name) {
		switch (name) {
			case "robots":
				return "robots.txt";
			case "sitemap":
				return "sitemap.xml";
			case "rss":
				return "rss.xml";
			default:
				return name;
		}
	}

	private String contentTypeOf(String name) {
		switch (name) {
			case "robots":
				return "text/plain";
			case "sitemap":
				return "application/xml";
			case "rss":
				return "application/rss+xml";
			default:
				return "application/octet-stream";
		}
	}
}
