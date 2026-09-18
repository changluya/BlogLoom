package com.changlu.blogloom.module.column.domain.vo;

import com.changlu.blogloom.module.column.domain.entity.BlogColumn;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ColumnTreeVo extends BlogColumn {
	private Integer blogCount;
	private List<ColumnTreeVo> children = new ArrayList<>();
}
