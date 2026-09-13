package com.changlu.blogloom.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import com.changlu.blogloom.entity.LoginLog;

import java.util.List;

/**
 * @Description: 登录日志持久层接口
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Mapper
@Repository
public interface LoginLogMapper {
	List<LoginLog> getLoginLogListByDate(String startDate, String endDate);

	int saveLoginLog(LoginLog log);

	int deleteLoginLogById(Long id);
}
