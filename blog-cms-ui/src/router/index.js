import Vue from 'vue'
import VueRouter from 'vue-router'
import getPageTitle from '@/util/get-page-title'
import Layout from '@/layout/index.vue'

Vue.use(VueRouter)

const RouterView = {render: h => h('router-view')}

const routes = [
	{
		path: '/404',
		component: () => import('@/views/404'),
		meta: {title: '404 NOT FOUND'},
		hidden: true
	},
	{
		path: '/login',
		component: () => import('@/views/login'),
		meta: {title: '后台管理登录'},
		hidden: true
	},
	{
		path: '/',
		component: Layout,
		redirect: '/dashboard',
		children: [
			{
				path: 'dashboard',
				name: 'Dashboard',
				component: () => import('@/views/dashboard'),
				meta: {title: 'Dashboard', icon: 'el-icon-odometer'}
			}
		]
	},
	{
		path: '/blog',
		name: 'Blog',
		redirect: '/blog/knowledge/manage',
		component: Layout,
		meta: {title: '博客管理', icon: 'el-icon-notebook-2'},
		children: [
			{
				path: 'write',
				name: 'WriteBlog',
				component: () => import('@/views/blog/blog/WriteBlog'),
				meta: {title: '写文章', icon: 'el-icon-edit-outline'},
				hidden: true
			},
			{
				path: 'moment/write',
				name: 'WriteMoment',
				component: () => import('@/views/blog/moment/WriteMoment'),
				meta: {title: '写动态', icon: 'el-icon-chat-line-square'},
				hidden: true
			},
			{
				path: 'edit/:id',
				name: 'EditBlog',
				component: () => import('@/views/blog/blog/WriteBlog'),
				meta: {title: '编辑文章', icon: 'el-icon-edit'},
				hidden: true
			},
			{
				path: 'moment/edit/:id',
				name: 'EditMoment',
				component: () => import('@/views/blog/moment/WriteMoment'),
				meta: {title: '编辑动态', icon: 'el-icon-edit'},
				hidden: true
			},
			{
				path: 'knowledge',
				name: 'Knowledge',
				component: RouterView,
				redirect: '/blog/knowledge/manage',
				meta: {title: '知识库', icon: 'el-icon-folder-opened'},
				children: [
					{
						path: 'manage',
						name: 'KnowledgeManage',
						component: () => import('@/views/knowledge/KnowledgeManage'),
						meta: {title: '知识库管理', icon: 'el-icon-folder-opened'}
					},
					{
						path: 'articles',
						name: 'BlogList',
						component: () => import('@/views/blog/blog/BlogList'),
						meta: {title: '文章管理', icon: 'el-icon-document'}
					}
				]
			},
			{
				path: 'list',
				redirect: '/blog/knowledge/articles',
				hidden: true
			},
			{
				path: 'column/list',
				name: 'ColumnManage',
				component: () => import('@/views/blog/column/ColumnManage'),
				meta: {title: '专栏管理', icon: 'el-icon-collection'}
			},
			{
				path: 'moment/list',
				name: 'MomentList',
				component: () => import('@/views/blog/moment/MomentList'),
				meta: {title: '动态管理', icon: 'el-icon-chat-dot-square'}
			},
			{
				path: 'comment/list',
				name: 'CommentList',
				component: () => import('@/views/blog/comment/CommentList'),
				meta: {title: '评论管理', icon: 'el-icon-chat-round'}
			},
		]
	},
	{
		path: '/page',
		name: 'Page',
		redirect: '/page/site',
		component: Layout,
		meta: {title: '页面管理', icon: 'el-icon-files'},
		children: [
			{
				path: 'site',
				name: 'SiteSetting',
				component: () => import('@/views/page/SiteSetting'),
				meta: {title: '站点设置', icon: 'el-icon-set-up'}
			},
			{
				path: 'friend',
				name: 'FriendList',
				component: () => import('@/views/page/FriendList'),
				meta: {title: '友链管理', icon: 'el-icon-link'}
			},
			{
				path: 'about',
				name: 'About',
				component: () => import('@/views/page/About'),
				meta: {title: '关于我', icon: 'el-icon-user'}
			},
		]
	},
	{
		path: '/pictureHosting',
		name: 'PictureHosting',
		redirect: '/pictureHosting/setting',
		component: Layout,
		meta: {title: '图床管理', icon: 'el-icon-picture-outline'},
		children: [
			{
				path: 'setting',
				name: 'Setting',
				component: () => import('@/views/pictureHosting/Setting'),
				meta: {title: '配置', icon: 'el-icon-setting'}
			},
			{
				path: 'github',
				name: 'GithubManage',
				component: () => import('@/views/pictureHosting/GithubManage'),
				meta: {title: 'GitHub', icon: 'github'}
			},
			{
				path: 'upyun',
				name: 'UpyunManage',
				component: () => import('@/views/pictureHosting/UpyunManage'),
				meta: {title: '又拍云', icon: 'el-icon-upload'}
			},
			{
				path: 'txyun',
				name: 'TxyunManage',
				component: () => import('@/views/pictureHosting/TxyunManage'),
				meta: {title: '腾讯云', icon: 'el-icon-cloudy'}
			},
		]
	},
	{
		path: '/system',
		name: 'System',
		redirect: '/system/job',
		component: Layout,
		meta: {title: '系统管理', icon: 'el-icon-s-tools'},
		children: [
			{
				path: 'job',
				name: 'JobList',
				component: () => import('@/views/system/ScheduleJobList'),
				meta: {title: '定时任务', icon: 'el-icon-alarm-clock'}
			},
		]
	},
	{
		path: '/log',
		name: 'Log',
		redirect: '/log/job',
		component: Layout,
		meta: {title: '日志管理', icon: 'el-icon-document'},
		children: [
			{
				path: 'job',
				name: 'JobLog',
				component: () => import('@/views/log/ScheduleJobLog'),
				meta: {title: '任务日志', icon: 'el-icon-alarm-clock'}
			},
			{
				path: 'login',
				name: 'LoginLog',
				component: () => import('@/views/log/LoginLog'),
				meta: {title: '登录日志', icon: 'el-icon-finished'}
			},
			{
				path: 'operation',
				name: 'OperationLog',
				component: () => import('@/views/log/OperationLog'),
				meta: {title: '操作日志', icon: 'el-icon-document-checked'}
			},
			{
				path: 'exception',
				name: 'ExceptionLog',
				component: () => import('@/views/log/ExceptionLog'),
				meta: {title: '异常日志', icon: 'el-icon-document-delete'}
			},
			{
				path: 'visit',
				name: 'VisitLog',
				component: () => import('@/views/log/VisitLog'),
				meta: {title: '访问日志', icon: 'el-icon-data-line'}
			},
		]
	},
	{
		path: '/statistics',
		name: 'Statistics',
		redirect: '/statistics/visitor',
		component: Layout,
		meta: {title: '数据统计', icon: 'el-icon-s-data'},
		children: [
			{
				path: 'visitor',
				name: 'Visitor',
				component: () => import('@/views/statistics/Visitor'),
				meta: {title: '访客统计', icon: 'el-icon-s-marketing'}
			},
		]
	},

	// 404 page must be placed at the end !!!
	{path: '*', redirect: '/404', hidden: true}
]

const router = new VueRouter({
	mode: 'history',
	base: import.meta.env.BASE_URL,
	routes
})

//挂载路由守卫
router.beforeEach((to, from, next) => {
	if (to.path !== '/login') {
		//获取token
		const tokenStr = window.localStorage.getItem('token')
		if (!tokenStr) return next("/login")
	}
	document.title = getPageTitle(to.meta.title)
	next()
})

export default router
