package com.changlu.blogloom.module.column.api;

import com.changlu.blogloom.annotation.OperationLogger;
import com.changlu.blogloom.model.vo.Result;
import com.changlu.blogloom.module.column.domain.dto.ColumnMoveRequest;
import com.changlu.blogloom.module.column.domain.entity.BlogColumn;
import com.changlu.blogloom.module.column.service.BlogColumnService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin")
public class ColumnAdminController {
	private final BlogColumnService columnService;
	public ColumnAdminController(BlogColumnService columnService) { this.columnService = columnService; }

	@GetMapping("/columns/tree") public Result tree() { return Result.ok("获取成功", columnService.getAdminTree()); }
	@GetMapping("/columns/options") public Result options() { return Result.ok("获取成功", columnService.getAdminTree()); }
	@GetMapping("/column") public Result detail(@RequestParam Long id) { return Result.ok("获取成功", columnService.getById(id)); }
	@OperationLogger("创建专栏") @PostMapping("/column") public Result save(@RequestBody BlogColumn column) { return Result.ok("创建成功", columnService.save(column)); }
	@OperationLogger("修改专栏") @PutMapping("/column") public Result update(@RequestBody BlogColumn column) { columnService.update(column); return Result.ok("更新成功"); }
	@OperationLogger("移动专栏") @PutMapping("/column/{id}/move") public Result move(@PathVariable Long id, @RequestBody ColumnMoveRequest request) { columnService.move(id, request.getTargetParentId(), request.getTargetSort()); return Result.ok("移动成功"); }
	@OperationLogger("修改专栏展示状态") @PutMapping("/column/{id}/published") public Result published(@PathVariable Long id, @RequestParam Boolean published) { columnService.updatePublished(id, published); return Result.ok(published ? "已开启前台展示" : "已关闭前台展示"); }
	@OperationLogger("删除专栏") @DeleteMapping("/column") public Result delete(@RequestParam Long id) { columnService.delete(id); return Result.ok("删除成功"); }
	@OperationLogger("上传专栏图片") @PostMapping(value="/column/{id}/cover", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
	public Result cover(@PathVariable Long id, @RequestParam("file") MultipartFile file) { return Result.ok("上传成功", columnService.uploadCover(id, file)); }
}
