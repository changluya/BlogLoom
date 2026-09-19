<template>
	<div>
		<!--搜索-->
		<el-row type="flex" justify="space-between" align="middle" class="list-toolbar">
			<el-col :span="18" class="search-and-manage">
				<el-input class="blog-search" placeholder="请输入标题" v-model="queryInfo.title" :clearable="true" @clear="search" @keyup.native.enter="search" size="small">
					<el-select v-model="queryInfo.categoryId" slot="prepend" placeholder="请选择分类" :clearable="true" @change="search" style="width: 160px">
						<el-option :label="item.name" :value="item.id" v-for="item in categoryList" :key="item.id"></el-option>
					</el-select>
					<el-button slot="append" icon="el-icon-search" @click="search"></el-button>
				</el-input>
			</el-col>
			<el-col :span="6" class="toolbar-actions">
				<el-button size="small" type="warning" icon="el-icon-refresh-left" :disabled="!selection.length" @click="restoreSelected">批量恢复</el-button>
				<el-button size="small" type="danger" icon="el-icon-delete" :disabled="!selection.length" @click="deleteSelected">彻底删除</el-button>
			</el-col>
		</el-row>

		<el-alert class="recycle-tip" title="回收站中的博客已从列表与前台隐藏，关联的分类、标签、专栏、评论仍保留，可随时恢复。" type="info" :closable="false" show-icon/>

		<el-table :data="blogList" @selection-change="handleSelectionChange">
			<el-table-column type="selection" width="50"></el-table-column>
			<el-table-column label="标题" prop="title" show-overflow-tooltip></el-table-column>
			<el-table-column label="分类" prop="category.name" width="150"></el-table-column>
			<el-table-column label="原状态" width="100">
				<template v-slot="scope">
					<el-tag size="mini" :type="scope.row.published ? 'success' : 'info'">{{ scope.row.published ? '公开' : '私密' }}</el-tag>
				</template>
			</el-table-column>
			<el-table-column label="删除时间" width="170">
				<template v-slot="scope">{{ scope.row.updateTime | dateFormat }}</template>
			</el-table-column>
			<el-table-column label="操作" width="200">
				<template v-slot="scope">
					<el-button type="primary" icon="el-icon-refresh-left" size="mini" @click="restore(scope.row.id)">恢复</el-button>
					<el-popconfirm title="彻底删除后不可恢复，确定吗？" icon="el-icon-delete" iconColor="red" @onConfirm="deletePermanently(scope.row.id)">
						<el-button size="mini" type="danger" icon="el-icon-delete" slot="reference">彻底删除</el-button>
					</el-popconfirm>
				</template>
			</el-table-column>
		</el-table>

		<!--分页-->
		<el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange" :current-page="queryInfo.pageNum"
		               :page-sizes="[10, 20, 30, 50]" :page-size="queryInfo.pageSize" :total="total"
		               layout="total, sizes, prev, pager, next, jumper" background>
		</el-pagination>
	</div>
</template>

<script>
	import Breadcrumb from "@/components/Breadcrumb";
	import {getRecycleDataByQuery, restoreBlogById, deleteBlogPermanentlyById, restoreBlogBatch, deleteBlogBatchPermanently} from '@/api/blog'

	export default {
		name: "RecycleBin",
		components: {Breadcrumb},
		data() {
			return {
				queryInfo: {
					title: '',
					categoryId: null,
					pageNum: 1,
					pageSize: 10
				},
				blogList: [],
				categoryList: [],
				total: 0,
				selection: []
			}
		},
		created() {
			this.getData()
		},
		methods: {
			getData() {
				getRecycleDataByQuery(this.queryInfo).then(res => {
					this.blogList = res.data.blogs.list
					this.categoryList = res.data.categories
					this.total = res.data.blogs.total
				})
			},
			search() {
				this.queryInfo.pageNum = 1
				this.queryInfo.pageSize = 10
				this.getData()
			},
			handleSelectionChange(selection) {
				this.selection = selection
			},
			handleSizeChange(newSize) {
				this.queryInfo.pageSize = newSize
				this.getData()
			},
			handleCurrentChange(newPage) {
				this.queryInfo.pageNum = newPage
				this.getData()
			},
			restore(id) {
				this.$confirm('确定要恢复该博客吗？', '提示', {
					confirmButtonText: '确定',
					cancelButtonText: '取消',
					type: 'warning'
				}).then(() => {
					restoreBlogById(id).then(res => {
						this.msgSuccess(res.msg)
						this.getData()
					})
				}).catch(() => {})
			},
			deletePermanently(id) {
				deleteBlogPermanentlyById(id).then(res => {
					this.msgSuccess(res.msg)
					this.getData()
				})
			},
			restoreSelected() {
				const ids = this.selection.map(item => item.id)
				if (!ids.length) return
				this.$confirm(`确定要恢复选中的 ${ids.length} 篇博客吗？`, '提示', {
					confirmButtonText: '确定',
					cancelButtonText: '取消',
					type: 'warning'
				}).then(() => {
					restoreBlogBatch(ids).then(res => {
						this.msgSuccess(res.msg)
						this.getData()
					})
				}).catch(() => {})
			},
			deleteSelected() {
				const ids = this.selection.map(item => item.id)
				if (!ids.length) return
				this.$confirm(`彻底删除选中的 ${ids.length} 篇博客后不可恢复，确定吗？`, '提示', {
					confirmButtonText: '彻底删除',
					cancelButtonText: '取消',
					type: 'warning'
				}).then(() => {
					deleteBlogBatchPermanently(ids).then(res => {
						this.msgSuccess(res.msg)
						this.getData()
					})
				}).catch(() => {})
			}
		}
	}
</script>

<style scoped>
	.list-toolbar { margin-bottom: 16px; }
	.search-and-manage { display:flex; align-items:center; gap:10px; }
	.blog-search { width:500px; flex:0 0 500px; }
	.toolbar-actions { text-align: right; }
	.recycle-tip { margin-bottom: 14px; }
	.el-button + span {
		margin-left: 10px;
	}
</style>
