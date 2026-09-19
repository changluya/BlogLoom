package com.changlu.blogloom.module.knowledge.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Description: 知识库 Markdown 顶部 JSON 代码块描述的文章元数据。
 * <p>
 * 固定结构（每篇 Markdown 最顶部用一个 ```json 代码块包裹）：
 * <pre>
 * {
 *   "title": "文章标题",
 *   "tags": "标签1,标签2,标签3",
 *   "category": "分类",
 *   "articleSummary": "150 字以内的摘要",
 *   "columns": "专栏1,专栏2",
 *   "createTime": "2026-09-19 14:30:00",
 *   "updateTime": "2026-09-19 14:30:00",
 *   "knowledgeBasePath": "/a/bb/cc"
 * }
 * </pre>
 * 字段约定：
 * <ul>
 *     <li>title：文章标题，缺省时回退为文件名；</li>
 *     <li>tags：标签，多个，逗号（中英文/顿号/竖线/分号均可）分隔；</li>
 *     <li>category：分类，单个；</li>
 *     <li>articleSummary：摘要，150 字以内；</li>
 *     <li>columns：专栏，多个，逗号分隔；</li>
 *     <li>createTime：创建时间，格式 YYYY-MM-DD HH:mm:ss，缺省为导入时间；</li>
 *     <li>updateTime：更新时间，格式 YYYY-MM-DD HH:mm:ss，缺省同创建时间；</li>
 *     <li>knowledgeBasePath：知识库目录路径，格式 /a/bb/cc，以 / 开头且末尾不带 /。
 *     仅在「Markdown 文件」导入时生效，用于定位文章所在知识库目录；ZIP 导入按压缩包目录结构，忽略该字段。</li>
 * </ul>
 *
 * @Author: changlu
 * @Date: 2026-09-19
 */
@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KnowledgeArticleMetadata {
	/** 文章标题（规则：标题即 title） */
	private String title;
	/** 文章摘要（规则：articleSummary 即文章描述，150 字以内） */
	private String articleSummary;
	/** 标签，多个（规则：3-5 个高度相关标签） */
	private List<String> tags = new ArrayList<>();
	/** 分类，单个（规则：category 默认就是一个） */
	private String category;
	/** 专栏，多个（规则：columns 可有多个，逗号分隔） */
	private List<String> columns = new ArrayList<>();
	/** 创建时间（格式 YYYY-MM-DD HH:mm:ss，缺省为导入时间） */
	@JsonAlias({"create_time", "createdAt", "created_at", "created"})
	private String createTime;
	/** 更新时间（格式 YYYY-MM-DD HH:mm:ss，缺省同创建时间） */
	@JsonAlias({"update_time", "updatedAt", "updated_at", "updated", "modified"})
	private String updateTime;
	/** 知识库目录路径（格式 /a/bb/cc，以 / 开头且末尾不带 /；仅「Markdown 文件」导入时生效） */
	@JsonAlias({"knowledge_base_path", "knowledgePath", "kbPath", "knowledge_path"})
	private String knowledgeBasePath;

	/**
	 * tags 同时兼容数组与逗号分隔字符串两种写法。
	 */
	@JsonAlias({"articleTags", "tagList", "tagNames"})
	@JsonSetter("tags")
	public void setTags(Object value) {
		this.tags = normalizeList(value);
	}

	/**
	 * columns 同时兼容数组与逗号分隔字符串两种写法。
	 */
	@JsonAlias({"columnList", "columnNames"})
	@JsonSetter("columns")
	public void setColumns(Object value) {
		this.columns = normalizeList(value);
	}

	/**
	 * 返回去重后的全部专栏名，供导入时逐个“先查后建”。
	 */
	public List<String> allColumns() {
		List<String> result = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (String item : columns) {
			if (item != null && !item.trim().isEmpty() && seen.add(item.trim())) result.add(item.trim());
		}
		return result;
	}

	/**
	 * 将 JSON 值统一规范为字符串列表：数组逐个取用，字符串按分隔符切分。
	 */
	private List<String> normalizeList(Object value) {
		List<String> result = new ArrayList<>();
		if (value instanceof Collection) {
			for (Object item : (Collection<?>) value) {
				if (item != null && !item.toString().trim().isEmpty()) result.add(item.toString().trim());
			}
		} else if (value instanceof String) {
			for (String part : ((String) value).split("[,，、|;；]")) {
				if (!part.trim().isEmpty()) result.add(part.trim());
			}
		}
		return result;
	}
}
