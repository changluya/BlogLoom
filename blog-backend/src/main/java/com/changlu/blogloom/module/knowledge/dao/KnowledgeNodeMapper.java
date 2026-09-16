package com.changlu.blogloom.module.knowledge.dao;

import com.changlu.blogloom.module.knowledge.domain.entity.KnowledgeNode;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface KnowledgeNodeMapper {
	@Select("select kn.*, b.title blog_title, case when kn.blog_id is null or b.id is not null then true else false end blog_exists " +
			"from knowledge_node kn left join blog b on b.id=kn.blog_id order by kn.parent_id, kn.sort, kn.id")
	List<KnowledgeNode> findAll();

	@Select("select * from knowledge_node where id=#{id}")
	KnowledgeNode findById(Long id);

	@Select("select count(*) from knowledge_node where parent_id=#{parentId}")
	int countChildren(Long parentId);

	@Select("select count(*) from knowledge_node where parent_id=#{parentId} and name=#{name} and type=#{type} " +
			"and (#{excludeId} is null or id != #{excludeId})")
	int countSameName(@Param("parentId") Long parentId, @Param("name") String name,
	                  @Param("type") String type, @Param("excludeId") Long excludeId);

	@Select("select * from knowledge_node where parent_id=#{parentId} and name=#{name} and type=#{type} order by id limit 1")
	KnowledgeNode findSameName(@Param("parentId") Long parentId, @Param("name") String name, @Param("type") String type);

	@Select("select * from knowledge_node where blog_id=#{blogId} order by id limit 1")
	KnowledgeNode findByBlogId(Long blogId);

	@Select("select content from blog where id=#{blogId}")
	String findBlogContent(Long blogId);

	@Select("select count(*) from blog where id=#{blogId}")
	int countBlog(Long blogId);

	@Select("select coalesce(max(sort), -1) from knowledge_node where parent_id=#{parentId}")
	int findMaxSort(Long parentId);

	@Insert("insert into knowledge_node(parent_id,blog_id,name,type,sort,create_time,update_time) " +
			"values(#{parentId},#{blogId},#{name},#{type},#{sort},now(),now())")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	int insert(KnowledgeNode node);

	@Update("update knowledge_node set name=#{name},update_time=now() where id=#{id}")
	int updateName(@Param("id") Long id, @Param("name") String name);

	@Update("update knowledge_node set parent_id=#{parentId},sort=#{sort},update_time=now() where id=#{id}")
	int move(@Param("id") Long id, @Param("parentId") Long parentId, @Param("sort") Integer sort);

	@Select("select id from knowledge_node where parent_id=#{parentId} order by sort,id")
	List<Long> findChildIds(Long parentId);

	@Update("update knowledge_node set sort=#{sort},update_time=now() where id=#{id}")
	int updateSort(@Param("id") Long id, @Param("sort") Integer sort);

	@Delete("delete from knowledge_node where id=#{id}")
	int delete(Long id);

	@Delete("delete from knowledge_node where blog_id=#{blogId}")
	int deleteByBlogId(Long blogId);

	@Insert("insert into knowledge_node(parent_id,blog_id,name,type,sort,create_time,update_time) " +
			"select 0,b.id,b.title,'DOC',0,now(),now() from blog b " +
			"left join knowledge_node kn on kn.blog_id=b.id where kn.id is null")
	int backfillBlogs();
}
