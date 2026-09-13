package com.changlu.blogloom;

import com.changlu.blogloom.env.SystemPropertyUtil;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class BlogApiApplication {

	public static void main(String[] args) {
		SystemPropertyUtil.setSystemUserDir();

		Map<String, Object> defaultProperties = new HashMap<>();
		defaultProperties.put("spring.config.location", "file:${user.dir.conf}/application.properties");
		// BlogLoom uses Spring Boot 2.2.x, whose static resource property has the spring.resources prefix.
		defaultProperties.put("spring.resources.static-locations",
				"file:${user.dir.conf}/static/,classpath:/META-INF/resources/,classpath:/resources/,classpath:/static/,classpath:/public/");

		// Only select the external Logback file when it exists; otherwise use the packaged default.
		if (Files.isRegularFile(SystemPropertyUtil.getDefaultLogbackFile())) {
			defaultProperties.put("logging.config", "file:${user.dir.conf}/logback-spring.xml");
		}

		new SpringApplicationBuilder(BlogApiApplication.class)
				.properties(defaultProperties)
				.run(args);
	}

}
