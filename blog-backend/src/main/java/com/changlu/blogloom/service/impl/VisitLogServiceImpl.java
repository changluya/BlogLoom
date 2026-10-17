package com.changlu.blogloom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.changlu.blogloom.entity.VisitLog;
import com.changlu.blogloom.exception.PersistenceException;
import com.changlu.blogloom.mapper.VisitLogMapper;
import com.changlu.blogloom.mapper.VisitorGeoMapper;
import com.changlu.blogloom.model.dto.UserAgentDTO;
import com.changlu.blogloom.model.dto.VisitLogUuidTime;
import com.changlu.blogloom.service.VisitLogService;
import com.changlu.blogloom.util.IpAddressUtils;
import com.changlu.blogloom.util.UserAgentUtils;

import java.util.List;
import java.util.Locale;

/**
 * @Description: 访问日志业务层实现
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Service
public class VisitLogServiceImpl implements VisitLogService {
	@Autowired
	VisitLogMapper visitLogMapper;
	@Autowired
	UserAgentUtils userAgentUtils;
	@Autowired
	IpAddressUtils ipAddressUtils;
	@Autowired
	VisitorGeoMapper visitorGeoMapper;

	@Override
	public List<VisitLog> getVisitLogListByUUIDAndDate(String uuid, String startDate, String endDate) {
		return visitLogMapper.getVisitLogListByUUIDAndDate(uuid, startDate, endDate);
	}

	@Override
	public List<VisitLogUuidTime> getUUIDAndCreateTimeByYesterday() {
		return visitLogMapper.getUUIDAndCreateTimeByYesterday();
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void saveVisitLog(VisitLog log) {
		String ipSource = ipAddressUtils.getCityInfo(log.getIp());
		UserAgentDTO userAgentDTO = userAgentUtils.parseOsAndBrowser(log.getUserAgent());
		log.setIpSource(ipSource);
		resolveGeo(log, ipSource);
		log.setOs(userAgentDTO.getOs());
		log.setBrowser(userAgentDTO.getBrowser());
		if (visitLogMapper.saveVisitLog(log) != 1) {
			throw new PersistenceException("日志添加失败");
		}
	}

	private void resolveGeo(VisitLog log, String ipSource) {
		if (isPrivateIp(log.getIp())) {
			log.setGeoStatus(2);
			return;
		}
		if (ipSource == null || ipSource.trim().isEmpty()) {
			log.setGeoStatus(0);
			return;
		}
		String[] fields = ipSource.split("\\|", -1);
		String city = fields.length > 2 ? fields[2].trim() : "";
		String countryCode = fields.length > 4 ? fields[4].trim().toUpperCase(Locale.ROOT) : "";
		Long geoId = null;
		if (!city.isEmpty()) {
			geoId = visitorGeoMapper.findCityGeoId(city, countryCode);
		}
		if (geoId == null && countryCode.length() == 2) {
			geoId = visitorGeoMapper.findCountryGeoId(countryCode);
		}
		log.setGeoId(geoId);
		log.setGeoStatus(geoId == null ? 0 : 1);
	}

	private boolean isPrivateIp(String ip) {
		if (ip == null) return false;
		String normalized = ip.trim().toLowerCase(Locale.ROOT);
		if (normalized.equals("::1") || normalized.startsWith("fc") || normalized.startsWith("fd") || normalized.startsWith("fe80:")) {
			return true;
		}
		if (normalized.startsWith("10.") || normalized.startsWith("127.") || normalized.startsWith("192.168.") || normalized.startsWith("169.254.")) {
			return true;
		}
		if (normalized.startsWith("172.")) {
			String[] parts = normalized.split("\\.");
			if (parts.length > 1) {
				try {
					int second = Integer.parseInt(parts[1]);
					return second >= 16 && second <= 31;
				} catch (NumberFormatException ignored) {
					return false;
				}
			}
		}
		return false;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void deleteVisitLogById(Long id) {
		if (visitLogMapper.deleteVisitLogById(id) != 1) {
			throw new PersistenceException("删除日志失败");
		}
	}
}
