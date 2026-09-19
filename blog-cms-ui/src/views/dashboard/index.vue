<template>
	<div class="dashboard-container">
		<div class="dashboard-heading">
			<div>
				<h2>数据概览</h2>
				<p>欢迎回来，这里汇总了 BlogLoom 的核心运营数据。</p>
			</div>
			<span class="dashboard-date">{{ currentDate }}</span>
		</div>

		<el-row class="panel-group metrics" :gutter="20">
			<el-col v-for="item in metrics" :key="item.label" :xs="24" :sm="12" :lg="6">
				<el-card class="card-panel" shadow="hover" body-style="padding: 0">
					<div class="card-panel-icon-wrapper" :class="item.className">
						<svg v-if="item.key === 'pv'" class="card-panel-icon" viewBox="0 0 24 24" aria-hidden="true">
							<path d="M3 17l5-5 4 4 8-9"/><path d="M14 7h6v6"/>
						</svg>
						<svg v-else-if="item.key === 'uv'" class="card-panel-icon" viewBox="0 0 24 24" aria-hidden="true">
							<circle cx="12" cy="8" r="4"/><path d="M4.5 21a7.5 7.5 0 0115 0"/>
						</svg>
						<svg v-else-if="item.key === 'article'" class="card-panel-icon" viewBox="0 0 24 24" aria-hidden="true">
							<path d="M6 3h8l4 4v14H6z"/><path d="M14 3v5h4M9 12h6M9 16h6"/>
						</svg>
						<svg v-else class="card-panel-icon" viewBox="0 0 24 24" aria-hidden="true">
							<path d="M4 5h16v12H9l-5 4z"/><path d="M8 9h8M8 13h5"/>
						</svg>
					</div>
					<div class="card-panel-description">
						<div class="card-panel-text">{{ item.label }}</div>
						<span class="card-panel-num">{{ item.value }}</span>
						<span class="card-panel-unit">{{ item.unit }}</span>
					</div>
				</el-card>
			</el-col>
		</el-row>

		<el-row class="panel-group charts distribution-charts" :gutter="20">
			<el-col :xs="24" :lg="12">
				<el-card class="chart-card" shadow="never">
					<div class="chart-heading">
						<strong>分类排行</strong>
						<span>共 {{ categorySummary.total }} 个，展示 Top {{ categorySummary.displayed }}<template v-if="categorySummary.remaining">，其余 {{ categorySummary.remaining }} 个</template></span>
					</div>
					<div class="distribution-body">
						<div v-show="!categoryExpanded" ref="categoryEcharts" class="chart"></div>
						<div v-if="categoryExpanded" v-loading="categoryRankingLoading" class="ranking-scroll">
							<div v-for="(item,index) in categoryRanking" :key="item.id" class="ranking-row">
								<span class="ranking-index">{{ index + 1 }}</span><span class="ranking-name" :title="item.name">{{ item.name }}</span>
								<span class="ranking-bar"><i :style="{width: rankPercent(item, categoryRanking)}"></i></span><strong>{{ item.value }}</strong>
							</div>
						</div>
						<button v-if="categorySummary.remaining || categoryExpanded" class="ranking-toggle" type="button" @click="toggleRanking('category')">
							<span v-if="!categoryExpanded" class="ranking-dots">•••</span>{{ categoryExpanded ? '收起排行' : '展开全部' }}
						</button>
					</div>
				</el-card>
			</el-col>
			<el-col :xs="24" :lg="12">
				<el-card class="chart-card" shadow="never">
					<div class="chart-heading">
						<strong>标签排行</strong>
						<span>共 {{ tagSummary.total }} 个，已使用 {{ tagSummary.used }} 个，展示 Top {{ tagSummary.displayed }}</span>
					</div>
					<div class="distribution-body">
						<div v-show="!tagExpanded" ref="tagEcharts" class="chart"></div>
						<div v-if="tagExpanded" v-loading="tagRankingLoading" class="ranking-scroll">
							<div v-for="(item,index) in tagRanking" :key="item.id" class="ranking-row">
								<span class="ranking-index">{{ index + 1 }}</span><span class="ranking-name" :title="item.name">{{ item.name }}</span>
								<span class="ranking-bar tag-ranking-bar"><i :style="{width: rankPercent(item, tagRanking)}"></i></span><strong>{{ item.value }}</strong>
							</div>
						</div>
						<button v-if="tagSummary.remaining || tagExpanded" class="ranking-toggle" type="button" @click="toggleRanking('tag')">
							<span v-if="!tagExpanded" class="ranking-dots">•••</span>{{ tagExpanded ? '收起排行' : '展开全部' }}
						</button>
					</div>
				</el-card>
			</el-col>
		</el-row>

		<el-row class="panel-group charts" :gutter="20">
			<el-col :xs="24">
				<el-card class="chart-card" shadow="never">
					<div class="chart-heading"><strong>访客地图</strong><span>访客地域实时分布</span></div>
					<div ref="mapEcharts" class="map-chart"></div>
				</el-card>
			</el-col>
		</el-row>

		<el-card class="panel-group chart-card trend-card" shadow="never">
			<div class="chart-heading"><strong>近七日访问趋势</strong><span>PV 与 UV 变化情况</span></div>
			<div ref="visitRecordEcharts" class="trend-chart"></div>
		</el-card>
	</div>
