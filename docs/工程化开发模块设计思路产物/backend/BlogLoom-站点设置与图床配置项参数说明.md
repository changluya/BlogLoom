# 站点设置

> 站点设置数据统一存放在数据库表 `site_setting`，字段为 `id`、`name_en`、`name_zh`、`value`、`type`。
> 后台「站点设置」页面通过 `GET /admin/siteSettings` 拉取，按 `type` 分组返回 `type1`、`type2`、`type3`、`type5`、`type6`；保存走 `POST /admin/siteSettings`。
> 本文按后台页面分区梳理全部配置项（`type=4` 的友链信息由「友链管理」独立维护，不在站点设置页内，文末单列说明）。
>
> 最新变更：新增 `type=6`「图床配置」分组（上传渠道选择 + 阿里云参数），由后台「图床管理 - 设置图床」页面维护，并驱动所有图片上传按渠道落库，详见文末《图床设置》。

## 基础设置

对应 `type=1`，后台分组键 `type1`。多为站点全局文案与图片。

| nameEn | nameZh | 值类型 | 说明 | 默认值 |
| --- | --- | --- | --- | --- |
| `blogName` | 博客名称 | 文本 | 站点名称，用于页面标题等 | `Changlu's Blog` |
| `webTitleSuffix` | 网页标题后缀 | 文本 | 拼接在浏览器标题后，如 `文章标题 - Changlu's Blog` | ` - Changlu's Blog` |
| `favicon` | 网站 Tab 图标 | 图片 | 网站 Tab 与后台左上角图标，支持图片上传 | `/img/site-favicon.png` |
| `footerImgTitle` | 页脚图片标题 | 文本 | 页脚图片说明文案 | `手机看本站` |
| `footerImgUrl` | 页脚二维码 | 图片 | 前台页脚展示（通常为手机访问二维码），支持图片上传；`name_zh` 由「页脚图片路径」更名而来 | `/img/qr.png` |
| `copyright` | Copyright | JSON | 页脚版权信息，见下方结构 | `{"title":"Copyright © 2026","siteName":"Changlu's Blog"}` |
| `beian` | ICP备案号 | 文本 | 页脚备案号 | 空 |
| `reward` | 赞赏码 | 图片 | 文章赞赏弹窗图片，支持图片上传 | `/img/reward.jpg?v=20260913` |
| `commentAdminFlag` | 博主评论标识 | 文本 | 标记博主评论的前缀文案 | `咕咕` |
| `playlistServer` | 播放器平台 | 文本 | 前台音乐播放器平台标识 | `netease` |
| `playlistId` | 播放器歌单 | 文本 | 前台音乐播放器歌单 ID | `3071528549` |
| `hitokotoTexts` | 页脚文案 | JSON 数组串 | 前台页脚随机展示，格式为多条双引号包裹文案用英文逗号分隔 | `"要改变别人的心真是件很难办的事，不过改变自己要容易一点。","Stay Hungry. Stay Foolish."` |

`copyright` 的值结构（后端 VO `Copyright`）：

| 字段 | 说明 |
| --- | --- |
| `title` | 版权文案 |
| `siteName` | 站点名称 |
| `siteUrl` | 站点链接，默认 `/` |

> 图片上传接口 `POST /admin/siteSettings/{id}/image` 仅放行 `footerImgUrl`、`reward`、`favicon`、`avatar` 四项。

## 资料卡

对应 `type=2`，后台分组键 `type2`。用于前台侧边栏个人资料卡。

