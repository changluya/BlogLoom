package com.changlu.blogloom.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 验证海外同名城市按省州名消歧：省州一致的行优先，省州缺失时仍可按城市 + 国家回退。
 */
@SpringBootTest
class VisitorGeoMapperRegionTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private VisitorGeoMapper visitorGeoMapper;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM visitor_geo");
		insertCity(1L, "US|CITY|US-001", "US", "Oregon", "波特兰", "Portland");
		insertCity(2L, "US|CITY|US-002", "US", "Maine", "波特兰", "Portland");
		insertCity(3L, "US|CITY|US-003", "US", "California", "旧金山", "San Francisco");
	}

	@Test
	void shouldPreferRegionWhenCityNameIsDuplicated() {
		assertEquals(1L, visitorGeoMapper.findCityGeoId("Portland", "US", "Oregon"));
		assertEquals(2L, visitorGeoMapper.findCityGeoId("Portland", "US", "Maine"));
	}

	@Test
	void shouldFallBackToCityAndCountryWhenRegionIsMissing() {
		assertEquals(1L, visitorGeoMapper.findCityGeoId("Portland", "US", null));
		assertEquals(1L, visitorGeoMapper.findCityGeoId("Portland", "US", ""));
	}

	@Test
	void shouldFallBackToCityAndCountryWhenRegionDoesNotMatch() {
		assertEquals(1L, visitorGeoMapper.findCityGeoId("Portland", "US", "Texas"));
	}

	@Test
	void shouldReturnNullForUnknownCity() {
		assertNull(visitorGeoMapper.findCityGeoId("Unknown", "US", "California"));
	}

	private void insertCity(long id, String geoKey, String countryCode, String regionName, String cityNameZh, String cityNameEn) {
		jdbcTemplate.update("INSERT INTO visitor_geo (id, geo_key, geo_level, country_code, region_name, city_name_zh, city_name_en, longitude, latitude) "
				+ "VALUES (?, ?, 2, ?, ?, ?, ?, 0, 0)", id, geoKey, countryCode, regionName, cityNameZh, cityNameEn);
	}
}
