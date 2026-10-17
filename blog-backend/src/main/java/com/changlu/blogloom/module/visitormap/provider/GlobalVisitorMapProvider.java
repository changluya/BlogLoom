package com.changlu.blogloom.module.visitormap.provider;

import com.changlu.blogloom.module.visitormap.domain.GlobalVisitorMapVO;
import com.changlu.blogloom.module.visitormap.domain.VisitorMapWindow;

public interface GlobalVisitorMapProvider {
	GlobalVisitorMapVO getGlobalVisitorMap(VisitorMapWindow window);
}
