package com.changlu.blogloom.task;

import com.changlu.blogloom.constant.CacheKeyConstants;
import com.changlu.blogloom.entity.VisitRecord;
import com.changlu.blogloom.model.dto.VisitLogUuidTime;
import com.changlu.blogloom.service.BlogCacheService;
import com.changlu.blogloom.service.CityVisitorService;
import com.changlu.blogloom.service.VisitLogService;
import com.changlu.blogloom.service.VisitRecordService;
import com.changlu.blogloom.service.VisitorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VisitorSyncScheduleTaskTest {
	private VisitorSyncScheduleTask task;
	private BlogCacheService cacheService;
	private VisitLogService visitLogService;
	private VisitorService visitorService;
	private VisitRecordService visitRecordService;
	private CityVisitorService cityVisitorService;

	@BeforeEach
	void setUp() {
		cacheService = Mockito.mock(BlogCacheService.class);
		visitLogService = Mockito.mock(VisitLogService.class);
		visitorService = Mockito.mock(VisitorService.class);
		visitRecordService = Mockito.mock(VisitRecordService.class);
		cityVisitorService = Mockito.mock(CityVisitorService.class);

		task = new VisitorSyncScheduleTask();
		ReflectionTestUtils.setField(task, "cacheService", cacheService);
		ReflectionTestUtils.setField(task, "visitLogService", visitLogService);
		ReflectionTestUtils.setField(task, "visitorService", visitorService);
		ReflectionTestUtils.setField(task, "visitRecordService", visitRecordService);
		ReflectionTestUtils.setField(task, "cityVisitorService", cityVisitorService);
	}

	@Test
	void shouldClearIdentificationSetAndRecordPvUv() {
		Date now = new Date();
		when(visitLogService.getUUIDAndCreateTimeByYesterday()).thenReturn(Arrays.asList(
				new VisitLogUuidTime("u1", now, null),
				new VisitLogUuidTime("u1", now, null),
				new VisitLogUuidTime("u2", now, null)));
		when(visitorService.getNewVisitorIpSourceByYesterday()).thenReturn(Arrays.asList(
				"中国|广东省|深圳市|电信"));

		task.syncVisitInfoToDatabase();

		// 原 Redis Set 清空逻辑改为缓存清理
		verify(cacheService).deleteCacheByKey(CacheKeyConstants.IDENTIFICATION_SET);
		// 昨日 PV=3、UV=2
		ArgumentCaptor<VisitRecord> captor = ArgumentCaptor.forClass(VisitRecord.class);
		verify(visitRecordService).saveVisitRecord(captor.capture());
		assertEquals(3, captor.getValue().getPv());
		assertEquals(2, captor.getValue().getUv());
		// 两位访客的 PV/最后访问时间都更新
		verify(visitorService, times(2)).updatePVAndLastTimeByUUID(Mockito.any());
		// 城市新增访客 UV 记录
		verify(cityVisitorService).saveCityVisitor(Mockito.argThat(cv -> "深圳市".equals(cv.getCity())));
	}
}
