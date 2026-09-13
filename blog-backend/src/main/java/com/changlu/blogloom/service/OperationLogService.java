package com.changlu.blogloom.service;

import org.springframework.scheduling.annotation.Async;
import com.changlu.blogloom.entity.OperationLog;

import java.util.List;

public interface OperationLogService {
	List<OperationLog> getOperationLogListByDate(String startDate, String endDate);

	@Async
	void saveOperationLog(OperationLog log);

	void deleteOperationLogById(Long id);
}
