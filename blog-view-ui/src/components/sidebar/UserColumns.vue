<template>
	<div v-if="columns.length" class="column-panel">
		<div class="panel-title">TA 的专栏</div>
		<div class="column-tree">
		<div v-for="column in columns" :key="column.id" class="column-group">
			<div class="column-row root-row" @click="go(column.id)">
				<span class="root-branch" aria-hidden="true"></span>
				<img v-if="column.cover && !failed[column.id]" :src="column.cover" class="column-logo" @error="imageFailed(column.id)">
				<span v-else class="column-logo fallback"><i class="el-icon-collection"></i></span>
				<span class="column-name" :title="column.name">{{ column.name }}</span><span class="count">{{ formatCount(column.blogCount) }}篇</span>
			</div>
			<div v-if="column.children && column.children.length" class="children">
				<div v-for="child in column.children" :key="child.id" class="column-row child-row" @click="go(child.id)">
					<span class="child-branch" aria-hidden="true"></span>
					<img v-if="child.cover && !failed[child.id]" :src="child.cover" class="column-logo child-logo" @error="imageFailed(child.id)">
					<span v-else class="column-logo child-logo fallback"><i class="el-icon-collection"></i></span>
					<span class="column-name" :title="child.name">{{ child.name }}</span><span class="count">{{ formatCount(child.blogCount) }}篇</span>
				</div>
			</div>
		</div>
		</div>
	</div>
</template>

<script>
import {getColumnTree} from '@/api/column'
export default {
	name: 'UserColumns',
	data() { return {columns: [], failed: {}} },
	created() { getColumnTree().then(res => { this.columns = res.data || [] }) },
	methods: {
		go(id) { this.$router.push(`/column/${id}`) },
		imageFailed(id) { this.$set(this.failed, id, true) },
		formatCount(value) {
			const count = Number(value)
			return Number.isFinite(count) ? count.toLocaleString('zh-CN') : '0'
		}
	}
}
</script>

<style scoped>
.column-panel { border-top:1px solid #ebeef5; padding:13px 12px 12px; background:#fff; }
.panel-title { margin:0 4px 10px; color:#303846; font-size:14px; font-weight:600; }
.column-tree { position:relative; padding-left:16px; }
.column-tree::before { position:absolute; top:17px; bottom:17px; left:4px; border-left:1px dotted #cbd2dc; content:''; }
.column-group { position:relative; }
.column-row { position:relative; display:flex; align-items:center; min-height:35px; padding:3px 4px; border-radius:4px; box-sizing:border-box; cursor:pointer; transition:color .18s, background .18s; }
.column-row:hover { background:#f4f7fb; }
.root-branch { position:absolute; top:50%; left:-12px; width:12px; border-top:1px dotted #cbd2dc; }
.column-logo { width:22px; height:22px; flex:0 0 22px; border:1px solid #e9edf3; border-radius:4px; box-sizing:border-box; object-fit:cover; background:#f3f6fa; }
.child-logo { width:20px; height:20px; flex-basis:20px; }
.fallback { display:inline-flex; align-items:center; justify-content:center; border-color:#e1e9f5; color:#409eff; background:#f2f7fd; }
.fallback i { font-size:13px; }
.child-logo.fallback i { font-size:12px; }
.column-name { min-width:0; margin-left:8px; overflow:hidden; color:#4b5563; font-size:13px; line-height:20px; text-overflow:ellipsis; white-space:nowrap; }
.root-row .column-name { color:#3c4655; font-weight:500; }
.count { min-width:34px; margin-left:auto; padding-left:8px; color:#969eaa; font-size:12px; text-align:right; white-space:nowrap; }
.children { position:relative; }
.child-row { min-height:34px; padding-left:28px; }
.child-branch { position:absolute; top:50%; left:-12px; width:36px; border-top:1px dotted #cbd2dc; }
.child-row:hover .column-name, .root-row:hover .column-name { color:#409eff; }
</style>
