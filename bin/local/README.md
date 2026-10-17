# BlogLoom 本地增量 SQL 升级

增量 SQL 统一维护在 `sql/increment/<版本>/` 下，文件名严格使用 AIChat 的 `<时间戳>_v<版本>.x.sql` 格式，例如 `20260916000000_v1.0.x.sql`。脚本按路径排序执行未归档的 SQL，成功后将副本记录到 `sql/local/`，不移动或删除源文件。

新增增量 SQL 使用当前开发时间；补录历史增量 SQL 时，应使用对应历史变更时间，确保其时间戳早于后续迁移。`sql/increment/1.0/` 下最早的全量脚本（含建库建表语句）即新环境初始化基线，脚本按时间戳顺序执行，全新环境首次运行即可完成初始化。

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

## 全球访客地图本地模拟数据

先通过 `upgrate-sql.sh` 执行全球访客地图增量脚本，再将可重复执行的模拟访问写入本地数据库：

```bash
MYSQL_PWD='<数据库密码>' mysql \
  -h 127.0.0.1 -P 3306 -u root blogloom \
  < bin/local/sql/global-visitor-map-mock.sql
```

该脚本只删除并重建 `remark='[GLOBAL_VISITOR_MAP_LOCAL_MOCK]'` 的测试记录，包含城市级、国家级、未知地域和非 PV 交互，用于验证全球访客地图数据库聚合接口。不要在正式数据库执行。

---

# ip2region 数据库同步

`update-ip2region.sh` 从 [ip2region 官方 data 目录](https://github.com/lionsoul2014/ip2region/tree/master/data) 同步最新的两份 XDB：IPv4 的 `ip2region_v4.xdb` 和 IPv6 的 `ip2region_v6.xdb`，保存到 `blog-backend/src/main/resources/ipdb/`。双栈查询需要这两份数据；后端通过同一个线程安全查询服务自动识别地址版本并选择对应数据。脚本会先下载并校验两份文件，再替换目标文件；下载失败时不会安装未完成的文件。

在项目根目录执行：

```bash
./bin/local/update-ip2region.sh
```

脚本依赖 `curl`，默认直接从 GitHub 官方仓库下载。网络环境需要镜像时，可覆盖下载根地址：

```bash
IP2REGION_BASE_URL=https://mirror.example.com/ip2region/master/data ./bin/local/update-ip2region.sh
```

**数据格式：** 后端使用 `org.lionsoul:ip2region:3.3.7`，从 classpath 加载上述两份文件，并以全量内存缓存提供查询。官方地域字段为 `Country|Province|City|ISP|ISO-Alpha2`；缺失项以空字段保留位置。中国城市 UV 聚合兼容旧四段格式和新五段格式，仍只汇总中国记录。

**注意：** 两份数据总计约 47 MB，位于 Maven 的 `src/main/resources` 下，会被打入后端 JAR，应用启动时也会加载到内存。旧的单文件 `ip2region.db`、`ip2region.xdb` 不是双栈服务的数据源，不应再通过重命名或覆盖的方式接入。
