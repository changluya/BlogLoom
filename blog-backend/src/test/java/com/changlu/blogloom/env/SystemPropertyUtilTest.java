package com.changlu.blogloom.env;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemPropertyUtilTest {

	@TempDir
	Path tempDir;

	@Test
	void shouldUseConfDirectoryUnderWorkingDirectory() throws IOException {
		Path confDirectory = Files.createDirectory(tempDir.resolve("conf"));

		assertEquals(confDirectory, SystemPropertyUtil.resolveConfDirectory(tempDir));
	}

	@Test
	void shouldUseParentConfDirectoryWhenStartedFromModule() throws IOException {
		Path confDirectory = Files.createDirectory(tempDir.resolve("conf"));
		Path moduleDirectory = Files.createDirectory(tempDir.resolve("blog-backend"));

		assertEquals(confDirectory, SystemPropertyUtil.resolveConfDirectory(moduleDirectory));
	}

	@Test
	void shouldReturnDefaultConfDirectoryEvenBeforeItExists() {
		assertEquals(tempDir.resolve("conf"), SystemPropertyUtil.resolveConfDirectory(tempDir));
	}
}
