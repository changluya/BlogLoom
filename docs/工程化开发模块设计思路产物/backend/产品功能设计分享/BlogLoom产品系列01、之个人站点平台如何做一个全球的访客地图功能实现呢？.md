[toc]



# 个人站点平台如何做一个全球的访客地图功能实现呢？

我最初的念头很简单：后台放一个能缓缓自转的地球，访客按数量亮成光点，悬停能看到城市。纯视觉愿望，甚至有点"想要个特效"。

真做下去才发现，难的不是让它转，而是让它转起来的每个点都对得上数。这篇记录我怎么把这件事从"一个 3D 地球"拆成一条可信的数据链路。

## 一、起点：我只有一张看中国的图

后台原本有一张中国地图，城市亮着，点越大访客越多。它只能回答"国内哪些城市来过人"。当时的样子大概是这样：

![image-20261017165144345](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610171651639.png)

直到日志里出现几个海外 IP，我才发现它答不了：

> 昨天有没有人从国外来访问过？

换底图解决不了，旧链路从根上只处理中国城市：

- 聚合任务只统计 `ip_source` 以"中国"开头的记录，海外访客进不了统计；
- `city_visitor` 只有 `city` 和 `uv`，没有日期、国家码、坐标；
- 城市名既是展示名又是主键，同名城市串台；
- 页面写着"实时"，数据其实是每天凌晨跑的。

动手前我先拆掉三个直觉：

| 直觉 | 为什么不对 | 我该问的 |
| --- | --- | --- |
| 换个世界地图就行 | 底图只是最后一层，前面还有地域解析、统计口径、时间窗口 | 数据链路能不能表达"国家/城市 + 时间"？ |
| 3D 地球必须上 WebGL / Three.js | 几百个点，2D Canvas 自绘够用且更可控 | 我要的是能转的球面投影，还是真实三维渲染？ |
| 前端拿 IP 自己定位 | 定位库几十 MB，前端既慢又泄露隐私 | 定位该在写入链路还是展示链路？ |

一张访客地图，80% 的难点在数据口径和地域标准化，20% 才在画得好不好。顺序反了，就会得到一张很酷但对不上数的图。

这里我放一张我最终的效果图：

![image-20261018220829024](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610182208408.png)

## 二、拆解：我怎么用 AI Coding 把愿望切成可验收的小闭环

我的方法只有一句：把**模糊目标切成可验证的小闭环**。最早我甚至没写设计文档，只是把想法丢给 AI，让它先跑出一个能看的原型，提示词就一句话——实现一个可视化的 3D 地图。

**整个拆解只有三步：**

1. 先描述我想要的业务效果，直接做出 MVP Demo 原型。
2. 根据 Demo 确认技术选型，同时把后端数据接口的规范定下来，前期用 mock 数据跑通。
3. 把假数据收敛成一份 `data.json`，后续替换成真实接口时，前端不需要改结构。

### 2.1 我到底想让页面回答什么

| 我想知道 | 页面必须给出 |
| --- | --- |
| 有没有外国人看我的博客 | 地球上出现海外光点，面板显示国家数 |
| 最近一天来了多少人 | 可选"今日/昨日/前 7 天/自定义"，显示对应 PV / UV |
| 现在哪里最热 | 光点大小随 UV 变化，右侧有中国/国外 Top 20 |
| 海外多还是国内多 | 两个榜单分开列，可直接对比 |

这四个问题定出五个设计维度：

```text
空间：全球 → 国家边界 + 海外省州 + 城市坐标
时间：可切片 → 时间窗口，而不是累计表
数值：PV / UV → 明确口径与去重层级
交互：转、缩、悬停、点击 → 投影与命中检测
质量：空地图要能解释 → 定位状态与质量指标
```

### 2.2 先定口径，再选技术栈

我调研时容易犯的错是先搜"3D globe"，然后被库牵着走。这次我把顺序反过来：先问数据从哪来、怎么算、能不能复算；再问接口返回什么；最后才问用什么画。技术栈只服务于最后一步的观感要求，比如我希望它渲染快、交互跟手。

| 调研项 | 结论 | 影响 |
| --- | --- | --- |
| 事实源 | 只用 `visit_log`，不读 `city_visitor` | 口径唯一、可按时间窗复算 |
| 地域解析 | 已集成的 ip2region 离线库 | 无网络依赖、无隐私外发 |
| 地域标准化 | 新增 `visitor_geo` 维表 | 解决同名城市、双语名、坐标来源 |
| 渲染 | Canvas 2D 自绘正交投影球 | 零新增依赖、几百点稳定 |
| 底图 | ECharts 4 自带 world/china + geoBoundaries ADM1 子集 | 离线可用、许可清晰 |
| 刷新 | 仅"今日"10 秒轮询 | 只有当日数据在增长 |

每一行都要能对应到一段代码或一张表。落不了地的，只是技术偏好。

### 2.3 用 MVP Demo 排掉技术风险

地图类需求有个特点：视觉不确定会盖住工程问题。直接在 Vue 工程里写，我会同时面对组件生命周期、构建工具、样式隔离、接口联调，卡住时分不清是投影算错还是组件没挂载。

**单文件 Demo 只验证三件事：**

```text
① 地球能不能转起来（投影 + 交互）
② 国家/省州板块能不能画出来（底图数据 + 解码）
③ 数据能不能变成光点（视觉编码 + 命中）
```

Demo 在 `核心demo/global-visitor-globe.html`，数据在 `核心demo/data/`。不接后端，读 `data.json` 生成的 `visitor-stats.js`，双击 `file://` 也能打开。

Demo 里我保留了两套渲染分支，但固定走 Canvas：

```js
const rendererMode = 'canvas';
if (rendererMode === 'canvas' || !window.Globe) {
    mountFallbackGlobe();   // Canvas 2D 正交投影
    return;                 // 后面的 Globe.gl 分支不会执行
}
const world = Globe()(container)   // WebGL 方案，仅作参考保留
```

| 方案 | 在 Demo 里的角色 | 结论 |
| --- | --- | --- |
| Globe.gl（WebGL） | 验证"材质 + 大气层"能做到什么程度 | 依赖重、标签与命中定制受限 |
| Canvas 2D 自绘 | 实际运行的方案 | 完全可控、零新增依赖 |

正式实现只有 Canvas 2D，不依赖 three.js。

**第一版我用 vibe coding 跑出了一个最小原型：**

![image-20261018221204195](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202610182212481.png)  

**核心梳理的后端的接口契约规范：**

原型阶段最有价值的产出不是那 1100 行 HTML，而是接口契约。契约稳定后，前端用 mock 并行开发，后端独立验证 SQL。

```json
{
  "meta": {
    "schemaVersion": "1.0",
    "dataMode": "database",
    "generatedAt": "2026-10-18T10:00:00+08:00",
    "periodType": "today",
    "periodStart": "2026-10-18T00:00:00+08:00",
    "periodEnd": "2026-10-18T10:00:00+08:00",
    "timezone": "Asia/Shanghai",
    "coordinateSystem": "WGS84",
    "quality": {"resolvedPv": 24, "countryOnlyPv": 4, "unknownPv": 0}
  },
  "summary": {"uv": 17, "pv": 28, "countryCount": 8, "cityCount": 12},
  "countries": [{"countryCode": "US", "uv": 1, "pv": 3}],
  "cities": [{
    "geoKey": "US|CITY|US-012",
    "cityNameZh": "旧金山",
    "cityNameEn": "San Francisco",
    "regionName": "California",
    "countryCode": "US",
    "lat": 37.7749,
    "lng": -122.4194,
    "uv": 1,
    "pv": 3
  }]
}
```

| 字段 | 谁消费 | 不返回会怎样 |
| --- | --- | --- |
| `meta.periodType` | 面板标题、刷新说明 | 不知道当前看的是哪个范围 |
| `meta.quality` | 空地图解释、状态提示 | 空白时无法区分"没人"和"没解析出来" |
| `summary.countryCount` | 右上角指标 | 只能前端自己数，口径会漂 |
| `countries[]` | 国家热度着色、国家榜单 | 只能画城市点，没有板块热度 |
| `cities[].lat/lng` | 光点位置 | 前端得自己维护全球坐标字典 |
| `cities[].regionName` | 中国省份着色与悬停 | 只能按城市画点，做不出板块效果 |

---

## 三、设计思路说明

这一章只讲"为什么这么选"：链路怎么切、前端用什么画、后端怎么定位、口径怎么定。具体代码和表结构统一放在第四章，避免设计和实现混在一起。

我的顺序是：先定义口径，再定义接口，最后定义视觉。视觉不能反过来修改统计事实。

### 3.1 五步链路

```text
① 采集   业务接口 @VisitLogger → AOP 组装日志 → @Async 异步落库
            ↓
② 定位   ip2region 解析 IP → 五段地域文本 → 匹配 visitor_geo
            内网→2 / 空→0 / 城市命中→1 / 国家兜底→1
            ↓
③ 落库   visit_log(uuid, is_page_view, geo_id, geo_status, create_time)
            ↓
④ 聚合   GET /admin/visitor-map/global?periodType=…
            按时间窗 + is_page_view=1 聚合 summary / countries / cities / quality
            ↓
⑤ 渲染   normalizeDataset → Canvas 球面投影 → 光点 / 板块 / 榜单 / Tooltip
```

每一步的核心代码位置：

