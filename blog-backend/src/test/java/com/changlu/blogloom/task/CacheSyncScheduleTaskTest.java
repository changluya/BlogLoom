package com.changlu.blogloom.task;

import com.changlu.blogloom.constant.CacheKeyConstants;
import com.changlu.blogloom.service.BlogCacheService;
import com.changlu.blogloom.service.BlogService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CacheSyncScheduleTaskTest {

	@Test
	void shouldSyncEveryBlogViewsToDatabase() {
		BlogCacheService cacheService = Mockito.mock(BlogCacheService.class);
		BlogService blogService = Mockito.mock(BlogService.class);
		CacheSyncScheduleTask task = new CacheSyncScheduleTask();
		org.springframework.test.util.ReflectionTestUtils.setField(task, "cacheService", cacheService);
		org.springframework.test.util.ReflectionTestUtils.setField(task, "blogService", blogService);

		Map<String, Object> views = new LinkedHashMap<>();
		views.put("1", 100);
		views.put("2", 200);
		when(cacheService.getMapByHash(CacheKeyConstants.BLOG_VIEWS_MAP)).thenReturn(views);

		task.syncBlogViewsToDatabase();

		verify(blogService).updateViews(1L, 100);
		verify(blogService).updateViews(2L, 200);
	}

	@Test
	void shouldIgnoreNullViews() {
		BlogCacheService cacheService = Mockito.mock(BlogCacheService.class);
		BlogService blogService = Mockito.mock(BlogService.class);
		CacheSyncScheduleTask task = new CacheSyncScheduleTask();
		org.springframework.test.util.ReflectionTestUtils.setField(task, "cacheService", cacheService);
		org.springframework.test.util.ReflectionTestUtils.setField(task, "blogService", blogService);

		Map<String, Object> views = new LinkedHashMap<>();
		views.put("1", null);
		when(cacheService.getMapByHash(CacheKeyConstants.BLOG_VIEWS_MAP)).thenReturn(views);

		task.syncBlogViewsToDatabase();

		verify(blogService, Mockito.never()).updateViews(Mockito.anyLong(), Mockito.anyInt());
	}
}
