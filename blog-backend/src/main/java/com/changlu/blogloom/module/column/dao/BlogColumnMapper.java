package com.changlu.blogloom.module.column.dao;

import com.changlu.blogloom.module.column.domain.entity.BlogColumn;
import com.changlu.blogloom.module.column.domain.vo.ColumnTreeVo;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface BlogColumnMapper {
	@Select("select c.id,c.parent_id,c.name,c.description,c.cover,c.sort,c.is_published published,c.create_time,c.update_time, " +
			"(select count(*) from blog_column_relation r where r.column_id=c.id) blog_count " +
			"from blog_column c order by c.parent_id,c.sort,c.id")
	List<ColumnTreeVo> findAdminRows();

	@Select("select c.id,c.parent_id,c.name,c.description,c.cover,c.sort,c.is_published published,c.create_time,c.update_time, " +
			"(select count(distinct b.id) from blog_column_relation r " +
			"join blog b on b.id=r.blog_id and b.is_published=true where r.column_id=c.id) blog_count " +
			"from blog_column c left join blog_column p on p.id=c.parent_id " +
			"where c.is_published=true and (c.parent_id=0 or p.is_published=true) " +
			"order by c.parent_id,c.sort,c.id")
	List<ColumnTreeVo> findPublicRows();

	@Select("select id,parent_id,name,description,cover,sort,is_published published,create_time,update_time " +
			"from blog_column where id=#{id}")
	BlogColumn findById(Long id);

	@Select("select count(*) from blog_column where parent_id=#{parentId}")
	int countChildren(Long parentId);

	@Select("select count(*) from blog_column where parent_id=#{parentId} and name=#{name} " +
			"and (#{excludeId} is null or id != #{excludeId})")
	int countSameName(@Param("parentId") Long parentId, @Param("name") String name,
	                  @Param("excludeId") Long excludeId);

	@Select("select coalesce(max(sort),0) from blog_column where parent_id=#{parentId}")
	int findMaxSort(Long parentId);

	@Insert("insert into blog_column(parent_id,name,description,cover,sort,is_published,create_time,update_time) " +
			"values(#{parentId},#{name},#{description},#{cover},#{sort},#{published},#{createTime},#{updateTime})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	int insert(BlogColumn column);

	@Update("update blog_column set parent_id=#{parentId},name=#{name},description=#{description},cover=#{cover}," +
			"sort=#{sort},is_published=#{published},update_time=#{updateTime} where id=#{id}")
	int update(BlogColumn column);

	@Update("update blog_column set parent_id=#{parentId},sort=#{sort},update_time=now() where id=#{id}")
	int move(@Param("id") Long id, @Param("parentId") Long parentId, @Param("sort") Integer sort);

	@Update("update blog_column set cover=#{cover},update_time=now() where id=#{id}")
	int updateCover(@Param("id") Long id, @Param("cover") String cover);

	@Update("update blog_column set is_published=#{published},update_time=now() where id=#{id}")
	int updatePublished(@Param("id") Long id, @Param("published") Boolean published);

	@Delete("delete from blog_column where id=#{id}")
	int delete(Long id);
}
