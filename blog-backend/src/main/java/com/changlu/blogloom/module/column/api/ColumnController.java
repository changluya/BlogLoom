package com.changlu.blogloom.module.column.api;

import com.changlu.blogloom.model.vo.Result;
import com.changlu.blogloom.module.column.service.BlogColumnService;
import org.springframework.web.bind.annotation.*;

@RestController
public class ColumnController {
	private final BlogColumnService columnService;
	public ColumnController(BlogColumnService columnService) { this.columnService = columnService; }

	@GetMapping("/columns/tree") public Result tree() { return Result.ok("获取成功", columnService.getPublicTree()); }
	@GetMapping("/column/{id}") public Result detail(@PathVariable Long id) { return Result.ok("获取成功", columnService.getPublicById(id)); }
	@GetMapping("/column/{id}/blogs") public Result blogs(@PathVariable Long id, @RequestParam(defaultValue="1") Integer pageNum) { return Result.ok("获取成功", columnService.getPublicBlogs(id, pageNum)); }
}
