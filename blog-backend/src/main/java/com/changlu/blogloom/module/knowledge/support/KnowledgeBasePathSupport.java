package com.changlu.blogloom.module.knowledge.support;

import com.changlu.blogloom.exception.BadRequestException;

/**
 * @Description: knowledgeBasePath 知识库路径的严格校验与归一化工具。
 * <p>
 * 规则：字段可为空（缺省视为知识库根目录）；非空时必须严格符合 <code>/a/bb/cc</code> 格式：
 * <ul>
 *     <li>必须以 <code>/</code> 开头；</li>
 *     <li>末尾不能带 <code>/</code>；</li>
 *     <li>不允许空路径段（连续 <code>//</code>）；</li>
 *     <li>不允许 <code>.</code>、<code>..</code> 路径段；</li>
 *     <li>不允许反斜杠、空字符；</li>
 *     <li>层级深度不超过 {@value #MAX_DEPTH} 层。</li>
 * </ul>
 * 校验通过后返回去掉前导 <code>/</code> 的路径（如 <code>/a/bb/cc</code> -&gt; <code>a/bb/cc</code>），
 * 以便与导入根目录拼接为知识库内的相对路径；空字段返回空串表示根目录。
 *
 * @Author: changlu
 * @Date: 2026-09-19
 */
public final class KnowledgeBasePathSupport {
	/** 知识库目录路径最大深度，与 ZIP 导入保持一致 */
	private static final int MAX_DEPTH = 20;

	private KnowledgeBasePathSupport() {
	}

	/**
	 * 严格校验并归一化 knowledgeBasePath。
	 *
	 * @param raw 元数据中声明的原始路径
	 * @return 去掉前导 / 的归一化路径；空字段返回空串（表示知识库根目录）
	 * @throws BadRequestException 当路径格式不合法时
	 */
	public static String normalize(String raw) {
		if (raw == null) return "";
		String value = raw.trim();
		if (value.isEmpty()) return "";
		if (value.indexOf('\\') >= 0) throw new BadRequestException("knowledgeBasePath 不能包含反斜杠: " + raw);
		if (value.indexOf('\0') >= 0) throw new BadRequestException("knowledgeBasePath 不能包含空字符");
		if (!value.startsWith("/")) throw new BadRequestException("knowledgeBasePath 必须以 / 开头: " + raw);
		if (value.endsWith("/")) throw new BadRequestException("knowledgeBasePath 末尾不能带 /: " + raw);
		String[] segments = value.substring(1).split("/", -1);
		if (segments.length > MAX_DEPTH) throw new BadRequestException("knowledgeBasePath 目录深度超过 " + MAX_DEPTH + ": " + raw);
		StringBuilder normalized = new StringBuilder();
		for (String segment : segments) {
			if (segment.isEmpty()) throw new BadRequestException("knowledgeBasePath 不能包含空路径段: " + raw);
			if (".".equals(segment) || "..".equals(segment)) throw new BadRequestException("knowledgeBasePath 不能包含 . 或 .. 路径段: " + raw);
			if (segment.trim().isEmpty()) throw new BadRequestException("knowledgeBasePath 不能包含空白路径段: " + raw);
			if (normalized.length() > 0) normalized.append('/');
			normalized.append(segment);
		}
		return normalized.toString();
	}
}
