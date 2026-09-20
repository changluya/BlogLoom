package com.changlu.blogloom.service.impl;

import com.changlu.blogloom.model.vo.BlogInfo;
import com.changlu.blogloom.model.vo.PageResult;
import com.changlu.blogloom.module.cache.InMemoryCacheMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MysqlBlogCacheServiceImplTest {
	private MysqlBlogCacheServiceImpl cacheService;
	private InMemoryCacheMapper cacheMapper;

	@BeforeEach
	void setUp() {
		cacheService = new MysqlBlogCacheServiceImpl();
		cacheMapper = new InMemoryCacheMapper();
		ReflectionTestUtils.setField(cacheService, "cacheMapper", cacheMapper);
	}

	@Test
	void shouldSaveAndReadHashValue() {
		cacheService.saveKVToHash("siteInfoMap", "blogName", "BlogLoom");
		assertEquals("BlogLoom", cacheService.getValueByHashKey("siteInfoMap", "blogName"));
	}

	@Test
	void shouldReturnNullForMissingHashKey() {
		assertNull(cacheService.getValueByHashKey("siteInfoMap", "notExist"));
	}

	@Test
	void shouldSaveAndReadWholeHash() {
		cacheService.saveKVToHash("aboutInfoMap", "title", "标题");
		cacheService.saveKVToHash("aboutInfoMap", "content", "正文");
		Map map = cacheService.getMapByHash("aboutInfoMap");
		assertEquals(2, map.size());
		assertEquals("标题", map.get("title"));
		assertEquals("正文", map.get("content"));
	}

	@Test
	void shouldSaveMapToHashInBatch() {
		Map<String, Object> values = new LinkedHashMap<>();
		values.put("1", 100);
		values.put("2", 200);
		cacheService.saveMapToHash("blogViewsMap", values);
		assertEquals(100, cacheService.getValueByHashKey("blogViewsMap", 1));
		assertEquals(200, cacheService.getValueByHashKey("blogViewsMap", 2));
	}

	@Test
	void shouldIncrementHashKeyFromExistingValue() {
		cacheService.saveKVToHash("blogViewsMap", 1L, 5);
		cacheService.incrementByHashKey("blogViewsMap", 1L, 1);
		assertEquals(6, cacheService.getValueByHashKey("blogViewsMap", 1L));
	}

	@Test
	void shouldInsertInitialValueWhenIncrementingMissingHashKey() {
		cacheService.incrementByHashKey("blogViewsMap", 9L, 1);
		assertEquals(1, cacheService.getValueByHashKey("blogViewsMap", 9L));
	}

	@Test
	void shouldRejectNegativeIncrement() {
		assertThrows(RuntimeException.class, () -> cacheService.incrementByHashKey("blogViewsMap", 1L, -1));
		assertThrows(RuntimeException.class, () -> cacheService.incrementByKey("limitKey", -1));
	}

	@Test
	void shouldDeleteHashKey() {
		cacheService.saveKVToHash("blogViewsMap", 1L, 10);
		cacheService.deleteByHashKey("blogViewsMap", 1L);
		assertNull(cacheService.getValueByHashKey("blogViewsMap", 1L));
	}

	@Test
	void shouldSaveAndReadListValue() {
		List<String> tags = Arrays.asList("java", "spring");
		cacheService.saveListToValue("newBlogList", tags);
		List<String> loaded = cacheService.getListByValue("newBlogList");
		assertEquals(tags, loaded);
	}

	@Test
	void shouldReturnNullForMissingListValue() {
		assertNull(cacheService.getListByValue("notExistList"));
	}

	@Test
	void shouldSaveAndReadMapValue() {
		Map<String, Object> value = new HashMap<>();
		value.put("count", 3);
		value.put("blogMap", new LinkedHashMap<String, Object>());
		cacheService.saveMapToValue("archiveBlogMap", value);
		Map<String, Object> loaded = cacheService.getMapByValue("archiveBlogMap");
		assertNotNull(loaded);
		assertEquals(3, loaded.get("count"));
	}

	@Test
	void shouldSaveAndReadObjectValue() {
		BlogInfo blogInfo = new BlogInfo();
		blogInfo.setId(1L);
		blogInfo.setTitle("hello");
		cacheService.saveObjectToValue("friendInfoMap", blogInfo);
		BlogInfo loaded = cacheService.getObjectByValue("friendInfoMap", BlogInfo.class);
		assertNotNull(loaded);
		assertEquals(1L, loaded.getId());
		assertEquals("hello", loaded.getTitle());
	}

	@Test
	void shouldConvertHashValueToPageResult() {
		PageResult<BlogInfo> pageResult = new PageResult<>(1, Arrays.asList(new BlogInfo()));
		cacheService.saveKVToHash("homeBlogInfoList", 1, pageResult);
		PageResult<BlogInfo> loaded = cacheService.getBlogInfoPageResultByHash("homeBlogInfoList", 1);
		assertNotNull(loaded);
		assertEquals(1, loaded.getTotalPage());
		assertEquals(1, loaded.getList().size());
	}

	@Test
	void shouldReturnNullPageResultWhenHashMissing() {
		assertNull(cacheService.getBlogInfoPageResultByHash("homeBlogInfoList", 99));
	}

	@Test
	void shouldCountSetMembers() {
		cacheService.saveValueToSet("identificationSet", "uuid-1");
		cacheService.saveValueToSet("identificationSet", "uuid-2");
		assertEquals(2, cacheService.countBySet("identificationSet"));
	}

	@Test
	void shouldCheckSetMembership() {
		cacheService.saveValueToSet("identificationSet", "uuid-1");
		assertTrue(cacheService.hasValueInSet("identificationSet", "uuid-1"));
		assertFalse(cacheService.hasValueInSet("identificationSet", "uuid-2"));
	}

	@Test
	void shouldDeleteSetMember() {
		cacheService.saveValueToSet("identificationSet", "uuid-1");
		cacheService.deleteValueBySet("identificationSet", "uuid-1");
		assertFalse(cacheService.hasValueInSet("identificationSet", "uuid-1"));
		assertEquals(0, cacheService.countBySet("identificationSet"));
	}

	@Test
	void shouldCheckHasKeyForValueAndHashAndSet() {
		cacheService.saveObjectToValue("valueKey", "v");
		cacheService.saveKVToHash("hashKey", "field", "v");
		cacheService.saveValueToSet("setKey", "member");
		assertTrue(cacheService.hasKey("valueKey"));
		assertTrue(cacheService.hasKey("hashKey"));
		assertTrue(cacheService.hasKey("setKey"));
		assertFalse(cacheService.hasKey("missingKey"));
	}

	@Test
	void shouldDeleteCacheByKeyForValueAndHashAndSet() {
		cacheService.saveObjectToValue("valueKey", "v");
		cacheService.saveKVToHash("hashKey", "field", "v");
		cacheService.saveValueToSet("setKey", "member");
		cacheService.deleteCacheByKey("valueKey");
		cacheService.deleteCacheByKey("hashKey");
		cacheService.deleteCacheByKey("setKey");
		assertFalse(cacheService.hasKey("valueKey"));
		assertFalse(cacheService.hasKey("hashKey"));
		assertFalse(cacheService.hasKey("setKey"));
	}

	@Test
	void shouldIncrementValueKey() {
		cacheService.incrementByKey("accessLimitKey", 1);
		cacheService.incrementByKey("accessLimitKey", 1);
		assertEquals(2, ((Integer) cacheService.getObjectByValue("accessLimitKey", Integer.class)).intValue());
	}

	@Test
	void shouldExpireValueKey() {
		cacheService.incrementByKey("accessLimitKey", 1);
		cacheService.expire("accessLimitKey", 1);
		assertNotNull(cacheMapper.selectByKey("accessLimitKey"));
	}

	@Test
	void shouldNotOverwriteExpireTimeWithNullOnUpdate() {
		cacheService.incrementByKey("accessLimitKey", 1);
		cacheService.expire("accessLimitKey", 3600);
		// 再次自增不应清空已有的过期时间
		cacheService.incrementByKey("accessLimitKey", 1);
		assertEquals(2, ((Integer) cacheService.getObjectByValue("accessLimitKey", Integer.class)).intValue());
		assertNotNull(cacheMapper.selectByKey("accessLimitKey").getExpireTime());
	}
}
