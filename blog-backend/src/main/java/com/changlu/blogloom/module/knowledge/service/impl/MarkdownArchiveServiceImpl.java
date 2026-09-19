package com.changlu.blogloom.module.knowledge.service.impl;

import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.module.knowledge.domain.dto.KnowledgeImportOptions;
import com.changlu.blogloom.module.knowledge.domain.entity.KnowledgeImportEntry;
import com.changlu.blogloom.module.knowledge.domain.entity.KnowledgeImportSession;
import com.changlu.blogloom.module.knowledge.domain.vo.KnowledgeImportPreview;
import com.changlu.blogloom.module.knowledge.dao.KnowledgeNodeMapper;
import com.changlu.blogloom.module.knowledge.domain.entity.KnowledgeNode;
import com.changlu.blogloom.module.knowledge.domain.enums.KnowledgeNodeType;
import com.changlu.blogloom.module.knowledge.service.KnowledgeImportTaskService;
import com.changlu.blogloom.module.knowledge.service.MarkdownArchiveService;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.file.*;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @Description: 知识库 ZIP 导入导出的入口服务。
 * <p>
 * 导入分为两个阶段：
 * <ol>
 *     <li>preview：上传 ZIP -> 校验并扫描 Entry -> 生成一次性 token 与预检结果（不落库）；</li>
 *     <li>execute：凭 token 取回会话 -> 创建进度任务 -> 单线程异步执行 {@link KnowledgeImportWorker}。</li>
 * </ol>
 *
 * @Author: changlu
 * @Date: 2026-09-19
 */
@Service
public class MarkdownArchiveServiceImpl implements MarkdownArchiveService {
	private static final long MAX_ZIP_SIZE = 500L * 1024 * 1024;
	private static final long MAX_ENTRY_SIZE = 5L * 1024 * 1024;
	private static final long MAX_TOTAL_SIZE = 2L * 1024 * 1024 * 1024;
	private static final int MAX_ENTRIES = 2000;
	private static final int MAX_ARCHIVE_ENTRIES = 10000;
	private static final int MAX_DEPTH = 20;

