package top.naccl.util.comment.channel;

import top.naccl.constant.CommentConstants;
import top.naccl.util.common.SpringContextUtils;

/**
 * 评论提醒方式
 *
 * @author: changlu
 * @date: 2026-09-13
 */
public class ChannelFactory {
	/**
	 * 创建评论提醒方式
	 *
	 * @param channelName 方式名称
	 * @return
	 */
	public static CommentNotifyChannel getChannel(String channelName) {
		if (CommentConstants.TELEGRAM.equalsIgnoreCase(channelName)) {
			return SpringContextUtils.getBean("telegramChannel", CommentNotifyChannel.class);
		} else if (CommentConstants.MAIL.equalsIgnoreCase(channelName)) {
			return SpringContextUtils.getBean("mailChannel", CommentNotifyChannel.class);
		}
		throw new RuntimeException("Unsupported value in [application.properties]: [comment.notify.channel]");
	}
}
