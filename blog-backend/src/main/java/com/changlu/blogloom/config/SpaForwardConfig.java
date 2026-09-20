package com.changlu.blogloom.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 前后端同源部署时的单页应用（SPA）路由回退配置。
 *
 * <p>Docker 部署中，博客前台（static/view，映射到根路径）和内容管理后台（static/cms，
 * 映射到 /cms）都由后端直接提供静态资源。浏览器刷新 history 路由（如 /blog/1、/cms/dashboard）
 * 时服务端并不存在对应文件，需要回退到各自入口 index.html；而接口请求、静态资源请求不应被回退。</p>
 *
 * @author BlogLoom
 */
@Configuration
public class SpaForwardConfig {

	/**
	 * 这些前缀属于接口、上传静态资源或系统路径，不参与 SPA 回退。
	 */
	private static final List<String> RESERVED_PREFIXES = Arrays.asList(
			"/admin", "/static", "/image", "/tg", "/error", "/actuator"
	);

	@Bean
	public FilterRegistrationBean<SpaForwardFilter> spaForwardFilter() {
		FilterRegistrationBean<SpaForwardFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(new SpaForwardFilter());
		registration.addUrlPatterns("/*");
		registration.setName("spaForwardFilter");
		registration.setOrder(Integer.MIN_VALUE);
		return registration;
	}

	static class SpaForwardFilter implements Filter {

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
				chain.doFilter(request, response);
				return;
			}
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			if (!shouldForward(httpRequest)) {
				chain.doFilter(request, response);
				return;
			}
			String path = resolvePath(httpRequest);
			String target = (path.equals("/cms") || path.startsWith("/cms/"))
					? "/cms/index.html"
					: "/index.html";
			httpRequest.getRequestDispatcher(target).forward(request, response);
		}

		/**
		 * 仅对浏览器文档导航（GET 且 Accept 包含 text/html）做回退，避免误伤接口与静态资源请求。
		 */
		private boolean shouldForward(HttpServletRequest request) {
			if (!"GET".equalsIgnoreCase(request.getMethod())) {
				return false;
			}
			String accept = request.getHeader("Accept");
			if (accept == null || !accept.contains("text/html")) {
				return false;
			}
			String path = resolvePath(request);
			for (String prefix : RESERVED_PREFIXES) {
				if (path.equals(prefix) || path.startsWith(prefix + "/")) {
					return false;
				}
			}
			String lastSegment = path.substring(path.lastIndexOf('/') + 1);
			// 带扩展名的请求视为静态资源，不回退。
			return lastSegment.indexOf('.') < 0;
		}

		private String resolvePath(HttpServletRequest request) {
			String path = request.getRequestURI();
			String contextPath = request.getContextPath();
			if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
				path = path.substring(contextPath.length());
			}
			return path.isEmpty() ? "/" : path;
		}
	}
}
