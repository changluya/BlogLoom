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
	title VARCHAR(255),
	description VARCHAR(1000),
	first_picture VARCHAR(1000),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_published TINYINT DEFAULT 0,
	password VARCHAR(255) DEFAULT '',
    views INT DEFAULT 0,
    is_deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS visitor_geo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    geo_key VARCHAR(160) NOT NULL,
    geo_level TINYINT NOT NULL,
    country_code CHAR(2) NOT NULL,
    region_name VARCHAR(128),
    city_name_zh VARCHAR(128),
    city_name_en VARCHAR(128),
    longitude DECIMAL(10, 6),
    latitude DECIMAL(9, 6)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_test_visitor_geo_key ON visitor_geo (geo_key);

CREATE TABLE IF NOT EXISTS visit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(255),
    uri VARCHAR(255),
    method VARCHAR(32),
    param VARCHAR(2000),
    behavior VARCHAR(255),
    is_page_view BOOLEAN,
    content VARCHAR(255),
    remark VARCHAR(255),
    ip VARCHAR(255),
    ip_source VARCHAR(255),
    geo_id BIGINT,
    geo_status TINYINT DEFAULT 0,
    os VARCHAR(255),
    browser VARCHAR(255),
    times INT,
    create_time TIMESTAMP,
    user_agent VARCHAR(2000)
);
