package com.changlu.blogloom.module.visitormap.domain;

import com.changlu.blogloom.exception.BadRequestException;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 全球访客地图的统计时间窗口。
 * <p>
 * start / end 为 null 表示该侧不设边界，例如 periodType=all 时两侧都为 null。
 */
public class VisitorMapWindow {
	public static final String ALL = "all";
	public static final String TODAY = "today";
	public static final String YESTERDAY = "yesterday";
	public static final String LAST_7D = "last_7d";
	public static final String CUSTOM = "custom";
	public static final String ROLLING_24H = "rolling_24h";
	public static final String DEFAULT_PERIOD_TYPE = ALL;
	public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

	private static final Set<String> SUPPORTED = Collections.unmodifiableSet(new LinkedHashSet<>(
			Arrays.asList(ALL, TODAY, YESTERDAY, LAST_7D, CUSTOM, ROLLING_24H)));
	private static final long MAX_CUSTOM_DAYS = 366;

	private final String periodType;
	private final ZoneId zoneId;
	private final Instant start;
	private final Instant end;

	private VisitorMapWindow(String periodType, ZoneId zoneId, Instant start, Instant end) {
		this.periodType = periodType;
		this.zoneId = zoneId;
		this.start = start;
		this.end = end;
	}

	public static VisitorMapWindow resolve(String periodType, String timezone, String startDate, String endDate) {
		return resolve(periodType, timezone, startDate, endDate, Instant.now());
	}

	public static VisitorMapWindow resolve(String periodType, String timezone, String startDate, String endDate, Instant now) {
		String normalizedPeriod = isBlank(periodType) ? DEFAULT_PERIOD_TYPE : periodType.trim();
		if (!SUPPORTED.contains(normalizedPeriod)) {
			throw new BadRequestException("暂不支持该统计周期: " + normalizedPeriod);
		}
		ZoneId zoneId = resolveZoneId(timezone);
		LocalDate today = now.atZone(zoneId).toLocalDate();
		switch (normalizedPeriod) {
			case ALL:
				return new VisitorMapWindow(ALL, zoneId, null, null);
			case ROLLING_24H:
				return new VisitorMapWindow(ROLLING_24H, zoneId, now.minusSeconds(24 * 60 * 60), now);
			case TODAY:
				return new VisitorMapWindow(TODAY, zoneId, today.atStartOfDay(zoneId).toInstant(), now);
			case YESTERDAY:
				return new VisitorMapWindow(YESTERDAY, zoneId,
						today.minusDays(1).atStartOfDay(zoneId).toInstant(),
						today.atStartOfDay(zoneId).toInstant());
			case LAST_7D:
				return new VisitorMapWindow(LAST_7D, zoneId,
						today.minusDays(6).atStartOfDay(zoneId).toInstant(), now);
			case CUSTOM:
				LocalDate customStart = parseDate(startDate, "开始日期");
				LocalDate customEnd = parseDate(endDate, "结束日期");
				if (customStart.isAfter(customEnd)) {
					throw new BadRequestException("开始日期不能晚于结束日期");
				}
				if (ChronoUnit.DAYS.between(customStart, customEnd) + 1 > MAX_CUSTOM_DAYS) {
					throw new BadRequestException("自定义时间范围不能超过 " + MAX_CUSTOM_DAYS + " 天");
				}
				return new VisitorMapWindow(CUSTOM, zoneId,
						customStart.atStartOfDay(zoneId).toInstant(),
						customEnd.plusDays(1).atStartOfDay(zoneId).toInstant());
			default:
				throw new BadRequestException("暂不支持该统计周期: " + normalizedPeriod);
		}
	}

	private static ZoneId resolveZoneId(String timezone) {
		String normalized = isBlank(timezone) ? DEFAULT_TIMEZONE : timezone.trim();
		try {
			return ZoneId.of(normalized);
		} catch (DateTimeException e) {
			throw new BadRequestException("无效的时区: " + normalized);
		}
	}

	private static LocalDate parseDate(String value, String label) {
		if (isBlank(value)) {
			throw new BadRequestException("自定义时间范围需要提供" + label);
		}
		try {
			return LocalDate.parse(value.trim());
		} catch (DateTimeParseException e) {
			throw new BadRequestException(label + "格式应为 yyyy-MM-dd");
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	public String getPeriodType() {
		return periodType;
	}

	public ZoneId getZoneId() {
		return zoneId;
	}

	public Instant getStart() {
		return start;
	}

	public Instant getEnd() {
		return end;
	}

	public ZonedDateTime getStartZoned() {
		return start == null ? null : start.atZone(zoneId);
	}

	public ZonedDateTime getEndZoned() {
		return end == null ? null : end.atZone(zoneId);
	}
}
