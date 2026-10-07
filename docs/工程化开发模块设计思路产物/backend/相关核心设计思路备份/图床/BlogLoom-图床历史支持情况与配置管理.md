# BlogLoom 图床历史支持情况与配置管理

> 本文梳理 BlogLoom 图床上传的历史支持情况、当前默认上传方式，以及配置参数的管理方式。

## 一、图床支持情况与默认上传方式

### 1.1 历史支持情况

BlogLoom 基于 [Naccl/NBlog](https://github.com/Naccl/NBlog) 二次开发，原项目图床能力包含两条独立链路：

| 链路 | 来源 | 支持的图床 | 传输方向 |
| --- | --- | --- | --- |
| 管理后台「图床管理」 | 移植自 `Naccl/PictureHosting` | GitHub、又拍云、腾讯云 COS | 浏览器直接请求第三方 API |
| 评论 QQ 头像上传 | 原 NBlog 后端 `UploadUtils` + `ChannelFactory` | 本地、GitHub、又拍云 | 后端下载 QQ 头像后转存 |

其中管理后台的图床管理页面（配置页、GitHub、又拍云、腾讯云）凭据保存在浏览器 `localStorage`，上传由前端直接发起，后端不参与；评论 QQ 头像则通过后端 `upload.channel` 参数选择本地或第三方通道。

### 1.2 现状：第三方渠道已移除，仅保留本地上传

在 1.0.x 迭代中，BlogLoom 确立了「本地文件上传 + 博客资源归档」方案，并已**移除 GitHub、又拍云、腾讯云三种第三方渠道**，包括后端通道实现、配置参数，以及管理后台「图床管理」下的三个页面。当前所有上传统一走本地：

| 场景 | 上传接口 | 实现 | 存储位置 |
| --- | --- | --- | --- |
| 博客编辑器正文/描述/封面资源 | `POST /admin/blog/resources` | `LocalResourceStorageService` | `${user.dir.upload}/tmp` → 保存时归档 `blogs/{blogId}` |
| 站点设置图片（头像、赞赏码等） | `POST /admin/siteSettings/{id}/image` | `SiteImageStorageService` | `${user.dir.upload}/site` |
| 专栏封面 | `POST /admin/column/{id}/cover` | `ColumnCoverStorageService` | `${user.dir.upload}/blogColumn/{columnId}` |
| 评论 QQ 头像 | 评论保存时触发 | `QQInfoUtils` → `UploadUtils` → `LocalChannel` | `${user.dir.upload}` |

### 1.3 目前是否为默认上传本地

是。移除第三方渠道后，**本地是唯一的上传方式**：

- 博客编辑器、站点设置、专栏资源：代码直接写入 `${user.dir.upload}`（即 `conf/upload/`），不存在其他通道。
- 评论 QQ 头像：由 `LocalChannel` 直接保存到上传根目录，不再读取任何图床切换参数。
- 管理后台不再提供「图床管理」菜单，第三方直传入口与浏览器凭据存储一并移除。

## 二、配置参数如何管理

### 2.1 后端配置

第三方图床相关的配置文件参数已全部移除，当前图床相关配置仅剩本地上传目录：

| 配置 | 绑定/实现 | 说明 |
| --- | --- | --- |
| 本地上传根目录 | `SystemPropertyUtil` / `UploadProperties` | 注册系统属性 `user.dir.upload`，实际为 `${user.dir.conf}/upload` |
| 静态访问映射 | `UploadProperties.getAccessPath()` / `getResourcesLocations()` | `/static/** -> file:${user.dir.upload}/` |

已移除的配置项：

- `upload.channel`：原用于在 `local`、`github`、`upyun` 之间切换评论 QQ 头像的图床，现已删除，`UploadUtils` 直接注入 `LocalChannel`。
- `upload.github.*`（`GithubProperties`）：GitHub 图床参数，随 `GithubChannel` 一并删除。
- `upload.upyun.*`（`UpyunProperties`）：又拍云图床参数，随 `UpyunChannel` 一并删除。

本地目录的管理方式：

- `SystemPropertyUtil.setLocalUploadFileDir()` 启动时注册 `user.dir.upload` 指向 `${user.dir.conf}/upload`。
- `UploadProperties` 基于该属性派生 `tmp`、`blogs/{blogId}`、`blogColumn/{columnId}`、`site` 等业务子目录，业务代码不直接拼接服务器绝对路径。
- 目录结构、归档流转与访问路径详见《BlogLoom 上传文件目录与访问路径规范》。

### 2.2 前端配置

管理后台「图床管理」页面及配套 API（`api/github.js`、`api/upyun.js`）与 `cos-js-sdk-v5` 依赖均已删除，因此**不再有任何前端图床配置**，浏览器 `localStorage` 中也不再有 `githubToken`、`upyunConfig`、`txyunConfig` 等凭据。

所有图片上传统一调用后端本地存储接口（如 `POST /admin/blog/resources`），由后端写入 `conf/upload/` 并返回 `/static/**` 访问地址。
