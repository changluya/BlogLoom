package com.changlu.blogloom.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.changlu.blogloom.constant.CacheKeyConstants;
import com.changlu.blogloom.entity.Blog;
import com.changlu.blogloom.exception.NotFoundException;
import com.changlu.blogloom.exception.PersistenceException;
import com.changlu.blogloom.mapper.BlogMapper;
import com.changlu.blogloom.model.dto.BlogView;
import com.changlu.blogloom.model.dto.BlogVisibility;
import com.changlu.blogloom.model.vo.ArchiveBlog;
import com.changlu.blogloom.model.vo.BlogDetail;
import com.changlu.blogloom.model.vo.BlogInfo;
import com.changlu.blogloom.model.vo.NewBlog;
import com.changlu.blogloom.model.vo.PageResult;
import com.changlu.blogloom.model.vo.RandomBlog;
import com.changlu.blogloom.model.vo.SearchBlog;
import com.changlu.blogloom.service.BlogService;
import com.changlu.blogloom.service.BlogResourceService;
import com.changlu.blogloom.service.BlogCacheService;
import com.changlu.blogloom.service.CommentService;
import com.changlu.blogloom.service.TagService;
import com.changlu.blogloom.util.JacksonUtils;
import com.changlu.blogloom.util.markdown.MarkdownUtils;
import com.changlu.blogloom.module.knowledge.dao.KnowledgeNodeMapper;
import com.changlu.blogloom.module.knowledge.domain.entity.KnowledgeNode;
import com.changlu.blogloom.module.knowledge.domain.enums.KnowledgeNodeType;
import com.changlu.blogloom.module.column.dao.BlogColumnMapper;
import com.changlu.blogloom.module.column.dao.BlogColumnRelationMapper;
import com.changlu.blogloom.exception.BadRequestException;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Date;

