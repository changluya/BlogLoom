<template>
	<div class="column-page">
		<div class="ui segment column-header">
			<div class="header-main">
				<img v-if="column.cover && !imageError" :src="column.cover" @error="imageError=true">
				<span v-else class="header-logo"><i class="el-icon-collection"></i></span>
				<div class="header-info">
					<div class="column-label">博客专栏</div>
					<h2>{{ column.name || '专栏' }}</h2>
					<p>{{ column.description || '暂无专栏简介' }}</p>
				</div>
			</div>
		</div>
		<BlogList :getBlogList="getBlogList" :blogList="blogList" :totalPage="totalPage"/>
	</div>
</template>

<script>
import BlogList from '@/components/blog/BlogList'
import {getColumnDetail, getColumnBlogs} from '@/api/column'
export default {
	name: 'Column', components: {BlogList},
	data() { return {column: {}, blogList: [], totalPage: 0, imageError: false} },
	watch: {'$route.params.id'() { this.load() }},
	created() { this.load() },
	methods: {
		load() { this.imageError = false; getColumnDetail(this.$route.params.id).then(res => { this.column = res.data || {}; document.title = this.column.name || '专栏' }); this.getBlogList(1) },
		getBlogList(pageNum) { getColumnBlogs(this.$route.params.id, pageNum).then(res => { this.blogList = res.data.list || []; this.totalPage = res.data.totalPage || 0; this.$nextTick(() => { if (window.Prism) window.Prism.highlightAll() }) }) }
	}
}
</script>

<style scoped>
/* 桌面端：作为中栏弹性容器，让空列表与左侧专栏区等高 */
@media (min-width: 768px) {
	.column-page { display:flex; flex:1; flex-direction:column; min-height:0; }
}
.column-header { padding:22px 26px !important; border-radius:8px !important; box-shadow:0 2px 8px rgba(31,45,61,.08) !important; }
.header-main { display:flex; align-items:center; gap:18px; }
.header-main img,.header-logo { width:72px; height:72px; flex:0 0 72px; border-radius:14px; object-fit:cover; background:#eef4ff; }
.header-logo { display:flex; align-items:center; justify-content:center; color:#409eff; font-size:32px; }
.header-info { min-width:0; }
.column-label { margin-bottom:5px; color:#409eff; font-size:12px; font-weight:600; letter-spacing:1px; }
.header-main h2 { margin:0 0 7px; color:#303133; font-size:22px; line-height:1.3; }
.header-main p { margin:0; color:#7b8491; line-height:1.7; }
</style>
