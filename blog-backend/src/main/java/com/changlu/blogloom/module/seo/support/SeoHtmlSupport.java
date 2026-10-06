package com.changlu.blogloom.module.seo.support;

import com.changlu.blogloom.module.seo.domain.SeoTocItem;

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SeoHtmlSupport {
	private static final Pattern IMAGE_TAG = Pattern.compile("<img([^>]*)>", Pattern.CASE_INSENSITIVE);
	private static final Pattern SRC_ATTRIBUTE = Pattern.compile("(?:^|\\s)src\\s*=", Pattern.CASE_INSENSITIVE);
	private static final Pattern DATA_SRC_ATTRIBUTE = Pattern.compile("\\sdata-src\\s*=", Pattern.CASE_INSENSITIVE);
	private static final Pattern LOADING_ATTRIBUTE = Pattern.compile("(?:^|\\s)loading\\s*=", Pattern.CASE_INSENSITIVE);
	private static final Pattern HEADING = Pattern.compile("<h([2-6])[^>]*\\sid=[\"']([^\"']+)[\"'][^>]*>(.*?)</h\\1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
	private static final Pattern TOC_PLACEHOLDER = Pattern.compile("(?i)<p>\\s*\\[toc]\\s*</p>");

	private SeoHtmlSupport() {
	}

	public static String optimizeArticleHtml(String html) {
		if (html == null || html.isEmpty()) return "";
		String headings = html.replaceAll("(?i)<h1(\\s|>)", "<h2$1")
				.replaceAll("(?i)</h1>", "</h2>");
		Matcher matcher = IMAGE_TAG.matcher(headings);
		StringBuffer optimized = new StringBuffer();
		while (matcher.find()) {
			String attributes = matcher.group(1);
			// Markdown 渲染器为 SPA 懒加载产出 data-src；SSR 无对应脚本，需恢复真实 src。
			if (!SRC_ATTRIBUTE.matcher(attributes).find()) {
				attributes = DATA_SRC_ATTRIBUTE.matcher(attributes).replaceFirst(" src=");
			}
			String loading = LOADING_ATTRIBUTE.matcher(attributes).find()
					? "" : " loading=\"lazy\" decoding=\"async\"";
			matcher.appendReplacement(optimized, Matcher.quoteReplacement("<img" + loading + attributes + ">"));
		}
		matcher.appendTail(optimized);
		return optimized.toString();
	}

	public static List<SeoTocItem> extractToc(String html) {
		List<SeoTocItem> items = new ArrayList<>();
		if (html == null || html.isEmpty()) return items;
		Matcher matcher = HEADING.matcher(html);
		while (matcher.find()) {
			String title = HTML_TAG.matcher(matcher.group(3)).replaceAll("").trim();
			if (!title.isEmpty()) items.add(new SeoTocItem(matcher.group(2), title, Integer.parseInt(matcher.group(1))));
		}
		return items;
	}

	public static String renderInlineToc(String html) {
		if (html == null || !TOC_PLACEHOLDER.matcher(html).find()) return html;
		List<SeoTocItem> items = extractToc(html);
		StringBuilder toc = new StringBuilder("<section class=\"article-inline-toc\"><div class=\"article-inline-toc-title\">文章目录</div>");
		if (items.isEmpty()) {
			toc.append("<div class=\"article-inline-toc-empty\">暂无可展示的文章标题</div>");
		} else {
			int minLevel = items.stream().mapToInt(SeoTocItem::getLevel).min().orElse(2);
			toc.append("<ul>");
			for (SeoTocItem item : items) {
				int level = Math.min(item.getLevel() - minLevel, 5);
				toc.append("<li class=\"article-inline-toc-level-").append(level).append("\"><a class=\"toc-link\" href=\"#")
						.append(escapeAttribute(item.getId())).append("\">").append(escapeText(item.getTitle())).append("</a></li>");
			}
			toc.append("</ul>");
		}
		toc.append("</section>");
		return TOC_PLACEHOLDER.matcher(html).replaceAll(Matcher.quoteReplacement(toc.toString()));
	}

	private static String escapeAttribute(String value) {
		return escapeText(value).replace("\"", "&quot;");
	}

	private static String escapeText(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