/**
 * @Description: 博客文章业务层实现
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Service
public class BlogServiceImpl implements BlogService {
	@Autowired
	BlogMapper blogMapper;
	@Autowired
	TagService tagService;
	@Autowired
	BlogCacheService cacheService;
	@Autowired
	BlogResourceService blogResourceService;
	@Autowired
	KnowledgeNodeMapper knowledgeNodeMapper;
	@Autowired
	BlogColumnMapper blogColumnMapper;
	@Autowired
	BlogColumnRelationMapper blogColumnRelationMapper;
	@Autowired
	CommentService commentService;
	//随机博客显示5条
	private static final int randomBlogLimitNum = 5;
	//最新推荐博客显示3条
	private static final int newBlogPageSize = 3;
	//每页显示5条博客简介
	private static final int pageSize = 15;
	//博客简介列表排序方式
	private static final String orderBy = "is_top desc, create_time desc";
	//私密博客提示
	private static final String PRIVATE_BLOG_DESCRIPTION = "此文章受密码保护！";

	/**
	 * 项目启动时，保存所有博客的浏览量到缓存
	 */
	@PostConstruct
	private void saveBlogViewsToCache() {
		String cacheKey = CacheKeyConstants.BLOG_VIEWS_MAP;
		//缓存中没有存储博客浏览量的Hash
		if (!cacheService.hasKey(cacheKey)) {
			//从数据库中读取并存入缓存
			Map<Long, Integer> blogViewsMap = getBlogViewsMap();
			cacheService.saveMapToHash(cacheKey, blogViewsMap);
		}
	}

	@Override
	public List<Blog> getListByTitleAndCategoryId(String title, Integer categoryId) {
		return blogMapper.getListByTitleAndCategoryId(title, categoryId);
	}

	@Override
	public List<Blog> getDeletedListByTitleAndCategoryId(String title, Integer categoryId) {
		return blogMapper.getDeletedListByTitleAndCategoryId(title, categoryId);
	}

	@Override
	public List<SearchBlog> getSearchBlogListByQueryAndIsPublished(String query) {
		List<SearchBlog> searchBlogs = blogMapper.getSearchBlogListByQueryAndIsPublished(query);
		// 数据库的处理是不区分大小写的，那么这里的匹配串处理也应该不区分大小写，否则会出现不准确的结果
		query = query.toUpperCase();
		for (SearchBlog searchBlog : searchBlogs) {
			String content = searchBlog.getContent().toUpperCase();
			int contentLength = content.length();
			int index = content.indexOf(query) - 10;
			index = Math.max(index, 0);
			int end = index + 21;//以关键字字符串为中心返回21个字
			end = Math.min(end, contentLength - 1);
			searchBlog.setContent(searchBlog.getContent().substring(index, end));
		}
		return searchBlogs;
	}

	@Override
	public List<Blog> getIdAndTitleList() {
		return blogMapper.getIdAndTitleList();
	}

	@Override
	public List<NewBlog> getNewBlogListByIsPublished() {
		String cacheKey = CacheKeyConstants.NEW_BLOG_LIST;
		List<NewBlog> newBlogListFromCache = cacheService.getListByValue(cacheKey);
		if (newBlogListFromCache != null) {
			return newBlogListFromCache;
		}
		PageHelper.startPage(1, newBlogPageSize);
		List<NewBlog> newBlogList = blogMapper.getNewBlogListByIsPublished();
		for (NewBlog newBlog : newBlogList) {
			if (!"".equals(newBlog.getPassword())) {
				newBlog.setPrivacy(true);
				newBlog.setPassword("");
			} else {
				newBlog.setPrivacy(false);
			}
		}
		cacheService.saveListToValue(cacheKey, newBlogList);
		return newBlogList;
	}

	@Override
	public PageResult<BlogInfo> getBlogInfoListByIsPublished(Integer pageNum, String sort) {
		boolean topOnly = "top".equalsIgnoreCase(sort);
		boolean byViews = "views".equalsIgnoreCase(sort);
		boolean byCreateTime = "createTime".equalsIgnoreCase(sort);
		// 默认（未选择筛选）：置顶优先，其次更新时间靠前
		String homeOrderBy;
		if (byViews) homeOrderBy = "is_top desc, views desc, create_time desc";
		else if (byCreateTime) homeOrderBy = "is_top desc, create_time desc";
		else homeOrderBy = "is_top desc, update_time desc";
		PageHelper.startPage(pageNum, pageSize, homeOrderBy);
		List<BlogInfo> blogInfos = processBlogInfosPassword(blogMapper.getBlogInfoListByIsPublished(topOnly));
		PageInfo<BlogInfo> pageInfo = new PageInfo<>(blogInfos);
		PageResult<BlogInfo> pageResult = new PageResult<>(pageInfo.getPages(), pageInfo.getList());
		setBlogViewsFromCacheToPageResult(pageResult);
		return pageResult;
	}

	/**
	 * 将pageResult中博客对象的浏览量设置为缓存中的最新值
	 *
	 * @param pageResult
	 */
	private void setBlogViewsFromCacheToPageResult(PageResult<BlogInfo> pageResult) {
		String cacheKey = CacheKeyConstants.BLOG_VIEWS_MAP;
		List<BlogInfo> blogInfos = pageResult.getList();
		for (int i = 0; i < blogInfos.size(); i++) {
			BlogInfo blogInfo = JacksonUtils.convertValue(blogInfos.get(i), BlogInfo.class);
			Long blogId = blogInfo.getId();
			/**
			 * 这里如果出现异常，通常是手动修改过 MySQL 而没有通过后台管理，导致缓存和 MySQL 不同步
			 * 从缓存中查出了 null，强转 int 时出现 NullPointerException
			 * 直接抛出异常比带着 bug 继续跑要好得多
			 *
			 * 解决步骤：
			 * 1.结束程序
			 * 2.删除 cache_entry 表中 blogViewsMap 相关记录（或直接清空 cache_entry 表）
			 * 3.重新启动程序
			 *
			 * 具体请查看: https://github.com/Naccl/NBlog/issues/58
			 */
			int view = (int) cacheService.getValueByHashKey(cacheKey, blogId);
			blogInfo.setViews(view);
			blogInfos.set(i, blogInfo);
		}
	}

	@Override
	public PageResult<BlogInfo> getBlogInfoListByCategoryNameAndIsPublished(String categoryName, Integer pageNum) {
		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<BlogInfo> blogInfos = processBlogInfosPassword(blogMapper.getBlogInfoListByCategoryNameAndIsPublished(categoryName));
		PageInfo<BlogInfo> pageInfo = new PageInfo<>(blogInfos);
		PageResult<BlogInfo> pageResult = new PageResult<>(pageInfo.getPages(), pageInfo.getList());
		setBlogViewsFromCacheToPageResult(pageResult);
		return pageResult;
	}

	@Override
	public PageResult<BlogInfo> getBlogInfoListByTagNameAndIsPublished(String tagName, Integer pageNum) {
		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<BlogInfo> blogInfos = processBlogInfosPassword(blogMapper.getBlogInfoListByTagNameAndIsPublished(tagName));
		PageInfo<BlogInfo> pageInfo = new PageInfo<>(blogInfos);
		PageResult<BlogInfo> pageResult = new PageResult<>(pageInfo.getPages(), pageInfo.getList());
		setBlogViewsFromCacheToPageResult(pageResult);
		return pageResult;
	}

	@Override
	public PageResult<BlogInfo> getBlogInfoListByColumnIdAndIsPublished(Long columnId, Integer pageNum) {
		PageHelper.startPage(pageNum, pageSize);
		List<BlogInfo> blogInfos = processBlogInfosPassword(blogMapper.getBlogInfoListByColumnIdAndIsPublished(columnId));
		PageInfo<BlogInfo> pageInfo = new PageInfo<>(blogInfos);
		PageResult<BlogInfo> pageResult = new PageResult<>(pageInfo.getPages(), pageInfo.getList());
		setBlogViewsFromCacheToPageResult(pageResult);
		return pageResult;
	}

	private List<BlogInfo> processBlogInfosPassword(List<BlogInfo> blogInfos) {
		for (BlogInfo blogInfo : blogInfos) {
			if (!"".equals(blogInfo.getPassword())) {
				blogInfo.setPrivacy(true);
				blogInfo.setPassword("");
				blogInfo.setDescription(PRIVATE_BLOG_DESCRIPTION);
			} else {
				blogInfo.setPrivacy(false);
				blogInfo.setDescription(MarkdownUtils.markdownToHtmlExtensions(blogInfo.getDescription()));
			}
			blogInfo.setTags(tagService.getTagListByBlogId(blogInfo.getId()));
		}
		return blogInfos;
	}

	@Override
	public Map<String, Object> getArchiveBlogAndCountByIsPublished() {
		String cacheKey = CacheKeyConstants.ARCHIVE_BLOG_MAP;
		Map<String, Object> mapFromCache = cacheService.getMapByValue(cacheKey);
		if (mapFromCache != null) {
			return mapFromCache;
		}
		List<String> groupYearMonth = blogMapper.getGroupYearMonthByIsPublished();
		Map<String, List<ArchiveBlog>> archiveBlogMap = new LinkedHashMap<>();
		for (String s : groupYearMonth) {
			List<ArchiveBlog> archiveBlogs = blogMapper.getArchiveBlogListByYearMonthAndIsPublished(s);
			for (ArchiveBlog archiveBlog : archiveBlogs) {
				if (!"".equals(archiveBlog.getPassword())) {
					archiveBlog.setPrivacy(true);
					archiveBlog.setPassword("");
				} else {
					archiveBlog.setPrivacy(false);
				}
			}
			archiveBlogMap.put(s, archiveBlogs);
		}
		Integer count = countBlogByIsPublished();
		Map<String, Object> map = new HashMap<>(4);
		map.put("blogMap", archiveBlogMap);
		map.put("count", count);
		cacheService.saveMapToValue(cacheKey, map);
		return map;
	}

	@Override
	public List<RandomBlog> getRandomBlogListByLimitNumAndIsPublishedAndIsRecommend() {
		List<RandomBlog> randomBlogs = blogMapper.getRandomBlogListByLimitNumAndIsPublishedAndIsRecommend(randomBlogLimitNum);
		for (RandomBlog randomBlog : randomBlogs) {
			if (!"".equals(randomBlog.getPassword())) {
				randomBlog.setPrivacy(true);
				randomBlog.setPassword("");
			} else {
				randomBlog.setPrivacy(false);
			}
		}
		return randomBlogs;
	}

	private Map<Long, Integer> getBlogViewsMap() {
		List<BlogView> blogViewList = blogMapper.getBlogViewsList();
		Map<Long, Integer> blogViewsMap = new HashMap<>(128);
		for (BlogView blogView : blogViewList) {
			blogViewsMap.put(blogView.getId(), blogView.getViews());
		}
		return blogViewsMap;
	}

	/**
	 * 逻辑删除博客：仅标记 is_deleted=1 移入回收站。
	 * 知识库节点、标签关联、专栏关联、评论均保留，便于回收站恢复；不再物理删除任何关联数据。
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public void deleteBlogById(Long id) {
		if (blogMapper.softDeleteBlogById(id) != 1) {
			throw new NotFoundException("该博客不存在");
		}
		deleteBlogCache();
		cacheService.deleteByHashKey(CacheKeyConstants.BLOG_VIEWS_MAP, id);
	}

	/**
	 * 从回收站恢复博客：清除 is_deleted 标记，并把浏览量回填到缓存，避免首页列表读取浏览量时出现空值。
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public void restoreBlogById(Long id) {
		if (blogMapper.restoreBlogById(id) != 1) {
			throw new NotFoundException("该博客不存在");
		}
		Integer views = blogMapper.getBlogViewsById(id);
		cacheService.saveKVToHash(CacheKeyConstants.BLOG_VIEWS_MAP, id, views == null ? 0 : views);
		deleteBlogCache();
	}

	/**
	 * 彻底删除博客（仅回收站使用）：物理删除博客及其知识库节点、标签关联、专栏关联、评论等全部关联数据。
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public void deleteBlogPermanentlyById(Long id) {
		commentService.deleteCommentsByBlogId(id);
		knowledgeNodeMapper.deleteByBlogId(id);
		blogColumnRelationMapper.deleteByBlogId(id);
		blogMapper.deleteBlogTagByBlogId(id);
		if (blogMapper.deleteBlogById(id) != 1) {
			throw new NotFoundException("该博客不存在");
		}
		deleteBlogCache();
		cacheService.deleteByHashKey(CacheKeyConstants.BLOG_VIEWS_MAP, id);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void deleteBlogTagByBlogId(Long blogId) {
		blogMapper.deleteBlogTagByBlogId(blogId);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void saveBlog(com.changlu.blogloom.model.dto.Blog blog) {
		if (blogMapper.saveBlog(blog) != 1) {
			throw new PersistenceException("添加博客失败");
		}
		blogResourceService.reconcileBlogResources(blog.getId(), blog);
		if (blogMapper.updateBlogResources(blog) != 1) {
			throw new PersistenceException("保存博客资源链接失败");
		}
		KnowledgeNode node = new KnowledgeNode();
		node.setParentId(0L);
		node.setBlogId(blog.getId());
		node.setName(blog.getTitle());
		node.setType(KnowledgeNodeType.DOC.name());
		node.setSort(knowledgeNodeMapper.findMaxSort(0L) + 1);
		knowledgeNodeMapper.insert(node);
		replaceBlogColumns(blog.getId(), blog.getColumnIds());
		cacheService.saveKVToHash(CacheKeyConstants.BLOG_VIEWS_MAP, blog.getId(), 0);
		deleteBlogCache();
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void saveBlogTag(Long blogId, Long tagId) {
		if (blogMapper.saveBlogTag(blogId, tagId) != 1) {
			throw new PersistenceException("维护博客标签关联表失败");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateBlogRecommendById(Long blogId, Boolean recommend) {
		if (blogMapper.updateBlogRecommendById(blogId, recommend) != 1) {
			throw new PersistenceException("操作失败");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateBlogVisibilityById(Long blogId, BlogVisibility blogVisibility) {
		if (blogMapper.updateBlogVisibilityById(blogId, blogVisibility) != 1) {
			throw new PersistenceException("操作失败");
		}
		cacheService.deleteCacheByKey(CacheKeyConstants.HOME_BLOG_INFO_LIST);
		cacheService.deleteCacheByKey(CacheKeyConstants.NEW_BLOG_LIST);
		cacheService.deleteCacheByKey(CacheKeyConstants.ARCHIVE_BLOG_MAP);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateBlogTopById(Long blogId, Boolean top) {
		if (blogMapper.updateBlogTopById(blogId, top) != 1) {
			throw new PersistenceException("操作失败");
		}
		cacheService.deleteCacheByKey(CacheKeyConstants.HOME_BLOG_INFO_LIST);
	}

	@Override
	public void updateViewsToCache(Long blogId) {
		cacheService.incrementByHashKey(CacheKeyConstants.BLOG_VIEWS_MAP, blogId, 1);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateViews(Long blogId, Integer views) {
		if (blogMapper.updateViews(blogId, views) != 1) {
			throw new PersistenceException("更新失败");
		}
	}

	@Override
	public Blog getBlogById(Long id) {
		Blog blog = blogMapper.getBlogById(id);
		if (blog == null) {
			throw new NotFoundException("博客不存在");
		}
		/**
		 * 将浏览量设置为缓存中的最新值
		 * 这里如果出现异常，查看第 152 行注释说明
		 * @see BlogServiceImpl#setBlogViewsFromCacheToPageResult
		 */
		int view = (int) cacheService.getValueByHashKey(CacheKeyConstants.BLOG_VIEWS_MAP, blog.getId());
		blog.setViews(view);
		blog.setColumnIds(blogColumnRelationMapper.findColumnIdsByBlogId(id));
		return blog;
	}

	@Override
	public String getTitleByBlogId(Long id) {
		return blogMapper.getTitleByBlogId(id);
	}

	@Override
	public BlogDetail getBlogByIdAndIsPublished(Long id) {
		BlogDetail blog = blogMapper.getBlogByIdAndIsPublished(id);
		if (blog == null) {
			throw new NotFoundException("该博客不存在");
		}
		blog.setContent(MarkdownUtils.markdownToHtmlExtensions(blog.getContent()));
		/**
		 * 将浏览量设置为缓存中的最新值
		 * 这里如果出现异常，查看第 152 行注释说明
		 * @see BlogServiceImpl#setBlogViewsFromCacheToPageResult
		 */
		int view = (int) cacheService.getValueByHashKey(CacheKeyConstants.BLOG_VIEWS_MAP, blog.getId());
		blog.setViews(view);
		return blog;
	}

	@Override
	public String getBlogPassword(Long blogId) {
		return blogMapper.getBlogPassword(blogId);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateBlog(com.changlu.blogloom.model.dto.Blog blog) {
		blogResourceService.reconcileBlogResources(blog.getId(), blog);
		if (blogMapper.updateBlog(blog) != 1) {
			throw new PersistenceException("更新博客失败");
		}
		replaceBlogColumns(blog.getId(), blog.getColumnIds());
		deleteBlogCache();
		cacheService.saveKVToHash(CacheKeyConstants.BLOG_VIEWS_MAP, blog.getId(), blog.getViews());
	}

	@Override
	public int countBlogByIsPublished() {
		return blogMapper.countBlogByIsPublished();
	}

	@Override
	public long sumViewsByIsPublished() {
		long totalViews = 0L;
		for (BlogView blogView : blogMapper.getBlogViewsListByIsPublished()) {
			Object cachedViews = cacheService.getValueByHashKey(CacheKeyConstants.BLOG_VIEWS_MAP, blogView.getId());
			if (cachedViews instanceof Number) {
				totalViews += ((Number) cachedViews).longValue();
			} else if (blogView.getViews() != null) {
				totalViews += blogView.getViews();
			}
		}
		return totalViews;
	}

	@Override
	public int countBlogByCategoryId(Long categoryId) {
		return blogMapper.countBlogByCategoryId(categoryId);
	}

	@Override
	public int countBlogByTagId(Long tagId) {
		return blogMapper.countBlogByTagId(tagId);
	}

	@Override
	public Boolean getCommentEnabledByBlogId(Long blogId) {
		return blogMapper.getCommentEnabledByBlogId(blogId);
	}

	@Override
	public Boolean getPublishedByBlogId(Long blogId) {
		return blogMapper.getPublishedByBlogId(blogId);
	}

	/**
	 * 删除首页缓存、最新推荐缓存、归档页面缓存、博客浏览量缓存
	 */
	private void deleteBlogCache() {
		cacheService.deleteCacheByKey(CacheKeyConstants.HOME_BLOG_INFO_LIST);
		cacheService.deleteCacheByKey(CacheKeyConstants.NEW_BLOG_LIST);
		cacheService.deleteCacheByKey(CacheKeyConstants.ARCHIVE_BLOG_MAP);
	}

	private void replaceBlogColumns(Long blogId, List<Long> columnIds) {
		blogColumnRelationMapper.deleteByBlogId(blogId);
		if (columnIds == null || columnIds.isEmpty()) {
			return;
		}
		List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(columnIds));
		for (Long columnId : distinctIds) {
			if (columnId == null || blogColumnMapper.findById(columnId) == null) {
				throw new BadRequestException("专栏不存在");
			}
		}
		blogColumnRelationMapper.batchInsert(blogId, distinctIds, new Date());
	}
}