| 步骤 | 核心代码 |
| --- | --- |
| ① 采集 | `blog-backend/src/main/java/com/changlu/blogloom/annotation/VisitLogger.java`（行为与 PV 口径）<br>`blog-backend/src/main/java/com/changlu/blogloom/aspect/VisitLogAspect.java`（身份识别、组装日志、异步调用） |
| ② 定位 | `blog-backend/src/main/java/com/changlu/blogloom/service/impl/VisitLogServiceImpl.java` 的 `resolveGeo`（内网/空/城市/国家四类状态）<br>`blog-backend/src/main/java/com/changlu/blogloom/mapper/VisitorGeoMapper.java` 的 `findCityGeoId` / `findCountryGeoId` |
| ③ 落库 | `blog-backend/src/main/java/com/changlu/blogloom/service/impl/VisitLogServiceImpl.java` 的 `saveVisitLog`（事务、解析、写入）<br>`blog-backend/src/main/java/com/changlu/blogloom/mapper/VisitLogMapper.java` |
| ④ 聚合 | `blog-backend/src/main/java/com/changlu/blogloom/controller/admin/GlobalVisitorMapAdminController.java`（入口）<br>`blog-backend/src/main/java/com/changlu/blogloom/module/visitormap/service/impl/GlobalVisitorMapServiceImpl.java`（默认值与校验）<br>`blog-backend/src/main/java/com/changlu/blogloom/module/visitormap/domain/VisitorMapWindow.java`（时间窗）<br>`blog-backend/src/main/java/com/changlu/blogloom/module/visitormap/provider/DatabaseGlobalVisitorMapProvider.java`（查询编排）<br>`blog-backend/src/main/java/com/changlu/blogloom/module/visitormap/mapper/GlobalVisitorMapMapper.java` + `blog-backend/src/main/resources/mapper/GlobalVisitorMapMapper.xml`（6 条 SQL） |
| ⑤ 渲染 | `blog-cms-ui/src/views/dashboard/global-visitor-map/index.vue`（时间范围、轮询、`normalizeDataset`）<br>`blog-cms-ui/src/components/VisitorGlobe/GlobeCanvas.vue`（组件生命周期）<br>`blog-cms-ui/src/components/VisitorGlobe/renderer.js`（投影、绘制、命中） |

| 步骤 | 只做什么 | 不做什么 |
| --- | --- | --- |
| 采集 | 固化 `is_page_view` | 不解析地域、不判断 IP |
| 定位 | 产出 `geo_id` + `geo_status` | 不猜坐标、不做统计 |
| 落库 | 存外键与时间 | 不冗余城市名、坐标 |
| 聚合 | 按窗口算 PV/UV | 不读累计表、不改写历史 |
| 渲染 | 消费接口契约 | 不跨层查库、不改口径 |

三条我不允许破的约束：

1. **口径可复算**：每个数字都能用一条 SQL 复算，`resolvedPv + countryOnlyPv + unknownPv = PV`。
2. **刷新不打断**：刷新只替换数据，`yaw/pitch/zoom` 与动画状态不动。
3. **旧链路不动**：`city_visitor`、`visit_record`、旧 `/dashboard` 保持原语义，新功能只加表、加字段、加接口。

### 3.2 前端核心设计

#### 3.2.1 我没有上 WebGL

| 方案 | 画球方式 | 优势 | 代价 | 我的结论 |
| --- | --- | --- | --- | --- |
| **Canvas 2D 自绘** | 单位球向量 → 旋转 → 正交投影 → 2D 路径 | 零新增依赖；背面剔除、裁剪、标签全可控；几百点稳定 60fps | 投影、命中、标签碰撞要自己写 | **选用** |
| ECharts GL `globe` | WebGL 球体 + 图层 | 配置式，生态一致 | 需 ECharts 5+；标签/命中定制受限 | 不选 |
| Three.js | 自建球体网格与材质 | 材质、光照、后期最强 | 新增数百 KB；文字、命中仍要自写 | 不选 |
| Cesium | 完整三维 GIS 引擎 | 地形、影像、相机现成 | 体积与复杂度远超需求 | 不选 |
| MapLibre GL | 矢量瓦片平面地图 | 交互成熟、样式强 | 本质是平面地图，做球体要额外投影 | 不选 |

我的判断依据是数据规模：同时绘制的城市点最多几百个，国家面与省界是静态路径。真正的开销是每帧路径点数，不是 GPU 填充率。

```text
单位球坐标 → yaw/pitch 旋转 → 正交投影 → 屏幕坐标
z <= 0 视为背面，直接剔除；跨背面的折线断开
canvas 圆形 clip，避免陆地画出球外
DPR 上限 1.35，避免高分屏像素量翻四倍
```

#### 3.2.2 我选正交投影

| 方案 | 视觉 | 代价 | 我的结论 |
| --- | --- | --- | --- |
| **正交投影** | 像从太空看地球，远近一样大 | 无近大远小 | **选用** |
| 透视投影 | 有纵深感，像卫星近景 | 边缘变形明显，标签与光点大小需补偿 | 不选 |

每帧对每个点走同一条链路：

```text
经纬度 → 单位球向量 → yaw 旋转 → pitch 旋转 → 正交投影 → 屏幕坐标
```

`z > 0` 才是正面。这是悬停与点击命中的第一道过滤条件，也是背面国家不会透到正面的原因。完整实现在 4.3.5。

我做的不是真正的三维渲染，是把**三维球面投影到二维画布。视觉上像 3D，技术上仍是 2D**，所以后面的实现比 Three.js 简单一个量级。

#### 3.2.3 底图：三种来源各管一段

| 数据 | 来源 | 体积 | 用途 |
| --- | --- | --- | --- |
| 世界国家边界 | ECharts `echarts/map/js/world` | 随 ECharts 4.9.0 | 国家面、国家热度填充 |
| 中国省界 | ECharts `echarts/map/js/china` | 同上 | 中国逐省绘制与高亮 |
| 海外省州 ADM1 | geoBoundaries gbOpen ADM1（CC BY 4.0） | 自建 10 国 / 226 个 ADM1 / 约 309KB | 放大后显示省州边界 |

**两个细节：**

1. ECharts 4 的地图是 `UTF8Encoding` 压缩格式，必须先解码才能拿到经纬度环；解码后置 `UTF8Encoding = false`，避免重复解码。
2. 抽稀只影响轮廓精度、不影响统计：世界边界最多 120 点/环，中国省界最多 220 点/环。中国单独逐省绘制，世界图层排除 `China`，否则两层描边重叠。

耦合代价我也认：ECharts 5/6 起 `echarts/map/js/*` 不再随包发布，升级 ECharts 等于要同时自建 GeoJSON 资源。

#### 3.2.4 视觉编码用 log1p

访客量是长尾分布，线性映射会让除头部城市外的点全挤在最小尺寸：

| 映射 | uv=1 | uv=100 | uv=100000 | 效果 |
| --- | --- | --- | --- | --- |
| 线性 | 0.00 | 0.01 | 1.00 | 中间几乎全糊成一点 |
| `log1p` | 0.17 | 0.56 | 1.00 | 层次可辨 |

```js
intensity = Math.min(Math.log1p(uv) / Math.log1p(maxUv), 1)
radius    = MIN + intensity * (MAX - MIN)     // 2.6 → 9
```

三条上限我写死了，否则脉冲与波纹会溢出：

| 项 | 上限 | 原因 |
| --- | --- | --- |
| 光点半径 | 9px | 脉冲会让半径浮动约 12%，先算脉冲再夹紧 |
| Top5 波纹 | 22px | 随时间扩散，不夹会糊成一团 |
| 光晕 `shadowBlur` | 24 | 大点叠加后视觉噪声过大 |

颜色也按 `uv / maxUv` 映射。这张图表达的是相对热度，不是绝对访客数。

#### 3.2.5 三层命中

| 层级 | 命中方式 | 数据要求 |
| --- | --- | --- |
| 城市 | 屏幕距离 ≤ 28px，且 `z > 0` | 城市经纬度 |
| 省份 | 屏幕点逆投影 → 经纬度 → bbox 粗筛 → 多边形判定 | 省界 rings + bbox |
| 国家 | 国家面填充（暂不做点击下钻） | 国家边界 |

逆投影是关键：把鼠标的屏幕坐标反算回经纬度，再拿经纬度去判断落在哪个省，这样每次 `mousemove` 都不需要把全部省界点重新投影一遍。

只有有访客统计的城市才弹 Tooltip。没数据的城市不显示名称和 0 值，避免噪音；此时鼠标落在城市上等价于落在所在省份上。

### 3.3 后端核心设计：IP 怎么变成可信地点

#### 3.3.1 IP 库

