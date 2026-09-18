<template>
	<div ref="nav" class="ui fixed inverted stackable pointing menu" :class="{'transparent':$route.name==='home' && clientSize.clientWidth>768}">
		<div class="ui container">
			<router-link to="/">
				<h3 class="ui header item m-blue">{{ blogName }}</h3>
			</router-link>
			<router-link to="/home" class="item" :class="{'m-mobile-hide': mobileHide,'active':$route.name==='home'}">
				<i class="home icon"></i>首页
			</router-link>
			<router-link to="/archives" class="item" :class="{'m-mobile-hide': mobileHide,'active':$route.name==='archives'}">
				<i class="clone icon"></i>归档
			</router-link>
			<router-link to="/moments" class="item" :class="{'m-mobile-hide': mobileHide,'active':$route.name==='moments'}">
				<i class="comment alternate outline icon"></i>动态
			</router-link>
			<router-link to="/friends" class="item" :class="{'m-mobile-hide': mobileHide,'active':$route.name==='friends'}">
				<i class="users icon"></i>友人帐
			</router-link>
			<router-link to="/about" class="item" :class="{'m-mobile-hide': mobileHide,'active':$route.name==='about'}">
				<i class="info icon"></i>关于我
			</router-link>
			<el-autocomplete v-model="queryString" :fetch-suggestions="debounceQuery" placeholder="搜索文章..."
			                 class="right item m-search" :class="{'m-mobile-hide': mobileHide}"
			                 popper-class="m-search-item" aria-label="搜索文章" @select="handleSelect">
				<i class="search icon el-input__icon" slot="suffix"></i>
				<template slot-scope="{ item }">
					<div class="title">{{ item.title }}</div>
					<span class="content">{{ item.content }}</span>
				</template>
			</el-autocomplete>
			<button class="ui menu black icon button m-right-top m-mobile-show" @click="toggle">
				<i class="sidebar icon"></i>
			</button>
		</div>
	</div>
</template>

<script>
	import {getSearchBlogList} from "@/api/blog";
	import {mapState} from 'vuex'

	export default {
		name: "Nav",
		props: {
			blogName: {
				type: String,
				required: true
			},
		},
		data() {
			return {
				mobileHide: true,
				queryString: '',
				queryResult: [],
				timer: null
			}
		},
		computed: {
			...mapState(['clientSize'])
		},
		watch: {
			//路由改变时，收起导航栏
			'$route.path'() {
				this.mobileHide = true
			}
		},
		mounted() {
			//监听页面滚动位置，改变导航栏的显示
			window.addEventListener('scroll', () => {
				//首页且不是移动端
				if (this.$route.name === 'home' && this.clientSize.clientWidth > 768) {
					if (window.scrollY > this.clientSize.clientHeight / 2) {
						this.$refs.nav.classList.remove('transparent')
					} else {
						this.$refs.nav.classList.add('transparent')
					}
				}
			})
			//监听点击事件，收起导航菜单
			document.addEventListener('click', (e) => {
				//遍历冒泡
				let flag = this.$refs.nav.contains(e.target)
				//如果导航栏是打开状态，且点击的元素不是Nav的子元素，则收起菜单
				if (!this.mobileHide && !flag) {
					this.mobileHide = true
				}
			})
		},
		methods: {
			toggle() {
				this.mobileHide = !this.mobileHide
			},
			debounceQuery(queryString, callback) {
				this.timer && clearTimeout(this.timer)
				if (queryString == null || queryString.trim() === '') {
					callback([])
					return
				}
				this.timer = setTimeout(() => this.querySearchAsync(queryString, callback), 1000)
			},
			querySearchAsync(queryString, callback) {
				if (queryString == null
						|| queryString.trim() === ''
						|| queryString.indexOf('%') !== -1
						|| queryString.indexOf('_') !== -1
						|| queryString.indexOf('[') !== -1
						|| queryString.indexOf('#') !== -1
						|| queryString.indexOf('*') !== -1
						|| queryString.trim().length > 20) {
					callback([])
					return
				}
				getSearchBlogList(queryString).then(res => {
					if (res.code === 200) {
						this.queryResult = res.data
						if (this.queryResult.length === 0) {
							this.queryResult.push({title: '无相关搜索结果'})
						}
						callback(this.queryResult)
					} else {
						callback([])
					}
				}).catch(() => {
					callback([])
					this.msgError("请求失败")
				})
			},
			handleSelect(item) {
				if (item.id) {
					this.$router.push(`/blog/${item.id}`)
				}
			}
		}
	}
</script>

