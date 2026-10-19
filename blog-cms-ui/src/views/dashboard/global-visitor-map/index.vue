<template>
	<main class="visitor-globe-page">
		<GlobeCanvas
			v-if="dataset"
			ref="globe"
			:dataset="dataset"
			:language="language"
			:auto-rotate="autoRotate"
			@status="status = $event"
			@hover="handleHover"
		/>
		<div v-else class="loading-state"><i class="el-icon-loading"></i><span>{{ language === 'zh' ? '正在加载全球访客数据' : 'Loading visitor data' }}</span></div>
		<div class="vignette"></div>

		<section class="intro-panel">
			<div class="eyebrow">GLOBAL VISITOR OBSERVATORY</div>
			<h1>{{ language === 'zh' ? '全球访客地球' : 'Global Visitor Globe' }}</h1>
			<p>{{ language === 'zh'
				? '访客抵达的国家与城市会被逐步点亮。拖拽旋转地球，滚轮放大查看城市热点。'
				: 'Visitor countries and cities light up over time. Drag to rotate and scroll to explore.' }}</p>
			<div class="actions">
				<button type="button" :class="{active: autoRotate}" @click="autoRotate = !autoRotate">
					{{ autoRotate ? (language === 'zh' ? '暂停旋转' : 'Pause') : (language === 'zh' ? '继续旋转' : 'Resume') }}
				</button>
				<button type="button" @click="resetGlobe">{{ language === 'zh' ? '复位视角' : 'Reset view' }}</button>
			</div>
		</section>

		<aside class="statistics-panel">
			<header>
				<div>
					<span class="live-dot"></span>
					<span class="panel-title">VISITOR PULSE · {{ periodShortLabel }}</span>
				</div>
				<div class="panel-actions">
					<div class="language-switch">
						<button type="button" :class="{active: language === 'zh'}" @click="language = 'zh'">中文</button>
						<button type="button" :class="{active: language === 'en'}" @click="language = 'en'">EN</button>
					</div>
					<button
						type="button"
						class="refresh-button"
						:class="{refreshing}"
						:disabled="refreshing"
						:title="refreshTitle"
						@click="refreshNow"
					>
						<i class="el-icon-refresh"></i>
					</button>
					<button
						type="button"
						class="fullscreen-button"
						:title="isFullscreen ? (language === 'zh' ? '退出全屏' : 'Exit fullscreen') : (language === 'zh' ? '全屏展开' : 'Fullscreen')"
						@click="toggleFullscreen"
					>
						<i :class="isFullscreen ? 'el-icon-copy-document' : 'el-icon-full-screen'"></i>
					</button>
				</div>
			</header>
			<div class="range-bar">
				<el-select v-model="periodType" size="mini" class="range-select" @change="handlePeriodChange">
					<el-option v-for="option in periodOptions" :key="option.value" :label="option.label" :value="option.value" />
				</el-select>
				<el-select v-model="rankMode" size="mini" class="rank-select">
					<el-option v-for="option in rankModeOptions" :key="option.value" :label="option.label" :value="option.value" />
				</el-select>
				<template v-if="periodType === 'custom'">
					<el-date-picker
						v-model="customStart"
						type="date"
						size="mini"
						class="range-date"
						value-format="yyyy-MM-dd"
						:placeholder="language === 'zh' ? '开始日期' : 'Start'"
						@change="handleCustomChange"
					/>
					<span class="range-sep">~</span>
					<el-date-picker
						v-model="customEnd"
						type="date"
						size="mini"
						class="range-date"
						value-format="yyyy-MM-dd"
						:placeholder="language === 'zh' ? '结束日期' : 'End'"
						@change="handleCustomChange"
					/>
				</template>
			</div>
			<div class="metric-grid">
				<div v-for="metric in metrics" :key="metric.label" class="metric-card">
					<span>{{ metric.label }}</span>
					<strong>{{ formatNumber(metric.value) }}</strong>
				</div>
			</div>
			<section class="rankings">
				<div class="rankings-title">{{ rankingsTitle }}</div>
				<div class="ranking-columns">
					<section class="ranking-section">
						<div class="ranking-heading"><span>{{ language === 'zh' ? '中国' : 'CHINA' }}</span><small>TOP 20</small></div>
						<ol>
							<li v-for="(item, index) in chinaRanking" :key="item.key">
								<em>{{ index + 1 }}</em><span :title="item.name">{{ item.name }}</span><strong>{{ formatNumber(item.uv) }}</strong>
							</li>
						</ol>
					</section>
					<section class="ranking-section">
						<div class="ranking-heading"><span>{{ language === 'zh' ? '国外' : 'OVERSEAS' }}</span><small>TOP 20</small></div>
						<ol>
							<li v-for="(item, index) in overseasRanking" :key="item.key">
								<em>{{ index + 1 }}</em><span :title="item.name">{{ item.name }}</span><strong>{{ formatNumber(item.uv) }}</strong>
							</li>
						</ol>
					</section>
				</div>
			</section>
		</aside>

		<div class="legend">
			<span>VISITOR INTENSITY</span>
			<i v-for="index in 4" :key="index"></i>
			<span>LOW&nbsp;&nbsp;HIGH</span>
		</div>
		<div class="runtime-status"><strong>{{ status.tone }}</strong> · {{ status.text }}</div>
		<div class="data-note">{{ dataNote }}</div>

		<div v-if="hover" class="map-tooltip" :style="tooltipStyle">
			<strong v-if="hover.city">{{ displayCityName(hover.city) }}</strong>
			<strong v-else-if="hover.province">{{ displayProvinceName(hover.province) }}</strong>
			<strong v-else-if="hover.region">{{ hover.region }}</strong>
			<span v-if="hover.city">
				{{ language === 'zh' ? '访客数' : 'Visitors' }}：{{ formatNumber(hover.stats.uv) }}
			</span>
			<span v-else-if="hover.provinceStats">
				{{ language === 'zh'
					? `访客城市 ${hover.provinceStats.cityCount} 个 · 最高 UV ${formatNumber(hover.provinceStats.maxUv)}`
					: `${hover.provinceStats.cityCount} cities · max UV ${formatNumber(hover.provinceStats.maxUv)}` }}
			</span>
			<span v-else-if="hover.regionStats">
				{{ language === 'zh'
					? `访客城市 ${hover.regionStats.cityCount} 个 · 最高 UV ${formatNumber(hover.regionStats.maxUv)}`
					: `${hover.regionStats.cityCount} cities · max UV ${formatNumber(hover.regionStats.maxUv)}` }}
			</span>
		</div>
	</main>
