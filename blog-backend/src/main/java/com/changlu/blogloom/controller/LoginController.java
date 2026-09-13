package com.changlu.blogloom.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.changlu.blogloom.constant.JwtConstants;
import com.changlu.blogloom.entity.User;
import com.changlu.blogloom.model.dto.LoginInfo;
import com.changlu.blogloom.model.vo.Result;
import com.changlu.blogloom.service.UserService;
import com.changlu.blogloom.util.JwtUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 前台登录
 * @Author: changlu
 * @Date: 2026-09-13
 */
@RestController
public class LoginController {
	@Autowired
	UserService userService;

	/**
	 * 登录成功后，签发博主身份Token
	 *
	 * @param loginInfo
	 * @return
	 */
	@PostMapping("/login")
	public Result login(@RequestBody LoginInfo loginInfo) {
		User user = userService.findUserByUsernameAndPassword(loginInfo.getUsername(), loginInfo.getPassword());
		if (!"ROLE_admin".equals(user.getRole())) {
			return Result.create(403, "无权限");
		}
		user.setPassword(null);
		String jwt = JwtUtils.generateToken(JwtConstants.ADMIN_PREFIX + user.getUsername());
		Map<String, Object> map = new HashMap<>(4);
		map.put("user", user);
		map.put("token", jwt);
		return Result.ok("登录成功", map);
	}
}
