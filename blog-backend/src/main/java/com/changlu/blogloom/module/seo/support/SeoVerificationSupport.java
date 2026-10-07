package com.changlu.blogloom.module.seo.support;

import java.util.Map;

/**
 * @Description: 站点验证 meta 标签构建（百度 / Bing / Google）
 * @Author: changlu
 * @Date: 2026-10-06
 */
public final class SeoVerificationSupport {

	private SeoVerificationSupport() {
	}

	/**
	 * 按配置构建站点验证 meta 标签（仅输出非空的项）。
	 *
	 * @param codes key：baidu / bing / google -> content
	 * @return 拼接好的 &lt;meta&gt; 标签字符串（可能为空串）
	 */
	public static String buildMetaTags(Map<String, String> codes) {
		if (codes == null || codes.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		append(sb, "baidu-site-verification", codes.get("baidu"));
		append(sb, "msvalidate.01", codes.get("bing"));
		append(sb, "google-site-verification", codes.get("google"));
		return sb.toString();
	}

	/** 把标签注入到 &lt;/head&gt; 之前；找不到 head 结束标签时原样返回 */
	public static String injectBeforeHeadEnd(String html, String tags) {
		if (html == null || tags == null || tags.isEmpty()) {
			return html;
		}
		int index = html.toLowerCase().indexOf("</head>");
		if (index < 0) {
			return html;
		}
		return html.substring(0, index) + tags + html.substring(index);
	}

	private static void append(StringBuilder sb, String name, String content) {
		if (content == null || content.trim().isEmpty()) {
			return;
		}
		sb.append("\t<meta name=\"").append(name).append("\" content=\"")
				.append(escape(content.trim())).append("\">\n");
	}

	private static String escape(String value) {
		return value.replace("&", "&amp;").replace("\"", "&quot;")
				.replace("<", "&lt;").replace(">", "&gt;");
	}
}
