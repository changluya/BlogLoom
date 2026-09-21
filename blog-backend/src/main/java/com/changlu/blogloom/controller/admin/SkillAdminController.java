package com.changlu.blogloom.controller.admin;

import com.changlu.blogloom.constant.JwtConstants;
import com.changlu.blogloom.service.SkillPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;

/**
 * @Description: BlogLoom Skill 下载（把本地博客同步到平台的 AI 技能包）。
 *
 * <p>下载的 ZIP 内含 SKILL.md、references SOP、py 工具与测试，并已注入当前登录用户的
 * BASE_URL 与专属 Token，可直接交给 AI Agent 使用。</p>
 *
 * @Author: changlu
 * @Date: 2026-09-21
 */
@RestController
@RequestMapping("/admin/skill")
public class SkillAdminController {

	@Autowired
	private SkillPackageService skillPackageService;

	/**
	 * 下载当前用户专属的 BlogLoom Skill 包。
	 */
	@GetMapping("/download")
	public void download(Principal principal, HttpServletResponse response) throws IOException {
		String username = resolveUsername(principal);
		skillPackageService.download(username, response);
	}

	private String resolveUsername(Principal principal) {
		String name = principal == null ? null : principal.getName();
		if (name == null || name.isEmpty()) {
			name = "admin";
		}
		// JWT subject 形如 admin:admin，打包时仅取真实用户名
		if (name.startsWith(JwtConstants.ADMIN_PREFIX)) {
			name = name.substring(JwtConstants.ADMIN_PREFIX.length());
		}
		return name;
	}
}