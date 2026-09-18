# BlogLoom 移动端适配与维护规范

> 迭代版本：`release_1.0.x`
>
> 文档状态：初步规范，后续实现与评审依据
>
> 需求范围：前台（blog-view-ui）与后台（blog-cms-ui）移动端样式的适配标准、文件组织与维护流程

## 1. 背景与目标

当前项目的响应式样式**散落在各个组件的 `<style>` 中**，与桌面端样式混写，存在以下问题：

1. 断点不统一：现有媒体查询出现 `480 / 560 / 640 / 768 / 900 / 1180` 等多个阈值，难以预期。
2. 移动端规则与桌面端规则写在同一个文件、同一段样式里，修改移动端时容易误伤桌面端。
3. 缺少统一的适配清单，新增组件时是否要适配移动端、适配到什么程度，没有依据。
4. PC 专属元素与移动端元素依靠零散的工具类（如 `m-mobile-hide`）控制，不够体系化。

本文档确立一条核心原则：

> **移动端样式必须与桌面端样式解耦，放在独立样式文件中单独维护。**

目标是让"移动端适配"成为一件**可预期、可复用、可独立演进**的常规工作，而不是散落在组件内部的临时补丁。

## 2. 设计原则

| 原则 | 说明 |
| --- | --- |
| 解耦 | 移动端样式不写在组件桌面端样式内部，独立成文件 |
| 单一来源 | 断点、间距、字号等通过统一变量/mixin 提供，禁止在业务样式里硬编码魔法值 |
| 就近维护 | 每个组件的移动端样式文件与组件同名同目录或按约定集中存放，改一处即可 |
| 渐进增强 | 默认写成自适应（流式）布局，移动端只做必要的覆盖 |
| 可验收 | 每个页面/组件有明确的移动端验收清单与目标宽度 |

## 3. 断点规范

统一使用以下断点，**不再新增其他阈值**：

| 档位 | 范围 | 变量/mixin | 典型设备 |
| --- | --- | --- | --- |
| 桌面 | `> 1024px` | 默认（无需媒体查询） | PC |
| 平板 | `641px ~ 1024px` | `@include tablet` | iPad 竖屏 |
| 移动 | `≤ 768px` | `@include mobile` | 手机横竖屏、大屏手机 |
| 小屏 | `≤ 480px` | `@include small` | 小屏手机 |

> 说明：`768px` 为主移动断点；`641~1024px` 为过渡平板档；`≤480px` 用于小屏精简。
> 现有 `900 / 1180 / 640 / 560 / 767 / 769` 等阈值在迁移时统一归并到上表。

断点统一由 `styles/variables.scss` + `styles/mixins.scss` 提供：

```scss
// styles/variables.scss
$bp-tablet-max: 1024px;
$bp-mobile-max: 768px;
$bp-small-max: 480px;

// styles/mixins.scss
@mixin mobile { @media (max-width: 768px) { @content; } }
@mixin small  { @media (max-width: 480px) { @content; } }
@mixin tablet { @media (min-width: 641px) and (max-width: 1024px) { @content; } }
```

## 4. 目录与文件组织规范

移动端样式**独立目录、独立文件**存放，推荐结构如下：

```text
blog-view-ui/src/
  styles/
    variables.scss              # 断点/颜色/间距/字号变量
    mixins.scss                 # 响应式 mixin
    index.scss                  # 全局样式入口（被 main.js 引入）
    mobile/
      _index.scss               # 移动端样式聚合入口
      components/
        footer.mobile.scss      # 与 components/index/Footer.vue 对应
        blog-item.mobile.scss
        nav.mobile.scss
        introduction.mobile.scss
        tags.mobile.scss
        categories.mobile.scss
      pages/
        home.mobile.scss
        blog.mobile.scss
        archives.mobile.scss
        moments.mobile.scss
        friends.mobile.scss
        about.mobile.scss
```

命名约定：

- 移动端文件统一以 `.mobile.scss` 结尾，**与组件/页面同名**。
- 一个组件对应一个移动端文件；禁止把多个组件的移动端样式塞进同一个文件。
- 后台 `blog-cms-ui` 同理，放在 `src/styles/mobile/` 下。

## 5. 组件接入规范

### 5.1 推荐方式：独立 scoped 样式文件

组件保留桌面端样式，移动端样式**单独成文件**，通过 SFC 的 `src` 引入并保持 scoped：

```vue
<!-- components/index/Footer.vue -->
<template>...</template>

<script>...</script>

<!-- 桌面端样式 -->
<style scoped>
  .site-footer { padding: 48px 0 24px; }
  /* ... 仅桌面端 ... */
</style>

<!-- 移动端样式（独立文件，单独维护） -->
<style scoped src="@/styles/mobile/components/footer.mobile.scss" lang="scss"></style>
```

