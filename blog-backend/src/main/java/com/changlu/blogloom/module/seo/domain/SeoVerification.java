package com.changlu.blogloom.module.seo.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Description: 各搜索引擎站点验证 content
 * @Author: changlu
 * @Date: 2026-10-06
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
public class SeoVerification {
	/** 百度 baidu-site-verification 的 content */
	private String baidu;
	/** Bing msvalidate.01 的 content */
	private String bing;
	/** Google google-site-verification 的 content */
	private String google;
}
