package top.naccl.util.comment.channel;

import top.naccl.model.dto.Comment;

/**
 * 评论提醒方式
 *
 * @author: changlu
 * @date: 2026-09-13
 */
public interface CommentNotifyChannel {
	/**
	 * 通过指定方式通知自己
	 *
	 * @param comment 当前收到的评论
	 */
	void notifyMyself(Comment comment);
}
