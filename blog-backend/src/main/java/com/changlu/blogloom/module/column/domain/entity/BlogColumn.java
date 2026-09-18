package com.changlu.blogloom.module.column.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class BlogColumn {
	private Long id;
	private Long parentId;
	private String name;
	private String description;
	private String cover;
	private Integer sort;
	private Boolean published;
	private Date createTime;
	private Date updateTime;
}
