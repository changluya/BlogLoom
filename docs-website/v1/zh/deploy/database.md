---
title: "初始化数据库"
description: 创建数据库并通过增量脚本完成初始化。
---

BlogLoom 使用 MySQL 存储业务数据。初始化通过 `bin/local/upgrate-sql.sh` 执行增量 SQL 完成，不再需要手动导入 `init.sql`。

## 1. 创建数据库

```sql
CREATE DATABASE blogloom
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

> 脚本执行前会校验数据库已存在，因此需要先手动创建。

## 2. 配置连接

首次使用先复制配置模板：

```bash
cp bin/local/deploy.conf.example bin/local/deploy.conf
```

编辑 `bin/local/deploy.conf`，填写：

| 配置 | 说明 |
| --- | --- |
| `DB_HOST` / `DB_PORT` | MySQL 地址与端口 |
| `DB_USER` / `DB_PASSWORD` | 数据库账号与密码 |
| `DB_NAME` | 数据库名，默认 `blogloom` |

也可以使用其他配置文件：

```bash
LOCAL_UPGRADE_CONFIG=/path/to/deploy.conf bin/local/upgrate-sql.sh
```

## 3. 执行增量 SQL

先预览待执行脚本，再正式执行：

```bash
bin/local/upgrate-sql.sh --dry-run   # 只列出待执行 SQL，不连接数据库
bin/local/upgrate-sql.sh             # 执行并记录
```

脚本行为：

- **待执行**：`sql/increment/<版本>/**/*.sql`，按文件名（时间戳）排序；
- **执行记录**：成功后把副本写入扁平目录 `sql/local/`；
- **幂等**：已记录在 `sql/local/` 的文件会被跳过，可安全重复执行；
- **失败保留现场**：执行失败时工作文件与日志保留在 `sql/local/.work/`。

## 账号与安全

- 为应用创建独立的数据库账号，避免使用 root。
- 仅授予必要的库表权限。
- 不要把数据库密码提交到版本库。

## 下一步

- [部署后端](/v1/zh/deploy/backend)
- [数据库结构](/v1/zh/guide/reference/database)
