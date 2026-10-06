package com.changlu.blogloom.module.seo.domain;

import com.changlu.blogloom.entity.Category;
import com.changlu.blogloom.entity.Tag;
import com.changlu.blogloom.model.vo.Badge;
import com.changlu.blogloom.model.vo.Introduction;
import com.changlu.blogloom.model.vo.NewBlog;
import com.changlu.blogloom.model.vo.RandomBlog;
import com.changlu.blogloom.module.column.domain.vo.ColumnTreeVo;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SeoSiteLayout {
	private Map<String, Object> siteInfo = new HashMap<>();
	private Introduction introduction = new Introduction();
	private List<Badge> badges = new ArrayList<>();
	private List<Category> categories = new ArrayList<>();
	private List<Tag> tags = new ArrayList<>();
	private List<RandomBlog> recommendations = new ArrayList<>();
	private List<NewBlog> newBlogs = new ArrayList<>();
	private List<ColumnTreeVo> columns = new ArrayList<>();
	private long totalViews;
	private int publishedBlogCount;
	private long totalBlogViews;
}
