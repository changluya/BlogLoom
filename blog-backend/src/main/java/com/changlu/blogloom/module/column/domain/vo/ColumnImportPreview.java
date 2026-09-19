package com.changlu.blogloom.module.column.domain.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ColumnImportPreview {
	private String token;
	private int rootCount;
	private int childCount;
	private int iconCount;
	private int missingIconCount;
	private List<String> paths;

	public ColumnImportPreview(String token, int rootCount, int childCount, int iconCount, int missingIconCount, List<String> paths) {
		this.token = token;
		this.rootCount = rootCount;
		this.childCount = childCount;
		this.iconCount = iconCount;
		this.missingIconCount = missingIconCount;
		this.paths = paths;
	}
}
