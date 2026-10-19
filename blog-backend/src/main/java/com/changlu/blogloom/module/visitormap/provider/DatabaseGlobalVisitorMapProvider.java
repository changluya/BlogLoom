package com.changlu.blogloom.module.visitormap.provider;

import com.changlu.blogloom.module.visitormap.domain.GlobalVisitorMapVO;
import com.changlu.blogloom.module.visitormap.domain.VisitorMapWindow;
import com.changlu.blogloom.module.visitormap.mapper.GlobalVisitorMapMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Primary
@Component
public class DatabaseGlobalVisitorMapProvider implements GlobalVisitorMapProvider {
	private final GlobalVisitorMapMapper mapper;

	public DatabaseGlobalVisitorMapProvider(GlobalVisitorMapMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public GlobalVisitorMapVO getGlobalVisitorMap(VisitorMapWindow window) {
		// 1. 取出时间窗口：zoneId 决定自然日边界，start/end 为 null 表示该侧不设限（all 视图）
		ZoneId zoneId = window.getZoneId();
		Instant generatedAt = Instant.now();
		Date start = window.getStart() == null ? null : Date.from(window.getStart());
		Date end = window.getEnd() == null ? null : Date.from(window.getEnd());

		// 2. 三条主查询：汇总 / 国家 / 城市，过滤条件统一为 is_page_view = 1 + 时间窗
		GlobalVisitorMapVO result = new GlobalVisitorMapVO();
		GlobalVisitorMapVO.Summary summary = mapper.getSummary(start, end);
		// 空周期没有任何记录时，Mapper 可能返回 null，这里兜底成零值对象，避免前端拿到 null
		result.setSummary(summary == null ? new GlobalVisitorMapVO.Summary() : summary);
		result.setCountries(mapper.getCountries(start, end));
		result.setCities(mapper.getCities(start, end));

		// 3. 补齐元信息：让前端知道当前看的是哪个周期、哪个时区、哪个坐标系
		GlobalVisitorMapVO.Meta meta = result.getMeta();
		meta.setSchemaVersion("1.0");
		meta.setDataMode("database");
		meta.setGeneratedAt(format(generatedAt, zoneId));
		meta.setPeriodType(window.getPeriodType());
		meta.setPeriodStart(window.getStartZoned() == null ? null : format(window.getStartZoned()));
		meta.setPeriodEnd(window.getEndZoned() == null ? null : format(window.getEndZoned()));
		meta.setTimezone(zoneId.getId());
		meta.setCoordinateSystem("WGS84");

		// 4. 质量指标：三段之和等于 summary.pv，用于解释地图上的空白
		meta.getQuality().setResolvedPv(mapper.countResolvedCityPv(start, end));
		meta.getQuality().setCountryOnlyPv(mapper.countCountryOnlyPv(start, end));
		meta.getQuality().setUnknownPv(mapper.countUnknownPv(start, end));
		return result;
	}

	private String format(Instant instant, ZoneId zoneId) {
		return format(instant.atZone(zoneId));
	}

	private String format(ZonedDateTime zonedDateTime) {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(zonedDateTime);
	}
}
