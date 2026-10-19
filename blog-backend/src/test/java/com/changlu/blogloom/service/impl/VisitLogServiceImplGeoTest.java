package com.changlu.blogloom.service.impl;

import com.changlu.blogloom.entity.VisitLog;
import com.changlu.blogloom.mapper.VisitLogMapper;
import com.changlu.blogloom.mapper.VisitorGeoMapper;
import com.changlu.blogloom.model.dto.UserAgentDTO;
import com.changlu.blogloom.util.IpAddressUtils;
import com.changlu.blogloom.util.UserAgentUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VisitLogServiceImplGeoTest {
	private VisitLogServiceImpl service;
	private VisitLogMapper visitLogMapper;
	private VisitorGeoMapper visitorGeoMapper;
	private IpAddressUtils ipAddressUtils;

	@BeforeEach
	void setUp() {
		service = new VisitLogServiceImpl();
		visitLogMapper = mock(VisitLogMapper.class);
		visitorGeoMapper = mock(VisitorGeoMapper.class);
		ipAddressUtils = mock(IpAddressUtils.class);
		UserAgentUtils userAgentUtils = mock(UserAgentUtils.class);
		when(userAgentUtils.parseOsAndBrowser(any())).thenReturn(new UserAgentDTO("Mac OS", "Chrome"));
		when(visitLogMapper.saveVisitLog(any())).thenReturn(1);
		ReflectionTestUtils.setField(service, "visitLogMapper", visitLogMapper);
		ReflectionTestUtils.setField(service, "visitorGeoMapper", visitorGeoMapper);
		ReflectionTestUtils.setField(service, "ipAddressUtils", ipAddressUtils);
		ReflectionTestUtils.setField(service, "userAgentUtils", userAgentUtils);
	}

	@Test
	void shouldResolveCityAndPersistLocatedStatus() {
		when(ipAddressUtils.getCityInfo("1.0.1.1")).thenReturn("中国|广东省|深圳市|电信|CN");
		when(visitorGeoMapper.findCityGeoId("深圳市", "CN", "广东省")).thenReturn(3L);

		service.saveVisitLog(log("1.0.1.1"));

		VisitLog saved = captureSavedLog();
		assertEquals(3L, saved.getGeoId());
		assertEquals(1, saved.getGeoStatus());
	}

	@Test
	void shouldFallBackToCountryWhenCityIsUnknown() {
		when(ipAddressUtils.getCityInfo("8.8.8.8")).thenReturn("United States|California|Unknown||US");
		when(visitorGeoMapper.findCityGeoId("Unknown", "US", "California")).thenReturn(null);
		when(visitorGeoMapper.findCountryGeoId("US")).thenReturn(9L);

		service.saveVisitLog(log("8.8.8.8"));

		VisitLog saved = captureSavedLog();
		assertEquals(9L, saved.getGeoId());
		assertEquals(1, saved.getGeoStatus());
	}

	@Test
	void shouldMarkPrivateIpWithoutGeoLookup() {
		when(ipAddressUtils.getCityInfo("192.168.1.8")).thenReturn("");

		service.saveVisitLog(log("192.168.1.8"));

		VisitLog saved = captureSavedLog();
		assertNull(saved.getGeoId());
		assertEquals(2, saved.getGeoStatus());
	}

	private VisitLog log(String ip) {
		return new VisitLog("uuid", "/", "GET", "访问页面", "首页", "", ip, 10, "ua");
	}

	private VisitLog captureSavedLog() {
		ArgumentCaptor<VisitLog> captor = ArgumentCaptor.forClass(VisitLog.class);
		verify(visitLogMapper).saveVisitLog(captor.capture());
		return captor.getValue();
	}
}
