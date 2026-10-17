package com.changlu.blogloom.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @Description: ip记录
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Slf4j
@Component
public class IpAddressUtils {
	/**
	 * 在Nginx等代理之后获取用户真实IP地址
	 *
	 * @param request
	 * @return
	 */
	public static String getIpAddress(HttpServletRequest request) {
		String ip = request.getHeader("X-Real-IP");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("x-forwarded-for");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_CLIENT_IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
			if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
				//根据网卡取本机配置的IP
				InetAddress inet = null;
				try {
					inet = InetAddress.getLocalHost();
				} catch (UnknownHostException e) {
					log.error("getIpAddress exception:", e);
				}
				ip = inet.getHostAddress();
			}
		}
		return StringUtils.substringBefore(ip, ",");
	}

	private Ip2Region ip2Region;

	/**
	 * 同时加载官方 IPv4 / IPv6 数据库。
	 * XDB 通过 classpath 流读取，兼容 Spring Boot 可执行 JAR；BufferCache 会将两份数据驻留内存。
	 *
	 * @throws Exception 初始化失败时终止启动，避免运行期地域查询静默失效
	 */
	@PostConstruct
	void initIp2regionResource() throws Exception {
		try (InputStream v4InputStream = new ClassPathResource("ipdb/ip2region_v4.xdb").getInputStream();
			 InputStream v6InputStream = new ClassPathResource("ipdb/ip2region_v6.xdb").getInputStream()) {
			Config v4Config = Config.custom()
					.setCachePolicy(Config.BufferCache)
					.setXdbInputStream(v4InputStream)
					.asV4();
			Config v6Config = Config.custom()
					.setCachePolicy(Config.BufferCache)
					.setXdbInputStream(v6InputStream)
					.asV6();
			ip2Region = Ip2Region.create(v4Config, v6Config);
		}
		log.info("ip2region IPv4 / IPv6 数据库加载完成");
	}

	/**
	 * 根据 IP 自动识别 IPv4 / IPv6 并查询地域。
	 * 返回字段保持为 Country|Province|City|ISP|ISO-Alpha2，缺失字段以空字符串占位。
	 *
	 * @param ip
	 * @return
	 */
	public String getCityInfo(String ip) {
		if (StringUtils.isBlank(ip) || ip2Region == null) {
			return "";
		}
		try {
			return normalizeRegion(ip2Region.search(ip.trim()));
		} catch (Exception e) {
			log.warn("ip2region 查询失败，ip={}", ip, e);
		}
		return "";
	}

	private String normalizeRegion(String region) {
		if (StringUtils.isBlank(region)) {
			return "";
		}
		String[] fields = region.split("\\|", -1);
		boolean hasValue = false;
		for (int i = 0; i < fields.length; i++) {
			fields[i] = StringUtils.trimToEmpty(fields[i]);
			if ("0".equals(fields[i])) {
				fields[i] = "";
			}
			if (StringUtils.isNotBlank(fields[i])) {
				hasValue = true;
			}
		}
		return hasValue ? String.join("|", fields) : "";
	}

	@PreDestroy
	void closeIp2region() {
		if (ip2Region != null) {
			try {
				ip2Region.close();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.warn("等待 ip2region 查询服务关闭时线程被中断", e);
			}
			ip2Region = null;
		}
	}
}
