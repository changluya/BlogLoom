package com.changlu.blogloom.module.seo.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class SeoArticle {
	private Long id;
	private String title;
	private String description;
	private String firstPicture;
	private Date createTime;
	private Date updateTime;
}
