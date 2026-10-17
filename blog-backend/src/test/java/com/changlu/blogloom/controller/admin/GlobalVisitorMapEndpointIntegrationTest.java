package com.changlu.blogloom.controller.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class GlobalVisitorMapEndpointIntegrationTest {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldExposeFrontendContractFromGlobalEndpoint() throws Exception {
		mockMvc.perform(get("/admin/visitor-map/global")
				.param("periodType", "rolling_24h")
				.param("timezone", "Asia/Shanghai"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.meta.dataMode").value("database"))
				.andExpect(jsonPath("$.data.meta.coordinateSystem").value("WGS84"))
				.andExpect(jsonPath("$.data.summary.pv").isNumber())
				.andExpect(jsonPath("$.data.summary.uv").isNumber())
				.andExpect(jsonPath("$.data.countries").isArray())
				.andExpect(jsonPath("$.data.cities").isArray());
	}
}
