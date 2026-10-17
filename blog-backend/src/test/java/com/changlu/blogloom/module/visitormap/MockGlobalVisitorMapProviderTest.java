package com.changlu.blogloom.module.visitormap;

import com.changlu.blogloom.module.visitormap.domain.GlobalVisitorMapVO;
import com.changlu.blogloom.module.visitormap.domain.VisitorMapWindow;
import com.changlu.blogloom.module.visitormap.provider.MockGlobalVisitorMapProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockGlobalVisitorMapProviderTest {
	private MockGlobalVisitorMapProvider provider;

	@BeforeEach
	void setUp() {
		provider = new MockGlobalVisitorMapProvider(new ObjectMapper());
		provider.load();
	}

	@Test
	void shouldLoadCompleteGlobalVisitorDataset() {
		GlobalVisitorMapVO result = provider.getGlobalVisitorMap(VisitorMapWindow.resolve("all", "Asia/Shanghai", null, null));

		assertEquals("mock", result.getMeta().getDataMode());
		assertEquals("WGS84", result.getMeta().getCoordinateSystem());
		assertEquals(19, result.getCountries().size());
		assertEquals(40, result.getCities().size());
		assertEquals(40, result.getSummary().getCityCount());
		assertTrue(result.getSummary().getPv() >= result.getMeta().getQuality().getResolvedPv());
	}

	@Test
	void shouldProvideValidCodesCoordinatesAndStableKeys() {
		GlobalVisitorMapVO result = provider.getGlobalVisitorMap(VisitorMapWindow.resolve("all", "UTC", null, null));
		Set<String> keys = new HashSet<>();
		for (GlobalVisitorMapVO.City city : result.getCities()) {
			assertNotNull(city.getCityNameZh());
			assertEquals(2, city.getCountryCode().length());
			assertTrue(city.getLat() >= -90 && city.getLat() <= 90);
			assertTrue(city.getLng() >= -180 && city.getLng() <= 180);
			assertTrue(keys.add(city.getGeoKey()), "地域键必须唯一: " + city.getGeoKey());
		}
	}
}
