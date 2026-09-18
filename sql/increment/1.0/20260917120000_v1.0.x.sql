-- release_1.0.x：基础设置新增网站 Tab 图标配置。
UPDATE `site_setting` SET `name_zh` = '页脚二维码' WHERE `name_en` = 'footerImgUrl';

DELETE FROM `site_setting` WHERE `name_en` = 'favicon';

INSERT INTO `site_setting` (`name_en`, `name_zh`, `value`, `type`)
VALUES ('favicon', '网站 Tab 图标', '/img/site-favicon.png', 1);
