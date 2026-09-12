package top.naccl.entity;

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
	private List<Blog> blogs = new ArrayList<>();//该分类下的博客文章
}
