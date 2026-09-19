-- release_1.0.x：站点设置新增「自定义前台展示模块」独立栏目（type=5，值为 JSON：title/content/enabled）。
-- 幂等：先删除同名配置再插入，可重复执行。
DELETE FROM `site_setting` WHERE `name_en` = 'customModule';
INSERT INTO `site_setting` (`name_en`, `name_zh`, `value`, `type`)
VALUES ('customModule', '自定义前台展示模块', '{"title":"","content":"","enabled":false}', 5);
