package com.changlu.blogloom.controller;

import com.changlu.blogloom.constant.CacheKeyConstants;
import com.changlu.blogloom.controller.admin.DashboardAdminController;
import com.changlu.blogloom.interceptor.AccessLimitInterceptor;
import com.changlu.blogloom.model.vo.BlogDetail;
import com.changlu.blogloom.model.vo.PageResult;
import com.changlu.blogloom.module.cache.InMemoryCacheMapper;
import com.changlu.blogloom.service.BlogService;
import com.changlu.blogloom.service.DashboardService;
import com.changlu.blogloom.service.MomentService;
import com.changlu.blogloom.service.impl.MysqlBlogCacheServiceImpl;
import com.changlu.blogloom.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 后端接口模拟测试：不依赖真实数据库，用内存 CacheMapper 驱动真实缓存实现，
 * 验证原 Redis 能力（阅读量、UV、限流）在接口层行为一致。
 */
class CacheInterfaceSimulationTest {
	private MysqlBlogCacheServiceImpl cacheService;
	private AccessLimitInterceptor accessLimitInterceptor;

	@BeforeEach
	void setUp() {
		cacheService = new MysqlBlogCacheServiceImpl();
		ReflectionTestUtils.setField(cacheService, "cacheMapper", new InMemoryCacheMapper());

		accessLimitInterceptor = new AccessLimitInterceptor();
		ReflectionTestUtils.setField(accessLimitInterceptor, "cacheService", cacheService);
	}

	@Test
	void shouldCountTodayUvFromCacheOnDashboard() throws Exception {
		cacheService.saveValueToSet(CacheKeyConstants.IDENTIFICATION_SET, "uuid-1");
		cacheService.saveValueToSet(CacheKeyConstants.IDENTIFICATION_SET, "uuid-2");

		DashboardService dashboardService = Mockito.mock(DashboardService.class);
		when(dashboardService.countVisitLogByToday()).thenReturn(10);
		DashboardAdminController controller = new DashboardAdminController();
		ReflectionTestUtils.setField(controller, "dashboardService", dashboardService);
		ReflectionTestUtils.setField(controller, "cacheService", cacheService);

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

		mockMvc.perform(get("/admin/dashboard"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.uv").value(2))
				.andExpect(jsonPath("$.data.pv").value(10));
	}

	@Test
	void shouldIncrementBlogViewsOnDetailEndpoint() throws Exception {
		BlogService blogService = Mockito.mock(BlogService.class);
		BlogDetail detail = new BlogDetail();
		detail.setId(1L);
		detail.setPassword("");
		when(blogService.getBlogByIdAndIsPublished(1L)).thenReturn(detail);

		BlogController controller = new BlogController();
		ReflectionTestUtils.setField(controller, "blogService", blogService);
		ReflectionTestUtils.setField(controller, "userService", Mockito.mock(UserServiceImpl.class));

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

		mockMvc.perform(get("/blog").param("id", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200));

		verify(blogService).updateViewsToCache(1L);
	}

	@Test
	void shouldReturnBlogListEndpoint() throws Exception {
		BlogService blogService = Mockito.mock(BlogService.class);
		when(blogService.getBlogInfoListByIsPublished(1, "createTime"))
				.thenReturn(new PageResult<>(1, Collections.emptyList()));

		BlogController controller = new BlogController();
		ReflectionTestUtils.setField(controller, "blogService", blogService);
		ReflectionTestUtils.setField(controller, "userService", Mockito.mock(UserServiceImpl.class));

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

		mockMvc.perform(get("/blogs").param("pageNum", "1").param("sort", "createTime"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200));
	}

	@Test
	void shouldRateLimitMomentLikeByCache() throws Exception {
		MomentService momentService = Mockito.mock(MomentService.class);
		MomentController controller = new MomentController();
		ReflectionTestUtils.setField(controller, "momentService", momentService);
		ReflectionTestUtils.setField(controller, "userService", Mockito.mock(UserServiceImpl.class));

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.addInterceptors(accessLimitInterceptor)
				.build();

		// 第一次点赞成功
		mockMvc.perform(post("/moment/like/1").header("X-Real-IP", "10.0.0.1"))
				.andExpect(status().isOk());
		// 第二次触发限流
		mockMvc.perform(post("/moment/like/1").header("X-Real-IP", "10.0.0.1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(403))
				.andExpect(jsonPath("$.msg").value("不可以重复点赞哦"));
	}
}
