package com.changlu.blogloom.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import com.changlu.blogloom.entity.Friend;

import java.util.List;

/**
 * @Description: 友链持久层接口
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Mapper
@Repository
public interface FriendMapper {
	List<Friend> getFriendList();

	List<com.changlu.blogloom.model.vo.Friend> getFriendVOList();

	int updateFriendPublishedById(Long id, Boolean published);

	int saveFriend(Friend friend);

	int updateFriend(com.changlu.blogloom.model.dto.Friend friend);

	int deleteFriend(Long id);

	int updateViewsByNickname(String nickname);
}
