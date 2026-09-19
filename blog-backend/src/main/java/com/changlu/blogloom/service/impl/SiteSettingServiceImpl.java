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
import com.changlu.blogloom.model.vo.CustomModule;
import com.changlu.blogloom.model.vo.Favorite;
import com.changlu.blogloom.model.vo.Introduction;
import com.changlu.blogloom.service.RedisService;
import com.changlu.blogloom.service.SiteSettingService;
import com.changlu.blogloom.service.SiteImageStorageService;
import com.changlu.blogloom.util.JacksonUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.multipart.MultipartFile;

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
	@Autowired
	SiteImageStorageService siteImageStorageService;

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
		List<SiteSetting> type5 = new ArrayList<>();
		List<SiteSetting> type6 = new ArrayList<>();
		for (SiteSetting s : siteSettings) {
			// 自定义模块固定归入独立分组，避免历史上被当作资料卡记录展示
			if (SiteSettingConstants.CUSTOM_MODULE.equals(s.getNameEn())) {
				type5.add(s);
				continue;
			}
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
				case 6:
					type6.add(s);
					break;
				default:
					break;
			}
		}
		Map<String, List<SiteSetting>> map = new HashMap<>(8);
		map.put("type1", type1);
		map.put("type2", type2);
		map.put("type3", type3);
		map.put("type5", type5);
		map.put("type6", type6);
		return map;
	}

	@Override
	public Map<String, Object> getSiteInfo() {
		List<SiteSetting> siteSettings = siteSettingMapper.getList();
		Map<String, Object> siteInfo = new HashMap<>(2);
		List<Badge> badges = new ArrayList<>();
		Introduction introduction = new Introduction();
		List<Favorite> favorites = new ArrayList<>();
		List<String> rollTexts = new ArrayList<>();
		for (SiteSetting s : siteSettings) {
			// 自定义模块：按名称识别，与 type 值无关
			if (SiteSettingConstants.CUSTOM_MODULE.equals(s.getNameEn())) {
				CustomModule customModule = JacksonUtils.readValue(s.getValue(), CustomModule.class);
				if (customModule != null) {
					siteInfo.put(SiteSettingConstants.CUSTOM_MODULE, customModule);
				}
				continue;
			}
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

	@Transactional(rollbackFor = Exception.class)
	@Override
	public Map<String, String> uploadImage(Integer id, MultipartFile file) {
		SiteSetting setting = siteSettingMapper.getList().stream()
				.filter(item -> id.equals(item.getId()))
				.findFirst()
				.orElseThrow(() -> new com.changlu.blogloom.exception.NotFoundException("站点配置不存在"));
		if (!("footerImgUrl".equals(setting.getNameEn()) || "reward".equals(setting.getNameEn())
				|| SiteSettingConstants.FAVICON.equals(setting.getNameEn())
				|| SiteSettingConstants.AVATAR.equals(setting.getNameEn()))) {
			throw new com.changlu.blogloom.exception.BadRequestException("该配置项不支持图片上传");
		}
		String url = siteImageStorageService.save(file);
		setting.setValue(url);
		updateOneSiteSetting(setting);
		deleteSiteInfoRedisCache();
		Map<String, String> result = new LinkedHashMap<>();
		result.put("url", url);
		return result;
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
