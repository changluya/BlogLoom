package com.changlu.blogloom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.changlu.blogloom.constant.CacheKeyConstants;
import com.changlu.blogloom.entity.About;
import com.changlu.blogloom.exception.PersistenceException;
import com.changlu.blogloom.mapper.AboutMapper;
import com.changlu.blogloom.service.AboutService;
import com.changlu.blogloom.service.BlogCacheService;
import com.changlu.blogloom.util.markdown.MarkdownUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Description: 关于我页面业务层实现
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Service
public class AboutServiceImpl implements AboutService {
	@Autowired
	AboutMapper aboutMapper;
	@Autowired
	BlogCacheService cacheService;

	@Override
	public Map<String, String> getAboutInfo() {
		String cacheKey = CacheKeyConstants.ABOUT_INFO_MAP;
		Map<String, String> aboutInfoMapFromCache = cacheService.getMapByValue(cacheKey);
		if (aboutInfoMapFromCache != null) {
			return aboutInfoMapFromCache;
		}
		List<About> abouts = aboutMapper.getList();
		Map<String, String> aboutInfoMap = new HashMap<>(16);
		for (About about : abouts) {
			if ("content".equals(about.getNameEn())) {
				about.setValue(MarkdownUtils.markdownToHtmlExtensions(about.getValue()));
			}
			aboutInfoMap.put(about.getNameEn(), about.getValue());
		}
		cacheService.saveMapToValue(cacheKey, aboutInfoMap);
		return aboutInfoMap;
	}

	@Override
	public Map<String, String> getAboutSetting() {
		List<About> abouts = aboutMapper.getList();
		Map<String, String> map = new HashMap<>(16);
		for (About about : abouts) {
			map.put(about.getNameEn(), about.getValue());
		}
		return map;
	}

	@Override
	public void updateAbout(Map<String, String> map) {
		Set<String> keySet = map.keySet();
		for (String key : keySet) {
			updateOneAbout(key, map.get(key));
		}
		deleteAboutCache();
	}

	@Transactional(rollbackFor = Exception.class)
	public void updateOneAbout(String nameEn, String value) {
		if (aboutMapper.updateAbout(nameEn, value) != 1) {
			throw new PersistenceException("修改失败");
		}
	}

	@Override
	public boolean getAboutCommentEnabled() {
		String commentEnabledString = aboutMapper.getAboutCommentEnabled();
		return Boolean.parseBoolean(commentEnabledString);
	}

	/**
	 * 删除关于我页面缓存
	 */
	private void deleteAboutCache() {
		cacheService.deleteCacheByKey(CacheKeyConstants.ABOUT_INFO_MAP);
	}
}
