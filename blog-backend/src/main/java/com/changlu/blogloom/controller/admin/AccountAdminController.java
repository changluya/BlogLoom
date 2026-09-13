package com.changlu.blogloom.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.changlu.blogloom.entity.User;
import com.changlu.blogloom.model.vo.Result;
import com.changlu.blogloom.service.UserService;

/**
 * @Description: 账号后台管理
 * @Author: changlu
 * @Date: 2026-09-13
 */
@RestController
@RequestMapping("/admin")
public class AccountAdminController {
	@Autowired
	UserService userService;

	/**
	 * 账号密码修改
	 */
	@PostMapping("/account")
	public Result account(@RequestBody User user, @RequestHeader(value = "Authorization", defaultValue = "") String jwt) {
		boolean res = userService.changeAccount(user, jwt);
		return res ? Result.ok("修改成功") : Result.error("修改失败");
	}
}
