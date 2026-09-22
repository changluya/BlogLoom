package com.changlu.blogloom.module.knowledge.service.impl;

import com.changlu.blogloom.entity.Category;
import com.changlu.blogloom.entity.Tag;
import com.changlu.blogloom.entity.User;
import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.module.column.dao.BlogColumnMapper;
import com.changlu.blogloom.module.column.domain.entity.BlogColumn;
import com.changlu.blogloom.module.knowledge.dao.KnowledgeNodeMapper;
import com.changlu.blogloom.module.knowledge.domain.dto.KnowledgeArticleMetadata;
import com.changlu.blogloom.module.knowledge.domain.dto.KnowledgeImportOptions;
import com.changlu.blogloom.module.knowledge.domain.entity.KnowledgeImportEntry;
import com.changlu.blogloom.module.knowledge.domain.entity.KnowledgeImportSession;
import com.changlu.blogloom.module.knowledge.domain.entity.KnowledgeNode;
import com.changlu.blogloom.module.knowledge.domain.enums.KnowledgeNodeType;
import com.changlu.blogloom.module.knowledge.service.KnowledgeImportTaskService;
import com.changlu.blogloom.module.knowledge.support.KnowledgeArticleMetadataParser;
import com.changlu.blogloom.module.knowledge.support.KnowledgeArticleMetadataParser.ParsedArticle;
import com.changlu.blogloom.module.knowledge.support.KnowledgeCoverExtractor;
import com.changlu.blogloom.service.BlogService;
import com.changlu.blogloom.service.CategoryService;
import com.changlu.blogloom.service.TagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @Description: 知识库导入的核心执行器，支持两种来源：
 * <ul>
 *     <li>ZIP 归档：目录与 Markdown 按压缩包结构还原（忽略元数据 knowledgeBasePath）；</li>
 *     <li>Markdown 文件：目录由每篇元数据 knowledgeBasePath 决定，缺失目录逐级创建。</li>
 * </ul>
 * <p>
 * 核心描述：把目录与 Markdown 还原成知识库树，并把每篇 Markdown 转成一篇博客。
 * 目录按层级复用/创建；每篇文档解析顶部 JSON 元数据后，标题取 title、描述取 articleSummary、
 * 首图取正文中 alt 为 coverImg 的图片；标签、分类遵循“先查库 -> 不存在则新建 -> 再关联”，
 * 专栏则只匹配已存在的并关联（不自动创建）；默认以“公开”状态入库。整个过程在一个事务内完成，任一文档失败即整体回滚。
 * <p>
 * 整体流程（由 MarkdownArchiveServiceImpl 在后台线程中调用）：
 * <ol>
 *     <li>按“父目录先于子节点”的顺序遍历预检阶段扫描出的 Entry；</li>
 *     <li>目录 Entry -> 在知识库树中创建/复用对应目录节点；</li>
 *     <li>Markdown Entry -> 解析元数据与正文 -> 创建博客 -> 关联标签/分类/专栏 -> 挂载为知识库文档节点。</li>
 * </ol>
 * <p>
 * 本类额外承载了“一键导入知识库”的业务规则：
 * <ol>
 *     <li>文章首图取正文中 alt 为 coverImg 的图片（Markdown {@code ![coverImg](url)} 或 HTML {@code <img alt="coverImg" src="url">}），无标记则无封面；</li>
 *     <li>标签（tags，3-5 个，逗号分隔）与分类（category，单个）若数据库不存在则自动创建，再与文章建立关联；</li>
 *     <li>标题取元数据 title，文章描述取元数据 articleSummary（150 字以内）；</li>
 *     <li>专栏取元数据 columns（多个，逗号分隔），仅匹配已存在的专栏并关联，未匹配到则不关联（不自动创建）；</li>
 *     <li>默认按“公开”状态导入（published 默认 true），且默认开启赞赏与评论；</li>
 *     <li>创建/更新时间取元数据 createTime/updateTime（格式 YYYY-MM-DD HH:mm:ss），缺省时使用导入时间。</li>
 * </ol>
 *
 * @Author: changlu
 * @Date: 2026-09-19
 */
