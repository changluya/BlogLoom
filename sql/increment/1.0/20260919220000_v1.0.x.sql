-- release_1.0.x：站点设置新增图床配置（type=6）：上传渠道选择 + 阿里云渠道参数。
-- 幂等：先删除同名配置再插入，可重复执行。
DELETE FROM `site_setting` WHERE `name_en` = 'uploadChannelChoose';
INSERT INTO `site_setting` (`name_en`, `name_zh`, `value`, `type`)
VALUES ('uploadChannelChoose', '上传渠道选择', 'local', 6);

DELETE FROM `site_setting` WHERE `name_en` = 'uploadChannelAliyun';
INSERT INTO `site_setting` (`name_en`, `name_zh`, `value`, `type`)
VALUES ('uploadChannelAliyun', '阿里云配置',
        '{"accessKeyId":"","accessKeySecret":"","bucket":"","area":"","path":""}', 6);
