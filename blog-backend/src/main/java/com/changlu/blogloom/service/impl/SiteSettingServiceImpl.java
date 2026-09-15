package com.changlu.blogloom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.changlu.blogloom.constant.RedisKeyConstants;
import com.changlu.blogloom.constant.SiteSettingConstants;
import com.changlu.blogloom.entity.SiteSetting;
import com.changlu.blogloom.exception.PersistenceException;
import com.changlu.blogloom.mapper.SiteSettingMapper;
import com.changlu.blogloom.model.vo.Badge;
import com.changlu.blogloom.model.vo.Copyright;
import com.changlu.blogloom.model.vo.Favorite;
import com.changlu.blogloom.model.vo.Introduction;
import com.changlu.blogloom.service.RedisService;
import com.changlu.blogloom.service.SiteSettingService;
import com.changlu.blogloom.util.JacksonUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description: 站点设置业务层实现
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Service
public class SiteSettingServiceImpl implements SiteSettingService {
	@Autowired
	SiteSettingMapper siteSettingMapper;
	@Autowired
	RedisService redisService;

	private static final Pattern PATTERN = Pattern.compile("\"(.*?)\"");

	/**
	 * 站点配置可能通过初始化 SQL 或后台服务之外的方式变更，启动时清理旧缓存，
	 * 避免资料卡继续使用已经删除的社交链接。
	 */
	@PostConstruct
	private void clearSiteInfoCacheOnStartup() {
		deleteSiteInfoRedisCache();
	}

	@Override
	public Map<String, List<SiteSetting>> getList() {
		List<SiteSetting> siteSettings = siteSettingMapper.getList();
		List<SiteSetting> type1 = new ArrayList<>();
		List<SiteSetting> type2 = new ArrayList<>();
		List<SiteSetting> type3 = new ArrayList<>();
		for (SiteSetting s : siteSettings) {
			switch (s.getType()) {
				case 1:
					type1.add(s);
					break;
				case 2:
					type2.add(s);
					break;
				case 3:
					type3.add(s);
					break;
				default:
					break;
			}
		}
		Map<String, List<SiteSetting>> map = new HashMap<>(8);
		map.put("type1", type1);
		map.put("type2", type2);
		map.put("type3", type3);
		return map;
	}

	@Override
	public Map<String, Object> getSiteInfo() {
		String redisKey = RedisKeyConstants.SITE_INFO_MAP;
		Map<String, Object> siteInfoMapFromRedis = redisService.getMapByValue(redisKey);
		if (siteInfoMapFromRedis != null) {
			return siteInfoMapFromRedis;
		}
		List<SiteSetting> siteSettings = siteSettingMapper.getList();
		Map<String, Object> siteInfo = new HashMap<>(2);
		List<Badge> badges = new ArrayList<>();
		Introduction introduction = new Introduction();
		List<Favorite> favorites = new ArrayList<>();
		List<String> rollTexts = new ArrayList<>();
		for (SiteSetting s : siteSettings) {
			switch (s.getType()) {
				case 1:
					if (SiteSettingConstants.COPYRIGHT.equals(s.getNameEn())) {
						Copyright copyright = JacksonUtils.readValue(s.getValue(), Copyright.class);
						siteInfo.put(s.getNameEn(), copyright);
					} else {
						siteInfo.put(s.getNameEn(), s.getValue());
					}
					break;
					case 2:
						switch (s.getNameEn()) {
						case SiteSettingConstants.AVATAR:
							introduction.setAvatar(s.getValue());
							break;
						case SiteSettingConstants.NAME:
							introduction.setName(s.getValue());
							break;
						case SiteSettingConstants.GITHUB:
							introduction.setGithub(normalizeOptionalLink(s.getValue()));
							break;
						case SiteSettingConstants.TELEGRAM:
							introduction.setTelegram(normalizeOptionalLink(s.getValue()));
							break;
						case SiteSettingConstants.QQ:
							introduction.setQq(normalizeOptionalLink(s.getValue()));
							break;
						case SiteSettingConstants.BILIBILI:
							introduction.setBilibili(normalizeOptionalLink(s.getValue()));
							break;
						case SiteSettingConstants.NETEASE:
							introduction.setNetease(normalizeOptionalLink(s.getValue()));
							break;
						case SiteSettingConstants.EMAIL:
							introduction.setEmail(normalizeOptionalLink(s.getValue()));
							break;
						case SiteSettingConstants.FAVORITE:
							Favorite favorite = JacksonUtils.readValue(s.getValue(), Favorite.class);
							favorites.add(favorite);
							break;
						case SiteSettingConstants.ROLL_TEXT:
							Matcher m = PATTERN.matcher(s.getValue());
							while (m.find()) {
								rollTexts.add(m.group(1));
							}
							break;
						default:
							break;
					}
					break;
				case 3:
					Badge badge = JacksonUtils.readValue(s.getValue(), Badge.class);
					badges.add(badge);
					break;
				default:
					break;
			}
		}
		introduction.setFavorites(favorites);
		introduction.setRollText(rollTexts);
		Map<String, Object> map = new HashMap<>(8);
		map.put("introduction", introduction);
		map.put("siteInfo", siteInfo);
		map.put("badges", badges);
		redisService.saveMapToValue(redisKey, map);
		return map;
	}

	@Override
	public String getWebTitleSuffix() {
		return siteSettingMapper.getWebTitleSuffix();
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateSiteSetting(List<LinkedHashMap> siteSettings, List<Integer> deleteIds) {
		for (Integer id : deleteIds) {
			//删除
			deleteOneSiteSettingById(id);
		}
		for (LinkedHashMap s : siteSettings) {
			SiteSetting siteSetting = JacksonUtils.convertValue(s, SiteSetting.class);
			if (siteSetting.getId() != null) {
				//修改
				updateOneSiteSetting(siteSetting);
			} else {
				//添加
				saveOneSiteSetting(siteSetting);
			}
		}
		deleteSiteInfoRedisCache();
	}

	public void saveOneSiteSetting(SiteSetting siteSetting) {
		if (siteSettingMapper.saveSiteSetting(siteSetting) != 1) {
			throw new PersistenceException("配置添加失败");
		}
	}

	public void updateOneSiteSetting(SiteSetting siteSetting) {
		if (siteSettingMapper.updateSiteSetting(siteSetting) != 1) {
			throw new PersistenceException("配置修改失败");
		}
	}

	public void deleteOneSiteSettingById(Integer id) {
		if (siteSettingMapper.deleteSiteSettingById(id) != 1) {
			throw new PersistenceException("配置删除失败");
		}
	}

	/**
	 * 删除站点信息缓存
	 */
	private void deleteSiteInfoRedisCache() {
		redisService.deleteCacheByKey(RedisKeyConstants.SITE_INFO_MAP);
	}

	private String normalizeOptionalLink(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		if (normalized.isEmpty() || "#".equals(normalized)
				|| "null".equalsIgnoreCase(normalized) || "undefined".equalsIgnoreCase(normalized)) {
			return null;
		}
		return normalized;
	}
}
