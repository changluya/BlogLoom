package com.changlu.blogloom.env;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Initializes filesystem related system properties before Spring Boot starts.
 */
public final class SystemPropertyUtil {

	public static final String CONF_DIR_PROPERTY = "user.dir.conf";
	public static final String UPLOAD_DIR_PROPERTY = "user.dir.upload";
	public static final String DEFAULT_CONFIG_FILE = "application.properties";
	public static final String DEFAULT_LOGBACK_FILE = "logback-spring.xml";

	private SystemPropertyUtil() {
	}

	/**
	 * Locates the default conf directory. When the application is started from the
	 * backend module, the repository-level conf directory is also supported.
	 */
	public static void setSystemUserDir() {
		setConfDir();
		setLocalUploadFileDir();
		System.out.println("Config path: " + getConfDir());
	}

	/**
	 * 初始化外置配置目录。
	 */
	public static void setConfDir() {
		Path workingDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
		Path confDirectory = resolveConfDirectory(workingDirectory);
		System.setProperty(CONF_DIR_PROPERTY, confDirectory.toString());
	}

	/**
	 * 设置本地文件上传路径。
	 *
	 * <p>上传目录属于运行环境目录，不再从 application.properties 读取，避免
	 * 配置项和实际静态资源映射出现分叉。默认使用当前配置目录下的 upload 目录。</p>
	 */
	public static void setLocalUploadFileDir() {
		setCustomDir("upload");
	}

	/**
	 * 注册运行环境下的自定义目录系统属性，例如 user.dir.upload。
	 */
	public static void setCustomDir(String dirName) {
		Path baseDirectory = Paths.get(getConfDir()).toAbsolutePath().normalize();
		Path targetDirectory = baseDirectory.resolve(dirName).normalize();
		try {
			Files.createDirectories(targetDirectory);
			Files.createDirectories(targetDirectory.resolve("tmp"));
			Files.createDirectories(targetDirectory.resolve("blogs"));
		} catch (Exception e) {
			throw new IllegalStateException("Unable to create directory: " + targetDirectory, e);
		}
		System.setProperty("user.dir." + dirName, targetDirectory.toString());
	}

	static Path resolveConfDirectory(Path workingDirectory) {
		Path currentConf = workingDirectory.resolve("conf");
		if (Files.isDirectory(currentConf)) {
			return currentConf;
		}

		Path parent = workingDirectory.getParent();
		if (parent != null) {
			Path parentConf = parent.resolve("conf");
			if (Files.isDirectory(parentConf)) {
				return parentConf;
			}
		}
		return currentConf;
	}

	public static String getConfDir() {
		return System.getProperty(CONF_DIR_PROPERTY);
	}

	public static String getLocalUploadFileDir() {
		String uploadDirectory = System.getProperty(UPLOAD_DIR_PROPERTY);
		if (uploadDirectory != null && !uploadDirectory.trim().isEmpty()) {
			return uploadDirectory;
		}
		String confDirectory = getConfDir();
		Path baseDirectory = confDirectory == null
				? resolveConfDirectory(Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize())
				: Paths.get(confDirectory);
		return baseDirectory.resolve("upload").normalize().toString();
	}

	public static Path getDefaultConfigFile() {
		return Paths.get(getConfDir(), DEFAULT_CONFIG_FILE);
	}

	public static Path getDefaultLogbackFile() {
		return Paths.get(getConfDir(), DEFAULT_LOGBACK_FILE);
	}

	public static Path getStaticDir() {
		return Paths.get(getConfDir(), "static");
	}
}
