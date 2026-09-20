-- BlogLoom 应用账号授权。
-- 官方 mysql 镜像会先根据 MYSQL_USER/MYSQL_PASSWORD/MYSQL_DATABASE 创建应用账号，
-- 再执行本目录脚本；此处补授全局权限，使增量 SQL 中的 CREATE DATABASE IF NOT EXISTS 可执行。
GRANT ALL PRIVILEGES ON *.* TO 'blogloom'@'%';
FLUSH PRIVILEGES;
