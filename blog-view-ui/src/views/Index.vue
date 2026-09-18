<template>
	<div class="site" :class="{'blog-detail-site': $route.name === 'blog'}">
		<!--顶部导航-->
		<Nav :blogName="siteInfo.blogName"/>
		<!--首页大图 只在首页且pc端时显示-->
		<div class="m-mobile-hide">
			<Header v-if="$route.name==='home'"/>
		</div>

		<div class="main">
			<div class="m-padded-tb-big">
				<div class="ui container">
					<div class="ui stackable grid">
						<!--左侧-->
						<div class="three wide column m-mobile-hide">
							<Introduction :class="{'m-display-none':focusMode, 'article-introduction-sticky': $route.name === 'blog'}"/>
						</div>
						<!--中间-->
						<div class="ten wide column">
							<keep-alive include="Home">
								<router-view/>
							</keep-alive>
						</div>
						<!--右侧-->
						<div class="three wide column m-mobile-hide">
							<RandomBlog :randomBlogList="randomBlogList" :class="{'m-display-none':focusMode}"/>
							<Categories :categoryList="categoryList" :class="{'m-display-none':focusMode}"/>
							<Tags :tagList="tagList" :class="{'m-display-none':focusMode}"/>
							<!--只在文章页面显示目录-->
							<Tocbot v-if="$route.name==='blog'"/>
						</div>
					</div>
				</div>
			</div>
		</div>

		<!--私密文章密码对话框-->
		<BlogPasswordDialog/>

		<!--APlayer-->
		<div class="m-mobile-hide">
			<meting-js :server="siteInfo.playlistServer" :id="siteInfo.playlistId" type="playlist" fixed="true" theme="#25CCF7" v-if="siteInfo.playlistServer && siteInfo.playlistId"></meting-js>
		</div>
		<!--回到顶部-->
		<el-backtop style="box-shadow: none;background: none;z-index: 9999;">
			<img src="/img/paper-plane.png" style="width: 40px;height: 40px;">
		</el-backtop>
		<!--底部footer-->
		<Footer :siteInfo="siteInfo" :badges="badges" :newBlogList="newBlogList" :categoryList="categoryList" :introduction="introduction" :hitokoto="hitokotoText" :compact="$route.name === 'column'"/>
	</div>
</template>

<script>
	import {getSite} from '@/api/index'
	import Nav from "@/components/index/Nav";
	import Header from "@/components/index/Header";
	import Footer from "@/components/index/Footer";
	import Introduction from "@/components/sidebar/Introduction";
	import Categories from "@/components/sidebar/Categories";
	import Tags from "@/components/sidebar/Tags";
	import RandomBlog from "@/components/sidebar/RandomBlog";
	import Tocbot from "@/components/sidebar/Tocbot";
	import BlogPasswordDialog from "@/components/index/BlogPasswordDialog";
	import {mapState} from 'vuex'
	import {SAVE_CLIENT_SIZE, SAVE_INTRODUCTION, SAVE_SITE_INFO, RESTORE_COMMENT_FORM} from "@/store/mutations-types";
	import {setFavicon} from '@/util/favicon'

	export default {
		name: "Index",
		components: {Header, BlogPasswordDialog, Tocbot, RandomBlog, Categories, Tags, Nav, Footer, Introduction},
		data() {
			return {
				siteInfo: {
					blogName: '',
					webTitleSuffix: '',
					playlistServer: '',
					playlistId: ''
				},
				categoryList: [],
				tagList: [],
				randomBlogList: [],
				badges: [],
				newBlogList: [],
				hitokotoText: '',
			}
		},
		computed: {
			...mapState(['focusMode', 'introduction'])
		},
		created() {
			this.getSite()
			//从localStorage恢复之前的评论信息
			this.$store.commit(RESTORE_COMMENT_FORM)
		},
		mounted() {
			//保存可视窗口大小
			this.$store.commit(SAVE_CLIENT_SIZE, {clientHeight: document.body.clientHeight, clientWidth: document.body.clientWidth})
			window.onresize = () => {
				this.$store.commit(SAVE_CLIENT_SIZE, {clientHeight: document.body.clientHeight, clientWidth: document.body.clientWidth})
			}
		},
		methods: {
			getSite() {
				getSite().then(res => {
					if (res.code === 200) {
						this.siteInfo = res.data.siteInfo
						this.badges = res.data.badges
						this.newBlogList = res.data.newBlogList
						this.categoryList = res.data.categoryList
						this.tagList = res.data.tagList
						this.randomBlogList = res.data.randomBlogList
						res.data.introduction.totalViews = res.data.totalViews || 0
						res.data.introduction.publishedBlogCount = res.data.publishedBlogCount || 0
						res.data.introduction.totalBlogViews = res.data.totalBlogViews || 0
						this.$store.commit(SAVE_SITE_INFO, this.siteInfo)
						setFavicon(this.siteInfo.favicon)
						this.$store.commit(SAVE_INTRODUCTION, res.data.introduction)
						document.title = this.$route.meta.title + this.siteInfo.webTitleSuffix
						this.hitokotoText = this.pickHitokotoText(this.siteInfo.hitokotoTexts)
					}
				})
			},
			parseHitokotoTexts(value) {
				const texts = []
				const pattern = /"(.*?)"/g
				let match
				while ((match = pattern.exec(value || '')) !== null) texts.push(match[1])
				if (!texts.length && value) texts.push(value)
				return texts
			},
			pickHitokotoText(value) {
				const texts = this.parseHitokotoTexts(value)
				if (!texts.length) return ''
				return texts[Math.floor(Math.random() * texts.length)]
			}
		}
	}
</script>

<style scoped>
	.site {
		display: flex;
		min-height: 100vh; /* 没有元素时，也把页面撑开至100% */
		flex-direction: column;
	}

	.main {
		margin-top: 40px;
		flex: 1;
	}

	.blog-detail-site .main {
		background:
			radial-gradient(circle at 15% 8%, rgba(45, 212, 191, .08), transparent 25%),
			radial-gradient(circle at 86% 15%, rgba(56, 189, 248, .08), transparent 22%),
			linear-gradient(180deg, #f4f7fa 0, #eef2f5 55%, #f3f5f7 100%);
	}

	.main .ui.container {
		width: 1400px !important;
		margin-left: auto !important;
		margin-right: auto !important;
	}

	.ui.grid .three.column {
		padding: 0;
	}

	.ui.grid .ten.column {
		padding-top: 0;
	}

	.m-display-none {
		display: none !important;
	}

	.article-introduction-sticky {
		position: sticky;
		top: 60px;
		max-height: calc(100vh - 76px);
		overflow-x: hidden;
		overflow-y: auto;
		overscroll-behavior: contain;
		scrollbar-width: thin;
		scrollbar-color: #cbd5e1 transparent;
	}

	.article-introduction-sticky::-webkit-scrollbar { width: 5px; }
	.article-introduction-sticky::-webkit-scrollbar-thumb { border-radius: 999px; background: #cbd5e1; }
	.article-introduction-sticky::-webkit-scrollbar-track { background: transparent; }
</style>
