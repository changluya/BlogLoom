package com.changlu.blogloom.module.seo.dao;

import com.changlu.blogloom.module.seo.domain.SeoArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SeoArticleMapper {
	@Select("select id, title, description, first_picture as firstPicture, create_time as createTime, " +
			"update_time as updateTime from blog where is_published=true and is_deleted=0 " +
			"and (password is null or password='') order by create_time desc")
	List<SeoArticle> listPublicArticles();

	@Select("select id, title, description, first_picture as firstPicture, create_time as createTime, " +
			"update_time as updateTime from blog where is_published=true and is_deleted=0 " +
			"and (password is null or password='') order by create_time desc limit 20")
	List<SeoArticle> listRecentPublicArticles();
}
