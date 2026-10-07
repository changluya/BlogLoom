package com.changlu.blogloom.module.seo.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SeoMeta {
	private String title;
	private String description;
	private String canonicalUrl;
	private String baseUrl;
	private String image;
	private String siteName;
	private String authorName;
	private String category;
	private String publishedTime;
	private String modifiedTime;
	private String type = "website";
	private String ldJson;
	private List<String> tags = new ArrayList<>();

	/** 站点验证 content（SSR 页面 <head> 输出，未配置时为空） */
	private String baiduVerification;
	private String bingVerification;
	private String googleVerification;
}
