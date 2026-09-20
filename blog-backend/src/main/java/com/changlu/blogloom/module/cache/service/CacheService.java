package com.changlu.blogloom.module.cache.service;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @Description: 统一缓存接口，便于后续在不同缓存实现间切换
 * @Author: changlu
 * @Date: 2026-09-20
 */
public interface CacheService {
	/**
	 * 从缓存中获取值
	 *
	 * @param key 缓存键
	 * @return 缓存值，不存在返回 null
	 */
	<T> T get(String key);

	/**
	 * 从缓存中获取值（类型安全）
	 *
	 * @param key  缓存键
	 * @param type 值类型
	 * @return 缓存值，不存在返回 null
	 */
	<T> T get(String key, Class<T> type);

	/**
	 * 从缓存中获取值，不存在则调用 valueLoader 加载（使用指定过期时间）
	 */
	<T> T get(String key, Callable<T> valueLoader, long duration, TimeUnit timeUnit);

	/**
	 * 从缓存中获取值，不存在则调用 valueLoader 加载（使用默认过期时间）
	 */
	<T> T get(String key, Callable<T> valueLoader);

	/**
	 * 将值放入缓存（指定过期时间）
	 */
	void put(String key, Object value, long duration, TimeUnit timeUnit);

	/**
	 * 将值放入缓存（默认过期时间）
	 */
	void put(String key, Object value);

	/**
	 * 使指定键的缓存失效
	 */
	void invalidate(String key);

	/**
	 * 使所有缓存失效
	 */
	void invalidateAll();

	/**
	 * 获取缓存统计信息
	 */
	String getStats();
}
