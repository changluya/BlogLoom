<template>
	<div>
		<div class="ui top attached segment" style="text-align: center">
			<h2 class="m-text-500">文章归档</h2>
			<p>好! 目前共计 {{ count }} 篇日志。 继续努力。</p>
		</div>
		<div class="ui attached segment">
			<div class="timeline">
				<div class="tl-month" :class="colorObj[index%5]" v-for="(dayMap,key,index) in groupedBlogMap" :key="index">
					<div class="tl-header">
						<a class="ui large label m-text-500">{{ key }}</a>
					</div>
					<div class="tl-day" v-for="(blogs,day) in dayMap" :key="day">
						<div class="tl-day-label">{{ day }}</div>
						<div class="tl-day-body">
							<div class="tl-item" v-for="blog in blogs" :key="blog.id">
								<span class="tl-time">{{ blog.time || '' }}</span>
								<a href="javascript:;" @click.prevent="toBlog(blog)">
									<div class="ui left pointing label tl-title">{{ blog.title }}</div>
								</a>
							</div>
						</div>
					</div>
				</div>

				<div class="tl-header tl-end">
					<a class="ui black large label m-text-500">Hello World!</a>
				</div>
			</div>
		</div>
	</div>
</template>

<script>
	import {getArchives} from "@/api/archives";

	export default {
		name: "Archives",
		data() {
			return {
				blogMap: {},
				count: 0,
				colorObj: {
					0: 'tl-blue',
					1: 'tl-dark',
					2: 'tl-green',
					3: 'tl-purple',
					4: 'tl-red',
				}
			}
		},
		computed: {
			// 按「月份 -> 日期 -> 博客列表」分组，同一天的博客只在左侧显示一次日期
			groupedBlogMap() {
				const result = {}
				Object.keys(this.blogMap).forEach(month => {
					const dayMap = {}
					;(this.blogMap[month] || []).forEach(blog => {
						const day = blog.day || '未知日期'
						if (!dayMap[day]) dayMap[day] = []
						dayMap[day].push(blog)
					})
					result[month] = dayMap
				})
				return result
			}
		},
		created() {
			this.getArchives()
		},
		methods: {
			getArchives() {
				getArchives().then(res => {
					if (res.code === 200) {
						this.blogMap = res.data.blogMap
						this.count = res.data.count
					} else {
						this.msgError(res.msg);
					}
				}).catch(() => {
					this.msgError("请求失败");
				})
			},
			toBlog(blog) {
				this.$store.dispatch('goBlogPage', blog)
			}
		}
	}
</script>

<style scoped>
	.timeline {
		margin: 24px 0;
	}

	.tl-month {
		margin-bottom: 10px;
	}

	.tl-header {
		width: 12em;
		text-align: center;
	}

	.tl-end {
		margin-top: 18px;
	}

	.tl-day {
		display: flex;
		align-items: flex-start;
	}

	.tl-day-label {
		flex: 0 0 4.5em;
		padding: 12px 16px 12px 0;
		color: #6b7684;
		font-size: 13px;
		font-weight: 600;
		text-align: right;
		white-space: nowrap;
	}

	.tl-day-body {
		flex: 1;
		min-width: 0;
		padding: 6px 0 6px 24px;
		border-left: 3px solid #e3e8ef;
	}

	.tl-item {
		position: relative;
		display: flex;
		align-items: center;
		gap: 10px;
		padding: 6px 0;
	}

	.tl-item:before {
		position: absolute;
		top: 50%;
		left: -30px;
		width: 10px;
		height: 10px;
		box-sizing: border-box;
		background: #fff;
		border: 3px solid #e3e8ef;
		border-radius: 50%;
		content: "";
		transform: translateY(-50%);
		transition: all .2s;
	}

	.tl-item:hover:before {
		background: #fff;
	}

	.tl-time {
		flex: 0 0 auto;
		min-width: 42px;
		color: #98a3b1;
		font-size: 12px;
		font-variant-numeric: tabular-nums;
	}

	.tl-title {
		margin: 0 !important;
		padding: 10px 14px !important;
		font-size: 14px !important;
		font-weight: 500 !important;
		letter-spacing: .3px !important;
	}

	.tl-blue .tl-header a, .tl-blue .tl-item .tl-title {
		background: #23b7e5 !important;
		color: #fff !important;
	}

	.tl-blue .tl-day-body {
		border-color: #23b7e5;
	}

	.tl-blue .tl-item:before {
		border-color: #23b7e5;
	}

	.tl-dark .tl-header a, .tl-dark .tl-item .tl-title {
		background: #3a3f51 !important;
		color: #fff !important;
	}

	.tl-dark .tl-day-body {
		border-color: #3a3f51;
	}

	.tl-dark .tl-item:before {
		border-color: #3a3f51;
	}

	.tl-green .tl-header a, .tl-green .tl-item .tl-title {
		background: #27c24c !important;
		color: #fff !important;
	}

	.tl-green .tl-day-body {
		border-color: #27c24c;
	}

	.tl-green .tl-item:before {
		border-color: #27c24c;
	}

	.tl-purple .tl-header a, .tl-purple .tl-item .tl-title {
		background: #7266ba !important;
		color: #fff !important;
	}

	.tl-purple .tl-day-body {
		border-color: #7266ba;
	}

	.tl-purple .tl-item:before {
		border-color: #7266ba;
	}

	.tl-red .tl-header a, .tl-red .tl-item .tl-title {
		background: #f05050 !important;
		color: #fff !important;
	}

	.tl-red .tl-day-body {
		border-color: #f05050;
	}

	.tl-red .tl-item:before {
		border-color: #f05050;
	}
</style>
