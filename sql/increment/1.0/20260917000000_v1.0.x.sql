-- release_1.0.x：新增博客专栏及博客专栏关系表。
CREATE TABLE IF NOT EXISTS `blog_column` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '专栏ID',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父专栏ID，0表示一级专栏',
  `name` varchar(100) NOT NULL COMMENT '专栏名称',
  `description` varchar(500) NOT NULL DEFAULT '' COMMENT '专栏简介',
  `cover` varchar(500) NOT NULL DEFAULT '' COMMENT '专栏Logo URL',
  `sort` int NOT NULL DEFAULT 0 COMMENT '同级排序值，越小越靠前',
  `is_published` bit(1) NOT NULL DEFAULT b'1' COMMENT '前台是否展示',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_sort` (`parent_id`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='博客专栏';

CREATE TABLE IF NOT EXISTS `blog_column_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关系ID',
  `column_id` bigint NOT NULL COMMENT '专栏ID',
  `blog_id` bigint NOT NULL COMMENT '博客ID',
  `sort` int NOT NULL DEFAULT 0 COMMENT '博客在该专栏中的排序值',
  `create_time` datetime NOT NULL COMMENT '加入专栏时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='博客与专栏关系';
