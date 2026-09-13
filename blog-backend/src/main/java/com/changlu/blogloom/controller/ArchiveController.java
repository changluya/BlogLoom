package com.changlu.blogloom.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.changlu.blogloom.annotation.VisitLogger;
import com.changlu.blogloom.enums.VisitBehavior;
import com.changlu.blogloom.model.vo.Result;
import com.changlu.blogloom.service.BlogService;

import java.util.Map;

/**
 * @Description: 归档页面
 * @Author: changlu
 * @Date: 2026-09-13
 */
@RestController
public class ArchiveController {
	@Autowired
	BlogService blogService;

	/**
	 * 按年月分组归档公开博客 统计公开博客总数
	 *
	 * @return
	 */
	@VisitLogger(VisitBehavior.ARCHIVE)
	@GetMapping("/archives")
	public Result archives() {
		Map<String, Object> archiveBlogMap = blogService.getArchiveBlogAndCountByIsPublished();
		return Result.ok("请求成功", archiveBlogMap);
	}
}
