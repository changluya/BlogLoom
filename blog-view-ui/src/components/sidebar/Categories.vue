<template>
	<div class="ui segments m-box">
		<div class="ui secondary segment"><i class="folder open icon"></i>文章分类<span v-if="categoryList.length" class="segment-count">{{ categoryList.length }}</span></div>
		<div class="ui blue segment m-padding-small content-segment">
			<router-link v-for="category in categoryList" :key="category.id" :to="`/category/${encodeURIComponent(category.name)}`" class="category-label m-text-500" :title="category.name">
				<i class="folder outline icon"></i>
				<span class="category-name">{{ category.name }}</span>
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
.segment-count { margin-left:6px; padding:0 7px; border-radius:999px; background:#ecf5ff; color:#409eff; font-size:12px; line-height:17px; vertical-align:1px; }
.m-padding-small { padding:6px; }
/* 固定高度上限，分类过多时在面板内上下滚动，保持侧栏整体高度稳定 */
.content-segment { display:flex; flex-wrap:wrap; gap:5px; align-content:flex-start; min-height:76px; max-height:210px; overflow-y:auto; overscroll-behavior:contain; }
.content-segment::-webkit-scrollbar { width:5px; }
.content-segment::-webkit-scrollbar-thumb { border-radius:999px; background:#cbd5e1; }
.content-segment::-webkit-scrollbar-track { background:transparent; }
/* flex-grow 让同一行的分类自动撑满整行宽度 */
.category-label { display:inline-flex; flex:1 1 auto; align-items:center; justify-content:center; min-width:0; margin:0; padding:4px 8px; border:1px solid #d9ecff; border-radius:4px; background:#ecf5ff; color:#409eff; font-size:11px; line-height:1; transition:all .2s; }
.category-label:hover { border-color:#409eff; background:#409eff; color:#fff; transform:translateY(-1px); }
.category-label i { margin-right:4px; flex:0 0 auto; font-size:11px; }
.category-name { min-width:0; overflow:hidden; white-space:nowrap; text-overflow:ellipsis; }
.category-count { margin-left:4px; padding:0 5px; border-radius:999px; background:rgba(64,158,255,.14); color:#409eff; font-size:10px; line-height:14px; flex:0 0 auto; }
.category-label:hover .category-count { background:rgba(255,255,255,.25); color:#fff; }
.empty-state { display:flex; min-height:88px; flex-direction:column; align-items:center; justify-content:center; padding:8px 6px; color:#98a2b3; text-align:center; }
.empty-icon { display:flex; width:38px; height:38px; align-items:center; justify-content:center; margin-bottom:8px; border-radius:50%; background:#f3f6f9; font-size:17px; }
.empty-icon i { margin:0; }
.empty-title { color:#606b79; font-size:14px; font-weight:600; }
.empty-description { margin-top:4px; font-size:12px; }
</style>