```scss
/* styles/mobile/components/footer.mobile.scss */
@import "@/styles/mixins.scss";

@include mobile {
  .site-footer { padding: 32px 0 20px; }
  .footer-main { grid-template-columns: 1fr; gap: 24px; }
}

@include small {
  .footer-bottom { flex-direction: column; align-items: flex-start; }
}
```

> 该方式的优点：文件物理隔离；`scoped` 仍然生效，选择器与组件一一对应；修改移动端只动 mobile 文件。

### 5.2 备选方式：全局移动端样式表

对于跨组件的通用移动端规则（如全局字号、容器内边距、通用工具类），可放入 `styles/mobile/_index.scss` 全局引入。**仅限真正全局的规则**，组件级规则仍走 5.1。

### 5.3 禁止事项

- ❌ 在组件桌面端 `<style>` 内直接写 `@media (max-width: ...)`。
- ❌ 在桌面试样里用移动端类名/反逻辑覆盖。
- ❌ 硬编码断点数值 `@media (max-width: 767px)` 等。
- ❌ 新增组件时默认不评估移动端。

## 6. 适配规则清单

移动端实现时逐条对照：

| 类别 | 规则 |
| --- | --- |
| 布局 | 单列优先；网格在移动端收敛为 1 列；避免固定宽度，使用 `flex` / `grid` + `min-width:0` |
| 容器 | 页面左右内边距统一（建议 `16px`）；内容最大宽度自适应 |
| 字体 | 正文字号不低于 `13px`；标题层级在移动端整体下调 1~2px |
| 触摸 | 可点击目标不小于 `44×44px`；链接/按钮间距足够，避免误触 |
| 图片 | 使用 `object-fit: cover`；设置 `max-width:100%`；避免固定高度导致拉伸 |
| 溢出 | 长文本 `word-break` / `overflow-wrap:anywhere`；横向滚动需显式允许 |
| 导航 | 移动端折叠菜单；PC 专属区块用 `m-mobile-hide` 隐藏 |
| 安全区 | 全面屏底部使用 `env(safe-area-inset-bottom)` |
| 视口 | 慎用 `100vh`（移动端地址栏问题），优先 `100dvh` 或 `min-height` 兜底 |
| 表格/代码 | 宽表格、代码块允许横向滚动，不撑破页面 |
| 滚动 | 卡片内滚动区设置 `overscroll-behavior: contain` |
| 性能 | 图片懒加载（`v-lazy`）；移动端减少大图与重阴影 |

## 7. 维护流程

1. **新增/修改组件时**：先确认桌面端样式，再评估移动端；移动端规则一律写入对应 `.mobile.scss`。
2. **只改移动端**：只需打开对应 mobile 文件，不触碰桌面端样式。
3. **新增断点/mixin**：先在 `variables.scss` / `mixins.scss` 登记，再使用。
4. **提交前自检**：按第 8 节验收清单在至少 3 个宽度（`375 / 768 / 1280`）下核对。
5. **评审要点**：是否存在写死的媒体查询、是否误改桌面端、是否遗漏移动端文件。

## 8. 验收清单

- [ ] 目标页面在 `375 / 390 / 414 / 768 / 1024 / 1280` 宽度下无横向滚动条。
- [ ] 无内容重叠、溢出、被裁切。
- [ ] 文字可读，字号与行距合理。
- [ ] 可点击元素触摸区域足够，点击不误触相邻元素。
- [ ] 图片不变形，加载不抖动。
- [ ] 移动端隐藏的 PC 元素不占位。
- [ ] 断点统一为本文档规定的阈值。
- [ ] 移动端样式全部位于独立 `.mobile.scss` 文件中。

## 9. 现状与迁移建议

当前散落的媒体查询（供迁移参考）：

| 断点 | 出现处（示例） |
| --- | --- |
| `max-width: 768px` | `BlogItem.vue`、`Home.vue`、`Nav.vue` |
| `max-width: 480px` | `BlogItem.vue` |
| `max-width: 900px` / `560px` | `Footer.vue` |
| `max-width: 767px` / `min-width: 769px` | 其他组件 |
| `max-width: 640px` / `1180px` | 其他组件 |

迁移步骤建议：

1. 建立 `styles/variables.scss`、`styles/mixins.scss`、`styles/mobile/` 目录骨架。
2. 逐组件把移动端相关的 `@media` 规则抽取到对应 `.mobile.scss`，删除桌面端样式内的媒体查询。
3. 把非标准断点（`560 / 640 / 767 / 900 / 1180`）归并到 `480 / 768 / 1024`。
4. 每迁移一个页面，按第 8 节验收。

## 10. 首期不做

- 不引入 UI 框架级响应式方案（如 Tailwind/自研栅格系统）。
- 不做独立移动端站点或 SSR 分流。
- 不引入 PostCSS 自动 px→vw 转换（后续按需再评估）。
- 后台 `blog-cms-ui` 的移动端适配在规范落地后单独排期。
