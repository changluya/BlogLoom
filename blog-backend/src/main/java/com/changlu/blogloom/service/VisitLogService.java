package com.changlu.blogloom.service;

import org.springframework.scheduling.annotation.Async;
import com.changlu.blogloom.entity.VisitLog;
import com.changlu.blogloom.model.dto.VisitLogUuidTime;

import java.util.List;

public interface VisitLogService {
	List<VisitLog> getVisitLogListByUUIDAndDate(String uuid, String startDate, String endDate);

	List<VisitLogUuidTime> getUUIDAndCreateTimeByYesterday();

	@Async
	void saveVisitLog(VisitLog log);

	void deleteVisitLogById(Long id);
}
