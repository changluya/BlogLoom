package com.changlu.blogloom.service.impl;

import com.changlu.blogloom.constant.CacheKeyConstants;
import com.changlu.blogloom.entity.Blog;
import com.changlu.blogloom.mapper.BlogMapper;
import com.changlu.blogloom.model.vo.BlogDetail;
import com.changlu.blogloom.model.vo.BlogInfo;
import com.changlu.blogloom.model.vo.NewBlog;
import com.changlu.blogloom.model.vo.PageResult;
import com.changlu.blogloom.module.cache.InMemoryCacheMapper;
import com.changlu.blogloom.module.column.dao.BlogColumnMapper;
import com.changlu.blogloom.module.column.dao.BlogColumnRelationMapper;
import com.changlu.blogloom.module.knowledge.dao.KnowledgeNodeMapper;
import com.changlu.blogloom.service.BlogCacheService;
import com.changlu.blogloom.service.BlogResourceService;
import com.changlu.blogloom.service.CommentService;
import com.changlu.blogloom.service.TagService;
import com.github.pagehelper.PageHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 博客业务层缓存路径测试：覆盖原 Redis 浏览量、页面缓存、缓存失效等逻辑。
 */
class BlogServiceImplCacheTest {
	private BlogServiceImpl service;
	private BlogMapper blogMapper;
	private TagService tagService;
	private MysqlBlogCacheServiceImpl cacheService;
	private InMemoryCacheMapper cacheMapper;
	private BlogResourceService blogResourceService;
	private KnowledgeNodeMapper knowledgeNodeMapper;
	private BlogColumnMapper blogColumnMapper;
	private BlogColumnRelationMapper blogColumnRelationMapper;
	private CommentService commentService;

	@BeforeEach
	void setUp() {
		blogMapper = Mockito.mock(BlogMapper.class);
		tagService = Mockito.mock(TagService.class);
		blogResourceService = Mockito.mock(BlogResourceService.class);
		knowledgeNodeMapper = Mockito.mock(KnowledgeNodeMapper.class);
		blogColumnMapper = Mockito.mock(BlogColumnMapper.class);
		blogColumnRelationMapper = Mockito.mock(BlogColumnRelationMapper.class);
		commentService = Mockito.mock(CommentService.class);

		cacheMapper = new InMemoryCacheMapper();
		cacheService = new MysqlBlogCacheServiceImpl();
		ReflectionTestUtils.setField(cacheService, "cacheMapper", cacheMapper);

		service = new BlogServiceImpl();
		ReflectionTestUtils.setField(service, "blogMapper", blogMapper);
		ReflectionTestUtils.setField(service, "tagService", tagService);
		ReflectionTestUtils.setField(service, "cacheService", (BlogCacheService) cacheService);
		ReflectionTestUtils.setField(service, "blogResourceService", blogResourceService);
		ReflectionTestUtils.setField(service, "knowledgeNodeMapper", knowledgeNodeMapper);
		ReflectionTestUtils.setField(service, "blogColumnMapper", blogColumnMapper);
		ReflectionTestUtils.setField(service, "blogColumnRelationMapper", blogColumnRelationMapper);
		ReflectionTestUtils.setField(service, "commentService", commentService);
	}

	@AfterEach
	void tearDown() {
		PageHelper.clearPage();
	}

	@Test
	void shouldIncrementAndReadBlogViewsFromCache() {
		BlogDetail detail = new BlogDetail();
		detail.setId(1L);
		detail.setContent("");
		detail.setPassword("");
		when(blogMapper.getBlogByIdAndIsPublished(1L)).thenReturn(detail);

		service.updateViewsToCache(1L);
		service.updateViewsToCache(1L);

		BlogDetail loaded = service.getBlogByIdAndIsPublished(1L);
		assertEquals(2, loaded.getViews().intValue());
	}

	@Test
	void shouldReturnCachedNewBlogListWithoutQueryingDatabase() {
		List<NewBlog> cached = new ArrayList<>(Arrays.asList(new NewBlog()));
		cacheService.saveListToValue(CacheKeyConstants.NEW_BLOG_LIST, cached);

		List<NewBlog> result = service.getNewBlogListByIsPublished();

		assertEquals(1, result.size());
		verify(blogMapper, never()).getNewBlogListByIsPublished();
	}

	@Test
	void shouldQueryAndCacheNewBlogListOnMiss() {
		NewBlog newBlog = new NewBlog();
		newBlog.setId(1L);
		newBlog.setPassword("");
		when(blogMapper.getNewBlogListByIsPublished()).thenReturn(new ArrayList<>(Arrays.asList(newBlog)));

		List<NewBlog> result = service.getNewBlogListByIsPublished();

		assertEquals(1, result.size());
		assertFalse(newBlog.getPrivacy());
		assertNotNull(cacheService.getListByValue(CacheKeyConstants.NEW_BLOG_LIST));
	}