@Service
public class KnowledgeImportWorker {
	/** 元数据未提供 category 时使用的兜底分类名 */
	private static final String DEFAULT_CATEGORY_NAME = "知识库";
	/** 标签名最大长度，超出则截断，避免超过数据库字段限制 */
	private static final int MAX_TAG_NAME_LENGTH = 100;
	/** 专栏名最大长度，超出则截断，避免超过数据库字段限制 */
	private static final int MAX_COLUMN_NAME_LENGTH = 100;
	/** 规则6：创建/更新时间支持的时间格式（标准为 YYYY-MM-DD HH:mm:ss，其余为兼容写法） */
	private static final String[] DATE_TIME_PATTERNS = {
			"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
			"yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM/dd",
			"yyyy年MM月dd日 HH:mm:ss", "yyyy年MM月dd日"
	};

	private final KnowledgeNodeMapper mapper;
	private final BlogService blogService;
	private final CategoryService categoryService;
	private final TagService tagService;
	private final BlogColumnMapper columnMapper;
	private final KnowledgeImportTaskService taskService;

	public KnowledgeImportWorker(KnowledgeNodeMapper mapper, BlogService blogService,
	                             CategoryService categoryService, TagService tagService,
	                             BlogColumnMapper columnMapper,
	                             KnowledgeImportTaskService taskService) {
		this.mapper = mapper;
		this.blogService = blogService;
		this.categoryService = categoryService;
		this.tagService = tagService;
		this.columnMapper = columnMapper;
		this.taskService = taskService;
	}

	/**
	 * 执行整个导入会话。
	 * <p>
	 * 说明：方法整体处于事务中，任一文件导入失败会回滚本次所有数据库写入，
	 * 保证“知识库树 + 博客 + 关联关系”的一致性。
	 *
	 * @param session 预检阶段构建的导入会话（ZIP 模式含临时压缩包路径，文件模式含文档字节；均含已排序 Entry 列表）
	 * @param taskId  内存级进度任务 ID，用于前端轮询导入进度
	 */
	@Transactional(rollbackFor = Exception.class)
	public void execute(KnowledgeImportSession session, String taskId) throws IOException {
		KnowledgeImportOptions options = session.getOptions();
		// 步骤1：确定导入根目录（0 表示知识库根节点），并校验目标目录必须是 DIR 类型
		Long rootId = options.getTargetParentId() == null ? 0L : options.getTargetParentId();
		if (rootId != 0L) {
			KnowledgeNode root = mapper.findById(rootId);
			if (root == null || !KnowledgeNodeType.DIR.name().equals(root.getType())) throw new BadRequestException("导入目标目录不存在");
		}
		// 步骤2：directoryIds 维护“导入路径 -> 已创建/复用的知识库目录节点 ID”映射，空路径即导入根目录
		Map<String, Long> directoryIds = new HashMap<>();
		directoryIds.put("", rootId);
		// 步骤3：逐个处理 Entry。Entry 已按目录深度升序排序，因此父目录一定先于子目录/文档被创建
		// ZIP 模式从压缩包 Entry 读取正文；Markdown 文件模式从会话缓存的文档字节读取正文
		if (session.isArchive()) {
			try (ZipFile zip = new ZipFile(session.getZipPath().toFile(), StandardCharsets.UTF_8)) {
				processEntries(session, entry -> readZipDocument(zip, entry), options, directoryIds, taskId);
			}
		} else {
			processEntries(session, entry -> readSessionDocument(session, entry), options, directoryIds, taskId);
		}
	}

