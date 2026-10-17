package com.changlu.blogloom.enums;

/**
 * 访问行为枚举类
 *
 * @author: changlu
 * @date: 2026-09-13
 */
public enum VisitBehavior {
	UNKNOWN("UNKNOWN", "UNKNOWN", false),

	INDEX("访问页面", "首页", true),
	ARCHIVE("访问页面", "归档", true),
	MOMENT("访问页面", "动态", true),
	FRIEND("访问页面", "友链", true),
	ABOUT("访问页面", "关于我", true),

	BLOG("查看博客", "", true),
	CATEGORY("查看分类", "", true),
	TAG("查看标签", "", true),
	SEARCH("搜索博客", "", true),
	CLICK_FRIEND("点击友链", "", false),
	LIKE_MOMENT("点赞动态", "", false),
	CHECK_PASSWORD("校验博客密码", "", false),
	;

	/**
	 * 访问行为
	 */
	private String behavior;
	/**
	 * 访问内容
	 */
	private String content;
	/**
	 * 是否计入页面浏览量（PV）
	 */
	private boolean pageView;

	VisitBehavior(String behavior, String content, boolean pageView) {
		this.behavior = behavior;
		this.content = content;
		this.pageView = pageView;
	}

	public String getBehavior() {
		return behavior;
	}

	public String getContent() {
		return content;
	}

	public boolean isPageView() {
		return pageView;
	}
}
