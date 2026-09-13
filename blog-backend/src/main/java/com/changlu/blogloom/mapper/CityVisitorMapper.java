package com.changlu.blogloom.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import com.changlu.blogloom.entity.CityVisitor;

import java.util.List;

/**
 * @Description: 城市访客数量统计持久层接口
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Mapper
@Repository
public interface CityVisitorMapper {
	List<CityVisitor> getCityVisitorList();

	int saveCityVisitor(CityVisitor cityVisitor);
}
