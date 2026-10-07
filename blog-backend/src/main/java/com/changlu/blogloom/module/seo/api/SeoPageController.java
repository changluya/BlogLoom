package com.changlu.blogloom.module.seo.api;

import com.changlu.blogloom.exception.NotFoundException;
import com.changlu.blogloom.model.vo.ArchiveBlog;
import com.changlu.blogloom.model.vo.BlogDetail;
import com.changlu.blogloom.model.vo.BlogInfo;
import com.changlu.blogloom.model.vo.PageResult;
import com.changlu.blogloom.module.seo.support.SeoHtmlSupport;
import com.changlu.blogloom.module.seo.support.SeoMetaBuilder;
import com.changlu.blogloom.module.seo.service.SeoSiteLayoutService;
import com.changlu.blogloom.service.BlogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
public class SeoPageController {
	private final BlogService blogService;
	private final SeoMetaBuilder metaBuilder;
	private final SeoSiteLayoutService siteLayoutService;

	public SeoPageController(BlogService blogService, SeoMetaBuilder metaBuilder, SeoSiteLayoutService siteLayoutService) {
		this.blogService = blogService;
		this.metaBuilder = metaBuilder;
		this.siteLayoutService = siteLayoutService;
	}

	@GetMapping(value = "/blog/{id}", produces = "text/html")
	public String article(@PathVariable Long id, Model model) {
		try {
			BlogDetail article = blogService.getBlogByIdAndIsPublished(id);
			if (StringUtils.hasText(article.getPassword())) throw notFound();
			article.setContent(SeoHtmlSupport.renderInlineToc(SeoHtmlSupport.optimizeArticleHtml(article.getContent())));
			model.addAttribute("seo", metaBuilder.forArticle(article));
			model.addAttribute("article", article);
			model.addAttribute("layout", siteLayoutService.getLayout());
			model.addAttribute("toc", SeoHtmlSupport.extractToc(article.getContent()));
			return "seo/blog";
		} catch (NotFoundException e) {
			throw notFound();
		}
	}

	@GetMapping(value = "/home", produces = "text/html")
	public String home(@RequestParam(defaultValue = "1") Integer pageNum, Model model) {
		return listPage("首页", "最新博客文章", "/home",
				blogService.getBlogInfoListByIsPublished(pageNum, "createTime"), model);
	}

	@GetMapping(value = "/tag/{name}", produces = "text/html")
	public String tag(@PathVariable String name, @RequestParam(defaultValue = "1") Integer pageNum, Model model) {
		return listPage("标签：" + name, "标签「" + name + "」下的文章", "/tag/" + name,
				blogService.getBlogInfoListByTagNameAndIsPublished(name, pageNum), model);
	}

	@GetMapping(value = "/category/{name}", produces = "text/html")
	public String category(@PathVariable String name, @RequestParam(defaultValue = "1") Integer pageNum, Model model) {
		return listPage("分类：" + name, "分类「" + name + "」下的文章", "/category/" + name,
				blogService.getBlogInfoListByCategoryNameAndIsPublished(name, pageNum), model);
	}

	@GetMapping(value = "/column/{id}", produces = "text/html")
	public String column(@PathVariable Long id, @RequestParam(defaultValue = "1") Integer pageNum, Model model) {
		return listPage("专栏文章", "专栏下的公开文章", "/column/" + id,
				blogService.getBlogInfoListByColumnIdAndIsPublished(id, pageNum), model);
	}

	@GetMapping(value = "/archives", produces = "text/html")
	@SuppressWarnings("unchecked")
	public String archives(Model model) {
		Map<String, Object> archive = blogService.getArchiveBlogAndCountByIsPublished();
		List<ArchiveBlog> articles = new ArrayList<>();
		Object value = archive.get("blogMap");
		if (value instanceof Map) {
			((Map<String, List<ArchiveBlog>>) value).values().forEach(articles::addAll);
		}
		articles.removeIf(item -> item.getPrivacy() != null && item.getPrivacy());
		model.addAttribute("seo", metaBuilder.forList("文章归档", "按时间查看全部公开文章", "/archives"));
		model.addAttribute("title", "文章归档");
		model.addAttribute("archiveArticles", articles);
		model.addAttribute("articles", Collections.emptyList());
		return "seo/list";
	}

	private String listPage(String title, String description, String path, PageResult<BlogInfo> page, Model model) {
		List<BlogInfo> articles = page == null || page.getList() == null
				? new ArrayList<>() : new ArrayList<>(page.getList());
		articles.removeIf(article -> StringUtils.hasText(article.getPassword()));
		model.addAttribute("seo", metaBuilder.forList(title, description, path));
		model.addAttribute("title", title);
		model.addAttribute("articles", articles);
		model.addAttribute("archiveArticles", Collections.emptyList());
		model.addAttribute("totalPage", page == null ? 1 : page.getTotalPage());
		return "seo/list";
	}

	private ResponseStatusException notFound() {
		return new ResponseStatusException(HttpStatus.NOT_FOUND);
	}
}