| nameEn | nameZh | 值类型 | 说明 | 默认值 |
| --- | --- | --- | --- | --- |
| `avatar` | 头像 | 图片 | 资料卡头像，支持图片上传 | `/img/avatar.jpg?v=20260913` |
| `name` | 昵称 | 文本 | 资料卡昵称 | `changlu` |
| `rollText` | 滚动个签 | JSON 数组串 | 多条个签用双引号包裹、英文逗号分隔，后台支持拖拽排序 | `"每个人都是独一无二的，把握好自己的节奏，跟着自己的心走～","Stay Hungry. Stay Foolish."` |
| `github` | GitHub | 链接 | 社交链接，空值/`#`/`null`/`undefined` 会被忽略 | `https://github.com/changluya/BlogLoom` |
| `telegram` | Telegram | 链接 | 同上 | 空 |
| `qq` | QQ | 链接 | 同上 | 空 |
| `bilibili` | bilibili | 链接 | 同上 | `https://space.bilibili.com/481905751?...` |
| `netease` | 网易云音乐 | 链接 | 同上 | 空 |
| `email` | email | 链接 | 同上，通常为 `mailto:` 形式 | `mailto:changlucode666@163.com` |
| `favorite` | 自定义 | JSON | 自定义爱好条目，可在后台动态「添加自定义」，可多条 | 无预置，动态新增 |

`favorite` 的值结构（后端 VO `Favorite`）：

| 字段 | 说明 |
| --- | --- |
| `title` | 爱好标题 |
| `content` | 爱好内容 |

## 页脚徽标

对应 `type=3`，后台分组键 `type3`。前台页脚展示的 shields.io 风格徽标，可多条动态增删。

| nameEn | nameZh | 值类型 | 说明 |
| --- | --- | --- | --- |
| `badge` | 徽标 | JSON | 单条徽标，可添加多条 |

`badge` 的值结构（后端 VO `Badge`）：

| 字段 | 说明 |
| --- | --- |
| `title` | 徽标提示标题 |
| `url` | 点击跳转链接 |
| `subject` | 左侧标签 |
| `value` | 右侧内容 |
| `color` | 右侧颜色（shields.io 颜色名） |

默认值：`{"title":"本博客已开源于 GitHub","url":"https://github.com/changluya/BlogLoom","subject":"BlogLoom","value":"Open Source","color":"brightgreen"}`

## 自定义前台展示模块

对应 `type=5`，后台分组键 `type5`。在博客前台侧栏展示一个自定义模块，值为 JSON，后台可编辑标题、内容并控制开关。

| nameEn | nameZh | 值类型 | 说明 | 默认值 |
| --- | --- | --- | --- | --- |
| `customModule` | 自定义前台展示模块 | JSON | 见下方结构 | `{"title":"","content":"","enabled":false}` |

`customModule` 的值结构（后端 VO `CustomModule`）：

| 字段 | 说明 |
| --- | --- |
| `title` | 栏目标题，如「技术交流群」 |
| `content` | 栏目内容，支持自定义 HTML |
| `enabled` | 是否开启，开启后前台侧栏展示 |

> 说明：`type=5` 分组按 `name_en=customModule` 识别，与 `type` 值解耦；历史上该配置曾被归入资料卡，现独立成组。

## 图床配置（type=6）

对应 `type=6`，后台分组键 `type6`。持久化图片上传渠道及其参数，由「图床管理 - 设置图床」页面维护，是「站点设置」之外独立入口但同表存储的配置。

| nameEn | nameZh | 值类型 | 说明 | 默认值 |
| --- | --- | --- | --- | --- |
| `uploadChannelChoose` | 上传渠道选择 | 文本 | 当前上传渠道，可选 `local`（本地）、`aliyun`（阿里云 OSS） | `local` |
| `uploadChannelAliyun` | 阿里云配置 | JSON | 阿里云 OSS 渠道参数，见下方结构 | `{"accessKeyId":"","accessKeySecret":"","bucket":"","area":"","path":""}` |

`uploadChannelAliyun` 的值结构（后端 VO `AliyunOssConfig`）：

| 字段 | 说明 |
| --- | --- |
| `accessKeyId` | 阿里云访问密钥 ID |
| `accessKeySecret` | 阿里云访问密钥 Secret |
| `bucket` | OSS Bucket 名称 |
| `area` | 地域节点，如 `oss-cn-beijing` |
| `path` | 上传路径前缀，如 `blogloom/` |

