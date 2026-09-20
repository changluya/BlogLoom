package com.changlu.blogloom.module.cache.domain.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * @Description: 缓存实体，对应 cache_entry 表
 * @Author: changlu
 * @Date: 2026-09-20
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
public class CacheEntry {
	private Long id;
	/**
	 * 缓存键
	 */
	private String cacheKey;
	/**
	 * 缓存值（JSON 序列化后字符串）
	 */
	private String cacheValue;
	/**
	 * 缓存类型
	 */
	private String cacheType;
	/**
	 * 过期时间，为空表示永不过期
	 */
	private LocalDateTime expireTime;
	private LocalDateTime createTime;
	private LocalDateTime modifyTime;

	public CacheEntry(String cacheKey, String cacheValue, String cacheType, LocalDateTime expireTime) {
		this.cacheKey = cacheKey;
		this.cacheValue = cacheValue;
		this.cacheType = cacheType;
		this.expireTime = expireTime;
	}
}
