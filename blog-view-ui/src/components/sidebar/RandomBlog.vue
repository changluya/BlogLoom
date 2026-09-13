<template>
	<!--随机文章-->
	<div class="ui segments m-box">
		<div class="ui secondary segment"><i class="bookmark icon"></i>随机文章</div>
		<div class="ui yellow segment content-segment">
			<div v-if="randomBlogList.length" class="ui divided items">
				<div class="m-item" v-for="blog in randomBlogList" :key="blog.id" @click.prevent="toBlog(blog)">
					<div class="img" :style="{'background-image':'url(' + blog.firstPicture + ')'}"></div>
					<div class="info">
						<div class="date">{{ blog.createTime | dateFormat('YYYY-MM-DD') }}</div>
						<div class="title">{{ blog.title }}</div>
					</div>
				</div>
			</div>
			<div v-else class="empty-state">
				<div class="empty-icon"><i class="file alternate outline icon"></i></div>
				<div class="empty-title">暂无随机文章</div>
				<div class="empty-description">发布更多文章后将在这里展示推荐内容</div>
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
			}
		}
	}
</script>

<style scoped>
	.secondary.segment {
		padding: 10px;
	}

	.content-segment {
		padding: 12px !important;
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

	.ui.divided.items .m-item:first-child {
		margin-top: 0;
	}

	.ui.divided.items .m-item {
		margin-top: 1rem;
		height: 7rem;
		position: relative;
		overflow: hidden;
		border-radius: 5px;
		cursor: pointer;
		user-select: none;
	}

	.ui.divided.items .m-item .img {
		position: absolute;
		top: 0;
		left: 0;
		right: 0;
		bottom: 0;
		object-fit: cover;
		background-position-x: center;
		background-position-y: center;
		background-size: cover;
	}

	.ui.divided.items .m-item .info {
		z-index: 1;
		background: linear-gradient(to bottom, rgba(0, 0, 0, 0), rgba(0, 0, 0, 0.8));
		position: absolute;
		left: 0;
		right: 0;
		bottom: 0;
		padding: .5rem !important;
		font-size: 12px;
		color: white;
	}

	.ui.divided.items .m-item .info .title {
		overflow: hidden;
		text-overflow: ellipsis;
		display: -webkit-box;
		-webkit-box-orient: vertical;
		-webkit-line-clamp: 1;
		word-break: break-word;
	}
</style>
