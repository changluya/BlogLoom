package top.naccl.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.naccl.entity.CityVisitor;
import top.naccl.mapper.CityVisitorMapper;
import top.naccl.service.CityVisitorService;

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
