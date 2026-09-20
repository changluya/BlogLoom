package com.changlu.blogloom.module.cache;

import com.changlu.blogloom.module.cache.dao.CacheMapper;
import com.changlu.blogloom.module.cache.domain.pojo.CacheEntry;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存版 CacheMapper，用于在无数据库环境下验证缓存语义。
 * 行为与 CacheMapper.xml 保持一致：仅更新非空字段、过期行不参与查询。
 */
public class InMemoryCacheMapper implements CacheMapper {
	private final Map<String, CacheEntry> store = new LinkedHashMap<>();
	private final AtomicLong idGenerator = new AtomicLong();

	@Override
	public CacheEntry selectByKey(String cacheKey) {
		CacheEntry entry = store.get(cacheKey);
		if (entry == null || isExpired(entry)) {
			return null;
		}
		return copy(entry);
	}

	@Override
	public List<CacheEntry> selectByKeyPrefix(String prefix) {
		List<CacheEntry> result = new ArrayList<>();
		for (CacheEntry entry : store.values()) {
			if (entry.getCacheKey().startsWith(prefix) && !isExpired(entry)) {
				result.add(copy(entry));
			}
		}
		return result;
	}

	@Override
	public int countByKeyPrefix(String prefix) {
		return selectByKeyPrefix(prefix).size();
	}

	@Override
	public int deleteByKeyPrefix(String prefix) {
		List<String> keys = new ArrayList<>();
		for (String key : store.keySet()) {
			if (key.startsWith(prefix)) {
				keys.add(key);
			}
		}
		keys.forEach(store::remove);
		return keys.size();
	}

	@Override
	public int incrementByKey(String cacheKey, int increment) {
		CacheEntry entry = store.get(cacheKey);
		if (entry == null || isExpired(entry)) {
			return 0;
		}
		int current = Integer.parseInt(entry.getCacheValue());
		entry.setCacheValue(String.valueOf(current + increment));
		return 1;
	}

	@Override
	public int updateExpire(String cacheKey, LocalDateTime expireTime) {
		CacheEntry entry = store.get(cacheKey);
		if (entry == null) {
			return 0;
		}
		entry.setExpireTime(expireTime);
		return 1;
	}

	@Override
	public int insertCache(CacheEntry cacheEntry) {
		if (store.containsKey(cacheEntry.getCacheKey())) {
			throw new DuplicateKeyException("duplicate cache key: " + cacheEntry.getCacheKey());
		}
		CacheEntry saved = copy(cacheEntry);
		saved.setId(idGenerator.incrementAndGet());
		saved.setCreateTime(LocalDateTime.now());
		saved.setModifyTime(LocalDateTime.now());
		store.put(saved.getCacheKey(), saved);
		return 1;
	}

	@Override
	public int updateCache(CacheEntry cacheEntry) {
		CacheEntry existing = store.get(cacheEntry.getCacheKey());
		if (existing == null) {
			return 0;
		}
		if (cacheEntry.getCacheValue() != null) {
			existing.setCacheValue(cacheEntry.getCacheValue());
		}
		if (cacheEntry.getCacheType() != null && !cacheEntry.getCacheType().isEmpty()) {
			existing.setCacheType(cacheEntry.getCacheType());
		}
		if (cacheEntry.getExpireTime() != null) {
			existing.setExpireTime(cacheEntry.getExpireTime());
		}
		existing.setModifyTime(LocalDateTime.now());
		return 1;
	}

	@Override
	public int deleteByKey(String cacheKey) {
		return store.remove(cacheKey) != null ? 1 : 0;
	}

	@Override
	public int deleteByKeys(List<String> keys) {
		int count = 0;
		for (String key : keys) {
			count += deleteByKey(key);
		}
		return count;
	}

	@Override
	public int deleteAll() {
		int size = store.size();
		store.clear();
		return size;
	}

	@Override
	public int deleteExpired(LocalDateTime currentTime) {
		List<String> expired = new ArrayList<>();
		for (CacheEntry entry : store.values()) {
			if (entry.getExpireTime() != null && !entry.getExpireTime().isAfter(currentTime)) {
				expired.add(entry.getCacheKey());
			}
		}
		expired.forEach(store::remove);
		return expired.size();
	}

	@Override
	public List<String> selectAllKeys() {
		List<String> keys = new ArrayList<>();
		for (CacheEntry entry : store.values()) {
			if (!isExpired(entry)) {
				keys.add(entry.getCacheKey());
			}
		}
		return keys;
	}

	@Override
	public Long countCache() {
		return (long) selectAllKeys().size();
	}

	private boolean isExpired(CacheEntry entry) {
		return entry.getExpireTime() != null && entry.getExpireTime().isBefore(LocalDateTime.now());
	}

	private CacheEntry copy(CacheEntry source) {
		CacheEntry target = new CacheEntry(source.getCacheKey(), source.getCacheValue(),
				source.getCacheType(), source.getExpireTime());
		target.setId(source.getId());
		target.setCreateTime(source.getCreateTime());
		target.setModifyTime(source.getModifyTime());
		return target;
	}
}
