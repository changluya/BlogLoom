# 前端后端接口 BaseURL 同源部署与 Docker 打包复刻 SOP

> 文档类型：工程化实现与部署 SOP
> 适用模块：博客前台、管理后台、后端 API、Docker 一体化镜像
> 适用版本：`release_1.0.x`
> 更新日期：2026-10-27

---

## 一、文档目标

本文沉淀一套可复用的前后端接口地址设计方案，解决以下问题：

- 前端代码中的 `baseURL` 如何配置；
- Docker 打包时如何把不同环境的接口地址注入前端；
- 页面访问地址、API 地址、管理后台地址之间如何规划；
- 为什么浏览器访问 `https://blog.changlu.cloud/home` 时，接口会自动请求到 `https://blog.changlu.cloud/columns/tree`；
- 如何将这套方案复刻到其他前后端项目中；
- 如何排查生产环境出现 `localhost`、跨域、404、静态资源路径错误等问题。

本文的核心原则是：

> 生产环境优先采用前后端同源部署，前端使用相对路径访问后端 API；Docker 构建阶段注入 `VITE_API_URL`，运行阶段只负责启动服务，不再修改已经编译完成的前端接口地址。

---

## 二、核心结论

### 2.1 最终接口地址的组成公式

前端发起请求时，最终 URL 通常由三部分组成：

```text
最终请求地址 = 当前站点 Origin + 前端 baseURL + API 相对路径
```

以 BlogLoom 为例：

```text
当前站点 Origin：  https://blog.changlu.cloud
前台 baseURL：     /
API 相对路径：     columns/tree
最终请求地址：     https://blog.changlu.cloud/columns/tree
```

因此，页面路径是 `/home` 并不意味着接口会请求 `/home/columns/tree`。前台 `baseURL` 是根路径 `/`，所以请求从站点根目录开始解析。

### 2.2 BlogLoom 当前 Docker 生产配置

| 模块 | 页面地址 | 编译时 `VITE_API_URL` | 后端 API 地址 |
| --- | --- | --- | --- |
| 博客前台 | `/`、`/home`、`/blog/1` | `/` | `/columns/tree`、`/blogs`、`/site` |
| 管理后台 | `/cms/` | `/admin/` | `/admin/login`、`/admin/blogs` |

对应的生产访问示例：

```text
博客前台页面： https://blog.changlu.cloud/home
博客前台接口： https://blog.changlu.cloud/columns/tree

管理后台页面： https://blog.changlu.cloud/cms/
管理后台接口： https://blog.changlu.cloud/admin/login
```

### 2.3 `BLOG_API` 不等于前端 Axios 的 `baseURL`

项目中存在两类容易混淆的配置：

| 配置 | 生效阶段 | 主要用途 |
| --- | --- | --- |
| `VITE_API_URL` | 前端构建阶段 | 写入前端 JavaScript，决定 Axios 请求 API 的前缀 |
| `BLOG_API` | 后端运行阶段 | 生成上传资源、SEO、邮件、通知等场景需要的绝对 URL |

例如：

```text
VITE_API_URL=/
BLOG_API=https://blog.changlu.cloud
```

前者决定浏览器如何请求 API，后者供后端生成对外链接。修改 Docker Compose 中的 `BLOG_API`，不会改变已经打包完成的前端 Axios `baseURL`。

---

## 三、BlogLoom 当前实现链路

### 3.1 前台请求客户端

文件：`blog-view-ui/src/plugins/axios.js`

```js
const request = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8090/',
  timeout: 10000,
})
```

前台 API 文件只声明相对路径，例如：

文件：`blog-view-ui/src/api/column.js`

```js
export function getColumnTree() {
  return axios({
    url: 'columns/tree',
    method: 'GET'
  })
}
```

前台请求路径的设计原则是：

- API 模块只维护业务路径，例如 `columns/tree`、`blogs`、`site`；
- 域名、端口、协议、统一 API 前缀由 `baseURL` 统一管理；
- 业务 API 文件不重复拼接域名；
- 生产环境尽量使用相对路径，避免把具体域名写进前端业务代码。

