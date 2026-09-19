# BlogLoom 首页首图滚动视差与固定遮罩实现方案

> 迭代版本：`release_1.0.x`  
> 文档状态：已落地  
> 涉及范围：`blog-view-ui/src/views/Index.vue`、`blog-view-ui/src/components/index/Header.vue`、`blog-view-ui/src/settings.js`

## 一、背景

### 1.1 改造背景

BlogLoom 首页原本由 `Header.vue` 渲染 Banner，并监听鼠标位置修改 CSS 变量，再通过 `translateX` 实现水平视差。这种实现会让整张图随鼠标晃动，且 Banner 会随 Header 一起离开视口，不能形成“正文在固定首图上向上覆盖”的空间感。

本次改造参考沉浸式博客首页的分层效果，将首图、首屏文案和正文内容拆成独立层级。

### 1.2 交互目标

- 进入首页时，Banner 完整占满首屏，文案、导航和波浪悬浮其上；
- 向下滚动时，首图固定在视口，正文从底部向上覆盖；
- 正文区域携带稳定的浅色半透明遮罩，既提升可读性，又保留图片细节；
- 正文完全进入后，遮罩浓度保持不变，不继续变白；
- 正文遮罩与 Footer 无缝衔接，不在底部泄露一条未遮罩背景。

### 1.3 效果页

#### 正文完全进入

![BlogLoom 首页正文完全进入效果](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202609191728850.png)

首图固定在视口下方，正文与侧边栏完整进入，`0.42` 浅色遮罩保持稳定，背景细节仍然可见。

#### 向下滚动一半

![BlogLoom 首页向下滚动一半效果](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202609191728502.png)

正文从视口底部向上覆盖，遮罩边界与 `.main` 顶边一致，上方未覆盖区域仍展示原始首图。

#### 进入首页

