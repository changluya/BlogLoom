package com.changlu.blogloom.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.changlu.blogloom.constant.CacheKeyConstants;
import com.changlu.blogloom.service.BlogCacheService;
import com.changlu.blogloom.service.BlogService;

import java.util.Map;

/**
 * @Description: 缓存相关定时任务
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Component
public class CacheSyncScheduleTask {
	@Autowired
	BlogCacheService cacheService;
	@Autowired
	BlogService blogService;

	/**
	 * 从缓存同步博客文章浏览量到数据库
	 */
	public void syncBlogViewsToDatabase() {
		String cacheKey = CacheKeyConstants.BLOG_VIEWS_MAP;
		Map blogViewsMap = cacheService.getMapByHash(cacheKey);
		for (Object keyObj : blogViewsMap.keySet()) {
			Object viewsObj = blogViewsMap.get(keyObj);
			if (viewsObj == null) {
				continue;
			}
			Long blogId = Long.valueOf(keyObj.toString());
			Integer views = Integer.valueOf(viewsObj.toString());
			blogService.updateViews(blogId, views);
		}
	}
}