### 3.2 管理后台请求客户端

文件：`blog-cms-ui/src/util/request.js`

```js
const request = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8090/admin/',
  timeout: 5000
})
```

管理后台 API 文件同样使用相对路径：

```js
// blog-cms-ui/src/api/login.js
url: 'login'
```

Docker 打包时将其配置为 `/admin/`，所以最终请求为：

```text
/admin/ + login
= https://blog.changlu.cloud/admin/login
```

### 3.3 Docker 构建阶段注入配置

文件：`docker/Dockerfile`

博客前台：

```dockerfile
RUN VITE_API_URL=/ VITE_ASSETS_DIR=assets npm run build
```

管理后台：

```dockerfile
RUN VITE_API_URL=/admin/ npm run build -- --base=/cms/
```

这里的 `VITE_API_URL` 是 Shell 环境变量，会被 Vite 在构建阶段读取并编译到静态 JavaScript 中。

也就是说，打包后浏览器执行的不是动态读取 Docker 容器环境变量，而是已经包含类似以下逻辑的构建产物：

```text
前台产物：baseURL = "/"
后台产物：baseURL = "/admin/"
```

### 3.4 前端静态资源与后端 API 共用一个容器

Dockerfile 将前端构建产物复制到后端容器：

```dockerfile
COPY --from=view-build /workspace/blog-view-ui/dist/ ${APP_HOME}/conf/static/view/
COPY --from=cms-build /workspace/blog-cms-ui/dist/ ${APP_HOME}/conf/static/cms/
```

Spring Boot 启动时配置静态资源目录：

```java
spring.resources.static-locations=
  file:${user.dir.conf}/static/view/,
  file:${user.dir.conf}/static/,
  classpath:/META-INF/resources/,
  classpath:/resources/,
  classpath:/static/,
  classpath:/public/
```

对应的对外路径为：

```text
/       → conf/static/view/，博客前台
/cms    → conf/static/cms/，管理后台
/admin  → Spring Boot 管理端 API
```

因为页面与 API 使用同一个域名和端口，浏览器不需要跨域请求，也不需要在前端写死后端容器地址。

---

## 四、前台与 CMS 页面路由和接口路由的映射

BlogLoom 同时包含博客前台和 CMS 管理后台，两者都运行在同一个域名下，但页面路由前缀和 API 路由前缀不同：

| 场景 | 页面路由前缀 | API 路由前缀 | Docker 构建配置 |
| --- | --- | --- | --- |
| 博客前台 | `/` | `/`（当前项目） | `VITE_API_URL=/` |
| CMS 后台 | `/cms/` | `/admin/` | `VITE_API_URL=/admin/`、`--base=/cms/` |

必须明确区分：

- 页面路由由 Vue Router 处理；
- API 路由由 Axios `baseURL` 与后端 Controller 处理；
- 页面路径和 API 路径没有直接的父子关系；
- `/cms` 是后台前端静态应用的部署前缀，`/admin` 是后端管理接口的 URL 前缀。

### 4.1 前台场景：`/home` 页面请求 `/columns/tree`

完整调用链如下：

```text
用户打开 https://blog.changlu.cloud/home
        ↓
Vue 页面加载侧边栏 / 专栏组件
        ↓
调用 getColumnTree()
        ↓
API 文件声明 url: "columns/tree"
        ↓
Axios baseURL = "/"
        ↓
浏览器请求 https://blog.changlu.cloud/columns/tree
        ↓
同一个 Spring Boot 服务处理该 API
```

需要注意：

- `/home` 是 Vue Router 的前台页面路由；
- `/columns/tree` 是后端 REST API 路由；
- 两者没有父子关系；
- 请求地址由 Axios 的 `baseURL` 和 API 文件里的相对路径共同决定。

如果将前台配置成：

```text
VITE_API_URL=/api/
```

那么相同的业务代码会请求：

```text
https://blog.changlu.cloud/api/columns/tree
```

