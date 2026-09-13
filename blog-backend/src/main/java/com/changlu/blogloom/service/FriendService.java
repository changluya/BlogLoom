package com.changlu.blogloom.service;

import com.changlu.blogloom.entity.Friend;
import com.changlu.blogloom.model.vo.FriendInfo;

import java.util.List;

public interface FriendService {
	List<Friend> getFriendList();

	List<com.changlu.blogloom.model.vo.Friend> getFriendVOList();

	void updateFriendPublishedById(Long friendId, Boolean published);

	void saveFriend(Friend friend);

	void updateFriend(com.changlu.blogloom.model.dto.Friend friend);

	void deleteFriend(Long id);

	void updateViewsByNickname(String nickname);

	FriendInfo getFriendInfo(boolean cache, boolean md);

	void updateFriendInfoContent(String content);

	void updateFriendInfoCommentEnabled(Boolean commentEnabled);
}
