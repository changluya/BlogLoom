package com.changlu.blogloom.module.seo.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Description: SEO 配置（后台 SEO 配置页）
 * @Author: changlu
 * @Date: 2026-10-06
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
public class SeoConfig {
	/** 当前生效的 SEO 域名（后台配置优先，兜底配置参数 blog.view） */
	private String seoDomain;
	/** 配置文件默认值（blog.view），用于后台展示提示 */
	private String defaultSeoDomain;
	/** 是否已在后台显式配置 */
	private Boolean configured;
}
