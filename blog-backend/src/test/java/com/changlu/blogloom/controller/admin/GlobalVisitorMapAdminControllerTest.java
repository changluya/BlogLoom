package com.changlu.blogloom.controller.admin;

import com.changlu.blogloom.model.vo.Result;
import com.changlu.blogloom.module.visitormap.domain.GlobalVisitorMapVO;
import com.changlu.blogloom.module.visitormap.service.GlobalVisitorMapService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlobalVisitorMapAdminControllerTest {
	@Test
	void shouldReturnUnifiedResultFromDedicatedEndpoint() {
		GlobalVisitorMapService service = mock(GlobalVisitorMapService.class);
		GlobalVisitorMapVO data = new GlobalVisitorMapVO();
		when(service.getGlobalVisitorMap("rolling_24h", "Asia/Shanghai", null, null)).thenReturn(data);
		GlobalVisitorMapAdminController controller = new GlobalVisitorMapAdminController(service);

		Result result = controller.global("rolling_24h", "Asia/Shanghai", null, null);

		assertEquals(200, result.getCode());
		assertEquals("获取成功", result.getMsg());
		assertSame(data, result.getData());
		verify(service).getGlobalVisitorMap("rolling_24h", "Asia/Shanghai", null, null);
	}
}
