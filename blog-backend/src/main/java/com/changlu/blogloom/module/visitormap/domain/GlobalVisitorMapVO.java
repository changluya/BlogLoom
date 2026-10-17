package com.changlu.blogloom.module.visitormap.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 全球访客地图统一响应模型。Mock 与数据库 Provider 必须返回相同结构。
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GlobalVisitorMapVO {
	private Meta meta = new Meta();
	private Summary summary = new Summary();
	private List<Country> countries = new ArrayList<>();
	private List<City> cities = new ArrayList<>();

	@Getter
	@Setter
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Meta {
		private String schemaVersion;
		private String dataMode;
		private String generatedAt;
		private String periodType;
		private String periodStart;
		private String periodEnd;
		private String timezone;
		private String coordinateSystem;
		private Quality quality = new Quality();
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Quality {
		private long resolvedPv;
		private long countryOnlyPv;
		private long unknownPv;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Summary {
		private long uv;
		private long pv;
		private int countryCount;
		private int cityCount;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Country {
		private String countryCode;
		private long uv;
		private long pv;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class City {
		private String geoKey;
		@JsonAlias("city")
		private String cityNameZh;
		@JsonAlias("cityEn")
		private String cityNameEn;
		private String regionName;
		private String countryCode;
		private double lat;
		private double lng;
		private long uv;
		private long pv;
	}
}
