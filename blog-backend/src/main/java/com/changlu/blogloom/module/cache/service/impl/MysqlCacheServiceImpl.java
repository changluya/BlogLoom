package com.changlu.blogloom.module.cache.service.impl;

import com.changlu.blogloom.module.cache.constant.CacheConstant;
import com.changlu.blogloom.module.cache.dao.CacheMapper;
import com.changlu.blogloom.module.cache.domain.pojo.CacheEntry;
import com.changlu.blogloom.module.cache.service.CacheService;
import com.changlu.blogloom.util.JacksonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @Description: 基于 MySQL 的缓存实现，用于替代 Redis 组件
 * @Author: changlu
 * @Date: 2026-09-20
 */
@Slf4j
@Service("mysqlCacheService")
public class MysqlCacheServiceImpl implements CacheService {
	@Autowired
	private CacheMapper cacheMapper;

	// 默认缓存时间(5分钟)
	private final long defaultExpireTime = 5;
	// 默认缓存时间单位(分钟)
	private final TimeUnit defaultExpireUnit = TimeUnit.MINUTES;

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		return (T) get(key, Object.class);
	}

	@Override
	public <T> T get(String key, Class<T> type) {
		try {
			CacheEntry cacheEntry = cacheMapper.selectByKey(key);
			if (cacheEntry == null) {
				return null;
			}
			String cacheValue = cacheEntry.getCacheValue();
			if (cacheValue == null) {
				return null;
			}
			return JacksonUtils.readValue(cacheValue, type);
		} catch (Exception e) {
			log.error("从 MySQL 缓存读取失败，key: {}", key, e);
			return null;
		}
	}

	@Override
	public <T> T get(String key, Callable<T> valueLoader) {
		return get(key, valueLoader, defaultExpireTime, defaultExpireUnit);
	}

	@Override
	public <T> T get(String key, Callable<T> valueLoader, long duration, TimeUnit timeUnit) {
		T value = get(key);
		if (value != null) {
			return value;
		}
		try {
			T loadedValue = valueLoader.call();
			if (loadedValue != null) {
				put(key, loadedValue, duration, timeUnit);
			}
			return loadedValue;
		} catch (Exception e) {
			log.error("加载缓存值失败，key: {}", key, e);
			return null;
		}
	}

	@Override
	public void put(String key, Object value) {
		put(key, value, defaultExpireTime, defaultExpireUnit);
	}

	@Override
	public void put(String key, Object value, long duration, TimeUnit timeUnit) {
		try {
			String jsonValue = JacksonUtils.writeValueAsString(value);
			LocalDateTime expireTime = calculateExpireTime(duration, timeUnit);
			CacheEntry cacheEntry = new CacheEntry(key, jsonValue, CacheConstant.DEFAULT_CACHE_TYPE, expireTime);
			int updated = cacheMapper.updateCache(cacheEntry);
			if (updated == 0) {
				cacheMapper.insertCache(cacheEntry);
			}
			log.debug("写入 MySQL 缓存，key: {}, expireTime: {}", key, expireTime);
		} catch (Exception e) {
			log.error("写入 MySQL 缓存失败，key: {}", key, e);
		}
	}

	@Override
	public void invalidate(String key) {
		try {
			cacheMapper.deleteByKey(key);
			log.debug("失效 MySQL 缓存，key: {}", key);
		} catch (Exception e) {
			log.error("失效 MySQL 缓存失败，key: {}", key, e);
		}
	}

	@Override
	public void invalidateAll() {
		try {
			cacheMapper.deleteAll();
			log.debug("清空所有 MySQL 缓存");
		} catch (Exception e) {
			log.error("清空所有 MySQL 缓存失败", e);
		}
	}

	@Override
	public String getStats() {
		try {
			Long count = cacheMapper.countCache();
			List<String> keys = cacheMapper.selectAllKeys();
			return String.format("MySQL Cache Stats - Total entries: %d, Keys: %s", count, keys);
		} catch (Exception e) {
			log.error("获取 MySQL 缓存统计失败", e);
			return "MySQL Cache Stats - Error occurred";
		}
	}

	/**
	 * 清理过期缓存（可定时调用）
	 */
	public void cleanExpired() {
		try {
			int deletedCount = cacheMapper.deleteExpired(LocalDateTime.now());
			log.debug("清理 {} 条过期缓存", deletedCount);
		} catch (Exception e) {
			log.error("清理过期缓存失败", e);
		}
	}

	/**
	 * 计算过期时间，duration <= 0 表示永不过期
	 */
	private LocalDateTime calculateExpireTime(long duration, TimeUnit timeUnit) {
		if (duration <= 0) {
			return null;
		}
		long seconds = timeUnit.toSeconds(duration);
		return LocalDateTime.now().plusSeconds(seconds);
	}
}
