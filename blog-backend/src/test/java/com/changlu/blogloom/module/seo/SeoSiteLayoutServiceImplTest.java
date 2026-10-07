package com.changlu.blogloom.module.seo;

import com.changlu.blogloom.model.vo.NewBlog;
import com.changlu.blogloom.module.seo.domain.SeoSiteLayout;
import com.changlu.blogloom.module.seo.service.impl.SeoSiteLayoutServiceImpl;
import com.changlu.blogloom.service.BlogService;
import com.changlu.blogloom.service.CategoryService;
import com.changlu.blogloom.service.SiteSettingService;
import com.changlu.blogloom.service.TagService;
import com.changlu.blogloom.service.DashboardService;
import com.changlu.blogloom.module.column.service.BlogColumnService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SeoSiteLayoutServiceImplTest {
	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	void shouldRestoreCachedMapsAndFilterPrivateArticles() {
		SiteSettingService siteSettingService = mock(SiteSettingService.class);
		BlogService blogService = mock(BlogService.class);
		CategoryService categoryService = mock(CategoryService.class);
		TagService tagService = mock(TagService.class);
		DashboardService dashboardService = mock(DashboardService.class);
		BlogColumnService columnService = mock(BlogColumnService.class);
		when(siteSettingService.getSiteInfo()).thenReturn(Collections.emptyMap());
		when(categoryService.getCategoryNameList()).thenReturn(Collections.emptyList());
		when(tagService.getTagListNotId()).thenReturn(Collections.emptyList());
		when(columnService.getPublicTree()).thenReturn(Collections.emptyList());
		when(blogService.getRandomBlogListByLimitNumAndIsPublishedAndIsRecommend()).thenReturn(Collections.emptyList());

		Map<String, Object> visible = new LinkedHashMap<>();
		visible.put("id", 16L);
		visible.put("title", "visible");
		visible.put("privacy", false);
		visible.put("password", "");
		Map<String, Object> hidden = new LinkedHashMap<>();
		hidden.put("id", 17L);
		hidden.put("title", "hidden");
		hidden.put("privacy", true);
		List cachedList = Arrays.asList(visible, hidden);
		when(blogService.getNewBlogListByIsPublished()).thenReturn((List<NewBlog>) cachedList);

		SeoSiteLayout result = new SeoSiteLayoutServiceImpl(
				siteSettingService, blogService, categoryService, tagService, dashboardService, columnService).getLayout();

		assertEquals(1, result.getNewBlogs().size());
		assertEquals(16L, result.getNewBlogs().get(0).getId());
		assertEquals("visible", result.getNewBlogs().get(0).getTitle());
	}
}
