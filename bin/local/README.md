# BlogLoom 本地增量 SQL 升级

增量 SQL 统一维护在 `sql/increment/<版本>/` 下，文件名严格使用 AIChat 的 `<时间戳>_v<版本>.x.sql` 格式，例如 `20260916000000_v1.0.x.sql`。脚本按路径排序执行未归档的 SQL，成功后将副本记录到 `sql/local/`，不移动或删除源文件。

新增增量 SQL 使用当前开发时间；补录历史增量 SQL 时，应使用对应历史变更时间，确保其时间戳早于后续迁移。`sql/increment/init.sql` 是新环境全量初始化基线，升级脚本会明确排除它。

首次使用：

```bash
cp bin/local/deploy.conf.example bin/local/deploy.conf
```

编辑 `deploy.conf` 后先预览待执行脚本：

```bash
./bin/local/upgrate-sql.sh --dry-run
```

确认后执行：

```bash
./bin/local/upgrate-sql.sh
```

如需使用其他配置文件：

```bash
LOCAL_UPGRADE_CONFIG=/path/to/deploy.conf ./bin/local/upgrate-sql.sh --dry-run
```

`sql/local/` 是本机执行记录，已加入 `.gitignore`。不同版本的增量 SQL 也不能使用相同文件名。