	private final Map<String, KnowledgeImportSession> sessions = new ConcurrentHashMap<>();
	private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
			new SynchronousQueue<>(), runnable -> new Thread(runnable, "knowledge-import"), new ThreadPoolExecutor.AbortPolicy());
	private final AtomicBoolean running = new AtomicBoolean(false);
	private final KnowledgeImportTaskService taskService;
	private final KnowledgeImportWorker worker;
	private final KnowledgeNodeMapper nodeMapper;

	public MarkdownArchiveServiceImpl(KnowledgeImportTaskService taskService, KnowledgeImportWorker worker,
	                                  KnowledgeNodeMapper nodeMapper) {
		this.taskService = taskService;
		this.worker = worker;
		this.nodeMapper = nodeMapper;
	}

	/**
	 * 预检阶段：仅校验与扫描，不写入数据库。
	 * 步骤1 校验参数与文件；步骤2 落临时 ZIP；步骤3 扫描 Entry；
	 * 步骤4 生成 30 分钟有效的一次性 token 并缓存会话；步骤5 返回预检统计给前端确认。
	 */
	@Override
	public KnowledgeImportPreview preview(MultipartFile file, KnowledgeImportOptions options) {
		// 步骤1：校验导入参数与上传文件（非空、大小、扩展名）
		validateOptions(options);
		if (file == null || file.isEmpty()) throw new BadRequestException("请选择 ZIP 文件");
		if (file.getSize() > MAX_ZIP_SIZE) throw new BadRequestException("ZIP 文件不能超过 500 MB");
		String filename = file.getOriginalFilename();
		if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".zip")) throw new BadRequestException("仅支持 ZIP 文件");
		Path temp = null;
		try {
			// 步骤2：把上传流拷贝到临时文件，后续扫描与执行都基于该文件
			temp = Files.createTempFile("blogloom-knowledge-", ".zip");
			try (InputStream input = file.getInputStream()) {
				Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
			}
			// 步骤3：扫描 ZIP，得到去重、排序后的目录/文档 Entry 列表
			ScanResult result = scan(temp);
			// 步骤4：生成一次性 token，缓存会话（含临时文件路径、Entry、参数、过期时间）
			String token = UUID.randomUUID().toString();
			sessions.put(token, new KnowledgeImportSession(token, temp, result.entries, copy(options), LocalDateTime.now().plusMinutes(30)));
			// 步骤5：返回预检统计（目录数、文档数、忽略数、总大小、路径列表）
			return new KnowledgeImportPreview(token, result.directoryCount, result.documentCount, result.ignoredCount,
					result.totalBytes, result.paths);
		} catch (BadRequestException e) {
			deleteQuietly(temp);
			throw e;
		} catch (IOException e) {
			deleteQuietly(temp);
			String detail = e.getMessage() == null || e.getMessage().trim().isEmpty() ? "未知读取错误" : e.getMessage();
			throw new BadRequestException("ZIP 文件读取失败: " + detail, e);
		}
	}

	/**
	 * 执行阶段：凭预检 token 提交异步导入任务，立即返回 taskId 供前端轮询进度。
	 * 步骤1 取出并校验会话（一次性、有过期时间）；
	 * 步骤2 用 running 标志保证同一时刻仅有一个导入任务；
	 * 步骤3 创建进度任务并投递到单线程执行器；
	 * 步骤4 执行成功/失败分别更新任务状态，最后清理临时 ZIP 并释放标志。
	 */
	@Override
	public String execute(String token) {
		// 步骤1：会话是一次性的，remove 后即失效；校验存在性与过期时间
		KnowledgeImportSession session = sessions.remove(token);
		if (session == null || session.getExpiresAt().isBefore(LocalDateTime.now())) {
			if (session != null) deleteQuietly(session.getZipPath());
			throw new BadRequestException("预检会话已过期或已提交");
		}
		// 步骤2：通过 CAS 抢占运行标志，避免并发导入相互干扰；失败则归还会话
		if (!running.compareAndSet(false, true)) {
			sessions.put(token, session);
			throw new BadRequestException("当前已有知识库导入任务");
		}
		// 步骤3：按 Entry 总数创建进度任务，并把实际导入交给单线程执行器异步处理
		String taskId = taskService.create(session.getEntries().size());
		try {
			executor.execute(() -> {
				try {
					taskService.start(taskId);
					// 核心导入逻辑：目录/文档创建 + 博客与标签/分类/专栏关联
					worker.execute(session, taskId);
					taskService.succeed(taskId);
				} catch (Exception e) {
					taskService.fail(taskId, e.getMessage(), taskService.get(taskId).getCurrentPath());
				} finally {
					// 步骤4：无论成功失败都清理临时文件并释放运行标志
					deleteQuietly(session.getZipPath());
					running.set(false);
				}
			});
		} catch (RejectedExecutionException e) {
			running.set(false);
			sessions.put(token, session);
			throw new BadRequestException("导入任务繁忙，请稍后重试");
		}
		return taskId;
	}

	@Override
	public void exportAll(HttpServletResponse response) throws IOException {
		response.setContentType("application/zip");
		response.setHeader("Content-Disposition", "attachment; filename=blogloom-knowledge.zip");
		List<KnowledgeNode> nodes = nodeMapper.findAll();
		Map<Long, List<KnowledgeNode>> children = new LinkedHashMap<>();
		Set<Long> ids = new HashSet<>();
		for (KnowledgeNode node : nodes) ids.add(node.getId());
		for (KnowledgeNode node : nodes) {
			Long parentId = node.getParentId() == null || (!Objects.equals(node.getParentId(), 0L) && !ids.contains(node.getParentId())) ? 0L : node.getParentId();
			children.computeIfAbsent(parentId, key -> new ArrayList<>()).add(node);
		}
		Set<String> usedPaths = new HashSet<>();
		try (ZipOutputStream zip = new ZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8)) {
			writeExportChildren(zip, children, 0L, "", usedPaths, new HashSet<>());
			zip.finish();
		}
	}

	@Override
	public void exportDocument(Long nodeId, HttpServletResponse response) throws IOException {
		KnowledgeNode node = nodeMapper.findById(nodeId);
		if (node == null) throw new BadRequestException("知识库文档不存在");
		if (!KnowledgeNodeType.DOC.name().equals(node.getType())) throw new BadRequestException("仅支持下载文档节点");
		if (node.getBlogId() == null || nodeMapper.countBlog(node.getBlogId()) == 0) throw new BadRequestException("关联博客不存在");
		String content = nodeMapper.findBlogContent(node.getBlogId());
		String filename = sanitizeExportName(node.getName()) + ".md";
		String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8.name()).replace("+", "%20");
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType("text/markdown;charset=UTF-8");
		response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
		response.getOutputStream().write((content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
	}

	private void writeExportChildren(ZipOutputStream zip, Map<Long, List<KnowledgeNode>> children, Long parentId,
	                                 String parentPath, Set<String> usedPaths, Set<Long> visiting) throws IOException {
		for (KnowledgeNode node : children.getOrDefault(parentId, Collections.emptyList())) {
			if (!visiting.add(node.getId())) continue;
			String baseName = sanitizeExportName(node.getName());
			if (KnowledgeNodeType.DIR.name().equals(node.getType())) {
				String path = uniqueExportPath(parentPath + baseName + "/", usedPaths, true);
				zip.putNextEntry(new ZipEntry(path));
				zip.closeEntry();
				writeExportChildren(zip, children, node.getId(), path, usedPaths, visiting);
			} else if (node.getBlogId() != null) {
				String path = uniqueExportPath(parentPath + baseName + ".md", usedPaths, false);
				zip.putNextEntry(new ZipEntry(path));
				String content = nodeMapper.findBlogContent(node.getBlogId());
				if (content != null) zip.write(content.getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
			}
			visiting.remove(node.getId());
		}
	}

	private String sanitizeExportName(String name) {
		String value = name == null ? "untitled" : name.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
		return value.isEmpty() ? "untitled" : value;
	}

	private String uniqueExportPath(String path, Set<String> usedPaths, boolean directory) {
		String key = path.toLowerCase(Locale.ROOT);
		if (usedPaths.add(key)) return path;
		String suffix = directory ? "/" : "";
		String raw = directory ? path.substring(0, path.length() - 1) : path.substring(0, path.length() - 3);
		String extension = directory ? "" : ".md";
		for (int i = 1; ; i++) {
			String candidate = raw + " (" + i + ")" + extension + suffix;
			if (usedPaths.add(candidate.toLowerCase(Locale.ROOT))) return candidate;
		}
	}

	private ScanResult scan(Path path) throws IOException {
		List<KnowledgeImportEntry> accepted = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		long total = 0;
		int rawCount = 0;
		int acceptedCount = 0;
		int ignored = 0;
		try (ZipFile zip = new ZipFile(path.toFile(), StandardCharsets.UTF_8)) {
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
				if (++rawCount > MAX_ARCHIVE_ENTRIES) throw new BadRequestException("ZIP 原始 Entry 数量超过 " + MAX_ARCHIVE_ENTRIES);
				String normalized = normalize(zipEntry.getName());
				boolean directory = zipEntry.isDirectory() || zipEntry.getName().endsWith("/");
				boolean markdown = !directory && normalized.toLowerCase(Locale.ROOT).endsWith(".md");
				if (normalized.isEmpty() || ignored(normalized)) { ignored++; continue; }
				if (!directory && !markdown) { ignored++; continue; }
				if (++acceptedCount > MAX_ENTRIES) throw new BadRequestException("可导入目录和 Markdown 数量超过 " + MAX_ENTRIES);
				long entryBytes = directory ? 0 : zipEntry.getSize();
				if (entryBytes > MAX_ENTRY_SIZE) throw new BadRequestException("Markdown 文件不能超过 5 MB: " + normalized);
				byte[] markdownBytes = markdown ? readMarkdown(zip, zipEntry, normalized) : null;
				if (markdown && entryBytes < 0) entryBytes = markdownBytes.length;
				total += Math.max(0, entryBytes);
				if (total > MAX_TOTAL_SIZE) throw new BadRequestException("Markdown 解压总大小超过限制");
				if (markdown) validateUtf8(markdownBytes, normalized);
				String key = normalized.toLowerCase(Locale.ROOT);
				if (!seen.add(key)) throw new BadRequestException("ZIP 内存在重名路径: " + normalized);
				accepted.add(new KnowledgeImportEntry(zipEntry.getName(), normalized, directory, entryBytes));
			}
		}
		if (accepted.isEmpty()) throw new BadRequestException("ZIP 中没有可导入的 Markdown 或目录");
		stripSingleRoot(accepted);
		LinkedHashMap<String, KnowledgeImportEntry> all = new LinkedHashMap<>();
		for (KnowledgeImportEntry entry : accepted) {
			String[] segments = entry.getPath().split("/");
			String current = "";
			int limit = entry.isDirectory() ? segments.length : segments.length - 1;
			for (int i = 0; i < limit; i++) {
				current = current.isEmpty() ? segments[i] : current + "/" + segments[i];
				all.putIfAbsent("D:" + current, new KnowledgeImportEntry(null, current, true, 0));
			}
			if (!entry.isDirectory()) all.put("F:" + entry.getPath(), entry);
		}
		List<KnowledgeImportEntry> entries = new ArrayList<>(all.values());
		entries.sort(Comparator.comparingInt((KnowledgeImportEntry e) -> depth(e.getPath())).thenComparing(KnowledgeImportEntry::getPath));
		int dirs = (int) entries.stream().filter(KnowledgeImportEntry::isDirectory).count();
		List<String> paths = new ArrayList<>();
		for (KnowledgeImportEntry entry : entries) paths.add(entry.getPath());
		return new ScanResult(entries, dirs, entries.size() - dirs, ignored, total, paths);
	}

	private byte[] readMarkdown(ZipFile zip, ZipEntry entry, String path) throws IOException {
		try (InputStream input = zip.getInputStream(entry); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[8192];
			int read;
			while ((read = input.read(buffer)) != -1) {
				if ((long) output.size() + read > MAX_ENTRY_SIZE) throw new BadRequestException("Markdown 文件不能超过 5 MB: " + path);
				output.write(buffer, 0, read);
			}
			return output.toByteArray();
		}
	}

	private void stripSingleRoot(List<KnowledgeImportEntry> entries) {
		String root = null;
		for (KnowledgeImportEntry entry : entries) {
			int slash = entry.getPath().indexOf('/');
			if (slash < 0) continue;
			String value = entry.getPath().substring(0, slash);
			if (root == null) root = value;
			else if (!root.equalsIgnoreCase(value)) return;
		}
		if (root == null) return;
		for (KnowledgeImportEntry entry : entries) {
			if (!entry.getPath().equalsIgnoreCase(root) && !entry.getPath().toLowerCase(Locale.ROOT).startsWith(root.toLowerCase(Locale.ROOT) + "/")) return;
		}
		List<KnowledgeImportEntry> stripped = new ArrayList<>();
		for (KnowledgeImportEntry old : entries) {
			if (old.getPath().equalsIgnoreCase(root) && old.isDirectory()) continue;
			String value = old.getPath().substring(root.length() + 1);
			if (!value.isEmpty()) stripped.add(new KnowledgeImportEntry(old.getSourceName(), value, old.isDirectory(), old.getSize()));
		}
		entries.clear();
		entries.addAll(stripped);
	}

	private void validateUtf8(byte[] bytes, String path) {
		try {
			StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes));
		} catch (Exception e) {
			throw new BadRequestException("Markdown 不是有效 UTF-8 编码: " + path);
		}
	}

	private String normalize(String name) {
		if (name == null || name.indexOf('\0') >= 0) throw new BadRequestException("ZIP Entry 路径不合法");
		String value = name.replace('\\', '/');
		while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
		if (value.startsWith("/") || value.matches("^[A-Za-z]:.*")) throw new BadRequestException("ZIP 不能包含绝对路径");
		for (String part : value.split("/")) if (part.isEmpty() || part.equals("..") || part.equals(".")) throw new BadRequestException("ZIP Entry 路径不合法");
		if (depth(value) > MAX_DEPTH) throw new BadRequestException("ZIP 目录深度超过 " + MAX_DEPTH);
		return value;
	}

	private boolean ignored(String path) {
		if (path.equals("__MACOSX") || path.startsWith("__MACOSX/")) return true;
		for (String segment : path.split("/")) {
			if (segment.startsWith(".")) return true;
		}
		return false;
	}
	private int depth(String path) { return path.isEmpty() ? 0 : path.split("/").length; }
	private void validateOptions(KnowledgeImportOptions options) {
		if (options == null) throw new BadRequestException("导入参数不能为空");
		if (!"SKIP".equals(options.getConflictPolicy()) && !"RENAME".equals(options.getConflictPolicy())) throw new BadRequestException("冲突策略不正确");
	}
	private KnowledgeImportOptions copy(KnowledgeImportOptions source) {
		KnowledgeImportOptions value = new KnowledgeImportOptions();
		value.setTargetParentId(source.getTargetParentId());
		value.setPublished(source.getPublished()); value.setConflictPolicy(source.getConflictPolicy());
		return value;
	}
	private void deleteQuietly(Path path) { if (path != null) try { Files.deleteIfExists(path); } catch (IOException ignored) { } }
	@Scheduled(fixedDelay = 300000)
	public void cleanExpiredSessions() {
		LocalDateTime now = LocalDateTime.now();
		sessions.entrySet().removeIf(entry -> {
			if (entry.getValue().getExpiresAt().isAfter(now)) return false;
			deleteQuietly(entry.getValue().getZipPath());
			return true;
		});
	}
	@PreDestroy public void destroy() { executor.shutdownNow(); for (KnowledgeImportSession session : sessions.values()) deleteQuietly(session.getZipPath()); }

	private static class ScanResult {
		final List<KnowledgeImportEntry> entries; final int directoryCount; final int documentCount; final int ignoredCount; final long totalBytes; final List<String> paths;
		ScanResult(List<KnowledgeImportEntry> entries, int directoryCount, int documentCount, int ignoredCount, long totalBytes, List<String> paths) {
			this.entries = entries; this.directoryCount = directoryCount; this.documentCount = documentCount;
			this.ignoredCount = ignoredCount; this.totalBytes = totalBytes; this.paths = paths;
		}
	}
}
