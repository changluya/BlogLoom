# BlogLoom 上传文件目录与访问路径规范

## 1. 统一根目录

本地上传文件统一以 `${user.dir.upload}` 为根目录，其实际路径为：

```text
${user.dir.conf}/upload
```

后端统一静态访问映射：

```text
/static/** -> file:${user.dir.upload}/**
```

业务代码通过 `UploadProperties` 获取目录，不直接拼接服务器绝对路径。

## 2. 功能模块与上传目录

| 功能模块 | 物理存储目录 | 访问路径 | 说明 |
| --- | --- | --- | --- |
| 博客文章图片/附件（临时） | `${user.dir.upload}/tmp/{uuid}.{ext}` | `{blog.api}/static/tmp/{uuid}.{ext}` | 编辑文章时先上传到临时目录，不能作为长期资源地址 |
| 博客文章封面（归档） | `${user.dir.upload}/blogs/{blogId}/cover/{uuid}.{ext}` | `{blog.api}/static/blogs/{blogId}/cover/{uuid}.{ext}` | 编辑页复用通用图片上传组件，保存文章时将首图单独归档到 `cover` 子目录 |
| 博客文章正文图片/附件（归档） | `${user.dir.upload}/blogs/{blogId}/**` | `{blog.api}/static/blogs/{blogId}/**` | 保存文章时将正文、描述引用的临时资源归档到对应博客目录 |
| 博客专栏图片 | `${user.dir.upload}/blogColumn/{columnId}/{uuid}.{ext}` | `{blog.api}/static/blogColumn/{columnId}/{uuid}.{ext}` | 每个专栏使用独立目录存储 Logo/配图 |
| 站点设置图片 | `${user.dir.upload}/site/{uuid}.{ext}` | `{blog.api}/static/site/{uuid}.{ext}` | 用于头像、页脚图片、赞赏码等站点配置图片 |

### 2.1 文章封面路径流转

文章编辑页上传封面时复用文章资源上传接口，先进入临时目录；文章首次保存取得 `blogId` 或更新文章时，再归档到该文章独立的 `cover` 子目录：

```text
临时物理路径：${user.dir.upload}/tmp/{uuid}.{ext}
临时访问路径：{blog.api}/static/tmp/{uuid}.{ext}
                         ↓ 保存/更新文章
正式物理路径：${user.dir.upload}/blogs/{blogId}/cover/{uuid}.{ext}
正式访问路径：{blog.api}/static/blogs/{blogId}/cover/{uuid}.{ext}
```

数据库 `first_picture` 字段保存正式访问 URL。封面只与正文图片进行目录隔离，不裁剪或转换上传的原始图片。

目录结构示例：

```text
conf/upload/
├── tmp/                         # 文章编辑阶段的临时资源
├── blogs/
│   └── {blogId}/                # 文章正式资源
│       └── cover/                # 文章封面图片
├── blogColumn/
│   └── {columnId}/              # 专栏图片
└── site/                        # 站点设置图片
```

## 3. 模块实现对应关系

| 功能模块 | 上传接口 | 主要实现 |
| --- | --- | --- |
| 文章临时资源上传 | `POST /admin/blog/resources` | `BlogResourceAdminController`、`LocalResourceStorageService` |
| 文章资源归档（含封面） | 随文章保存执行 | `BlogResourceService`、`LocalResourceStorageService` |
| 专栏图片上传 | `POST /admin/column/{id}/cover` | `ColumnAdminController`、`ColumnCoverStorageService` |
| 站点设置图片上传 | `POST /admin/siteSettings/{id}/image` | `SiteSettingAdminController`、`SiteImageStorageService` |

## 4. 历史路径兼容

历史站点图片地址继续保留，不要求迁移数据库：

```text
/img/qr.png
/img/reward.jpg?v=20260913
/img/avatar.jpg?v=20260913
```

对应文件同时放在以下目录，确保前台和 CMS 均可预览：

```text
blog-view-ui/public/img/
blog-cms-ui/public/img/
```

历史 Markdown 中的 `/image/**` 也继续兼容；所有新上传资源统一生成 `/static/**` 地址。

## 5. 新增模块约定

新增上传功能时，按以下形式分配独立业务目录：

```text
${user.dir.upload}/{domain}/{aggregateId}/{uuid}.{ext}
```

同时遵循：

- 临时资源进入 `tmp`；
- 正式资源进入对应业务目录；
- 文件名由服务端生成 UUID；
- 数据库保存访问 URL，不保存服务器绝对路径；
- 目录统一在 `UploadProperties` 中维护。
