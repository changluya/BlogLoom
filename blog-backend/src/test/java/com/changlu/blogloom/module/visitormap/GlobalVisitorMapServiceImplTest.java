package com.changlu.blogloom.module.visitormap;

import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.module.visitormap.domain.GlobalVisitorMapVO;
import com.changlu.blogloom.module.visitormap.domain.VisitorMapWindow;
import com.changlu.blogloom.module.visitormap.provider.GlobalVisitorMapProvider;
import com.changlu.blogloom.module.visitormap.service.impl.GlobalVisitorMapServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GlobalVisitorMapServiceImplTest {
	@Test
	void shouldDefaultToAllTimeAndShanghaiTimezone() {
		CapturingProvider provider = new CapturingProvider();
		GlobalVisitorMapServiceImpl service = new GlobalVisitorMapServiceImpl(provider);

		service.getGlobalVisitorMap(null, null, null, null);

		assertEquals(VisitorMapWindow.ALL, provider.window.getPeriodType());
		assertEquals("Asia/Shanghai", provider.window.getZoneId().getId());
		assertNull(provider.window.getStart());
		assertNull(provider.window.getEnd());
	}

	@Test
	void shouldRejectUnsupportedPeriodAndInvalidTimezone() {
		GlobalVisitorMapServiceImpl service = new GlobalVisitorMapServiceImpl(new CapturingProvider());

		assertThrows(BadRequestException.class, () -> service.getGlobalVisitorMap("month", "Asia/Shanghai", null, null));
		assertThrows(BadRequestException.class, () -> service.getGlobalVisitorMap("all", "Mars/Base", null, null));
	}

	@Test
	void shouldResolveNaturalDayWindowsInTimezone() {
		Instant now = Instant.parse("2026-10-17T10:00:00Z");
		ZoneId shanghai = ZoneId.of("Asia/Shanghai");

		VisitorMapWindow today = VisitorMapWindow.resolve("today", "Asia/Shanghai", null, null, now);
		assertEquals(LocalDate.of(2026, 10, 17).atStartOfDay(shanghai).toInstant(), today.getStart());
		assertEquals(now, today.getEnd());

		VisitorMapWindow yesterday = VisitorMapWindow.resolve("yesterday", "Asia/Shanghai", null, null, now);
		assertEquals(LocalDate.of(2026, 10, 16).atStartOfDay(shanghai).toInstant(), yesterday.getStart());
		assertEquals(LocalDate.of(2026, 10, 17).atStartOfDay(shanghai).toInstant(), yesterday.getEnd());

		VisitorMapWindow last7d = VisitorMapWindow.resolve("last_7d", "Asia/Shanghai", null, null, now);
		assertEquals(LocalDate.of(2026, 10, 11).atStartOfDay(shanghai).toInstant(), last7d.getStart());
		assertEquals(now, last7d.getEnd());
	}

	@Test
	void shouldResolveCustomRangeAsInclusiveDays() {
		ZoneId shanghai = ZoneId.of("Asia/Shanghai");
		VisitorMapWindow window = VisitorMapWindow.resolve("custom", "Asia/Shanghai", "2026-10-01", "2026-10-03");

		assertEquals(LocalDate.of(2026, 10, 1).atStartOfDay(shanghai).toInstant(), window.getStart());
		assertEquals(LocalDate.of(2026, 10, 4).atStartOfDay(shanghai).toInstant(), window.getEnd());
	}

	@Test
	void shouldRejectInvalidCustomRange() {
		assertThrows(BadRequestException.class, () -> VisitorMapWindow.resolve("custom", "Asia/Shanghai", null, "2026-10-03"));
		assertThrows(BadRequestException.class, () -> VisitorMapWindow.resolve("custom", "Asia/Shanghai", "2026-10-03", "2026-10-01"));
		assertThrows(BadRequestException.class, () -> VisitorMapWindow.resolve("custom", "Asia/Shanghai", "2026/10/01", "2026/10/03"));
		assertThrows(BadRequestException.class, () -> VisitorMapWindow.resolve("custom", "Asia/Shanghai", "2024-01-01", "2026-10-03"));
	}

	private static class CapturingProvider implements GlobalVisitorMapProvider {
		private VisitorMapWindow window;

		@Override
		public GlobalVisitorMapVO getGlobalVisitorMap(VisitorMapWindow window) {
			this.window = window;
			return new GlobalVisitorMapVO();
		}
	}
}
