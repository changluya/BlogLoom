package com.changlu.blogloom.service;

import com.changlu.blogloom.model.dto.Blog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 负责识别 Markdown/HTML 中的本地资源，并在博客保存或编辑时完成资源归档。
 */
@Service
public class BlogResourceService {
	private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("(!?\\[[^\\]]*\\]\\()([^\\s)]+)([^)]*\\))");
	private static final Pattern HTML_ATTRIBUTE_PATTERN = Pattern.compile("(?i)(\\b(?:src|href|poster)\\s*=\\s*)(['\"])([^'\"\\s]+)(\\2)");

	@Autowired
	private LocalResourceStorageService localResourceStorageService;

	/**
	 * 将正文、描述和首图中的 /static/tmp/** 资源复制到博客目录，并重写链接。
	 */
	public void reconcileBlogResources(Long blogId, Blog blog) {
		// 同一个临时资源可能同时出现在首图、描述和正文中，使用 Set 统一收集并去重。
		Set<String> temporaryResources = new HashSet<>();
		blog.setFirstPicture(rewriteUrl(blog.getFirstPicture(), blogId, temporaryResources));
		blog.setDescription(rewriteContent(blog.getDescription(), blogId, temporaryResources));
		blog.setContent(rewriteContent(blog.getContent(), blogId, temporaryResources));
		// 只有 copyToBlog 成功后才会记录临时资源，所有引用重写完成后再统一删除源文件。
		for (String temporaryResource : temporaryResources) {
			localResourceStorageService.deleteTemporaryResource(temporaryResource);
		}
	}

	/**
	 * 归档并重写文本中由 BlogLoom 管理的本地资源链接。
	 *
	 * <p>处理流程：</p>
	 * <ol>
	 *     <li>扫描 Markdown 图片和链接语法，将可管理资源复制到当前博客目录并替换 URL。</li>
	 *     <li>在 Markdown 处理结果上继续扫描 HTML 的 src、href 和 poster 属性并重写。</li>
	 *     <li>将已成功归档的 tmp 资源记录到 temporaryResources，由外层流程统一清理。</li>
	 * </ol>
	 *
	 * <p>非本地受管资源、非法 URL 或归档失败的链接均保持原样。</p>
	 *
	 * @param content            待处理的 Markdown 或 HTML 文本
	 * @param blogId             资源归档所属的博客 ID
	 * @param temporaryResources 成功归档、待后续删除的临时资源相对路径集合
	 * @return 完成本地资源链接重写后的文本
	 */
	private String rewriteContent(String content, Long blogId, Set<String> temporaryResources) {
		if (content == null || content.isEmpty()) {
			return content;
		}

		// 步骤 1：处理 Markdown 语法，分组 2 是需要重写的 URL，其余语法结构保持不变。
		Matcher markdownMatcher = MARKDOWN_LINK_PATTERN.matcher(content);
		StringBuffer markdownResult = new StringBuffer();
		while (markdownMatcher.find()) {
			String rewrittenUrl = rewriteUrl(markdownMatcher.group(2), blogId, temporaryResources);
			String replacement = markdownMatcher.group(1) + rewrittenUrl + markdownMatcher.group(3);
			markdownMatcher.appendReplacement(markdownResult, Matcher.quoteReplacement(replacement));
		}
		markdownMatcher.appendTail(markdownResult);

		// 步骤 2：基于上一步结果处理内嵌 HTML，避免两类语法混用时遗漏资源。
		Matcher htmlMatcher = HTML_ATTRIBUTE_PATTERN.matcher(markdownResult.toString());
		StringBuffer htmlResult = new StringBuffer();
		while (htmlMatcher.find()) {
			String replacement = htmlMatcher.group(1) + htmlMatcher.group(2)
					+ rewriteUrl(htmlMatcher.group(3), blogId, temporaryResources) + htmlMatcher.group(4);
			htmlMatcher.appendReplacement(htmlResult, Matcher.quoteReplacement(replacement));
		}
		htmlMatcher.appendTail(htmlResult);
		return htmlResult.toString();
	}