此时后端或 Nginx 必须同步提供 `/api/columns/tree` 路由，否则会返回 404。

### 4.2 CMS 场景：`/cms/dashboard/overview` 页面请求 `/admin/...`

CMS 页面地址：

```text
https://blog.changlu.cloud/cms/dashboard/overview
```

它由 `blog-cms-ui/src/router/index.js` 中的 Vue Router 路由定义：

```js
{
  path: '/dashboard',
  component: Layout,
  redirect: '/dashboard/overview',
  children: [
    {
      path: 'overview',
      component: () => import('@/views/dashboard')
    }
  ]
}
```

CMS 构建时使用：

```dockerfile
RUN VITE_API_URL=/admin/ npm run build -- --base=/cms/
```

其中：

```text
--base=/cms/       → 静态资源和 Vue Router 的外部部署前缀
/dashboard/overview → Vue Router 在应用内部的页面路径
```

因此最终页面地址为：

```text
/cms/ + /dashboard/overview
= https://blog.changlu.cloud/cms/dashboard/overview
```

#### CMS 页面加载链路

```text
浏览器访问 /cms/dashboard/overview
        ↓
Spring Boot 发现这是 CMS 的 history 页面路由
        ↓
回退到 /cms/index.html
        ↓
加载 blog-cms-ui 静态资源
        ↓
Vue Router 匹配 /dashboard/overview
        ↓
渲染 views/dashboard 页面
```

后端的 SPA 回退逻辑位于 `SpaForwardConfig`：

```java
String target = (path.equals("/cms") || path.startsWith("/cms/"))
        ? "/cms/index.html"
        : "/index.html";
```

但 `/admin/**` 被列为保留路径，不会被错误回退为前端 HTML，而是继续交给后端 Controller 处理。

#### CMS API 请求链路

CMS 请求客户端的 `baseURL` 为：

```js
baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8090/admin/'
```

站点设置页面的业务 API 只声明相对路径：

```js
request({
  url: 'siteSettings',
  method: 'GET'
})
```

经过 Docker 构建配置后：

```text
VITE_API_URL = /admin/
业务路径      = siteSettings
最终请求      = /admin/siteSettings
```

完整调用链为：

```text
CMS 站点设置页面 /cms/page/site
        ↓
调用 getSiteSettingData()
        ↓
API 文件声明 url: "siteSettings"
        ↓
Axios baseURL = "/admin/"
        ↓
浏览器请求 https://blog.changlu.cloud/admin/siteSettings
        ↓
Spring Boot 的 SiteSettingAdminController 处理
```

后端 Controller 的路由由两部分组成：

```java
@RequestMapping("/admin")
public class SiteSettingAdminController {

    @GetMapping("/siteSettings")
    public Result siteSettings() {
        ...
    }
}
```

最终得到：

```text
/admin + /siteSettings
= /admin/siteSettings
```

#### CMS 页面与 API 的典型对应关系

| CMS 页面 | 页面用途 | 典型 API |
| --- | --- | --- |
| `/cms/dashboard/overview` | 数据概览 | `/admin/dashboard`、`/admin/dashboard/ranking` |
| `/cms/page/site` | 站点设置 | `/admin/siteSettings` |
| `/cms/blog/content/articles` | 文章管理 | `/admin/blogs`、`/admin/blog` |
| `/cms/imageHost/setting` | 图床配置 | `/admin/siteSettings`、`/admin/imageHost/test` |

因此，访问 `/cms/dashboard/overview` 和请求 `/admin/siteSettings` 并不是同一个路由的前后端映射；前者是 CMS 页面路由，后者是某个 CMS 功能页面调用的后端 API。它们只是由同一个 CMS 应用和同一个域名承载。

#### CMS 管理接口的登录态

CMS 请求拦截器会从浏览器 `localStorage` 读取 token，并写入请求头：

```js
const token = window.localStorage.getItem('token')
if (token) {
  config.headers.Authorization = token
}
```

