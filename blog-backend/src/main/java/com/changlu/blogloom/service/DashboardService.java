package com.changlu.blogloom.service;

import com.changlu.blogloom.entity.CityVisitor;
import com.changlu.blogloom.model.vo.CategoryBlogCount;

import java.util.List;
import java.util.Map;

public interface DashboardService {
	int countVisitLogByToday();

	long countVisitLog();

	int getBlogCount();

	int getCommentCount();

	Map<String, Object> getCategoryBlogCountMap();

	Map<String, Object> getTagBlogCountMap();

	List<? extends CategoryBlogCount> getDistributionRanking(String type);

	Map<String, List> getVisitRecordMap();

	List<CityVisitor> getCityVisitorList();
}
