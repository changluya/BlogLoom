---
title: "运维"
description: 备份、升级与日常维护。
---

本页描述 BlogLoom 的日常运维操作：备份、升级与维护。

## 备份

- 定期使用 `mysqldump` 备份数据库。
- 备份 `conf/upload/`、`conf/static/` 与 `conf/application.properties`。
- 将备份保存到独立位置并验证可恢复。

## 升级

1. 备份数据库与配置文件。
2. 拉取新版本代码。
3. 应用增量 SQL 或使用 `bin/local/upgrate-sql.sh`。
4. 重新构建后端与前端。
5. 重启服务并做回归验证。

## 日常维护

- 关注 `conf/logs/` 中的异常与访问日志。
- 定期清理过期日志与无效缓存。
- 检查磁盘空间，尤其是上传目录。
- 升级依赖前先评估兼容性。

## 回滚

保留上一个版本的构建产物与数据库备份，出现问题时先回滚服务，再评估数据修复方案。

## 下一步

- [故障排查](/v1/zh/deploy/troubleshooting)
- [数据库结构](/v1/zh/guide/reference/database)