	/**
	 * 将单个受管本地资源归档到指定博客目录，并返回归档后的访问地址。
	 *
	 * <p>核心路径流转（以 blogId=100 为例）：</p>
	 * <pre>
	 * 来源 URL： /static/tmp/example.png?width=800#preview
	 * 来源文件： {uploadRoot}/tmp/example.png
	 *                    ↓ copyToBlog
	 * 归档文件： {uploadRoot}/blogs/100/example.png
	 * 结果 URL： {blog.api}/static/blogs/100/example.png?width=800#preview
	 * </pre>
	 *
	 * <p>已归档资源如 {@code /static/blogs/旧博客ID/example.png} 也会复制到
	 * {@code {uploadRoot}/blogs/当前博客ID/example.png}，便于复制或重新归属博客时迁移资源。</p>
	 *
	 * @param url                原始资源 URL，可包含查询参数和锚点
	 * @param blogId             目标博客 ID，决定归档目录 {@code blogs/{blogId}/}
	 * @param temporaryResources 成功归档后可安全清理的 {@code tmp/} 源资源集合
	 * @return 归档后的访问 URL；非受管资源或源文件不存在时返回原 URL
	 */
	private String rewriteUrl(String url, Long blogId, Set<String> temporaryResources) {
		if (url == null || url.isEmpty()) {
			return url;
		}
		// 步骤 1：先拆出 ?query/#fragment，文件归档只使用主路径，最终再原样拼回后缀。
		int suffixIndex = findSuffixIndex(url);
		String mainPart = suffixIndex < 0 ? url : url.substring(0, suffixIndex);
		String suffix = suffixIndex < 0 ? "" : url.substring(suffixIndex);
		// 步骤 2：将 /static/tmp/**、/image/** 或 /static/blogs/** 等 URL 转为 uploadRoot 下的相对路径。
		String managedPath = toManagedRelativePath(mainPart);
		if (managedPath == null) {
			return url;
		}
		// 步骤 3：从 uploadRoot/{managedPath} 复制到 uploadRoot/blogs/{blogId}/**。
		String archivedPath = localResourceStorageService.copyToBlog(blogId, managedPath);
		if (archivedPath == null) {
			return url;
		}
		// 临时文件仅在复制成功后才登记，防止归档失败时误删源文件。
		if (managedPath.replace('\\', '/').startsWith("tmp/")) {
			temporaryResources.add(managedPath);
		}
		// 步骤 4：构造 {blog.api}/static/blogs/{blogId}/** 并恢复原 URL 的查询参数/锚点。
		return localResourceStorageService.buildAccessUrl(archivedPath) + suffix;
	}

	private String toManagedRelativePath(String url) {
		String path = url;
		try {
			URI uri = new URI(url);
			if (uri.isAbsolute()) {
				if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
					return null;
				}
				path = uri.getPath();
			}
		} catch (URISyntaxException e) {
			return null;
		}
		if (path == null) {
			return null;
		}
		while (path.startsWith("/")) {
			path = path.substring(1);
		}
		if (path.startsWith("static/")) {
			return path.substring("static/".length());
		}
		if (path.startsWith("image/")) {
			return path.substring("image/".length());
		}
		if (path.startsWith("upload/tmp/")) {
			return path.substring("upload/".length());
		}
		if (path.startsWith("upload/blogs/")) {
			return path.substring("upload/".length());
		}
		if (path.startsWith("tmp/") || path.startsWith("blogs/")) {
			return path;
		}
		return null;
	}

	private int findSuffixIndex(String url) {
		int queryIndex = url.indexOf('?');
		int fragmentIndex = url.indexOf('#');
		if (queryIndex < 0) {
			return fragmentIndex;
		}
		if (fragmentIndex < 0) {
			return queryIndex;
		}
		return Math.min(queryIndex, fragmentIndex);
	}
}
