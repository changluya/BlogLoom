package com.changlu.blogloom.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import com.changlu.blogloom.annotation.AccessLimit;
import com.changlu.blogloom.model.vo.Result;
import com.changlu.blogloom.service.BlogCacheService;
import com.changlu.blogloom.util.IpAddressUtils;
import com.changlu.blogloom.util.JacksonUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * @Description: 访问控制拦截器
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Component
public class AccessLimitInterceptor extends HandlerInterceptorAdapter {
	@Autowired
	BlogCacheService cacheService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if (handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;
			AccessLimit accessLimit = handlerMethod.getMethodAnnotation(AccessLimit.class);
			//方法上没有访问控制的注解，直接通过
			if (accessLimit == null) {
				return true;
			}
			int seconds = accessLimit.seconds();
			int maxCount = accessLimit.maxCount();
			String ip = IpAddressUtils.getIpAddress(request);
			String method = request.getMethod();
			String requestURI = request.getRequestURI();
			String cacheKey = ip + ":" + method + ":" + requestURI;
			Integer count = cacheService.getObjectByValue(cacheKey, Integer.class);
			if (count == null) {
				//在规定周期内第一次访问，写入缓存
				cacheService.incrementByKey(cacheKey, 1);
				cacheService.expire(cacheKey, seconds);
			} else {
				if (count >= maxCount) {
					//超出访问限制次数
					response.setContentType("application/json;charset=utf-8");
					PrintWriter out = response.getWriter();
					Result result = Result.create(403, accessLimit.msg());
					out.write(JacksonUtils.writeValueAsString(result));
					out.flush();
					out.close();
					return false;
				} else {
					//没超出访问限制次数
					cacheService.incrementByKey(cacheKey, 1);
				}
			}
		}
		return true;
	}
}
