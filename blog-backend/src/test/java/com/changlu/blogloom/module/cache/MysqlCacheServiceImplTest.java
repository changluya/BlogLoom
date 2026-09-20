package com.changlu.blogloom.module.cache;

import com.changlu.blogloom.module.cache.domain.pojo.CacheEntry;
import com.changlu.blogloom.module.cache.service.impl.MysqlCacheServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MysqlCacheServiceImplTest {
	private MysqlCacheServiceImpl cacheService;
	private InMemoryCacheMapper cacheMapper;

	@BeforeEach
	void setUp() {
		cacheService = new MysqlCacheServiceImpl();
		cacheMapper = new InMemoryCacheMapper();
		ReflectionTestUtils.setField(cacheService, "cacheMapper", cacheMapper);
	}

	@Test
	void shouldPutAndGetTypedValue() {
		cacheService.put("stringKey", "hello", 5, TimeUnit.MINUTES);
		assertEquals("hello", cacheService.get("stringKey", String.class));
	}

	@Test
	void shouldGetValueWithObjectType() {
		cacheService.put("numberKey", 42, 5, TimeUnit.MINUTES);
		Object value = cacheService.get("numberKey");
		assertEquals(42, ((Number) value).intValue());
	}

	@Test
	void shouldReturnNullWhenKeyMissing() {
		assertNull(cacheService.get("missingKey", String.class));
		assertNull(cacheService.get("missingKey"));
	}

	@Test
	void shouldUpdateExistingKeyInsteadOfDuplicating() {
		cacheService.put("key", "first", 5, TimeUnit.MINUTES);
		cacheService.put("key", "second", 5, TimeUnit.MINUTES);
		assertEquals("second", cacheService.get("key", String.class));
		assertEquals(1L, cacheMapper.countCache());
	}

	@Test
	void shouldReturnNullForExpiredEntry() {
		CacheEntry expired = new CacheEntry("expiredKey", "\"v\"", "DEFAULT", LocalDateTime.now().minusSeconds(5));
		cacheMapper.insertCache(expired);
		assertNull(cacheService.get("expiredKey", String.class));
	}

	@Test
	void shouldLoadValueWhenCacheMiss() {
		AtomicInteger loadCount = new AtomicInteger();
		String value = cacheService.get("loadKey", () -> {
			loadCount.incrementAndGet();
			return "loaded";
		}, 5, TimeUnit.MINUTES);
		assertEquals("loaded", value);
		assertEquals(1, loadCount.get());
		// 第二次命中缓存，不再触发加载
		String cached = cacheService.get("loadKey", () -> {
			loadCount.incrementAndGet();
			return "loaded-again";
		}, 5, TimeUnit.MINUTES);
		assertEquals("loaded", cached);
		assertEquals(1, loadCount.get());
	}

	@Test
	void shouldUseDefaultExpireTimeWhenLoaderWithoutDuration() {
		String value = cacheService.get("defaultLoadKey", () -> "value");
		assertEquals("value", value);
		assertNotNull(cacheMapper.selectByKey("defaultLoadKey"));
	}

	@Test
	void shouldReturnNullWhenLoaderThrows() {
		String value = cacheService.get("errorKey", () -> {
			throw new IllegalStateException("boom");
		}, 5, TimeUnit.MINUTES);
		assertNull(value);
	}

	@Test
	void shouldInvalidateSingleKey() {
		cacheService.put("key", "v", 5, TimeUnit.MINUTES);
		cacheService.invalidate("key");
		assertNull(cacheService.get("key", String.class));
	}

	@Test
	void shouldInvalidateAllKeys() {
		cacheService.put("key1", "v1", 5, TimeUnit.MINUTES);
		cacheService.put("key2", "v2", 5, TimeUnit.MINUTES);
		cacheService.invalidateAll();
		assertEquals(0L, cacheMapper.countCache());
	}

	@Test
	void shouldReportStats() {
		cacheService.put("key", "v", 5, TimeUnit.MINUTES);
		String stats = cacheService.getStats();
		assertTrue(stats.contains("Total entries: 1"));
	}

	@Test
	void shouldCleanExpiredEntries() {
		cacheMapper.insertCache(new CacheEntry("expired", "\"v\"", "DEFAULT", LocalDateTime.now().minusSeconds(5)));
		cacheService.put("alive", "v", 5, TimeUnit.MINUTES);
		cacheService.cleanExpired();
		assertEquals(1L, cacheMapper.countCache());
		assertNotNull(cacheMapper.selectByKey("alive"));
	}
}
