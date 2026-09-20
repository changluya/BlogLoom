package com.changlu.blogloom.module.cache.dao;

import com.changlu.blogloom.module.cache.domain.pojo.CacheEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Description: 缓存 Mapper
 * @Author: changlu
 * @Date: 2026-09-20
 */
@Mapper
@Repository
public interface CacheMapper {
	/**
	 * 根据键查询未过期缓存
	 */
	CacheEntry selectByKey(@Param("cacheKey") String cacheKey);

	/**
	 * 根据键前缀查询未过期缓存
	 */
	List<CacheEntry> selectByKeyPrefix(@Param("prefix") String prefix);

	/**
	 * 根据键前缀统计未过期缓存数量
	 */
	int countByKeyPrefix(@Param("prefix") String prefix);

	/**
	 * 根据键前缀删除缓存
	 */
	int deleteByKeyPrefix(@Param("prefix") String prefix);

	/**
	 * 原子递增指定键的数值（键不存在时由调用方补插）
	 */
	int incrementByKey(@Param("cacheKey") String cacheKey, @Param("increment") int increment);

	/**
	 * 更新指定键的过期时间
	 */
	int updateExpire(@Param("cacheKey") String cacheKey, @Param("expireTime") LocalDateTime expireTime);

	/**
	 * 插入缓存
	 */
	int insertCache(CacheEntry cacheEntry);

	/**
	 * 更新缓存
	 */
	int updateCache(CacheEntry cacheEntry);

	/**
	 * 删除缓存
	 */
	int deleteByKey(@Param("cacheKey") String cacheKey);

	/**
	 * 批量删除缓存
	 */
	int deleteByKeys(@Param("keys") List<String> keys);

	/**
	 * 清空所有缓存
	 */
	int deleteAll();

	/**
	 * 删除过期缓存
	 */
	int deleteExpired(@Param("currentTime") LocalDateTime currentTime);

	/**
	 * 查询所有未过期缓存键
	 */
	List<String> selectAllKeys();

	/**
	 * 统计未过期缓存数量
	 */
	Long countCache();
}
