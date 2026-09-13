package com.changlu.blogloom.service;

import org.springframework.scheduling.annotation.Async;
import com.changlu.blogloom.entity.ExceptionLog;

import java.util.List;

public interface ExceptionLogService {
	List<ExceptionLog> getExceptionLogListByDate(String startDate, String endDate);

	@Async
	void saveExceptionLog(ExceptionLog log);

	void deleteExceptionLogById(Long id);
}
