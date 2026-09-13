package com.changlu.blogloom.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.changlu.blogloom.annotation.VisitLogger;
import com.changlu.blogloom.enums.VisitBehavior;
import com.changlu.blogloom.model.vo.BlogInfo;
import com.changlu.blogloom.model.vo.PageResult;
import com.changlu.blogloom.model.vo.Result;
import com.changlu.blogloom.service.BlogService;

/**
 * @Description: 分类
 * @Author: changlu
 * @Date: 2026-09-13
 */
@RestController
public class CategoryController {
	@Autowired
	BlogService blogService;

	/**
	 * 根据分类name分页查询公开博客列表
	 *
	 * @param categoryName 分类name
	 * @param pageNum      页码
	 * @return
	 */
	@VisitLogger(VisitBehavior.CATEGORY)
	@GetMapping("/category")
	public Result category(@RequestParam String categoryName,
	                       @RequestParam(defaultValue = "1") Integer pageNum) {
		PageResult<BlogInfo> pageResult = blogService.getBlogInfoListByCategoryNameAndIsPublished(categoryName, pageNum);
		return Result.ok("请求成功", pageResult);
	}
}
