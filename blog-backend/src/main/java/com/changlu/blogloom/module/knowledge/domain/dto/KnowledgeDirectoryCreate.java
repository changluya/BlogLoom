package com.changlu.blogloom.module.knowledge.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeDirectoryCreate {
	private Long parentId = 0L;
	private String name;
}
