-- release_1.0.x：新增本地 Markdown 知识库节点表，并幂等回填存量博客。
CREATE TABLE IF NOT EXISTS `knowledge_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '节点ID',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父节点ID，0表示虚拟根节点',
  `blog_id` bigint DEFAULT NULL COMMENT 'DOC节点关联的博客ID，DIR节点必须为空',
  `name` varchar(255) NOT NULL COMMENT '节点显示名',
  `type` varchar(20) NOT NULL COMMENT '节点类型：DIR目录、DOC文档',
  `sort` int NOT NULL DEFAULT 0 COMMENT '同级排序值',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='博客知识库节点树';

INSERT INTO `knowledge_node` (`parent_id`,`blog_id`,`name`,`type`,`sort`,`create_time`,`update_time`)
SELECT 0,b.id,b.title,'DOC',0,NOW(),NOW()
FROM `blog` b
LEFT JOIN `knowledge_node` kn ON kn.blog_id=b.id
WHERE kn.id IS NULL;
