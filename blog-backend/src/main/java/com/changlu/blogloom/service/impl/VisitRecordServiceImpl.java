package com.changlu.blogloom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.changlu.blogloom.entity.VisitRecord;
import com.changlu.blogloom.mapper.VisitRecordMapper;
import com.changlu.blogloom.service.VisitRecordService;

/**
 * @Description: 访问记录业务层实现
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Service
public class VisitRecordServiceImpl implements VisitRecordService {
	@Autowired
	VisitRecordMapper visitRecordMapper;

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void saveVisitRecord(VisitRecord visitRecord) {
		visitRecordMapper.saveVisitRecord(visitRecord);
	}
}
