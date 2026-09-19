package com.changlu.blogloom.model.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * @Description: 随机博客
 * @Author: changlu
 * @Date: 2026-09-13
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
public class RandomBlog {
	private Long id;
	private String title;//文章标题
	private String firstPicture;//文章首图，用于随机文章展示
	private Date createTime;//创建时间
	private String password;//文章密码
	private Integer views;//阅读量
	private Boolean privacy;//是否私密文章
}
