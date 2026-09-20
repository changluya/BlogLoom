package com.changlu.blogloom.interceptor;

import com.changlu.blogloom.annotation.AccessLimit;
import com.changlu.blogloom.module.cache.InMemoryCacheMapper;
import com.changlu.blogloom.service.impl.MysqlBlogCacheServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 访问频率限制接口的模拟测试：验证原 Redis 限流能力在 MySQL 缓存下行为一致。
 */
class AccessLimitInterceptorTest {
	private AccessLimitInterceptor interceptor;
	private MysqlBlogCacheServiceImpl cacheService;
	private InMemoryCacheMapper cacheMapper;

	@BeforeEach
	void setUp() {
		cacheMapper = new InMemoryCacheMapper();
		cacheService = new MysqlBlogCacheServiceImpl();
		ReflectionTestUtils.setField(cacheService, "cacheMapper", cacheMapper);

		interceptor = new AccessLimitInterceptor();
		ReflectionTestUtils.setField(interceptor, "cacheService", cacheService);
	}

	@Test
	void shouldPassWhenMethodHasNoAccessLimit() throws Exception {
		HandlerMethod handlerMethod = handlerMethod("unlimited");
		boolean result = interceptor.preHandle(request(), new MockHttpServletResponse(), handlerMethod);
		assertTrue(result);
	}

	@Test
	void shouldPassAndInitializeCounterOnFirstAccess() throws Exception {
		HandlerMethod handlerMethod = handlerMethod("limitedOnce");
		MockHttpServletRequest request = request();
		boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod);
		assertTrue(result);
		String key = "10.0.0.1:GET:/test";
		assertEquals(1, ((Integer) cacheService.getObjectByValue(key, Integer.class)).intValue());
		assertTrue(cacheMapper.selectByKey(key).getExpireTime() != null);
	}

	@Test
	void shouldRejectWhenExceedingMaxCount() throws Exception {
		HandlerMethod handlerMethod = handlerMethod("limitedOnce");
		MockHttpServletRequest first = request();
		assertTrue(interceptor.preHandle(first, new MockHttpServletResponse(), handlerMethod));

		MockHttpServletRequest second = request();
		MockHttpServletResponse response = new MockHttpServletResponse();
		boolean result = interceptor.preHandle(second, response, handlerMethod);
		assertFalse(result);
		assertTrue(response.getContentAsString().contains("不可以重复点赞哦"));
	}

	@Test
	void shouldAllowUpToMaxCount() throws Exception {
		HandlerMethod handlerMethod = handlerMethod("limitedTwice");
		assertTrue(interceptor.preHandle(request(), new MockHttpServletResponse(), handlerMethod));
		assertTrue(interceptor.preHandle(request(), new MockHttpServletResponse(), handlerMethod));
		boolean third = interceptor.preHandle(request(), new MockHttpServletResponse(), handlerMethod);
		assertFalse(third);
	}

	@Test
	void shouldResetCounterAfterExpire() throws Exception {
		HandlerMethod handlerMethod = handlerMethod("limitedOnce");
		assertTrue(interceptor.preHandle(request(), new MockHttpServletResponse(), handlerMethod));
		// 模拟计数过期
		cacheService.expire("10.0.0.1:GET:/test", -1);
		assertTrue(interceptor.preHandle(request(), new MockHttpServletResponse(), handlerMethod));
		assertEquals(1, ((Integer) cacheService.getObjectByValue("10.0.0.1:GET:/test", Integer.class)).intValue());
	}

	private MockHttpServletRequest request() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		request.addHeader("X-Real-IP", "10.0.0.1");
		return request;
	}

	private HandlerMethod handlerMethod(String methodName) throws NoSuchMethodException {
		Handler handler = new Handler();
		return new HandlerMethod(handler, Handler.class.getMethod(methodName));
	}

	static class Handler {
		public void unlimited() {
		}

		@AccessLimit(seconds = 60, maxCount = 1, msg = "不可以重复点赞哦")
		public void limitedOnce() {
		}

		@AccessLimit(seconds = 60, maxCount = 2, msg = "访问过于频繁")
		public void limitedTwice() {
		}
	}
}