	/**
	 * 按 Entry 顺序处理整个导入会话：目录先建、文档后建。
	 */
	private void processEntries(KnowledgeImportSession session, DocumentContentReader reader,
	                            KnowledgeImportOptions options, Map<String, Long> directoryIds,
	                            String taskId) throws IOException {
		for (KnowledgeImportEntry entry : session.getEntries()) {
			if (entry.isDirectory()) importDirectory(entry, options, directoryIds, taskId);
			else importDocument(reader, entry, options, directoryIds, taskId);
		}
	}

	/** 从 ZIP Entry 读取文档正文 */
	private String readZipDocument(ZipFile zip, KnowledgeImportEntry entry) throws IOException {
		ZipEntry source = zip.getEntry(entry.getSourceName());
		if (source == null) throw new BadRequestException("ZIP Entry 不存在: " + entry.getPath());
		return readUtf8(zip, source);
	}

	/** 从会话缓存的 Markdown 文件读取正文 */
	private String readSessionDocument(KnowledgeImportSession session, KnowledgeImportEntry entry) {
		byte[] bytes = session.getDocuments() == null ? null : session.getDocuments().get(entry.getPath());
		if (bytes == null) throw new BadRequestException("导入文档内容不存在: " + entry.getPath());
		return new String(bytes, StandardCharsets.UTF_8);
	}

	/** 文档正文读取器：屏蔽 ZIP / 文件两种来源差异 */
	@FunctionalInterface
	private interface DocumentContentReader {
		String read(KnowledgeImportEntry entry) throws IOException;
	}

	/**
	 * 处理目录 Entry：在知识库树中创建目录节点；若同父级下已存在同名目录则直接复用（幂等）。
	 */
	private void importDirectory(KnowledgeImportEntry entry, KnowledgeImportOptions options,
	                             Map<String, Long> directoryIds, String taskId) {
		// 步骤1：根据路径解析父目录，并从 directoryIds 中取出父目录节点 ID
		String parentPath = parent(entry.getPath());
		Long parentId = requireParent(directoryIds, parentPath);
		String name = filename(entry.getPath());
		// 步骤2：同父级同名目录已存在时直接复用，避免重复创建（目录不做 RENAME，天然幂等）
		KnowledgeNode existing = mapper.findSameName(parentId, name, KnowledgeNodeType.DIR.name());
		if (existing != null) {
			directoryIds.put(entry.getPath(), existing.getId());
			taskService.advance(taskId, entry.getPath(), true, true);
			return;
		}
		// 步骤3：新建目录节点，排序值取当前父级最大 sort + 1，保证追加在末尾
		KnowledgeNode node = new KnowledgeNode();
		node.setParentId(parentId); node.setName(name); node.setType(KnowledgeNodeType.DIR.name());
		node.setSort(mapper.findMaxSort(parentId) + 1);
		mapper.insert(node);
		// 步骤4：记录路径与节点 ID 的映射，供后续子节点解析父目录使用；并推进进度
		directoryIds.put(entry.getPath(), node.getId());
		taskService.advance(taskId, entry.getPath(), true, false);
	}