> 这两条记录由增量脚本 `sql/increment/1.0/20260919220000_v1.0.x.sql` 幂等写入（先按 `name_en` 删除再插入，可重复执行）。渠道行为详见《图床设置》。

## 附：友链信息（type=4）

`type=4` 的友链配置由「友链管理」独立维护，不通过站点设置页编辑，`GET /admin/siteSettings` 也不会返回该分组。相关记录：

| nameEn | nameZh | 说明 | 默认值 |
| --- | --- | --- | --- |
| `friendContent` | 友链页面信息 | 友链页顶部说明 Markdown | 见初始化 SQL |
| `friendCommentEnabled` | 友链页面评论开关 | `1` 开启、`0` 关闭 | `1` |

## 接口与数据流

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/admin/siteSettings` | GET | 返回 `{type1, type2, type3, type5, type6}` 分组列表 |
| `/admin/siteSettings` | POST | 入参 `{settings, deleteIds}`，批量新增/更新/删除；图床配置也复用它保存 |
| `/admin/siteSettings/{id}/image` | POST | 站点设置图片上传，仅限 `footerImgUrl`/`reward`/`favicon`/`avatar` |
| `/admin/webTitleSuffix` | GET | 单独查询网页标题后缀 |
| `/site` | GET | 前台聚合接口，返回 `introduction`、`siteInfo`、`badges`、`customModule` 等 |

> 缓存：站点信息写入 Redis（`RedisKeyConstants.SITE_INFO_MAP`），每次更新站点设置或上传图片后清理；应用启动时也会主动清理一次。图床渠道配置不缓存，每次上传时按 `name_en` 直查数据库，改配置即时生效。

> 注意：建表语句中 `type` 字段注释为「1基础设置，2页脚徽标，3资料卡，4友链信息」，与实际代码分组不一致。以代码为准：`2` 为资料卡、`3` 为页脚徽标、`5` 为自定义前台展示模块、`6` 为图床配置。

---

# 图床设置

> BlogLoom 后端内置图床渠道，配置持久化在 `site_setting`（`type=6`），由后台「图床管理 - 设置图床」页面维护。所有图片上传接口在运行时按当前渠道选取实现，本地与阿里云的业务后缀路径完全一致。

## 图床选择

当前渠道由 `site_setting.uploadChannelChoose` 决定，取值与后端渠道组件一一对应：

| 渠道值 | 名称 | 实现组件 | 存储位置 | 访问方式 |
| --- | --- | --- | --- | --- |
| `local` | 本地（默认） | `LocalUploadChannel` | `${user.dir.upload}` | `{blog.api}/static/**` |
| `aliyun` | 阿里云 OSS | `AliyunUploadChannel` | OSS Bucket | OSS 域名 |

选择与路由机制：

- `UploadChannelManager` 在每次上传时读取 `uploadChannelChoose`，从注册的渠道组件中选取对应实现；未配置或取值未知时回退 `local`。
- 渠道通过 `site_setting` 动态切换，无需改配置文件、无需重启。
- 后台入口：`图床管理 - 设置图床`（路由 `/imageHost/setting`），保存复用 `POST /admin/siteSettings`。
- 本地渠道使用系统默认上传目录（`conf/upload`），无需额外参数。

受当前渠道影响的上传入口：

| 场景 | 上传接口 | 渠道内相对路径 |
| --- | --- | --- |
| 博客编辑器正文/描述/封面 | `POST /admin/blog/resources` | `tmp/{uuid}.{ext}` → 归档 `blogs/{blogId}/**` |
| 站点设置图片（头像、赞赏码等） | `POST /admin/siteSettings/{id}/image` | `site/{uuid}.{ext}` |
| 专栏封面 | `POST /admin/column/{id}/cover` | `blogColumn/{columnId}/{uuid}.{ext}` |
| 评论 QQ 头像 | 评论保存时触发 | `{uuid}.{ext}` |

> 后缀路径与《BlogLoom 上传文件目录与访问路径规范》一致；切换渠道只改变「前缀」与「访问域名」，不改变 `tmp/`、`blogs/{id}/`、`site/`、`blogColumn/{id}/` 等业务后缀。

## 阿里云

阿里云参数以 JSON 字符串存于 `site_setting.uploadChannelAliyun`，字段结构见《图床配置（type=6）》。运行时映射规则：

| 项 | 规则 |
| --- | --- |
| Endpoint | `https://{area}.aliyuncs.com`（`area` 缺少 `.aliyuncs.com` 时自动补全） |
| Object Key | `{path}{相对路径}`，如 `blogloom/tmp/{uuid}.png` |
| 访问 URL | `https://{bucket}.{area}.aliyuncs.com/{path}{相对路径}`（路径分段做 URL 编码） |

渠道实现基于 `aliyun-sdk-oss:3.15.1`，覆盖与本地对等的完整生命周期：

| 操作 | SDK 方法 | 用途 |
| --- | --- | --- |
| 上传 | `putObject` | 图片上传到 `{path}{相对路径}` |
| 复制 | `copyObject` | 文章保存时把 `tmp/**` 归档到 `blogs/{blogId}/**` |
| 删除 | `deleteObject` | 归档成功后清理 `tmp` |
| 存在性 | `doesObjectExist` | 归档前判断源资源，缺失时返回 `null` 以保留原 URL |
| URL 反解 | `toRelativePath` | 从 OSS URL 剥掉域名与 `path` 前缀，还原业务相对路径 |

> 参数不完整（缺 `accessKeyId`/`accessKeySecret`/`bucket`/`area`）时上传会直接抛出「阿里云图床参数不完整」提示，不会静默回退本地。
> 安全提示：`accessKeyId`/`accessKeySecret` 以明文存于数据库，请勿导出到公开仓库或文档。

## 附：外部 PicGo 工具配置

> 以下为博客写作时使用的独立工具 **PicGo** 配置，与 BlogLoom 后端的图床渠道相互独立，仅作记录。

PicGo 当前默认图床为**阿里云 OSS**，配置文件中 `current` 与 `uploader` 均为 `aliyun`。

| 配置项 | 值 | 说明 |
| --- | --- | --- |
| `picBed.current` | `aliyun` | 当前选中的默认图床 |
| `picBed.uploader` | `aliyun` | 当前默认上传器 |
| `picBed.smms.token` | 空 | SM.MS 图床未配置 |
| `settings.autoRename` | `true` | 上传自动重命名 |
| `settings.uploadNotification` | `true` | 上传结果通知 |
| `settings.privacyEnsure` | `true` | 隐私保护 |
| `settings.showUpdateTip` | `false` | 关闭更新提示 |
| `settings.shortKey.picgo:upload` | `CommandOrControl+Shift+P` | 快捷上传快捷键，已启用 |
| `settings.server` | `127.0.0.1:36677`，`enable=true` | PicGo 本地 HTTP 服务 |
| `picgoPlugins` | 空 | 未安装插件 |

PicGo 阿里云 OSS 图床参数：

| 参数 | 值 | 说明 |
| --- | --- | --- |
| `accessKeyId` | `CHANGE_ME_ALIYUN_ACCESS_KEY_ID` | 阿里云访问密钥 ID |
| `accessKeySecret` | `CHANGE_ME_ALIYUN_ACCESS_KEY_SECRET` | 阿里云访问密钥 Secret |
| `bucket` | `pictured-bed` | OSS Bucket 名称 |
| `area` | `oss-cn-beijing` | 地域节点（华北 2·北京） |
| `path` | `img/2024/` | 上传文件存储路径前缀 |
| `customUrl` | 空 | 自定义访问域名，未配置则使用默认 OSS 域名 |
| `options` | 空 | 额外选项，未配置 |

> 对应本地配置文件：`picgo-config.json`（PicGo 原始配置位于 `~/Library/Application Support/picgo/data.json`）。
> 安全提示：以上为明文凭据，请勿提交至公开仓库，建议加入 `.gitignore`。
