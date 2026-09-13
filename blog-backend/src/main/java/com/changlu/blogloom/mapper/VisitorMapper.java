package com.changlu.blogloom.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import com.changlu.blogloom.entity.Visitor;
import com.changlu.blogloom.model.dto.VisitLogUuidTime;

import java.util.List;

/**
 * @Description: 访客统计持久层接口
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Mapper
@Repository
public interface VisitorMapper {
	List<Visitor> getVisitorListByDate(String startDate, String endDate);

	List<String> getNewVisitorIpSourceByYesterday();

	int hasUUID(String uuid);

	int saveVisitor(Visitor visitor);

	int updatePVAndLastTimeByUUID(VisitLogUuidTime dto);

	int deleteVisitorById(Long id);
}
