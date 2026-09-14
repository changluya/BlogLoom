<template>
	<div class="article-page" :class="{'is-loading': loading, 'is-ready': !loading}" v-loading="loading">
		<div class="ui padded attached segment m-padded-tb-large article-card">
			<div class="ui large red right corner label" v-if="blog.top">
				<i class="arrow alternate circle up icon"></i>
			</div>
			<div class="ui middle aligned mobile reversed stackable">
				<div class="ui grid m-margin-lr">
					<!--标题-->
					<div class="row m-padded-tb-small">
						<h1 class="ui header m-center article-title">{{ blog.title }}</h1>
					</div>
					<!--文章简要信息-->
					<div class="row m-padded-tb-small">
						<div class="ui horizontal link list m-center article-meta">
							<div class="item m-datetime">
								<i class="small calendar icon"></i><span>{{ blog.createTime | dateFormat('YYYY-MM-DD') }}</span>
							</div>
							<div class="item m-views">
								<i class="small eye icon"></i><span>{{ blog.views }}</span>
							</div>
							<div class="item m-common-black">
								<i class="small pencil alternate icon"></i><span>字数≈{{ blog.words }}字</span>
							</div>
							<div class="item m-common-black">
								<i class="small clock icon"></i><span>阅读时长≈{{ blog.readTime }}分</span>
							</div>
							<a class="item m-common-black article-tool" @click.prevent="bigFontSize=!bigFontSize">
								<div data-inverted="" data-tooltip="点击切换字体大小" data-position="top center">
									<i class="font icon"></i>
								</div>
							</a>
							<a class="item m-common-black article-tool" @click.prevent="changeFocusMode">
								<div data-inverted="" data-tooltip="专注模式" data-position="top center">
									<i class="book icon"></i>
								</div>
							</a>
						</div>
					</div>
					<!--分类-->
					<router-link :to="`/category/${blog.category.name}`" class="ui orange large ribbon label" v-if="blog.category">
						<i class="small folder open icon"></i><span class="m-text-500">{{ blog.category.name }}</span>
					</router-link>
					<!--文章Markdown正文-->
					<div class="typo js-toc-content m-padded-tb-small match-braces rainbow-braces article-content" v-lazy-container="{selector: 'img'}" v-viewer :class="{'m-big-fontsize':bigFontSize}" v-html="blog.content"></div>
					<!--赞赏-->
					<div style="margin: 2em auto">
						<el-popover placement="top" width="220" trigger="click" v-if="blog.appreciation">
							<div class="ui orange basic label" style="width: 100%">
								<div class="image">
									<div style="font-size: 12px;text-align: center;margin-bottom: 5px;">一毛是鼓励</div>
									<img :src="$store.state.siteInfo.reward" alt="" class="ui rounded bordered image" style="width: 100%">
									<div style="font-size: 12px;text-align: center;margin-top: 5px;">一块是真爱</div>
								</div>
							</div>
							<el-button slot="reference" class="ui orange inverted circular button m-text-500">赞赏</el-button>
						</el-popover>
					</div>
					<!--横线-->
					<el-divider></el-divider>
					<!--标签-->
					<div class="row m-padded-tb-no">
						<div class="column m-padding-left-no">
							<router-link :to="`/tag/${tag.name}`" class="ui tag label m-text-500 m-margin-small" :class="tag.color" v-for="(tag,index) in blog.tags" :key="index">{{ tag.name }}</router-link>
						</div>
					</div>
				</div>
			</div>
		</div>
		<!--博客信息-->
		<div class="ui attached positive message article-license">
			<ul class="list">
				<li>作者：{{ $store.state.introduction.name }}
					<router-link to="/about">（联系作者）</router-link>
				</li>
				<li>发表时间：{{ blog.createTime | dateFormat('YYYY-MM-DD HH:mm') }}</li>
				<li>最后修改：{{ blog.updateTime | dateFormat('YYYY-MM-DD HH:mm') }}</li>
				<li>本站点采用<a href="https://creativecommons.org/licenses/by/4.0/" target="_blank"> 署名 4.0 国际 (CC BY 4.0) </a>创作共享协议。可自由转载、引用，并且允许商业性使用。但需署名作者且注明文章出处。</li>
			</ul>
		</div>
		<!--评论-->
		<div class="ui bottom teal attached segment threaded comments article-comments">
			<CommentList :page="0" :blogId="blogId" v-if="blog.commentEnabled"/>
			<h3 class="ui header" v-else>评论已关闭</h3>
		</div>
	</div>