	@Test
	void shouldReturnCachedArchiveWithoutQueryingDatabase() {
		Map<String, Object> cached = new HashMap<>();
		cached.put("count", 5);
		cacheService.saveMapToValue(CacheKeyConstants.ARCHIVE_BLOG_MAP, cached);

		Map<String, Object> result = service.getArchiveBlogAndCountByIsPublished();

		assertEquals(5, result.get("count"));
		verify(blogMapper, never()).getGroupYearMonthByIsPublished();
	}

	@Test
	void shouldQueryAndCacheArchiveOnMiss() {
		when(blogMapper.getGroupYearMonthByIsPublished()).thenReturn(Arrays.asList("2026-09"));
		when(blogMapper.getArchiveBlogListByYearMonthAndIsPublished("2026-09")).thenReturn(new ArrayList<>());
		when(blogMapper.countBlogByIsPublished()).thenReturn(3);

		Map<String, Object> result = service.getArchiveBlogAndCountByIsPublished();

		assertEquals(3, result.get("count"));
		assertNotNull(cacheService.getMapByValue(CacheKeyConstants.ARCHIVE_BLOG_MAP));
	}

	@Test
	void shouldDeleteBlogCacheAndViewFieldOnDelete() {
		when(blogMapper.softDeleteBlogById(1L)).thenReturn(1);
		cacheService.saveKVToHash(CacheKeyConstants.BLOG_VIEWS_MAP, 1L, 10);
		cacheService.saveListToValue(CacheKeyConstants.NEW_BLOG_LIST, new ArrayList<>());

		service.deleteBlogById(1L);

		assertNull(cacheService.getValueByHashKey(CacheKeyConstants.BLOG_VIEWS_MAP, 1L));
		assertFalse(cacheService.hasKey(CacheKeyConstants.NEW_BLOG_LIST));
	}

	@Test
	void shouldRestoreViewsFromDatabaseIntoCache() {
		when(blogMapper.restoreBlogById(1L)).thenReturn(1);
		when(blogMapper.getBlogViewsById(1L)).thenReturn(42);

		service.restoreBlogById(1L);

		assertEquals(42, cacheService.getValueByHashKey(CacheKeyConstants.BLOG_VIEWS_MAP, 1L));
	}

	@Test
	void shouldWriteViewsToCacheOnUpdate() {
		com.changlu.blogloom.model.dto.Blog dto = new com.changlu.blogloom.model.dto.Blog();
		dto.setId(1L);
		dto.setViews(99);
		dto.setColumnIds(new ArrayList<>());
		when(blogMapper.updateBlog(dto)).thenReturn(1);

		service.updateBlog(dto);

		assertEquals(99, cacheService.getValueByHashKey(CacheKeyConstants.BLOG_VIEWS_MAP, 1L));
	}

	@Test
	void shouldInitializeViewsToZeroOnSaveBlog() {
		com.changlu.blogloom.model.dto.Blog dto = new com.changlu.blogloom.model.dto.Blog();
		dto.setId(5L);
		dto.setTitle("title");
		dto.setColumnIds(new ArrayList<>());
		when(blogMapper.saveBlog(dto)).thenReturn(1);
		when(blogMapper.updateBlogResources(dto)).thenReturn(1);
		when(knowledgeNodeMapper.findMaxSort(0L)).thenReturn(0);
		when(knowledgeNodeMapper.insert(Mockito.any())).thenReturn(1);

		service.saveBlog(dto);

		assertEquals(0, cacheService.getValueByHashKey(CacheKeyConstants.BLOG_VIEWS_MAP, 5L));
	}

	@Test
	void shouldSetViewsFromCacheInBlogInfoList() {
		BlogInfo info = new BlogInfo();
		info.setId(1L);
		info.setDescription("");
		info.setPassword("");
		when(blogMapper.getBlogInfoListByIsPublished(false)).thenReturn(new ArrayList<>(Arrays.asList(info)));
		when(tagService.getTagListByBlogId(1L)).thenReturn(new ArrayList<>());
		cacheService.saveKVToHash(CacheKeyConstants.BLOG_VIEWS_MAP, 1L, 77);

		PageResult<BlogInfo> result = service.getBlogInfoListByIsPublished(1, "createTime");

		assertEquals(77, result.getList().get(0).getViews().intValue());
	}

	@Test
	void shouldGetBlogByIdWithCachedViews() {
		Blog blog = new Blog();
		blog.setId(1L);
		blog.setViews(0);
		when(blogMapper.getBlogById(1L)).thenReturn(blog);
		when(blogColumnRelationMapper.findColumnIdsByBlogId(1L)).thenReturn(new ArrayList<>());
		cacheService.saveKVToHash(CacheKeyConstants.BLOG_VIEWS_MAP, 1L, 88);

		Blog loaded = service.getBlogById(1L);

		assertEquals(88, loaded.getViews().intValue());
	}
}