因此 CMS API 除了路径前缀正确，还需要保证登录后的 `Authorization` 请求头存在。后端 `/admin/**` 接口由 Spring Security 和 JWT 过滤器进行鉴权。

---

## 五、推荐的通用目录与配置设计

其他项目复刻时，建议将“请求客户端”和“环境配置”集中管理。

### 5.1 推荐目录

```text
frontend/
├── src/
│   ├── api/
│   │   ├── article.js          # 只写业务相对路径
│   │   └── user.js
│   ├── plugins/
│   │   └── request.js          # 统一创建 Axios 实例
│   └── router/
├── .env.development            # 本地开发接口地址
├── .env.test                   # 测试环境接口地址
├── .env.production             # 非 Docker 构建时的生产默认值
└── package.json
```

### 5.2 通用请求客户端模板

```js
import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api/',
  timeout: 10000
})

export default request
```

业务 API：

```js
import request from '@/plugins/request'

export function getArticleList(params) {
  return request({
    url: 'articles',
    method: 'GET',
    params
  })
}
```

建议统一约定：

- `baseURL` 负责域名、端口和 API 前缀；
- 业务 URL 不以 `/` 开头，避免不同 Axios 版本对路径覆盖规则产生歧义；
- `baseURL` 保留末尾 `/`，例如 `/api/`、`https://api.example.com/`；
- 同源模式下使用 `/` 或 `/api/`；
- 跨域模式下使用完整 HTTPS 地址，并同步配置 CORS。

### 5.3 两种推荐部署模式

#### 模式 A：同源部署，推荐

```text
页面：https://example.com/
接口：https://example.com/api/
```

前端构建参数：

```text
VITE_API_URL=/api/
```

优点：

- 不需要前端感知后端真实容器地址；
- 不容易出现 HTTP/HTTPS 混用导致的 Mixed Content；
- 通过 Nginx 或后端统一转发即可；
- 发布镜像后更换域名通常不需要重新编译前端。

#### 模式 B：前后端分域，按需使用

```text
页面：https://www.example.com/
接口：https://api.example.com/
```

前端构建参数：

```text
VITE_API_URL=https://api.example.com/
```

必须额外处理：

- 后端 CORS；
- Cookie 的 `Domain`、`SameSite`、`Secure`；
- HTTPS 证书；
- 预检请求 `OPTIONS`；
- 不同域名下的登录态和文件上传限制。

除非有明确的独立扩展需求，否则博客、CMS、API 推荐先采用同源模式。

---

## 六、可复刻的 Docker 打包 SOP

### 步骤 1：先确定对外路由表

在写 Dockerfile 之前，先明确以下路由：

| 类型 | 推荐路径 | 示例 |
| --- | --- | --- |
| 博客前台 | `/` | `https://example.com/` |
| 管理后台页面 | `/cms/` | `https://example.com/cms/` |
| 公开 API | `/api/` 或根路径 | `https://example.com/api/articles` |
| 管理 API | `/admin/` | `https://example.com/admin/articles` |
| 上传资源 | `/static/` | `https://example.com/static/blogs/1/a.png` |

BlogLoom 当前公开 API 使用根路径，例如 `/columns/tree`；新项目更推荐显式使用 `/api/`，可以减少和页面路由、静态资源路由发生冲突的概率。

### 步骤 2：让前端业务代码只使用相对接口路径

正确：

```js
request({ url: 'articles', method: 'GET' })
```

不推荐：

```js
request({ url: 'https://api.example.com/articles', method: 'GET' })
```

业务代码中写死完整域名，会导致：

- 本地、测试、生产环境难以切换；
- 域名变更必须修改大量业务文件；
- 同源部署和反向代理失去意义；
- 线上可能出现 HTTP/HTTPS 混用。

### 步骤 3：在前端请求客户端读取 `VITE_API_URL`

```js
const request = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api/',
  timeout: 10000
})
```

环境文件可以提供开发默认值：

```dotenv
# .env.development
VITE_API_URL=http://localhost:8090/api/
```

