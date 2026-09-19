# BlogLoom Redis 组件相关功能梳理与移除评估

> 文档状态：现状梳理，用于评估「直接移除 Redis」的改造范围
>
> 梳理范围：`blog-backend` 后端全部涉及 Redis 的代码路径
>
> 结论先行：当前 Redis 承担了 4 类职责——浏览量暂存、页面缓存、访客 UV 标识、访问频率限制。其中页面缓存与访客 UV 标识属于「可替代」；浏览量暂存与频率限制在单机部署下也可用本地实现替代。

## 1. Redis 组件引入位置

| 层次 | 文件 | 说明 |
| --- | --- | --- |
| 依赖 | `blog-backend/pom.xml:115-119` | `spring-boot-starter-data-redis` |
| 配置 | `conf/application.properties:20-25` | host/port/password/database/timeout |
| 序列化 | `config/RedisSerializeConfig.java` | 定义 `jsonRedisTemplate`，使用 `Jackson2JsonRedisSerializer` |
| 封装接口 | `service/RedisService.java` | 统一的 Redis 操作抽象（Hash / Value / Set / 过期） |
| 封装实现 | `service/impl/RedisServiceImpl.java` | 基于 `RedisTemplate` 的实现 |
| Key 常量 | `constant/RedisKeyConstants.java` | 全部缓存 Key 的集中定义 |

## 2. Redis Key 总览

| Key 常量 | Key 值 | 数据结构 | 用途 | 读取方 | 写入/清理方 |
| --- | --- | --- | --- | --- | --- |
| `BLOG_VIEWS_MAP` | `blogViewsMap` | Hash | 博客浏览量暂存，定时回写数据库 | 首页/分类/标签/详情/统计 | 启动初始化、阅读自增、增删改博客、定时任务 |
| `NEW_BLOG_LIST` | `newBlogList` | Value(List) | 最新推荐博客缓存 | `/site` | `/site` 未命中回填、博客增删改时清理 |
| `ARCHIVE_BLOG_MAP` | `archiveBlogMap` | Value(Map) | 归档页缓存 | `/archives` | `/archives` 未命中回填、博客增删改时清理 |
| `HOME_BLOG_INFO_LIST` | `homeBlogInfoList` | Hash | 首页列表缓存 | **无实际读取**（见 4.1） | 博客增删改时清理 |
| `SITE_INFO_MAP` | `siteInfoMap` | Value | 站点信息缓存 | **无实际读取**（见 4.2） | 启动、更新站点设置/上传图片时清理 |
| `ABOUT_INFO_MAP` | `aboutInfoMap` | Value(Map) | 关于我页缓存 | `/about` | `/about` 未命中回填、后台更新时清理 |
| `FRIEND_INFO_MAP` | `friendInfoMap` | Value(Object) | 友链页缓存 | `/friends`、`/comments`（评论开关判断） | `/friends` 未命中回填、后台更新时清理 |
| `IDENTIFICATION_SET` | `identificationSet` | Set | 当日访客 UUID 集合，用于 UV 统计 | `/admin/dashboard` 计数 | 访客埋点写入、删除访客、每日 0 点清空 |
| `QQ_AVATAR_URL_MAP` | `qqAvatarUrlMap` | Hash | QQ 头像 URL 缓存，避免重复上传 | 评论提交 | 评论提交时回填 |
| （动态 Key） | `ip:method:uri` | Value | 访问频率限制计数 | 拦截器校验 | 拦截器自增、带 TTL 过期 |

## 3. 涉及 Redis 的后端接口

### 3.1 前台接口

