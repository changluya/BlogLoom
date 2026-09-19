package com.changlu.blogloom.module.column.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonPropertyOrder({"title", "description", "icon", "order", "children"})
public class ColumnImportItem {
	private String title;
	private String description;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private String icon;
	private Integer order;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<ColumnImportItem> children = new ArrayList<>();
}
