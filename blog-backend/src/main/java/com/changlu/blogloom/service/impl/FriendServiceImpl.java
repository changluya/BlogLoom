package com.changlu.blogloom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.changlu.blogloom.constant.CacheKeyConstants;
import com.changlu.blogloom.entity.Friend;
import com.changlu.blogloom.entity.SiteSetting;
import com.changlu.blogloom.exception.PersistenceException;
import com.changlu.blogloom.mapper.FriendMapper;
import com.changlu.blogloom.mapper.SiteSettingMapper;
import com.changlu.blogloom.model.vo.FriendInfo;
import com.changlu.blogloom.service.FriendService;
import com.changlu.blogloom.service.BlogCacheService;
import com.changlu.blogloom.util.markdown.MarkdownUtils;

import java.util.Date;
import java.util.List;

/**
 * @Description: 友链业务层实现
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Service
public class FriendServiceImpl implements FriendService {
	@Autowired
	FriendMapper friendMapper;
	@Autowired
	SiteSettingMapper siteSettingMapper;
	@Autowired
	BlogCacheService cacheService;

	@Override
	public List<Friend> getFriendList() {
		return friendMapper.getFriendList();
	}

	@Override
	public List<com.changlu.blogloom.model.vo.Friend> getFriendVOList() {
		return friendMapper.getFriendVOList();
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateFriendPublishedById(Long friendId, Boolean published) {
		if (friendMapper.updateFriendPublishedById(friendId, published) != 1) {
			throw new PersistenceException("操作失败");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void saveFriend(Friend friend) {
		friend.setViews(0);
		friend.setCreateTime(new Date());
		if (friendMapper.saveFriend(friend) != 1) {
			throw new PersistenceException("添加失败");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateFriend(com.changlu.blogloom.model.dto.Friend friend) {
		if (friendMapper.updateFriend(friend) != 1) {
			throw new PersistenceException("修改失败");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void deleteFriend(Long id) {
		if (friendMapper.deleteFriend(id) != 1) {
			throw new PersistenceException("删除失败");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateViewsByNickname(String nickname) {
		if (friendMapper.updateViewsByNickname(nickname) != 1) {
			throw new PersistenceException("操作失败");
		}
	}

	@Override
	public FriendInfo getFriendInfo(boolean cache, boolean md) {
		String cacheKey = CacheKeyConstants.FRIEND_INFO_MAP;
		if (cache) {
			FriendInfo friendInfoFromCache = cacheService.getObjectByValue(cacheKey, FriendInfo.class);
			if (friendInfoFromCache != null) {
				return friendInfoFromCache;
			}
		}
		List<SiteSetting> siteSettings = siteSettingMapper.getFriendInfo();
		FriendInfo friendInfo = new FriendInfo();
		for (SiteSetting siteSetting : siteSettings) {
			if ("friendContent".equals(siteSetting.getNameEn())) {
				if (md) {
					friendInfo.setContent(MarkdownUtils.markdownToHtmlExtensions(siteSetting.getValue()));
				} else {
					friendInfo.setContent(siteSetting.getValue());
				}
			} else if ("friendCommentEnabled".equals(siteSetting.getNameEn())) {
				if ("1".equals(siteSetting.getValue())) {
					friendInfo.setCommentEnabled(true);
				} else {
					friendInfo.setCommentEnabled(false);
				}
			}
		}
		if (cache && md) {
			cacheService.saveObjectToValue(cacheKey, friendInfo);
		}
		return friendInfo;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateFriendInfoContent(String content) {
		if (siteSettingMapper.updateFriendInfoContent(content) != 1) {
			throw new PersistenceException("修改失败");
		}
		deleteFriendInfoCache();
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateFriendInfoCommentEnabled(Boolean commentEnabled) {
		if (siteSettingMapper.updateFriendInfoCommentEnabled(commentEnabled) != 1) {
			throw new PersistenceException("修改失败");
		}
		deleteFriendInfoCache();
	}

	/**
	 * 删除友链页面缓存
	 */
	private void deleteFriendInfoCache() {
		cacheService.deleteCacheByKey(CacheKeyConstants.FRIEND_INFO_MAP);
	}
}
