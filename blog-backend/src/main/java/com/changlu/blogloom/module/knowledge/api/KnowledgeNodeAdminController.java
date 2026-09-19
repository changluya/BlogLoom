package com.changlu.blogloom.module.knowledge.api;

import com.changlu.blogloom.model.vo.Result;
import com.changlu.blogloom.module.knowledge.domain.dto.*;
import com.changlu.blogloom.module.knowledge.service.KnowledgeImportTaskService;
import com.changlu.blogloom.module.knowledge.service.KnowledgeNodeService;
import com.changlu.blogloom.module.knowledge.service.MarkdownArchiveService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/admin/knowledge")
public class KnowledgeNodeAdminController {
	private final KnowledgeNodeService nodeService;
	private final MarkdownArchiveService archiveService;
	private final KnowledgeImportTaskService taskService;

	public KnowledgeNodeAdminController(KnowledgeNodeService nodeService, MarkdownArchiveService archiveService,
	                                    KnowledgeImportTaskService taskService) {
		this.nodeService = nodeService;
		this.archiveService = archiveService;
		this.taskService = taskService;
	}

	@GetMapping("/tree")
	public Result tree() { return Result.ok("获取成功", nodeService.getTree()); }

	@PostMapping("/directories")
	public Result createDirectory(@RequestBody KnowledgeDirectoryCreate request) {
		return Result.ok("创建成功", nodeService.createDirectory(request));
	}

	@PutMapping("/nodes/{id}")
	public Result rename(@PathVariable Long id, @RequestBody KnowledgeNodeUpdate request) {
		nodeService.rename(id, request.getName()); return Result.ok("重命名成功");
	}

	@PutMapping("/nodes/{id}/move")
	public Result move(@PathVariable Long id, @RequestBody KnowledgeNodeMove request) {
		nodeService.move(id, request.getTargetParentId(), request.getTargetSort()); return Result.ok("移动成功");
	}

	@DeleteMapping("/nodes/{id}")
	public Result delete(@PathVariable Long id) { nodeService.deleteNode(id); return Result.ok("删除成功"); }

	@PostMapping("/nodes/batch-delete")
	public Result batchDelete(@RequestBody KnowledgeNodeBatchDelete request) {
		nodeService.deleteNodes(request.getIds());
		return Result.ok("批量删除成功");
	}

	@PostMapping("/repair")
	public Result repair() { return Result.ok("修复完成", Collections.singletonMap("created", nodeService.repairMissingBlogs())); }

	/**
	 * 一键导入第 1 步：上传 ZIP 预检。
	 * published 默认 true，即默认按“公开”导入；conflictPolicy 默认 SKIP（同名文档跳过）。
	 */
	@PostMapping("/import/preview")
	public Result preview(@RequestParam MultipartFile file,
	                      @RequestParam(defaultValue = "0") Long targetParentId,
	                      @RequestParam(defaultValue = "true") Boolean published,
	                      @RequestParam(defaultValue = "SKIP") String conflictPolicy) {
		KnowledgeImportOptions options = new KnowledgeImportOptions();
		options.setTargetParentId(targetParentId); options.setPublished(published);
		options.setConflictPolicy(conflictPolicy.toUpperCase());
		return Result.ok("预检完成", archiveService.preview(file, options));
	}

	/**
	 * 一键导入第 1 步（非 ZIP）：上传单个/多个 Markdown 文件预检。
	 * 目录由每篇 Markdown 元数据中的 knowledgeBasePath 决定（为空则落在导入根目录），缺失目录导入时逐级创建。
	 */
	@PostMapping("/import/preview/files")
	public Result previewFiles(@RequestParam("files") MultipartFile[] files,
	                           @RequestParam(defaultValue = "0") Long targetParentId,
	                           @RequestParam(defaultValue = "true") Boolean published,
	                           @RequestParam(defaultValue = "SKIP") String conflictPolicy) {
		KnowledgeImportOptions options = new KnowledgeImportOptions();
		options.setTargetParentId(targetParentId); options.setPublished(published);
		options.setConflictPolicy(conflictPolicy.toUpperCase());
		return Result.ok("预检完成", archiveService.previewDocuments(Arrays.asList(files), options));
	}

	/** 一键导入第 2 步：凭预检 token 提交异步导入任务，返回 taskId */
	@PostMapping("/import/{token}/execute")
	public Result execute(@PathVariable String token) {
		return Result.ok("导入任务已提交", Collections.singletonMap("taskId", archiveService.execute(token)));
	}

	/** 一键导入第 3 步：按 taskId 轮询导入进度与结果 */
	@GetMapping("/import/tasks/{taskId}")
	public Result progress(@PathVariable String taskId) { return Result.ok("获取成功", taskService.get(taskId)); }

	@GetMapping("/export")
	public void export(HttpServletResponse response) throws IOException { archiveService.exportAll(response); }

	@GetMapping("/export/documents/{id}")
	public void exportDocument(@PathVariable Long id, HttpServletResponse response) throws IOException {
		archiveService.exportDocument(id, response);
	}
}
