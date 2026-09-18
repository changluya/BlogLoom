package com.changlu.blogloom.module.column.dao;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Mapper
@Repository
public interface BlogColumnRelationMapper {
	@Select("select column_id from blog_column_relation where blog_id=#{blogId} order by id")
	List<Long> findColumnIdsByBlogId(Long blogId);

	@Select("select count(*) from blog_column_relation where column_id=#{columnId}")
	int countByColumnId(Long columnId);

	@Delete("delete from blog_column_relation where blog_id=#{blogId}")
	int deleteByBlogId(Long blogId);

	@Insert({"<script>",
			"insert into blog_column_relation(column_id,blog_id,sort,create_time) values",
			"<foreach collection='columnIds' item='columnId' separator=','>",
			"(#{columnId},#{blogId},0,#{createTime})",
			"</foreach>",
			"</script>"})
	int batchInsert(@Param("blogId") Long blogId, @Param("columnIds") List<Long> columnIds,
	                @Param("createTime") Date createTime);
}
