-- 仅供本地联调：先执行 sql/increment/1.0/20261017000000_v1.0.x.sql。
-- 使用固定 UUID 和 remark 标识，重复执行前会删除上一批模拟访问，不污染正式环境。

DELETE FROM `visit_log` WHERE `remark` = '[GLOBAL_VISITOR_MAP_LOCAL_MOCK]';

INSERT INTO `visit_log`
  (`uuid`, `uri`, `method`, `param`, `behavior`, `is_page_view`, `content`, `remark`,
   `ip`, `ip_source`, `geo_id`, `geo_status`, `os`, `browser`, `times`, `create_time`, `user_agent`)
VALUES
  ('mock-cn-001', '/blog/1', 'GET', '{}', '查看博客', b'1', '全球地图联调文章', '[GLOBAL_VISITOR_MAP_LOCAL_MOCK]',
   '1.0.1.1', '中国|广东省|深圳市|电信|CN', (SELECT id FROM visitor_geo WHERE geo_key='CN|CITY|CN-003'), 1, 'Mac OS', 'Chrome', 32, DATE_SUB(NOW(), INTERVAL 20 MINUTE), 'BlogLoom Local Mock'),
  ('mock-cn-001', '/blog/2', 'GET', '{}', '查看博客', b'1', '全球地图联调文章', '[GLOBAL_VISITOR_MAP_LOCAL_MOCK]',
   '1.0.1.1', '中国|广东省|深圳市|电信|CN', (SELECT id FROM visitor_geo WHERE geo_key='CN|CITY|CN-003'), 1, 'Mac OS', 'Chrome', 35, DATE_SUB(NOW(), INTERVAL 15 MINUTE), 'BlogLoom Local Mock'),
  ('mock-cn-002', '/', 'GET', '{}', '访问页面', b'1', '首页', '[GLOBAL_VISITOR_MAP_LOCAL_MOCK]',
   '1.0.2.2', '中国|北京市|北京市|联通|CN', (SELECT id FROM visitor_geo WHERE geo_key='CN|CITY|CN-001'), 1, 'Windows', 'Edge', 24, DATE_SUB(NOW(), INTERVAL 1 HOUR), 'BlogLoom Local Mock'),
  ('mock-jp-001', '/blog/1', 'GET', '{}', '查看博客', b'1', '全球地图联调文章', '[GLOBAL_VISITOR_MAP_LOCAL_MOCK]',
   '1.0.3.3', 'Japan|Tokyo|东京||JP', (SELECT id FROM visitor_geo WHERE geo_key='JP|CITY|JP-010'), 1, 'iOS', 'Safari', 41, DATE_SUB(NOW(), INTERVAL 2 HOUR), 'BlogLoom Local Mock'),
  ('mock-us-001', '/blog/1', 'GET', '{}', '查看博客', b'1', '全球地图联调文章', '[GLOBAL_VISITOR_MAP_LOCAL_MOCK]',
   '1.0.4.4', 'United States|New York|纽约||US', (SELECT id FROM visitor_geo WHERE geo_key='US|CITY|US-035'), 1, 'Windows', 'Chrome', 55, DATE_SUB(NOW(), INTERVAL 3 HOUR), 'BlogLoom Local Mock'),
  ('mock-de-001', '/blog/1', 'GET', '{}', '查看博客', b'1', '全球地图联调文章', '[GLOBAL_VISITOR_MAP_LOCAL_MOCK]',
   '1.0.5.5', 'Germany|Berlin|柏林||DE', (SELECT id FROM visitor_geo WHERE geo_key='DE|CITY|DE-011'), 1, 'Linux', 'Firefox', 47, DATE_SUB(NOW(), INTERVAL 4 HOUR), 'BlogLoom Local Mock'),
  ('mock-country-only', '/', 'GET', '{}', '访问页面', b'1', '首页', '[GLOBAL_VISITOR_MAP_LOCAL_MOCK]',
   '1.0.6.6', 'Australia|||ISP|AU', (SELECT id FROM visitor_geo WHERE geo_key='AU|COUNTRY'), 1, 'Android', 'Chrome', 30, DATE_SUB(NOW(), INTERVAL 5 HOUR), 'BlogLoom Local Mock'),
  ('mock-unknown', '/', 'GET', '{}', '访问页面', b'1', '首页', '[GLOBAL_VISITOR_MAP_LOCAL_MOCK]',
   '127.0.0.1', '内网IP||||', NULL, 2, 'Linux', 'Chrome', 10, DATE_SUB(NOW(), INTERVAL 6 HOUR), 'BlogLoom Local Mock'),
  ('mock-non-pv', '/moment/1/like', 'POST', '{}', '点赞动态', b'0', '动态', '[GLOBAL_VISITOR_MAP_LOCAL_MOCK]',
   '1.0.1.1', '中国|广东省|深圳市|电信|CN', (SELECT id FROM visitor_geo WHERE geo_key='CN|CITY|CN-003'), 1, 'Mac OS', 'Chrome', 18, DATE_SUB(NOW(), INTERVAL 10 MINUTE), 'BlogLoom Local Mock');