</template>

<script>
	import {getBlogById} from "@/api/blog";
	import CommentList from "@/components/comment/CommentList";
	import {mapState} from "vuex";
	import {SET_FOCUS_MODE, SET_IS_BLOG_RENDER_COMPLETE} from '@/store/mutations-types';

	export default {
		name: "Blog",
		components: {CommentList},
		data() {
			return {
				blog: {},
				bigFontSize: false,
				loading: true,
			}
		},
		computed: {
			blogId() {
				return parseInt(this.$route.params.id)
			},
			...mapState(['siteInfo', 'focusMode'])
		},
		beforeRouteEnter(to, from, next) {
			//路由到博客文章页面之前，应将文章的渲染完成状态置为 false
			next(vm => {
				// 当 beforeRouteEnter 钩子执行前，组件实例尚未创建
				// vm 就是当前组件的实例，可以在 next 方法中把 vm 当做 this用
				vm.$store.commit(SET_IS_BLOG_RENDER_COMPLETE, false)
			})
		},
		beforeRouteLeave(to, from, next) {
			this.$store.commit(SET_FOCUS_MODE, false)
			// 从文章页面路由到其它页面时，销毁当前组件的同时，要销毁tocbot实例
			// 否则tocbot一直在监听页面滚动事件，而文章页面的锚点已经不存在了，会报"Uncaught TypeError: Cannot read property 'className' of null"
			tocbot.destroy()
			next()
		},
		beforeRouteUpdate(to, from, next) {
			// 一般有两种情况会触发这个钩子
			// ①当前文章页面跳转到其它文章页面
			// ②点击目录跳转锚点时，路由hash值会改变，导致当前页面会重新加载，这种情况是不希望出现的
			// 在路由 beforeRouteUpdate 中判断路径是否改变
			// 如果跳转到其它页面，to.path!==from.path 就放行 next()
			// 如果是跳转锚点，path不会改变，hash会改变，to.path===from.path, to.hash!==from.path 不放行路由跳转，就能让锚点正常跳转
			if (to.path !== from.path) {
				this.$store.commit(SET_FOCUS_MODE, false)
				//在当前组件内路由到其它博客文章时，要重新获取文章
				this.getBlog(to.params.id)
				//只要路由路径有改变，且停留在当前Blog组件内，就把文章的渲染完成状态置为 false
				this.$store.commit(SET_IS_BLOG_RENDER_COMPLETE, false)
				next()
			}
		},
		created() {
			this.getBlog()
		},
		methods: {
			getBlog(id = this.blogId) {
				this.loading = true
				//密码保护的文章，需要发送密码验证通过后保存在localStorage的Token
				const blogToken = window.localStorage.getItem(`blog${id}`)
				//如果有则发送博主身份Token
				const adminToken = window.localStorage.getItem('adminToken')
				const token = adminToken ? adminToken : (blogToken ? blogToken : '')
				getBlogById(token, id).then(res => {
					if (res.code === 200) {
						this.blog = res.data
						document.title = this.blog.title + this.siteInfo.webTitleSuffix
						//v-html渲染完毕后，渲染代码块样式
						this.$nextTick(() => {
							Prism.highlightAll()
							//将文章渲染完成状态置为 true
							this.$store.commit(SET_IS_BLOG_RENDER_COMPLETE, true)
							this.loading = false
						})
					} else {
						this.loading = false
						this.msgError(res.msg)
					}
				}).catch(() => {
					this.loading = false
					this.msgError("请求失败")
				})
			},
			changeFocusMode() {
				this.$store.commit(SET_FOCUS_MODE, !this.focusMode)
			}
		}
	}
</script>

