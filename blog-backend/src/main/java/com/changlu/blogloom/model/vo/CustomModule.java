package com.changlu.blogloom.model.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Description: 自定义模块（站点配置 customModule 的 JSON 结构）
 * @Author: changlu
 * @Date: 2026-09-19
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
public class CustomModule {
	private String title;//栏目标题
	private String content;//栏目内容（支持自定义 HTML）
	private Boolean enabled;//是否开启
}
