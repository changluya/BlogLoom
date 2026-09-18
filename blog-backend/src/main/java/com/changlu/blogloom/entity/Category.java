package com.changlu.blogloom.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description: 博客分类
 * @Author: changlu
 * @Date: 2026-09-13
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Category {
	private Long id;
	private String name;//分类名称
	private Integer blogCount;//该分类下已发布博客数量
	private List<Blog> blogs = new ArrayList<>();//该分类下的博客文章
}
