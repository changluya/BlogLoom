package com.changlu.blogloom.module.seo.service;

/**
 * @Description: 站点级 SEO 文件内容（robots / sitemap / rss）统一出口，供公开端点与后台下载复用
 * @Author: changlu
 * @Date: 2026-10-06
 */
public interface SeoFileService {

	/** robots.txt 内容 */
	String robots();

	/** sitemap.xml 内容 */
	String sitemap();

	/** rss.xml 内容 */
	String rss();
}
