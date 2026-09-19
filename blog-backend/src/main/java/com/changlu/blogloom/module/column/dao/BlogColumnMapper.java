package com.changlu.blogloom.module.column.dao;

import com.changlu.blogloom.module.column.domain.entity.BlogColumn;
import com.changlu.blogloom.module.column.domain.vo.ColumnTreeVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface BlogColumnMapper {
	List<ColumnTreeVo> findAdminRows();

	List<ColumnTreeVo> findPublicRows();

	BlogColumn findById(Long id);

	int countChildren(Long parentId);

	int countSameName(@Param("parentId") Long parentId, @Param("name") String name,
	                  @Param("excludeId") Long excludeId);

	BlogColumn findByName(@Param("parentId") Long parentId, @Param("name") String name);

	BlogColumn findByNameAnyLevel(@Param("name") String name);

	int findMaxSort(Long parentId);

	int insert(BlogColumn column);

	int update(BlogColumn column);

	int move(@Param("id") Long id, @Param("parentId") Long parentId, @Param("sort") Integer sort);

	int updateCover(@Param("id") Long id, @Param("cover") String cover);

	int updatePublished(@Param("id") Long id, @Param("published") Boolean published);

	int delete(Long id);
}
