package com.changlu.blogloom.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import com.changlu.blogloom.annotation.OperationLogger;
import com.changlu.blogloom.entity.SiteSetting;
import com.changlu.blogloom.model.dto.ImageHostTestRequest;
import com.changlu.blogloom.model.vo.Result;
import com.changlu.blogloom.service.SiteSettingService;
import com.changlu.blogloom.service.storage.ImageHostConnectivityService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 站点设置后台管理
 * @Author: changlu
 * @Date: 2026-09-13
 */
@RestController
@RequestMapping("/admin")
public class SiteSettingAdminController {
	@Autowired
	SiteSettingService siteSettingService;
	@Autowired
	ImageHostConnectivityService imageHostConnectivityService;

	/**
	 * 获取所有站点配置信息
	 *
	 * @return
	 */
	@GetMapping("/siteSettings")
	public Result siteSettings() {
		Map<String, List<SiteSetting>> typeMap = siteSettingService.getList();
		return Result.ok("请求成功", typeMap);
	}

	/**
	 * 修改、删除(部分配置可为空，但不可删除)、添加(只能添加部分)站点配置
	 *
	 * @param map 包含所有站点信息更新后的数据 map => {settings=[更新后的所有配置List], deleteIds=[要删除的配置id List]}
	 * @return
	 */
	@OperationLogger("更新站点配置信息")
	@PostMapping("/siteSettings")
	public Result updateAll(@RequestBody Map<String, Object> map) {
		List<LinkedHashMap> siteSettings = (List<LinkedHashMap>) map.get("settings");
		List<Integer> deleteIds = (List<Integer>) map.get("deleteIds");
		siteSettingService.updateSiteSetting(siteSettings, deleteIds);
		return Result.ok("更新成功");
	}

	@OperationLogger("上传站点设置图片")
	@PostMapping(value = "/siteSettings/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Result uploadImage(@PathVariable Integer id, @RequestParam("file") MultipartFile file) {
		return Result.ok("上传成功", siteSettingService.uploadImage(id, file));
	}

	/**
	 * 图床渠道连通性测试：模拟上传后立即删除。
	 *
	 * @param request 渠道标识与渠道配置字符串（配置为空时使用已保存配置）
	 * @return
	 */
	@OperationLogger("测试图床连通性")
	@PostMapping("/imageHost/test")
	public Result testImageHost(@RequestBody ImageHostTestRequest request) {
		return Result.ok("连通性正常",
				imageHostConnectivityService.test(request.getChannel(), request.getValue()));
	}

	/**
	 * 查询网页标题后缀
	 *
	 * @return
	 */
	@GetMapping("/webTitleSuffix")
	public Result getWebTitleSuffix() {
		return Result.ok("请求成功", siteSettingService.getWebTitleSuffix());
	}
}
