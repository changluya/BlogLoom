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
							<span v-if="item.top" class="blog-top-tag">
								<svg class="top-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M16 9V4h1c.55 0 1-.45 1-1s-.45-1-1-1H7c-.55 0-1 .45-1 1s.45 1 1 1h1v5c0 1.66-1.34 3-3 3v2h5.97v7l1 1 1-1v-7H19v-2c-1.66 0-3-1.34-3-3z"/></svg>置顶
							</span>
							<span><i class="calendar outline icon"></i>{{ item.createTime | dateFormat('YYYY-MM-DD') }}</span>
							<span><i class="eye outline icon"></i>{{ item.views }} 阅读</span>
							<span v-if="item.category"><i class="folder open outline icon"></i>{{ item.category.name }}</span>
							<span><i class="clock outline icon"></i>{{ item.readTime || 0 }} 分钟</span>
						</div>
						<div class="blog-tags">
							<router-link :to="`/tag/${tag.name}`" class="ui mini label m-text-500" :class="tagColor(tag)"
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
			},
			// 标签有配置颜色则用配置；否则按名称稳定取一个颜色，避免全部落在默认灰色
			tagColor(tag) {
				if (tag && tag.color) return tag.color
				const palette = ['red', 'orange', 'green', 'blue', 'violet', 'purple', 'pink', 'teal', 'olive', 'brown', 'black']
				const name = (tag && tag.name) || ''
				let hash = 0
				for (let i = 0; i < name.length; i++) hash = (hash * 31 + name.charCodeAt(i)) >>> 0
				return palette[hash % palette.length]
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
	.blog-tags .label.red { background: #e06a6a !important; }
	.blog-tags .label.orange { background: #e89a4e !important; }
	.blog-tags .label.yellow { background: #e6c85a !important; color: #5a4600 !important; }
	.blog-tags .label.olive { background: #a8bd5c !important; color: #3a440f !important; }
	.blog-tags .label.green { background: #4fbf85 !important; }
	.blog-tags .label.teal { background: #45b3a6 !important; }
	.blog-tags .label.blue { background: #6a9fe0 !important; }
	.blog-tags .label.violet { background: #8f83dd !important; }
	.blog-tags .label.purple { background: #ab7dd0 !important; }
	.blog-tags .label.pink { background: #e585ac !important; }
	.blog-tags .label.brown { background: #ab8a68 !important; }
	.blog-tags .label.grey { background: #9aa5b1 !important; }
	.blog-tags .label.black { background: #55606f !important; }
	.blog-meta { display: flex; align-items: center; justify-content: flex-start; flex-wrap: wrap; gap: 6px 14px; color: #8a9199; font-size: 12px; }
	.blog-meta span { white-space: nowrap; }
	.blog-meta .icon { margin-right: 4px !important; }
	/* 置顶标记：位于发布时间左侧 */
	.blog-meta .blog-top-tag { display: inline-flex; align-items: center; gap: 3px; padding: 1px 7px; border-radius: 4px; background: #fef0f0; color: #f56c6c; font-size: 12px; font-weight: 600; line-height: 1.4; }
	.blog-meta .blog-top-tag .top-icon { width: 12px; height: 12px; flex: 0 0 auto; fill: currentColor; }
</style>

<!-- 移动端样式（独立文件，单独维护） -->
<style scoped src="@/styles/mobile/components/blog-item.mobile.scss" lang="scss"></style>
