package com.changlu.blogloom.module.column.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Mapper
@Repository
public interface BlogColumnRelationMapper {
	List<Long> findColumnIdsByBlogId(Long blogId);

	int countByColumnId(Long columnId);

	int deleteByBlogId(Long blogId);

	int batchInsert(@Param("blogId") Long blogId, @Param("columnIds") List<Long> columnIds,
	                @Param("createTime") Date createTime);
}
