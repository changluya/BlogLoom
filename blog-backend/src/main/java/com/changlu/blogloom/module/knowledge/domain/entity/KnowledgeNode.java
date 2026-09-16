package com.changlu.blogloom.module.knowledge.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class KnowledgeNode {
	private Long id;
	private Long parentId;
	private Long blogId;
	private String name;
	private String type;
	private Integer sort;
	private Date createTime;
	private Date updateTime;
	private String blogTitle;
	private Boolean blogExists;
}