```dotenv
# .env.production
VITE_API_URL=https://example.com/api/
```

### 步骤 4：Docker 构建阶段显式覆盖生产值

推荐写法：

```dockerfile
FROM node:18 AS frontend-build

ARG VITE_PUBLIC_API_BASE=/api/
WORKDIR /workspace/frontend

COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./

RUN VITE_API_URL="${VITE_PUBLIC_API_BASE}" npm run build
```

构建时可按环境传入：

```bash
docker build \
  --build-arg VITE_PUBLIC_API_BASE=/api/ \
  -t blogloom:1.0.0 .
```

如果是前台和后台两个独立前端，应分别配置：

```dockerfile
# 前台
RUN VITE_API_URL=/api/ npm run build

# 后台
RUN VITE_API_URL=/admin/ npm run build -- --base=/cms/
```

### 步骤 5：后端提供对应的静态页面和 API 路径

前端 baseURL 配置完成后，后端必须满足路由对应关系：

```text
前端请求 /api/articles  → 后端存在 /api/articles
前端请求 /admin/login   → 后端存在 /admin/login
页面请求 /cms/dashboard  → 返回 /cms/index.html
```

如果使用 Nginx，则至少要保证：

```nginx
location / {
    proxy_pass http://blogloom-app:8090;
}

location /api/ {
    proxy_pass http://blogloom-app:8090;
}

location /admin/ {
    proxy_pass http://blogloom-app:8090;
}
```

如果由 Spring Boot 直接托管静态文件，则不需要额外的 Nginx，但必须实现 SPA history 路由回退。例如 `/cms/dashboard` 刷新时应回退到 `/cms/index.html`，而 `/admin/**`、`/api/**`、静态资源请求不能被错误回退成 HTML。

### 步骤 6：运行时配置只维护后端绝对地址

Docker Compose 中可以配置：

```yaml
environment:
  BLOG_API: https://example.com
  BLOG_CMS: https://example.com/cms
  BLOG_VIEW: https://example.com
```

这些变量用于后端生成外链。前端 API 地址已经在镜像构建阶段完成注入，不要误以为在 Compose 中设置：

```yaml
environment:
  VITE_API_URL: https://example.com/api/
```

就能改变静态前端的 Axios 配置。对于普通 Vite 静态产物，这样设置不会自动生效，除非项目额外实现了运行时配置文件机制。

---

## 七、运行时配置与构建时配置的边界

### 7.1 构建时配置

适合放入 `VITE_*` 的内容：

- API baseURL；
- 前端部署子路径；
- 是否启用某个前端功能；
- 构建版本号。

特点：

- 构建时读取；
- 会进入静态 JavaScript；
- 修改后必须重新构建前端或重新构建镜像。

### 7.2 运行时配置

适合放入 Docker Compose 环境变量的内容：

- 数据库连接；
- JWT 密钥；
- 邮箱、对象存储、通知服务配置；
- 后端生成链接需要的站点域名；
- JVM 参数。

特点：

- 容器启动时读取；
- 修改后通常重启容器即可；
- 不会自动改变已经编译好的前端 JavaScript。

### 7.3 如果确实需要运行时切换前端 API 地址

可增加运行时配置文件：

```html
<!-- index.html -->
<script src="/runtime-config.js"></script>
```

```js
// runtime-config.js
window.__APP_CONFIG__ = {
  API_BASE_URL: '/api/'
}
```

```js
const request = axios.create({
  baseURL: window.__APP_CONFIG__?.API_BASE_URL || '/api/'
})
```

然后由 Docker 启动脚本根据环境变量生成 `runtime-config.js`。这属于另一种架构，只有在同一个镜像必须适配多个域名或多个后端地址时才建议采用；普通同源部署使用构建时注入更简单、更容易验证。

---

## 八、域名部署复刻示例

假设新项目需要部署到：

```text
https://demo.example.com
```

后端容器监听 `8090`，对外映射 `18080`。

### 8.1 前台构建配置

```dockerfile
RUN VITE_API_URL=/api/ npm run build
```