| 接口 | 方法 | 触发位置 | 涉及 Redis 能力 | 用途 |
| --- | --- | --- | --- | --- |
| `/blogs` | GET | `BlogServiceImpl#getBlogInfoListByIsPublished` → `setBlogViewsFromRedisToPageResult` | `BLOG_VIEWS_MAP` 读 | 列表浏览量取 Redis 最新值 |
| `/blog` | GET | `BlogServiceImpl#getBlogByIdAndIsPublished`、`#updateViewsToRedis` | `BLOG_VIEWS_MAP` 读 + 自增 | 详情浏览量读取，阅读量 +1 |
| `/archives` | GET | `BlogServiceImpl#getArchiveBlogAndCountByIsPublished` | `ARCHIVE_BLOG_MAP` 读/写 | 归档结果缓存 |
| `/category` | GET | `BlogServiceImpl#getBlogInfoListByCategoryNameAndIsPublished` | `BLOG_VIEWS_MAP` 读 | 列表浏览量取 Redis 最新值 |
| `/tag` | GET | `BlogServiceImpl#getBlogInfoListByTagNameAndIsPublished` | `BLOG_VIEWS_MAP` 读 | 列表浏览量取 Redis 最新值 |
| `/site` | GET | `BlogServiceImpl#getNewBlogListByIsPublished`、`#sumViewsByIsPublished` | `NEW_BLOG_LIST` 读/写、`BLOG_VIEWS_MAP` 读 | 最新推荐缓存、总浏览量统计 |
| `/about` | GET | `AboutServiceImpl#getAboutInfo` | `ABOUT_INFO_MAP` 读/写 | 关于我页缓存 |
| `/friends` | GET | `FriendServiceImpl#getFriendInfo(true,true)` | `FRIEND_INFO_MAP` 读/写 | 友链页缓存 |
| `/friend` | POST | `VisitLogAspect` | `IDENTIFICATION_SET` 写 | 访客埋点签发/校验 UUID |
| `/moments` | GET | `VisitLogAspect` | `IDENTIFICATION_SET` 写 | 访客埋点签发/校验 UUID |
| `/moment/like/{id}` | POST | `AccessLimitInterceptor`、`VisitLogAspect` | 动态限流 Key、`IDENTIFICATION_SET` 写 | 24h 点赞限流 + 访客埋点 |
| `/comments` | GET | `CommentUtils#judgeCommentState` → `FriendServiceImpl#getFriendInfo(true,false)` | `FRIEND_INFO_MAP` 读 | 友链页评论开关判断 |
| `/comment` | POST | `AccessLimitInterceptor`、`CommentUtils#setCommentQQAvatar` | 动态限流 Key、`QQ_AVATAR_URL_MAP` 读/写 | 30s 评论限流 + QQ 头像缓存 |
| `/checkBlogPassword` | POST | `VisitLogAspect` | `IDENTIFICATION_SET` 写 | 访客埋点 |
| `/searchBlog` | GET | `VisitLogAspect` | `IDENTIFICATION_SET` 写 | 访客埋点 |
| 所有带 `@VisitLogger` 的接口 | — | `VisitLogAspect#checkIdentification` / `#saveUUID` | `IDENTIFICATION_SET` 读/写 | 访客识别与每日 UV |

> 注：`VisitLogAspect` 是 AOP 环绕通知，凡标注 `@VisitLogger` 的接口都会触发 `IDENTIFICATION_SET` 的读写，上表已标注主要入口。

### 3.2 后台接口

| 接口 | 方法 | 触发位置 | 涉及 Redis 能力 | 用途 |
| --- | --- | --- | --- | --- |
| `/admin/dashboard` | GET | `DashboardAdminController#dashboard` | `IDENTIFICATION_SET` 计数 | 今日 UV 统计 |
| `/admin/visitor` | DELETE | `VisitorServiceImpl#deleteVisitor` | `IDENTIFICATION_SET` 删除 | 删除访客时同步移除 UUID |
| `/admin/blog` | DELETE | `BlogServiceImpl#deleteBlogById` | `BLOG_VIEWS_MAP` 删除 + 页面缓存清理 | 移入回收站 |
| `/admin/blog/recycle/restore` | PUT | `BlogServiceImpl#restoreBlogById` | `BLOG_VIEWS_MAP` 回填 + 页面缓存清理 | 回收站恢复 |
| `/admin/blog/recycle/restore` | POST | `BlogServiceImpl#restoreBlogById`（批量） | `BLOG_VIEWS_MAP` 回填 + 页面缓存清理 | 批量恢复 |
| `/admin/blog/recycle` | DELETE | `BlogServiceImpl#deleteBlogPermanentlyById` | `BLOG_VIEWS_MAP` 删除 + 页面缓存清理 | 彻底删除 |
| `/admin/blog/recycle/delete` | POST | `BlogServiceImpl#deleteBlogPermanentlyById`（批量） | `BLOG_VIEWS_MAP` 删除 + 页面缓存清理 | 批量彻底删除 |
| `/admin/blog/top` | PUT | `BlogServiceImpl#updateBlogTopById` | `HOME_BLOG_INFO_LIST` 清理 | 置顶状态变更 |
| `/admin/blog/{id}/visibility` | PUT | `BlogServiceImpl#updateBlogVisibilityById` | `HOME_BLOG_INFO_LIST`/`NEW_BLOG_LIST`/`ARCHIVE_BLOG_MAP` 清理 | 可见性变更 |
| `/admin/blog` | GET | `BlogServiceImpl#getBlogById` | `BLOG_VIEWS_MAP` 读 | 后台详情浏览量 |
| `/admin/blog` | POST | `BlogServiceImpl#saveBlog` | `BLOG_VIEWS_MAP` 写 + 页面缓存清理 | 发布文章 |
| `/admin/blog` | PUT | `BlogServiceImpl#updateBlog` | `BLOG_VIEWS_MAP` 写 + 页面缓存清理 | 更新文章 |
| `/admin/siteSettings` | POST | `SiteSettingServiceImpl#updateSiteSetting` | `SITE_INFO_MAP` 清理 | 更新站点配置 |
| `/admin/siteSettings/{id}/image` | POST | `SiteSettingServiceImpl#uploadImage` | `SITE_INFO_MAP` 清理 | 上传站点图片 |
| `/admin/about` | PUT | `AboutServiceImpl#updateAbout` | `ABOUT_INFO_MAP` 清理 | 更新关于我 |
| `/admin/friendInfo/commentEnabled` | PUT | `FriendServiceImpl#updateFriendInfoCommentEnabled` | `FRIEND_INFO_MAP` 清理 | 友链评论开关 |
| `/admin/friendInfo/content` | PUT | `FriendServiceImpl#updateFriendInfoContent` | `FRIEND_INFO_MAP` 清理 | 友链页内容 |

