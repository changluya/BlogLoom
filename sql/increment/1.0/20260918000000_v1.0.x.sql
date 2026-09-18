-- release_1.0.x：基础设置新增页脚固定文案配置（平台全局，前台页脚随机展示一句）。
INSERT INTO `site_setting` (`name_en`, `name_zh`, `value`, `type`)
VALUES ('hitokotoTexts', '页脚文案', '"要改变别人的心真是件很难办的事，不过改变自己要容易一点。","Stay Hungry. Stay Foolish."', 1);