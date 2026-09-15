# BlogLoom 本地文件上传与博客资源归档设计方案

> 开发分支：`feat_1.0.x_1`
>
> 迭代版本：`release_1.0.x`

## 1. 文档说明

本文档面向 BlogLoom `release_1.0.x`，汇总项目文件资源上传的历史设计、本次优化思路、已落地的核心流程及代码位置。

本次改造的核心目标是：在新建博客时先将编辑器资源上传到临时目录，待文章保存取得 `blogId` 后，将文章引用的本地资源归档到独立的博客目录，并自动重写文章中的资源 URL。

## 2. 本次需求描述

本次需求为“支持文件上传实现，并扩展默认对接到本地文件系统”，具体包括：

1. 支持博客编辑器本地文件上传。
2. 默认用户博客上传资源的根目录为 `${user.conf}/upload`。
3. 编辑期间先上传到临时目录，例如 `${user.conf}/upload/tmp/xxx.jpg`，并支持图片以外的其他资源扩展名与多级路径。
4. 新建或编辑文章保存时，扫描本篇文章关联的资源，将其重新复制到 `${user.conf}/upload/blogs/{blogId}/`。
5. 归档后自动将首图、文章描述和文章正文中的资源地址替换为博客正式资源地址。
6. 归档成功后删除本次已处理的临时文件，避免 `tmp` 目录无限增长。

### 2.1 术语对应

需求中的 `${user.conf}` 是外置 `conf` 目录的逻辑名称。当前实现中：

```text
${user.conf}                 = System.getProperty("user.dir.conf")
${user.conf}/upload          = System.getProperty("user.dir.upload")
```

默认仓库结构下：

```text
BlogLoom/conf/               -> user.dir.conf
BlogLoom/conf/upload/        -> user.dir.upload
```

业务代码统一读取 `user.dir.upload`，不直接拼接 `conf/upload`，以便未来调整运行目录时只修改环境解析逻辑。

## 3. 背景与原有问题

### 3.1 新建文章时没有 blogId

编辑器上传图片的时机早于文章保存。数据库自增 `blogId` 在插入文章后才能取得，因此上传时无法直接确定最终的 `blogs/{blogId}` 目录。

### 3.2 资源与文章没有稳定归属

原有后端上传更偏向“将一张远程图片持久化并返回 URL”，没有面向博客编辑器的 Multipart 上传入口，也没有按 `blogId` 管理文章资源的生命周期。

### 3.3 文章内的资源表达不唯一

文章可能同时包含 Markdown 图片、Markdown 链接和 HTML 的 `src`/`href`/`poster` 属性，URL 又可能是绝对地址、相对地址、历史 `/image/**` 地址或新 `/static/**` 地址。仅依赖前端记录上传列表，容易在复制粘贴和编辑回显时漏掉资源。

### 3.4 文件系统与数据库不共享事务

文章写入 MySQL，资源写入本地文件系统。两者不是同一个事务资源，因此必须确保只在文件复制成功后重写 URL，并只删除已确认归档成功的临时文件。

## 4. 历史设计

### 4.1 NBlog 初始上传抽象

项目初始代码已包含 `FileUploadChannel`、`ChannelFactory`、`LocalChannel`、`GithubChannel` 和 `UpyunChannel`。

历史链路为：

```text
远程图片 URL
  -> UploadUtils.getImageByRequest
  -> ImageResource
  -> FileUploadChannel
     |- local:  本地目录
     |- github: GitHub Contents API
     `- upyun:  又拍云存储
  -> 可访问图片 URL
```

该链路的主要目的是 QQ 头像等服务端图片的持久化，输入是内部 `ImageResource`，而不是编辑器提交的 `MultipartFile`。

### 4.2 品牌化与配置外置化

BlogLoom 品牌化后，上传抽象迁移到 `com.changlu.blogloom.util.upload` 包。随后将开发机绝对路径调整为仓库级外置 `conf`，避免发布新 JAR 时覆盖用户配置、日志和运行数据。

历史访问模型为：

```text
conf/upload/<uuid>.<ext>
  <-> /image/<uuid>.<ext>
