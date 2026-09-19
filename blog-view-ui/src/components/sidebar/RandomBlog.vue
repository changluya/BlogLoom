<template>
	<!--推荐文章-->
	<div class="ui segments m-box">
		<div class="ui secondary segment"><i class="bookmark icon"></i>推荐文章</div>
		<div class="ui yellow segment content-segment">
			<ul v-if="randomBlogList.length" class="recommend-list">
				<li v-for="(blog, index) in randomBlogList" :key="blog.id" @click="toBlog(blog)" :title="blog.title">
					<span class="rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
					<span class="title">{{ blog.title }}</span>
					<span class="views"><i class="eye outline icon"></i>{{ formatViews(blog.views) }}</span>
				</li>
			</ul>
			<div v-else class="empty-state">
				<div class="empty-icon"><i class="file alternate outline icon"></i></div>
				<div class="empty-title">暂无推荐文章</div>
				<div class="empty-description">将文章设为「推荐」后将在这里展示</div>
			</div>
		</div>
	</div>
</template>

<script>
	export default {
		name: "RandomBlog",
		props: {
			randomBlogList: {
				type: Array,
				required: true
			},
		},
		methods: {
			toBlog(blog) {
				this.$store.dispatch('goBlogPage', blog)
			},
			formatViews(value) {
				const count = Number(value) || 0
				return count.toLocaleString('zh-CN')
			}
		}
	}
</script>

<style scoped>
	.secondary.segment {
		padding: 10px;
	}

	.content-segment {
		padding: 6px 12px !important;
		max-height: 210px;
		overflow-y: auto;
		overscroll-behavior: contain;
	}

	.content-segment::-webkit-scrollbar { width: 5px; }
	.content-segment::-webkit-scrollbar-thumb { border-radius: 999px; background: #cbd5e1; }
	.content-segment::-webkit-scrollbar-track { background: transparent; }

	.recommend-list {
		margin: 0;
		padding: 0;
		list-style: none;
	}

	.recommend-list li {
		display: flex;
		align-items: flex-start;
		gap: 8px;
		padding: 8px 2px;
		border-bottom: 1px dashed #eef0f3;
		cursor: pointer;
	}

	.recommend-list li:last-child {
		border-bottom: none;
	}

	.rank {
		flex: 0 0 18px;
		height: 18px;
		line-height: 18px;
		margin-top: 1px;
		border-radius: 4px;
		background: #eef2f7;
		color: #8a94a3;
		font-size: 12px;
		font-weight: 600;
		text-align: center;
	}

	.rank-1 { background: #f5222d; color: #fff; }
	.rank-2 { background: #fa8c16; color: #fff; }
	.rank-3 { background: #facc15; color: #5a4600; }

	.title {
		flex: 1;
		min-width: 0;
		color: #4b5563;
		font-size: 13px;
		line-height: 1.5;
		overflow: hidden;
		text-overflow: ellipsis;
		display: -webkit-box;
		-webkit-box-orient: vertical;
		-webkit-line-clamp: 2;
		word-break: break-word;
		transition: color .2s;
	}

	.views {
		flex: 0 0 auto;
		display: inline-flex;
		align-items: center;
		margin-top: 2px;
		color: #a0a8b3;
		font-size: 11px;
		white-space: nowrap;
	}

	.views .icon { margin: 0 2px 0 0 !important; }

	.recommend-list li:hover .title {
		color: #00a7e0;
	}

	.empty-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		min-height: 118px;
		padding: 12px 6px;
		color: #98a2b3;
		text-align: center;
	}

	.empty-icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 42px;
		height: 42px;
		margin-bottom: 10px;
		border-radius: 50%;
		background: #f3f6f9;
		color: #9aa8b8;
		font-size: 18px;
	}

	.empty-icon i { margin: 0; }
	.empty-title { color: #606b79; font-size: 14px; font-weight: 600; }
	.empty-description { margin-top: 5px; font-size: 12px; line-height: 1.6; }
</style>
