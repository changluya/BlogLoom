-- release_1.0.x：新增 SEO 域名配置项（后台 SEO 配置页使用；为空时回退 blog.view）。
-- 幂等：先删除同名配置再插入，可重复执行。
DELETE FROM `site_setting` WHERE `name_en` = 'seoDomain';
INSERT INTO `site_setting` (`name_en`, `name_zh`, `value`, `type`)
VALUES ('seoDomain', 'SEO 域名', '', 1);
