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
 * <p>
 * 一次 today 视图的返回示例如下：
 * <pre>{@code
 * {
 *   "meta": {
 *     "schemaVersion": "1.0",
 *     "dataMode": "database",
 *     "generatedAt": "2026-10-18T10:00:00+08:00",
 *     "periodType": "today",
 *     "periodStart": "2026-10-18T00:00:00+08:00",
 *     "periodEnd": "2026-10-18T10:00:00+08:00",
 *     "timezone": "Asia/Shanghai",
 *     "coordinateSystem": "WGS84",
 *     "quality": {"resolvedPv": 24, "countryOnlyPv": 4, "unknownPv": 0}
 *   },
 *   "summary": {"uv": 17, "pv": 28, "countryCount": 8, "cityCount": 12},
 *   "countries": [
 *     {"countryCode": "US", "uv": 1, "pv": 3},
 *     {"countryCode": "CN", "uv": 9, "pv": 14}
 *   ],
 *   "cities": [
 *     {
 *       "geoKey": "US|CITY|US-012",
 *       "cityNameZh": "旧金山",
 *       "cityNameEn": "San Francisco",
 *       "regionName": "California",
 *       "countryCode": "US",
 *       "lat": 37.7749,
 *       "lng": -122.4194,
 *       "uv": 1,
 *       "pv": 3
 *     }
 *   ]
 * }
 * }</pre>
 * 页面上的每个数字都能用一条 SQL 复算：resolvedPv + countryOnlyPv + unknownPv = summary.pv。
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GlobalVisitorMapVO {
	/** 元信息：数据模式、统计周期、时区、坐标系与质量指标 */
	private Meta meta = new Meta();
	/** 全局汇总：周期内的 UV / PV 与国家数、城市数 */
	private Summary summary = new Summary();
	/** 国家维度热度，用于国家板块着色与国家榜单 */
	private List<Country> countries = new ArrayList<>();
	/** 城市维度热度，带坐标，用于光点位置与城市榜单 */
	private List<City> cities = new ArrayList<>();

	@Getter
	@Setter
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Meta {
		/** 契约版本号，字段变更时递增 */
		private String schemaVersion;
		/** 数据来源：database 为真实查询，mock 为开发环境假数据 */
		private String dataMode;
		/** 本次聚合的生成时间 */
		private String generatedAt;
		/** 统计周期：all / today / yesterday / last_7d / custom */
		private String periodType;
		/** 周期起点，all 时为 null，表示不设下界 */
		private String periodStart;
		/** 周期终点，all 时为 null，表示不设上界 */
		private String periodEnd;
		/** 计算自然日边界所用的时区，如 Asia/Shanghai */
		private String timezone;
		/** 坐标参考系，当前固定 WGS84 */
		private String coordinateSystem;
		/** 数据质量：解释地图为什么有空白 */
		private Quality quality = new Quality();
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Quality {
		/** 命中城市级（geo_level = 2 且 geo_status = 1）的 PV */
		private long resolvedPv;
		/** 仅定位到国家（geo_level = 1 且 geo_status = 1）的 PV */
		private long countryOnlyPv;
		/** 未知 / 内网 / 无效 IP 的 PV */
		private long unknownPv;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Summary {
		/** 全局 UV：周期内去重后的访客数，不等于各国 UV 之和 */
		private long uv;
		/** 全局 PV：周期内 is_page_view = 1 的行数 */
		private long pv;
		/** 出现过的国家数 */
		private int countryCount;
		/** 出现过的城市数（仅 geo_level = 2） */
		private int cityCount;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Country {
		/** ISO 3166-1 Alpha-2 国家码，如 US、CN */
		private String countryCode;
		/** 该国 UV：同一访客在该国只算 1 次 */
		private long uv;
		/** 该国 PV */
		private long pv;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class City {
		/** 稳定地域键，如 US|CITY|US-012，不随展示名变化 */
		private String geoKey;
		/** 城市中文名，兼容旧字段 city */
		@JsonAlias("city")
		private String cityNameZh;
		/** 城市英文名，兼容旧字段 cityEn */
		@JsonAlias("cityEn")
		private String cityNameEn;
		/** 省 / 州名，用于区分同名城市与中国省份着色 */
		private String regionName;
		/** 所属国家码，参与同名城市消歧 */
		private String countryCode;
		/** 纬度（WGS84），光点纵坐标来源 */
		private double lat;
		/** 经度（WGS84），光点横坐标来源 */
		private double lng;
		/** 城市 UV：跨城市访客在每个城市各算 1 次 */
		private long uv;
		/** 城市 PV */
		private long pv;
	}
}
