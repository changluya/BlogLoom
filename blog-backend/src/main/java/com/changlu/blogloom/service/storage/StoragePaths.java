package com.changlu.blogloom.service.storage;

/**
 * 渠道内相对路径拼接工具。
 */
final class StoragePaths {
	private StoragePaths() {
	}

	static String join(String directory, String fileName) {
		String dir = directory == null ? "" : directory.replace('\\', '/');
		while (dir.startsWith("/")) {
			dir = dir.substring(1);
		}
		while (dir.endsWith("/")) {
			dir = dir.substring(0, dir.length() - 1);
		}
		return dir.isEmpty() ? fileName : dir + "/" + fileName;
	}

	static String fileName(String path) {
		if (path == null) {
			return "";
		}
		String value = path.replace('\\', '/');
		int slash = value.lastIndexOf('/');
		return slash < 0 ? value : value.substring(slash + 1);
	}
}
