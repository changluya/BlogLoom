package com.changlu.blogloom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.changlu.blogloom.entity.CityVisitor;
import com.changlu.blogloom.entity.VisitRecord;
import com.changlu.blogloom.mapper.BlogMapper;
import com.changlu.blogloom.mapper.CategoryMapper;
import com.changlu.blogloom.mapper.CityVisitorMapper;
import com.changlu.blogloom.mapper.CommentMapper;
import com.changlu.blogloom.mapper.TagMapper;
import com.changlu.blogloom.mapper.VisitLogMapper;
import com.changlu.blogloom.mapper.VisitRecordMapper;
import com.changlu.blogloom.model.vo.CategoryBlogCount;
import com.changlu.blogloom.model.vo.TagBlogCount;
import com.changlu.blogloom.service.DashboardService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 仪表盘业务层实现
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Service
public class DashboardServiceImpl implements DashboardService {
	@Autowired
	BlogMapper blogMapper;
	@Autowired
	CommentMapper commentMapper;
	@Autowired
	CategoryMapper categoryMapper;
	@Autowired
	TagMapper tagMapper;
	@Autowired
	VisitLogMapper visitLogMapper;
	@Autowired
	VisitRecordMapper visitRecordMapper;
	@Autowired
	CityVisitorMapper cityVisitorMapper;
	//查询最近30天的记录
	private static final int visitRecordLimitNum = 30;
	private static final int CATEGORY_RANK_LIMIT = 10;
	private static final int TAG_RANK_LIMIT = 15;

	@Override
	public int countVisitLogByToday() {
		return visitLogMapper.countVisitLogByToday();
	}

	@Override
	public long countVisitLog() {
		return visitLogMapper.countVisitLog();
	}

	@Override
	public int getBlogCount() {
		return blogMapper.countBlog();
	}

	@Override
	public int getCommentCount() {
		return commentMapper.countComment();
	}

	@Override
	public Map<String, Object> getCategoryBlogCountMap() {
		List<CategoryBlogCount> series = blogMapper.getTopCategoryBlogCountList(CATEGORY_RANK_LIMIT);
		int total = categoryMapper.countCategory();
		Map<String, Object> map = new HashMap<>(8);
		map.put("total", total);
		map.put("displayed", series.size());
		map.put("remaining", Math.max(total - series.size(), 0));
		map.put("series", series);
		return map;
	}

	@Override
	public Map<String, Object> getTagBlogCountMap() {
		List<TagBlogCount> series = tagMapper.getTopTagBlogCount(TAG_RANK_LIMIT);
		int total = tagMapper.countTag();
		int used = tagMapper.countUsedTag();
		Map<String, Object> map = new HashMap<>(8);
		map.put("total", total);
		map.put("used", used);
		map.put("unused", Math.max(total - used, 0));
		map.put("displayed", series.size());
		map.put("remaining", Math.max(total - series.size(), 0));
		map.put("series", series);
		return map;
	}

	@Override
	public List<? extends CategoryBlogCount> getDistributionRanking(String type) {
		if ("category".equals(type)) {
			return blogMapper.getTopCategoryBlogCountList(categoryMapper.countCategory());
		}
		if ("tag".equals(type)) {
			return tagMapper.getTopTagBlogCount(tagMapper.countTag());
		}
		throw new IllegalArgumentException("不支持的排行类型");
	}

	@Override
	public Map<String, List> getVisitRecordMap() {
		List<VisitRecord> visitRecordList = visitRecordMapper.getVisitRecordListByLimit(visitRecordLimitNum);
		List<String> date = new ArrayList<>(visitRecordList.size());
		List<Integer> pv = new ArrayList<>(visitRecordList.size());
		List<Integer> uv = new ArrayList<>(visitRecordList.size());
		for (int i = visitRecordList.size() - 1; i >= 0; i--) {
			VisitRecord visitRecord = visitRecordList.get(i);
			date.add(visitRecord.getDate());
			pv.add(visitRecord.getPv());
			uv.add(visitRecord.getUv());
		}
		Map<String, List> map = new HashMap<>(8);
		map.put("date", date);
		map.put("pv", pv);
		map.put("uv", uv);
		return map;
	}

	@Override
	public List<CityVisitor> getCityVisitorList() {
		return cityVisitorMapper.getCityVisitorList();
	}
}
