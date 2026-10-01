<template>
	<header ref="header">
		<div class="hero-overlay"></div>
		<div class="hero-content">
			<div class="hero-eyebrow">{{ heroConfig.eyebrow }}</div>
			<h1>{{ heroConfig.title }}</h1>
			<p>{{ heroConfig.description }}</p>
		</div>
		<div class="wrapper" role="button" tabindex="0" aria-label="向下浏览" @click="scrollToMain" @keyup.enter="scrollToMain">
			<span class="scroll-chevron" aria-hidden="true"></span>
		</div>
		<div class="wave1"></div>
		<div class="wave2"></div>
	</header>
</template>

<script>
	import {mapState} from 'vuex'
	import defaultSettings from '@/settings'

	export default {
		name: "Header",
		data() {
			return {
				defaultSettings
			}
		},
		computed: {
			...mapState(['clientSize', 'siteInfo']),
			heroConfig() {
				const configured = this.siteInfo && this.siteInfo.heroConfig
				return {
					eyebrow: configured && configured.eyebrow || defaultSettings.heroEyebrow,
					title: configured && configured.title || defaultSettings.malfunctionText,
					description: configured && configured.description || defaultSettings.heroDescription
				}
			}
		},
		watch: {
			'clientSize.clientHeight'() {
				this.setHeaderHeight()
			}
		},
		mounted() {
			this.setHeaderHeight()
		},
		methods: {
			//根据可视窗口高度，动态改变首图大小
			setHeaderHeight() {
				this.$refs.header.style.height = this.clientSize.clientHeight + 'px'
			},
			//平滑滚动至正文部分
			scrollToMain() {
				window.scrollTo({top: this.clientSize.clientHeight, behavior: 'smooth'})
			}
		},
	}
</script>

<style scoped>
	header {
		position: relative;
		overflow: hidden;
		user-select: none;
	}

	/* 背景图由 Index 的固定层提供，这里只负责首屏文字遮罩 */
	.hero-overlay {
		position: absolute;
		inset: 0;
		z-index: 30;
		background:
			linear-gradient(180deg, rgba(8, 15, 28, .48) 0%, rgba(8, 15, 28, .08) 35%, rgba(8, 15, 28, .28) 100%),
			radial-gradient(circle at center, transparent 20%, rgba(4, 9, 18, .2) 100%);
		pointer-events: none;
	}

	.hero-content {
		position: absolute;
		top: 43%;
		left: 50%;
		z-index: 60;
		width: min(760px, calc(100% - 40px));
		transform: translate(-50%, -50%);
		color: #fff;
		text-align: center;
		text-shadow: 0 3px 24px rgba(4, 12, 25, .34);
	}

	.hero-eyebrow {
		display: inline-flex;
		align-items: center;
		min-height: 28px;
		padding: 0 17px;
		border: 1px solid rgba(255, 255, 255, .3);
		border-radius: 999px;
		background: rgba(42, 78, 126, .34);
		box-shadow: inset 0 1px 0 rgba(255,255,255,.14), 0 5px 18px rgba(7,22,45,.12);
		backdrop-filter: blur(12px);
		-webkit-backdrop-filter: blur(12px);
		color: rgba(255,255,255,.94);
		font-size: 11px;
		font-weight: 650;
		line-height: 1;
		letter-spacing: 2.35px;
	}

	.hero-content h1 {
		margin: 20px 0 15px;
		font-family: Avenir, "Helvetica Neue", Arial, sans-serif;
		font-size: clamp(48px, 6vw, 82px);
		font-weight: 750;
		line-height: 1.05;
		letter-spacing: -2.8px;
		text-shadow: 0 4px 24px rgba(7, 23, 47, .38), 0 1px 2px rgba(7, 23, 47, .24);
	}

	.hero-content p {
		margin: 0 auto;
		max-width: 620px;
		color: rgba(255, 255, 255, .88);
		font-size: clamp(14px, 1.5vw, 18px);
		font-weight: 400;
		line-height: 1.75;
		letter-spacing: .65px;
		text-shadow: 0 2px 12px rgba(7, 23, 47, .4);
	}

	.wrapper {
		position: absolute;
		bottom: 102px;
		left: 50%;
		z-index: 100;
		display: flex;
		width: 52px;
		height: 52px;
		align-items: center;
		justify-content: center;
		transform: translateX(-50%);
		border: 1px solid rgba(255,255,255,.62);
		border-radius: 50%;
		background: rgba(37,65,101,.18);
		box-shadow: inset 0 1px 0 rgba(255,255,255,.22), 0 7px 24px rgba(6,20,40,.2);
		backdrop-filter: blur(7px);
		-webkit-backdrop-filter: blur(7px);
		cursor: pointer;
		animation: opener 1.8s ease-in-out infinite;
		transition: border-color .22s ease, background .22s ease, box-shadow .22s ease;
	}

	.scroll-chevron {
		display: block;
		width: 13px;
		height: 13px;
		margin-top: -5px;
		transform: rotate(45deg);
		border-right: 2px solid rgba(255,255,255,.96);
		border-bottom: 2px solid rgba(255,255,255,.96);
		border-radius: 0 0 2px 0;
		filter: drop-shadow(0 2px 4px rgba(5,18,38,.32));
	}

	.wrapper:hover {
		border-color: rgba(255,255,255,.9);
		background: rgba(255,255,255,.18);
		box-shadow: inset 0 1px 0 rgba(255,255,255,.3), 0 9px 28px rgba(6,20,40,.25);
	}

	@keyframes opener {
		0%, 100% { transform: translate(-50%, 0); }
		50% { transform: translate(-50%, 7px); }
	}

	.wave1, .wave2 {
		position: absolute;
		bottom: 0;
		transition-duration: .4s, .4s;
		z-index: 80;
	}

	.wave1 {
		background: url('/img/header/wave1.png') repeat-x;
		height: 75px;
		width: 100%;
	}

	.wave2 {
		background: url('/img/header/wave2.png') repeat-x;
		height: 90px;
		width: calc(100% + 100px);
		left: -100px;
	}
</style>

<!-- 移动端样式（独立文件，单独维护） -->
<style scoped src="@/styles/mobile/components/header.mobile.scss" lang="scss"></style>
