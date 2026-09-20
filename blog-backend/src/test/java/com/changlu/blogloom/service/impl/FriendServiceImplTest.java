package com.changlu.blogloom.service.impl;

import com.changlu.blogloom.constant.CacheKeyConstants;
import com.changlu.blogloom.entity.SiteSetting;
import com.changlu.blogloom.mapper.FriendMapper;
import com.changlu.blogloom.mapper.SiteSettingMapper;
import com.changlu.blogloom.model.vo.FriendInfo;
import com.changlu.blogloom.module.cache.InMemoryCacheMapper;
import com.changlu.blogloom.service.BlogCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FriendServiceImplTest {
	private FriendServiceImpl service;
	private FriendMapper friendMapper;
	private SiteSettingMapper siteSettingMapper;
	private MysqlBlogCacheServiceImpl cacheService;

	@BeforeEach
	void setUp() {
		friendMapper = Mockito.mock(FriendMapper.class);
		siteSettingMapper = Mockito.mock(SiteSettingMapper.class);
		cacheService = new MysqlBlogCacheServiceImpl();
		ReflectionTestUtils.setField(cacheService, "cacheMapper", new InMemoryCacheMapper());

		service = new FriendServiceImpl();
		ReflectionTestUtils.setField(service, "friendMapper", friendMapper);
		ReflectionTestUtils.setField(service, "siteSettingMapper", siteSettingMapper);
		ReflectionTestUtils.setField(service, "cacheService", (BlogCacheService) cacheService);
	}

	@Test
	void shouldReturnCachedFriendInfoWithoutQueryingDatabase() {
		FriendInfo cached = new FriendInfo();
		cached.setCommentEnabled(true);
		cacheService.saveObjectToValue(CacheKeyConstants.FRIEND_INFO_MAP, cached);

		FriendInfo result = service.getFriendInfo(true, true);

		assertTrue(result.getCommentEnabled());
		verify(siteSettingMapper, never()).getFriendInfo();
	}

	@Test
	void shouldQueryAndCacheFriendInfoOnMiss() {
		SiteSetting content = new SiteSetting();
		content.setNameEn("friendContent");
		content.setValue("hello");
		SiteSetting commentEnabled = new SiteSetting();
		commentEnabled.setNameEn("friendCommentEnabled");
		commentEnabled.setValue("1");
		when(siteSettingMapper.getFriendInfo()).thenReturn(Arrays.asList(content, commentEnabled));

		FriendInfo result = service.getFriendInfo(true, true);

		assertTrue(result.getContent().contains("hello"));
		assertTrue(result.getCommentEnabled());
		assertNotNull(cacheService.getObjectByValue(CacheKeyConstants.FRIEND_INFO_MAP, FriendInfo.class));
	}

	@Test
	void shouldInvalidateFriendInfoCacheOnContentUpdate() {
		cacheService.saveObjectToValue(CacheKeyConstants.FRIEND_INFO_MAP, new FriendInfo());
		when(siteSettingMapper.updateFriendInfoContent("new")).thenReturn(1);

		service.updateFriendInfoContent("new");

		assertNull(cacheService.getObjectByValue(CacheKeyConstants.FRIEND_INFO_MAP, FriendInfo.class));
	}

	@Test
	void shouldInvalidateFriendInfoCacheOnCommentEnabledUpdate() {
		cacheService.saveObjectToValue(CacheKeyConstants.FRIEND_INFO_MAP, new FriendInfo());
		when(siteSettingMapper.updateFriendInfoCommentEnabled(true)).thenReturn(1);

		service.updateFriendInfoCommentEnabled(true);

		assertNull(cacheService.getObjectByValue(CacheKeyConstants.FRIEND_INFO_MAP, FriendInfo.class));
	}
}