</template>

<script>
	import GlobeCanvas from '@/components/VisitorGlobe/GlobeCanvas.vue'
	import mockDataset from '@/components/VisitorGlobe/data/visitor-stats.json'
	import {getCityDisplayName} from '@/components/VisitorGlobe/renderer'
	import {getGlobalVisitorMap} from '@/api/visitorMap'

	const REFRESH_INTERVAL = 10000
	const countryNameFormatters = {}
	const MUNICIPALITIES = ['北京', '天津', '上海', '重庆', '香港', '澳门']
	const AUTONOMOUS_REGIONS = ['内蒙古', '广西', '西藏', '宁夏', '新疆']
	const PROVINCE_NAME_EN = {
		北京: 'Beijing', 天津: 'Tianjin', 上海: 'Shanghai', 重庆: 'Chongqing',
		河北: 'Hebei', 山西: 'Shanxi', 辽宁: 'Liaoning', 吉林: 'Jilin', 黑龙江: 'Heilongjiang',
		江苏: 'Jiangsu', 浙江: 'Zhejiang', 安徽: 'Anhui', 福建: 'Fujian', 江西: 'Jiangxi',
		山东: 'Shandong', 河南: 'Henan', 湖北: 'Hubei', 湖南: 'Hunan', 广东: 'Guangdong',
		海南: 'Hainan', 四川: 'Sichuan', 贵州: 'Guizhou', 云南: 'Yunnan', 陕西: 'Shaanxi',
		甘肃: 'Gansu', 青海: 'Qinghai', 台湾: 'Taiwan', 内蒙古: 'Inner Mongolia',
		广西: 'Guangxi', 西藏: 'Tibet', 宁夏: 'Ningxia', 新疆: 'Xinjiang',
		香港: 'Hong Kong', 澳门: 'Macao'
	}

	function getCountryNameFormatter(language) {
		const locale = language === 'en' ? 'en' : 'zh-CN'
		if (Object.prototype.hasOwnProperty.call(countryNameFormatters, locale)) return countryNameFormatters[locale]
		let formatter = null
		try {
			if (typeof Intl !== 'undefined' && typeof Intl.DisplayNames === 'function') {
				formatter = new Intl.DisplayNames([locale], {type: 'region'})
			}
		} catch (error) {
			formatter = null
		}
		countryNameFormatters[locale] = formatter
		return formatter
	}

	export default {
		name: 'GlobalVisitorMap',
		components: {GlobeCanvas},
		data() {
			return {
				dataset: null,
				language: 'zh',
				autoRotate: true,
				isFullscreen: false,
				status: {tone: 'BOOTING', text: 'dark globe initializing'},
				hover: null,
				refreshing: false,
				refreshFailed: false,
				lastUpdatedAt: null,
				refreshTimer: null,
				periodType: 'all',
				rankMode: 'country',
				customStart: null,
				customEnd: null
			}
		},
		mounted() {
			document.addEventListener('fullscreenchange', this.handleFullscreenChange)
			document.addEventListener('visibilitychange', this.handleVisibilityChange)
			this.loadVisitorMap()
			this.restartAutoRefresh()
		},
		beforeDestroy() {
			document.removeEventListener('fullscreenchange', this.handleFullscreenChange)
			document.removeEventListener('visibilitychange', this.handleVisibilityChange)
			this.stopAutoRefresh()
		},
		computed: {
			periodOptions() {
				const zh = this.language === 'zh'
				return [
					{value: 'all', label: zh ? '全部' : 'All time'},
					{value: 'today', label: zh ? '今日' : 'Today'},
					{value: 'yesterday', label: zh ? '昨日' : 'Yesterday'},
					{value: 'last_7d', label: zh ? '前 7 天' : 'Last 7 days'},
					{value: 'custom', label: zh ? '自定义' : 'Custom'}
				]
			},
			periodShortLabel() {
				return {
					all: 'ALL',
					today: 'TODAY',
					yesterday: 'YESTERDAY',
					last_7d: '7D',
					custom: 'CUSTOM'
				}[this.periodType] || 'ALL'
			},
			rankModeOptions() {
				const zh = this.language === 'zh'
				return [
					{value: 'city', label: zh ? '城市' : 'City'},
					{value: 'country', label: zh ? '国家' : 'Country'}
				]
			},
			rankingsTitle() {
				const zh = this.language === 'zh'
				if (this.rankMode === 'country') return zh ? '热点国家排名 · UV' : 'HOT COUNTRIES · UV'
				return zh ? '热点城市排名 · UV' : 'HOT CITIES · UV'
			},
			dataNote() {
				const base = this.language === 'zh'
					? '光点大小随访客量 · IP 定位为近似位置'
					: 'DOT SIZE BY VISITOR VOLUME · IP-BASED APPROXIMATE LOCATION'
				if (this.periodType !== 'today') {
					return this.language === 'zh' ? `手动刷新 · ${base}` : `MANUAL REFRESH · ${base}`
				}
				return this.language === 'zh' ? `自动刷新 10 秒 · ${base}` : `AUTO REFRESH 10S · ${base}`
			},
			metrics() {
				const summary = this.dataset ? this.dataset.summary : {uv: 0, pv: 0, countryCount: 0, cityCount: 0}
				return [
					{label: this.language === 'zh' ? '全球 UV' : 'GLOBAL UV', value: summary.uv},
					{label: this.language === 'zh' ? '全球 PV' : 'GLOBAL PV', value: summary.pv},
					{label: this.language === 'zh' ? '点亮国家' : 'COUNTRIES', value: summary.countryCount},
					{label: this.language === 'zh' ? '城市热点' : 'CITIES', value: summary.cityCount}
				]
			},
			chinaRanking() {
				if (!this.dataset) return []
				if (this.rankMode === 'country') {
					const china = (this.dataset.countries || []).find(item => item.countryCode === 'CN')
					return [{key: 'CN', name: this.displayCountryName('CN'), uv: china ? china.uv : 0}]
				}
				return this.rankingItems(this.dataset.cities.filter(city => city.countryCode === 'CN'))
			},
			overseasRanking() {
				if (!this.dataset) return []
				const list = this.rankMode === 'country' ? (this.dataset.countries || []) : this.dataset.cities
				return this.rankingItems(list.filter(item => item.countryCode !== 'CN'))
			},
			tooltipStyle() {
				return {left: `${this.hover.x + 14}px`, top: `${this.hover.y - 35}px`}
			},
			refreshTitle() {
				const base = this.language === 'zh' ? '刷新数据（自动每 10 秒）' : 'Refresh data (auto every 10s)'
				if (this.refreshFailed) {
					return this.language === 'zh' ? `${base} · 最近一次刷新失败` : `${base} · last refresh failed`
				}
				return this.lastUpdatedAt
					? `${base} · ${this.language === 'zh' ? '更新于' : 'updated'} ${this.lastUpdatedAt.toLocaleTimeString()}`
					: base
			}
		},
		methods: {
			resetGlobe() {
				if (this.$refs.globe) this.$refs.globe.reset()
			},
			restartAutoRefresh() {
				this.stopAutoRefresh()
				if (this.periodType !== 'today') return
				this.refreshTimer = window.setInterval(() => {
					if (document.hidden || this.refreshing) return
					this.loadVisitorMap(true)
				}, REFRESH_INTERVAL)
			},
			handlePeriodChange() {
				this.restartAutoRefresh()
				if (this.periodType === 'custom') {
					if (this.customStart && this.customEnd) this.loadVisitorMap()
					return
				}
				this.loadVisitorMap()
			},
			handleCustomChange() {
				if (this.customStart && this.customEnd) this.loadVisitorMap()
			},
			stopAutoRefresh() {
				if (this.refreshTimer) {
					window.clearInterval(this.refreshTimer)
					this.refreshTimer = null
				}
			},
			refreshNow() {
				if (this.refreshing) return
				this.loadVisitorMap(true)
			},
			handleVisibilityChange() {
				if (!document.hidden && !this.refreshing) this.loadVisitorMap(true)
			},
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
					this.lastUpdatedAt = new Date()
					this.refreshFailed = false
				} catch (error) {
					if (this.dataset) {
						this.refreshFailed = true
						return
					}
					if (import.meta.env.DEV) {
						this.dataset = this.normalizeDataset(mockDataset)
						this.status = {tone: 'LOCAL MOCK', text: 'backend unavailable · development fallback'}
						return
					}
					this.status = {tone: 'ERROR', text: 'visitor data unavailable'}
				} finally {
					this.refreshing = false
				}
			},
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
			},
			rankingItems(list) {
				const byCountry = this.rankMode === 'country'
				return list
					.slice()
					.sort((left, right) => (Number(right.uv) || 0) - (Number(left.uv) || 0))
					.slice(0, 20)
					.map(item => ({
						key: byCountry ? item.countryCode : (item.cityCode || item.city),
						name: byCountry ? this.displayCountryName(item.countryCode) : this.displayCityName(item),
						uv: item.uv
					}))
			},
			formatNumber(value) {
				return Number(value || 0).toLocaleString()
			},
			displayCityName(city) {
				return getCityDisplayName(city, this.language)
			},
			displayCountryName(countryCode) {
				if (!countryCode) return ''
				const formatter = getCountryNameFormatter(this.language)
				if (!formatter) return countryCode
				try {
					return formatter.of(countryCode) || countryCode
				} catch (error) {
					return countryCode
				}
			},
			displayProvinceName(name) {
				if (!name) return ''
				if (this.language === 'en') return PROVINCE_NAME_EN[name] || name
				if (MUNICIPALITIES.includes(name)) return name
				if (AUTONOMOUS_REGIONS.includes(name)) return `${name}自治区`
				return `${name}省`
			},
			handleHover(payload) {
				if (!payload) {
					this.hover = null
					return
				}
				const bounds = this.$el.getBoundingClientRect()
				this.hover = {...payload, x: payload.x - bounds.left, y: payload.y - bounds.top}
			},
			async toggleFullscreen() {
				try {
					if (document.fullscreenElement) await document.exitFullscreen()
					else await this.$el.requestFullscreen()
				} catch (error) {
					this.$message.error(this.language === 'zh' ? '当前浏览器无法切换全屏' : 'Fullscreen is unavailable')
				}
			},
			handleFullscreenChange() {
				this.isFullscreen = document.fullscreenElement === this.$el
			}
		}
	}
