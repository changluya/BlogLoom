<template>
	<div class="article-page" :class="{'is-loading': loading, 'is-ready': !loading}" v-loading="loading">
		<div class="ui padded attached segment m-padded-tb-large article-card">
			<header class="article-header">
				<router-link :to="`/category/${blog.category.name}`" class="article-category" v-if="blog.category">
					<i class="small folder open icon"></i><span>{{ blog.category.name }}</span>
				</router-link>
				<h1 class="article-title">{{ blog.title }}</h1>
				<div class="article-meta">
					<div class="meta-item m-datetime">
						<i class="small calendar outline icon"></i><span>{{ blog.createTime | dateFormat('YYYY-MM-DD') }}</span>
					</div>
					<div class="meta-item m-views">
						<i class="small eye outline icon"></i><span>{{ blog.views }} 次阅读</span>
					</div>
					<div class="meta-item">
						<i class="small pencil alternate icon"></i><span>{{ blog.words }} 字</span>
					</div>
					<div class="meta-item">
						<i class="small clock outline icon"></i><span>约 {{ blog.readTime }} 分钟</span>
					</div>
					<div class="article-tools">
						<button type="button" class="article-tool" :class="{'is-active': bigFontSize}" @click="bigFontSize=!bigFontSize" data-tooltip="切换字体大小" data-position="top center" aria-label="切换字体大小">
							<i class="font icon"></i>
						</button>
						<button type="button" class="article-tool" :class="{'is-active': focusMode}" @click="changeFocusMode" data-tooltip="专注模式" data-position="top center" aria-label="切换专注模式">
							<i class="book open icon"></i>
						</button>
					</div>
				</div>
			</header>

			<!--文章Markdown正文-->
			<div class="typo js-toc-content match-braces rainbow-braces article-content" v-lazy-container="{selector: 'img'}" v-viewer :class="{'m-big-fontsize':bigFontSize}" v-html="blog.content"></div>

			<div class="article-footer">
				<div class="article-tags" v-if="blog.tags && blog.tags.length">
					<router-link :to="`/tag/${tag.name}`" class="article-tag" v-for="(tag,index) in blog.tags" :key="index">
						<i class="tag icon"></i>{{ tag.name }}
					</router-link>
				</div>
				<el-popover placement="top" width="220" trigger="click" v-if="blog.appreciation">
					<div class="ui orange basic label" style="width: 100%">
						<div class="image">
							<div style="font-size: 12px;text-align: center;margin-bottom: 5px;">一毛是鼓励</div>
							<img :src="$store.state.siteInfo.reward" alt="赞赏码" class="ui rounded bordered image" style="width: 100%">
							<div style="font-size: 12px;text-align: center;margin-top: 5px;">一块是真爱</div>
						</div>
					</div>
					<el-button slot="reference" class="reward-button" size="small" round><i class="heart outline icon"></i>赞赏</el-button>
				</el-popover>
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
			removeTocPlaceholder(content) {
				if (typeof content !== 'string') {
					return content
				}
				return content.replace(/<p>\s*\[toc\]\s*<\/p>/gi, '')
			},
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
						this.blog.content = this.removeTocPlaceholder(this.blog.content)
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
		--article-accent: #0ea5b7;
		--article-accent-dark: #087f8c;
		--article-ink: #26354a;
		--article-muted: #7a8799;
		--article-border: #e7edf3;
		border-radius: 18px;
	}

	.article-card {
		position: relative;
		overflow: hidden;
		padding: 0 48px 32px !important;
		border: 1px solid rgba(148, 163, 184, .2) !important;
		border-radius: 18px !important;
		background: rgba(255, 255, 255, .98) !important;
		box-shadow: 0 18px 55px rgba(30, 41, 59, .08) !important;
	}

	.article-card::before {
		position: absolute;
		top: 0;
		left: 0;
		width: 100%;
		height: 4px;
		content: "";
		background: linear-gradient(90deg, #0ea5b7, #38bdf8 55%, #818cf8);
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

	.article-header {
		position: relative;
		padding: 44px 8px 30px;
		border-bottom: 1px solid var(--article-border);
		text-align: center;
	}

	.article-category {
		display: inline-flex;
		align-items: center;
		gap: 5px;
		margin-bottom: 17px;
		padding: 7px 13px;
		border: 1px solid rgba(14, 165, 183, .14);
		border-radius: 999px;
		background: rgba(14, 165, 183, .08);
		color: var(--article-accent-dark);
		font-size: 13px;
		font-weight: 600;
		transition: background .2s ease, transform .2s ease;
	}

	.article-category:hover {
		background: rgba(14, 165, 183, .14);
		color: var(--article-accent-dark);
		transform: translateY(-1px);
	}

	.article-title {
		max-width: 720px;
		margin: 0 auto 20px !important;
		color: #1e293b !important;
		font-size: 32px !important;
		font-weight: 750 !important;
		letter-spacing: -.025em;
		line-height: 1.35 !important;
	}

	.article-meta {
		display: flex;
		align-items: center;
		justify-content: center;
		flex-wrap: wrap;
		gap: 8px 18px;
		color: var(--article-muted);
		font-size: 13px;
	}

	.meta-item {
		display: inline-flex;
		align-items: center;
		gap: 4px;
		white-space: nowrap;
	}

	.meta-item i {
		margin: 0 !important;
		color: #94a3b8;
	}

	.article-tools {
		display: inline-flex;
		gap: 5px;
		padding-left: 12px;
		border-left: 1px solid #dce4ec;
	}

	.article-tool {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 29px;
		height: 29px;
		padding: 0;
		border: 0;
		border-radius: 8px;
		background: #f1f5f9;
		color: #718096;
		cursor: pointer;
		transition: color .2s ease, background .2s ease, transform .2s ease;
	}

	.article-tool i {
		margin: 0 !important;
	}

	.article-tool:hover,
	.article-tool.is-active {
		background: rgba(14, 165, 183, .12);
		color: var(--article-accent-dark);
		transform: translateY(-1px);
	}

	.article-content {
		padding: 38px 8px 32px !important;
		color: var(--article-ink);
		font-size: 16px;
		line-height: 1.95;
	}

	.article-content::v-deep p,
	.article-content::v-deep ul,
	.article-content::v-deep ol {
		font-size: 16px;
		line-height: 1.95;
	}

	.article-content::v-deep p {
		margin: 1.15em 0;
		text-align: left;
	}

	.article-content::v-deep h1,
	.article-content::v-deep h2,
	.article-content::v-deep h3,
	.article-content::v-deep h4 {
		color: #26354a;
		font-weight: 700;
		letter-spacing: -.015em;
	}

	.article-content::v-deep h2 {
		position: relative;
		margin: 2.2em 0 .8em;
		padding: 0 0 11px 17px;
		border-bottom: 1px solid var(--article-border);
		font-size: 27px;
	}

	.article-content::v-deep h2::after {
		position: absolute;
		top: 6px;
		bottom: 16px;
		left: 0;
		width: 4px;
		border-radius: 3px;
		content: "";
		background: linear-gradient(180deg, #14b8a6, #38bdf8);
	}

	.article-content::v-deep h3 {
		margin: 1.8em 0 .7em;
		font-size: 21px;
	}

	.article-content::v-deep ul,
	.article-content::v-deep ol {
		padding-left: 1.55em;
	}

	.article-content::v-deep ul {
		list-style: disc;
	}

	.article-content::v-deep ol {
		list-style: decimal;
	}

	.article-content::v-deep li {
		margin: .35em 0;
		padding-left: .25em;
	}

	.article-content::v-deep a {
		color: #0891a2;
		font-weight: 500;
	}

	.article-content::v-deep blockquote {
		margin: 24px 0;
		padding: 16px 20px;
		border: 0;
		border-left: 4px solid #2dd4bf;
		border-radius: 0 10px 10px 0;
		background: linear-gradient(90deg, rgba(45, 212, 191, .09), rgba(56, 189, 248, .03));
		color: #5f6f82;
	}

	.article-content::v-deep code:not([class*="language-"]) {
		padding: 2px 6px;
		border: 1px solid #e2e8f0;
		border-radius: 5px;
		background: #f6f8fa;
		color: #d24b68;
		font-size: .9em;
	}

	.article-content::v-deep img {
		margin: 28px auto;
		border: 1px solid rgba(148, 163, 184, .18);
		border-radius: 12px;
		box-shadow: 0 12px 30px rgba(15, 23, 42, .1);
	}

	.article-content::v-deep pre {
		margin: 24px 0;
		border: 1px solid rgba(148, 163, 184, .15);
		border-radius: 12px;
		box-shadow: 0 8px 24px rgba(15, 23, 42, .08);
	}

	.article-content::v-deep table {
		display: block;
		overflow-x: auto;
		width: 100%;
		margin: 24px 0;
		border: 1px solid var(--article-border);
		border-radius: 10px;
	}

	.article-content::v-deep th {
		background: #f4f8fb;
		color: #475569;
	}

	.article-footer {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 16px;
		padding: 22px 8px 2px;
		border-top: 1px solid var(--article-border);
	}

	.article-tags {
		display: flex;
		flex-wrap: wrap;
		gap: 8px;
	}

	.article-tag {
		display: inline-flex;
		align-items: center;
		padding: 6px 11px;
		border-radius: 7px;
		background: #f1f5f9;
		color: #64748b;
		font-size: 12px;
		font-weight: 600;
	}

	.article-tag:hover {
		background: rgba(14, 165, 183, .11);
		color: var(--article-accent-dark);
	}

	.reward-button {
		border-color: rgba(14, 165, 183, .24) !important;
		background: #fff !important;
		color: var(--article-accent-dark) !important;
	}

	.article-license {
		margin-top: 16px !important;
		padding: 21px 26px !important;
		border: 1px solid rgba(14, 165, 183, .12) !important;
		border-radius: 14px !important;
		background: linear-gradient(135deg, #f2fbfb, #f5f9fd) !important;
		box-shadow: none !important;
		color: #607083 !important;
		line-height: 1.8;
	}

	.article-license a {
		color: var(--article-accent-dark) !important;
	}

	.article-comments {
		margin-top: 16px !important;
		padding: 28px !important;
		border: 1px solid rgba(148, 163, 184, .2) !important;
		border-radius: 14px !important;
		box-shadow: 0 10px 34px rgba(30, 41, 59, .06) !important;
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
		.article-card { padding: 0 18px 22px !important; border-radius: 12px !important; }
		.article-header { padding: 31px 0 24px; }
		.article-category { margin-bottom: 13px; }
		.article-title { margin-bottom: 16px !important; font-size: 25px !important; }
		.article-meta { gap: 7px 12px; }
		.article-tools { width: 100%; justify-content: center; padding: 7px 0 0; border: 0; }
		.article-content { padding: 26px 0 20px !important; }
		.article-content::v-deep p,
		.article-content::v-deep ul,
		.article-content::v-deep ol { font-size: 15px; line-height: 1.85; }
		.article-content::v-deep h2 { padding-left: 13px; font-size: 23px; }
		.article-content::v-deep h3 { font-size: 19px; }
		.article-footer { align-items: flex-start; flex-direction: column; padding-right: 0; padding-left: 0; }
		.article-license, .article-comments { padding: 18px 16px !important; }
	}

	@media (prefers-reduced-motion: reduce) {
		.article-page.is-ready .article-card,
		.article-page.is-ready .article-license,
		.article-page.is-ready .article-comments { animation: none; }
	}
</style>
