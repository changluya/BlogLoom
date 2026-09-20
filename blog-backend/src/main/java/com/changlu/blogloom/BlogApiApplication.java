package com.changlu.blogloom;

import com.changlu.blogloom.env.SystemPropertyUtil;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableScheduling
public class BlogApiApplication {

	public static void main(String[] args) {
		SystemPropertyUtil.setSystemUserDir();

		Map<String, Object> defaultProperties = new HashMap<>();
		defaultProperties.put("spring.config.location", "file:${user.dir.conf}/application.properties");
		// BlogLoom uses Spring Boot 2.2.x, whose static resource property has the spring.resources prefix.
		// static/view 存放博客前台（映射到根路径），static 下其余内容（如 static/cms）按子路径访问。
		defaultProperties.put("spring.resources.static-locations",
				"file:${user.dir.conf}/static/view/,file:${user.dir.conf}/static/,classpath:/META-INF/resources/,classpath:/resources/,classpath:/static/,classpath:/public/");

		// Only select the external Logback file when it exists; otherwise use the packaged default.
		if (Files.isRegularFile(SystemPropertyUtil.getDefaultLogbackFile())) {
			defaultProperties.put("logging.config", "file:${user.dir.conf}/logback-spring.xml");
		}

		new SpringApplicationBuilder(BlogApiApplication.class)
				.properties(defaultProperties)
				.run(args);
	}

}
