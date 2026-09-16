package com.changlu.blogloom.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.changlu.blogloom.entity.Category;
import com.changlu.blogloom.entity.Tag;
import com.changlu.blogloom.model.vo.NewBlog;
import com.changlu.blogloom.model.vo.RandomBlog;
import com.changlu.blogloom.model.vo.Result;
import com.changlu.blogloom.service.BlogService;
import com.changlu.blogloom.service.CategoryService;
import com.changlu.blogloom.service.DashboardService;
import com.changlu.blogloom.service.SiteSettingService;
import com.changlu.blogloom.service.TagService;

import java.util.List;
import java.util.Map;

/**
 * @Description: 站点相关
 * @Author: changlu
 * @Date: 2026-09-13
 */

@RestController
public class IndexController {
	@Autowired
	SiteSettingService siteSettingService;
	@Autowired
	BlogService blogService;
	@Autowired
	CategoryService categoryService;
	@Autowired
	TagService tagService;
	@Autowired
	DashboardService dashboardService;

	/**
	 * 获取站点配置信息、最新推荐博客、分类列表、标签云、随机博客
	 *
	 * @return
	 */
	@GetMapping("/site")
	public Result site() {
		Map<String, Object> map = siteSettingService.getSiteInfo();
		List<NewBlog> newBlogList = blogService.getNewBlogListByIsPublished();
		List<Category> categoryList = categoryService.getCategoryNameList();
		List<Tag> tagList = tagService.getTagListNotId();
		List<RandomBlog> randomBlogList = blogService.getRandomBlogListByLimitNumAndIsPublishedAndIsRecommend();
		map.put("totalViews", dashboardService.countVisitLog());
		map.put("publishedBlogCount", blogService.countBlogByIsPublished());
		map.put("totalBlogViews", blogService.sumViewsByIsPublished());
		map.put("newBlogList", newBlogList);
		map.put("categoryList", categoryList);
		map.put("tagList", tagList);
		map.put("randomBlogList", randomBlogList);
		return Result.ok("请求成功", map);
	}
}
