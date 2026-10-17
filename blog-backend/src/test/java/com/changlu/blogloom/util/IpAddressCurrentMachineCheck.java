package com.changlu.blogloom.util;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Locale;

/**
 * 手动运行的联网检查：读取当前机器的公网出口 IP，再用本地 ip2region 数据库查询并打印结果。
 * 类名不以 Test 结尾，因此不会被普通 Maven 全量测试自动执行。
 */
class IpAddressCurrentMachineCheck {
	@Test
	void shouldPrintCurrentMachineIpAndRegion() throws Exception {
		String currentIp;
		String ipSource;
		try {
			currentIp = fetchPublicIp();
			ipSource = "公网出口";
		} catch (Exception e) {
			System.out.println("公网出口 IP 获取失败，改用本地活动网卡地址：" + e.getMessage());
			currentIp = findLocalIpAddress();
			ipSource = "本地网卡（通常是内网 IP）";
		}
		if (currentIp == null) {
			System.out.println("未检测到可用的公网出口或本地网卡 IP");
			return;
		}

		IpAddressUtils ipAddressUtils = new IpAddressUtils();
		try {
			ipAddressUtils.initIp2regionResource();
			String region = ipAddressUtils.getCityInfo(currentIp);
			System.out.println("当前机器 IP（" + ipSource + "）：" + currentIp);
			System.out.println("ip2region 查询结果：" + (region.isEmpty() ? "<未命中>" : region));
		} finally {
			ipAddressUtils.closeIp2region();
		}
	}

	private String findLocalIpAddress() throws SocketException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		if (interfaces == null) {
			return null;
		}
		String ipv4AddressFallback = null;
		String otherAddressFallback = null;
		while (interfaces.hasMoreElements()) {
			NetworkInterface networkInterface = interfaces.nextElement();
			if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()
					|| isVirtualOrTunnelInterface(networkInterface.getName())) {
				continue;
			}
			Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
			while (addresses.hasMoreElements()) {
				InetAddress address = addresses.nextElement();
				if (address.isLoopbackAddress() || address.isLinkLocalAddress()) {
					continue;
				}
				String hostAddress = removeIpv6Scope(address.getHostAddress());
				if (!isPrivateAddress(address)) {
					return hostAddress;
				}
				if (address instanceof Inet4Address && ipv4AddressFallback == null) {
					ipv4AddressFallback = hostAddress;
				} else if (otherAddressFallback == null) {
					otherAddressFallback = hostAddress;
				}
			}
		}
		return ipv4AddressFallback != null ? ipv4AddressFallback : otherAddressFallback;
	}

	private boolean isPrivateAddress(InetAddress address) {
		if (address.isSiteLocalAddress()) {
			return true;
		}
		if (address instanceof Inet6Address) {
			byte[] bytes = address.getAddress();
			// IPv6 Unique Local Address (fc00::/7) is private even if the JDK does not classify it as site-local.
			return (bytes[0] & 0xFE) == 0xFC;
		}
		return false;
	}

	private boolean isVirtualOrTunnelInterface(String interfaceName) {
		String normalizedName = interfaceName.toLowerCase(Locale.ROOT);
		return normalizedName.startsWith("utun") || normalizedName.startsWith("tun")
				|| normalizedName.startsWith("tap") || normalizedName.startsWith("wg")
				|| normalizedName.startsWith("ppp") || normalizedName.startsWith("bridge")
				|| normalizedName.startsWith("vmenet") || normalizedName.startsWith("vmnet")
				|| normalizedName.startsWith("docker") || normalizedName.startsWith("veth")
				|| normalizedName.startsWith("awdl") || normalizedName.startsWith("llw")
				|| normalizedName.startsWith("anpi") || normalizedName.startsWith("ap");
	}

	private String removeIpv6Scope(String address) {
		int scopeIndex = address.indexOf('%');
		return scopeIndex >= 0 ? address.substring(0, scopeIndex) : address;
	}

	private String fetchPublicIp() throws Exception {
		String publicIpUrl = System.getProperty("ip2region.public-ip-url", "https://api.ipify.org");
		Process process = new ProcessBuilder(
				"curl", "--fail", "--silent", "--show-error", "--max-time", "10", publicIpUrl)
				.redirectErrorStream(true)
				.start();
		String response;
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			response = reader.readLine();
		}
		int exitCode = process.waitFor();
		if (exitCode != 0) {
			throw new IllegalStateException("curl 获取公网 IP 失败，exitCode=" + exitCode + "，输出=" + response);
		}
		if (response == null || response.trim().isEmpty()) {
			throw new IllegalStateException("公网 IP 检测服务返回空结果");
		}
		return response.trim();
	}
}