前端业务接口：

```js
url: 'articles'
```

浏览器请求：

```text
https://demo.example.com/api/articles
```

### 8.2 管理后台构建配置

```dockerfile
RUN VITE_API_URL=/admin/ npm run build -- --base=/cms/
```

管理后台登录请求：

```text
https://demo.example.com/admin/login
```

后台页面：

```text
https://demo.example.com/cms/
```

### 8.3 Compose 运行配置

```yaml
environment:
  BLOG_API: https://demo.example.com
  BLOG_CMS: https://demo.example.com/cms
  BLOG_VIEW: https://demo.example.com
ports:
  - "18080:8090"
```

这里端口映射只影响用户访问的外部入口，不影响前端使用相对路径的方式：

```text
浏览器访问 https://demo.example.com
前端请求 /api/articles
最终仍然是 https://demo.example.com/api/articles
```

---

## 九、部署验证 SOP

每次打包、换域名或修改路由后，按以下顺序验证。

### 9.1 构建前检查

- [ ] 前台请求客户端读取 `import.meta.env.VITE_API_URL`；
- [ ] API 文件使用相对路径，不写死生产域名；
- [ ] `.env.production` 的默认值没有被误认为 Docker 运行时配置；
- [ ] Dockerfile 明确设置了 `VITE_API_URL`；
- [ ] 前台、后台的 API 前缀分别明确；
- [ ] 前端构建子路径与静态资源服务路径一致。

### 9.2 镜像构建后检查

可以在构建产物或镜像中搜索旧地址：

```bash
rg -n "localhost:8090|127.0.0.1|api\.example\.com" blog-view-ui/dist blog-cms-ui/dist
```

生产同源构建时，前台产物不应继续携带错误的 `http://localhost:8090/`。

### 9.3 容器启动后检查

```bash
curl -I https://demo.example.com/
curl -I https://demo.example.com/cms/
curl -i https://demo.example.com/api/articles
curl -i https://demo.example.com/admin/login
```

注意：登录接口可能需要 POST 参数，`curl -I` 只能验证路由是否可达；接口业务验证应使用正确的 HTTP 方法和请求体。

### 9.4 浏览器检查

打开浏览器开发者工具 Network 面板，确认：

- [ ] 请求协议为 HTTPS；
- [ ] 请求 Host 为当前站点域名；
- [ ] 前台接口没有请求到 `localhost`；
- [ ] 管理后台接口以 `/admin/` 开头；
- [ ] 页面刷新 `/home`、`/blog/1`、`/cms/dashboard` 不返回 404；
- [ ] 接口响应是 JSON，不是前端 `index.html`；
- [ ] 静态 JS、CSS、图片路径没有错误地带上 `/cms/cms/` 或缺少 `/cms/`。

### 9.5 推荐的最小验收用例

| 验收项 | 预期结果 |
| --- | --- |
| 访问 `/home` | 页面正常加载 |
| 前台调用 `columns/tree` | 请求当前域名下的 `/columns/tree` 或约定的 `/api/columns/tree` |
| 访问 `/cms/` | 管理后台入口正常加载 |
| 后台登录 | 请求当前域名下的 `/admin/login` |
| 刷新 `/cms/dashboard` | 返回后台 SPA 页面，不是 404 |
| 修改 `WEB_PORT` | 前端仍可通过当前域名相对路径访问 API |
| 修改 `BLOG_API` 后重启 | 后端生成的外链改变，前端 `baseURL` 不改变 |
| 使用 HTTPS 域名 | 不出现 Mixed Content |

---

## 十、常见问题与排障

### 10.1 生产环境仍请求 `http://localhost:8090`

常见原因：

- 直接执行了 `npm run build`，没有使用 Dockerfile 中的覆盖变量；
- 使用了旧镜像，未重新构建或未重新加载镜像；
- `.env.production` 仍是本地默认值；
- Dockerfile 的 `RUN VITE_API_URL=... npm run build` 没有执行到目标前端。

处理方式：

