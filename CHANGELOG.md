# Changelog

所有对本项目的重要更改都将记录在此文件中。

本日志格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
并且项目遵循[语义化版本](https://semver.org/lang/zh-CN/)。

## v1.0.1 - 2026.09.22-2026.10.13

### Added

- [cb608b8] 新增搜索引擎 SEO 优化一期：公开页面（文章详情 / 首页 / 标签 / 分类 / 专栏 / 归档）由后端 Thymeleaf 服务端直出并绕开 SPA 回退，新增 `robots.txt`、`sitemap.xml`、`rss.xml` 站点级文件与页面级 title / description / canonical / OpenGraph / JSON-LD 结构化数据；后台新增「SEO优化」顶级菜单与「SEO配置」页，支持 SEO 域名配置（优先后台配置、兜底 blog.view）及核心文件快捷查看与下载。
- [3040ca7] 新增「SEO 平台关联」（站点验证）：后台配置百度 / Bing / Google 验证 content，支持整段验证代码自动提取 content、空值不输出；采用方案 B 由 `SpaIndexController` 把验证 meta 注入 SPA index.html，SSR `seo-head` 同步输出，覆盖 `/` 与 `/home` 等页面。
- [83dbfab] 新增搜索引擎广告变现（Google AdSense）接入：新增 AdSense 配置项与 `AdSlot` / `AdsenseConfig` 领域模型，支持发布商 ID、自动 / 手动广告位（顶部 / 文内 / 底部 / 右侧目录下方）与 `ads.txt` 自定义内容，可粘贴广告单元代码自动识别并校验 `client` / `slot`、空值不注入；SSR 与 SPA 双端按位置注入广告位，新增 `/ads.txt` 端点随后台配置自动生成；后台新增「广告平台关联」配置页。
- [fb7dfd5] 新增首页文章列表 RSS 订阅入口，并完成移动端适配。
- [625a6b5] 新增 BlogLoom 分发 Skill（Playwright + CDP 多渠道发布）与「BlogLoom 分发 Skill」专栏（中英文）。
- [2b5d856] 新增公众号（gzh）分发渠道与渠道级 `conf` 配置，重构 publisher 为分层模块，补充 CSDN / 公众号能力与发布流程文档。
- [c0f7d87] 新增 Windows PowerShell 一键安装与升级支持。
- [f1019d9] 部署脚本支持多架构镜像，`push.sh` 改用 buildx 构建推送 amd64 / arm64 manifest。
- [fef02a0] 本地图床渠道新增访问地址配置，后台可视化展示与修改，未配置时回退 `blog.api`。
- [b18a385] 用户版首次部署自动生成随机 MySQL 密码与令牌密钥，MySQL 映射 13306 供外网连接，并同步运维说明。
- [659198c] 新增「联系我」区块与开源社区文件（CONTRIBUTING、SECURITY、Issue 与 PR 模板）。
- [4beb50c] 新增后端 Maven 与前后端构建 CI，跟踪 `package-lock.json` 并补充 README 构建 / 发布徽章。
- [59cdc5d] 新增专栏备份导出。
- [41cb9f3] 优化 CMS Skill 下载凭证：为下载包签发无过期时间的专属 Token，并基于 `X-Forwarded` 请求头及当前请求动态注入部署服务 BaseURL。
- [5569347] 新增 v1.0.0 发布说明留档（与 GitHub Release 内容一致）。

### Changed

- [60db32e] 优化文章目录、详情页阅读排版与首页导航布局。
- [edff8dd] 优化博客阅读详情页顶部导航栏：新增详情页专属紧凑样式，缩减导航高度并同步调整栏目字号、内边距、图标间距、站点标题与搜索框尺寸，补充移动端适配。
- [83dbfab] 微调文章详情页右侧目录宽度与横向位置（Vue 站内页与 SEO 直出页保持一致）。
- [380084d] 优化首页封面加载与博客列表排版，调整筛选搜索栏尺寸。
- [c35a85d] 优化首页首屏与资料卡视觉，新增可配置首屏及 CSDN 信息。
- [0e57fd3] 优化博客首页左侧自定义模块与博客详情阅读页右侧栏目。
- [358682b] 新增专栏快捷树形移动与拖拽边缘自动滚动。
- [cb608b8] 阅读页性能优化：补齐详情接口封面 / 摘要 / 作者字段，正文 h1 降级与目录，图片懒加载、脚本 defer 与 HTTP 压缩。
- [3040ca7] docker 构建脚本新增 `NPM_REGISTRY` / `MAVEN_MIRROR` 可配置依赖源（默认国内源），修复官方源网络中断导致的构建失败。
- [83dbfab] docker 构建补齐 Maven 依赖源回退与失败排查指引。
- [2b5d856] 公众号分发 Skill 保存草稿改为轮询 URL 回读校验，避免「假成功」。
- [7819eec] README 新增社交预览图并加入产品展示，移除顶部 NBlog 提及。
- [4e329a6] README 补全目录结构，忽略 `codes` 目录。
- [9eaf568] README 弱化 MySQL 缓存实现细节，移除 `cache_entry` 相关描述。
- [659198c] 调整 README 章节顺序。

### Fixed

- [59cdc5d] 修复专栏单次移动与层级排序，优化后台登录及记住密码。
- [cb473cd] 修复专栏背景缺失并补齐 SEO 分享封面映射。
- [b5c6c6d] 修复首页专栏展开拉高列表，补充开发环境与部署运维说明。
- [41cb9f3] 修复 `skill.env.sh` 地址占位符未生效问题，补充 ZIP 下载、Token 验签及反向代理地址解析测试。

## v1.0.0 - 2026.09.13-2026.09.22

### Added

- [f785762] 完成 BlogLoom 首次开源提交：提供博客前台、内容管理后台与 Spring Boot 后端服务，覆盖 Markdown 创作与展示、分类标签、归档、评论与回复、动态、友链及关于页，以及站点配置、内容管理、图床配置、访客统计、日志中心和定时任务；支持 Spring Security + JWT 认证、MySQL 持久化、Redis 缓存、Quartz 调度、评论通知与本地及第三方存储上传，并提供初始化 SQL、部署脚本、配置示例与基础文档。
- [8ad1c23] 新增外置 `conf` 目录配置加载机制，支持从项目根目录或后端模块启动时自动定位配置、日志、静态资源和上传目录。
- [01、BlogLoom 本地文件上传与博客资源归档设计方案.md](<docs/devDocs/release_1.0.x/01、BlogLoom 本地文件上传与博客资源归档设计方案.md>)：支持博客编辑器本地文件上传，新上传资源先写入 `${user.conf}/upload/tmp/`，文章保存或编辑时按 `blogId` 归档至 `${user.conf}/upload/blogs/{blogId}/`；同步支持扫描首图、Markdown 和 HTML 中的受管资源，自动重写访问 URL 并清理已归档的临时文件。
- [ff2c3e8] 新增本地 Markdown 知识库管理：支持 ZIP 校验导入、内存级进度查询、目录与文档树展示、搜索、拖拽移动、重命名、递归及批量删除、单文档下载与知识库 ZIP 导出。
- [e92f373] 新增博客回收站（列表 / 恢复 / 彻底删除），并支持专栏导入归档与知识库文章元数据解析。
- [faade4a] 重构图床为「本地」与「阿里云 OSS」两种上传渠道，并支持真实上传 / 删除的连通性测试。
- [22b6d7b] 新增 Docker 一键部署与增量升级方案，并将 Redis 缓存迁移至 MySQL（`cache_entry` 表）。
- [64e64c2] 新增 BlogLoom Skill：后台「文章管理」可下载专属 AI 技能包，用自然语言完成本地 Markdown 文章的导入、更新与查询，行为与后台导入能力一致。
- [5aaaad4] 新增官网文档站（docs-website）及文档页面与导航校验 CI。

### Changed

- [4babb0a] 完成 NBlog 到 BlogLoom 的项目品牌化重构（统一后端 Java 包名、应用入口、数据库脚本、前后台标识及项目配置），并重构博客前台与管理后台整体布局，更新首页 Banner、侧边栏、导航、仪表盘、登录页和品牌素材。
- [8ad1c23] 优化前台导航、文章卡片、侧边栏、页脚及响应式布局，完善搜索和“阅读全文”交互；将运行配置、Logback、上传和静态资源目录收口至 `conf` 目录，整理全量 SQL 及默认站点数据，并重写项目 README。
- [0b78cee] 优化文章详情页的标题、元信息、正文、版权和评论区样式，新增加载状态、入场动画与移动端适配；统一路由滚动策略（进入新页面回到顶部、前进后退恢复历史位置、支持带偏移的锚点定位）。
- [e66f59f] 更新 BlogLoom 项目 Logo。
- [ff2c3e8] 重构文章编辑发布流程与全屏编辑体验；优化博客详情作者资料卡并增加累计 PV、公开博客数与公开文章总阅读量统计；同步规范初始化及增量 SQL 目录，新增本地数据库快速升级脚本与部署配置。
- [8a6ce4c] 优化整体 UI 布局与首页博客搜索右侧栏目数据统计。
- [3482e40] 支持移动端样式对接。
- [9d0dd4d] 仪表盘分类 / 标签统计改为 SQL 聚合，增强知识库导入会话与首页展示。
- [845d1be] 优化首页布局与文章卡片展示。
- [27fe60e] 移除历史 GitHub / 又拍云 / 腾讯云图床代码与依赖。
- [9c70731] 同步文档至最新实现：移除 Redis 改用 MySQL 缓存、图床收敛为本地 / 阿里云 OSS、新增 Docker 部署与回收站 / 专栏 / 知识库等功能说明。
- [b2cc8e5] README 新增产品展示章节与前后台界面截图，快速开始拆分为用户部署 / 本地开发。
- [e58fbdf] 优化 README 拆分用户 / 开发视角，修正用户版部署脚本分支地址。
- [39fa6bd] 用户版部署脚本地址恢复为 master 分支（后续合并到 master 后生效）。
- [67a9ec1] 优化首页博客列表标签展示（超长单行截断并以「...」收尾，鼠标悬浮即时展示全部标签）并优化 BlogLoom Skill。
- [e731ce6] 去除无用脚本。
- 收紧文章封面图识别规则：仅识别正文中 alt 为 `coverImg` 的图片（`![coverImg](url)` 或 `<img alt="coverImg" src="url">`），普通图片不再作为封面，无标记则无封面；同步更新 CMS 产品文档、根 README 与官网文档，并新增官网「AI Skill 同步」页面（中英文）。

### Fixed

- [0b78cee] 补充 MySQL `allowPublicKeyRetrieval` 连接参数，改善默认配置下的数据库连接兼容性。