我用的 ip2region 是开源项目，地址在 [github.com/lionsoul2014/ip2region](https://github.com/lionsoul2014/ip2region)，IPv4 / IPv6 的 XDB 数据可以从它的 [data 目录](https://github.com/lionsoul2014/ip2region/tree/master/data) 直接下载。

| 方案 | 优势 | 代价 | 我的结论 |
| --- | --- | --- | --- |
| **ip2region 离线双栈** | 免费、离线、IPv4/IPv6、已集成 | 精度取决于库版本；各国城市命名不统一 | **选用** |
| 在线 IP API | 数据较新 | 网络依赖、限流、隐私外发 | 不选 |
| MaxMind GeoLite2 | 字段规范、生态成熟 | 需账号与定期更新 | 备选 |

启动时加载 `ip2region_v4.xdb` + `ip2region_v6.xdb`，全量内存缓存约 47MB，按 IP 版本自动路由。

查询结果统一为五段：

```text
Country|Province|City|ISP|ISO-Alpha2

中国|广东省|深圳市|电信|CN
United States|California|San Francisco||US
```

三个规范化细节：

- 空值与上游的 `0` 统一替换为空字符串，但保留分隔位置，避免国家码错位。
- 全部为空时返回空字符串，而不是 `"||||"`。
- IP 获取顺序：`X-Real-IP` → `x-forwarded-for` → `Proxy-Client-IP` → … → `remoteAddr`。

#### 3.3.2 四类状态

```text
IP → 五段地域文本
  ├─ 内网 IP                → geo_status = 2，不上图
  ├─ 地域为空               → geo_status = 0，不上图
  ├─ 城市命中 visitor_geo(level=2) → geo_id，有坐标，上图
  └─ 城市未命中但有国家码   → 降级到 visitor_geo(level=1)，只进国家统计
```

城市匹配同时支持中英文名，并用国家码约束，避免同名城市串台（具体 SQL 见 4.2.4）。

如果只存 `geo_id`，地图空白时我分不清三种情况：真的没人、IP 没解析出来、城市字典没收录。`geo_status` 加质量指标，把空白变成可解释的状态。

#### 3.3.3 统计口径

```text
PV      = 周期内 is_page_view = 1 的 visit_log 行数
全球 UV = COUNT(DISTINCT uuid)
国家 UV = COUNT(DISTINCT uuid) GROUP BY country_code
城市 UV = COUNT(DISTINCT uuid) GROUP BY geo_id (geo_level = 2)
```

| 规则 | 说明 |
| --- | --- |
| 分层去重 | 全球 UV ≠ 国家 UV 之和；国家 UV ≠ 城市 UV 之和 |
| 跨城市访客 | 全球只算 1，两个城市各算 1 |
| 时间窗口 | 左闭右开 |
| 时区 | 自然日边界按 `timezone` 计算 |
| PV 口径 | 由 `VisitBehavior.pageView` 在写入时固化 |

质量三指标，三者之和等于周期内 PV：

| 字段 | 含义 | 口径 |
| --- | --- | --- |
| `resolvedPv` | 命中城市级 | `geo_level = 2` 且 `geo_status = 1` |
| `countryOnlyPv` | 仅到国家级 | `geo_level = 1` 且 `geo_status = 1` |
| `unknownPv` | 未知/内网/无效 | `geo_status != 1 OR geo_id IS NULL` |

### 3.4 取舍汇总

| 能力 | 我的选择 | 理由 |
| --- | --- | --- |
| 渲染 | Canvas 2D 自绘 | 数据量小、零新增依赖、完全可控 |
| 投影 | 正交投影 + 背面剔除 | 视觉最像看地球，实现最直接 |
| 世界/中国底图 | ECharts 4 内置地图资产 | 已随依赖存在、可离线 |
| 海外省州 | geoBoundaries ADM1 子集 | 有 ADM1 层级、许可清晰 |
| IP 解析 | ip2region 3.3.7 双栈 XDB | 免费离线、已集成 |
| 地域与坐标 | `visitor_geo` 维表 | 稳定键 + 国家码 + 坐标 + 双语 |
| 视觉编码 | `log1p` + 上下限 | 长尾下层次可辨且不溢出 |
| 聚合 | `visit_log` 实时 SQL | 口径唯一、可复算 |
| 刷新 | 仅"今日"10 秒轮询 | 无状态、够用、不做无效查询 |

## 四、落地：从一次请求到地球上的一个光点

按真实顺序：请求进来 → 身份识别 → 地域定位 → 落库 → 接口聚合 → 前端渲染 → 验证。

### 4.1 端到端链路

```text
浏览器请求 /blogs
  │
  ├─ @VisitLogger(VisitBehavior.INDEX)
  ▼
VisitLogAspect.logAround
  ├─ 记录开始时间 → 执行业务方法 → 计算耗时
  ├─ checkIdentification(request)
  │    ├─ 有 identification：cache_entry → visitor 表 → 恢复 / 重新签发
  │    └─ 无 identification：按"小时时间戳 + IP + UA"生成 UUID
  ├─ handleLog(...)：URI、IP、UA、is_page_view
  └─ visitLogService.saveVisitLog(log)   ← @Async
        │
        ▼
VisitLogServiceImpl.saveVisitLog（@Transactional）
  ├─ ip2region → 五段地域文本
  ├─ resolveGeo：内网 / 空 / 城市命中 / 国家兜底
  ├─ yauaa → OS / 浏览器
  └─ INSERT visit_log
        │
        ▼
GET /admin/visitor-map/global
  └─ Controller → Service → VisitorMapWindow.resolve
       → DatabaseGlobalVisitorMapProvider → Mapper（6 条 SQL）
        │
        ▼
前端 index.vue
  ├─ 时间范围 / 刷新锁 / 轮询
  ├─ normalizeDataset：补齐 city/cityEn/rank
  └─ GlobeCanvas → renderer.setDataset(...)
        │
        ▼
Canvas 2D 球面投影渲染
```

### 4.2 后端链路

后端一共四步：采集 → 定位 → 落库 → 聚合。每一步只做一件事，边界清楚，后面才不会互相污染。

```text
① 采集   @VisitLogger → VisitLogAspect → @Async
② 定位   ip2region 五段文本 → visitor_geo 匹配
③ 落库   visit_log(is_page_view, geo_id, geo_status, create_time)
④ 聚合   /admin/visitor-map/global → 6 条 SQL
```

#### 4.2.1 采集：PV 口径写进枚举

旧实现靠中文行为文案判断是否算 PV，文案一改口径就漂。现在把"是否计入 PV"变成枚举属性：

```java
public enum VisitBehavior {
    UNKNOWN("UNKNOWN", "UNKNOWN", false),
    INDEX("访问页面", "首页", true),
    ARCHIVE("访问页面", "归档", true),
    BLOG("查看博客", "", true),
    SEARCH("搜索博客", "", true),
    CLICK_FRIEND("点击友链", "", false),
    LIKE_MOMENT("点赞动态", "", false),
    CHECK_PASSWORD("校验博客密码", "", false);
}
```

控制器只声明行为，切面负责写入：

```java
@VisitLogger(VisitBehavior.INDEX)
@GetMapping("/blogs")
public Result blogs(@RequestParam(defaultValue = "1") Integer pageNum) { ... }

log.setPageView(visitLogger.value().isPageView());
```

哪些接口算 PV 由代码常量决定，有单元测试保护，不再依赖数据库里的历史文案。

#### 4.2.2 UUID：访客身份怎么来

PV 靠日志行数，UV 靠 `COUNT(DISTINCT uuid)`，所以 `uuid` 的质量直接决定 UV 是否可信。整条身份逻辑在 `VisitLogAspect.checkIdentification` 里：

```text
有 identification 请求头
  ├─ 缓存里有        → 直接复用
  ├─ 缓存没有、库里有 → 回填缓存后复用
  └─ 库里也没有      → 重新签发
没有 identification 请求头 → 直接签发
```

```java
private String checkIdentification(HttpServletRequest request) {
    String identification = request.getHeader("identification");
    if (identification == null) {
        return saveUUID(request);              // 首次访问，签发
    }
    if (cacheService.hasValueInSet(CacheKeyConstants.IDENTIFICATION_SET, identification)) {
        return identification;                 // 命中缓存，最热路径
    }
    if (visitorService.hasUUID(identification)) {
        cacheService.saveValueToSet(CacheKeyConstants.IDENTIFICATION_SET, identification);
        return identification;                 // 缓存失效但库里还有，回填
    }
    return saveUUID(request);                  // 伪造或过期，重新签发
}
```

签发时不是随机 UUID，而是用"小时时间戳 + IP + UA"做确定性哈希：

```java
private String saveUUID(HttpServletRequest request) {
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);                       // 截断到小时
    String timestamp = Long.toString(calendar.getTimeInMillis() / 1000);
    String ip = IpAddressUtils.getIpAddress(request);
    String userAgent = request.getHeader("User-Agent");

    String uuid = UUID.nameUUIDFromBytes((timestamp + ip + userAgent).getBytes()).toString();

    response.addHeader("identification", uuid);             // 回写给前端
    response.addHeader("Access-Control-Expose-Headers", "identification");   // 允许跨域读取
    // 首次出现才落库 visitor 表 + 缓存集合
    return uuid;
}
```

三个输入分别是什么：

| 输入 | 取值来源 | 例子 | 作用 |
| --- | --- | --- | --- |
| 小时时间戳 | `Calendar` 截断分秒后取秒数 | `1787000000` | 把身份有效期限制在一小时内，同一人一小时内反复刷新不会重复计数 |
| IP | `IpAddressUtils.getIpAddress(request)` | `119.137.52.1` | 区分不同网络出口的访客 |
| UA | 请求头 `User-Agent` | `Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36` | 区分同一出口 IP 下的不同浏览器 / 设备 |

UA 就是浏览器自报的"身份串"，包含操作系统、浏览器内核和版本。它的作用是：公司或校园网里很多人共用一个出口 IP，如果只用 IP，这些人都算同一个访客；加上 UA 后，Windows + Chrome 和 macOS + Safari 会被区分成两个访客。

但 UA 的区分能力有限：同型号设备、同版本浏览器、同网络出口的两个人，UA 完全一致，仍会被算成一个人。这是"不采集指纹"前提下的必然结果，也是为什么我在页面上把它称作 UV 而不是"真实人数"。

| 设计点 | 为什么这么做 |
| --- | --- |
| 截断到小时 | 同一 IP + UA 在一小时内多次无头访问会得到同一个 UUID，避免刷高 UV |
| 确定性哈希 | 不依赖随机数，重算即可复现，便于排查 |
| 回写响应头 | 前端保存后，后续请求带上，UV 才能跨页面保持稳定 |
| 缓存 + 库双查 | 缓存抗热点，库做持久兜底 |
| 只落 `visitor` 表 | `visit_log` 只存 `uuid` 字符串，UV 直接 `COUNT(DISTINCT)`，不需要 join |

代价也很明确：UUID 的有效期跟着小时走，跨小时且前端没带 `identification` 时可能换新 UUID，UV 会略微偏高。这是我在"防刷"和"绝对准确"之间做的取舍。

#### 4.2.3 操作系统与浏览器：yauaa 解析 UA

`visit_log.os` 和 `visit_log.browser` 不是前端传的，也不是正则手写的，而是用 [yauaa](https://github.com/nielsbasjes/yauaa) 从 `User-Agent` 解析出来的。依赖版本在 `blog-backend/pom.xml` 里固定为 7.11.0：

```xml
<dependency>
    <groupId>nl.basjes.parse.useragent</groupId>
    <artifactId>yauaa</artifactId>
    <version>7.11.0</version>
</dependency>
```

初始化时只加载需要的两个字段，并开 10000 条缓存，避免重复解析同一个 UA：

```java
@Component
public class UserAgentUtils {
    private final UserAgentAnalyzer uaa;

    public UserAgentUtils() {
        this.uaa = UserAgentAnalyzer.newBuilder()
                .useJava8CompatibleCaching()
                .withCache(10000)
                .hideMatcherLoadStats()
                .withField(UserAgent.OPERATING_SYSTEM_NAME_VERSION_MAJOR)   // 如 "Mac OS 10.15"
                .withField(UserAgent.AGENT_NAME_VERSION)                    // 如 "Chrome 120.0.0.0"
                .build();
    }

    public UserAgentDTO parseOsAndBrowser(String userAgent) {
        UserAgent agent = uaa.parse(userAgent);
        return new UserAgentDTO(
                agent.getValue(UserAgent.OPERATING_SYSTEM_NAME_VERSION_MAJOR),
                agent.getValue(UserAgent.AGENT_NAME_VERSION));
    }
}
```

调用点在 `saveVisitLog` 里，和地域解析并列，一起写进同一条日志：

```java
String ipSource = ipAddressUtils.getCityInfo(log.getIp());
log.setIpSource(ipSource);
resolveGeo(log, ipSource);

UserAgentDTO ua = userAgentUtils.parseOsAndBrowser(log.getUserAgent());
log.setOs(ua.getOs());
log.setBrowser(ua.getBrowser());
```

几个实现上的取舍：

| 点 | 说明 |
| --- | --- |
| 只加载两个字段 | yauaa 支持上百个字段，全量加载会显著拖慢启动；这里只要 OS 和浏览器 |
| 版本带主版本号 | `OPERATING_SYSTEM_NAME_VERSION_MAJOR` 只到主版本，够展示且不会因小版本频繁变化 |
| 缓存 10000 条 | 同一个站点的 UA 高度重复，缓存命中率很高 |
| 解析失败不阻断 | yauaa 对未知 UA 返回 `Unknown`，不会抛异常，日志照常落库 |
| 与地图无关 | `os` / `browser` 只用于访问明细展示，不参与 PV / UV / 地域聚合 |

它和 UUID 里的 UA 是同一个原始字符串，只是用途不同：UUID 用原文做哈希，这里用解析后的结构化结果做展示。

#### 4.2.4 定位：四类状态落库

定位分两段：先用 ip2region 把 IP 变成五段文本，再拿文本去 `visitor_geo` 匹配 `geo_id`。

**① IP 解析：ip2region 双栈 API 用法**

ip2region 是开源项目：[github.com/lionsoul2014/ip2region](https://github.com/lionsoul2014/ip2region)，官方文档见 [ip2region 使用说明](https://github.com/lionsoul2014/ip2region/blob/master/README.md)，XDB 数据在 [data 目录](https://github.com/lionsoul2014/ip2region/tree/master/data) 下载（`ip2region_v4.xdb`、`ip2region_v6.xdb`）。

启动时一次性加载 IPv4 / IPv6 两份 XDB，常驻内存：

```java
private Ip2Region ip2Region;

@PostConstruct
void initIp2regionResource() throws Exception {
    try (InputStream v4 = new ClassPathResource("ipdb/ip2region_v4.xdb").getInputStream();
         InputStream v6 = new ClassPathResource("ipdb/ip2region_v6.xdb").getInputStream()) {
        Config v4Config = Config.custom()
                .setCachePolicy(Config.BufferCache)   // 全量缓存进内存
                .setXdbInputStream(v4)                // 用流读取，兼容可执行 JAR
                .asV4();
        Config v6Config = Config.custom()
                .setCachePolicy(Config.BufferCache)
                .setXdbInputStream(v6)
                .asV6();
        ip2Region = Ip2Region.create(v4Config, v6Config);   // 一个实例同时管 v4/v6
    }
}
```

查询时不用自己判断 IP 版本，`search` 会按 IP 自动路由到 v4 或 v6 库：

```java
public String getCityInfo(String ip) {
    if (StringUtils.isBlank(ip) || ip2Region == null) return "";
    try {
        return normalizeRegion(ip2Region.search(ip.trim()));
    } catch (Exception e) {
        log.warn("ip2region 查询失败，ip={}", ip, e);
    }
    return "";
}
```

`search` 返回的原始文本就是五段，缺失字段是 `0` 或空串。这里统一清洗，但**保留分隔位置**，否则国家码会错位：

```java
private String normalizeRegion(String region) {
    if (StringUtils.isBlank(region)) return "";
    String[] fields = region.split("\\|", -1);     // -1 保留末尾空段
    boolean hasValue = false;
    for (int i = 0; i < fields.length; i++) {
        fields[i] = StringUtils.trimToEmpty(fields[i]);
        if ("0".equals(fields[i])) fields[i] = "";   // 上游用 0 表示未知
        if (StringUtils.isNotBlank(fields[i])) hasValue = true;
    }
    return hasValue ? String.join("|", fields) : "";  // 全空返回 ""，而不是 "||||"
}
```

结果形如：

```text
中国|广东省|深圳市|电信|CN
United States|California|San Francisco||US
```

调用方在 `saveVisitLog` 里，先取 IP 再解析地域，最后落库：

```java
@Transactional(rollbackFor = Exception.class)
public void saveVisitLog(VisitLog log) {
    String ipSource = ipAddressUtils.getCityInfo(log.getIp());
    log.setIpSource(ipSource);
    resolveGeo(log, ipSource);        // 五段文本 → geo_id + geo_status
    if (visitLogMapper.saveVisitLog(log) != 1) {
        throw new PersistenceException("日志添加失败");
    }
}
```

IP 本身来自 `IpAddressUtils.getIpAddress(request)`，按代理头依次兜底，取不到再退到 `remoteAddr`：

```text
X-Real-IP → x-forwarded-for → Proxy-Client-IP → WL-Proxy-Client-IP
→ HTTP_CLIENT_IP → HTTP_X_FORWARDED_FOR → remoteAddr
```

**② 匹配：四类状态**

```java
private void resolveGeo(VisitLog log, String ipSource) {
    // 内网 IP：不查库，直接标记为 2（不上图）
    if (isPrivateIp(log.getIp())) { log.setGeoStatus(2); return; }
    // 解析结果为空：标记为 0（未知）
    if (ipSource == null || ipSource.trim().isEmpty()) { log.setGeoStatus(0); return; }

    // 五段文本：国家|省|市|ISP|国家码
    String[] fields = ipSource.split("\\|", -1);
    String city = fields.length > 2 ? fields[2].trim() : "";
    String countryCode = fields.length > 4 ? fields[4].trim().toUpperCase(Locale.ROOT) : "";

    Long geoId = null;
    // 优先命中城市级（geo_level = 2），有坐标，可上图
    if (!city.isEmpty()) {
        geoId = visitorGeoMapper.findCityGeoId(city, countryCode);
    }
    // 城市没命中但有国家码：降级到国家级（geo_level = 1），只进国家统计
    if (geoId == null && countryCode.length() == 2) {
        geoId = visitorGeoMapper.findCountryGeoId(countryCode);
    }

    log.setGeoId(geoId);
    // 有 geo_id 即已定位（1），否则未知（0）
    log.setGeoStatus(geoId == null ? 0 : 1);
}
```

内网判断覆盖 IPv4 私网段、回环、链路本地，以及 IPv6 的 `::1`、`fc`、`fd`、`fe80:`：

```java
private boolean isPrivateIp(String ip) {
    if (ip == null) return false;
    String normalized = ip.trim().toLowerCase(Locale.ROOT);
    if (normalized.equals("::1") || normalized.startsWith("fc")
            || normalized.startsWith("fd") || normalized.startsWith("fe80:")) return true;
    if (normalized.startsWith("10.") || normalized.startsWith("127.")
            || normalized.startsWith("192.168.") || normalized.startsWith("169.254.")) return true;
    if (normalized.startsWith("172.")) {
        String[] parts = normalized.split("\\.");
        if (parts.length > 1) {
            try {
                int second = Integer.parseInt(parts[1]);
                return second >= 16 && second <= 31;      // 172.16.0.0/12
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
    }
    return false;
}
```

城市匹配同时支持中英文名，并用国家码消歧：

```sql
SELECT id FROM visitor_geo
WHERE geo_level = 2
  AND (city_name_zh = #{cityName} OR city_name_en = #{cityName})
  AND country_code = #{countryCode}
LIMIT 1
```

| 输入情况 | `geo_id` | `geo_status` | 地图表现 |
| --- | --- | --- | --- |
| 内网 IP | `NULL` | `2` | 不上图，计入 `unknownPv` |
| 地域文本为空 | `NULL` | `0` | 不上图，计入 `unknownPv` |
| 城市命中 | 城市行 | `1` | 有坐标，画光点 |
| 仅国家命中 | 国家行 | `1` | 无坐标，只着色板块 |

只有 `geo_id` 时，地图空白无法区分"真的没人""IP 没解析出来""城市字典没收录"。`geo_status` 把空白变成可解释的状态。

#### 4.2.5 数据库核心表设计

整个功能只新增一张维表、扩展一张事实表，不新增聚合表：

```text
visit_log（访问事实）
  ├─ geo_id ──────────► visitor_geo（标准地域维表：国家/城市、双语名、坐标）
  ├─ uuid ────────────► visitor（访客档案，旧链路继续使用）
  └─ create_time ─────► 按窗口聚合，供全球地图查询

cache_entry（键值缓存）── identificationSet:<uuid> ──► 今日 UV 去重（旧口径）
```

**① `visitor_geo`：标准地域维表**

```sql
CREATE TABLE IF NOT EXISTS `visitor_geo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `geo_key` varchar(160) NOT NULL COMMENT '稳定地域键，如 CN|CITY|CN-003',
  `geo_level` tinyint NOT NULL COMMENT '1国家，2城市',
  `country_code` char(2) NOT NULL COMMENT 'ISO 3166-1 Alpha-2',
  `region_name` varchar(128) DEFAULT NULL COMMENT '省/州，用于区分同名城市',
  `city_name_zh` varchar(128) DEFAULT NULL COMMENT '城市中文名',
  `city_name_en` varchar(128) DEFAULT NULL COMMENT '城市英文名',
  `longitude` decimal(10,6) DEFAULT NULL COMMENT 'WGS84 经度',
  `latitude` decimal(9,6) DEFAULT NULL COMMENT 'WGS84 纬度',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_visitor_geo_key` (`geo_key`),
  KEY `idx_visitor_geo_country_level` (`country_code`, `geo_level`),
  KEY `idx_visitor_geo_zh` (`country_code`, `city_name_zh`),
  KEY `idx_visitor_geo_en` (`country_code`, `city_name_en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='访客标准地域维表';
```

| 字段 | 含义 | 说明 |
| --- | --- | --- |
| `id` | 自增主键 | `visit_log.geo_id` 指向它 |
| `geo_key` | 稳定地域键 | 国家行 `US\|COUNTRY`，城市行 `US\|CITY\|US-012`，不随展示名变化 |
| `geo_level` | 地域层级 | `1` 国家，`2` 城市；聚合靠它区分"城市命中"与"国家兜底" |
| `country_code` | ISO Alpha-2 | 既是国家聚合维度，也是城市匹配的消歧条件 |
| `region_name` | 省 / 州 | 区分同名城市，也用于中国省份着色 |
| `city_name_zh` | 城市中文名 | 展示名 + 匹配键 |
| `city_name_en` | 城市英文名 | 展示名 + 匹配键 |
| `longitude` | 经度 | WGS84，城市行必填，国家行可空 |
| `latitude` | 纬度 | WGS84，城市行必填，国家行可空 |

两行真实数据：

```text
id=1   geo_key=CN|COUNTRY      geo_level=1  country_code=CN  region_name=NULL   city_name_zh=NULL    lng=103.000000  lat=35.000000
id=63  geo_key=CN|CITY|CN-003  geo_level=2  country_code=CN  region_name=广东省  city_name_zh=深圳市  lng=114.064552  lat=22.548457
```

这份字典由增量 SQL 预刷，来源分三块：

| 数据 | 来源 | 说明 |
| --- | --- | --- |
| 国家行 | XDB 中实际出现的国家码 | 首批 19 个常用国家带粗略中心坐标，后续补齐到 249 行，新增行坐标为空，只做聚合兜底 |
| 中国城市行 | XDB 城市名 + 前端 `city2coord.json` | 城市名必须与 ip2region 输出一致（如"深圳市"），坐标取 362 条中国城市字典 |
| 海外城市行 | 手工挑选 20 个主要城市 | 首都 / 知名城市，中英文名与坐标按公开地理数据录入 |

脚本是 `sql/increment/1.0/20261017000000_v1.0.x.sql`（建表 + 首批 19 国 40 城）和 `20261017100000_v1.0.x.sql`（补齐国家码、341 个中国城市、索引），都用 `ON DUPLICATE KEY UPDATE`，可重复执行。

ip2region 的精度上限决定了这张表不需要全量：

| 范围 | 实际粒度 | 结果 |
| --- | --- | --- |
| 中国境内 | 省 / 市 / 区县 | 第三段拿到城市名，命中后上图 |
| 海外主流国家 | 多数只到国家，部分到城市 | 命中城市才上图，否则只着色板块 |
| 小国家 / 长尾 IP | 只有国家码或为空 | `geo_status = 0`，计入 PV 与 `unknownPv` |

**② `visit_log`：访问事实表**

`is_page_view` / `geo_id` / `geo_status` 与组合索引是本功能新增的部分：

```sql
CREATE TABLE `visit_log` (
  `id`           bigint        NOT NULL AUTO_INCREMENT,
  `uuid`         varchar(36)   DEFAULT NULL COMMENT '访客标识码',
  `uri`          varchar(255)  NOT NULL     COMMENT '请求接口',
  `method`       varchar(255)  NOT NULL     COMMENT '请求方式',
  `param`        varchar(2000) NOT NULL     COMMENT '请求参数',
  `behavior`     varchar(255)  DEFAULT NULL COMMENT '访问行为',
  `is_page_view` bit(1)        DEFAULT NULL COMMENT '是否计入页面 PV',
  `content`      varchar(255)  DEFAULT NULL COMMENT '访问内容',
  `remark`       varchar(255)  DEFAULT NULL COMMENT '备注',
  `ip`           varchar(255)  DEFAULT NULL COMMENT 'ip',
  `ip_source`    varchar(255)  DEFAULT NULL COMMENT 'ip来源（五段地域文本）',
  `geo_id`       bigint        DEFAULT NULL COMMENT '关联 visitor_geo.id',
  `geo_status`   tinyint       NOT NULL DEFAULT 0 COMMENT '0未知，1已定位，2内网IP，3无效IP',
  `os`           varchar(255)  DEFAULT NULL COMMENT '操作系统',
  `browser`      varchar(255)  DEFAULT NULL COMMENT '浏览器',
  `times`        int           NOT NULL     COMMENT '请求耗时（毫秒）',
  `create_time`  datetime      NOT NULL     COMMENT '访问时间',
  `user_agent`   varchar(2000) DEFAULT NULL COMMENT 'user-agent用户代理',
  PRIMARY KEY (`id`),
  KEY `idx_visit_map_query` (`is_page_view`, `create_time`, `geo_id`, `uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='访问日志';
```

| 字段 | 在地图里的作用 |
| --- | --- |
| `uuid` | UV 去重依据：`COUNT(DISTINCT uuid)` |
| `behavior` | 枚举文案，仅展示，不再用于判断 PV |
| `is_page_view` | `1` 计入 PV，`0` 不计入，写入时由枚举固化 |
| `ip` | 仅内网判断与重解析，不对外返回 |
| `ip_source` | ip2region 五段文本，回填时按它重新匹配 |
| `geo_id` | 城市行或国家行；未知为 `NULL` |
| `geo_status` | `0` 未知、`1` 已定位、`2` 内网、`3` 无效 |
| `create_time` | 时间窗口过滤的唯一依据，左闭右开 |
| 其余字段 | `uri` / `method` / `param` / `content` / `remark` / `os` / `browser` / `times` / `user_agent` 均不参与地图聚合 |

一条已定位记录：

```text
id=1024  uuid=9f2c...  is_page_view=1
ip=119.137.52.1  ip_source=中国|广东省|深圳市|电信|CN  geo_id=63  geo_status=1
create_time=2026-10-18 09:41:12
```

**③ 索引顺序**

```sql
KEY `idx_visit_map_query` (`is_page_view`, `create_time`, `geo_id`, `uuid`)
```

顺序对应 SQL 执行顺序：

```text
is_page_view 过滤 → create_time 切窗口 → geo_id 分组 → uuid 去重
```

这一条组合索引覆盖 summary / countries / cities / quality 四类查询。

**④ 设计要点**

1. `visit_log` 只存 `geo_id`，不冗余城市名和坐标：改名只改维表，历史日志自动跟着变。
2. 国家级行只做聚合兜底，城市级行才带真实坐标，所以国家只着色板块、不画光点。
3. `country_code` 参与城市匹配，避免 Springfield 这类同名城市串台。
4. 中英文名各建联合索引，国家码在前，与 `findCityGeoId` 的查询顺序一致。
5. 字典不是全量世界城市库：XDB 中约 2.3 万个海外城市，长尾城市降级到国家级，是刻意取舍。

#### 4.2.6 聚合：六条 SQL

六条查询共用同一个过滤片段，`is_page_view = 1` 是 PV 口径，时间条件只在有边界时拼接：

```xml
<sql id="periodFilter">
    log.is_page_view = 1
    <!-- all 视图 start 为 null，这里不会拼条件，也就不会出现假起点 -->
    <if test="start != null">AND log.create_time &gt;= #{start,jdbcType=TIMESTAMP}</if>
    <!-- 左闭右开：end 是下一段的起点，避免跨窗口重复计数 -->
    <if test="end != null">AND log.create_time &lt; #{end,jdbcType=TIMESTAMP}</if>
</sql>
```

**① 汇总：全球 PV / UV + 国家数 + 城市数**

```sql
SELECT COUNT(*) AS pv,                       -- 全局 PV：命中过滤条件的行数
       COUNT(DISTINCT log.uuid) AS uv,       -- 全局 UV：按访客去重，不按国家相加
       COUNT(DISTINCT CASE WHEN geo.country_code IS NOT NULL
                           THEN geo.country_code END) AS country_count,  -- 出现过的国家
       COUNT(DISTINCT CASE WHEN geo.geo_level = 2
                           THEN geo.id END) AS city_count                -- 只数城市级
FROM visit_log log
LEFT JOIN visitor_geo geo ON geo.id = log.geo_id   -- LEFT JOIN：未定位的 PV 仍要计入全球 PV
WHERE log.is_page_view = 1 AND log.create_time >= ? AND log.create_time < ?
```

**② 国家：按国家码聚合**

```sql
SELECT geo.country_code,
       COUNT(*) AS pv,
       COUNT(DISTINCT log.uuid) AS uv
FROM visit_log log
JOIN visitor_geo geo ON geo.id = log.geo_id        -- INNER JOIN：没定位的记录不进国家榜
WHERE log.is_page_view = 1 AND log.create_time >= ? AND log.create_time < ?
GROUP BY geo.country_code                          -- 国家行和城市行都会归到同一个国家码
ORDER BY uv DESC, pv DESC
```

**③ 城市：带坐标，供前端画光点**

```sql
SELECT geo.geo_key, geo.city_name_zh, geo.city_name_en, geo.region_name,
       geo.country_code,
       geo.latitude AS lat, geo.longitude AS lng,  -- 光点位置，直接来自维表
       COUNT(*) AS pv,
       COUNT(DISTINCT log.uuid) AS uv
FROM visit_log log
JOIN visitor_geo geo ON geo.id = log.geo_id
                    AND geo.geo_level = 2          -- 只要城市级，国家级没有坐标
WHERE log.is_page_view = 1 AND log.create_time >= ? AND log.create_time < ?
GROUP BY geo.id, geo.geo_key, geo.city_name_zh, geo.city_name_en,
         geo.region_name, geo.country_code, geo.latitude, geo.longitude
ORDER BY uv DESC, pv DESC                          -- 顺序决定前端 rank 与 Top 榜
```

**④ 质量三指标：三段之和等于 PV**

```sql
-- resolvedPv：命中城市级
SELECT COUNT(*) FROM visit_log log
JOIN visitor_geo geo ON geo.id = log.geo_id AND geo.geo_level = 2
WHERE log.is_page_view = 1 AND log.create_time >= ? AND log.create_time < ?

-- countryOnlyPv：只到国家级
SELECT COUNT(*) FROM visit_log log
JOIN visitor_geo geo ON geo.id = log.geo_id AND geo.geo_level = 1
WHERE log.is_page_view = 1 AND log.create_time >= ? AND log.create_time < ?

-- unknownPv：未知 / 内网 / 无效
SELECT COUNT(*) FROM visit_log log
LEFT JOIN visitor_geo geo ON geo.id = log.geo_id
WHERE log.is_page_view = 1 AND log.create_time >= ? AND log.create_time < ?
  AND (log.geo_status != 1 OR geo.id IS NULL)       -- 定位失败或压根没关联上
```

三类 JOIN 的差别是刻意的：汇总用 `LEFT JOIN` 保住全球 PV，榜单用 `JOIN` 过滤掉未定位记录，质量指标用 `LEFT JOIN` 才能把"没关联上"也算进 `unknownPv`。

#### 4.2.7 时间窗口：先解析成不可变对象

```java
public static VisitorMapWindow resolve(String periodType, String timezone,
                                       String startDate, String endDate, Instant now) {
    String normalized = isBlank(periodType) ? "all" : periodType.trim();
    if (!SUPPORTED.contains(normalized)) {
        throw new BadRequestException("暂不支持该统计周期: " + normalized);
    }
    ZoneId zoneId = resolveZoneId(timezone);
    LocalDate today = now.atZone(zoneId).toLocalDate();
    switch (normalized) {
        case "all":       return new VisitorMapWindow("all", zoneId, null, null);
        case "today":     return window(today.atStartOfDay(zoneId).toInstant(), now);
        case "yesterday": return window(today.minusDays(1).atStartOfDay(zoneId).toInstant(),
                                        today.atStartOfDay(zoneId).toInstant());
        case "last_7d":   return window(today.minusDays(6).atStartOfDay(zoneId).toInstant(), now);
        case "custom":    // 校验 yyyy-MM-dd、start <= end、跨度 <= 366 天
                          return window(customStart.atStartOfDay(zoneId).toInstant(),
                                        customEnd.plusDays(1).atStartOfDay(zoneId).toInstant());
    }
}
```

`all` 时 `start/end` 都是 `null`，Mapper 不拼时间条件，也不会出现"从 1970 年开始"的假起点。

#### 4.2.8 历史回填与聚合表时机

| 数据 | 回填方式 | 注意 |
| --- | --- | --- |
| `is_page_view` | 按 `behavior` 文案映射 | 只覆盖已知行为，未知保持 `NULL` |
| `geo_id` | 按 `ip_source` 第三段城市匹配，失败按末段国家码兜底 | 只覆盖五段格式 |
| 回填范围 | 不追求全量 | 未回填行只计入 PV 与质量指标，不进国家 / 城市榜 |

本期不建预聚合表，代价是 `all` 视图随日志增长变慢：

| 视图 | 扫描范围 | 风险 |
| --- | --- | --- |
| `today` / `yesterday` | 单日 | 低 |
| `last_7d` | 7 天 | 低 |
| `custom` | 最长 366 天 | 中，随跨度线性增长 |
| `all` | 全表 | 高 |

触发条件：`all` 视图稳定 > 300ms 时引入 `visitor_map_daily(date, geo_id, pv, uv)`；日志到百万级且索引无法覆盖时引入 `visitor_map_hourly`。聚合表只是派生数据，事实源仍是 `visit_log`。

### 4.3 前端渲染

真正画东西的只有 `renderer.js`：`GlobeCanvas.vue` 负责挂载与转发 `dataset`，`index.vue` 负责取数。这一节只讲四件事：地图怎么初始化、中国板块怎么画、光点怎么设计、后端经纬度起什么作用。

#### 4.3.1 初始化地图

```text
① 引入地图资产    import 'echarts/map/js/world' / 'china'
② 取出 GeoJSON    echarts.getMap('world') / getMap('china')
③ 解码 + 抽稀     decodeEchartsGeoJson → prepareFeatures
④ 建 Canvas        canvas + 2D context + state + frame
⑤ 启动绘制循环    resize → requestAnimationFrame(draw)
```

```js
import echarts from 'echarts'
import 'echarts/map/js/world'
import 'echarts/map/js/china'

const worldMap = echarts.getMap('world')
const worldFeatures = prepareFeatures(decodeEchartsGeoJson(worldMap.geoJson), 120)
    .filter(feature => feature.name !== 'China')
const chinaFeatures = prepareFeatures(decodeEchartsGeoJson(chinaMap.geoJson), 220)

const canvas = document.createElement('canvas')
const context = canvas.getContext('2d')
const state = {yaw: -1.83, pitch: 0.25, zoom: 1, autoRotate: true, language: 'zh', visible: true}
const frame = {centerX: 0, centerY: 0, radius: 1, yawCos: 1, yawSin: 0, pitchCos: 1, pitchSin: 0}
```

关键认知：**ECharts 只提供经纬度数据，不参与渲染**。`echarts/map/js/*` 的副作用只是把 GeoJSON 注册进内存，后面全部由 Canvas 画。

ECharts 4 的地图是 `UTF8Encoding` 压缩字符串，每个点用两个字符编码增量坐标，必须先还原：

```js
function decodeEchartsGeoJson(geoJson) {
    if (!geoJson || !geoJson.UTF8Encoding) return geoJson
    const scale = geoJson.UTF8Scale || 1024
    const decodeRing = (encoded, offset) => {
        const decoded = []
        let previousX = offset[0], previousY = offset[1]
        for (let i = 0; i < encoded.length; i += 2) {
            let x = encoded.charCodeAt(i) - 64
            let y = encoded.charCodeAt(i + 1) - 64
            x = (x >> 1) ^ (-(x & 1))     // zigzag
            y = (y >> 1) ^ (-(y & 1))
            x += previousX                // 累加增量
            y += previousY
            previousX = x
            previousY = y
            decoded.push([x / scale, y / scale])
        }
        return decoded
    }
    // Polygon / MultiPolygon 逐个环解码
    geoJson.UTF8Encoding = false          // 避免重复解码
    return geoJson
}
```

解码后按 `maxPoints` 等间隔抽稀，统一成 `{name, rings}`。抽稀只影响轮廓精度，不影响统计。`China` 从世界图层里排除，因为中国要单独逐省绘制，否则两层描边会叠在一起。

#### 4.3.2 中国板块怎么画

**这 34 个省级 feature 来自 ECharts 自带的 `china` 地图，不是另外维护的数据。** 具体来源是依赖包里的一个文件：

```text
blog-cms-ui/node_modules/echarts/map/js/china.js   ← echarts 4.9.0（package.json 锁定）
```

这个文件的唯一作用就是调用一次 `registerMap`，把一份 GeoJSON 注册进 ECharts：

```js
echarts.registerMap('china', {
  type: 'FeatureCollection',
  UTF8Encoding: true,
  features: [
    {
      type: 'Feature',
      id: '440000',
      properties: {id: '440000', cp: [113.280637, 23.125178], name: '广东', childNum: 21},
      geometry: {type: 'MultiPolygon', coordinates: [/* UTF8 压缩的经纬度环 */]}
    }
    // ... 共 34 条
  ]
})
```

实际 34 条分别是：23 个省、5 个自治区、4 个直辖市、2 个特别行政区。我用脚本从依赖包里数过一遍：

```text
台湾 / 河北 / 山西 / 内蒙古 / 辽宁 / 吉林 / 黑龙江 / 江苏 / 浙江 / 安徽
福建 / 江西 / 山东 / 河南 / 湖北 / 湖南 / 广东 / 广西 / 海南 / 四川
贵州 / 云南 / 西藏 / 陕西 / 甘肃 / 青海 / 宁夏 / 新疆 / 北京 / 天津
上海 / 重庆 / 香港 / 澳门
```

每个 feature 里有四个字段，我实际只用了其中两个：

| 字段 | 例子 | 用途 |
| --- | --- | --- |
| `properties.name` | `广东` | 与城市 `regionName` 归一化后匹配 |
| `geometry` | `MultiPolygon` / `Polygon` | 轮廓环，解码后用于绘制与命中 |
| `properties.id` | `440000` | 行政区划码，本项目未使用 |
| `properties.cp` | `[113.28, 23.12]` | 省份中心点，本项目未使用 |

`geometry.coordinates` 不是数字数组，而是 `UTF8Encoding: true` 的压缩字符串，`UTF8Scale` 缺省为 1024。所以必须先 `decodeEchartsGeoJson` 还原，再 `prepareFeatures` 抽稀成 `{name, rings}`，这一步在 4.3.1 已经说明。

顺带对比一下世界图层，`world.js` 的 217 个国家 feature 只有 `properties.name`，没有 `id` 和 `cp`：

```js
{type: 'Feature', properties: {name: 'Morocco', childNum: 1}, geometry: {...}}
```

所以前端要把国家名映射成 ISO 码，靠的是自己维护的 `COUNTRY_CODE_BY_MAP_NAME` 表，而不是 GeoJSON 自带。

每个 feature 先算一个 bbox，供悬停粗筛：

```js
const chinaFeatures = prepareFeatures(decodeEchartsGeoJson(chinaMap.geoJson), 220)
    .map(feature => {
        let minLng = Infinity, maxLng = -Infinity, minLat = Infinity, maxLat = -Infinity
        feature.rings.forEach(ring => ring.forEach(([lng, lat]) => {
            if (lng < minLng) minLng = lng
            if (lng > maxLng) maxLng = lng
            if (lat < minLat) minLat = lat
            if (lat > maxLat) maxLat = lat
        }))
        return {...feature, bbox: [minLng, minLat, maxLng, maxLat]}
    })
```

这里没用 `properties.cp`，而是自己从轮廓算 bbox：`cp` 只是一个标注点，bbox 才能用于"先粗筛再精判"的命中优化。

省份的统计值来自城市数据，而不是省份表。优先用后端返回的 `regionName`，没有就用城市坐标做多边形反查：

```js
function buildProvinceStats(list) {
    const stats = new Map()
    list.forEach(city => {
        if (city.countryCode !== 'CN') return
        const province = normalizeProvinceName(city.regionName) || provinceByPoint(city.lat, city.lng)
        if (!province) return
        const item = stats.get(province) || {cityCount: 0, maxUv: 0, pv: 0}
        item.cityCount += 1
        item.maxUv = Math.max(item.maxUv, Number(city.uv) || 0)
        item.pv += Number(city.pv) || 0
        stats.set(province, item)
    })
    return stats
}
```

名字要归一，否则 `"广东省"` 对不上 GeoJSON 的 `"广东"`：

```js
function normalizeProvinceName(name) {
    return String(name || '')
        .replace(/特别行政区$/, '')
        .replace(/(维吾尔|壮族|回族)?自治区$/, '')
        .replace(/[省市]$/, '')
        .trim()
}
```

绘制时逐省 `fill` + `stroke`，有访客的省按省内最高城市 UV 着色：

```js
function drawChina() {
    chinaFeatures.forEach(province => {
        const stats = provinceStats.get(normalizeProvinceName(province.name))
        const hovered = hoveredProvince === province.name
        context.beginPath()
        traceFeatures([province])
        context.fillStyle = stats
            ? heatColor(stats.maxUv, hovered ? 0.82 : 0.6)
            : (hovered ? 'rgba(44, 68, 170, .88)' : 'rgba(13, 20, 89, .68)')
        context.fill('evenodd')
        context.strokeStyle = hovered ? '#8df2ff' : 'rgba(56, 157, 255, .72)'
        context.lineWidth = hovered ? 1.5 : 0.8
        context.stroke()
    })
}
```

悬停命中分两级：先按城市 28px 找，找不到再把鼠标位置逆投影成经纬度，用 bbox 粗筛、`pointInRing` 精判：

```js
function provinceAt(clientX, clientY) {
    const bounds = canvas.getBoundingClientRect()
    const point = unprojectPoint(clientX - bounds.left, clientY - bounds.top)
    if (!point) return ''
    for (const province of chinaFeatures) {
        const [minLng, minLat, maxLng, maxLat] = province.bbox
        if (point.lng < minLng || point.lng > maxLng || point.lat < minLat || point.lat > maxLat) continue
        if (province.rings.some(ring => pointInRing(point.lng, point.lat, ring))) return province.name
    }
    return ''
}
```

#### 4.3.3 光点怎么设计

光点位置来自接口的 `cities[].lat/lng`，大小来自 `uv`。访客量是长尾分布，所以强度用 `log1p` 归一化，半径再按强度插值：

```js
function cityIntensity(city) {
    const value = cityVolume(city)                    // uv > 0 ? uv : pv
    if (maxUv <= 1) return value > 0 ? 0.35 : 0
    return Math.min(Math.log1p(value) / Math.log1p(maxUv), 1)
}

function cityRadius(city, pulse) {
    const base = CITY_RADIUS_MIN + cityIntensity(city) * (CITY_RADIUS_MAX - CITY_RADIUS_MIN)
    return Math.min(Math.max(base * pulse, CITY_RADIUS_MIN), CITY_RADIUS_MAX)   // 2.6 → 9
}
```

绘制时叠三层视觉：脉冲半径、发光阴影、Top5 波纹与标签：

```js
function drawCities(timestamp) {
    cities.slice(0, revealedCities).forEach(city => {
        const point = project(city.lat, city.lng)
        if (point.z <= 0) return                                  // 背面不画
        const intensity = cityIntensity(city)
        const pulse = 1 + Math.sin(timestamp / 500 + city.rank) * 0.12
        const radius = cityRadius(city, pulse)
        context.fillStyle = city.rank <= 3 ? '#c5fbff' : '#54d9e8'
        context.shadowBlur = Math.min(10 + intensity * 20, 24)
        context.shadowColor = '#55ddec'
        context.beginPath()
        context.arc(point.x, point.y, radius, 0, Math.PI * 2)
        context.fill()
        context.shadowBlur = 0
        if (city.rank <= 5) {
            context.strokeStyle = 'rgba(105, 236, 245, .4)'
            context.beginPath()
            context.arc(point.x, point.y,
                Math.min(radius * 2.2 + (timestamp / 100 % 16), CITY_RIPPLE_MAX), 0, Math.PI * 2)
            context.stroke()
        }
    })
}
```

三个上限写死，否则脉冲与波纹会溢出：半径 9px、波纹 22px、光晕 `shadowBlur` 24。`cityRadius` 必须"先算脉冲再夹紧"，顺序反了最大点会突破上限。

#### 4.3.4 后端返回的经纬度起什么作用

`cities[].lat/lng` 是城市行的 WGS84 坐标，前端只做一件事：**投影到屏幕**。整个前端没有全球城市坐标字典，坐标只来自 `visitor_geo`。

```js
function projectUnit(unit) {
    const x = unit[0] * frame.yawCos + unit[2] * frame.yawSin
    const z = unit[2] * frame.yawCos - unit[0] * frame.yawSin
    const y = unit[1] * frame.pitchCos - z * frame.pitchSin
    return {x: frame.centerX + x * frame.radius, y: frame.centerY - y * frame.radius,
            z: unit[1] * frame.pitchSin + z * frame.pitchCos}
}

function project(lat, lng) {
    const a = lat * Math.PI / 180, b = lng * Math.PI / 180, c = Math.cos(a)
    return projectUnit([c * Math.sin(b), Math.sin(a), c * Math.cos(b)])
}
```

同一份坐标在四个地方被复用：

| 用途 | 代码 | 说明 |
| --- | --- | --- |
| 画光点 | `project(city.lat, city.lng)` | 经纬度 → 屏幕 x/y，`z <= 0` 说明在背面 |
| 悬停命中 | `nearestCity` 里算屏幕距离 | 28px 内取最近城市，避免点到球外 |
| 点击聚焦 | `pointerUp` 反推 `yaw/pitch` | 点城市后把该城市转到正面 |
| 省份兜底 | `provinceByPoint(city.lat, city.lng)` | `regionName` 为空时用坐标判断属于哪个省 |

国家行没有坐标（补齐的 249 个国家坐标是 `NULL`），所以国家只做板块填充，不产生光点。这也解释了为什么海外长尾城市只有板块热度、没有城市光点：不是前端不画，而是后端没有对应的城市坐标行。

#### 4.3.5 每帧绘制与热更新

```js
function draw(timestamp = 0) {
    updateFrame(container.clientWidth, container.clientHeight)
    context.clearRect(0, 0, container.clientWidth, container.clientHeight)
    context.save()
    context.beginPath()
    context.arc(frame.centerX, frame.centerY, frame.radius, 0, Math.PI * 2)
    context.clip()                 // 陆地裁在球内
    drawGrid(); drawWorld(); drawAdm1(); drawChina(); drawCountryLabels(); drawCities(timestamp)
    context.restore()
    if (state.autoRotate && !state.dragging) state.yaw += 0.0024
    animationFrame = window.requestAnimationFrame(draw)
}
```

顺序不能调：网格在陆地之下，标签与光点在最上层，否则会被国家填充盖住。

刷新走另一条路：`setDataset` 只重算数据与 `maxUv`，`revealAll=true` 时直接把点亮进度置满，不重播动画，也不碰 `state.yaw/pitch/zoom`。

```js
return {
    setDataset(source) { applyDataset(source) },
    getState() { return {yaw: state.yaw, pitch: state.pitch, zoom: state.zoom} },
    destroy() { /* 取消 rAF、定时器、事件监听 */ }
}
```

### 4.4 前端交互与刷新

#### 4.4.1 请求锁与只刷数据不刷视角

```js
const REFRESH_INTERVAL = 10000

restartAutoRefresh() {
    this.stopAutoRefresh()
    if (this.periodType !== 'today') return          // 只有今日需要自动刷新
    this.refreshTimer = window.setInterval(() => {
        if (document.hidden || this.refreshing) return
        this.loadVisitorMap(true)
    }, REFRESH_INTERVAL)
}

async loadVisitorMap(silent = false) {
    if (this.refreshing) return
    const params = {periodType: this.periodType, timezone: 'Asia/Shanghai'}
    if (this.periodType === 'custom') {
        if (!this.customStart || !this.customEnd) return
        params.startDate = this.customStart
        params.endDate = this.customEnd
    }
    this.refreshing = true
    try {
        const response = await getGlobalVisitorMap(params)
        this.dataset = this.normalizeDataset(response.data)
        this.refreshFailed = false
    } catch (error) {
        if (this.dataset) { this.refreshFailed = true; return }   // 保留旧数据
        this.status = {tone: 'ERROR', text: 'visitor data unavailable'}
    } finally {
        this.refreshing = false
    }
}
```

- `periodType` 默认 `all`，切换范围立即查询，不等下一个轮询周期。
- `refreshing` 兼作请求锁，避免自动刷新与手动刷新并发。
- 页面隐藏时跳过轮询，恢复可见时立即补一次。
- 已有数据时刷新失败不清空页面，只标记失败。

#### 4.4.2 边界处做一次字段翻译

后端返回 `cityNameZh/cityNameEn`，渲染器历史上用 `city/cityEn`：

```js
normalizeDataset(source) {
    return {
        ...source,
        countries: source.countries || [],
        cities: (source.cities || []).map((city, index) => ({
            ...city,
            city: city.cityNameZh || city.city,
            cityEn: city.cityNameEn || city.cityEn,
            rank: city.rank || index + 1
        }))
    }
}
```

后端已按 UV 降序返回，`index + 1` 就是名次，`rank` 决定标签与颜色优先级。

#### 4.4.3 热更新：状态要抽出来

旧实现把 `cities/countries/maxUv` 当闭包常量，数据变了只能销毁重建，副作用是视角重置、点亮动画重播。抽成可变状态后，刷新只走 `applyDataset`：

```js
return {
    setDataset(source) { applyDataset(source) },
    setLanguage(language) { state.language = language },
    setAutoRotate(enabled) { state.autoRotate = enabled },
    getState() { return {yaw: state.yaw, pitch: state.pitch, zoom: state.zoom} },
    reset() { state.yaw = -1.83; state.pitch = 0.25; state.zoom = 1 },
    destroy() { /* 取消 rAF、定时器、事件监听 */ }
}
```

刷新只更新数据与 `maxUv`，`state.yaw/pitch/zoom` 完全不动，`requestAnimationFrame` 循环继续跑。用户看到的是"数字变了"，不是"页面重开了"。

#### 4.4.4 销毁必须彻底

```js
destroy() {
    destroyed = true
    window.cancelAnimationFrame(animationFrame)
    timers.forEach(timer => { window.clearTimeout(timer); window.clearInterval(timer) })
    window.removeEventListener('resize', resize)
    document.removeEventListener('visibilitychange', visibilityChange)
    canvas.removeEventListener('pointerdown', pointerDown)
    canvas.removeEventListener('pointermove', pointerMove)
    canvas.removeEventListener('pointerup', pointerUp)
    canvas.removeEventListener('wheel', wheel)
    if (canvas.parentNode === container) container.removeChild(canvas)
}
```

路由切换不销毁渲染器，会留下一个永远在跑的 `requestAnimationFrame`。单次看不出来，来回切几次页面就开始掉帧。

#### 4.4.5 榜单维度切换

页面后来加了"国家 / 城市"维度切换，只改前端，不改接口：

```js
chinaRanking() {
    if (this.rankMode === 'country') {
        const china = (this.dataset.countries || []).find(item => item.countryCode === 'CN')
        return [{key: 'CN', name: this.displayCountryName('CN'), uv: china ? china.uv : 0}]
    }
    return this.rankingItems(this.dataset.cities.filter(city => city.countryCode === 'CN'))
}
```

| 方案 | 优点 | 缺点 | 结论 |
| --- | --- | --- | --- |
| 后端加参数 | 返回数据更小 | 多一套查询分支、多一次请求 | 不选 |
| 前端切换 | 复用同一份数据、切换零请求、视角不重置 | 传输略多 | **选用** |

`countries[]` 本来就要返回给地球做国家热度着色，榜单复用它是零成本的。

### 4.5 验证

#### 4.5.1 单元测试

| 测试 | 覆盖点 |
| --- | --- |
| `VisitBehaviorTest` | 枚举 PV 口径 |
| `VisitLogServiceImplGeoTest` | 城市命中、国家兜底、内网、空地域 |
| `DatabaseGlobalVisitorMapProviderTest` | 汇总/国家/城市/质量指标映射 |
| `GlobalVisitorMapServiceImplTest` | 默认 `all`、时区校验、非法周期、自然日窗口 |
| `GlobalVisitorMapAdminControllerTest` | 统一 `Result` 返回、参数透传 |
| `GlobalVisitorMapTrafficIntegrationTest` | 模拟访问 → 落库 → 聚合全链路 |

#### 4.5.2 真实 HTTP 模拟

```bash
BASE='http://127.0.0.1:8090'

# 首次请求：不带 identification，服务端签发
ID=$(curl -sS -o /dev/null -D - \
  -H 'X-Real-IP: 119.137.52.1' \
  -H 'User-Agent: Mozilla/5.0 ... Chrome/120' \
  "$BASE/blogs?pageNum=1" | tr -d '\r' | awk 'tolower($1)=="identification:"{print $2}')

# 同一访客再访问两次：PV +2，UV 仍为 1
curl -sS -o /dev/null -H "identification: $ID" -H 'X-Real-IP: 119.137.52.1' "$BASE/blogs?pageNum=2"
curl -sS -o /dev/null -H "identification: $ID" -H 'X-Real-IP: 119.137.52.1' "$BASE/archives"
```

16 个访客、28 次页面请求，覆盖中国与海外，另加 1 次非 PV 的"点击友链"：

| 维度 | 结果 |
| --- | --- |
| 落库行数 | 29 行（PV 28 + 非 PV 1） |
| 全球 | PV 28 / UV 17 |
| 中国 | PV 14 / UV 9 |
| 海外 | US 3/1、GB 3/2、DE 2/1、JP 2/1、AU 1/1、ID 1/1、SG 1/1、TH 1/1 |
| 质量 | `resolvedPv=24`、`countryOnlyPv=4`、`unknownPv=0` |

关键断言：

- 同一访客先深圳后伦敦：全球 UV 仍为 1，但深圳与伦敦各计 1 个城市 UV。
- 同一 IP + 同一 UA、同一小时内两次不带 `identification`，返回同一个 UUID。
- 点击友链 `is_page_view=0`，不进 PV/UV，但仍保留访问明细。
- `resolvedPv + countryOnlyPv + unknownPv = 28 = PV`。

#### 4.5.3 前端验证看行为，不看截图

| 步骤 | 期望结果 |
| --- | --- |
| 首屏（默认 `all`） | 显示全量指标，接口调用 1 次 |
| 切到"今日" | 指标变为当日口径，接口调用 +1 |
| 停留 11 秒 | 接口再调用 1 次（仅 `today` 自动刷新） |
| 点击刷新按钮 | 接口立即调用 1 次 |
| 刷新期间 | Canvas 持续渲染，视角不变，控制台 0 错误 |
| 构建 | `npm run build` 通过 |

截图只能证明某一刻好看，证明不了刷新不打断。我用路由拦截统计请求次数，再断言 `getState()` 的 `yaw/pitch/zoom` 在刷新前后不变。

### 4.6 已知限制

| 限制 | 影响 | 后续方向 |
| --- | --- | --- |
| 海外城市未建全量字典 | 海外访客只到国家，无城市热点 | 按人口 Top N 分批补 |
| 中国城市覆盖 361 个 | 县级市等无坐标城市降级到国家 | 按需补坐标 |
| 英文城市名历史回填未覆盖 | 旧数据城市命中率低 | 回填时同时匹配 `city_name_en` |
| UV 依赖前端携带 `identification` | 跨小时可能换 UUID | 延长有效期或使用持久 Cookie |
| 时区固定传 `Asia/Shanghai` | 海外站长看到的"今日"仍是北京时间 | 按浏览器时区传参 |
| 无预聚合 | 日志量大时 `all` 变慢 | 监控确认瓶颈后再评估 |
| ADM1 仅 10 国 | 其他国家放大后无省州边界 | 按需扩展 `adm1-mvp.js` |

后续任何优化（缓存、聚合、全量字典、历史回填）都必须保持三件事：接口契约不变、统计口径不漂移、刷新不打断浏览。

## 五、复盘

### 5.1 完整链路

一条请求到最后地球上的一个光点，中间只有这些环节：

```text
① 浏览器请求 /blogs
      ↓
② VisitLogAspect 拦截
      · 取 identification，没有就签发 UUID（小时时间戳 + IP + UA）
      · 记 URI、IP、UA、耗时
      · 按 @VisitLogger 写入 is_page_view
      ↓
③ @Async 异步落库 VisitLogServiceImpl
      · ip2region 解析 IP → 五段地域文本
      · 匹配 visitor_geo：城市命中 → 国家兜底 → 内网/未知
      · yauaa 解析 OS / 浏览器
      · INSERT visit_log(geo_id, geo_status, is_page_view)
      ↓
④ GET /admin/visitor-map/global
      · VisitorMapWindow 解析时间窗（all / today / yesterday / last_7d / custom）
      · 6 条 SQL 聚合 summary / countries / cities / quality
      ↓
⑤ 前端 index.vue
      · 请求锁 + 今日 10 秒轮询
      · normalizeDataset 补齐 city / cityEn / rank
      ↓
⑥ renderer.js
      · echarts.getMap 取 GeoJSON → 解码 → 抽稀
      · 每帧投影 → 国家板块 / 中国省份 / 城市光点
      · 刷新只换数据，不动视角
```

数据侧只有一张事实表 `visit_log` 和一张维表 `visitor_geo`，没有中间表；前端只有一个 canvas 和一个 `requestAnimationFrame` 循环。整条链路没有隐藏环节。

### 5.2 技术栈与开源库

| 用途 | 选择 | 为什么选它 |
| --- | --- | --- |
| IP 定位 | [ip2region](https://github.com/lionsoul2014/ip2region) 3.3.7 双栈 XDB | 免费、离线、同时支持 IPv4/IPv6，不用外发 IP |
| UA 解析 | [yauaa](https://github.com/nielsbasjes/yauaa) 7.11.0 | 规则库成熟，一个 UA 直接拿到 OS 和浏览器版本 |
| 世界/中国底图 | ECharts 4.9.0 自带 `world` / `china` | 依赖里已有，离线可用，不用另找 GeoJSON |
| 海外省州 | [geoBoundaries](https://github.com/wmgeolab/geoBoundaries) gbOpen ADM1 | 有 ADM1 层级、CC BY 4.0 许可清晰，自建 10 国 226 个行政区约 309KB |
| 渲染 | 浏览器原生 Canvas 2D | 零新增依赖，背面剔除、标签、命中全可控 |
| 后端框架 | Spring Boot + MyBatis | 项目已有，`@Async`、AOP、事务都是现成的 |

**唯一没选的大件是 Three.js / Cesium。** 我要的是"地球在转、光点在球面"的观感，不是真实三维渲染；几百个点用 2D 画布自绘反而更稳、更轻。

### 5.3 这次学到的

1. **先口径，后接口，最后视觉。** 顺序反了，图越酷，错得越隐蔽。
2. **原型用来排风险，不用来上线。** Demo 验证"能不能"，工程解决"稳不稳"，闭包里写死的状态是刷新出问题的根源。
3. **键选错比字段少更致命。** 城市名当主键，同名会串、改名会漂；换成稳定 `geo_key` + `geo_id` 之后，改名和补坐标都只是改维表。
4. **空白必须能解释。** 没光点可能是没人、没解析、没收录，所以要有 `geo_status` 和质量三指标。
5. **AI 适合做可验证的小闭环。** 需求拆解、口径判断、验收标准必须自己拍板。

### 5.4 判断标准

如果只能留一句话，我留这句：

> 页面上的每一个数字，都应该能用一条 SQL 复算出来；地图上的每一个空白，都应该能说清是"没人""没解析"还是"没收录"。

做到这两点，这张地图才不只是后台里的一个特效，而是一个真的能用来观察自己站点的工具。

---

# 参考资料

[1]. [ip2region 开源项目（IP 地域查询库与 XDB 数据）](https://github.com/lionsoul2014/ip2region)

[2]. [ip2region 官方 IPv4 / IPv6 数据目录](https://github.com/lionsoul2014/ip2region/tree/master/data)

[3]. [geoBoundaries 全球行政边界数据](https://github.com/wmgeolab/geoBoundaries)

[4]. [geoBoundaries API 文档](https://www.geoboundaries.org/api.html)

[5]. [Natural Earth 全球边界数据](https://www.naturalearthdata.com/)

[6]. [Natural Earth Admin 1 州与省数据说明](https://www.naturalearthdata.com/downloads/10m-cultural-vectors/10m-admin-1-states-provinces/)

[7]. [Apache ECharts 官网](https://echarts.apache.org/)

[8]. [Apache ECharts：Geo 坐标系与地图系列](https://echarts.apache.org/handbook/en/how-to/component-types/geo/svg-base-map/)

[9]. [Apache ECharts：Canvas 与 SVG 渲染器选择](https://echarts.apache.org/handbook/en/best-practices/canvas-vs-svg/)

[10]. [ISO 3166-1 国家代码标准](https://www.iso.org/iso-3166-country-codes.html)

[11]. [Playwright 官方文档（无头浏览器与 CDP 自动化）](https://playwright.dev/docs/intro)

整理者:长路 创建时间:2026.10.18 更新时间:2026.10.18