```bash
docker build --no-cache -f docker/Dockerfile -t blogloom:<new-tag> .
docker save blogloom:<new-tag> -o blogloom-<new-tag>.tar
```

然后重新 `docker load`、更新镜像标签并重启容器。

### 10.2 请求 `/columns/tree` 返回 404

检查顺序：

1. 后端是否存在对应 Controller 或路由映射；
2. 前端 API 路径是否拼写正确；
3. `VITE_API_URL` 是否配置成了错误前缀；
4. Nginx 是否把 `/columns/` 转发到了正确的后端；
5. 请求是否被前端 SPA 回退逻辑错误地处理成 `index.html`。

### 10.3 请求变成 `/home/columns/tree`

通常说明请求 URL 被当作相对于当前页面目录的普通浏览器 URL 解析，或者没有正确使用 Axios 实例。

检查：

- 请求是否通过统一 `request` 实例发起；
- `baseURL` 是否为 `/` 或 `/api/`；
- API URL 是否误写成 `./columns/tree`；
- 是否有原生 `fetch('columns/tree')` 绕过了统一请求客户端。

### 10.4 管理后台请求成 `/cms/admin/login`

说明管理后台的 API 前缀配置错误。管理后台页面部署在 `/cms/`，但 API 通常部署在 `/admin/`，两者不是同一个路径：

```text
页面资源前缀：/cms/
API 前缀：    /admin/
```

应该在 Docker 构建时设置：

```dockerfile
RUN VITE_API_URL=/admin/ npm run build -- --base=/cms/
```

### 10.5 页面可以打开，但接口返回 HTML

通常是 SPA 回退规则过宽：

- `/api/**` 或 `/admin/**` 被错误回退到 `index.html`；
- Nginx 的 `try_files` 优先级配置错误；
- 后端没有区分页面请求与 API 请求。

原则是：

```text
页面 history 路由 → 可以回退 index.html
API 路由           → 必须交给后端 Controller
静态资源路由       → 必须返回真实文件
```

### 10.6 修改 Docker Compose 的 `VITE_API_URL` 不生效

这是正常现象。Vite 静态产物的 `VITE_API_URL` 在构建时已经被编译进去，运行容器时再注入同名环境变量不会自动改变浏览器中的 JavaScript。

处理方式二选一：

- 重新构建镜像；
- 改造为运行时 `runtime-config.js` 方案。

---

## 十一、最终复刻模板

将本方案复刻到其他项目时，最小实现只需要保证以下内容一致：

### 前端请求客户端

```js
const request = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api/',
  timeout: 10000
})
```

### 前端业务接口

```js
request({
  url: 'articles',
  method: 'GET'
})
```

### Docker 构建

```dockerfile
RUN VITE_API_URL=/api/ npm run build
```

### 后端或 Nginx 路由

```text
/              → 前端页面
/api/**        → 公开 API
/admin/**      → 管理 API
/cms/**        → 管理后台页面
```

### 验证结果

```text
https://example.com/              页面正常
https://example.com/api/articles  API 正常
https://example.com/cms/          后台正常
https://example.com/admin/login   后台 API 正常
```

只要“前端 `baseURL`、Docker 构建参数、后端 API 路由、反向代理规则”四者保持一致，就能稳定复刻前后端同源部署效果。

---

## 十二、验收结论

本项目当前实现满足以下结论：

1. 博客前台 Docker 构建时使用 `VITE_API_URL=/`；
2. 前台 `columns/tree` 通过 Axios 拼接为当前域名下的 `/columns/tree`；
3. 管理后台 Docker 构建时使用 `VITE_API_URL=/admin/`；
4. 前台页面、管理后台页面和后端 API 共用同一个 Spring Boot 容器；
5. `BLOG_API`、`BLOG_CMS`、`BLOG_VIEW` 只负责后端运行时的绝对链接配置，不负责修改前端 Axios `baseURL`；
6. 如果其他项目遵循本文的路由规划、构建注入和验证流程，即可复刻同源部署方案。
