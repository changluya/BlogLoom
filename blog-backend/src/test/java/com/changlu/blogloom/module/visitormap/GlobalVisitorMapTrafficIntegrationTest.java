package com.changlu.blogloom.module.visitormap;

import com.changlu.blogloom.entity.VisitLog;
import com.changlu.blogloom.module.visitormap.domain.GlobalVisitorMapVO;
import com.changlu.blogloom.module.visitormap.domain.VisitorMapWindow;
import com.changlu.blogloom.module.visitormap.provider.DatabaseGlobalVisitorMapProvider;
import com.changlu.blogloom.service.VisitLogService;
import com.changlu.blogloom.util.IpAddressUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * 模拟国内、国外访客请求，验证 IP 定位、访问日志落库及全球地图 PV/UV 聚合全链路。
 */
@SpringBootTest
class GlobalVisitorMapTrafficIntegrationTest {
	private static final String USER_AGENT =
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/120 Safari/537.36";

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private VisitLogService visitLogService;
	@Autowired
	private DatabaseGlobalVisitorMapProvider provider;
	@MockBean
	private IpAddressUtils ipAddressUtils;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM visit_log");
		jdbcTemplate.update("DELETE FROM visitor_geo");
		insertCity(101, "CN|CITY|SHENZHEN", "CN", "深圳市", "Shenzhen", 114.064552, 22.548457);
		insertCity(102, "CN|CITY|BEIJING", "CN", "北京市", "Beijing", 116.407526, 39.904030);
		insertCity(201, "US|CITY|MOUNTAIN_VIEW", "US", "山景城", "Mountain View", -122.084057, 37.421999);
		insertCity(301, "GB|CITY|LONDON", "GB", "伦敦", "London", -0.127758, 51.507351);

		when(ipAddressUtils.getCityInfo("1.0.1.1")).thenReturn("中国|广东省|深圳市|电信|CN");
		when(ipAddressUtils.getCityInfo("1.2.4.8")).thenReturn("中国|北京市|北京市|联通|CN");
		when(ipAddressUtils.getCityInfo("8.8.8.8")).thenReturn("United States|California|Mountain View|Google|US");
		when(ipAddressUtils.getCityInfo("81.2.69.142")).thenReturn("United Kingdom|England|London||GB");
	}

	@Test
	void shouldPersistSimulatedDomesticAndOverseasRequestsAndAggregatePvUv() {
		// 深圳：同一个访客访问两次，PV=2、UV=1。
		simulatePageRequest("visitor-cn-shenzhen", "1.0.1.1", "/api/blogs");
		simulatePageRequest("visitor-cn-shenzhen", "1.0.1.1", "/api/blog/1");
		// 北京：另一个访客访问一次。中国合计 PV=3、UV=2。
		simulatePageRequest("visitor-cn-beijing", "1.2.4.8", "/api/archives");
		// 美国：同一个访客访问三次，PV=3、UV=1。
		simulatePageRequest("visitor-us", "8.8.8.8", "/api/blogs");
		simulatePageRequest("visitor-us", "8.8.8.8", "/api/blog/1");
		simulatePageRequest("visitor-us", "8.8.8.8", "/api/about");
		// 英国：一个访客访问一次，PV=1、UV=1。
		simulatePageRequest("visitor-gb", "81.2.69.142", "/api/blogs");
		awaitVisitLogPersisted(7);

		assertEquals(7, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM visit_log", Integer.class));
		assertEquals(4, jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT uuid) FROM visit_log", Integer.class));
		assertEquals(7, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM visit_log WHERE geo_status = 1 AND geo_id IS NOT NULL", Integer.class));
		assertEquals(2, countPersistedRequests("1.0.1.1", 101));
		assertEquals(1, countPersistedRequests("1.2.4.8", 102));
		assertEquals(3, countPersistedRequests("8.8.8.8", 201));
		assertEquals(1, countPersistedRequests("81.2.69.142", 301));

		GlobalVisitorMapVO result = provider.getGlobalVisitorMap(VisitorMapWindow.resolve("all", "Asia/Shanghai", null, null));
		assertEquals(7, result.getSummary().getPv());
		assertEquals(4, result.getSummary().getUv());
		assertEquals(3, result.getSummary().getCountryCount());
		assertEquals(4, result.getSummary().getCityCount());
		assertEquals(7, result.getMeta().getQuality().getResolvedPv());

		Map<String, GlobalVisitorMapVO.Country> countries = result.getCountries().stream()
				.collect(Collectors.toMap(GlobalVisitorMapVO.Country::getCountryCode, Function.identity()));
		assertPvUv(countries.get("CN"), 3, 2);
		assertPvUv(countries.get("US"), 3, 1);
		assertPvUv(countries.get("GB"), 1, 1);

		Map<String, GlobalVisitorMapVO.City> cities = result.getCities().stream()
				.collect(Collectors.toMap(GlobalVisitorMapVO.City::getCityNameEn, Function.identity()));
		assertPvUv(cities.get("Shenzhen"), 2, 1);
		assertPvUv(cities.get("Beijing"), 1, 1);
		assertPvUv(cities.get("Mountain View"), 3, 1);
		assertPvUv(cities.get("London"), 1, 1);
	}

	private void simulatePageRequest(String uuid, String ip, String uri) {
		VisitLog log = new VisitLog(uuid, uri, "GET", "访问页面", uri, "模拟访客请求", ip, 10, USER_AGENT);
		log.setPageView(true);
		log.setParam("{}");
		visitLogService.saveVisitLog(log);
	}

	/**
	 * 访问日志通过 @Async 异步落库，断言前需要等待写入完成，避免用例偶发失败。
	 */
	private void awaitVisitLogPersisted(int expected) {
		long deadline = System.currentTimeMillis() + 5000;
		while (System.currentTimeMillis() < deadline) {
			Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM visit_log", Integer.class);
			if (count != null && count >= expected) return;
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	private void insertCity(long id, String geoKey, String countryCode, String cityNameZh,
	                        String cityNameEn, double longitude, double latitude) {
		jdbcTemplate.update("INSERT INTO visitor_geo (id, geo_key, geo_level, country_code, city_name_zh, city_name_en, longitude, latitude) VALUES (?, ?, 2, ?, ?, ?, ?, ?)",
				id, geoKey, countryCode, cityNameZh, cityNameEn, longitude, latitude);
	}

	private int countPersistedRequests(String ip, long geoId) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM visit_log WHERE ip = ? AND geo_id = ? AND geo_status = 1",
				Integer.class, ip, geoId);
	}

	private void assertPvUv(GlobalVisitorMapVO.Country item, long pv, long uv) {
		assertEquals(pv, item.getPv());
		assertEquals(uv, item.getUv());
	}

	private void assertPvUv(GlobalVisitorMapVO.City item, long pv, long uv) {
		assertEquals(pv, item.getPv());
		assertEquals(uv, item.getUv());
	}
}
