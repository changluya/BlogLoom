package com.changlu.blogloom.module.visitormap.provider;

import com.changlu.blogloom.module.visitormap.domain.GlobalVisitorMapVO;
import com.changlu.blogloom.module.visitormap.domain.VisitorMapWindow;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/**
 * 从 classpath 加载静态全球访客数据。该实现用于首期联调和自动化测试。
 */
@Component
public class MockGlobalVisitorMapProvider implements GlobalVisitorMapProvider {
	static final String DATA_PATH = "visitor-map/global-visitor-map.mock.json";

	private final ObjectMapper objectMapper;
	private GlobalVisitorMapVO dataset;

	public MockGlobalVisitorMapProvider(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@PostConstruct
	public void load() {
		ClassPathResource resource = new ClassPathResource(DATA_PATH);
		try (InputStream inputStream = resource.getInputStream()) {
			dataset = objectMapper.readValue(inputStream, GlobalVisitorMapVO.class);
			prepareAndValidate(dataset);
		} catch (IOException e) {
			throw new IllegalStateException("加载全球访客地图静态数据失败: " + DATA_PATH, e);
		}
	}

	@Override
	public GlobalVisitorMapVO getGlobalVisitorMap(VisitorMapWindow window) {
		GlobalVisitorMapVO response = objectMapper.convertValue(dataset, GlobalVisitorMapVO.class);
		response.getMeta().setPeriodType(window.getPeriodType());
		response.getMeta().setPeriodStart(window.getStartZoned() == null
				? null : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(window.getStartZoned()));
		response.getMeta().setPeriodEnd(window.getEndZoned() == null
				? null : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(window.getEndZoned()));
		response.getMeta().setTimezone(window.getZoneId().getId());
		return response;
	}

	private void prepareAndValidate(GlobalVisitorMapVO data) {
		if (data == null || data.getMeta() == null || data.getSummary() == null) {
			throw new IllegalStateException("全球访客地图静态数据缺少 meta 或 summary");
		}
		if (data.getCountries() == null || data.getCities() == null) {
			throw new IllegalStateException("全球访客地图静态数据缺少国家或城市集合");
		}
		data.getMeta().setDataMode("mock");
		Set<String> countryCodes = new HashSet<>();
		long countryPv = 0;
		for (GlobalVisitorMapVO.Country country : data.getCountries()) {
			if (country.getCountryCode() == null || country.getCountryCode().length() != 2) {
				throw new IllegalStateException("全球访客地图存在非法国家码");
			}
			countryCodes.add(country.getCountryCode());
			countryPv += country.getPv();
		}
		long cityPv = 0;
		int sequence = 1;
		for (GlobalVisitorMapVO.City city : data.getCities()) {
			if (!countryCodes.contains(city.getCountryCode())) {
				throw new IllegalStateException("城市引用了不存在的国家: " + city.getCountryCode());
			}
			if (city.getLat() < -90 || city.getLat() > 90 || city.getLng() < -180 || city.getLng() > 180) {
				throw new IllegalStateException("城市经纬度越界: " + city.getCityNameZh());
			}
			if (city.getGeoKey() == null || city.getGeoKey().trim().isEmpty()) {
				city.setGeoKey(city.getCountryCode() + "|CITY|" + String.format("%03d", sequence));
			}
			cityPv += city.getPv();
			sequence++;
		}
		GlobalVisitorMapVO.Quality quality = data.getMeta().getQuality();
		quality.setResolvedPv(cityPv);
		quality.setCountryOnlyPv(Math.max(0, countryPv - cityPv));
		quality.setUnknownPv(Math.max(0, data.getSummary().getPv() - countryPv));
		data.getSummary().setCountryCount(data.getCountries().size());
		data.getSummary().setCityCount(data.getCities().size());
	}
}
