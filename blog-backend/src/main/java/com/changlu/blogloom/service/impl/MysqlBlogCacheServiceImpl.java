package com.changlu.blogloom.service.impl;

import com.changlu.blogloom.module.cache.constant.CacheConstant;
import com.changlu.blogloom.module.cache.dao.CacheMapper;
import com.changlu.blogloom.module.cache.domain.pojo.CacheEntry;
import com.changlu.blogloom.model.vo.BlogInfo;
import com.changlu.blogloom.model.vo.PageResult;
import com.changlu.blogloom.service.BlogCacheService;
import com.changlu.blogloom.util.JacksonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 基于 MySQL 的博客缓存实现，替代原有 Redis 实现。
 * Hash / Set 语义通过「逻辑键 + 冒号 + 字段」的独立行来模拟，
 * 后续若要切换回 Redis，只需新增一个 BlogCacheService 实现即可。
 * @Author: changlu
 * @Date: 2026-09-20
 */
@Slf4j
@Service
public class MysqlBlogCacheServiceImpl implements BlogCacheService {
	/**
	 * Hash / Set 成员键的分隔符
	 */
	private static final String FIELD_SEPARATOR = ":";

	@Autowired
	private CacheMapper cacheMapper;

	@Override
	public PageResult<BlogInfo> getBlogInfoPageResultByHash(String hash, Integer pageNum) {
		Object value = getValueByHashKey(hash, pageNum);
		if (value == null) {
			return null;
		}
		return JacksonUtils.convertValue(value, PageResult.class);
	}

	@Override
	public void saveKVToHash(String hash, Object key, Object value) {
		putRaw(fieldKey(hash, key), JacksonUtils.writeValueAsString(value), null);
	}

	@Override
	public void saveMapToHash(String hash, Map map) {
		if (map == null) {
			return;
		}
		for (Object entryObj : map.entrySet()) {
			Map.Entry entry = (Map.Entry) entryObj;
			saveKVToHash(hash, entry.getKey(), entry.getValue());
		}
	}

	@Override
	public Map getMapByHash(String hash) {
		List<CacheEntry> entries = cacheMapper.selectByKeyPrefix(prefix(hash));
		Map<String, Object> result = new LinkedHashMap<>(Math.max(entries.size(), 1));
		for (CacheEntry entry : entries) {
			result.put(extractField(hash, entry.getCacheKey()), readScalar(entry.getCacheValue()));
		}
		return result;
	}

	@Override
	public Object getValueByHashKey(String hash, Object key) {
		CacheEntry entry = cacheMapper.selectByKey(fieldKey(hash, key));
		if (entry == null) {
			return null;
		}
		return readScalar(entry.getCacheValue());
	}

	@Override
	public void incrementByHashKey(String hash, Object key, int increment) {
		if (increment < 0) {
			throw new RuntimeException("递增因子必须大于0");
		}
		incrementValue(fieldKey(hash, key), increment);
	}

	@Override
	public void deleteByHashKey(String hash, Object key) {
		cacheMapper.deleteByKey(fieldKey(hash, key));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> getListByValue(String key) {
		CacheEntry entry = cacheMapper.selectByKey(key);
		if (entry == null || entry.getCacheValue() == null) {
			return null;
		}
		return (List<T>) JacksonUtils.readValue(entry.getCacheValue(), List.class);
	}

	@Override
	public <T> void saveListToValue(String key, List<T> list) {
		putRaw(key, JacksonUtils.writeValueAsString(list), null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Map<String, T> getMapByValue(String key) {
		CacheEntry entry = cacheMapper.selectByKey(key);
		if (entry == null || entry.getCacheValue() == null) {
			return null;
		}
		return (Map<String, T>) JacksonUtils.readValue(entry.getCacheValue(), Map.class);
	}

	@Override
	public <T> void saveMapToValue(String key, Map<String, T> map) {
		putRaw(key, JacksonUtils.writeValueAsString(map), null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getObjectByValue(String key, Class t) {
		CacheEntry entry = cacheMapper.selectByKey(key);
		if (entry == null || entry.getCacheValue() == null) {
			return null;
		}
		return (T) JacksonUtils.readValue(entry.getCacheValue(), t);
	}

	@Override
	public void incrementByKey(String key, int increment) {
		if (increment < 0) {
			throw new RuntimeException("递增因子必须大于0");
		}
		incrementValue(key, increment);
	}

	@Override
	public void saveObjectToValue(String key, Object object) {
		putRaw(key, JacksonUtils.writeValueAsString(object), null);
	}

	@Override
	public void saveValueToSet(String key, Object value) {
		putRaw(fieldKey(key, value), JacksonUtils.writeValueAsString(value), null);
	}

	@Override
	public int countBySet(String key) {
		return cacheMapper.countByKeyPrefix(prefix(key));
	}

	@Override
	public void deleteValueBySet(String key, Object value) {
		cacheMapper.deleteByKey(fieldKey(key, value));
	}

	@Override
	public boolean hasValueInSet(String key, Object value) {
		return cacheMapper.selectByKey(fieldKey(key, value)) != null;
	}

	@Override
	public void deleteCacheByKey(String key) {
		// 逻辑键可能以单行（Value）或前缀多行（Hash / Set）两种形式存在，二者都需清理
		cacheMapper.deleteByKey(key);
		cacheMapper.deleteByKeyPrefix(prefix(key));
	}

	@Override
	public boolean hasKey(String key) {
		if (cacheMapper.selectByKey(key) != null) {
			return true;
		}
		return cacheMapper.countByKeyPrefix(prefix(key)) > 0;
	}

	@Override
	public void expire(String key, long time) {
		cacheMapper.updateExpire(key, LocalDateTime.now().plusSeconds(time));
	}

	/**
	 * 原子递增：键不存在时补插初值；若行已存在但已过期（或并发插入冲突），
	 * 则把计数重置为当前增量，随后由调用方的 expire 重新设置过期时间。
	 */
	private void incrementValue(String key, int increment) {
		int updated = cacheMapper.incrementByKey(key, increment);
		if (updated > 0) {
			return;
		}
		CacheEntry entry = new CacheEntry(key, String.valueOf(increment), CacheConstant.DEFAULT_CACHE_TYPE, null);
		try {
			cacheMapper.insertCache(entry);
		} catch (DuplicateKeyException e) {
			cacheMapper.updateCache(entry);
		}
	}

	/**
	 * 更新优先、插入兜底的写入逻辑
	 */
	private void putRaw(String key, String jsonValue, LocalDateTime expireTime) {
		CacheEntry entry = new CacheEntry(key, jsonValue, CacheConstant.DEFAULT_CACHE_TYPE, expireTime);
		int updated = cacheMapper.updateCache(entry);
		if (updated == 0) {
			try {
				cacheMapper.insertCache(entry);
			} catch (DuplicateKeyException e) {
				cacheMapper.updateCache(entry);
			}
		}
	}

	private String fieldKey(String key, Object field) {
		return key + FIELD_SEPARATOR + field;
	}

	private String prefix(String key) {
		return key + FIELD_SEPARATOR;
	}

	private String extractField(String key, String cacheKey) {
		return cacheKey.substring(key.length() + FIELD_SEPARATOR.length());
	}

	/**
	 * 将缓存值按 JSON 反序列化为标量（数字得到 Integer，字符串得到 String）
	 */
	private Object readScalar(String cacheValue) {
		if (cacheValue == null) {
			return null;
		}
		Object value = JacksonUtils.readValue(cacheValue, Object.class);
		return value != null ? value : cacheValue;
	}
}