	/**
	 * 处理 Markdown 文档 Entry：解析元数据 -> 构建并保存博客 -> 关联标签/分类/专栏 -> 挂载为知识库文档节点。
	 */
	private void importDocument(DocumentContentReader reader, KnowledgeImportEntry entry, KnowledgeImportOptions options,
	                            Map<String, Long> directoryIds, String taskId) throws IOException {
		// 步骤1：解析文档所属父目录
		Long parentId = requireParent(directoryIds, parent(entry.getPath()));

		// 步骤2：解析 Markdown 顶部的 JSON 元数据代码块，得到 metadata 与移除该代码块后的正文 content
		ParsedArticle parsed = KnowledgeArticleMetadataParser.parse(reader.read(entry));
		KnowledgeArticleMetadata metadata = parsed.getMetadata();
		String content = parsed.getContent();

		// 步骤3（规则3）：标题取元数据 title，缺失时回退为文件名（去掉 .md 后缀）
		String fallbackName = stripMd(filename(entry.getPath()));
		String name = hasText(metadata == null ? null : metadata.getTitle()) ? metadata.getTitle().trim() : fallbackName;

		// 步骤4：按冲突策略处理同名文档。SKIP -> 跳过；RENAME -> 生成 "xxx (n)" 的新名字
		KnowledgeNode existing = mapper.findSameName(parentId, name, KnowledgeNodeType.DOC.name());
		if (existing != null && "SKIP".equals(options.getConflictPolicy())) {
			taskService.advance(taskId, entry.getPath(), false, true);
			return;
		}
		if (existing != null) name = nextName(parentId, name, KnowledgeNodeType.DOC.name());

		// 步骤5（规则3）：文章描述优先取元数据 articleSummary，缺失时从正文截取前 200 字兜底
		String description = hasText(metadata == null ? null : metadata.getArticleSummary())
				? metadata.getArticleSummary().trim() : description(content, name);

		// 步骤6（规则1）：首图取正文中 alt 为 coverImg 的图片（Markdown 或 HTML，取出现位置更靠前者），无则空串
		String firstPicture = firstPicture(content);

		// 步骤7（规则2）：解析标签，数据库不存在则自动创建
		List<Tag> tags = resolveTags(metadata);
		// 步骤8（规则2）：解析分类，数据库不存在则自动创建（缺省用“知识库”分类）
		Category category = resolveCategory(metadata);
		// 步骤9（规则4）：解析专栏，仅匹配已存在的专栏并关联，未匹配到则跳过（不创建，可空）
		List<Long> columnIds = resolveColumns(metadata);

		// 步骤9.1（规则6）：解析创建/更新时间，缺省时创建时间取当前时间、更新时间同创建时间
		Date now = new Date();
		Date createTime = parseDateTime(metadata == null ? null : metadata.getCreateTime(), now);
		Date updateTime = parseDateTime(metadata == null ? null : metadata.getUpdateTime(), createTime);

		// 步骤10（规则5）：构建博客实体（默认公开），保存博客、分类关联与专栏关联
		com.changlu.blogloom.model.dto.Blog blog = buildBlog(name, content, description, firstPicture, category, columnIds, createTime, updateTime, options);
		blogService.saveBlog(blog);
		// 步骤11（规则2）：逐条写入 blog_tag 关联表，完成标签与文章的关联
		for (Tag tag : tags) blogService.saveBlogTag(blog.getId(), tag.getId());

		// 步骤12：saveBlog 内部已按博客创建了根级知识库节点，这里取出该节点并移动到目标目录下
		KnowledgeNode node = mapper.findByBlogId(blog.getId());
		if (node == null) throw new BadRequestException("博客知识库节点创建失败");
		mapper.updateName(node.getId(), name);
		mapper.move(node.getId(), parentId, mapper.findMaxSort(parentId) + 1);
		// 步骤13：推进进度（记录当前文件路径、是否目录、是否跳过）
		taskService.advance(taskId, entry.getPath(), false, false);
	}

