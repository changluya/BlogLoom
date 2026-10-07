package com.changlu.blogloom.module.seo.api;

import com.changlu.blogloom.env.SystemPropertyUtil;
import com.changlu.blogloom.module.seo.support.SeoVerificationSupport;
import com.changlu.blogloom.service.SiteSettingService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @Description: SPA 入口注入（方案 B）
 * <p>读取前台构建产物 conf/static/view/index.html，把各搜索引擎的站点验证 meta 注入 &lt;head&gt; 后返回。
 * 这样即使根路径由 SPA 承载，百度 / Bing / Google 也能在服务端返回的 HTML 中读到验证标签（不依赖 JS）。</p>
 * @Author: changlu
 * @Date: 2026-10-06
 */
@Controller
public class SpaIndexController {

	private final SiteSettingService siteSettingService;

	public SpaIndexController(SiteSettingService siteSettingService) {
		this.siteSettingService = siteSettingService;
	}

	@GetMapping(value = {"/", "/index.html"}, produces = "text/html;charset=UTF-8")
	@ResponseBody
	public ResponseEntity<String> index() {
		Path indexFile = SystemPropertyUtil.getStaticDir().resolve("view").resolve("index.html");
		if (!Files.isRegularFile(indexFile)) {
			return ResponseEntity.notFound().build();
		}
		try {
			String html = new String(Files.readAllBytes(indexFile), StandardCharsets.UTF_8);
			String tags = SeoVerificationSupport.buildMetaTags(siteSettingService.getSeoVerifications());
			html = SeoVerificationSupport.injectBeforeHeadEnd(html, tags);
			return ResponseEntity.ok()
					.contentType(MediaType.valueOf("text/html;charset=UTF-8"))
					.body(html);
		} catch (IOException e) {
			return ResponseEntity.status(500).body("读取前台入口文件失败：" + e.getMessage());
		}
	}
}
