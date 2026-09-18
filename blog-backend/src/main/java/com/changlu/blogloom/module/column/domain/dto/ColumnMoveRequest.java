package com.changlu.blogloom.module.column.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ColumnMoveRequest {
	private Long targetParentId;
	private Integer targetSort;
}
