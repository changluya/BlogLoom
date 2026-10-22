package com.changlu.blogloom.service;

import com.changlu.blogloom.entity.Friend;
import com.changlu.blogloom.model.vo.FriendInfo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface FriendService {
	List<Friend> getFriendList();

	List<com.changlu.blogloom.model.vo.Friend> getFriendVOList();

	void updateFriendPublishedById(Long friendId, Boolean published);

	Friend saveFriend(Friend friend);

	Friend saveFriend(Friend friend, MultipartFile avatar);

	void updateFriend(com.changlu.blogloom.model.dto.Friend friend);

	Map<String, String> uploadAvatar(Long friendId, MultipartFile file);

	void deleteFriend(Long id);

	void updateViewsByNickname(String nickname);

	FriendInfo getFriendInfo(boolean cache, boolean md);

	void updateFriendInfoContent(String content);

	void updateFriendInfoCommentEnabled(Boolean commentEnabled);
}
