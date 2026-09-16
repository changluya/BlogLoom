package com.changlu.blogloom.module.knowledge.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeNodeMove {
	private Long targetParentId = 0L;
	private Integer targetSort;
}
