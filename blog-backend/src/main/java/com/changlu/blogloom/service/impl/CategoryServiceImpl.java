package com.changlu.blogloom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.changlu.blogloom.entity.Category;
import com.changlu.blogloom.exception.NotFoundException;
import com.changlu.blogloom.exception.PersistenceException;
import com.changlu.blogloom.mapper.CategoryMapper;
import com.changlu.blogloom.service.CategoryService;
import com.changlu.blogloom.service.TagService;

import java.util.List;

/**
 * @Description: 博客分类业务层实现
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Service
public class CategoryServiceImpl implements CategoryService {
	@Autowired
	CategoryMapper categoryMapper;
	@Autowired
	TagService tagService;

	@Override
	public List<Category> getCategoryList() {
		return categoryMapper.getCategoryList();
	}

	@Override
	public List<Category> getCategoryNameList() {
		//直接查库，实时统计各分类下已发布博客数量
		return categoryMapper.getCategoryNameList();
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void saveCategory(Category category) {
		if (categoryMapper.saveCategory(category) != 1) {
			throw new PersistenceException("分类添加失败");
		}
	}

	@Override
	public Category getCategoryById(Long id) {
		Category category = categoryMapper.getCategoryById(id);
		if (category == null) {
			throw new NotFoundException("分类不存在");
		}
		return category;
	}

	@Override
	public Category getCategoryByName(String name) {
		return categoryMapper.getCategoryByName(name);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void deleteCategoryById(Long id) {
		if (categoryMapper.deleteCategoryById(id) != 1) {
			throw new PersistenceException("删除分类失败");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateCategory(Category category) {
		if (categoryMapper.updateCategory(category) != 1) {
			throw new PersistenceException("分类更新失败");
		}
	}
}
