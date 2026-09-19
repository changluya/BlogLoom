<template>
	<div class="home-page">
		<!--工具栏：左侧排序，右侧搜索-->
		<div class="home-toolbar">
			<div class="home-filters">
				<span class="filter-tab" :class="{active: sort === 'top' && !searching}" @click="changeSort('top')">
					<svg class="filter-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M16 9V4h1c.55 0 1-.45 1-1s-.45-1-1-1H7c-.55 0-1 .45-1 1s.45 1 1 1h1v5c0 1.66-1.34 3-3 3v2h5.97v7l1 1 1-1v-7H19v-2c-1.66 0-3-1.34-3-3z"/></svg>置顶文章
				</span>
				<span class="filter-tab" :class="{active: sort === 'createTime' && !searching}" @click="changeSort('createTime')">
					<i class="clock outline icon"></i>按发布时间
				</span>
				<span class="filter-tab" :class="{active: sort === 'views' && !searching}" @click="changeSort('views')">
					<i class="fire icon"></i>按访问量
				</span>
			</div>
			<div class="home-search">
				<el-input v-model="keyword" size="small" placeholder="搜索文章..." clearable prefix-icon="el-icon-search"
				          @keyup.enter.native="doSearch" @clear="clearSearch"></el-input>
			</div>
		</div>

		<!--搜索结果-->
		<div v-if="searching">
			<div class="search-summary">
				<span>搜索「{{ lastKeyword }}」，共 {{ searchResults.length }} 条</span>
				<el-button type="text" @click="clearSearch">返回全部</el-button>
			</div>
			<div v-if="!searchResults.length" class="search-empty">无相关结果</div>
			<a v-for="item in searchResults" :key="item.id" href="javascript:;" class="search-hit" @click.prevent="toBlog(item)">
				<div class="search-hit-title">{{ item.title }}</div>
				<div class="search-hit-content">{{ strip(item.content) }}</div>
			</a>
		</div>

		<!--博客列表-->
		<BlogList v-else :key="sort" :getBlogList="getBlogList" :blogList="blogList" :totalPage="totalPage"/>
	</div>
</template>

<script>
	import BlogList from "@/components/blog/BlogList";
	import {getBlogList} from "@/api/home";
	import {getSearchBlogList} from "@/api/blog";
	import {SET_IS_BLOG_TO_HOME} from "../../store/mutations-types";

	export default {
		name: "Home",
		components: {BlogList},
		data() {
			return {
				blogList: [],
				totalPage: 0,
				getBlogListFinish: false,
				sort: '',
				keyword: '',
				lastKeyword: '',
				searching: false,
				searchResults: []
			}
		},
		beforeRouteEnter(to, from, next) {
			next(vm => {
				if (from.name !== 'blog') {
					//其它页面跳转到首页时，重新请求数据
					//设置一个flag，让首页的分页组件指向正确的页码
					vm.$store.commit(SET_IS_BLOG_TO_HOME, false)
					vm.getBlogList()
				} else {
					//如果文章页面是起始访问页，首页将是第一次进入，即缓存不存在，要请求数据
					if (!vm.getBlogListFinish) {
						vm.getBlogList()
					}
					//从文章页面跳转到首页时，使用首页缓存
					vm.$store.commit(SET_IS_BLOG_TO_HOME, true)
				}
			})
		},
		methods: {
			getBlogList(pageNum) {
				getBlogList(pageNum, this.sort).then(res => {
					if (res.code === 200) {
						this.blogList = res.data.list
						this.totalPage = res.data.totalPage
						this.$nextTick(() => {
							Prism.highlightAll()
						})
						this.getBlogListFinish = true
					} else {
						this.msgError(res.msg)
					}
				}).catch(() => {
					this.msgError("请求失败")
				})
			},
			changeSort(sort) {
				// 再次点击已选中的选项则取消选中，回到默认排序（置顶优先、更新时间靠前）
				const next = this.sort === sort ? '' : sort
				if (this.sort === next && !this.searching) return
				this.sort = next
				this.clearSearch()
				this.$store.commit(SET_IS_BLOG_TO_HOME, false)
				this.getBlogList()
			},
			doSearch() {
				const q = (this.keyword || '').trim()
				if (!q) return this.clearSearch()
				getSearchBlogList(q).then(res => {
					if (res.code === 200) {
						this.searchResults = res.data || []
						this.lastKeyword = q
						this.searching = true
					} else {
						this.msgError(res.msg)
					}
				}).catch(() => {
					this.msgError("请求失败")
				})
			},
			clearSearch() {
				this.searching = false
				this.searchResults = []
				this.lastKeyword = ''
				this.keyword = ''
			},
			strip(html) {
				if (!html) return ''
				return html
					.replace(/<[^>]+>/g, '')
					.replace(/[#*`>]/g, ' ')
					.replace(/\s+/g, ' ')
					.trim()
					.slice(0, 120)
			},
			toBlog(blog) {
				this.$store.dispatch('goBlogPage', blog)
			}
		}
	}
</script>

<style scoped>
	/* 桌面端：作为中栏弹性容器，让列表区撑满剩余高度 */
	@media (min-width: 768px) {
		.home-page {
			display: flex;
			flex: 1;
			flex-direction: column;
			min-height: 0;
		}
	}

	.home-toolbar {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 16px;
		margin-bottom: 14px;
		padding: 10px 16px;
		border-radius: 8px;
		background: rgba(255, 255, 255, .96);
		box-shadow: 0 1px 4px rgba(5, 12, 24, .06);
	}

	.home-filters {
		display: flex;
		align-items: center;
		gap: 6px;
	}

	.filter-tab {
		display: inline-flex;
		align-items: center;
		gap: 5px;
		padding: 6px 14px;
		border-radius: 6px;
		color: #5b6470;
		font-size: 13px;
		cursor: pointer;
		transition: color .2s, background .2s;
	}

	.filter-tab:hover {
		color: #00a7e0;
		background: #f0f8ff;
	}

	.filter-tab.active {
		color: #fff;
		background: #00a7e0;
	}

	.filter-tab .icon {
		margin: 0 !important;
	}

	.filter-icon {
		width: 1em;
		height: 1em;
		flex: 0 0 auto;
		fill: currentColor;
	}

	.home-search {
		width: 220px;
	}

	.search-summary {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 10px;
		color: #7b8491;
		font-size: 13px;
	}

	.search-empty {
		padding: 40px 0;
		color: #98a2b3;
		text-align: center;
	}

	.search-hit {
		display: block;
		padding: 14px 18px;
		margin-bottom: 10px;
		border-radius: 8px;
		background: rgba(255, 255, 255, .96);
		box-shadow: 0 1px 4px rgba(5, 12, 24, .06);
		transition: box-shadow .2s, transform .2s;
	}

	.search-hit:hover {
		box-shadow: 0 4px 14px rgba(5, 12, 24, .09);
		transform: translateY(-1px);
	}

	.search-hit-title {
		margin-bottom: 6px;
		color: #303846;
		font-size: 16px;
		font-weight: 600;
	}

	.search-hit:hover .search-hit-title {
		color: #00a7e0;
	}

	.search-hit-content {
		color: #7b8491;
		font-size: 13px;
		line-height: 1.6;
	}
</style>

<!-- 移动端样式（独立文件，单独维护） -->
<style scoped src="@/styles/mobile/pages/home.mobile.scss" lang="scss"></style>