<style>
	.ui.fixed.menu .container {
		width: min(1280px, calc(100% - 48px)) !important;
		margin-left: auto !important;
		margin-right: auto !important;
		display: flex !important;
		align-items: center;
	}

	.ui.fixed.menu {
		min-height: 64px;
		border: 0;
		background: rgba(13, 20, 31, .92) !important;
		box-shadow: 0 8px 28px rgba(5, 12, 24, .12);
		transition: background .3s ease, box-shadow .3s ease;
	}

	.ui.inverted.pointing.menu.transparent {
		background: linear-gradient(180deg, rgba(7, 13, 24, .7), rgba(7, 13, 24, .18)) !important;
		box-shadow: none;
		backdrop-filter: blur(8px);
		-webkit-backdrop-filter: blur(8px);
	}

	.ui.inverted.menu .item {
		display: inline-flex;
		align-items: center;
		min-height: 42px;
		margin: 0 2px;
		padding: 0 14px;
		border-radius: 8px;
		font-size: 14px;
		font-weight: 500;
		transition: color .2s ease, background .2s ease;
	}

	.ui.inverted.menu .item::before,
	.ui.inverted.pointing.menu .active.item::after {
		display: none !important;
		content: none !important;
	}

	.ui.inverted.menu .item > i.icon {
		width: auto;
		margin: 0 7px 0 0;
		opacity: .88;
	}

	.ui.inverted.menu .item .caret.icon {
		margin: 0 0 0 4px;
		font-size: 11px;
		opacity: .7;
	}

	.ui.inverted.menu .item:hover,
	.ui.inverted.menu .active.item {
		background: rgba(255, 255, 255, .11) !important;
		color: #fff !important;
	}

	.ui.inverted.menu .header.item {
		min-width: 145px;
		margin: 0 8px 0 0;
		padding-left: 0;
		padding-right: 16px;
		font-size: 17px;
		font-weight: 700;
		letter-spacing: -.3px;
		text-shadow: 0 2px 12px rgba(0, 0, 0, .28);
	}

	.ui.inverted.menu .header.item:hover {
		background: transparent !important;
	}

	.el-dropdown {
		display: inline-flex;
		align-self: stretch;
		align-items: center;
	}

	.ui.inverted.pointing.menu.transparent .active.item:after {
		background: transparent !important;
		transition: .3s ease-out;
	}

	.ui.inverted.pointing.menu.transparent .active.item:hover:after {
		background: transparent !important;
	}

	.el-dropdown-link {
		outline-style: none !important;
		outline-color: unset !important;
		height: 100%;
		cursor: pointer;
	}

	.el-dropdown-menu {
		margin: 7px 0 0 0 !important;
		padding: 0 !important;
		border: 0 !important;
		background: #1b1c1d !important;
	}

	.el-dropdown-menu__item {
		padding: 0 15px !important;
		color: rgba(255, 255, 255, .9) !important;
	}

	.el-dropdown-menu__item:hover {
		background: rgba(255, 255, 255, .08) !important;
	}

	.el-popper .popper__arrow::after {
		content: none !important;
	}

	.popper__arrow {
		display: none !important;
	}

	.m-search {
		min-width: 210px;
		min-height: 38px !important;
		margin: 0 0 0 auto !important;
		padding: 0 !important;
		border: 1px solid rgba(255, 255, 255, .24) !important;
		border-radius: 999px !important;
		background: rgba(255, 255, 255, .1) !important;
		transition: border-color .2s ease, background .2s ease, box-shadow .2s ease;
	}

	.m-search:focus-within {
		border-color: rgba(70, 216, 255, .8) !important;
		background: rgba(8, 18, 32, .55) !important;
		box-shadow: 0 0 0 3px rgba(70, 216, 255, .12);
	}

	.m-search input {
		height: 38px;
		color: #fff;
		border: 0 !important;
		border-radius: 999px;
		background: transparent !important;
		padding: 0 40px 0 16px;
	}

	.m-search input::placeholder {
		color: rgba(255, 255, 255, .7);
	}

	.m-search i {
		color: rgba(255, 255, 255, .9) !important;
		line-height: 38px !important;
	}

	.m-search-item {
		min-width: 350px !important;
	}

	.m-search-item li {
		line-height: normal !important;
		padding: 8px 10px !important;
	}

	.m-search-item li .title {
		text-overflow: ellipsis;
		overflow: hidden;
		color: rgba(0, 0, 0, 0.87);
	}

	.m-search-item li .content {
		text-overflow: ellipsis;
		font-size: 12px;
		color: rgba(0, 0, 0, .70);
	}

</style>

<!-- 移动端样式（独立文件，单独维护） -->
<style src="@/styles/mobile/components/nav.mobile.scss" lang="scss"></style>
