package com.changlu.blogloom.module.seo;

import com.changlu.blogloom.entity.Category;
import com.changlu.blogloom.model.vo.BlogDetail;
import com.changlu.blogloom.model.vo.BlogInfo;
import com.changlu.blogloom.model.vo.PageResult;
import com.changlu.blogloom.module.seo.domain.SeoSiteLayout;
import com.changlu.blogloom.module.seo.service.SeoSiteLayoutService;
import com.changlu.blogloom.service.BlogService;
import com.changlu.blogloom.service.SiteSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Date;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class SeoEndpointIntegrationTest {
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@MockBean
	private BlogService blogService;
	@MockBean
	private SiteSettingService siteSettingService;
	@MockBean
	private SeoSiteLayoutService seoSiteLayoutService;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("delete from blog");
		when(siteSettingService.getSeoDomain()).thenReturn("https://blog.example.com/");
		when(siteSettingService.getWebTitleSuffix()).thenReturn(" - BlogLoom");
		when(seoSiteLayoutService.getLayout()).thenReturn(new SeoSiteLayout());
	}

	@Test
	void shouldRenderArticleHtmlWithSeoAndOptimizedContent() throws Exception {
		BlogDetail article = new BlogDetail();
		article.setId(1L);
		article.setTitle("SEO article");
		article.setContent("<h1>inner heading</h1><img src=\"/image/a.png\">");
		article.setDescription("article summary");
		article.setCreateTime(new Date());
		article.setUpdateTime(new Date());
		Category category = new Category(); category.setName("Java"); article.setCategory(category);
		when(blogService.getBlogByIdAndIsPublished(1L)).thenReturn(article);

		mockMvc.perform(get("/blog/1").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("seo/blog"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<title>SEO article - BlogLoom</title>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("https://blog.example.com/blog/1")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("loading=\"lazy\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"three wide column m-mobile-hide\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("本文目录")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"site-footer\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"home-hero-background m-mobile-hide\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("BlogPosting")));
	}

	@Test
	void shouldRenderTagAggregationAsHtml() throws Exception {
		BlogInfo item = new BlogInfo(); item.setId(2L); item.setTitle("Tagged article"); item.setCreateTime(new Date());
		when(blogService.getBlogInfoListByTagNameAndIsPublished(anyString(), anyInt()))
				.thenReturn(new PageResult<>(1, Collections.singletonList(item)));
		mockMvc.perform(get("/tag/Java").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(view().name("seo/list"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("/blog/2")));
	}

	@Test
	void shouldExposeSiteLevelSeoFiles() throws Exception {
		mockMvc.perform(get("/robots.txt"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Sitemap: https://blog.example.com/sitemap.xml")));
		mockMvc.perform(get("/sitemap.xml"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<urlset")));
		mockMvc.perform(get("/rss.xml"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<rss version=\"2.0\">")));
	}

	@Test
	void shouldServeSeoStyleAssets() throws Exception {
		mockMvc.perform(get("/seo-assets/semantic.min.css"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Semantic UI")));
		mockMvc.perform(get("/seo-assets/themes/default/assets/fonts/icons.woff2"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/seo-assets/vue-exact.css"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(".introduction-shell")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(".m-toc")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(".footer-container")));
	}

	@Test
	void sitemapAndRssShouldOnlyExposePublishedPublicArticles() throws Exception {
		jdbcTemplate.update("insert into blog(id,title,description,create_time,update_time,is_published,password,is_deleted) " +
				"values(101,'public','visible',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,'',0)");
		jdbcTemplate.update("insert into blog(id,title,description,create_time,update_time,is_published,password,is_deleted) " +
				"values(102,'protected','hidden',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,'secret',0)");
		jdbcTemplate.update("insert into blog(id,title,description,create_time,update_time,is_published,password,is_deleted) " +
				"values(103,'draft','hidden',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'',0)");

		mockMvc.perform(get("/sitemap.xml"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("/blog/101")))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/blog/102"))))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/blog/103"))));
		mockMvc.perform(get("/rss.xml"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("public")))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("protected"))));
	}
}
