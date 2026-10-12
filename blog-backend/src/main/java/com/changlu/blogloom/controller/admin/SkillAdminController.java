package com.changlu.blogloom.controller.admin;

import com.changlu.blogloom.constant.JwtConstants;
import com.changlu.blogloom.service.SkillPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
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
	public void download(Principal principal, HttpServletRequest request, HttpServletResponse response) throws IOException {
		String username = resolveUsername(principal);
		skillPackageService.download(username, resolveBaseUrl(request), response);
	}

	/**
	 * 优先使用反向代理传递的外部协议、主机和路径前缀，使下载包指向
	 * 用户实际访问的部署服务，而不是容器内部地址。
	 */
	String resolveBaseUrl(HttpServletRequest request) {
		String scheme = firstHeaderValue(request, "X-Forwarded-Proto");
		if (isBlank(scheme)) {
			scheme = request.getScheme();
		}

		String host = firstHeaderValue(request, "X-Forwarded-Host");
		if (isBlank(host)) {
			host = request.getHeader("Host");
		}
		if (isBlank(host)) {
			host = request.getServerName();
			int port = request.getServerPort();
			if (port > 0 && !("http".equalsIgnoreCase(scheme) && port == 80)
					&& !("https".equalsIgnoreCase(scheme) && port == 443)) {
				host += ":" + port;
			}
		}

		String prefix = firstHeaderValue(request, "X-Forwarded-Prefix");
		if (isBlank(prefix)) {
			prefix = request.getContextPath();
		}
		prefix = normalizePrefix(prefix);
		return scheme + "://" + host + prefix;
	}

	private String firstHeaderValue(HttpServletRequest request, String name) {
		String value = request.getHeader(name);
		if (isBlank(value)) {
			return null;
		}
		return value.split(",", 2)[0].trim();
	}

	private String normalizePrefix(String prefix) {
		if (isBlank(prefix) || "/".equals(prefix.trim())) {
			return "";
		}
		String value = prefix.trim();
		if (!value.startsWith("/")) {
			value = "/" + value;
		}
		while (value.endsWith("/")) {
			value = value.substring(0, value.length() - 1);
		}
		return value;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
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
