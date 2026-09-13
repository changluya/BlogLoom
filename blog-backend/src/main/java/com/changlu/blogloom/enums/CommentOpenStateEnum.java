package com.changlu.blogloom.enums;

/**
 * 评论开放状态枚举类
 *
 * @author: changlu
 * @date: 2026-09-13
 */
public enum CommentOpenStateEnum {
	/**
	 * 博客不存在，或博客未公开
	 */
	NOT_FOUND,
	/**
	 * 评论正常开放
	 */
	OPEN,
	/**
	 * 评论已关闭
	 */
	CLOSE,
	/**
	 * 评论所在页面需要密码
	 */
	PASSWORD,
}
