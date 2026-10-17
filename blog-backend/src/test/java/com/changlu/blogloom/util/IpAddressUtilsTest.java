package com.changlu.blogloom.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IpAddressUtilsTest {
	private static IpAddressUtils ipAddressUtils;

	@BeforeAll
	static void setUp() throws Exception {
		ipAddressUtils = new IpAddressUtils();
		ipAddressUtils.initIp2regionResource();
	}

	@AfterAll
	static void tearDown() {
		ipAddressUtils.closeIp2region();
	}

	@Test
	void shouldQueryIpv4AndIpv6WithOneService() {
		String ipv4Region = ipAddressUtils.getCityInfo("1.2.3.4");
		String ipv6Region = ipAddressUtils.getCityInfo("240e:3b7:3272:d8d0:db09:c067:8d59:539e");

		assertEquals("Australia|Queensland|Brisbane||AU", ipv4Region);
		assertEquals("中国|广东省|深圳市|电信|CN", ipv6Region);

		String[] ipv4Fields = ipv4Region.split("\\|", -1);
		String[] ipv6Fields = ipv6Region.split("\\|", -1);
		assertEquals(5, ipv4Fields.length);
		assertEquals("Australia", ipv4Fields[0]);
		assertEquals("Brisbane", ipv4Fields[2]);
		assertEquals("AU", ipv4Fields[4]);
		assertEquals(5, ipv6Fields.length);
		assertEquals("中国", ipv6Fields[0]);
		assertEquals("深圳市", ipv6Fields[2]);
		assertEquals("CN", ipv6Fields[4]);
	}
}
