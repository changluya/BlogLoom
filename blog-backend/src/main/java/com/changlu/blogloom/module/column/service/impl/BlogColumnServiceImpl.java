package com.changlu.blogloom.module.column.service.impl;

import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.exception.NotFoundException;
import com.changlu.blogloom.exception.PersistenceException;
import com.changlu.blogloom.model.vo.BlogInfo;
import com.changlu.blogloom.model.vo.PageResult;
import com.changlu.blogloom.module.column.dao.BlogColumnMapper;
import com.changlu.blogloom.module.column.dao.BlogColumnRelationMapper;
import com.changlu.blogloom.module.column.domain.entity.BlogColumn;
import com.changlu.blogloom.module.column.domain.vo.ColumnTreeVo;
import com.changlu.blogloom.module.column.service.BlogColumnService;
import com.changlu.blogloom.module.column.service.ColumnCoverStorageService;
import com.changlu.blogloom.service.BlogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class BlogColumnServiceImpl implements BlogColumnService {
	private final BlogColumnMapper columnMapper;
	private final BlogColumnRelationMapper relationMapper;
	private final ColumnCoverStorageService coverStorageService;
	private final BlogService blogService;

	public BlogColumnServiceImpl(BlogColumnMapper columnMapper, BlogColumnRelationMapper relationMapper,
	                             ColumnCoverStorageService coverStorageService, BlogService blogService) {
		this.columnMapper = columnMapper;
		this.relationMapper = relationMapper;
		this.coverStorageService = coverStorageService;
		this.blogService = blogService;
	}

	@Override public List<ColumnTreeVo> getAdminTree() { return buildTree(columnMapper.findAdminRows(), false); }
	@Override public List<ColumnTreeVo> getPublicTree() { return buildTree(columnMapper.findPublicRows(), false); }

	@Override public BlogColumn getById(Long id) {
		BlogColumn column = columnMapper.findById(id);
		if (column == null) throw new NotFoundException("专栏不存在");
		return column;
	}

	@Override public BlogColumn getPublicById(Long id) {
		BlogColumn column = getById(id);
		if (!Boolean.TRUE.equals(column.getPublished())) throw new NotFoundException("专栏不存在");
		if (column.getParentId() != 0L && !Boolean.TRUE.equals(getById(column.getParentId()).getPublished())) {
			throw new NotFoundException("专栏不存在");
		}
		return column;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override public BlogColumn save(BlogColumn column) {
		normalize(column, null);
		Date now = new Date(); column.setCreateTime(now); column.setUpdateTime(now);
		if (columnMapper.insert(column) != 1) throw new PersistenceException("创建专栏失败");
		return column;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override public void update(BlogColumn column) {
		BlogColumn old = getById(column.getId());
		// 层级移动必须统一走 move 接口，避免普通编辑绕过两级结构校验。
		column.setParentId(old.getParentId());
		if (column.getCover() == null) column.setCover(old.getCover());
		normalize(column, column.getId()); column.setUpdateTime(new Date());
		if (columnMapper.update(column) != 1) throw new PersistenceException("更新专栏失败");
	}

	@Transactional(rollbackFor = Exception.class)
	@Override public void move(Long id, Long targetParentId, Integer targetSort) {
		BlogColumn source = getById(id); long parentId = targetParentId == null ? 0L : targetParentId;
		if (id.equals(parentId)) throw new BadRequestException("专栏不能移动到自身下面");
		if (parentId != 0L) {
			BlogColumn parent = getById(parentId);
			if (parent.getParentId() != 0L) throw new BadRequestException("专栏最多支持两级");
			if (columnMapper.countChildren(source.getId()) > 0) throw new BadRequestException("包含子专栏的专栏不能移动到第二级");
		}
		int sort = targetSort == null ? columnMapper.findMaxSort(parentId) + 10 : targetSort;
		if (columnMapper.move(id, parentId, sort) != 1) throw new PersistenceException("移动专栏失败");
	}

	@Transactional(rollbackFor = Exception.class)
	@Override public void updatePublished(Long id, Boolean published) {
		getById(id);
		if (published == null) throw new BadRequestException("请选择是否在前台展示");
		if (columnMapper.updatePublished(id, published) != 1) throw new PersistenceException("更新专栏展示状态失败");
	}

	@Transactional(rollbackFor = Exception.class)
	@Override public void delete(Long id) {
		getById(id);
		if (columnMapper.countChildren(id) > 0) throw new BadRequestException("请先删除或移动子专栏");
		if (relationMapper.countByColumnId(id) > 0) throw new BadRequestException("请先移除专栏中的博客");
		if (columnMapper.delete(id) != 1) throw new PersistenceException("删除专栏失败");
	}

	@Transactional(rollbackFor = Exception.class)
	@Override public Map<String, String> uploadCover(Long id, MultipartFile file) {
		getById(id); String url = coverStorageService.save(id, file);
		if (columnMapper.updateCover(id, url) != 1) throw new PersistenceException("更新专栏图片失败");
		Map<String, String> result = new LinkedHashMap<>(); result.put("url", url); return result;
	}

	@Override public List<Long> getColumnIdsByBlogId(Long blogId) { return relationMapper.findColumnIdsByBlogId(blogId); }

	@Transactional(rollbackFor = Exception.class)
	@Override public void replaceBlogColumns(Long blogId, List<Long> columnIds) {
		relationMapper.deleteByBlogId(blogId);
		if (columnIds == null || columnIds.isEmpty()) return;
		List<Long> ids = new ArrayList<>(new LinkedHashSet<>(columnIds));
		for (Long id : ids) { if (id == null) throw new BadRequestException("专栏ID不能为空"); getById(id); }
		relationMapper.batchInsert(blogId, ids, new Date());
	}

	@Override public void deleteRelationsByBlogId(Long blogId) { relationMapper.deleteByBlogId(blogId); }

	@Override public PageResult<BlogInfo> getPublicBlogs(Long columnId, Integer pageNum) {
		getPublicById(columnId); return blogService.getBlogInfoListByColumnIdAndIsPublished(columnId, pageNum);
	}

	private void normalize(BlogColumn column, Long excludeId) {
		if (column == null || column.getName() == null || column.getName().trim().isEmpty()) throw new BadRequestException("专栏名称不能为空");
		column.setName(column.getName().trim());
		if (column.getName().length() > 100) throw new BadRequestException("专栏名称不能超过100个字符");
		if (column.getDescription() == null) column.setDescription("");
		if (column.getDescription().length() > 500) throw new BadRequestException("专栏简介不能超过500个字符");
		if (column.getCover() == null) column.setCover("");
		if (column.getParentId() == null) column.setParentId(0L);
		if (column.getParentId() != 0L && getById(column.getParentId()).getParentId() != 0L) throw new BadRequestException("专栏最多支持两级");
		if (excludeId != null && excludeId.equals(column.getParentId())) throw new BadRequestException("专栏不能作为自己的父专栏");
		if (columnMapper.countSameName(column.getParentId(), column.getName(), excludeId) > 0) throw new BadRequestException("同级专栏名称已存在");
		if (column.getSort() == null) column.setSort(columnMapper.findMaxSort(column.getParentId()) + 10);
		if (column.getPublished() == null) column.setPublished(true);
	}

	private List<ColumnTreeVo> buildTree(List<ColumnTreeVo> rows, boolean hideEmpty) {
		Map<Long, ColumnTreeVo> roots = new LinkedHashMap<>();
		for (ColumnTreeVo item : rows) if (item.getParentId() == 0L) roots.put(item.getId(), item);
		for (ColumnTreeVo item : rows) if (item.getParentId() != 0L && roots.containsKey(item.getParentId())) roots.get(item.getParentId()).getChildren().add(item);
		if (hideEmpty) {
			for (ColumnTreeVo root : roots.values()) root.getChildren().removeIf(item -> item.getBlogCount() == null || item.getBlogCount() == 0);
			roots.values().removeIf(item -> (item.getBlogCount() == null || item.getBlogCount() == 0) && item.getChildren().isEmpty());
		}
		return new ArrayList<>(roots.values());
	}
}
