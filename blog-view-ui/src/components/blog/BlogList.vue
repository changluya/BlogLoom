<template>
	<div class="blog-list">
		<!--空状态-->
		<div v-if="!blogList.length" class="blog-empty">
			<div class="blog-empty-icon"><i class="file alternate outline icon"></i></div>
			<h3 class="blog-empty-title">{{ emptyText }}</h3>
			<p class="blog-empty-desc">{{ emptyDesc }}</p>
			<el-button size="small" icon="el-icon-refresh" @click="refresh">刷新看看</el-button>
		</div>
		<!--content-->
		<template v-else>
			<BlogItem :blogList="blogList"/>
			<!--分页-->
			<Pagination :getBlogList="getBlogList" :totalPage="totalPage"/>
		</template>
	</div>
</template>

<script>
	import BlogItem from "./BlogItem";
	import Pagination from "./Pagination";

	export default {
		name: "BlogList",
		components: {Pagination, BlogItem},
		props: {
			getBlogList: {
				type: Function,
				required: true
			},
			blogList: {
				type: Array,
				required: true
			},
			totalPage: {
				type: Number,
				required: true
			},
			emptyText: {
				type: String,
				default: '这里还没有文章'
			},
			emptyDesc: {
				type: String,
				default: '博主还在努力创作中，敬请期待～'
			}
		},
		methods: {
			refresh() {
				this.getBlogList(1)
			}
		}
	}
</script>

<style scoped>
	/* 桌面端：列表区撑满中栏剩余高度，空卡片随之与左侧专栏区等高 */
	@media (min-width: 768px) {
		.blog-list {
			display: flex;
			flex: 1;
			flex-direction: column;
			min-height: 0;
		}

		.blog-list .blog-empty {
			flex: 1;
		}
	}

	.blog-empty {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 64px 24px;
		text-align: center;
		border-radius: 8px;
		background: rgba(255, 255, 255, .96);
		box-shadow: 0 1px 4px rgba(5, 12, 24, .06);
	}

	.blog-empty-icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 84px;
		height: 84px;
		margin-bottom: 18px;
		border-radius: 50%;
		background: #f0f8ff;
	}

	.blog-empty-icon .icon {
		margin: 0 !important;
		color: #00a7e0;
		font-size: 34px;
	}

	.blog-empty-title {
		margin: 0 0 8px;
		color: #303846;
		font-size: 18px;
		font-weight: 600;
	}

	.blog-empty-desc {
		margin: 0 0 18px;
		color: #98a2b3;
		font-size: 13px;
		line-height: 1.6;
	}

	@media (max-width: 768px) {
		.blog-empty {
			padding: 44px 18px;
		}

		.blog-empty-icon {
			width: 68px;
			height: 68px;
			margin-bottom: 14px;
		}

		.blog-empty-icon .icon {
			font-size: 28px;
		}

		.blog-empty-title {
			font-size: 16px;
		}
	}
</style>
