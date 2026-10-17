package com.changlu.blogloom.module.visitormap.service;

import com.changlu.blogloom.module.visitormap.domain.GlobalVisitorMapVO;

public interface GlobalVisitorMapService {
	/**
	 * @param periodType all / today / yesterday / last_7d / custom / rolling_24h，默认 all
	 * @param startDate  自定义开始日期（yyyy-MM-dd），仅 custom 需要
	 * @param endDate    自定义结束日期（yyyy-MM-dd），仅 custom 需要
	 */
	GlobalVisitorMapVO getGlobalVisitorMap(String periodType, String timezone, String startDate, String endDate);
}
