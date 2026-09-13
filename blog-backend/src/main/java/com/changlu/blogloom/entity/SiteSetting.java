package com.changlu.blogloom.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Description: 站点设置
 * @Author: changlu
 * @Date: 2026-09-13
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class SiteSetting {
	private Long id;
	private String nameEn;
	private String nameZh;
	private String value;
	private Integer type;
}
