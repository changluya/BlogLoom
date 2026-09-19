package com.changlu.blogloom.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 图床连通性测试入参：渠道标识 + 渠道配置字符串（可为空，为空时使用已保存配置）。
 * 配置统一以字符串传递，由后端按 channel 转换。
 */
@NoArgsConstructor
@Getter
@Setter
public class ImageHostTestRequest {
	private String channel;
	private String value;
}
