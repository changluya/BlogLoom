package com.changlu.blogloom.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import com.changlu.blogloom.entity.Moment;

import java.util.List;

/**
 * @Description: 博客动态持久层接口
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Mapper
@Repository
public interface MomentMapper {
	List<Moment> getMomentList();
	List<Moment> getMomentListByQuery(@Param("query") String query);

	int addLikeByMomentId(Long momentId);

	int updateMomentPublishedById(Long momentId, Boolean published);

	Moment getMomentById(Long id);

	int deleteMomentById(Long id);

	int saveMoment(Moment moment);

	int updateMoment(Moment moment);
}
