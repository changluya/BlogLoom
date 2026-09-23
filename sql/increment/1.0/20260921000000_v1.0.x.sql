-- release_1.0.x：站点设置新增本地上传渠道配置（type=6）：uploadChannelLocal。
-- 本地上传默认无需配置，address 留空时回退 blog.api；此处补充默认参数以便后台可视化展示与修改。
-- 幂等：先删除同名配置再插入，可重复执行。
DELETE FROM `site_setting` WHERE `name_en` = 'uploadChannelLocal';
INSERT INTO `site_setting` (`name_en`, `name_zh`, `value`, `type`)
VALUES ('uploadChannelLocal', '本地配置',
        '{"address":"http://localhost:8090"}', 6);