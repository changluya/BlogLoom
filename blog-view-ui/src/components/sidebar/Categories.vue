<template>
	<div class="ui segments m-box">
		<div class="ui secondary segment"><i class="folder open icon"></i>文章分类</div>
		<div class="ui blue segment m-padding-small content-segment">
			<router-link v-for="category in categoryList" :key="category.id" :to="`/category/${encodeURIComponent(category.name)}`" class="category-label m-text-500">
				<i class="folder outline icon"></i>{{ category.name }}
				<span class="category-count">{{ category.blogCount || 0 }}</span>
			</router-link>
			<div v-if="!categoryList.length" class="empty-state">
				<div class="empty-icon"><i class="folder open outline icon"></i></div>
				<div class="empty-title">暂无分类</div>
				<div class="empty-description">创建文章分类后将在这里展示</div>
			</div>
		</div>
	</div>
</template>

<script>
export default {
	name: 'Categories',
	props: {categoryList: {type: Array, required: true}}
}
</script>

<style scoped>
.secondary.segment { padding:10px; }
.m-padding-small { padding:7px; }
.content-segment { min-height:76px; }
.category-label { display:inline-flex; align-items:center; margin:3px; padding:7px 10px; border:1px solid #d9ecff; border-radius:5px; background:#ecf5ff; color:#409eff; font-size:12px; line-height:1; transition:all .2s; }
.category-label:hover { border-color:#409eff; background:#409eff; color:#fff; transform:translateY(-1px); }
.category-label i { margin-right:5px; }
.category-count { margin-left:6px; padding:0 6px; border-radius:999px; background:rgba(64,158,255,.14); color:#409eff; font-size:11px; line-height:16px; }
.category-label:hover .category-count { background:rgba(255,255,255,.25); color:#fff; }
.empty-state { display:flex; min-height:88px; flex-direction:column; align-items:center; justify-content:center; padding:8px 6px; color:#98a2b3; text-align:center; }
.empty-icon { display:flex; width:38px; height:38px; align-items:center; justify-content:center; margin-bottom:8px; border-radius:50%; background:#f3f6f9; font-size:17px; }
.empty-icon i { margin:0; }
.empty-title { color:#606b79; font-size:14px; font-weight:600; }
.empty-description { margin-top:4px; font-size:12px; }
</style>