</template>

<script>
	import echarts from 'echarts'
	import 'echarts/map/js/china'
	import {getDashboard, getDashboardRanking} from "@/api/dashboard";
	//城市经纬度数据来自 https://github.com/Naccl/region2coord
	import geoCoordMap from '@/util/city2coord.json'

	export default {
		name: "Dashboard",
		data() {
			return {
				currentDate: new Intl.DateTimeFormat('zh-CN', {
					year: 'numeric', month: 'long', day: 'numeric', weekday: 'long'
				}).format(new Date()),
				pv: 0,
				uv: 0,
				blogCount: 0,
				commentCount: 0,
				categorySummary: {total: 0, displayed: 0, remaining: 0},
				tagSummary: {total: 0, used: 0, unused: 0, displayed: 0, remaining: 0},
				categoryExpanded: false,
				tagExpanded: false,
				categoryRankingLoading: false,
				tagRankingLoading: false,
				categoryRanking: [],
				tagRanking: [],
				categoryEcharts: null,
				tagEcharts: null,
				mapEcharts: null,
				visitRecordEcharts: null,
				categoryOption: {
					tooltip: {
						trigger: 'axis',
						axisPointer: {type: 'shadow'},
						formatter: params => `${params[0].name}<br/>文章数：${params[0].value}`
					},
					grid: {left: 16, right: 52, top: 18, bottom: 10, containLabel: true},
					xAxis: {
						type: 'value',
						minInterval: 1,
						splitLine: {lineStyle: {color: '#eef2f6'}},
						axisLine: {show: false},
						axisTick: {show: false}
					},
					yAxis: {type: 'category', data: [], axisLine: {show: false}, axisTick: {show: false}, axisLabel: {color: '#657386', width: 150, overflow: 'truncate'}},
					series: [
						{
							name: '文章数量',
							type: 'bar',
							barMaxWidth: 18,
							data: [],
							label: {show: true, position: 'right', color: '#637083'},
							itemStyle: {barBorderRadius: [0, 6, 6, 0], color: '#49a9ee'}
						}
					]
				},
				tagOption: {
					tooltip: {
						trigger: 'axis',
						axisPointer: {type: 'shadow'},
						formatter: params => `${params[0].name}<br/>文章数：${params[0].value}`
					},
					grid: {left: 16, right: 52, top: 12, bottom: 8, containLabel: true},
					xAxis: {
						type: 'value',
						minInterval: 1,
						splitLine: {lineStyle: {color: '#eef2f6'}},
						axisLine: {show: false},
						axisTick: {show: false}
					},
					yAxis: {type: 'category', data: [], axisLine: {show: false}, axisTick: {show: false}, axisLabel: {color: '#657386', width: 150, overflow: 'truncate', fontSize: 11}},
					series: [
						{
							name: '文章数量',
							type: 'bar',
							barMaxWidth: 13,
							data: [],
							label: {show: true, position: 'right', color: '#637083', fontSize: 11},
							itemStyle: {barBorderRadius: [0, 6, 6, 0], color: '#67c7c1'}
						}
					]
				},
				//地图效果 reference https://www.jianshu.com/p/028525cbd080
				//reference https://echarts.apache.org/examples/zh/editor.html?c=map-polygon
				mapOption: {
					title: {
						show: false
					},
					tooltip: {
						show: false
					},
					geo: {
						map: "china",
						roam: false,//关闭拖拽
						zoom: 1.24,
						center: [104.2, 36],//调整地图位置
						label: {
							normal: {
								show: false,//关闭省份名展示
								fontSize: "10",
								color: "rgba(0,0,0,0.7)"
							},
							emphasis: {
								show: false
							}
						},
						itemStyle: {
							normal: {
								areaColor: "#0d0059",
								borderColor: "#389dff",
								borderWidth: 1,//设置外层边框
								shadowBlur: 5,
								shadowOffsetY: 8,
								shadowOffsetX: 0,
								shadowColor: "#01012a"
							},
							emphasis: {
								areaColor: "#184cff",
								shadowOffsetX: 0,
								shadowOffsetY: 0,
								shadowBlur: 5,
								borderWidth: 0,
								shadowColor: "rgba(0, 0, 0, 0.5)"
							}
						}
					},
					series: [
						{
							type: "map",
							map: "china",
							roam: false,
							zoom: 1.24,
							center: [104.2, 36],
							showLegendSymbol: false,
							label: {
								normal: {
									show: false
								},
								emphasis: {
									show: false
								}
							},
							itemStyle: {
								normal: {
									areaColor: "#0d0059",
									borderColor: "#389dff",
									borderWidth: 0.5
								},
								emphasis: {
									areaColor: "#17008d",
									shadowOffsetX: 0,
									shadowOffsetY: 0,
									shadowBlur: 5,
									borderWidth: 0,
									shadowColor: "rgba(0, 0, 0, 0.5)"
								}
							}
						},
						{
							name: "",
							type: "scatter",
							coordinateSystem: "geo",
							data: [],
							symbol: "circle",
							symbolSize: 5,
							hoverSymbolSize: 10,
							tooltip: {
								formatter(value) {
									return value.data.name + "<br/>" + "访客数：" + value.data.uv
								},
								show: true
							},
							encode: {
								value: 2
							},
							label: {
								formatter: "{b}",
								position: "right",
								show: false
							},
							itemStyle: {
								color: "#0efacc"
							},
							emphasis: {
								label: {
									show: false
								}
							}
						},
						{
							name: "Top 5",
							type: "effectScatter",
							coordinateSystem: "geo",
							data: [],
							symbol: "circle",
							symbolSize: 12,
							tooltip: {
								formatter(value) {
									return value.data.name + "<br/>" + "访客数：" + value.data.uv
								},
								show: true
							},
							encode: {
								value: 2
							},
							showEffectOn: "render",
							rippleEffect: {
								brushType: "stroke",
								color: "#0efacc",
								period: 9,
								scale: 5
							},
							hoverAnimation: true,
							label: {
								formatter: "{b}",
								position: "right",
								show: true
							},
							itemStyle: {
								color: "#0efacc",
								shadowBlur: 2,
								shadowColor: "#333"
							},
							zlevel: 1
						}
					]
				},
				visitRecordOption: {
					xAxis: {
						data: [],
						boundaryGap: false,
						axisTick: {
							show: false
						}
					},
					grid: {
						left: 10,
						right: 20,
						top: 30,
						bottom: 0,
						containLabel: true
					},
					tooltip: {
						trigger: 'axis',
						axisPointer: {
							type: 'cross'
						},
						padding: [5, 10]
					},
					yAxis: {
						axisTick: {
							show: false
						}
					},
					legend: {
						top: 0,
						data: ['访问量(PV)', '独立访客(UV)']
					},
					series: [
						{
							name: '访问量(PV)',
							smooth: true,
							type: 'line',
							itemStyle: {
								normal: {
									color: '#FF005A',
									lineStyle: {
										color: '#FF005A',
										width: 2
									}
								}
							},
							data: [],
							animationDuration: 2800,
							animationEasing: 'cubicInOut'
						},
						{
							name: '独立访客(UV)',
							smooth: true,
							type: 'line',
							itemStyle: {
								normal: {
									color: '#3888fa',
									lineStyle: {
										color: '#3888fa',
										width: 2
									},
									areaStyle: {
										color: '#f3f8ff'
									}
								}
							},
							data: [],
							animationDuration: 2800,
							animationEasing: 'quadraticOut'
						}
					]
				},
			}
		},
		computed: {
			metrics() {
				return [
					{key: 'pv', label: '今日 PV', value: this.pv, unit: '次浏览', className: 'pv-icon'},
					{key: 'uv', label: '今日 UV', value: this.uv, unit: '位访客', className: 'uv-icon'},
					{key: 'article', label: '文章总数', value: this.blogCount, unit: '篇文章', className: 'article-icon'},
					{key: 'comment', label: '评论总数', value: this.commentCount, unit: '条评论', className: 'comment-icon'}
				]
			}
		},
		mounted() {
			this.getData()
			window.addEventListener('resize', this.resizeCharts)
		},
		beforeDestroy() {
			window.removeEventListener('resize', this.resizeCharts)
			this.disposeCharts()
		},
		methods: {
			async toggleRanking(type) {
				const expandedKey = `${type}Expanded`
				if (this[expandedKey]) {
					this[expandedKey] = false
					this.$nextTick(() => {
						const chart = type === 'category' ? this.categoryEcharts : this.tagEcharts
						if (chart) chart.resize()
					})
					return
				}
				this[expandedKey] = true
				const rankingKey = `${type}Ranking`
				if (this[rankingKey].length) return
				const loadingKey = `${type}RankingLoading`
				this[loadingKey] = true
				try {
					const res = await getDashboardRanking(type)
					this[rankingKey] = res.data || []
				} catch (error) {
					this[expandedKey] = false
				} finally {
					this[loadingKey] = false
				}
			},
			rankPercent(item, ranking) {
				const max = ranking.length ? Number(ranking[0].value) || 1 : 1
				return `${Math.max((Number(item.value) || 0) / max * 100, 2)}%`
			},
			resizeCharts() {
				this.$nextTick(() => {
					[this.categoryEcharts, this.tagEcharts, this.mapEcharts, this.visitRecordEcharts]
						.forEach(chart => chart && chart.resize())
				})
			},
			disposeCharts() {
				[this.categoryEcharts, this.tagEcharts, this.mapEcharts, this.visitRecordEcharts]
					.forEach(chart => chart && chart.dispose())
			},
			getData() {
				getDashboard().then(res => {
					this.pv = res.data.pv
					this.uv = res.data.uv
					this.blogCount = res.data.blogCount
					this.commentCount = res.data.commentCount
					//横向排行图从下到上递增，因此将后端的降序 Top N 反转后渲染
					const categorySeries = (res.data.category.series || []).slice().reverse()
					this.categorySummary = res.data.category
					this.categoryOption.yAxis.data = categorySeries.map(item => item.name)
					this.categoryOption.series[0].data = categorySeries.map(item => item.value)
					this.initCategoryEcharts()
					const tagSeries = (res.data.tag.series || []).slice().reverse()
					this.tagSummary = res.data.tag
					this.tagOption.yAxis.data = tagSeries.map(item => item.name)
					this.tagOption.series[0].data = tagSeries.map(item => item.value)
					this.initTagEcharts()
					//渲染访客地图数据
					let mapData = this.convertData(res.data.cityVisitor)
					this.mapOption.series[1].data = mapData
					this.mapOption.series[2].data = mapData.splice(0, 5)
					this.initMapEcharts()
					//渲染一周访问量数据
					this.visitRecordOption.xAxis.data = res.data.visitRecord.date
					this.visitRecordOption.series[0].data = res.data.visitRecord.pv
					this.visitRecordOption.series[1].data = res.data.visitRecord.uv
					this.initVisitRecordEcharts()
				}).catch(() => {
					// The request interceptor already displays the server error and redirects
					// stale sessions to /login. Keep the dashboard promise handled.
				})
			},
			initCategoryEcharts() {
				this.categoryEcharts = echarts.init(this.$refs.categoryEcharts, 'light')
				this.categoryEcharts.setOption(this.categoryOption)
			},
			initTagEcharts() {
				this.tagEcharts = echarts.init(this.$refs.tagEcharts, 'light')
				this.tagEcharts.setOption(this.tagOption)
			},
			initMapEcharts() {
				this.mapEcharts = echarts.init(this.$refs.mapEcharts)
				this.mapEcharts.setOption(this.mapOption)
			},
			convertData(data) {
				let res = []
				for (let i = 0; i < data.length; i++) {
					let geoCoord = geoCoordMap[data[i].city]
					if (geoCoord) {
						res.push({
							name: data[i].city,
							value: geoCoord,
							uv: data[i].uv
						})
					}
				}
				return res
			},
			initVisitRecordEcharts() {
				this.visitRecordEcharts = echarts.init(this.$refs.visitRecordEcharts)
				this.visitRecordEcharts.setOption(this.visitRecordOption)
			},
		}
	}
