package com.changlu.blogloom.controller.admin;

import com.changlu.blogloom.model.vo.Result;
import com.changlu.blogloom.service.LocalResourceStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 博客编辑器资源上传接口。
 */
@RestController
@RequestMapping("/admin/blog")
public class BlogResourceAdminController {
	@Autowired
	private LocalResourceStorageService localResourceStorageService;

	@PostMapping(value = "/resources", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Result upload(@RequestParam("file") MultipartFile file) {
		return Result.ok("上传成功", localResourceStorageService.uploadTemp(file));
	}
}
