package com.changlu.blogloom.module.seo.api;

import com.changlu.blogloom.model.vo.Result;
import com.changlu.blogloom.module.seo.domain.AdSlot;
import com.changlu.blogloom.module.seo.domain.AdsenseConfig;
import com.changlu.blogloom.service.SiteSettingService;
import com.changlu.blogloom.util.JacksonUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/admin/seo/adsense")
public class AdsenseConfigController {
	/** 广告投放位置白名单（code → 中文名），同时作为后台下拉选项的数据源 */
	private static final Map<String, String> POSITION_LABELS = new LinkedHashMap<>();
	private static final Set<String> POSITIONS;
	private static final Set<String> FORMATS = new HashSet<>();
	private static final Set<String> ALIGNMENTS = new HashSet<>();
	static {
		POSITION_LABELS.put("articleTop", "文章详情·标题下方");
		POSITION_LABELS.put("articleInArticle", "文章详情·正文内嵌");
		POSITION_LABELS.put("articleBottom", "文章详情·正文结束后");
		POSITION_LABELS.put("articleSidebarToc", "文章详情·右侧目录下方");
		POSITION_LABELS.put("listTop", "列表页·顶部");
		POSITIONS = new HashSet<>(POSITION_LABELS.keySet());
		FORMATS.add("auto");
		FORMATS.add("fluid");
		FORMATS.add("autorelaxed");
		FORMATS.add("rectangle");
		FORMATS.add("horizontal");
		FORMATS.add("vertical");
		ALIGNMENTS.add("left");
		ALIGNMENTS.add("center");
		ALIGNMENTS.add("right");
	}

	private final SiteSettingService siteSettingService;

	public AdsenseConfigController(SiteSettingService siteSettingService) {
		this.siteSettingService = siteSettingService;
	}

	@GetMapping
	public Result getConfig() {
		return Result.ok("获取成功", siteSettingService.getAdsenseConfig());
	}

	@GetMapping("/positions")
	public Result positions() {
		List<Map<String, String>> list = new ArrayList<>();
		POSITION_LABELS.forEach((code, label) -> {
			Map<String, String> item = new LinkedHashMap<>();
			item.put("value", code);
			item.put("label", label);
			list.add(item);
		});
		return Result.ok("获取成功", list);
	}

	@PostMapping
	public Result updateConfig(@RequestBody AdsenseConfig config) {
		String error = validate(config);
		if (error != null) return Result.error(error);
		siteSettingService.saveAdsenseConfig(JacksonUtils.writeValueAsString(config));
		return Result.ok("保存成功");
	}

	private String validate(AdsenseConfig config) {
		if (config == null) return "广告配置不能为空";
		String client = trim(config.getClient());
		if (!client.isEmpty() && !client.matches("^ca-pub-\\d+$")) {
			return "client 格式应为 ca-pub-数字";
		}
		Set<String> positions = new HashSet<>();
		boolean hasEnabledSlot = false;
		if (config.getSlots() == null) {
			if (Boolean.TRUE.equals(config.getEnabled()) && Boolean.TRUE.equals(config.getAutoAds()) && client.isEmpty())
				return "启用自动广告前请填写发布商 ID";
			return null;
		}
		for (AdSlot item : config.getSlots()) {
			if (item == null) return "slots 中不能包含空项";
			String position = trim(item.getPosition());
			String slot = trim(item.getSlot());
			if (!POSITIONS.contains(position)) return "不支持的广告位置：" + position;
			if (!positions.add(position)) return "广告位置不能重复：" + position;
			if (slot.isEmpty() && (Boolean.FALSE.equals(config.getEnabled()) || Boolean.FALSE.equals(item.getEnabled()))) continue;
			if (!slot.matches("^\\d+$")) return "slot 必须为数字：" + position;
			String format = trim(item.getFormat());
			if (!format.isEmpty() && !FORMATS.contains(format)) return "不支持的广告格式：" + format;
			String alignment = trim(item.getAlignment());
			if (!alignment.isEmpty() && !ALIGNMENTS.contains(alignment)) return "不支持的对齐方式：" + alignment;
			if (!Boolean.FALSE.equals(item.getEnabled())) hasEnabledSlot = true;
		}
		if (Boolean.TRUE.equals(config.getEnabled()) && (Boolean.TRUE.equals(config.getAutoAds()) || hasEnabledSlot)
				&& client.isEmpty()) return "启用广告前请填写发布商 ID";
		return null;
	}

	private String trim(String value) {
		return value == null ? "" : value.trim();
	}
}
