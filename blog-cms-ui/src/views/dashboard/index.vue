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

		<el-row class="panel-group charts" :gutter="20">
			<el-col :xs="24" :lg="6">
				<el-card class="chart-card" shadow="never">
					<div class="chart-heading"><strong>分类分布</strong><span>各分类文章占比</span></div>
					<div ref="categoryEcharts" class="chart"></div>
				</el-card>
			</el-col>
			<el-col :xs="24" :lg="6">
				<el-card class="chart-card" shadow="never">
					<div class="chart-heading"><strong>标签分布</strong><span>各标签文章占比</span></div>
					<div ref="tagEcharts" class="chart"></div>
				</el-card>
			</el-col>
			<el-col :xs="24" :lg="12">
				<el-card class="chart-card" shadow="never">
					<div class="chart-heading"><strong>访客地图</strong><span>访客地域实时分布</span></div>
					<div ref="mapEcharts" class="chart"></div>
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
	import {getDashboard} from "@/api/dashboard";
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
				categoryEcharts: null,
				tagEcharts: null,
				mapEcharts: null,
				visitRecordEcharts: null,
				categoryOption: {
					title: {
						show: false
					},
					tooltip: {
						trigger: 'item',
						formatter: '{a} <br/>{b} : {c} ({d}%)'
					},
					legend: {
						left: 'center',
						bottom: 4,
						data: []
					},
					series: [
						{
							name: '文章数量',
							type: 'pie',
							radius: ['42%', '68%'],
							center: ['50%', '44%'],
							data: []
						}
					]
				},
				tagOption: {
					title: {
						show: false
					},
					tooltip: {
						trigger: 'item',
						formatter: '{a} <br/>{b} : {c} ({d}%)'
					},
					legend: {
						left: 'center',
						bottom: 4,
						data: []
					},
					series: [
						{
							name: '文章数量',
							type: 'pie',
							radius: ['42%', '68%'],
							center: ['50%', '44%'],
							data: []
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
					//渲染分类数据
					this.categoryOption.legend.data = res.data.category.legend
					this.categoryOption.series[0].data = res.data.category.series
					this.initCategoryEcharts()
					//渲染标签数据
					this.tagOption.legend.data = res.data.tag.legend
					this.tagOption.series[0].data = res.data.tag.series
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

	.trend-chart {
		height: 340px;
	}

	@media (max-width: 768px) {
		.dashboard-container { padding: 16px; }
		.dashboard-date { display: none; }
		.chart { height: 320px; }
	}
</style>
