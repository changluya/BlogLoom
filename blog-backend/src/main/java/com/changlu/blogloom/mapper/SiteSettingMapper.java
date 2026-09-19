package com.changlu.blogloom.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import com.changlu.blogloom.entity.SiteSetting;

import java.util.List;

/**
 * @Description: 站点设置持久层接口
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Mapper
@Repository
public interface SiteSettingMapper {
	List<SiteSetting> getList();

	SiteSetting getByNameEn(String nameEn);

	List<SiteSetting> getFriendInfo();

	String getWebTitleSuffix();

	int updateSiteSetting(SiteSetting siteSetting);

	int deleteSiteSettingById(Integer id);

	int saveSiteSetting(SiteSetting siteSetting);

	int updateFriendInfoContent(String content);

	int updateFriendInfoCommentEnabled(Boolean commentEnabled);
}
