<template>
	<div>
		<article class="ui attached segment m-margin-bottom-big m-box blog-card" v-for="item in blogList" :key="item.id">
			<div class="blog-card-layout" :class="{'without-cover': !item.firstPicture}">
				<a v-if="item.firstPicture" href="javascript:;" class="blog-cover" @click.prevent="toBlog(item)" :aria-label="`阅读文章：${item.title}`">
					<img v-lazy="item.firstPicture" :alt="item.title">
				</a>
				<div class="blog-summary">
					<h2 class="ui header blog-title">
						<a href="javascript:;" @click.prevent="toBlog(item)" class="m-black">{{ item.title }}</a>
					</h2>
					<div class="blog-description" v-html="item.description"></div>
					<div class="blog-footer">
						<div class="blog-meta">
							<span><i class="calendar outline icon"></i>{{ item.createTime | dateFormat('YYYY-MM-DD') }}</span>
							<span><i class="eye outline icon"></i>{{ item.views }} 阅读</span>
							<span v-if="item.category"><i class="folder open outline icon"></i>{{ item.category.name }}</span>
							<span><i class="clock outline icon"></i>{{ item.readTime || 0 }} 分钟</span>
						</div>
						<div class="blog-tags">
							<router-link :to="`/tag/${tag.name}`" class="ui mini label m-text-500" :class="tag.color"
							             v-for="(tag,index) in item.tags" :key="index">{{ tag.name }}</router-link>
						</div>
					</div>
				</div>
			</div>
		</article>
	</div>
</template>

<script>
	export default {
		name: "BlogItem",
		props: {
			blogList: {
				type: Array,
				required: true
			}
		},
		methods: {
			toBlog(blog) {
				this.$store.dispatch('goBlogPage', blog)
			}
		}
	}
</script>

<style scoped>
	.blog-card {
		padding: 18px 20px !important;
		margin-bottom: 14px !important;
		background: rgba(255, 255, 255, .96) !important;
	}

	.blog-card-layout { display: flex; align-items: stretch; gap: 20px; min-width: 0; }
	.blog-cover { flex: 0 0 220px; width: 220px; height: 132px; overflow: hidden; border-radius: 8px; background: #f3f4f6; }
	.blog-cover img { display: block; width: 100%; height: 100%; object-fit: cover; transition: transform .3s ease; }
	.blog-cover:hover img { transform: scale(1.035); }
	.blog-summary { display: flex; flex: 1; min-width: 0; flex-direction: column; }
	.blog-title.ui.header {
		margin: 0 0 9px;
		font-size: 20px;
		font-weight: 700;
		line-height: 1.35;
	}
	.blog-title a:hover { color: #00a7e0 !important; }
	.blog-description { display: -webkit-box; overflow: hidden; color: #6b7280; font-size: 14px; line-height: 1.65; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
	.blog-description ::v-deep p { display: inline; margin: 0; }
	.blog-description ::v-deep img { display: none; }
	.blog-footer { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; margin-top: auto; padding-top: 10px; }
	.blog-tags { display: flex; flex-wrap: wrap; gap: 5px; }
	.blog-tags .label {
		margin: 0 !important;
		padding: 3px 10px !important;
		border-radius: 6px !important;
		background: #6c7a92 !important;
		color: #fff !important;
		font-size: 12px !important;
		font-weight: 500 !important;
		letter-spacing: .2px;
		box-shadow: 0 1px 2px rgba(16, 24, 40, .08);
		transition: opacity .2s, transform .2s;
	}
	.blog-tags .label:hover { opacity: .88; transform: translateY(-1px); }
	.blog-tags .label.red { background: #e05555 !important; }
	.blog-tags .label.orange { background: #e8853a !important; }
	.blog-tags .label.yellow { background: #f0c419 !important; color: #5a4600 !important; }
	.blog-tags .label.olive { background: #9bbf3f !important; color: #33430a !important; }
	.blog-tags .label.green { background: #34b37a !important; }
	.blog-tags .label.teal { background: #26a69a !important; }
	.blog-tags .label.blue { background: #4a90e2 !important; }
	.blog-tags .label.violet { background: #8b7bd8 !important; }
	.blog-tags .label.purple { background: #9b59b6 !important; }
	.blog-tags .label.pink { background: #ec6fa6 !important; }
	.blog-tags .label.brown { background: #a0785a !important; }
	.blog-tags .label.grey { background: #9aa5b1 !important; }
	.blog-tags .label.black { background: #4b5563 !important; }
	.blog-meta { display: flex; align-items: center; justify-content: flex-start; flex-wrap: wrap; gap: 6px 14px; color: #8a9199; font-size: 12px; }
	.blog-meta span { white-space: nowrap; }
	.blog-meta .icon { margin-right: 4px !important; }
</style>

<!-- 移动端样式（独立文件，单独维护） -->
<style scoped src="@/styles/mobile/components/blog-item.mobile.scss" lang="scss"></style>
