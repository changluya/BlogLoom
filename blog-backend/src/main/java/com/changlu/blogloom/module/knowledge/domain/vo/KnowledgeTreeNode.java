package com.changlu.blogloom.module.knowledge.domain.vo;

import com.changlu.blogloom.module.knowledge.domain.entity.KnowledgeNode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class KnowledgeTreeNode {
	private Long id;
	private Long parentId;
	private Long blogId;
	private String name;
	private String type;
	private Integer sort;
	private String blogTitle;
	private Boolean blogExists;
	private List<KnowledgeTreeNode> children = new ArrayList<>();

	public static KnowledgeTreeNode from(KnowledgeNode node) {
		KnowledgeTreeNode value = new KnowledgeTreeNode();
		value.id = node.getId();
		value.parentId = node.getParentId();
		value.blogId = node.getBlogId();
		value.name = node.getName();
		value.type = node.getType();
		value.sort = node.getSort();
		value.blogTitle = node.getBlogTitle();
		value.blogExists = node.getBlogExists();
		return value;
	}
}
