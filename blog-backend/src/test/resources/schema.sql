-- 单元测试用的最小 H2 表结构，仅覆盖应用启动阶段会访问的表。
-- 其余业务表在纯单元测试中通过 Mockito 模拟，不需要真实建表。
CREATE TABLE IF NOT EXISTS cache_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cache_key VARCHAR(512) NOT NULL,
    cache_value CLOB,
    cache_type VARCHAR(100) DEFAULT 'DEFAULT',
    expire_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_cache_entry_key ON cache_entry (cache_key);

CREATE TABLE IF NOT EXISTS schedule_job (
    job_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bean_name VARCHAR(255),
    method_name VARCHAR(255),
    params VARCHAR(255),
    cron VARCHAR(255),
    status TINYINT,
    remark VARCHAR(255),
    create_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS blog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    views INT DEFAULT 0,
    is_deleted TINYINT DEFAULT 0
);
