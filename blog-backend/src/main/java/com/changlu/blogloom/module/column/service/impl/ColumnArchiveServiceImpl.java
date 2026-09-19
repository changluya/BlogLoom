package com.changlu.blogloom.module.column.service.impl;

import com.changlu.blogloom.config.properties.UploadProperties;
import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.exception.PersistenceException;
import com.changlu.blogloom.module.column.dao.BlogColumnMapper;
import com.changlu.blogloom.module.column.domain.dto.ColumnImportItem;
import com.changlu.blogloom.module.column.domain.dto.ColumnImportOptions;
import com.changlu.blogloom.module.column.domain.entity.BlogColumn;
import com.changlu.blogloom.module.column.domain.entity.ColumnImportSession;
import com.changlu.blogloom.module.column.domain.vo.ColumnImportPreview;
import com.changlu.blogloom.module.column.domain.vo.ColumnTreeVo;
import com.changlu.blogloom.module.column.service.BlogColumnService;
import com.changlu.blogloom.module.column.service.ColumnArchiveService;
import com.changlu.blogloom.module.column.service.ColumnCoverStorageService;
import com.changlu.blogloom.util.JacksonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Service
public class ColumnArchiveServiceImpl implements ColumnArchiveService {
	private static final long MAX_ZIP_SIZE = 100L * 1024 * 1024;
	private static final long MAX_ENTRY_SIZE = 5L * 1024 * 1024;
	private static final long MAX_TOTAL_SIZE = 200L * 1024 * 1024;
	private static final long MAX_JSON_SIZE = 2L * 1024 * 1024;
	private static final int MAX_ARCHIVE_ENTRIES = 10000;
	private static final int MAX_DEPTH = 10;
	private static final String JSON_NAME = "columns.json";
	private static final Set<String> ICON_EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "webp"));

	private final Map<String, ColumnImportSession> sessions = new ConcurrentHashMap<>();
	private final BlogColumnService columnService;
	private final BlogColumnMapper columnMapper;
	private final ColumnCoverStorageService coverStorageService;
	private final UploadProperties uploadProperties;

	public ColumnArchiveServiceImpl(BlogColumnService columnService, BlogColumnMapper columnMapper,
	                                ColumnCoverStorageService coverStorageService, UploadProperties uploadProperties) {
		this.columnService = columnService;
		this.columnMapper = columnMapper;
		this.coverStorageService = coverStorageService;
		this.uploadProperties = uploadProperties;
	}

	@Override
	public ColumnImportPreview preview(MultipartFile file, ColumnImportOptions options) {
		validateOptions(options);
		if (file == null || file.isEmpty()) throw new BadRequestException("请选择 ZIP 文件");
		if (file.getSize() > MAX_ZIP_SIZE) throw new BadRequestException("ZIP 文件不能超过 100 MB");
		String filename = file.getOriginalFilename();
		if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".zip")) throw new BadRequestException("仅支持 ZIP 文件");
		Path temp = null;
		try {
			temp = Files.createTempFile("blogloom-columns-", ".zip");
			try (InputStream input = file.getInputStream()) {
				Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
			}
			ScanResult result = scan(temp);
			String token = UUID.randomUUID().toString();
			sessions.put(token, new ColumnImportSession(token, temp, result.items, result.entryMap, result.baseDir,
					copy(options), LocalDateTime.now().plusMinutes(30)));
			return new ColumnImportPreview(token, result.rootCount, result.childCount, result.iconCount,
					result.missingIconCount, result.paths);
		} catch (BadRequestException e) {
			deleteQuietly(temp);
			throw e;
		} catch (IOException e) {
			deleteQuietly(temp);
			String detail = e.getMessage() == null || e.getMessage().trim().isEmpty() ? "未知读取错误" : e.getMessage();
			throw new BadRequestException("ZIP 文件读取失败: " + detail, e);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public Map<String, Object> execute(String token) {
		ColumnImportSession session = sessions.remove(token);
		if (session == null || session.getExpiresAt().isBefore(LocalDateTime.now())) {
			if (session != null) deleteQuietly(session.getZipPath());
			throw new BadRequestException("预检会话已过期或已提交");
		}
		int createdRoot = 0;
		int createdChild = 0;
		int skipped = 0;
		int importedIcon = 0;
		try (ZipFile zip = new ZipFile(session.getZipPath().toFile(), StandardCharsets.UTF_8)) {
			for (ColumnImportItem rootItem : session.getItems()) {
				String rootTitle = rootItem.getTitle().trim();
				BlogColumn existingRoot = columnMapper.findByName(0L, rootTitle);
				Long rootId;
				if (existingRoot != null && skipExisting(session)) {
					rootId = existingRoot.getId();
					skipped++;
				} else {
					if (existingRoot != null) rootTitle = uniqueName(0L, rootTitle);
					rootId = createColumn(0L, rootTitle, rootItem.getDescription(), rootItem.getOrder(), session.getOptions().getPublished());
					createdRoot++;
				}
				if (applyCover(rootId, rootItem.getIcon(), zip, session)) importedIcon++;
				List<ColumnImportItem> children = rootItem.getChildren();
				if (children == null) continue;
				for (ColumnImportItem childItem : children) {
					String childTitle = childItem.getTitle().trim();
					BlogColumn existingChild = columnMapper.findByName(rootId, childTitle);
					if (existingChild != null && skipExisting(session)) {
						skipped++;
						continue;
					}
					if (existingChild != null) childTitle = uniqueName(rootId, childTitle);
					Long childId = createColumn(rootId, childTitle, childItem.getDescription(), childItem.getOrder(), session.getOptions().getPublished());
					createdChild++;
					if (applyCover(childId, childItem.getIcon(), zip, session)) importedIcon++;
				}
			}
		} catch (IOException e) {
			throw new PersistenceException("读取导入文件失败", e);
		} finally {
			deleteQuietly(session.getZipPath());
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("createdRootCount", createdRoot);
		result.put("createdChildCount", createdChild);
		result.put("skippedCount", skipped);
		result.put("importedIconCount", importedIcon);
		return result;
	}

	@Override
	public void exportAll(HttpServletResponse response) throws IOException {
		List<ColumnTreeVo> tree = columnService.getAdminTree();
		List<ColumnImportItem> exportItems = new ArrayList<>();
		Map<String, byte[]> icons = new LinkedHashMap<>();
		Set<String> usedNames = new HashSet<>();
		for (ColumnTreeVo root : tree) {
			ColumnImportItem rootItem = toExportItem(root, icons, usedNames);
			if (root.getChildren() != null) {
				for (ColumnTreeVo child : root.getChildren()) rootItem.getChildren().add(toExportItem(child, icons, usedNames));
			}
			exportItems.add(rootItem);
		}
		response.setContentType("application/zip");
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setHeader("Content-Disposition", "attachment; filename=columns.zip");
		try (ZipOutputStream zip = new ZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8)) {
			zip.putNextEntry(new ZipEntry("columns/"));
			zip.closeEntry();
			zip.putNextEntry(new ZipEntry("columns/" + JSON_NAME));
			zip.write(JacksonUtils.writeValueAsString(exportItems).getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
			for (Map.Entry<String, byte[]> icon : icons.entrySet()) {
				zip.putNextEntry(new ZipEntry("columns/icons/" + icon.getKey()));
				zip.write(icon.getValue());
				zip.closeEntry();
			}
			zip.finish();
		}
	}

	private ScanResult scan(Path path) throws IOException {
		Map<String, String> entryMap = new LinkedHashMap<>();
		String jsonKey = null;
		String jsonSource = null;
		int rawCount = 0;
		long total = 0;
		try (ZipFile zip = new ZipFile(path.toFile(), StandardCharsets.UTF_8)) {
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (++rawCount > MAX_ARCHIVE_ENTRIES) throw new BadRequestException("ZIP Entry 数量超过 " + MAX_ARCHIVE_ENTRIES);
				if (entry.isDirectory()) continue;
				String normalized = normalize(entry.getName());
				if (normalized.isEmpty() || ignored(normalized)) continue;
				String key = normalized.toLowerCase(Locale.ROOT);
				if (entryMap.containsKey(key)) throw new BadRequestException("ZIP 内存在重名路径: " + normalized);
				entryMap.put(key, entry.getName());
				if (filename(normalized).equalsIgnoreCase(JSON_NAME)) {
					if (jsonKey != null) throw new BadRequestException("ZIP 中存在多个 " + JSON_NAME);
					jsonKey = key;
					jsonSource = entry.getName();
				}
				long size = entry.getSize();
				if (size > MAX_ENTRY_SIZE) throw new BadRequestException("文件不能超过 5 MB: " + normalized);
				total += Math.max(0, size);
				if (total > MAX_TOTAL_SIZE) throw new BadRequestException("ZIP 解压总大小超过限制");
			}
			if (jsonSource == null) throw new BadRequestException("ZIP 中缺少 " + JSON_NAME);
			String content = readText(zip, jsonSource, MAX_JSON_SIZE);
			List<ColumnImportItem> items = JacksonUtils.readValue(content, new TypeReference<List<ColumnImportItem>>() {});
			if (items == null || items.isEmpty()) throw new BadRequestException(JSON_NAME + " 中没有可导入的专栏");
			String baseDir = parent(jsonKey);
			return validate(items, entryMap, baseDir);
		}
	}

	private ScanResult validate(List<ColumnImportItem> items, Map<String, String> entryMap, String baseDir) {
		int rootCount = 0;
		int childCount = 0;
		int iconCount = 0;
		int missingIconCount = 0;
		List<String> paths = new ArrayList<>();
		Set<String> rootNames = new HashSet<>();
		for (ColumnImportItem root : items) {
			String title = requireTitle(root.getTitle(), "一级专栏");
			if (!rootNames.add(title)) throw new BadRequestException("同级专栏名称重复: " + title);
			validateDescription(root.getDescription(), title);
			rootCount++;
			paths.add(title);
			if (hasIcon(root.getIcon())) {
				if (resolveIconKey(root.getIcon(), entryMap, baseDir) != null) iconCount++; else missingIconCount++;
			}
			List<ColumnImportItem> children = root.getChildren();
			if (children == null) continue;
			Set<String> childNames = new HashSet<>();
			for (ColumnImportItem child : children) {
				if (child.getChildren() != null && !child.getChildren().isEmpty()) {
					throw new BadRequestException("专栏最多支持两级: " + child.getTitle());
				}
				String childTitle = requireTitle(child.getTitle(), "子专栏");
				if (!childNames.add(childTitle)) throw new BadRequestException("同级专栏名称重复: " + childTitle);
				validateDescription(child.getDescription(), childTitle);
				childCount++;
				paths.add(title + " / " + childTitle);
				if (hasIcon(child.getIcon())) {
					if (resolveIconKey(child.getIcon(), entryMap, baseDir) != null) iconCount++; else missingIconCount++;
				}
			}
		}
		return new ScanResult(items, entryMap, baseDir, rootCount, childCount, iconCount, missingIconCount, paths);
	}

	private Long createColumn(Long parentId, String name, String description, Integer order, Boolean published) {
		BlogColumn column = new BlogColumn();
		column.setParentId(parentId);
		column.setName(name);
		column.setDescription(description == null ? "" : description.trim());
		column.setCover("");
		column.setSort(order == null ? columnMapper.findMaxSort(parentId) + 10 : order);
		column.setPublished(published == null || published);
		Date now = new Date();
		column.setCreateTime(now);
		column.setUpdateTime(now);
		if (columnMapper.insert(column) != 1) throw new PersistenceException("创建专栏失败");
		return column.getId();
	}

	private boolean applyCover(Long columnId, String icon, ZipFile zip, ColumnImportSession session) throws IOException {
		if (!hasIcon(icon)) return false;
		String key = resolveIconKey(icon, session.getEntryMap(), session.getBaseDir());
		if (key == null) return false;
		String source = session.getEntryMap().get(key);
		ZipEntry entry = source == null ? null : zip.getEntry(source);
		if (entry == null) return false;
		String name = filename(source);
		if (!supportedIcon(name)) return false;
		byte[] content = readBytes(zip, entry, MAX_ENTRY_SIZE);
		String url = coverStorageService.save(columnId, name, content);
		if (columnMapper.updateCover(columnId, url) != 1) throw new PersistenceException("更新专栏图片失败");
		return true;
	}

	private ColumnImportItem toExportItem(BlogColumn column, Map<String, byte[]> icons, Set<String> usedNames) {
		ColumnImportItem item = new ColumnImportItem();
		item.setTitle(column.getName());
		item.setDescription(column.getDescription());
		item.setOrder(column.getSort());
		byte[] content = readCover(column);
		if (content != null) {
			String name = uniqueIconName(filename(column.getCover()), usedNames);
			icons.put(name, content);
			item.setIcon("icons/" + name);
		}
		return item;
	}

	private byte[] readCover(BlogColumn column) {
		String cover = column.getCover();
		if (cover == null || cover.trim().isEmpty()) return null;
		String fileName = filename(cover);
		if (fileName.isEmpty() || !supportedIcon(fileName)) return null;
		Path directory = Paths.get(uploadProperties.getBlogColumnPath(column.getId())).toAbsolutePath().normalize();
		Path target = directory.resolve(fileName).normalize();
		if (!target.startsWith(directory) || !Files.isRegularFile(target)) return null;
		try {
			return Files.readAllBytes(target);
		} catch (IOException e) {
			return null;
		}
	}

	private String resolveIconKey(String icon, Map<String, String> entryMap, String baseDir) {
		String value = icon.replace('\\', '/').trim();
		while (value.startsWith("/")) value = value.substring(1);
		if (value.isEmpty()) return null;
		String direct = value.toLowerCase(Locale.ROOT);
		if (entryMap.containsKey(direct)) return direct;
		if (!baseDir.isEmpty()) {
			String prefixed = (baseDir + "/" + value).toLowerCase(Locale.ROOT);
			if (entryMap.containsKey(prefixed)) return prefixed;
		}
		return null;
	}

	private String requireTitle(String title, String label) {
		if (title == null || title.trim().isEmpty()) throw new BadRequestException(label + "名称不能为空");
		String value = title.trim();
		if (value.length() > 100) throw new BadRequestException("专栏名称不能超过100个字符: " + value);
		return value;
	}

	private void validateDescription(String description, String title) {
		if (description != null && description.length() > 500) throw new BadRequestException("专栏简介不能超过500个字符: " + title);
	}

	private boolean hasIcon(String icon) { return icon != null && !icon.trim().isEmpty(); }

	private boolean supportedIcon(String name) {
		int dot = name.lastIndexOf('.');
		return dot >= 0 && ICON_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
	}

	private boolean skipExisting(ColumnImportSession session) { return "SKIP".equals(session.getOptions().getConflictPolicy()); }

	private String uniqueName(Long parentId, String name) {
		for (int i = 1; ; i++) {
			String candidate = name + " (" + i + ")";
			if (columnMapper.findByName(parentId, candidate) == null) return candidate;
		}
	}

	private String uniqueIconName(String name, Set<String> used) {
		String value = name == null || name.trim().isEmpty() ? "icon.png" : name.trim();
		if (used.add(value.toLowerCase(Locale.ROOT))) return value;
		String base = value;
		String extension = "";
		int dot = value.lastIndexOf('.');
		if (dot > 0) {
			base = value.substring(0, dot);
			extension = value.substring(dot);
		}
		for (int i = 1; ; i++) {
			String candidate = base + " (" + i + ")" + extension;
			if (used.add(candidate.toLowerCase(Locale.ROOT))) return candidate;
		}
	}

	private byte[] readBytes(ZipFile zip, ZipEntry entry, long max) throws IOException {
		try (InputStream input = zip.getInputStream(entry); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[8192];
			int read;
			while ((read = input.read(buffer)) != -1) {
				if ((long) output.size() + read > max) throw new BadRequestException("文件超过大小限制: " + entry.getName());
				output.write(buffer, 0, read);
			}
			return output.toByteArray();
		}
	}

	private String readText(ZipFile zip, String sourceName, long max) throws IOException {
		ZipEntry entry = zip.getEntry(sourceName);
		if (entry == null) throw new BadRequestException("ZIP Entry 不存在: " + sourceName);
		byte[] bytes = readBytes(zip, entry, max);
		try {
			return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
		} catch (Exception e) {
			throw new BadRequestException(JSON_NAME + " 不是有效 UTF-8 编码");
		}
	}

	private String normalize(String name) {
		if (name == null || name.indexOf('\0') >= 0) throw new BadRequestException("ZIP Entry 路径不合法");
		String value = name.replace('\\', '/');
		while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
		if (value.startsWith("/") || value.matches("^[A-Za-z]:.*")) throw new BadRequestException("ZIP 不能包含绝对路径");
		String[] segments = value.split("/");
		for (String part : segments) if (part.isEmpty() || part.equals("..") || part.equals(".")) throw new BadRequestException("ZIP Entry 路径不合法");
		if (segments.length > MAX_DEPTH) throw new BadRequestException("ZIP 目录深度超过 " + MAX_DEPTH);
		return value;
	}

	private boolean ignored(String path) {
		if (path.equalsIgnoreCase("__MACOSX") || path.toLowerCase(Locale.ROOT).startsWith("__macosx/")) return true;
		for (String segment : path.split("/")) if (segment.startsWith(".")) return true;
		return false;
	}

	private String parent(String path) {
		int slash = path.lastIndexOf('/');
		return slash < 0 ? "" : path.substring(0, slash);
	}

	private String filename(String path) {
		if (path == null) return "";
		String value = path.replace('\\', '/');
		int slash = value.lastIndexOf('/');
		return slash < 0 ? value : value.substring(slash + 1);
	}

	private void validateOptions(ColumnImportOptions options) {
		if (options == null) throw new BadRequestException("导入参数不能为空");
		if (!"SKIP".equals(options.getConflictPolicy()) && !"RENAME".equals(options.getConflictPolicy())) throw new BadRequestException("冲突策略不正确");
		if (options.getPublished() == null) options.setPublished(Boolean.TRUE);
	}

	private ColumnImportOptions copy(ColumnImportOptions source) {
		ColumnImportOptions value = new ColumnImportOptions();
		value.setConflictPolicy(source.getConflictPolicy());
		value.setPublished(source.getPublished());
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

	@PreDestroy
	public void destroy() { for (ColumnImportSession session : sessions.values()) deleteQuietly(session.getZipPath()); }

	private static class ScanResult {
		final List<ColumnImportItem> items;
		final Map<String, String> entryMap;
		final String baseDir;
		final int rootCount;
		final int childCount;
		final int iconCount;
		final int missingIconCount;
		final List<String> paths;

		ScanResult(List<ColumnImportItem> items, Map<String, String> entryMap, String baseDir, int rootCount, int childCount,
		           int iconCount, int missingIconCount, List<String> paths) {
			this.items = items;
			this.entryMap = entryMap;
			this.baseDir = baseDir;
			this.rootCount = rootCount;
			this.childCount = childCount;
			this.iconCount = iconCount;
			this.missingIconCount = missingIconCount;
			this.paths = paths;
		}
	}
}
