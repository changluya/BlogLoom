-- 仪表盘标签 Top N 聚合以 tag_id 为起始列；文章编辑回查以 blog_id 为起始列。
ALTER TABLE `blog_tag`
    ADD INDEX `idx_blog_tag_tag_blog` (`tag_id`, `blog_id`),
    ADD INDEX `idx_blog_tag_blog_tag` (`blog_id`, `tag_id`);
