package com.changlu.blogloom.service.impl;

import com.changlu.blogloom.constant.CacheKeyConstants;
import com.changlu.blogloom.mapper.VisitorMapper;
import com.changlu.blogloom.module.cache.InMemoryCacheMapper;
import com.changlu.blogloom.service.BlogCacheService;
import com.changlu.blogloom.util.UserAgentUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class VisitorServiceImplTest {
	private VisitorServiceImpl service;
	private VisitorMapper visitorMapper;
	private MysqlBlogCacheServiceImpl cacheService;

	@BeforeEach
	void setUp() {
		visitorMapper = Mockito.mock(VisitorMapper.class);
		cacheService = new MysqlBlogCacheServiceImpl();
		ReflectionTestUtils.setField(cacheService, "cacheMapper", new InMemoryCacheMapper());

		service = new VisitorServiceImpl();
		ReflectionTestUtils.setField(service, "visitorMapper", visitorMapper);
		ReflectionTestUtils.setField(service, "cacheService", (BlogCacheService) cacheService);
		ReflectionTestUtils.setField(service, "userAgentUtils", Mockito.mock(UserAgentUtils.class));
	}

	@Test
	void shouldCheckUuidFromDatabase() {
		when(visitorMapper.hasUUID("u1")).thenReturn(1);
		assertTrue(service.hasUUID("u1"));
	}

	@Test
	void shouldRemoveUuidFromCacheWhenDeletingVisitor() {
		cacheService.saveValueToSet(CacheKeyConstants.IDENTIFICATION_SET, "u1");
		when(visitorMapper.deleteVisitorById(1L)).thenReturn(1);

		service.deleteVisitor(1L, "u1");

		assertFalse(cacheService.hasValueInSet(CacheKeyConstants.IDENTIFICATION_SET, "u1"));
	}
}
