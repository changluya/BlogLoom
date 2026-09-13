package com.changlu.blogloom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.changlu.blogloom.entity.LoginLog;
import com.changlu.blogloom.exception.PersistenceException;
import com.changlu.blogloom.mapper.LoginLogMapper;
import com.changlu.blogloom.model.dto.UserAgentDTO;
import com.changlu.blogloom.service.LoginLogService;
import com.changlu.blogloom.util.IpAddressUtils;
import com.changlu.blogloom.util.UserAgentUtils;

import java.util.List;

/**
 * @Description: 登录日志业务层实现
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Service
public class LoginLogServiceImpl implements LoginLogService {
	@Autowired
	LoginLogMapper loginLogMapper;
	@Autowired
	UserAgentUtils userAgentUtils;

	@Override
	public List<LoginLog> getLoginLogListByDate(String startDate, String endDate) {
		return loginLogMapper.getLoginLogListByDate(startDate, endDate);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void saveLoginLog(LoginLog log) {
		String ipSource = IpAddressUtils.getCityInfo(log.getIp());
		UserAgentDTO userAgentDTO = userAgentUtils.parseOsAndBrowser(log.getUserAgent());
		log.setIpSource(ipSource);
		log.setOs(userAgentDTO.getOs());
		log.setBrowser(userAgentDTO.getBrowser());
		if (loginLogMapper.saveLoginLog(log) != 1) {
			throw new PersistenceException("日志添加失败");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void deleteLoginLogById(Long id) {
		if (loginLogMapper.deleteLoginLogById(id) != 1) {
			throw new PersistenceException("删除日志失败");
		}
	}
}
