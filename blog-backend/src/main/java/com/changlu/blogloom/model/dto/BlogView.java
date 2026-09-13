package com.changlu.blogloom.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Description: 博客浏览量
 * @Author: changlu
 * @Date: 2026-09-13
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
public class BlogView {
	private Long id;
	private Integer views;
}
