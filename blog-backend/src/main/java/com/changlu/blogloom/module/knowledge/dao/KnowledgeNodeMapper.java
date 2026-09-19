package com.changlu.blogloom.module.knowledge.dao;

import com.changlu.blogloom.module.knowledge.domain.entity.KnowledgeNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface KnowledgeNodeMapper {
	List<KnowledgeNode> findAll();

	KnowledgeNode findById(Long id);

	int countChildren(Long parentId);

	int countSameName(@Param("parentId") Long parentId, @Param("name") String name,
	                  @Param("type") String type, @Param("excludeId") Long excludeId);

	KnowledgeNode findSameName(@Param("parentId") Long parentId, @Param("name") String name, @Param("type") String type);

	KnowledgeNode findByBlogId(Long blogId);

	String findBlogContent(Long blogId);

	int countBlog(Long blogId);

	int findMaxSort(Long parentId);

	int insert(KnowledgeNode node);

	int updateName(@Param("id") Long id, @Param("name") String name);

	int move(@Param("id") Long id, @Param("parentId") Long parentId, @Param("sort") Integer sort);

	List<Long> findChildIds(Long parentId);

	int updateSort(@Param("id") Long id, @Param("sort") Integer sort);

	int delete(Long id);

	int deleteByBlogId(Long blogId);

	int backfillBlogs();
}
