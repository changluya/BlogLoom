package com.changlu.blogloom.module.column.service;

import com.changlu.blogloom.model.vo.PageResult;
import com.changlu.blogloom.model.vo.BlogInfo;
import com.changlu.blogloom.module.column.domain.entity.BlogColumn;
import com.changlu.blogloom.module.column.domain.vo.ColumnTreeVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface BlogColumnService {
	List<ColumnTreeVo> getAdminTree();
	List<ColumnTreeVo> getPublicTree();
	BlogColumn getById(Long id);
	BlogColumn getPublicById(Long id);
	BlogColumn save(BlogColumn column);
	void update(BlogColumn column);
	void move(Long id, Long targetParentId, Integer targetSort);
	void updatePublished(Long id, Boolean published);
	void delete(Long id);
	Map<String, String> uploadCover(Long id, MultipartFile file);
	List<Long> getColumnIdsByBlogId(Long blogId);
	void replaceBlogColumns(Long blogId, List<Long> columnIds);
	void deleteRelationsByBlogId(Long blogId);
	PageResult<BlogInfo> getPublicBlogs(Long columnId, Integer pageNum);
}
