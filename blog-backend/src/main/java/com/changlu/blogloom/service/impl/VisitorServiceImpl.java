package com.changlu.blogloom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.changlu.blogloom.constant.CacheKeyConstants;
import com.changlu.blogloom.entity.Visitor;
import com.changlu.blogloom.exception.PersistenceException;
import com.changlu.blogloom.mapper.VisitorMapper;
import com.changlu.blogloom.model.dto.UserAgentDTO;
import com.changlu.blogloom.model.dto.VisitLogUuidTime;
import com.changlu.blogloom.service.BlogCacheService;
import com.changlu.blogloom.service.VisitorService;
import com.changlu.blogloom.util.IpAddressUtils;
import com.changlu.blogloom.util.UserAgentUtils;

import java.util.List;

/**
 * @Description: 访客统计业务层实现
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Service
public class VisitorServiceImpl implements VisitorService {
	@Autowired
	VisitorMapper visitorMapper;
	@Autowired
	BlogCacheService cacheService;
	@Autowired
	UserAgentUtils userAgentUtils;

	@Override
	public List<Visitor> getVisitorListByDate(String startDate, String endDate) {
		return visitorMapper.getVisitorListByDate(startDate, endDate);
	}

	@Override
	public List<String> getNewVisitorIpSourceByYesterday() {
		return visitorMapper.getNewVisitorIpSourceByYesterday();
	}

	@Override
	public boolean hasUUID(String uuid) {
		return visitorMapper.hasUUID(uuid) != 0;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void saveVisitor(Visitor visitor) {
		String ipSource = IpAddressUtils.getCityInfo(visitor.getIp());
		UserAgentDTO userAgentDTO = userAgentUtils.parseOsAndBrowser(visitor.getUserAgent());
		visitor.setIpSource(ipSource);
		visitor.setOs(userAgentDTO.getOs());
		visitor.setBrowser(userAgentDTO.getBrowser());
		if (visitorMapper.saveVisitor(visitor) != 1) {
			throw new PersistenceException("访客添加失败");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updatePVAndLastTimeByUUID(VisitLogUuidTime dto) {
		visitorMapper.updatePVAndLastTimeByUUID(dto);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void deleteVisitor(Long id, String uuid) {
		//删除缓存中该访客的uuid
		cacheService.deleteValueBySet(CacheKeyConstants.IDENTIFICATION_SET, uuid);
		if (visitorMapper.deleteVisitorById(id) != 1) {
			throw new PersistenceException("删除访客失败");
		}
	}
}
