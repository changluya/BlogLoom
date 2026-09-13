<template>
	<div>
		<article class="ui padded attached segment m-padded-tb-large m-margin-bottom-big m-box blog-card" v-for="item in blogList" :key="item.id">
			<div class="ui large red right corner label" v-if="item.top">
				<i class="arrow alternate circle up icon"></i>
			</div>
			<div class="ui middle aligned mobile reversed stackable">
				<div class="ui grid m-margin-lr">
					<!--标题-->
					<div class="row m-padded-tb-small">
						<h2 class="ui header m-center m-scaleup">
							<a href="javascript:;" @click.prevent="toBlog(item)" class="m-black">{{ item.title }}</a>
						</h2>
					</div>
					<!--文章简要信息-->
					<div class="row m-padded-tb-small">
						<div class="ui horizontal link list m-center">
							<div class="item m-datetime">
								<i class="small calendar icon"></i><span>{{ item.createTime | dateFormat('YYYY-MM-DD')}}</span>
							</div>
							<div class="item m-views">
								<i class="small eye icon"></i><span>{{ item.views }}</span>
							</div>
							<div class="item m-common-black">
								<i class="small pencil alternate icon"></i><span>字数≈{{ item.words }}字</span>
							</div>
							<div class="item m-common-black">
								<i class="small clock icon"></i><span>阅读时长≈{{ item.readTime }}分</span>
							</div>
						</div>
					</div>
					<!--分类-->
					<router-link :to="`/category/${item.category.name}`" class="ui orange large ribbon label">
						<i class="small folder open icon"></i><span class="m-text-500">{{ item.category.name }}</span>
					</router-link>
					<!--文章Markdown描述-->
					<div class="typo m-padded-tb-small line-numbers match-braces rainbow-braces" v-lazy-container="{selector: 'img'}" v-viewer v-html="item.description"></div>
					<!--阅读全文按钮-->
					<div class="row m-padded-tb-small m-margin-top read-more-row">
						<a href="javascript:;" @click.prevent="toBlog(item)" class="read-more-btn" :aria-label="`阅读全文：${item.title}`">
							<span>阅读全文</span>
							<i class="arrow right icon" aria-hidden="true"></i>
						</a>
					</div>
					<!--横线-->
					<div class="ui section divider m-margin-lr-no"></div>
					<!--标签-->
					<div class="row m-padded-tb-no">
						<div class="column m-padding-left-no">
							<router-link :to="`/tag/${tag.name}`" class="ui tag label m-text-500 m-margin-small" :class="tag.color" v-for="(tag,index) in item.tags" :key="index">{{ tag.name }}</router-link>
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
		padding: 26px 30px !important;
		background: rgba(255, 255, 255, .96) !important;
	}

	.blog-card h2.ui.header {
		margin-top: 0;
		font-size: 24px;
		font-weight: 700;
		letter-spacing: -.3px;
	}

	.blog-card .horizontal.list {
		color: #7b8794;
		font-size: 13px;
	}

	.blog-card .typo {
		width: 100%;
		min-height: 74px;
		padding: 18px 10px !important;
		color: #4b5563;
		font-size: 15px;
		line-height: 1.85;
	}

	.blog-card .ui.section.divider {
		margin-top: 24px;
		margin-bottom: 16px;
		border-color: #edf0f3;
	}

	.read-more-row {
		justify-content: center;
	}

	.read-more-btn {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		gap: 8px;
		min-width: 118px;
		height: 40px;
		margin: 0 auto;
		padding: 0 22px;
		border: 1px solid rgba(0, 167, 224, .26);
		border-radius: 10px;
		background: linear-gradient(135deg, #00a7e0 0%, #17b7a5 100%);
		box-shadow: 0 7px 18px rgba(0, 167, 224, .18);
		color: #fff;
		font-size: 14px;
		font-weight: 600;
		letter-spacing: .04em;
		line-height: 1;
		text-decoration: none;
		transition: transform .2s ease, box-shadow .2s ease, filter .2s ease;
	}

	.read-more-btn .icon {
		width: auto;
		margin: 0 !important;
		font-size: 12px;
		transition: transform .2s ease;
	}

	.read-more-btn:hover,
	.read-more-btn:focus-visible {
		color: #fff;
		filter: brightness(1.04);
		transform: translateY(-2px);
		box-shadow: 0 10px 24px rgba(0, 167, 224, .25);
		outline: none;
	}

	.read-more-btn:hover .icon,
	.read-more-btn:focus-visible .icon {
		transform: translateX(3px);
	}

	.read-more-btn:active {
		transform: translateY(0);
		box-shadow: 0 4px 12px rgba(0, 167, 224, .2);
	}

	@media (max-width: 768px) {
		.blog-card { padding: 22px 16px !important; }
		.blog-card h2.ui.header { font-size: 21px; }
		.read-more-btn { min-width: 112px; height: 38px; }
	}
</style>