</script>

<style lang="scss">
	.visitor-globe-page {
		--panel: rgba(7, 18, 31, .82);
		--line: rgba(125, 185, 222, .2);
		--text: #e7f7ff;
		--muted: #7e9bac;
		--cyan: #5de3ff;
		position: relative;
		min-height: calc(100vh - 50px);
		overflow: hidden;
		background: radial-gradient(circle at 48% 48%, rgba(25, 75, 115, .16), transparent 35%), #03070d;
		color: var(--text);
		font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;

		* { box-sizing: border-box; }
		button { font: inherit; }
		.vignette { position: absolute; inset: 0; pointer-events: none; background: radial-gradient(circle at 50% 50%, transparent 44%, rgba(3, 7, 13, .58)); }
		.intro-panel { position: absolute; z-index: 2; top: 30px; left: 34px; width: 430px; pointer-events: none; }
		.eyebrow { color: var(--cyan); font-size: 11px; font-weight: 700; letter-spacing: .22em; }
		h1 { margin: 10px 0 0; font-size: 40px; line-height: 1.05; letter-spacing: -.04em; }
		.intro-panel p { margin: 14px 0 0; color: var(--muted); font-size: 13px; line-height: 1.7; }
		.actions { display: flex; gap: 6px; margin-top: 14px; pointer-events: auto; }
		.actions button, .language-switch button { border: 1px solid var(--line); border-radius: 8px; color: var(--text); background: rgba(12, 29, 47, .72); cursor: pointer; }
		.actions button { min-height: 28px; padding: 5px 9px; border-radius: 6px; font-size: 11px; line-height: 16px; }
		.actions button:hover, .actions button.active { border-color: rgba(93, 227, 255, .75); color: var(--cyan); }
		.statistics-panel { position: absolute; z-index: 3; top: 20px; right: 20px; width: 330px; padding: 14px; border: 1px solid var(--line); border-radius: 14px; background: linear-gradient(145deg, rgba(11, 27, 44, .92), rgba(5, 13, 23, .78)); box-shadow: 0 20px 60px rgba(0, 0, 0, .42); backdrop-filter: blur(16px); }
		.statistics-panel header { display: flex; align-items: center; justify-content: space-between; }
		.panel-actions { display: flex; align-items: center; gap: 7px; }
		.range-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; margin-top: 10px; }
		.range-select { width: 96px; }
		.rank-select { width: 84px; }
		.range-date { flex: 1 1 96px; min-width: 0; }
		.range-sep { color: var(--muted); font-size: 11px; }
		.range-bar .el-input__inner { height: 28px; line-height: 28px; border-color: var(--line); background: rgba(4, 13, 23, .6); color: var(--text); font-size: 11px; }
		.range-bar .el-input__icon { line-height: 28px; }
		.range-bar .el-input__prefix, .range-bar .el-input__suffix { color: var(--muted); }
		.panel-title { font-size: 12px; font-weight: 700; letter-spacing: .1em; }
		.live-dot { display: inline-block; width: 7px; height: 7px; margin-right: 8px; border-radius: 50%; background: #73f0b1; box-shadow: 0 0 15px #73f0b1; }
		.language-switch { display: flex; gap: 2px; padding: 2px; border: 1px solid var(--line); border-radius: 7px; }
		.language-switch button { min-width: 38px; padding: 4px 7px; border: 0; color: var(--muted); background: transparent; font-size: 10px; }
		.language-switch button.active { color: #031018; background: var(--cyan); }
		.fullscreen-button { display: grid; width: 29px; height: 29px; padding: 0; place-items: center; border: 1px solid var(--line); border-radius: 7px; color: var(--muted); background: rgba(4, 13, 23, .48); cursor: pointer; }
		.fullscreen-button:hover { border-color: rgba(93, 227, 255, .7); color: var(--cyan); }
		.refresh-button { display: grid; width: 29px; height: 29px; padding: 0; place-items: center; border: 1px solid var(--line); border-radius: 7px; color: var(--muted); background: rgba(4, 13, 23, .48); cursor: pointer; }
		.refresh-button:hover { border-color: rgba(93, 227, 255, .7); color: var(--cyan); }
		.refresh-button:disabled { cursor: progress; }
		.refresh-button.refreshing { color: var(--cyan); }
		.refresh-button.refreshing i { animation: visitor-globe-spin .8s linear infinite; }
		@keyframes visitor-globe-spin { to { transform: rotate(360deg); } }
		.metric-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; margin-top: 12px; }
		.metric-card { min-width: 0; padding: 9px 7px; border: 1px solid rgba(125, 185, 222, .12); border-radius: 8px; background: rgba(4, 13, 23, .45); }
		.metric-card span { display: block; color: var(--muted); font-size: 11px; }
		.metric-card strong { display: block; margin-top: 4px; overflow: hidden; font-size: 16px; font-weight: 500; font-variant-numeric: tabular-nums; text-overflow: ellipsis; }
		.rankings { margin-top: 11px; padding-top: 10px; border-top: 1px solid var(--line); }
		.rankings-title { color: var(--muted); font-size: 11px; letter-spacing: .08em; }
		.ranking-columns { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-top: 8px; }
		.ranking-heading { display: flex; justify-content: space-between; margin-bottom: 5px; font-size: 12px; font-weight: 600; }
		.ranking-heading small { color: var(--muted); font-size: 9px; }
		.ranking-section ol { height: 210px; margin: 0; padding: 0 5px 0 0; overflow-y: auto; list-style: none; scrollbar-width: thin; scrollbar-color: rgba(93, 227, 255, .35) transparent; }
		.ranking-section li { display: grid; grid-template-columns: 17px minmax(0, 1fr) auto; align-items: center; gap: 5px; min-height: 23px; border-bottom: 1px solid rgba(125, 185, 222, .08); font-size: 10px; }
		.ranking-section li em { color: var(--muted); font-style: normal; }
		.ranking-section li:nth-child(-n+3) em, .ranking-section li strong { color: var(--cyan); }
		.ranking-section li span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
		.ranking-section li strong { font-weight: 500; font-variant-numeric: tabular-nums; }
		.legend { position: absolute; z-index: 2; bottom: 28px; left: 34px; display: flex; align-items: center; gap: 4px; padding: 10px 12px; border: 1px solid var(--line); border-radius: 9px; background: rgba(5, 14, 24, .75); color: var(--muted); font-size: 10px; }
		.legend span:first-child { margin-right: 8px; }
		.legend span:last-child { margin-left: 8px; }
		.legend i { width: 22px; height: 5px; border-radius: 4px; background: #14304a; }
		.legend i:nth-of-type(2) { background: #1a6384; }
		.legend i:nth-of-type(3) { background: #27b5cb; }
		.legend i:nth-of-type(4) { background: #7ae9ed; }
		.runtime-status { position: absolute; z-index: 2; right: 28px; bottom: 28px; color: var(--muted); font-size: 11px; }
		.runtime-status strong { color: var(--cyan); font-weight: 500; }
		.data-note { position: absolute; z-index: 2; bottom: 28px; left: 50%; transform: translateX(-50%); color: rgba(231, 247, 255, .45); font-size: 10px; letter-spacing: .08em; }
		.map-tooltip { position: absolute; z-index: 6; display: flex; flex-direction: column; min-width: 130px; padding: 9px 11px; border: 1px solid rgba(93, 227, 255, .4); border-radius: 8px; background: rgba(4, 14, 24, .96); box-shadow: 0 12px 32px rgba(0, 0, 0, .34); font-size: 12px; line-height: 1.6; pointer-events: none; }
		.map-tooltip strong { color: var(--cyan); }
		.map-tooltip span { color: var(--muted); }
		.loading-state { position: absolute; z-index: 4; inset: 0; display: flex; align-items: center; justify-content: center; gap: 10px; color: var(--muted); font-size: 13px; }
		.loading-state i { color: var(--cyan); font-size: 18px; }
	}

	@media (max-width: 1000px) {
		.visitor-globe-page {
			.intro-panel { left: 20px; width: 340px; }
			h1 { font-size: 30px; }
			.statistics-panel { right: 18px; width: 330px; }
			.ranking-section ol { max-height: 160px; }
			.data-note { display: none; }
		}
	}

	@media (max-width: 720px) {
		.visitor-globe-page {
			min-height: 900px;
			.intro-panel { top: 18px; left: 18px; width: calc(100% - 36px); }
			.statistics-panel { top: auto; right: 18px; bottom: 68px; width: calc(100% - 36px); }
			.legend { left: 18px; bottom: 18px; }
			.runtime-status { display: none; }
		}
	}
</style>