![BlogLoom 首页首屏效果](https://pictured-bed.oss-cn-beijing.aliyuncs.com/img/2024/202609191728179.png)

首屏完整展示 Banner，正文遮罩尚未进入可视区，Hero 局部暗色渐变用于保证白色文案的对比度。

## 二、分析调研

### 2.1 固定背景方案对比

| 方案 | 优点 | 问题 | 结论 |
| --- | --- | --- | --- |
| Header 内绝对定位背景 | 结构直观 | Header 离开后背景一起消失 | 不采用 |
| `.site` + `background-attachment: fixed` | 代码较少 | 移动端兼容不稳定，背景与容器强绑定 | 不采用 |
| 固定元素 + `z-index: -1` | 背景独立 | 容易被 `#app` 底色覆盖 | 不采用 |
| 独立固定背景层 + 局部遮罩 | 层级清晰、无滚动 JS、移动端可独立关闭 | 需建立局部堆叠上下文 | **采用** |

### 2.2 全屏遮罩问题

早期方案使用全屏 `position: fixed` 白色遮罩，根据滚动距离将透明度从 `0` 提高到 `0.85`。这会导致用户刚开始滚动，整个视口就开始变白；最终只保留约 `15%` 的背景视觉信息，首图细节几乎消失。

最终将遮罩收敛到 `.home-site .main`，让遮罩的几何边界与正文边界一致，而不是根据滚动数值推导。

### 2.3 遮罩透明度调研

| 透明度 | 视觉表现 | 结论 |
| --- | --- | --- |
| `0.25` 以下 | 背景鲜明，卡片外的内容易受干扰 | 偏弱 |
| `0.35 ~ 0.48` | 背景细节与正文层次较平衡 | **推荐** |
| `0.60 ~ 0.75` | 阅读区更亮，首图色彩明显衰减 | 偏重 |
| `0.85` | 接近不透明底色，背景细节丢失 | 不采用 |

最终选择 `rgba(239, 239, 239, .42)`，颜色与项目原 `#app` 背景色保持一致。

### 2.4 兼容性与性能

- 删除 `mousemove` 监听，不再频繁修改 CSS 变量与背景位移；
- 不监听 `scroll` 计算透明度，滚动由浏览器布局与合成完成；
- 通过 `isolation: isolate` 建立局部堆叠上下文，避免负 `z-index`；
- 固定背景层使用 `m-mobile-hide`，移动端不启用大图固定效果；
- 背景层设置 `pointer-events: none`，不拦截链接、点击与滚动。

## 三、设计落地实现步骤

### 3.1 统一 Banner 配置

Banner 入口收敛在 `blog-view-ui/src/settings.js`：

```js
export default {
	banner: '/img/banner/home-banner.png',
	malfunctionText: 'Changlu\'s Blog',
	heroEyebrow: 'JAVA BACKEND · AI AGENT · OPEN SOURCE',
	heroDescription: '每个人都是独一无二的，把握好自己的节奏，跟着自己的心走。'
}
```

背景资源位于 `blog-view-ui/public/img/banner/home-banner.png`。

### 3.2 建立固定背景层

`Index.vue` 根容器增加首页独立背景层：

```html
<div class="site" :class="{'home-site': $route.name === 'home', 'blog-detail-site': $route.name === 'blog'}">
	<div v-if="$route.name === 'home'" class="home-hero-background m-mobile-hide"></div>
	<Nav :blogName="siteInfo.blogName"/>
	<div class="m-mobile-hide"><Header v-if="$route.name === 'home'"/></div>
	<div class="main">...</div>
	<Footer .../>
</div>
```

```css
.site {
	position: relative;
	isolation: isolate;
	display: flex;
	min-height: 100vh;
	flex-direction: column;
}

.home-hero-background {
	position: fixed;
	inset: 0;
	z-index: 0;
	background: url('/img/banner/home-banner.png') center center / cover no-repeat;
	pointer-events: none;
}
```

### 3.3 建立内容层与正文遮罩

```css
.site > .m-mobile-hide:not(.home-hero-background),
.site > .main,
.site > footer {
	position: relative;
	z-index: 1;
}

.home-site .main {
	margin-top: 0;
	padding-top: 40px;
	background: rgba(239, 239, 239, .42);
}
```

首页将 `.main` 原来的 `40px` 外边距换成等值内边距，保持内容间距不变，并让遮罩从正文顶边开始连续绘制。

### 3.4 修复 Footer 衔接缝隙

Footer 组件默认存在 `margin-top: 40px`。margin 属于元素外部，不会绘制 `.main` 遮罩或 Footer 背景，因此在正文末尾和 Footer 之间会泄露一条首图。

只在首页取消该外边距：

```css
.home-site > footer {
	margin-top: 0;
}
```

该规则不会影响归档、文章详情等其他路由。

### 3.5 精简 Header 并完成验收

`Header.vue` 删除 `.view/.bg`、`--percentage`、`mousemove`、`mouseenter`、`mouseout` 和 `.moving` 样式，只保留 Hero 文案、局部暗色遮罩、波浪和下滚按钮。

落地后执行：

```bash
cd blog-view-ui
npm run build
```

手工验收项：

1. 打开 `/home`，确认首屏完整、背景无水平晃动；
2. 滚动半屏，确认正文与遮罩同步向上覆盖；
3. 滚动一屏，确认首图保持固定、遮罩不继续变深；
4. 滚动到页面底部，确认正文遮罩与 Footer 无透明缝隙；
5. 验证移动端和非首页路由不受影响。

## 四、原理介绍

### 4.1 固定背景的视差原理

`position: fixed` 使背景相对视口定位，页面滚动不会改变它的视口位置。Header、Main 和 Footer 仍处于文档流中。上层内容移动、下层背景不动，人眼会感知到深度差，因此无需 JavaScript 计算位移也能产生稳定视差。

### 4.2 堆叠上下文原理

`isolation: isolate` 让 `.site` 建立独立堆叠上下文。背景层使用 `z-index: 0`，内容层使用 `z-index: 1`，层级只在 `.site` 内比较，不会因负层级落到 `#app` 背景之后。

`:not(.home-hero-background)` 用于避免背景层因同时带有 `.m-mobile-hide` 而被内容层选择器误伤。

### 4.3 半透明遮罩的颜色合成

`rgba(239, 239, 239, .42)` 是在首图上方合成浅灰色，而不是降低首图元素自身的透明度：

```text
结果色 = 遮罩色 × 0.42 + 背景色 × 0.58
```

背景仍保留 `58%` 的色彩与细节贡献，再由高不透明度的文章卡片提供第二层阅读保障。

### 4.4 局部遮罩与外边距

CSS `background` 只绘制在元素的 content、padding 和 border 区域，不绘制在 margin 区域。因此：

- `.main` 顶部间距要用 `padding-top`，否则遮罩顶部会断开；
- Footer 顶部不能留外边距，否则在 Main 与 Footer 之间会露出固定背景；
- Footer 自身是不透明深色背景，与 Main 直接衔接即可完成页面收尾。

### 4.5 CSS 驱动的取舍

| 对比项 | 滚动 JavaScript 方案 | 当前 CSS 分层方案 |
| --- | --- | --- |
| 滚动监听 | 需要 | 不需要 |
| 中间状态 | 需要 `heroProgress` | 无 |
| 遮罩边界 | 根据滚动距离推导 | 由 `.main` 几何边界决定 |
| 透明度 | 随滚动连续变化 | 固定 `0.42` |
| 维护成本 | 需注册、节流和销毁监听 | 由布局与合成完成 |

当前需求只需要“背景不动、内容上浮、遮罩稳定”，CSS 分层比连续滚动计算更符合交互语义。
