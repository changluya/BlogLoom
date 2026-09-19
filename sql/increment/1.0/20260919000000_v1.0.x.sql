-- release_1.0.x：博客支持逻辑删除（回收站）。新增 is_deleted 标记，0 未删除、1 已删除。
ALTER TABLE `blog`
    ADD COLUMN `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除（回收站）' AFTER `password`;