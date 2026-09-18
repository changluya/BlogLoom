<template>
	<div v-if="columns.length" class="column-panel">
		<div class="panel-title">TA 的专栏</div>
		<div v-for="column in columns" :key="column.id" class="column-group">
			<div class="column-row root-row" @click="go(column.id)">
				<button v-if="column.children && column.children.length" class="toggle" @click.stop="toggle(column.id)"><i :class="expanded[column.id] ? 'el-icon-arrow-down' : 'el-icon-arrow-right'"></i></button>
				<span v-else class="toggle-placeholder"></span>
				<img v-if="column.cover && !failed[column.id]" :src="column.cover" class="column-logo" @error="imageFailed(column.id)">
				<span v-else class="column-logo fallback"><i class="el-icon-collection"></i></span>
				<span class="column-name" :title="column.name">{{ column.name }}</span><span class="count">{{ column.blogCount }}篇</span>
			</div>
			<div v-show="expanded[column.id]" class="children">
				<div v-for="child in column.children" :key="child.id" class="column-row child-row" @click="go(child.id)">
					<span class="tree-line">└</span>
					<img v-if="child.cover && !failed[child.id]" :src="child.cover" class="column-logo child-logo" @error="imageFailed(child.id)">
					<span v-else class="column-logo child-logo fallback"><i class="el-icon-collection"></i></span>
					<span class="column-name" :title="child.name">{{ child.name }}</span><span class="count">{{ child.blogCount }}篇</span>
				</div>
			</div>
		</div>
	</div>
</template>

<script>
import {getColumnTree} from '@/api/column'
export default {
	name: 'UserColumns',
	data() { return {columns: [], expanded: {}, failed: {}} },
	created() { getColumnTree().then(res => { this.columns = res.data || []; this.columns.slice(0, 3).forEach(item => this.$set(this.expanded, item.id, true)) }) },
	methods: {
		toggle(id) { this.$set(this.expanded, id, !this.expanded[id]) },
		go(id) { this.$router.push(`/column/${id}`) },
		imageFailed(id) { this.$set(this.failed, id, true) }
	}
}
</script>

<style scoped>
.column-panel { border-top:1px solid #ebeef5; padding:13px 10px 10px; background:#fff; }
.panel-title { margin:0 6px 9px; color:#303133; font-size:14px; font-weight:600; }
.column-row { display:flex; align-items:center; min-height:40px; padding:4px 4px; border-radius:6px; cursor:pointer; transition:background .2s; }
.column-row:hover { background:#f5f7fa; }
.toggle { width:20px; padding:0; border:0; background:transparent; color:#909399; cursor:pointer; }
.toggle-placeholder { width:20px; }
.column-logo { width:30px; height:30px; flex:0 0 30px; border-radius:7px; object-fit:cover; background:#eef2f7; }
.child-logo { width:24px; height:24px; flex-basis:24px; border-radius:6px; }
.fallback { display:inline-flex; align-items:center; justify-content:center; color:#409eff; }
.column-name { min-width:0; margin-left:8px; overflow:hidden; color:#4b5563; font-size:13px; text-overflow:ellipsis; white-space:nowrap; }
.count { margin-left:auto; padding-left:6px; color:#909399; font-size:12px; white-space:nowrap; }
.child-row { padding-left:22px; }
.tree-line { width:17px; color:#c0c4cc; font-size:13px; }
</style>
