-- release_1.0.x：全球访客地图地域维度与访问事实字段。
-- visitor_geo 保存可稳定关联的国家/城市及城市代表坐标；visit_log 继续作为 PV/UV 事实表。

CREATE TABLE IF NOT EXISTS `visitor_geo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `geo_key` varchar(160) NOT NULL COMMENT '稳定地域键，例如 CN|GD|SHENZHEN',
  `geo_level` tinyint NOT NULL COMMENT '1国家，2城市',
  `country_code` char(2) NOT NULL COMMENT 'ISO 3166-1 Alpha-2 国家码',
  `region_name` varchar(128) DEFAULT NULL COMMENT '匹配区域名并区分同名城市',
  `city_name_zh` varchar(128) DEFAULT NULL COMMENT '城市中文名',
  `city_name_en` varchar(128) DEFAULT NULL COMMENT '城市英文名',
  `longitude` decimal(10,6) DEFAULT NULL COMMENT 'WGS84 经度',
  `latitude` decimal(9,6) DEFAULT NULL COMMENT 'WGS84 纬度',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_visitor_geo_key` (`geo_key`),
  KEY `idx_visitor_geo_country_level` (`country_code`, `geo_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='访客标准地域维表';

ALTER TABLE `visit_log`
  ADD COLUMN `is_page_view` bit(1) DEFAULT NULL COMMENT '是否计入页面 PV' AFTER `behavior`,
  ADD COLUMN `geo_id` bigint DEFAULT NULL COMMENT '关联 visitor_geo.id' AFTER `ip_source`,
  ADD COLUMN `geo_status` tinyint NOT NULL DEFAULT 0 COMMENT '0未知，1已定位，2内网IP，3无效IP' AFTER `geo_id`,
  ADD KEY `idx_visit_map_query` (`is_page_view`, `create_time`, `geo_id`, `uuid`);

-- 初始化 MVP 覆盖范围内的国家和城市地域字典；重复执行时更新名称与坐标。
INSERT INTO `visitor_geo`
  (`geo_key`, `geo_level`, `country_code`, `region_name`, `city_name_zh`, `city_name_en`, `longitude`, `latitude`)
VALUES
  ('CN|COUNTRY', 1, 'CN', NULL, NULL, NULL, 103.000000, 35.000000),
  ('SG|COUNTRY', 1, 'SG', NULL, NULL, NULL, 104.000000, 1.000000),
  ('JP|COUNTRY', 1, 'JP', NULL, NULL, NULL, 138.000000, 36.000000),
  ('US|COUNTRY', 1, 'US', NULL, NULL, NULL, -97.000000, 38.000000),
  ('DE|COUNTRY', 1, 'DE', NULL, NULL, NULL, 10.000000, 51.000000),
  ('AU|COUNTRY', 1, 'AU', NULL, NULL, NULL, 134.000000, -25.000000),
  ('GB|COUNTRY', 1, 'GB', NULL, NULL, NULL, -2.000000, 54.000000),
  ('CA|COUNTRY', 1, 'CA', NULL, NULL, NULL, -106.000000, 57.000000),
  ('BR|COUNTRY', 1, 'BR', NULL, NULL, NULL, -52.000000, -10.000000),
  ('IN|COUNTRY', 1, 'IN', NULL, NULL, NULL, 79.000000, 22.000000),
  ('KR|COUNTRY', 1, 'KR', NULL, NULL, NULL, 128.000000, 36.500000),
  ('FR|COUNTRY', 1, 'FR', NULL, NULL, NULL, 2.000000, 46.000000),
  ('RU|COUNTRY', 1, 'RU', NULL, NULL, NULL, 90.000000, 61.000000),
  ('AE|COUNTRY', 1, 'AE', NULL, NULL, NULL, 54.000000, 24.000000),
  ('TH|COUNTRY', 1, 'TH', NULL, NULL, NULL, 101.000000, 15.000000),
  ('ID|COUNTRY', 1, 'ID', NULL, NULL, NULL, 118.000000, -2.000000),
  ('MX|COUNTRY', 1, 'MX', NULL, NULL, NULL, -102.000000, 23.000000),
  ('ZA|COUNTRY', 1, 'ZA', NULL, NULL, NULL, 24.000000, -30.000000),
  ('EG|COUNTRY', 1, 'EG', NULL, NULL, NULL, 30.000000, 27.000000),
  ('CN|CITY|CN-001', 2, 'CN', NULL, '北京市', 'Beijing', 116.413384, 39.910925),
  ('CN|CITY|CN-002', 2, 'CN', NULL, '上海市', 'Shanghai', 121.480539, 31.235929),
  ('CN|CITY|CN-003', 2, 'CN', NULL, '深圳市', 'Shenzhen', 114.064552, 22.548457),
  ('CN|CITY|CN-004', 2, 'CN', NULL, '广州市', 'Guangzhou', 113.271431, 23.135336),
  ('CN|CITY|CN-005', 2, 'CN', NULL, '杭州市', 'Hangzhou', 120.215512, 30.253083),
  ('CN|CITY|CN-006', 2, 'CN', NULL, '成都市', 'Chengdu', 104.081534, 30.655822),
  ('CN|CITY|CN-007', 2, 'CN', NULL, '武汉市', 'Wuhan', 114.311582, 30.598467),
  ('CN|CITY|CN-008', 2, 'CN', NULL, '西安市', 'Xi''an', 108.946466, 34.347269),
  ('SG|CITY|SG-009', 2, 'SG', NULL, '新加坡', 'Singapore', 103.819800, 1.352100),
  ('JP|CITY|JP-010', 2, 'JP', NULL, '东京', 'Tokyo', 139.650300, 35.676200),
  ('DE|CITY|DE-011', 2, 'DE', NULL, '柏林', 'Berlin', 13.405000, 52.520000),
  ('US|CITY|US-012', 2, 'US', NULL, '旧金山', 'San Francisco', -122.419400, 37.774900),
  ('GB|CITY|GB-013', 2, 'GB', NULL, '伦敦', 'London', -0.127600, 51.507200),
  ('AU|CITY|AU-014', 2, 'AU', NULL, '悉尼', 'Sydney', 151.209300, -33.868800),
  ('CA|CITY|CA-015', 2, 'CA', NULL, '多伦多', 'Toronto', -79.383200, 43.653200),
  ('BR|CITY|BR-016', 2, 'BR', NULL, '圣保罗', 'Sao Paulo', -46.633300, -23.550500),
  ('IN|CITY|IN-017', 2, 'IN', NULL, '孟买', 'Mumbai', 72.877700, 19.076000),
  ('CN|CITY|CN-018', 2, 'CN', NULL, '重庆市', 'Chongqing', 106.558434, 29.568996),
  ('CN|CITY|CN-019', 2, 'CN', NULL, '南京市', 'Nanjing', 118.802422, 32.064653),
  ('CN|CITY|CN-020', 2, 'CN', NULL, '苏州市', 'Suzhou', 120.592412, 31.303564),
  ('CN|CITY|CN-021', 2, 'CN', NULL, '天津市', 'Tianjin', 117.209523, 39.093668),
  ('CN|CITY|CN-022', 2, 'CN', NULL, '郑州市', 'Zhengzhou', 113.631419, 34.753439),
  ('CN|CITY|CN-023', 2, 'CN', NULL, '长沙市', 'Changsha', 112.945473, 28.234889),
  ('CN|CITY|CN-024', 2, 'CN', NULL, '青岛市', 'Qingdao', 120.389455, 36.072227),
  ('CN|CITY|CN-025', 2, 'CN', NULL, '厦门市', 'Xiamen', 118.096435, 24.485407),
  ('CN|CITY|CN-026', 2, 'CN', NULL, '合肥市', 'Hefei', 117.233443, 31.826578),
  ('CN|CITY|CN-027', 2, 'CN', NULL, '济南市', 'Jinan', 117.126399, 36.656554),
  ('CN|CITY|CN-028', 2, 'CN', NULL, '福州市', 'Fuzhou', 119.303470, 26.080429),
  ('CN|CITY|CN-029', 2, 'CN', NULL, '昆明市', 'Kunming', 102.852448, 24.873998),
  ('KR|CITY|KR-030', 2, 'KR', NULL, '首尔', 'Seoul', 126.978000, 37.566500),
  ('FR|CITY|FR-031', 2, 'FR', NULL, '巴黎', 'Paris', 2.352200, 48.856600),
  ('RU|CITY|RU-032', 2, 'RU', NULL, '莫斯科', 'Moscow', 37.617300, 55.755800),
  ('AE|CITY|AE-033', 2, 'AE', NULL, '迪拜', 'Dubai', 55.270800, 25.204800),
  ('TH|CITY|TH-034', 2, 'TH', NULL, '曼谷', 'Bangkok', 100.501800, 13.756300),
  ('US|CITY|US-035', 2, 'US', NULL, '纽约', 'New York', -74.006000, 40.712800),
  ('ID|CITY|ID-036', 2, 'ID', NULL, '雅加达', 'Jakarta', 106.845600, -6.208800),
  ('MX|CITY|MX-037', 2, 'MX', NULL, '墨西哥城', 'Mexico City', -99.133200, 19.432600),
  ('ZA|CITY|ZA-038', 2, 'ZA', NULL, '约翰内斯堡', 'Johannesburg', 28.047300, -26.204100),
  ('AU|CITY|AU-039', 2, 'AU', NULL, '墨尔本', 'Melbourne', 144.963100, -37.813600),
  ('EG|CITY|EG-040', 2, 'EG', NULL, '开罗', 'Cairo', 31.235700, 30.044400)
ON DUPLICATE KEY UPDATE
  `country_code` = VALUES(`country_code`),
  `city_name_zh` = VALUES(`city_name_zh`),
  `city_name_en` = VALUES(`city_name_en`),
  `longitude` = VALUES(`longitude`),
  `latitude` = VALUES(`latitude`);

-- 兼容历史日志：根据稳定行为文案回填 PV 口径。
UPDATE `visit_log`
SET `is_page_view` = b'1'
WHERE `is_page_view` IS NULL
  AND `behavior` IN ('访问页面', '查看博客', '查看分类', '查看标签', '搜索博客');

UPDATE `visit_log`
SET `is_page_view` = b'0'
WHERE `is_page_view` IS NULL
  AND `behavior` IN ('点击友链', '点赞动态', '校验博客密码', 'UNKNOWN');

-- 兼容历史四段/五段 ip_source：优先按第三段城市名命中城市字典。
UPDATE `visit_log` log
JOIN `visitor_geo` geo
  ON geo.geo_level = 2
 AND geo.city_name_zh = NULLIF(SUBSTRING_INDEX(SUBSTRING_INDEX(log.ip_source, '|', 3), '|', -1), '')
SET log.geo_id = geo.id,
    log.geo_status = 1
WHERE log.geo_id IS NULL
  AND log.ip_source IS NOT NULL
  AND log.ip_source <> '';

-- 五段数据若城市未命中，则按最后一段 ISO Alpha-2 国家码降级到国家级。
UPDATE `visit_log` log
JOIN `visitor_geo` geo
  ON geo.geo_level = 1
 AND geo.country_code = UPPER(SUBSTRING_INDEX(log.ip_source, '|', -1))
SET log.geo_id = geo.id,
    log.geo_status = 1
WHERE log.geo_id IS NULL
  AND LENGTH(SUBSTRING_INDEX(log.ip_source, '|', -1)) = 2;