</script>

<style scoped>
	.dashboard-container {
		min-height: calc(100vh - 50px);
		padding: 24px;
		background: #f5f7fa;
	}

	.dashboard-heading {
		display: flex;
		align-items: flex-end;
		justify-content: space-between;
		margin-bottom: 22px;
	}

	.dashboard-heading h2 {
		margin: 0 0 7px;
		color: #1f2d3d;
		font-size: 24px;
	}

	.dashboard-heading p,
	.dashboard-date {
		margin: 0;
		color: #8c98a7;
		font-size: 14px;
	}

	.panel-group {
		margin-bottom: 20px;
	}

	.metrics .el-col,
	.charts .el-col {
		margin-bottom: 20px;
	}

	.metrics {
		margin-bottom: 0;
	}

	.card-panel {
		height: 118px;
		position: relative;
		overflow: hidden;
		border: 0;
		border-radius: 10px;
	}

	.card-panel::after {
		position: absolute;
		right: -24px;
		bottom: -35px;
		width: 90px;
		height: 90px;
		border-radius: 50%;
		background: rgba(52, 211, 153, .06);
		content: '';
	}

	.card-panel-icon-wrapper {
		position: absolute;
		left: 20px;
		top: 24px;
		display: flex;
		align-items: center;
		justify-content: center;
		width: 56px;
		height: 56px;
		border-radius: 14px;
	}

	.card-panel-icon {
		width: 28px;
		height: 28px;
		fill: none;
		stroke: currentColor;
		stroke-width: 1.8;
		stroke-linecap: round;
		stroke-linejoin: round;
	}

	.pv-icon { color: #0ea5e9; background: #e0f2fe; }
	.uv-icon { color: #10b981; background: #d1fae5; }
	.article-icon { color: #8b5cf6; background: #ede9fe; }
	.comment-icon { color: #f59e0b; background: #fef3c7; }

	.card-panel-description {
		margin: 23px 18px 0 94px;
	}

	.card-panel-text {
		margin-bottom: 8px;
		color: #7b8794;
		font-size: 14px;
	}

	.card-panel-num {
		color: #172b4d;
		font-size: 30px;
		font-weight: 700;
		line-height: 1;
	}

	.card-panel-unit {
		margin-left: 7px;
		color: #a0a9b4;
		font-size: 12px;
	}

	.chart-card {
		border-color: #e8edf3;
		border-radius: 10px;
	}

	.chart-card ::v-deep .el-card__body {
		padding: 20px 20px 12px;
	}

	.chart-heading {
		display: flex;
		align-items: baseline;
		justify-content: space-between;
		padding: 0 4px 12px;
		border-bottom: 1px solid #f0f2f5;
	}

	.chart-heading strong {
		color: #27364b;
		font-size: 16px;
	}

	.chart-heading span {
		color: #a0a9b4;
		font-size: 12px;
	}

	.chart {
		height: 350px;
	}

	.distribution-charts .chart {
		height: 350px;
	}

	.distribution-body { height:390px; }
	.ranking-scroll { height:350px; overflow-x:hidden; overflow-y:auto; padding:8px 8px 4px 4px; box-sizing:border-box; scrollbar-width:thin; scrollbar-color:#cbd5e1 transparent; }
	.ranking-scroll::-webkit-scrollbar { width:5px; }
	.ranking-scroll::-webkit-scrollbar-thumb { border-radius:999px; background:#cbd5e1; }
	.ranking-scroll::-webkit-scrollbar-track { background:transparent; }
	.ranking-row { display:flex; min-height:32px; align-items:center; gap:9px; border-bottom:1px solid #f3f5f7; color:#667386; font-size:12px; }
	.ranking-index { width:25px; flex:0 0 25px; color:#a0a9b4; text-align:right; }
	.ranking-row:nth-child(-n+3) .ranking-index { color:#409eff; font-weight:700; }
	.ranking-name { width:132px; flex:0 0 132px; overflow:hidden; color:#465568; text-overflow:ellipsis; white-space:nowrap; }
	.ranking-bar { position:relative; height:7px; flex:1; overflow:hidden; border-radius:999px; background:#eef4fa; }
	.ranking-bar i { display:block; height:100%; border-radius:inherit; background:#49a9ee; }
	.tag-ranking-bar i { background:#67c7c1; }
	.ranking-row strong { width:42px; flex:0 0 42px; color:#536274; text-align:right; }
	.ranking-toggle { display:flex; width:100%; height:40px; align-items:center; justify-content:center; gap:7px; padding:0; border:0; border-top:1px solid #eef1f5; outline:none; background:#fff; color:#8b97a6; font-size:12px; cursor:pointer; transition:color .2s,background .2s; }
	.ranking-toggle:hover { color:#409eff; background:#f7fbff; }
	.ranking-dots { letter-spacing:3px; transform:translateY(-2px); }

	.map-chart {
		height: 420px;
	}

	.trend-chart {
		height: 340px;
	}

	@media (max-width: 768px) {
		.dashboard-container { padding: 16px; }
		.dashboard-date { display: none; }
		.chart,
		.distribution-charts .chart,
		.map-chart { height: 340px; }
		.distribution-body { height:380px; }
		.ranking-scroll { height:340px; }
	}
</style>
