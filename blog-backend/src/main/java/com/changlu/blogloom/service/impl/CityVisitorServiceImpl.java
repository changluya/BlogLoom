package com.changlu.blogloom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.changlu.blogloom.entity.CityVisitor;
import com.changlu.blogloom.mapper.CityVisitorMapper;
import com.changlu.blogloom.service.CityVisitorService;

/**
 * @Description: 城市访客数量统计业务层实现
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Service
public class CityVisitorServiceImpl implements CityVisitorService {
	@Autowired
	CityVisitorMapper cityVisitorMapper;

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void saveCityVisitor(CityVisitor cityVisitor) {
		cityVisitorMapper.saveCityVisitor(cityVisitor);
	}
}
