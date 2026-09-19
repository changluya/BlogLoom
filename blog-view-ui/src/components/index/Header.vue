<template>
	<header ref="header">
		<div class="view">
			<div class="bg" :style="{backgroundImage:'url('+defaultSettings.banner+')'}"></div>
		</div>
		<div class="hero-overlay"></div>
		<div class="hero-content">
			<div class="hero-eyebrow">{{ defaultSettings.heroEyebrow }}</div>
			<h1>{{ defaultSettings.malfunctionText }}</h1>
			<p>{{ defaultSettings.heroDescription }}</p>
			<div class="hero-divider"><span></span></div>
		</div>
		<div class="wrapper">
			<i class="ali-iconfont icon-down" @click="scrollToMain"></i>
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
			...mapState(['clientSize'])
		},
		watch: {
			'clientSize.clientHeight'() {
				this.setHeaderHeight()
			}
		},
		mounted() {
			this.setHeaderHeight()
			let startingPoint
			const header = this.$refs.header
			header.addEventListener('mouseenter', (e) => {
				startingPoint = e.clientX
			})
			header.addEventListener('mouseout', (e) => {
				header.classList.remove('moving')
				header.style.setProperty('--percentage', 0.5)
			})
			header.addEventListener('mousemove', (e) => {
				let percentage = (e.clientX - startingPoint) / window.outerWidth + 0.5
				header.style.setProperty('--percentage', percentage)
				header.classList.add('moving')
			})
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
		--percentage: 0.5;
		position: relative;
		overflow: hidden;
		user-select: none;
	}

	.view {
		position: absolute;
		top: 0;
		right: 0;
		bottom: 0;
		left: 0;
		display: flex;
		justify-content: center;
		transform: translatex(calc(var(--percentage) * 100px));
	}

	.view div {
		background-position: center center;
		background-size: cover;
		position: absolute;
		width: 110%;
		height: 100%;
	}

	.view .bg {
		z-index: 10;
	}

	.hero-overlay {
		position: absolute;
		inset: 0;
		z-index: 30;
		background:
			linear-gradient(180deg, rgba(8, 15, 28, .48) 0%, rgba(8, 15, 28, .08) 35%, rgba(8, 15, 28, .28) 100%),
			radial-gradient(circle at center, transparent 20%, rgba(4, 9, 18, .2) 100%);
		pointer-events: none;
	}

	header .view {
		transition: .2s all ease-in;
	}

	header.moving .view {
		transition: none;
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
		text-shadow: 0 2px 22px rgba(0, 0, 0, .3);
	}

	.hero-eyebrow {
		display: inline-flex;
		align-items: center;
		min-height: 30px;
		padding: 0 15px;
		border: 1px solid rgba(255, 255, 255, .4);
		border-radius: 999px;
		background: rgba(8, 20, 35, .25);
		backdrop-filter: blur(10px);
		-webkit-backdrop-filter: blur(10px);
		font-size: 11px;
		font-weight: 600;
		letter-spacing: 2.2px;
	}

	.hero-content h1 {
		margin: 21px 0 14px;
		font-family: Avenir, "Helvetica Neue", Arial, sans-serif;
		font-size: clamp(48px, 6vw, 82px);
		font-weight: 700;
		line-height: 1.05;
		letter-spacing: -2.5px;
	}

	.hero-content p {
		margin: 0 auto;
		max-width: 620px;
		color: rgba(255, 255, 255, .9);
		font-size: clamp(14px, 1.5vw, 18px);
		font-weight: 400;
		line-height: 1.8;
		letter-spacing: .5px;
	}

	.hero-divider {
		display: flex;
		justify-content: center;
		margin-top: 24px;
	}

	.hero-divider span {
		width: 42px;
		height: 3px;
		border-radius: 3px;
		background: linear-gradient(90deg, #27d9ff, #7c6cff);
		box-shadow: 0 0 18px rgba(39, 217, 255, .65);
	}

	.wrapper {
		position: absolute;
		width: 100px;
		bottom: 150px;
		left: 0;
		right: 0;
		margin: auto;
		font-size: 26px;
		z-index: 100;
	}

	.wrapper i {
		font-size: 60px;
		opacity: 0.5;
		cursor: pointer;
		position: absolute;
		top: 55px;
		left: 20px;
		animation: opener .5s ease-in-out alternate infinite;
		transition: opacity .2s ease-in-out, transform .5s ease-in-out .2s;
	}

	.wrapper i:hover {
		opacity: 1;
	}

	@keyframes opener {
		100% {
			top: 65px
		}
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