```

历史的 `/image/**` 映射现在仍保留，仅用于兼容已经写入旧文章或数据库的 URL。

### 4.3 管理后台第三方图床

管理后台还包含 GitHub、又拍云和腾讯云 COS 图床管理页面。这些页面由浏览器直接请求第三方存储，与本次博客编辑器的“临时上传 + 本地归档”链路互相独立。

## 5. 总体设计思路

### 5.1 采用临时上传与保存归档两阶段模型

```text
编辑阶段
用户选择文件
  -> POST /admin/blog/resources
  -> ${user.conf}/upload/tmp/<uuid>.<ext>
  -> 返回 ${blog.api}/static/tmp/<uuid>.<ext>
  -> 编辑器插入临时 URL

保存阶段
保存/编辑博客
  -> 取得 blogId
  -> 扫描 firstPicture + description + content
  -> 识别受管本地资源
  -> 复制到 ${user.conf}/upload/blogs/{blogId}/**
  -> 重写为 ${blog.api}/static/blogs/{blogId}/**
  -> 更新文章字段
  -> 删除已归档的 tmp 源文件
```

这种模型同时兼容新建文章尚无 `blogId` 和编辑文章已有 `blogId` 的场景。

### 5.2 后端根据文章实际内容决定资源归属

前端不额外维护“已上传资源列表”。保存时后端重新扫描文章字段，只归档文章当前真正引用的本地资源。这能兼容复制粘贴、编辑回显、历史地址和同一资源多次引用。

### 5.3 不下载外部 URL

`http`/`https` URL 只在其 URL path 符合 BlogLoom 本地映射规则时被当作受管资源。其他 CDN、GitHub、又拍云、COS、`data:` 和 `blob:` 地址保持原样，不在保存文章时下载，避免 SSRF 与不可控网络依赖。

## 6. 目录与 URL 规范

### 6.1 物理目录

```text
${user.conf}/upload/
|- tmp/
|  `- <uuid>.<ext>                # 编辑阶段临时资源
`- blogs/
   `- {blogId}/
      `- **                         # 文章归档资源，可保留多级目录
```

`SystemPropertyUtil.setSystemUserDir()` 在应用启动时依次完成：

1. 定位仓库根目录或后端父目录中的 `conf`。
2. 将该目录注册为 `user.dir.conf`。
3. 创建 `conf/upload`、`conf/upload/tmp` 和 `conf/upload/blogs`。
4. 将 `conf/upload` 注册为 `user.dir.upload`。

真实上传文件由 `conf/.gitignore` 忽略，只在 Git 中保留空目录占位文件。

### 6.2 访问 URL

```text
物理文件：${user.conf}/upload/tmp/a.png
访问 URL： ${blog.api}/static/tmp/a.png

物理文件：${user.conf}/upload/blogs/100/a.png
访问 URL： ${blog.api}/static/blogs/100/a.png
```

`WebConfig` 建立如下映射：

```text
/static/** -> file:${user.dir.upload}/
/image/**  -> file:${user.dir.upload}/   # 兼容历史 URL
```

## 7. 核心功能设计与处理流程

### 7.1 编辑器临时上传

#### 请求

```http
POST /admin/blog/resources
Content-Type: multipart/form-data

file=<binary>
```

该接口位于 `/admin/**` 下，复用现有管理端 JWT 权限链路。

#### 服务端处理

`LocalResourceStorageService.uploadTemp` 执行：

1. 校验文件不为空。
2. 从原文件名取得合法扩展名，无法取得时根据 Content-Type 推断，最终兜底为 `bin`。
3. 使用 UUID 生成服务端文件名，避免原始文件名带入路径或重名覆盖。
4. 将文件写入 `${user.conf}/upload/tmp/<uuid>.<ext>`。
5. 返回文件名、相对路径与可访问 URL。

#### 返回数据示例

```json
{
  "name": "550e8400-e29b-41d4-a716-446655440000.png",
  "relativePath": "tmp/550e8400-e29b-41d4-a716-446655440000.png",
  "url": "http://localhost:8090/static/tmp/550e8400-e29b-41d4-a716-446655440000.png"
}
```

#### 前端接入

`WriteBlog.vue` 中的“文章描述”和“文章正文”两个 `mavon-editor` 都监听 `imgAdd`：

```text
imgAdd
  -> uploadBlogResource(file)
  -> POST blog/resources
  -> editor.$img2Url(pos, response.url)
  -> Markdown 中插入临时 URL
```

### 7.2 新建文章归档

```text
POST /admin/blog
  -> BlogAdminController.getResult
  -> BlogServiceImpl.saveBlog
  -> blogMapper.saveBlog(blog)                 # 先插入，回填 blogId
  -> BlogResourceService.reconcileBlogResources
  -> blogMapper.updateBlogResources(blog)      # 持久化重写后的三个资源字段
  -> 更新缓存
```

必须先插入文章，因为归档目录依赖数据库回填的 `blogId`。

### 7.3 编辑文章归档

```text
PUT /admin/blog
  -> BlogAdminController.getResult
  -> BlogServiceImpl.updateBlog
  -> BlogResourceService.reconcileBlogResources(blogId, blog)
  -> blogMapper.updateBlog(blog)
  -> 更新缓存
```

编辑场景已知 `blogId`，因此可先归档资源和重写内容，再更新数库。

### 7.4 文章资源扫描

`BlogResourceService.reconcileBlogResources` 统一处理：

```text
firstPicture -> rewriteUrl
description  -> rewriteContent
content      -> rewriteContent
```

`rewriteContent` 采用两阶段扫描：

1. 扫描 Markdown 图片和文件链接，只替换 URL 分组。
2. 在 Markdown 处理结果上扫描 HTML `src`、`href` 和 `poster` 属性。
3. 使用 `Matcher.quoteReplacement` 避免 URL 中 `$` 或 `\` 被正则替换逻辑误解释。
4. 非受管资源或归档失败的 URL 保持原样。

### 7.5 单个 URL 归档与重写

`rewriteUrl` 的核心流程：

```text
原 URL
  -> 分离 ?query 和 #fragment
  -> 转换为 uploadRoot 下的受管相对路径
  -> copyToBlog(blogId, relativePath)
  -> 得到 blogs/{blogId}/**
  -> buildAccessUrl
  -> 恢复 query/fragment
  -> 最终 URL
```

示例：

```text
原 URL：   /static/tmp/images/a.png?width=800#preview
源文件：  ${user.conf}/upload/tmp/images/a.png
归档文件：${user.conf}/upload/blogs/100/images/a.png
最终 URL： ${blog.api}/static/blogs/100/images/a.png?width=800#preview
```

### 7.6 copyToBlog 目标路径生成策略

`LocalResourceStorageService.copyToBlog(blogId, sourceRelativePath)` 的目标目录始终是：

```text
${user.conf}/upload/blogs/{blogId}/{targetRelativePath}
```

`targetRelativePath` 按下列规则生成：

| 源相对路径 | 处理规则 | 目标相对路径（blogId=100） |
| --- | --- | --- |
| `tmp/a.png` | 去掉 `tmp/` | `blogs/100/a.png` |
| `tmp/2026/09/a.png` | 去掉 `tmp/`，保留多级目录 | `blogs/100/2026/09/a.png` |
| `blogs/20/a.png` | 去掉 `blogs/20/` | `blogs/100/a.png` |
| `blogs/20/images/a.png` | 去掉旧博客前缀，保留后续层级 | `blogs/100/images/a.png` |
| `other/a.png` | 完整保留相对路径 | `blogs/100/other/a.png` |

处理步骤：

1. 将 `sourceRelativePath` 解析为 `${user.conf}/upload` 下的物理路径。
2. `normalize` 后验证源路径仍以 uploadRoot 开头，拒绝路径穿越。
3. 生成 `targetRelativePath` 和目标博客目录。
4. 验证目标路径仍在 `blogs/{blogId}` 内。
5. 创建目标父目录，并使用覆盖策略复制文件。
6. 当源文件已是目标文件时跳过复制，保证编辑保存的幂等性。
7. 返回 `blogs/{blogId}/{targetRelativePath}`，供 `buildAccessUrl` 组装最终 URL。

### 7.7 临时资源清理

`reconcileBlogResources` 使用同一个 `Set<String>` 收集首图、描述和正文中成功归档的 `tmp/**` 路径：

1. 相同资源多次出现时只清理一次。
2. `copyToBlog` 返回成功后才加入集合。
3. 三个文章字段全部处理完成后再统一删除。
4. `deleteTemporaryResource` 只允许删除 `${user.conf}/upload/tmp` 下的文件。

## 8. 受管资源路径规则

### 8.1 支持转换的 URL 形式

| 输入形式 | 转换后的 uploadRoot 相对路径 |
| --- | --- |
| `/static/tmp/a.jpg` | `tmp/a.jpg` |
| `static/tmp/a.jpg` | `tmp/a.jpg` |
| `/image/tmp/a.jpg` | `tmp/a.jpg` |
| `/upload/tmp/a.jpg` | `tmp/a.jpg` |
| `tmp/a.jpg` | `tmp/a.jpg` |
| `/static/blogs/10/a.jpg` | `blogs/10/a.jpg` |
| `/upload/blogs/10/a.jpg` | `blogs/10/a.jpg` |
| `blogs/10/a.jpg` | `blogs/10/a.jpg` |
| `http://localhost:8090/static/tmp/a.jpg` | `tmp/a.jpg` |

查询参数与锚点不参与文件定位，但会在重写后原样保留。

### 8.2 保持原样的 URL

下列资源不归档：

- 路径不匹配 `static/`、`image/`、`upload/tmp/`、`upload/blogs/`、`tmp/` 或 `blogs/` 的普通 URL。
- `data:`、`blob:` 或非 HTTP(S) 协议。
- 受管路径对应的本地源文件不存在。
- 外部图床或 CDN 资源。

> 说明：当前是根据 URL path 判定是否受管，并未校验绝对 URL 的 host 是否等于 `blog.api`。但后续文件解析仍被限制在 uploadRoot 中，不会读取根目录之外的本地文件。

## 9. 核心代码设计

### 9.1 SystemPropertyUtil

职责：

- 定位 `user.dir.conf`。
- 初始化 `user.dir.upload`。
- 启动时创建 `upload/tmp` 与 `upload/blogs`。

核心代码：

```java
public static void setSystemUserDir() {
    setConfDir();
    setLocalUploadFileDir();
}

public static void setLocalUploadFileDir() {
    setCustomDir("upload");
}
```

### 9.2 UploadProperties 与 WebConfig

`UploadProperties` 不再从 `application.properties` 读取可变的本地路径，而是从 `SystemPropertyUtil` 获取唯一上传根目录：

```java
public String getPath() {
    return SystemPropertyUtil.getLocalUploadFileDir();
}

public String getAccessPath() {
    return "/static/**";
}
```

`WebConfig` 将 `/static/**` 和历史 `/image/**` 都映射到该根目录。

### 9.3 LocalResourceStorageService

职责：

- `uploadTemp`：保存 Multipart 文件到 `tmp`。
- `copyToBlog`：生成目标路径并复制资源。
- `buildAccessUrl`：生成 `${blog.api}/static/**` URL。
- `deleteTemporaryResource`：安全删除已归档的临时文件。

核心复制逻辑：

```java
String targetRelativePath = getTargetRelativePath(sourceRelativePath);
Path blogDirectory = Paths.get(uploadProperties.getBlogPath(blogId));
Path target = blogDirectory.resolve(targetRelativePath).normalize();

if (!target.startsWith(blogDirectory)) {
    throw new BadRequestException("资源路径不合法");
}
Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
return "blogs/" + blogId + "/" + targetRelativePath;
```

### 9.4 BlogResourceService

职责：

- 扫描 `firstPicture`、`description` 和 `content`。
- 识别 Markdown 和 HTML 资源链接。
- 将 URL 转换为 uploadRoot 相对路径。
- 调用 `copyToBlog` 完成归档。
- 将归档后的 URL 写回文章对象。
- 收集并清理已成功处理的临时资源。

核心入口：

```java
public void reconcileBlogResources(Long blogId, Blog blog) {
    Set<String> temporaryResources = new HashSet<>();
    blog.setFirstPicture(rewriteUrl(blog.getFirstPicture(), blogId, temporaryResources));
    blog.setDescription(rewriteContent(blog.getDescription(), blogId, temporaryResources));
    blog.setContent(rewriteContent(blog.getContent(), blogId, temporaryResources));
    for (String resource : temporaryResources) {
        localResourceStorageService.deleteTemporaryResource(resource);
    }
}
```

### 9.5 BlogServiceImpl 与 BlogMapper

新建文章时，`saveBlog` 先插入数据取得 ID，再调用资源服务，最后通过 `updateBlogResources` 只更新资源相关字段。编辑文章则先重写对象，再执行完整更新。

### 9.6 前端 WriteBlog.vue

前端仅负责上传临时文件并将返回 URL 插入编辑器，不在浏览器中猜测 `blogId` 或拼接最终路径。

```javascript
uploadBlogResource(file).then(res => {
    const editor = this.$refs[editorRef]
    if (editor && res.data && res.data.url) {
        editor.$img2Url(pos, res.data.url)
    }
})
```

## 10. 安全性、幂等性与一致性

### 10.1 路径安全

- 服务端生成 UUID 文件名，不直接使用客户端原始文件名。
- 扩展名仅接受 1～10 位小写字母或数字。
- 源路径与目标路径均执行 `toAbsolutePath().normalize()` 和 `startsWith` 校验。
- 临时删除额外校验路径以 `${user.conf}/upload/tmp` 开头。
- 非受管 URL 不触发本地文件读取。

### 10.2 幂等性

- 目标文件已存在时使用 `REPLACE_EXISTING`，重复保存结果一致。
- 源路径与目标路径相同时不执行复制。
- `Set` 对重复临时路径去重，避免重复删除。
- 已归档的 `blogs/{blogId}/**` 再次保存时仍会生成稳定地址。

### 10.3 一致性边界

MySQL 事务无法回滚文件系统操作。当前实现保证：

- 源文件不存在时保留原 URL。
- 文件复制成功后才替换为正式 URL。
- 文件复制成功后才记录临时文件待删除。

当前未实现严格的文件系统补偿事务。如数据库后续写入失败，已复制文件可能成为孤儿资源，后续应通过定时清理或资源索引机制兜底。

## 11. 兼容性与功能边界

### 11.1 兼容性

- 保留 `/image/**` 映射，兼容历史图片地址。
- 兼容已归档的 `blogs/{旧blogId}/**`，可复制到当前博客目录。
- 兼容 URL 查询参数与锚点。
- 外部图床地址原样保留。

### 11.2 当前已支持

- 编辑器 Multipart 本地上传。
- 文章描述和正文的图片上传。
- `firstPicture`、`description`、`content` 的归档与 URL 重写。
- Markdown 图片与链接。
- HTML `src`、`href`、`poster` 属性。
- 临时资源成功归档后的即时清理。

### 11.3 当前未支持或待完善

- 上传文件大小限制和业务级扩展名/Content-Type 白名单。
- `srcset` 中多个 URL 的拆分和重写。
- 文章删除时联动删除 `blogs/{blogId}` 目录。
- 超时未使用 `tmp` 文件的定时清理。
- 未被任何文章引用的孤儿资源清理。
- 数据库失败时的文件操作补偿。
- 将 GitHub、又拍云或 COS 统一接入这条博客资源归档链路。

## 12. 核心源码索引

| 职责 | 文件 |
| --- | --- |
| 初始化外置配置与上传目录 | `blog-backend/src/main/java/com/changlu/blogloom/env/SystemPropertyUtil.java` |
| 上传目录派生与访问路径 | `blog-backend/src/main/java/com/changlu/blogloom/config/properties/UploadProperties.java` |
| `/static/**` 和历史 `/image/**` 映射 | `blog-backend/src/main/java/com/changlu/blogloom/config/WebConfig.java` |
| 编辑器资源上传接口 | `blog-backend/src/main/java/com/changlu/blogloom/controller/admin/BlogResourceAdminController.java` |
| 临时上传、归档复制、URL 生成与临时清理 | `blog-backend/src/main/java/com/changlu/blogloom/service/LocalResourceStorageService.java` |
| 文章资源扫描与链接重写 | `blog-backend/src/main/java/com/changlu/blogloom/service/BlogResourceService.java` |
| 新建/编辑文章时调用资源归档 | `blog-backend/src/main/java/com/changlu/blogloom/service/impl/BlogServiceImpl.java` |
| 文章资源字段更新 SQL | `blog-backend/src/main/java/com/changlu/blogloom/mapper/BlogMapper.java` 与 `blog-backend/src/main/resources/mapper/BlogMapper.xml` |
| 管理端上传 API | `blog-cms-ui/src/api/blog.js` |
| mavon-editor 上传与 URL 回填 | `blog-cms-ui/src/views/blog/blog/WriteBlog.vue` |
| 运行目录 Git 忽略规则 | `conf/.gitignore` |

## 13. 验收要点

1. 从仓库根目录或 `blog-backend` 目录启动时，均能正确定位 `${user.conf}/upload`。
2. 编辑器上传资源后，文件出现在 `upload/tmp`，临时 URL 可访问。
3. 新建文章取得 `blogId` 后，所引用资源出现在 `upload/blogs/{blogId}`。
4. 编辑文章时，新上传和已归档资源都能正常处理。
5. 首图、描述和正文的最终 URL 均指向 `/static/blogs/{blogId}/**`。
6. URL 中的 query 与 fragment 原样保留。
7. 外部资源不下载、不改写。
8. `../`、绝对文件路径等越界输入无法读写 uploadRoot 之外的文件。
9. 归档失败时不删除临时源文件，不写入无效最终 URL。
10. 同一临时资源多次引用时仅执行一次有效清理。

## 14. 总结

本次设计将 BlogLoom 的博客资源处理从“独立图片上传”完善为“临时上传、按文章归档、内容 URL 重写、成功后清理”的完整生命周期：

```text
上传：${user.conf}/upload/tmp/**
  -> 保存/编辑文章
  -> 归档：${user.conf}/upload/blogs/{blogId}/**
  -> 访问：${blog.api}/static/blogs/{blogId}/**
  -> 清理已成功归档的 tmp 资源
```

核心价值在于：解决新建文章没有 `blogId` 的上传时序问题，将资源稳定归属到具体文章，同时保留历史 URL 和外部图床兼容能力。