	/**
	 * 构建博客实体，完成“元数据 -> 博客字段”的映射。
	 * 规则3：title 即文章标题；规则5：published 默认公开，赞赏与评论默认开启。
	 */
	private com.changlu.blogloom.model.dto.Blog buildBlog(String title, String content, String description,
	                                                      String firstPicture, Category category, List<Long> columnIds,
	                                                      Date createTime, Date updateTime,
	                                                      KnowledgeImportOptions options) {
		com.changlu.blogloom.model.dto.Blog blog = new com.changlu.blogloom.model.dto.Blog();
		// 规则3：标题直接使用解析出的 name（来自 title）
		blog.setTitle(title);
		blog.setContent(content);
		// 规则3：description 来自 articleSummary
		blog.setDescription(description);
		// 规则1：firstPicture 来自正文中 alt 为 coverImg 的图片，空值写空串（数据库字段非空）
		blog.setFirstPicture(firstPicture == null ? "" : firstPicture);
		// 规则5：默认公开导入；published 为 null 时也视为公开；赞赏与评论默认开启
		blog.setPublished(options.getPublished() == null || options.getPublished());
		blog.setAppreciation(true); blog.setCommentEnabled(true);
		// 其余展示属性使用安全默认值
		blog.setRecommend(false); blog.setTop(false);
		blog.setViews(0); blog.setPassword("");
		// 依据正文字符数估算字数与阅读时长（按 200 字/分钟）
		int words = content.replaceAll("\\s+", "").length();
		blog.setWords(words); blog.setReadTime((int) Math.round(words / 200.0));
		// 规则6：创建/更新时间来自元数据（缺省已由调用方兜底），保证归档与排序按真实时间
		blog.setCreateTime(createTime); blog.setUpdateTime(updateTime);
		// 规则2：绑定分类；规则4：绑定专栏列表；导入统一归属到管理员用户
		blog.setCategory(category);
		blog.setColumnIds(columnIds == null ? new ArrayList<>() : columnIds);
		User user = new User(); user.setId(1L); blog.setUser(user);
		return blog;
	}

	/**
	 * 规则2：解析标签集合。
	 * <p>
	 * 核心描述：对每篇文章的每个标签都先按名称查库（tag_name 为 utf8mb4_general_ci，忽略大小写），
	 * 命中则直接复用已有标签，未命中则新建一条 tag 记录；随后由调用方写入 blog_tag 完成关联。
	 * 同一篇文章内的标签按名称去重，超长名称截断到 100 字符；新建标签 color 可为空。
	 */
	private List<Tag> resolveTags(KnowledgeArticleMetadata metadata) {
		List<Tag> tags = new ArrayList<>();
		if (metadata == null || metadata.getTags() == null) return tags;
		Set<String> seen = new HashSet<>();
		for (String raw : metadata.getTags()) {
			if (!hasText(raw)) continue;
			String tagName = raw.trim();
			if (tagName.length() > MAX_TAG_NAME_LENGTH) tagName = tagName.substring(0, MAX_TAG_NAME_LENGTH);
			if (!seen.add(tagName)) continue;
			Tag tag = tagService.getTagByName(tagName);
			if (tag == null) {
				tag = new Tag();
				tag.setName(tagName);
				tagService.saveTag(tag);
			}
			tags.add(tag);
		}
		return tags;
	}

	/**
	 * 规则2：解析分类。
	 * <p>
	 * 核心描述：先按分类名查库，命中则复用，未命中则新建 category 记录；返回的分类通过
	 * blog.category_id 在插入博客时一并落库。元数据未提供分类时兜底使用默认分类“知识库”。
	 */
	private Category resolveCategory(KnowledgeArticleMetadata metadata) {
		String name = metadata != null && hasText(metadata.getCategory()) ? metadata.getCategory().trim() : DEFAULT_CATEGORY_NAME;
		Category category = categoryService.getCategoryByName(name);
		if (category == null) {
			category = new Category();
			category.setName(name);
			categoryService.saveCategory(category);
		}
		return category;
	}

	/**
	 * 规则4：解析所属专栏（多个）。
	 * <p>
	 * 核心描述：专栏字段为 columns（逗号分隔，可多个），迁移时**只匹配、不创建**——数据库中已有则关联，没有则跳过（不新建专栏）。
	 * 支持两种写法：
	 * <ul>
	 *     <li>“父专栏/子专栏”：先按名称找到一级专栏，再在其下按名称找到子专栏；</li>
	 *     <li>“专栏名”：在所有层级中按名称匹配（含二级专栏）。</li>
	 * </ul>
	 * 未匹配到的专栏不会自动创建，也不会与文章建立关联。
	 */
	private List<Long> resolveColumns(KnowledgeArticleMetadata metadata) {
		if (metadata == null) return Collections.emptyList();
		List<Long> columnIds = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (String raw : metadata.allColumns()) {
			if (!hasText(raw)) continue;
			String spec = raw.trim();
			if (spec.length() > MAX_COLUMN_NAME_LENGTH) spec = spec.substring(0, MAX_COLUMN_NAME_LENGTH);
			if (!seen.add(spec)) continue;
			BlogColumn column = matchColumn(spec);
			if (column != null) columnIds.add(column.getId());
		}
		return columnIds;
	}

