package com.changlu.blogloom.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.changlu.blogloom.annotation.VisitLogger;
import com.changlu.blogloom.enums.VisitBehavior;
import com.changlu.blogloom.model.vo.Result;
import com.changlu.blogloom.service.AboutService;

/**
 * @Description: 关于我页面
 * @Author: changlu
 * @Date: 2026-09-13
 */
@RestController
public class AboutController {
	@Autowired
	AboutService aboutService;

	/**
	 * 获取关于我页面信息
	 *
	 * @return
	 */
	@VisitLogger(VisitBehavior.ABOUT)
	@GetMapping("/about")
	public Result about() {
		return Result.ok("获取成功", aboutService.getAboutInfo());
	}
}
