package top.naccl.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 访问日志备注
 *
 * @author: changlu
 * @date: 2026-09-13
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class VisitLogRemark {
	/**
	 * 访问内容
	 */
	private String content;

	/**
	 * 备注
	 */
	private String remark;
}
