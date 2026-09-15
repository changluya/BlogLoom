<template>
	<!--文章目录-->
	<div class="ui segments m-toc toc-wrapper m-box">
		<div class="ui secondary segment toc-header">
			<div class="toc-title">
				<i class="list ul icon"></i>本文目录
			</div>
			<button type="button" class="toc-toggle" :class="{'is-collapsed': collapsed}" @click="toggleToc" :aria-expanded="String(!collapsed)" :title="collapsed ? '展开目录' : '收拢目录'">
				<i class="angle up icon"></i>
			</button>
		</div>
		<div class="ui yellow segment toc-content" :class="{'is-panel-collapsed': collapsed}">
			<div class="js-toc"></div>
		</div>
	</div>
</template>

<script>
	import {mapState} from 'vuex'

	export default {
		name: "Tocbot",
		data() {
			return {
				collapsed: false
			}
		},
		computed: {
			...mapState(['isBlogRenderComplete'])
		},
		mounted() {
			// 有可能组件创建比较慢，文章渲染已经完成，watch的时候，isBlogRenderComplete已经是true，监听不到 isBlogRenderComplete 的改变，也就不会执行watch中的方法
			// 就需要在 mounted 中init
			if (window.document.querySelector('.js-toc-content')) {
				this.initTocbot()
			}
		},
		watch: {
			//文章渲染完成时，生成目录
			isBlogRenderComplete() {
				if (this.isBlogRenderComplete) {
					this.initTocbot()
				}
			}
		},
		methods: {
			toggleToc() {
				this.collapsed = !this.collapsed
			},
			initTocbot() {
				tocbot.init({
					// Where to render the table of contents.
					tocSelector: '.js-toc',
					// Where to grab the headings to build the table of contents.
					contentSelector: '.js-toc-content',
					// Which headings to grab inside of the contentSelector element.
					headingSelector: 'h1,h2,h3,h4',
					// 展开层级统一交给右侧按钮控制，避免 Tocbot 的自动折叠状态相互覆盖。
					collapseDepth: 6,
					// Element to add the positionFixedClass to.
					positionFixedSelector: '.m-toc',
					// Smooth scrolling enabled.
					scrollSmooth: true,
					// Smooth scroll duration.
					scrollSmoothDuration: 420,
					//到顶部导航条的距离
					scrollSmoothOffset: -55,
					// Headings offset between the headings and the top of the document (this is meant for minor adjustments).
					// Can also be used to account for scroll height discrepancies from the use of css scroll-padding-top
					headingsOffset: -18
				})
			}
		}
	}
</script>

<style>
	.m-toc {
		z-index: 10 !important;
		border: 1px solid rgba(148, 163, 184, .2) !important;
		border-radius: 12px !important;
		box-shadow: 0 10px 28px rgba(30, 41, 59, .06) !important;
		overflow: hidden;
	}

	.m-toc > .secondary.segment {
		border-bottom: 1px solid #e7edf3 !important;
		background: #f8fafc !important;
		color: #46566b !important;
		font-weight: 600;
	}

	.m-toc .toc-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 11px 13px !important;
	}

	.m-toc .toc-title {
		display: flex;
		align-items: center;
	}

	.m-toc .toc-toggle {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 28px;
		height: 28px;
		padding: 0;
		border: 0;
		border-radius: 7px;
		background: transparent;
		color: #77869a;
		cursor: pointer;
		transition: color .2s ease, background .2s ease;
	}

	.m-toc .toc-toggle:hover {
		background: rgba(14, 165, 183, .1);
		color: #087f8c;
	}

	.m-toc .toc-toggle i {
		margin: 0 !important;
		transition: transform .25s ease;
	}

	.m-toc .toc-toggle.is-collapsed i {
		transform: rotate(180deg);
	}

	.m-toc > .yellow.segment {
		border-top: 0 !important;
		background: rgba(255, 255, 255, .98) !important;
	}

	.m-toc .toc-content {
		overflow-x: hidden;
		overflow-y: auto;
		box-sizing: border-box;
		height: min(440px, calc(100vh - 170px));
		min-height: 240px;
		padding-right: 8px !important;
		padding-top: 12px !important;
		padding-bottom: 12px !important;
		scroll-behavior: smooth;
		overscroll-behavior: contain;
		-webkit-overflow-scrolling: touch;
		opacity: 1;
		transition: height .3s ease, min-height .3s ease, padding .3s ease, opacity .2s ease, border-width .3s ease;
	}

	.m-toc .toc-content.is-panel-collapsed {
		height: 0;
		min-height: 0;
		padding-top: 0 !important;
		padding-bottom: 0 !important;
		border-top-width: 0 !important;
		opacity: 0;
	}

	.m-toc .toc-content::-webkit-scrollbar {
		width: 5px;
	}

	.m-toc .toc-content::-webkit-scrollbar-thumb {
		border-radius: 999px;
		background: #cbd5e1;
	}

	.m-toc .toc-content::-webkit-scrollbar-thumb:hover {
		background: #94a3b8;
	}

	.m-toc .toc-content::-webkit-scrollbar-track {
		background: transparent;
	}

	.m-toc .js-toc > ul {
		overflow: hidden;
		position: relative
	}

	.m-toc .js-toc > ul li {
		list-style: none
	}

	.m-toc .toc-list {
		list-style-type: none;
		margin: 0;
		padding-left: 10px
	}

	.m-toc .toc-list .toc-list {
		margin: 3px 0 3px 8px;
		padding-left: 10px;
		border-left: 1px solid #e5ebf1;
	}

	.m-toc .toc-list .toc-list .toc-link {
		font-size: 12px;
	}

	.m-toc .toc-list li a {
		display: block;
		padding: 5px 5px 5px 9px;
		border-radius: 6px;
		color: #66758a;
		font-size: 13px;
		font-weight: 400;
		line-height: 1.45;
		transition: color .2s ease, background .2s ease;
	}

	.m-toc .toc-list li a:hover {
		background: rgba(14, 165, 183, .07);
		color: #087f8c;
	}

	.m-toc a.toc-link {
		color: currentColor;
		height: 100%
	}

	.m-toc .is-collapsible {
		max-height: none !important;
		overflow: visible;
		transition: none
	}

	.m-toc .toc-list .is-collapsed {
		max-height: none !important
	}

	.m-toc.is-position-fixed {
		position: sticky !important;
		top: 60px
	}

	.m-toc .is-active-link {
		background: rgba(14, 165, 183, .09);
		font-weight: 600;
		color: #087f8c !important;
	}

	.m-toc .toc-link::before {
		background-color: #EEE;
		content: ' ';
		display: inline-block;
		height: 0;
		left: 0;
		margin-top: -1px;
		position: absolute;
		width: 2px
	}

	.m-toc .is-active-link::before {
		background-color: #14b8a6
	}
</style>
