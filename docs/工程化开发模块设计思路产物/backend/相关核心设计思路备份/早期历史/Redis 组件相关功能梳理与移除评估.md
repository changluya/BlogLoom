# BlogLoom Redis 组件相关功能梳理与移除评估

> 文档状态：改造已完成，记录背景、设计、实现流程与涉及代码
>
> 梳理范围：`blog-backend` 后端全部涉及 Redis 的代码路径
>
> 结论：BlogLoom 已在后端彻底移除 Redis 组件依赖，改用 MySQL 表 `cache_entry` 模拟原 Redis 的缓存语义；历史功能保持不变，并补充了单元测试与增量 SQL，后续可无缝切换回 Redis。

## 1. 历史背景

BlogLoom 最初基于 [Naccl/NBlog](https://github.com/Naccl/NBlog) 二次开发，缓存层直接沿用了 NBLog 的 Redis 方案：通过 `spring-boot-starter-data-redis` + `RedisTemplate` 承载站点运行期的全部临时数据。引入 Redis 的初衷是：

1. **浏览量暂存**：文章阅读量写多读多，先写 Redis 再定时回写数据库，避免频繁更新 MySQL。
2. **页面缓存**：首页推荐、归档、关于我、友链等低频变化页面做缓存，降低数据库压力。
3. **访客 UV 标识**：以 Redis Set 保存当天访客 UUID，用于仪表盘当日 UV 统计。
4. **访问频率限制**：点赞、评论等接口基于 Redis 计数 + TTL 做限流。
5. **QQ 头像缓存**：评论 QQ 头像 URL 去重缓存，避免重复拉取。

但在当前阶段（单机部署的个人博客、流量有限），引入 Redis 带来的运维成本（多一个组件、多一份配置、多一套部署依赖）大于收益。因此决定：**先移除 Redis 组件，用 MySQL 做等价模拟，保留统一缓存抽象，未来有需要时可无缝切回 Redis**。

### 1.1 原 Redis 引入位置（已移除）

| 层次 | 原文件 | 说明 |
| --- | --- | --- |
| 依赖 | `blog-backend/pom.xml` | `spring-boot-starter-data-redis` |
| 配置 | `conf/application.properties` | `spring.redis.host/port/password/database/timeout` |
| 序列化 | `config/RedisSerializeConfig.java` | 定义 `jsonRedisTemplate`，`Jackson2JsonRedisSerializer` |
| 缓存接口 | `service/RedisService.java` | Redis 操作抽象（Hash / Value / Set / expire） |
| 缓存实现 | `service/impl/RedisServiceImpl.java` | 基于 `RedisTemplate` 的实现 |
| Key 常量 | `constant/RedisKeyConstants.java` | 全部缓存 Key 集中定义 |

## 2. 原 Redis Key 总览与去向

| 原 Key 常量 | Key 值 | 数据结构 | 用途 | 移除后处理 |
| --- | --- | --- | --- | --- |
| `BLOG_VIEWS_MAP` | `blogViewsMap` | Hash | 博客浏览量暂存，定时回写数据库 | MySQL 前缀行模拟 Hash，`cache_entry.cache_key = blogViewsMap:<blogId>` |
| `NEW_BLOG_LIST` | `newBlogList` | Value(List) | 最新推荐博客缓存 | MySQL 单行 JSON 数组 |
| `ARCHIVE_BLOG_MAP` | `archiveBlogMap` | Value(Map) | 归档页缓存 | MySQL 单行 JSON 对象 |
| `HOME_BLOG_INFO_LIST` | `homeBlogInfoList` | Hash | 首页列表缓存（实际只删不读） | 保留清理逻辑，无实际读写 |
| `SITE_INFO_MAP` | `siteInfoMap` | Value | 站点信息缓存（实际只删不读） | 保留清理逻辑，无实际读写 |
| `ABOUT_INFO_MAP` | `aboutInfoMap` | Value(Map) | 关于我页缓存 | MySQL 单行 JSON 对象 |
| `FRIEND_INFO_MAP` | `friendInfoMap` | Value(Object) | 友链页缓存 | MySQL 单行 JSON 对象 |
| `IDENTIFICATION_SET` | `identificationSet` | Set | 当日访客 UUID 集合 | MySQL 前缀行模拟 Set，`cache_entry.cache_key = identificationSet:<uuid>` |
| `QQ_AVATAR_URL_MAP` | `qqAvatarUrlMap` | Hash | QQ 头像 URL 缓存 | MySQL 前缀行模拟 Hash |
| 动态 Key | `ip:method:uri` | Value | 访问频率限制计数 | MySQL 单行计数 + `expire_time` |

## 3. 设计思路

### 3.1 目标

1. **零 Redis 依赖**：移除 Maven 依赖、配置项、`RedisTemplate` 及其序列化配置。
2. **行为等价**：历史功能（浏览量、缓存、UV、限流、头像缓存）表现保持一致。
3. **统一抽象**：业务层不感知底层存储，未来可无缝切换 MySQL / Redis。
4. **可验证**：为缓存实现补充单元测试。
5. **可升级**：提供幂等增量 SQL，兼容全新环境与存量环境。

### 3.2 分层设计

参考 AI Chat 项目「用 MySQL 伪装 Redis」的 `CacheService` / `MysqlCacheServiceImpl` 设计，BlogLoom 落地为两层：

```text
业务层（BlogServiceImpl / VisitLogAspect / AccessLimitInterceptor ...）
        │  依赖接口，不依赖实现
        ▼
BlogCacheService（博客缓存抽象：Hash / Value / Set / increment / expire）
        │  默认实现
        ▼
MysqlBlogCacheServiceImpl（MySQL 实现）
        │  借助通用缓存原语
        ▼
CacheMapper + cache_entry 表（统一存储）

另：module.cache.CacheService / MysqlCacheServiceImpl 为通用缓存抽象（get/put/invalidate），
    是后续可复用、可替换的通用缓存底座。
```

- **`BlogCacheService`**：替代原 `RedisService` 的业务缓存契约，方法签名与原接口一一对应，保证调用方零改动语义。
- **`MysqlBlogCacheServiceImpl`**：把 Redis 的 Hash / Set 语义映射为「逻辑键 + `:` + 字段」的独立行；Value 语义映射为单行 JSON。
- **`module.cache.CacheService` / `MysqlCacheServiceImpl`**：复刻 AI Chat 的通用缓存服务，作为更通用的缓存底座。

### 3.3 Redis 语义到 MySQL 的映射规则

| Redis 语义 | MySQL 映射 | 示例 `cache_key` |
| --- | --- | --- |
| `HSET hash field value` | 独立行，键为 `hash:field`，值为字段值的 JSON | `blogViewsMap:12` |
| `HGETALL hash` | 按前缀 `hash:` 查询，截取字段名 | 前缀 `blogViewsMap:` |
| `SET key value` | 单行，键为 `key`，值为 JSON | `newBlogList` |
| `SADD set member` | 独立行，键为 `set:member` | `identificationSet:<uuid>` |
| `SCARD set` | 按前缀 `set:` 计数 | 前缀 `identificationSet:` |
| `INCR key` | `UPDATE ... SET cache_value = CAST(cache_value AS SIGNED) + n` | `blogViewsMap:12` / 限流 Key |
| `EXPIRE key seconds` | 更新 `expire_time = now() + seconds` | 限流 Key |

### 3.4 关键设计决策

1. **字段分隔符 `:`**：原 Redis Key 本身不含冒号（动态限流 Key 除外，但它只做单行操作），因此 `逻辑键:字段` 不会与 Value 键冲突。
2. **过期行不参与查询**：所有查询都带 `expire_time is null or expire_time > now()`，等价 Redis TTL。
3. **原子递增**：使用 SQL `CAST(cache_value AS SIGNED) + n` 原子更新；行不存在时补插初值，唯一键冲突（并发插入）时回退为更新，保证计数不丢。
4. **过期计数重置**：限流 Key 过期后再自增时，先命中失败，再以当前增量重置计数，随后由 `expire` 重新设置 TTL。
5. **删除兼容两种形态**：`deleteCacheByKey` 同时删除「单行 Value」和「前缀多行 Hash/Set」。
6. **`updateCache` 只更新非空字段**：避免写入时把已有 `expire_time` 清空（限流 TTL 不被后续自增覆盖）。

## 4. 核心实现流程

### 4.1 写入流程（Value / Hash / Set）

```text
业务调用 saveXxx
   → 序列化为 JSON（JacksonUtils）
   → updateCache(entry)         // 已存在则更新，返回 1
   → 若返回 0 → insertCache    // 不存在则插入
   → 若唯一键冲突（DuplicateKeyException）→ 回退 updateCache
```

### 4.2 自增流程（浏览量 / 限流）

```text
业务调用 incrementByKey / incrementByHashKey
   → incrementByKey SQL（仅更新未过期行）
   → 返回 > 0：结束
   → 返回 0：insertCache 初值
        → 冲突（行已存在但已过期）→ updateCache 重置为当前增量
   → 限流场景随后由 expire 设置 TTL
```

### 4.3 读取流程

```text
业务调用 getXxx
   → selectByKey（Value）或 selectByKeyPrefix（Hash/Set）
   → 过滤过期行
   → JSON 反序列化为标量 / List / Map / Object
```

### 4.4 应用启动流程

```text
BlogServiceImpl#saveBlogViewsToCache(@PostConstruct)
   → hasKey(blogViewsMap) ？
      → 否：从数据库读取全部浏览量 → saveMapToHash 预热缓存
      → 是：跳过
SiteSettingServiceImpl#clearSiteInfoCacheOnStartup(@PostConstruct)
   → deleteCacheByKey(siteInfoMap) 清理旧缓存
```

### 4.5 定时任务流程

| 任务 Bean（新） | 方法 | Cron | 流程 |
| --- | --- | --- | --- |
| `cacheSyncScheduleTask` | `syncBlogViewsToDatabase` | `0 0 1 * * ?` | `getMapByHash(blogViewsMap)` → 逐条 `updateViews` 回写数据库 |
| `visitorSyncScheduleTask` | `syncVisitInfoToDatabase` | `0 0 0 * * ?` | 清空 `identificationSet` → 以数据库访问日志统计昨日 PV/UV、访客 PV、城市 UV |

> 说明：任务 1 的 Spring Bean 名已由 `redisSyncScheduleTask` 改为 `cacheSyncScheduleTask`，增量 SQL 会同步更新 `schedule_job.bean_name`。

## 5. 涉及代码清单

### 5.1 新增代码

| 文件 | 说明 |
| --- | --- |
| `service/BlogCacheService.java` | 博客缓存抽象接口（替代原 `RedisService`） |
| `service/impl/MysqlBlogCacheServiceImpl.java` | 博客缓存的 MySQL 实现 |
| `module/cache/service/CacheService.java` | 通用缓存抽象（复刻 AI Chat） |
| `module/cache/service/impl/MysqlCacheServiceImpl.java` | 通用缓存的 MySQL 实现 |
| `module/cache/dao/CacheMapper.java` | 缓存 Mapper |
| `module/cache/domain/pojo/CacheEntry.java` | 缓存实体 |
| `module/cache/constant/CacheConstant.java` | 缓存常量 |
| `constant/CacheKeyConstants.java` | 缓存 Key 常量（替代 `RedisKeyConstants`） |
| `resources/mapper/CacheMapper.xml` | 缓存表 SQL 映射 |
| `sql/increment/1.0/20260920000000_v1.0.x.sql` | 增量 SQL：建 `cache_entry` + 同步定时任务 |

### 5.1.1 单元测试与接口模拟测试

| 文件 | 覆盖范围 | 条数 |
| --- | --- | --- |
| `src/test/.../module/cache/InMemoryCacheMapper.java` | 测试用内存 Mapper（模拟表语义：过期过滤、前缀查询、原子自增、唯一键冲突） | — |
| `src/test/.../service/impl/MysqlBlogCacheServiceImplTest.java` | 博客缓存实现：Hash / Value / Set / 自增 / 过期 / 删除 / PageResult 转换 | 22 |
| `src/test/.../module/cache/MysqlCacheServiceImplTest.java` | 通用缓存实现：get/put/invalidate/loader/过期/统计/清理 | 12 |
| `src/test/.../interceptor/AccessLimitInterceptorTest.java` | 限流接口：首次放行、未超限自增、超限 403、过期重置 | 5 |
| `src/test/.../service/impl/BlogServiceImplCacheTest.java` | 博客业务缓存路径：阅读量、推荐/归档缓存命中与回源、增删改缓存失效、启动预热逻辑 | 11 |
| `src/test/.../service/impl/AboutServiceImplTest.java` | 关于我缓存命中/回源/失效 | 3 |
| `src/test/.../service/impl/FriendServiceImplTest.java` | 友链缓存命中/回源/失效 | 4 |
| `src/test/.../service/impl/VisitorServiceImplTest.java` | 访客 UUID 缓存删除、数据库校验 | 2 |
| `src/test/.../task/CacheSyncScheduleTaskTest.java` | 浏览量定时回写数据库 | 2 |
| `src/test/.../task/VisitorSyncScheduleTaskTest.java` | 访客标识集合清理与昨日 PV/UV 统计 | 1 |
| `src/test/.../controller/CacheInterfaceSimulationTest.java` | 接口模拟：`/admin/dashboard` UV、`/blog` 阅读量自增、`/blogs` 列表、`/moment/like/{id}` 限流 | 4 |
| `src/test/resources/application.properties` | 测试专用配置（H2 内存库、token、评论、邮件、Quartz） | — |
| `src/test/resources/schema.sql` | 测试用最小 H2 表结构（`cache_entry` / `schedule_job` / `blog`） | — |

### 5.2 修改代码

| 文件 | 改动 |
| --- | --- |
| `pom.xml` | 移除 `spring-boot-starter-data-redis`，新增 H2（`test` 作用域，仅测试用） |
| `conf/application.properties`、`conf/application.properties.conf` | 移除 `spring.redis.*` 配置 |
| `service/BlogService.java` | `updateViewsToRedis` → `updateViewsToCache` |
| `service/impl/BlogServiceImpl.java` | 注入 `BlogCacheService`，重命名 `saveBlogViewsToCache`、`setBlogViewsFromCacheToPageResult`、`deleteBlogCache` 等 |
| `interceptor/AccessLimitInterceptor.java` | 注入 `BlogCacheService` 实现限流 |
| `aspect/VisitLogAspect.java` | 访客 UUID 读写改为 `BlogCacheService` |
| `controller/admin/DashboardAdminController.java` | 当日 UV 统计改用 `BlogCacheService` |
| `controller/BlogController.java` | 阅读量自增改调 `updateViewsToCache` |
| `controller/admin/VisitorAdminController.java` | 注释同步更新 |
| `service/impl/AboutServiceImpl.java` | 关于我缓存改用 `BlogCacheService` |
| `service/impl/FriendServiceImpl.java` | 友链缓存改用 `BlogCacheService` |
| `service/impl/SiteSettingServiceImpl.java` | 站点信息缓存清理改用 `BlogCacheService` |
| `service/impl/VisitorServiceImpl.java` | 访客 UUID 删除改用 `BlogCacheService` |
| `util/comment/CommentUtils.java` | QQ 头像缓存改用 `BlogCacheService` |
| `task/RedisSyncScheduleTask.java` → `task/CacheSyncScheduleTask.java` | 类与 Bean 重命名，Map 类型安全化 |
| `task/VisitorSyncScheduleTask.java` | 访客标识集合清理改用 `BlogCacheService` |
| `resources/mapper/BlogMapper.xml` | 注释同步更新 |
| `sql/increment/1.0/20260915000000_v1.0.x.sql` | 全量基线补充 `cache_entry` 表与定时任务调整 |

### 5.3 删除代码

| 文件 | 原因 |
| --- | --- |
| `config/RedisSerializeConfig.java` | Redis 专用序列化配置 |
| `service/RedisService.java` | 被 `BlogCacheService` 取代 |
| `service/impl/RedisServiceImpl.java` | 被 `MysqlBlogCacheServiceImpl` 取代 |
| `constant/RedisKeyConstants.java` | 被 `CacheKeyConstants` 取代 |

## 6. 数据库表结构

```sql
CREATE TABLE `cache_entry` (
    `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `cache_key` varchar(512) NOT NULL COMMENT '缓存键',
    `cache_value` longtext NULL COMMENT '缓存值(JSON格式)',
    `cache_type` varchar(100) NULL DEFAULT 'DEFAULT' COMMENT '缓存类型',
    `expire_time` datetime(0) NULL DEFAULT NULL COMMENT '过期时间，为空表示永不过期',
    `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cache_key` (`cache_key`)
) COMMENT = '缓存表（替代 Redis）';
```

## 7. 验证

### 7.1 测试策略

- **缓存实现**：用内存版 `InMemoryCacheMapper` 驱动真实的 `MysqlBlogCacheServiceImpl` / `MysqlCacheServiceImpl`，验证 Hash / Value / Set / 自增 / 过期 / 唯一键冲突等语义。
- **业务与接口**：用 Mockito 模拟数据库与外部服务，注入真实缓存实现，验证各业务层与接口层的缓存行为。
- **接口模拟**：用 `MockMvc` 独立启动控制器 + 限流拦截器，模拟 `/admin/dashboard`、`/blog`、`/blogs`、`/moment/like/{id}` 等请求。
- **上下文启动**：`BlogApiApplicationTests` 使用 `src/test/resources` 下的 H2 内存库与最小表结构（`schema.sql`），无需外部 MySQL/Redis 即可加载完整 Spring 上下文。

### 7.2 结果

- 编译：`mvn -o clean test` 通过。
- 单元测试：全项目 **94 条测试全部通过**（`Tests run: 94, Failures: 0, Errors: 0, Skipped: 0`），其中本次新增缓存相关测试 66 条。
- 说明：`BlogApiApplicationTests` 原为依赖外部数据库的骨架测试，本次通过测试专用 H2 配置修复，现已纳入全绿范围。

## 8. 后续可扩展性

- **切回 Redis**：只需新增 `RedisBlogCacheServiceImpl implements BlogCacheService` 并调整 Bean 装配，业务代码零改动。
- **本地缓存**：可参照 AI Chat 的 `GuavaCacheServiceImpl` 增加 Caffeine/Guava 实现。
- **过期清理**：`MysqlCacheServiceImpl#cleanExpired` 已提供，可挂到定时任务中定期清理过期行。
- **表增长控制**：`cache_entry` 仅保存有效缓存，建议结合 `cleanExpired` 定期回收。
