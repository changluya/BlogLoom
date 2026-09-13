package com.changlu.blogloom.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * @Description: 访客更新DTO
 * @Author: changlu
 * @Date: 2026-09-13
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class VisitLogUuidTime {
	private String uuid;
	private Date time;
	private Integer pv;
}
