package com.changlu.blogloom.module.knowledge.service.impl;

import com.changlu.blogloom.entity.Category;
import com.changlu.blogloom.entity.User;
import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.module.knowledge.dao.KnowledgeNodeMapper;
import com.changlu.blogloom.module.knowledge.domain.dto.KnowledgeImportOptions;
import com.changlu.blogloom.module.knowledge.domain.entity.KnowledgeImportEntry;
import com.changlu.blogloom.module.knowledge.domain.entity.KnowledgeImportSession;
import com.changlu.blogloom.module.knowledge.domain.entity.KnowledgeNode;
import com.changlu.blogloom.module.knowledge.domain.enums.KnowledgeNodeType;
import com.changlu.blogloom.module.knowledge.service.KnowledgeImportTaskService;
import com.changlu.blogloom.service.BlogService;
import com.changlu.blogloom.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class KnowledgeImportWorker {
	private static final String DEFAULT_CATEGORY_NAME = "知识库";
	private final KnowledgeNodeMapper mapper;
	private final BlogService blogService;
	private final CategoryService categoryService;
	private final KnowledgeImportTaskService taskService;

	public KnowledgeImportWorker(KnowledgeNodeMapper mapper, BlogService blogService,
	                             CategoryService categoryService, KnowledgeImportTaskService taskService) {
		this.mapper = mapper;
		this.blogService = blogService;
		this.categoryService = categoryService;
		this.taskService = taskService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void execute(KnowledgeImportSession session, String taskId) throws IOException {
		KnowledgeImportOptions options = session.getOptions();
		Long rootId = options.getTargetParentId() == null ? 0L : options.getTargetParentId();
		if (rootId != 0L) {
			KnowledgeNode root = mapper.findById(rootId);
			if (root == null || !KnowledgeNodeType.DIR.name().equals(root.getType())) throw new BadRequestException("导入目标目录不存在");
		}
		Map<String, Long> directoryIds = new HashMap<>();
		directoryIds.put("", rootId);
		try (ZipFile zip = new ZipFile(session.getZipPath().toFile(), StandardCharsets.UTF_8)) {
			for (KnowledgeImportEntry entry : session.getEntries()) {
				if (entry.isDirectory()) importDirectory(entry, options, directoryIds, taskId);
				else importDocument(zip, entry, options, directoryIds, taskId);
			}
		}
	}

	private void importDirectory(KnowledgeImportEntry entry, KnowledgeImportOptions options,
	                             Map<String, Long> directoryIds, String taskId) {
		String parentPath = parent(entry.getPath());
		Long parentId = requireParent(directoryIds, parentPath);
		String name = filename(entry.getPath());
		KnowledgeNode existing = mapper.findSameName(parentId, name, KnowledgeNodeType.DIR.name());
		if (existing != null) {
			directoryIds.put(entry.getPath(), existing.getId());
			taskService.advance(taskId, entry.getPath(), true, true);
			return;
		}
		KnowledgeNode node = new KnowledgeNode();
		node.setParentId(parentId); node.setName(name); node.setType(KnowledgeNodeType.DIR.name());
		node.setSort(mapper.findMaxSort(parentId) + 1);
		mapper.insert(node);
		directoryIds.put(entry.getPath(), node.getId());
		taskService.advance(taskId, entry.getPath(), true, false);
	}

	private void importDocument(ZipFile zip, KnowledgeImportEntry entry, KnowledgeImportOptions options,
	                            Map<String, Long> directoryIds, String taskId) throws IOException {
		Long parentId = requireParent(directoryIds, parent(entry.getPath()));
		String name = stripMd(filename(entry.getPath()));
		KnowledgeNode existing = mapper.findSameName(parentId, name, KnowledgeNodeType.DOC.name());
		if (existing != null && "SKIP".equals(options.getConflictPolicy())) {
			taskService.advance(taskId, entry.getPath(), false, true);
			return;
		}
		if (existing != null) name = nextName(parentId, name, KnowledgeNodeType.DOC.name());
		ZipEntry source = zip.getEntry(entry.getSourceName());
		if (source == null) throw new BadRequestException("ZIP Entry 不存在: " + entry.getPath());
		String content = readUtf8(zip, source);
		com.changlu.blogloom.model.dto.Blog blog = buildBlog(name, content, options);
		blogService.saveBlog(blog);
		KnowledgeNode node = mapper.findByBlogId(blog.getId());
		if (node == null) throw new BadRequestException("博客知识库节点创建失败");
		mapper.updateName(node.getId(), name);
		mapper.move(node.getId(), parentId, mapper.findMaxSort(parentId) + 1);
		taskService.advance(taskId, entry.getPath(), false, false);
	}

	private com.changlu.blogloom.model.dto.Blog buildBlog(String title, String content, KnowledgeImportOptions options) {
		com.changlu.blogloom.model.dto.Blog blog = new com.changlu.blogloom.model.dto.Blog();
		blog.setTitle(title);
		blog.setContent(content);
		blog.setDescription(description(content, title));
		blog.setFirstPicture("");
		blog.setPublished(Boolean.TRUE.equals(options.getPublished()));
		blog.setRecommend(false); blog.setAppreciation(false); blog.setCommentEnabled(true); blog.setTop(false);
		blog.setViews(0); blog.setPassword("");
		int words = content.replaceAll("\\s+", "").length();
		blog.setWords(words); blog.setReadTime((int) Math.round(words / 200.0));
		Date now = new Date(); blog.setCreateTime(now); blog.setUpdateTime(now);
		Category category = categoryService.getCategoryByName(DEFAULT_CATEGORY_NAME);
		if (category == null) {
			category = new Category();
			category.setName(DEFAULT_CATEGORY_NAME);
			categoryService.saveCategory(category);
		}
		blog.setCategory(category);
		User user = new User(); user.setId(1L); blog.setUser(user);
		return blog;
	}

	private String description(String content, String fallback) {
		String plain = content.replaceAll("(?m)^#{1,6}\\s*", "").replaceAll("[`*_>\\[\\]()]", " ").replaceAll("\\s+", " ").trim();
		if (plain.isEmpty()) return fallback;
		return plain.substring(0, Math.min(200, plain.length()));
	}

	private String readUtf8(ZipFile zip, ZipEntry entry) throws IOException {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[8192]; int read;
			try (java.io.InputStream input = zip.getInputStream(entry)) {
				while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
			}
			return new String(output.toByteArray(), StandardCharsets.UTF_8);
		}
	}

	private String nextName(Long parentId, String base, String type) {
		for (int i = 1; i < 10000; i++) {
			String value = base + " (" + i + ")";
			if (mapper.findSameName(parentId, value, type) == null) return value;
		}
		throw new BadRequestException("无法生成不重名的文档名");
	}
	private Long requireParent(Map<String, Long> ids, String path) {
		Long id = ids.get(path); if (id == null) throw new BadRequestException("父目录未创建: " + path); return id;
	}
	private String parent(String path) { int index = path.lastIndexOf('/'); return index < 0 ? "" : path.substring(0, index); }
	private String filename(String path) { int index = path.lastIndexOf('/'); return index < 0 ? path : path.substring(index + 1); }
	private String stripMd(String name) { return name.toLowerCase(Locale.ROOT).endsWith(".md") ? name.substring(0, name.length() - 3) : name; }
}
