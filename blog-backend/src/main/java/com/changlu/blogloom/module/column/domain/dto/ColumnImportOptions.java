package com.changlu.blogloom.module.column.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ColumnImportOptions {
	private String conflictPolicy = "SKIP";
	private Boolean published = Boolean.TRUE;
}
