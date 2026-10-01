-- release_1.0.x：新增资料卡标签与首屏文案配置。
-- 幂等：先删除同名配置再插入，可重复执行。
UPDATE `site_setting` SET `value` = '长路' WHERE `name_en` = 'name';

DELETE FROM `site_setting` WHERE `name_en` = 'profileLabel';
INSERT INTO `site_setting` (`name_en`, `name_zh`, `value`, `type`)
VALUES ('profileLabel', '标签', 'Java 后端 · AI Agent', 2);

DELETE FROM `site_setting` WHERE `name_en` = 'csdn';
INSERT INTO `site_setting` (`name_en`, `name_zh`, `value`, `type`)
VALUES ('csdn', 'CSDN', 'https://changlu.blog.csdn.net/', 2);

DELETE FROM `site_setting` WHERE `name_en` = 'heroConfig';
INSERT INTO `site_setting` (`name_en`, `name_zh`, `value`, `type`)
VALUES ('heroConfig', '首屏设置',
        '{"eyebrow":"JAVA BACKEND · AI AGENT · OPEN SOURCE","title":"Changlu''s Blog","description":"每个人都是独一无二的，把握好自己的节奏，跟着自己的心走。","backgroundImage":"/img/banner/home-banner.png"}', 1);
