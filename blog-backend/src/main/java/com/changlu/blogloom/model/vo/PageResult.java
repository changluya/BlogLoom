package com.changlu.blogloom.model.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @Description: 分页结果
 * @Author: changlu
 * @Date: 2026-09-13
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
public class PageResult<T> {
	private Integer totalPage;//总页数
	private List<T> list;//数据

	public PageResult(Integer totalPage, List<T> list) {
		this.totalPage = totalPage;
		this.list = list;
	}
}
