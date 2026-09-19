<template>
	<div v-if="columns.length" class="ui segments m-box">
		<div class="ui secondary segment"><i class="columns icon"></i>TA 的专栏</div>
		<div class="ui segment column-panel">
		<div class="column-tree">
		<div v-for="column in visibleColumns" :key="column.id" class="column-group">
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
		<div v-if="overflow" class="column-toggle" :title="expanded ? '收起专栏' : '展开全部专栏'" @click="expanded = !expanded">
			<i :class="expanded ? 'el-icon-arrow-up' : 'el-icon-arrow-down'"></i>
			<span>{{ expanded ? '收起' : '展开全部' }}</span>
		</div>
		</div>
	</div>
</template>

<script>
import {getColumnTree} from '@/api/column'
const MAX_VISIBLE_ROWS = 8
export default {
	name: 'UserColumns',
	data() { return {columns: [], failed: {}, expanded: false} },
	computed: {
		overflow() {
			return this.columns.reduce((total, column) => total + 1 + (column.children ? column.children.length : 0), 0) > MAX_VISIBLE_ROWS
		},
		visibleColumns() {
			if (this.expanded || !this.overflow) return this.columns
			const rows = []
			let count = 0
			for (const column of this.columns) {
				const size = 1 + (column.children ? column.children.length : 0)
				if (count > 0 && count + size > MAX_VISIBLE_ROWS) break
				rows.push(column)
				count += size
			}
			return rows
		}
	},
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
.secondary.segment { padding:10px; }
.column-panel { padding:13px 12px 12px; }
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
.column-toggle { display:flex; width:fit-content; align-items:center; justify-content:center; gap:4px; margin:9px auto 0; padding:3px 14px; color:#969eaa; font-size:12px; line-height:18px; background:#f2f4f7; border-radius:12px; cursor:pointer; transition:color .18s, background .18s; }
.column-toggle:hover { color:#409eff; background:#eaf2fd; }
.column-toggle i { font-size:12px; }
</style>
