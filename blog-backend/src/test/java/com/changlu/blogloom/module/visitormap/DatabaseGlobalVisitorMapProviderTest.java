package com.changlu.blogloom.module.visitormap;

import com.changlu.blogloom.module.visitormap.domain.GlobalVisitorMapVO;
import com.changlu.blogloom.module.visitormap.domain.VisitorMapWindow;
import com.changlu.blogloom.module.visitormap.provider.DatabaseGlobalVisitorMapProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class DatabaseGlobalVisitorMapProviderTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private DatabaseGlobalVisitorMapProvider provider;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM visit_log");
		jdbcTemplate.update("DELETE FROM visitor_geo");
		jdbcTemplate.update("INSERT INTO visitor_geo (id, geo_key, geo_level, country_code, city_name_zh, city_name_en, longitude, latitude) VALUES (1, 'CN|CITY|SHENZHEN', 2, 'CN', '深圳市', 'Shenzhen', 114.064552, 22.548457)");
		jdbcTemplate.update("INSERT INTO visitor_geo (id, geo_key, geo_level, country_code, longitude, latitude) VALUES (2, 'US|COUNTRY', 1, 'US', -97, 38)");
		insertVisit("u-cn", 1L, 1, true);
		insertVisit("u-cn", 1L, 1, true);
		insertVisit("u-us", 2L, 1, true);
		insertVisit("u-unknown", null, 2, true);
		insertVisit("u-action", 1L, 1, false);
	}

	@Test
	void shouldAggregatePvUvAndQualityAtEachGeoLevel() {
		GlobalVisitorMapVO result = provider.getGlobalVisitorMap(VisitorMapWindow.resolve("all", "Asia/Shanghai", null, null));

		assertEquals("database", result.getMeta().getDataMode());
		assertEquals(4, result.getSummary().getPv());
		assertEquals(3, result.getSummary().getUv());
		assertEquals(2, result.getSummary().getCountryCount());
		assertEquals(1, result.getSummary().getCityCount());
		assertEquals(2, result.getMeta().getQuality().getResolvedPv());
		assertEquals(1, result.getMeta().getQuality().getCountryOnlyPv());
		assertEquals(1, result.getMeta().getQuality().getUnknownPv());
		assertEquals(2, result.getCountries().size());
		assertEquals(1, result.getCities().size());
		assertEquals("深圳市", result.getCities().get(0).getCityNameZh());
	}

	private void insertVisit(String uuid, Long geoId, int geoStatus, boolean pageView) {
		jdbcTemplate.update("INSERT INTO visit_log (uuid, is_page_view, geo_id, geo_status, create_time) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
				uuid, pageView, geoId, geoStatus);
	}
}
