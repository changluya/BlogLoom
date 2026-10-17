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
		ZoneId zoneId = window.getZoneId();
		Instant generatedAt = Instant.now();
		Date start = window.getStart() == null ? null : Date.from(window.getStart());
		Date end = window.getEnd() == null ? null : Date.from(window.getEnd());

		GlobalVisitorMapVO result = new GlobalVisitorMapVO();
		GlobalVisitorMapVO.Summary summary = mapper.getSummary(start, end);
		result.setSummary(summary == null ? new GlobalVisitorMapVO.Summary() : summary);
		result.setCountries(mapper.getCountries(start, end));
		result.setCities(mapper.getCities(start, end));

		GlobalVisitorMapVO.Meta meta = result.getMeta();
		meta.setSchemaVersion("1.0");
		meta.setDataMode("database");
		meta.setGeneratedAt(format(generatedAt, zoneId));
		meta.setPeriodType(window.getPeriodType());
		meta.setPeriodStart(window.getStartZoned() == null ? null : format(window.getStartZoned()));
		meta.setPeriodEnd(window.getEndZoned() == null ? null : format(window.getEndZoned()));
		meta.setTimezone(zoneId.getId());
		meta.setCoordinateSystem("WGS84");
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
