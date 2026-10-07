package com.changlu.blogloom.module.seo.api;

import com.changlu.blogloom.model.vo.Result;
import com.changlu.blogloom.module.seo.domain.SeoVerification;
import com.changlu.blogloom.service.SiteSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Description: SEO 平台关联（百度 / Bing / Google 站点验证）
 * @Author: changlu
 * @Date: 2026-10-06
 */
@RestController
@RequestMapping("/admin/seo")
public class SeoVerificationController {

	@Autowired
	private SiteSettingService siteSettingService;

	@GetMapping("/verification")
	public Result getVerification() {
		Map<String, String> values = siteSettingService.getSeoVerifications();
		SeoVerification verification = new SeoVerification();
		verification.setBaidu(values.get("baidu"));
		verification.setBing(values.get("bing"));
		verification.setGoogle(values.get("google"));
		return Result.ok("获取成功", verification);
	}

	@PostMapping("/verification")
	public Result updateVerification(@RequestBody SeoVerification verification) {
		Map<String, String> values = new LinkedHashMap<>(4);
		values.put("baidu", verification.getBaidu());
		values.put("bing", verification.getBing());
		values.put("google", verification.getGoogle());
		siteSettingService.saveSeoVerifications(values);
		return Result.ok("保存成功");
	}
}
