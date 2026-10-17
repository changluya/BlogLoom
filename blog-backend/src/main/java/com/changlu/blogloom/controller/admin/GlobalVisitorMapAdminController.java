package com.changlu.blogloom.controller.admin;

import com.changlu.blogloom.model.vo.Result;
import com.changlu.blogloom.module.visitormap.service.GlobalVisitorMapService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/visitor-map")
public class GlobalVisitorMapAdminController {
	private final GlobalVisitorMapService globalVisitorMapService;

	public GlobalVisitorMapAdminController(GlobalVisitorMapService globalVisitorMapService) {
		this.globalVisitorMapService = globalVisitorMapService;
	}

	@GetMapping("/global")
	public Result global(
			@RequestParam(value = "periodType", required = false) String periodType,
			@RequestParam(value = "timezone", required = false) String timezone,
			@RequestParam(value = "startDate", required = false) String startDate,
			@RequestParam(value = "endDate", required = false) String endDate) {
		return Result.ok("获取成功", globalVisitorMapService.getGlobalVisitorMap(periodType, timezone, startDate, endDate));
	}
}