## 4. 非接口触发的 Redis 使用

### 4.1 启动钩子

| 位置 | 行为 | 涉及 Key |
| --- | --- | --- |
| `BlogServiceImpl#saveBlogViewsToRedis`（`@PostConstruct`） | 启动时若 Hash 不存在，从数据库加载全部浏览量写入 Redis | `BLOG_VIEWS_MAP` |
| `SiteSettingServiceImpl#clearSiteInfoCacheOnStartup`（`@PostConstruct`） | 启动时清理站点信息缓存 | `SITE_INFO_MAP` |

### 4.2 定时任务（Quartz，存于 `schedule_job` 表）

| Bean | 方法 | Cron | 行为 | 涉及 Key |
| --- | --- | --- | --- | --- |
| `redisSyncScheduleTask` | `syncBlogViewsToDatabase` | `0 0 1 * * ?`（每天 01:00） | 将 Redis 浏览量回写数据库 | `BLOG_VIEWS_MAP` |
| `visitorSyncScheduleTask` | `syncVisitInfoToDatabase` | `0 0 0 * * ?`（每天 00:00） | 清空访客标识集合，统计昨日 PV/UV、访客 PV、城市 UV | `IDENTIFICATION_SET` |

### 4.3 两个「只写不读」的缓存

- `HOME_BLOG_INFO_LIST`：仅在博客变更时被删除，`RedisService#getBlogInfoPageResultByHash` 定义了读取方法但**全项目无调用**。
- `SITE_INFO_MAP`：仅在启动和站点设置变更时被删除，`getSiteInfo()` 每次直接查库，**没有读取缓存**。

这两处属于遗留缓存，移除 Redis 时可直接删除对应代码，无需替代。

## 5. 移除 Redis 的影响面小结

| 能力 | 使用场景 | 移除后可选替代方案 |
| --- | --- | --- |
| 浏览量暂存 | 阅读量 +1、定时回写 | 直接写库（`views = views + 1`）；或 JVM 内存 Map + 定时落库 |
| 页面缓存 | 最新推荐、归档、关于我、友链 | 直接查库；或 Caffeine 本地缓存 |
| 访客 UV 标识 | 每日 UV 统计、访客识别 | 直接基于数据库 `visit_log` 去重统计；访客 UUID 存 Cookie/请求头即可 |
| 访问频率限制 | 点赞、评论限流 | 单机场景改用 Caffeine / `ConcurrentHashMap` + 过期；分布式再考虑 Redis |
| QQ 头像缓存 | 评论头像复用 | 直接查库或本地缓存，也可每次直接拼接 URL |

> 综合来看，当前 Redis 的职责在单机个人博客场景下均可由数据库或 JVM 本地缓存替代，具备整体移除的可行性。
