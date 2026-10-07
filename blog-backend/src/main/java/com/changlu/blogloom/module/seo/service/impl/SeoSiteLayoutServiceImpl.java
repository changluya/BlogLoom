package com.changlu.blogloom.module.seo.service.impl;

import com.changlu.blogloom.model.vo.Badge;
import com.changlu.blogloom.model.vo.Introduction;
import com.changlu.blogloom.model.vo.NewBlog;
import com.changlu.blogloom.model.vo.RandomBlog;
import com.changlu.blogloom.module.seo.domain.SeoSiteLayout;
import com.changlu.blogloom.module.seo.service.SeoSiteLayoutService;
import com.changlu.blogloom.module.column.service.BlogColumnService;
import com.changlu.blogloom.service.BlogService;
import com.changlu.blogloom.service.CategoryService;
import com.changlu.blogloom.service.SiteSettingService;
import com.changlu.blogloom.service.TagService;
import com.changlu.blogloom.service.DashboardService;
import com.changlu.blogloom.util.JacksonUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class SeoSiteLayoutServiceImpl implements SeoSiteLayoutService {
	private final SiteSettingService siteSettingService;
	private final BlogService blogService;
	private final CategoryService categoryService;
	private final TagService tagService;
	private final DashboardService dashboardService;
	private final BlogColumnService columnService;

	public SeoSiteLayoutServiceImpl(SiteSettingService siteSettingService, BlogService blogService,
			CategoryService categoryService, TagService tagService, DashboardService dashboardService,
			BlogColumnService columnService) {
		this.siteSettingService = siteSettingService;
		this.blogService = blogService;
		this.categoryService = categoryService;
		this.tagService = tagService;
		this.dashboardService = dashboardService;
		this.columnService = columnService;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SeoSiteLayout getLayout() {
		SeoSiteLayout layout = new SeoSiteLayout();
		Map<String, Object> settings = siteSettingService.getSiteInfo();
		if (settings != null) {
			Object siteInfo = settings.get("siteInfo");
			if (siteInfo instanceof Map) layout.setSiteInfo((Map<String, Object>) siteInfo);
			Object introduction = settings.get("introduction");
			if (introduction instanceof Introduction) layout.setIntroduction((Introduction) introduction);
			Object badges = settings.get("badges");
			if (badges instanceof List) layout.setBadges((List<Badge>) badges);
		}
		layout.setCategories(orEmpty(categoryService.getCategoryNameList()));
		layout.setTags(orEmpty(tagService.getTagListNotId()));
		layout.setTotalViews(dashboardService.countVisitLog());
		layout.setPublishedBlogCount(blogService.countBlogByIsPublished());
		layout.setTotalBlogViews(blogService.sumViewsByIsPublished());
		layout.setColumns(orEmpty(columnService.getPublicTree()));

		List<RandomBlog> recommendations = normalize(
				blogService.getRandomBlogListByLimitNumAndIsPublishedAndIsRecommend(), RandomBlog.class);
		recommendations.removeIf(item -> Boolean.TRUE.equals(item.getPrivacy()) || StringUtils.hasText(item.getPassword()));
		layout.setRecommendations(recommendations);

		List<NewBlog> newBlogs = normalize(blogService.getNewBlogListByIsPublished(), NewBlog.class);
		newBlogs.removeIf(item -> Boolean.TRUE.equals(item.getPrivacy()) || StringUtils.hasText(item.getPassword()));
		layout.setNewBlogs(newBlogs);
		return layout;
	}

	private static <T> List<T> orEmpty(List<T> value) {
		return value == null ? Collections.emptyList() : value;
	}

	/**
	 * MySQL 缓存从 JSON 还原 List 时会丢失元素泛型，列表中实际可能是 LinkedHashMap。
	 * SEO 布局在过滤和渲染前统一还原为明确 VO，避免冷启动正常、缓存命中却报错。
	 */
	private static <T> List<T> normalize(List<?> values, Class<T> type) {
		if (values == null || values.isEmpty()) return new ArrayList<>();
		List<T> normalized = new ArrayList<>(values.size());
		for (Object value : values) {
			if (value == null) continue;
			normalized.add(type.isInstance(value) ? type.cast(value) : JacksonUtils.convertValue(value, type));
		}
		return normalized;
	}
}
