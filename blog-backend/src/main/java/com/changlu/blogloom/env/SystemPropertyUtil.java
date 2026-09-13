package com.changlu.blogloom.env;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Initializes filesystem related system properties before Spring Boot starts.
 */
public final class SystemPropertyUtil {

	public static final String CONF_DIR_PROPERTY = "user.dir.conf";
	public static final String DEFAULT_CONFIG_FILE = "application.properties";
	public static final String DEFAULT_LOGBACK_FILE = "logback-spring.xml";

	private SystemPropertyUtil() {
	}

	/**
	 * Locates the default conf directory. When the application is started from the
	 * backend module, the repository-level conf directory is also supported.
	 */
	public static void setSystemUserDir() {
		Path workingDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
		Path confDirectory = resolveConfDirectory(workingDirectory);
		System.setProperty(CONF_DIR_PROPERTY, confDirectory.toString());
		System.out.println("Config path: " + confDirectory);
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