	/**
	 * 按“父/子”路径或全层级名称匹配已存在的专栏，匹配不到返回 null（不创建）。
	 */
	private BlogColumn matchColumn(String spec) {
		int slash = spec.indexOf('/');
		if (slash > 0 && slash < spec.length() - 1) {
			String parentName = truncateColumnName(spec.substring(0, slash));
			String childName = truncateColumnName(spec.substring(slash + 1));
			BlogColumn parent = columnMapper.findByName(0L, parentName);
			return parent == null ? null : columnMapper.findByName(parent.getId(), childName);
		}
		return columnMapper.findByNameAnyLevel(spec);
	}

	private String truncateColumnName(String name) {
		String value = name == null ? "" : name.trim();
		return value.length() > MAX_COLUMN_NAME_LENGTH ? value.substring(0, MAX_COLUMN_NAME_LENGTH) : value;
	}

	/**
	 * 规则1：提取正文中的封面图链接（委托 {@link KnowledgeCoverExtractor}）。
	 * 仅识别 alt 为 coverImg 的图片，无标记则返回空串。
	 */
	private String firstPicture(String content) {
		return KnowledgeCoverExtractor.extract(content);
	}

	/**
	 * 规则6：解析创建/更新时间，支持多种常见格式，解析失败时返回兜底时间。
	 */
	private Date parseDateTime(String value, Date fallback) {
		if (!hasText(value)) return fallback;
		String text = value.trim();
		for (String pattern : DATE_TIME_PATTERNS) {
			try {
				SimpleDateFormat format = new SimpleDateFormat(pattern);
				format.setLenient(false);
				return format.parse(text);
			} catch (ParseException ignored) {
				// 尝试下一个格式
			}
		}
		return fallback;
	}

	/**
	 * 规则3兜底：元数据没有 articleSummary 时，从正文剥离 Markdown 标记后截取前 200 字作为描述。
	 */
	private String description(String content, String fallback) {
		String plain = content.replaceAll("(?m)^#{1,6}\\s*", "").replaceAll("[`*_>\\[\\]()]", " ").replaceAll("\\s+", " ").trim();
		if (plain.isEmpty()) return fallback;
		return plain.substring(0, Math.min(200, plain.length()));
	}

	private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }

	private String readUtf8(ZipFile zip, ZipEntry entry) throws IOException {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[8192]; int read;
			try (java.io.InputStream input = zip.getInputStream(entry)) {
				while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
			}
			return new String(output.toByteArray(), StandardCharsets.UTF_8);
		}
	}

	/** 生成同父级下不重名的文档名：base (1)、base (2) ... */
	private String nextName(Long parentId, String base, String type) {
		for (int i = 1; i < 10000; i++) {
			String value = base + " (" + i + ")";
			if (mapper.findSameName(parentId, value, type) == null) return value;
		}
		throw new BadRequestException("无法生成不重名的文档名");
	}
	/** 从 directoryIds 中取父目录节点 ID，取不到说明父目录尚未创建 */
	private Long requireParent(Map<String, Long> ids, String path) {
		Long id = ids.get(path); if (id == null) throw new BadRequestException("父目录未创建: " + path); return id;
	}
	private String parent(String path) { int index = path.lastIndexOf('/'); return index < 0 ? "" : path.substring(0, index); }
	private String filename(String path) { int index = path.lastIndexOf('/'); return index < 0 ? path : path.substring(index + 1); }
	private String stripMd(String name) { return name.toLowerCase(Locale.ROOT).endsWith(".md") ? name.substring(0, name.length() - 3) : name; }
}