<style scoped>
	.article-page {
		min-height: 620px;
		border-radius: 12px;
	}

	.article-card {
		padding: 36px 44px 28px !important;
		border: 1px solid rgba(29, 41, 57, .09) !important;
		border-radius: 12px 12px 0 0 !important;
		background: rgba(255, 255, 255, .98) !important;
		box-shadow: 0 8px 30px rgba(15, 23, 42, .07) !important;
	}

	.article-page.is-loading .article-card {
		min-height: 560px;
	}

	.article-page.is-ready .article-card,
	.article-page.is-ready .article-license,
	.article-page.is-ready .article-comments {
		animation: article-in .38s cubic-bezier(.2, .7, .2, 1) both;
	}

	.article-page.is-ready .article-license { animation-delay: .06s; }
	.article-page.is-ready .article-comments { animation-delay: .1s; }

	.article-title {
		margin: 4px auto 2px !important;
		color: #182230 !important;
		font-size: 30px !important;
		font-weight: 700 !important;
		letter-spacing: -.02em;
		line-height: 1.35 !important;
	}

	.article-meta {
		display: flex !important;
		align-items: center;
		justify-content: center;
		flex-wrap: wrap;
		gap: 6px 2px;
		padding: 7px 14px;
		border-radius: 10px;
		background: #f7f9fb;
		color: #748094 !important;
		font-size: 13px;
	}

	.article-meta .item {
		color: #667085 !important;
	}

	.article-tool {
		width: 30px;
		height: 30px;
		padding: 7px !important;
		border-radius: 8px;
		cursor: pointer;
		transition: color .2s ease, background .2s ease;
	}

	.article-tool:hover {
		background: #e9f8fb;
		color: #00a7e0 !important;
	}

	.article-content {
		padding: 30px 8px 20px !important;
		color: #344054;
		font-size: 16px;
		line-height: 1.9;
	}

	.article-content::v-deep p,
	.article-content::v-deep ul,
	.article-content::v-deep ol {
		font-size: 16px;
		line-height: 1.9;
	}

	.article-content::v-deep img {
		margin: 24px auto;
		border-radius: 10px;
		box-shadow: 0 8px 24px rgba(15, 23, 42, .1);
	}

	.article-content::v-deep pre {
		margin: 22px 0;
		border-radius: 10px;
		box-shadow: 0 6px 20px rgba(15, 23, 42, .08);
	}

	.article-license {
		margin-top: 12px !important;
		padding: 20px 24px !important;
		border-color: rgba(23, 183, 165, .16) !important;
		background: #f3fbf9 !important;
		color: #52606d !important;
		line-height: 1.8;
	}

	.article-comments {
		margin-top: 12px !important;
		padding: 26px !important;
		border: 1px solid rgba(29, 41, 57, .09) !important;
		border-radius: 12px !important;
		box-shadow: 0 6px 22px rgba(15, 23, 42, .06) !important;
	}

	.el-divider {
		margin: 1rem 0 !important;
	}

	h1::before, h2::before, h3::before, h4::before, h5::before, h6::before {
		display: block;
		content: " ";
		height: 55px;
		margin-top: -55px;
		visibility: hidden;
	}

	@keyframes article-in {
		from { opacity: 0; transform: translateY(12px); }
		to { opacity: 1; transform: translateY(0); }
	}

	@media (max-width: 768px) {
		.article-page { min-height: 480px; }
		.article-page.is-loading .article-card { min-height: 440px; }
		.article-card { padding: 24px 17px 20px !important; border-radius: 10px !important; }
		.article-title { font-size: 24px !important; }
		.article-meta { padding: 8px; background: transparent; }
		.article-content { padding: 24px 1px 14px !important; }
		.article-content::v-deep p,
		.article-content::v-deep ul,
		.article-content::v-deep ol { font-size: 15px; line-height: 1.85; }
		.article-license, .article-comments { padding: 18px 16px !important; }
	}

	@media (prefers-reduced-motion: reduce) {
		.article-page.is-ready .article-card,
		.article-page.is-ready .article-license,
		.article-page.is-ready .article-comments { animation: none; }
	}
</style>
