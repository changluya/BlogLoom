-- release_1.0.x：移除 Redis 组件，改用 MySQL 表 cache_entry 模拟缓存能力。
-- 覆盖原有 Redis 用途：浏览量暂存、页面缓存、访客 UV 标识集合、访问频率限制计数、QQ 头像缓存。
-- 幂等：缓存数据可再生，可重复执行。
DROP TABLE IF EXISTS `cache_entry`;
CREATE TABLE `cache_entry` (
    `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `cache_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '缓存键',
    `cache_value` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '缓存值(JSON格式)',
    `cache_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'DEFAULT' COMMENT '缓存类型',
    `expire_time` datetime(0) NULL DEFAULT NULL COMMENT '过期时间，为空表示永不过期',
    `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_cache_key` (`cache_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '缓存表（替代 Redis）' ROW_FORMAT = Dynamic;

-- 定时任务同步更新：不再依赖 Redis，bean 名与备注一并调整。
UPDATE `schedule_job`
SET `bean_name` = 'cacheSyncScheduleTask',
    `remark`    = '每天凌晨一点，从缓存将博客浏览量同步到数据库'
WHERE `job_id` = 1;
UPDATE `schedule_job`
SET `remark` = '清空当天缓存访客标识，记录当天的PV和UV，更新当天所有访客的PV和最后访问时间，更新城市新增访客UV数'
WHERE `job_id` = 2;
