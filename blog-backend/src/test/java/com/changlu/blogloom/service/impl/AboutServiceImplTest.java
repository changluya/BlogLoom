package com.changlu.blogloom.service.impl;

import com.changlu.blogloom.constant.CacheKeyConstants;
import com.changlu.blogloom.entity.About;
import com.changlu.blogloom.mapper.AboutMapper;
import com.changlu.blogloom.module.cache.InMemoryCacheMapper;
import com.changlu.blogloom.service.BlogCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AboutServiceImplTest {
	private AboutServiceImpl service;
	private AboutMapper aboutMapper;
	private MysqlBlogCacheServiceImpl cacheService;

	@BeforeEach
	void setUp() {
		aboutMapper = Mockito.mock(AboutMapper.class);
		cacheService = new MysqlBlogCacheServiceImpl();
		ReflectionTestUtils.setField(cacheService, "cacheMapper", new InMemoryCacheMapper());

		service = new AboutServiceImpl();
		ReflectionTestUtils.setField(service, "aboutMapper", aboutMapper);
		ReflectionTestUtils.setField(service, "cacheService", (BlogCacheService) cacheService);
	}

	@Test
	void shouldReturnCachedAboutInfoWithoutQueryingDatabase() {
		Map<String, String> cached = new HashMap<>();
		cached.put("title", "cached");
		cacheService.saveMapToValue(CacheKeyConstants.ABOUT_INFO_MAP, cached);

		Map<String, String> result = service.getAboutInfo();

		assertEquals("cached", result.get("title"));
		verify(aboutMapper, never()).getList();
	}

	@Test
	void shouldQueryAndCacheAboutInfoOnMiss() {
		About about = new About();
		about.setNameEn("title");
		about.setValue("hello");
		when(aboutMapper.getList()).thenReturn(Arrays.asList(about));

		Map<String, String> result = service.getAboutInfo();

		assertEquals("hello", result.get("title"));
		assertNotNull(cacheService.getMapByValue(CacheKeyConstants.ABOUT_INFO_MAP));
	}

	@Test
	void shouldInvalidateAboutCacheOnUpdate() {
		cacheService.saveMapToValue(CacheKeyConstants.ABOUT_INFO_MAP, new HashMap<>());
		when(aboutMapper.updateAbout("title", "new")).thenReturn(1);

		service.updateAbout(Collections.singletonMap("title", "new"));

		assertNull(cacheService.getMapByValue(CacheKeyConstants.ABOUT_INFO_MAP));
	}
}
