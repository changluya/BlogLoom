package com.changlu.blogloom.module.visitormap.service.impl;

import com.changlu.blogloom.module.visitormap.domain.GlobalVisitorMapVO;
import com.changlu.blogloom.module.visitormap.domain.VisitorMapWindow;
import com.changlu.blogloom.module.visitormap.provider.GlobalVisitorMapProvider;
import com.changlu.blogloom.module.visitormap.service.GlobalVisitorMapService;
import org.springframework.stereotype.Service;

@Service
public class GlobalVisitorMapServiceImpl implements GlobalVisitorMapService {
	private final GlobalVisitorMapProvider provider;

	public GlobalVisitorMapServiceImpl(GlobalVisitorMapProvider provider) {
		this.provider = provider;
	}

	@Override
	public GlobalVisitorMapVO getGlobalVisitorMap(String periodType, String timezone, String startDate, String endDate) {
		VisitorMapWindow window = VisitorMapWindow.resolve(periodType, timezone, startDate, endDate);
		return provider.getGlobalVisitorMap(window);
	}
}
