package com.changlu.blogloom.module.knowledge.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class KnowledgeNodeBatchDelete {
	private List<Long> ids;
}
