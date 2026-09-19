<template>
	<div>
		<!--搜索-->
		<el-row type="flex" justify="space-between" align="middle" class="list-toolbar">
			<el-col :span="18" class="search-and-manage">
				<PageTip title="文章管理">
					<p>这里集中管理所有文章：按标题/分类搜索，一键<strong>置顶、推荐</strong>，修改<strong>可见性</strong>（公开 / 私密 / 密码保护），编辑或删除文章。</p>
					<p>删除的文章会先进入<strong>回收站</strong>，可随时恢复；文章的分类、标签、专栏可在此快捷维护。</p>
				</PageTip>
				<el-input class="blog-search" placeholder="请输入标题" v-model="queryInfo.title" :clearable="true" @clear="search" @keyup.native.enter="search" size="small">
					<el-select v-model="queryInfo.categoryId" slot="prepend" placeholder="请选择分类" :clearable="true" @change="search" style="width: 160px">
						<el-option :label="item.name" :value="item.id" v-for="item in categoryList" :key="item.id"></el-option>
					</el-select>
					<el-button slot="append" icon="el-icon-search" @click="search"></el-button>
				</el-input>
				<el-button size="small" icon="el-icon-folder-opened" @click="categoryDialogVisible=true">分类管理</el-button>
				<el-button size="small" icon="el-icon-collection-tag" @click="tagDialogVisible=true">标签管理</el-button>
			</el-col>
			<el-col :span="6" class="toolbar-actions">
				<el-button size="small" icon="el-icon-upload2" @click="importVisible = true">快捷导入</el-button>
				<el-button type="primary" size="small" icon="el-icon-edit-outline" @click="goWriteBlogPage">写文章</el-button>
			</el-col>
		</el-row>

		<el-table :data="blogList">
			<el-table-column label="序号" type="index" width="50"></el-table-column>
			<el-table-column label="标题" prop="title" show-overflow-tooltip></el-table-column>
			<el-table-column label="分类" prop="category.name" width="150"></el-table-column>
			<el-table-column label="置顶" width="80">
				<template v-slot="scope">
					<el-switch v-model="scope.row.top" @change="blogTopChanged(scope.row)"></el-switch>
				</template>
			</el-table-column>
			<el-table-column label="推荐" width="80">
				<template v-slot="scope">
					<el-switch v-model="scope.row.recommend" @change="blogRecommendChanged(scope.row)"></el-switch>
				</template>
			</el-table-column>
			<el-table-column label="可见性" width="100">
				<template v-slot="scope">
					<el-link icon="el-icon-edit" :underline="false" @click="editBlogVisibility(scope.row)">
						{{ scope.row.published ? (scope.row.password !== '' ? '密码保护' : '公开') : '私密' }}
					</el-link>
				</template>
			</el-table-column>
			<el-table-column label="创建时间" width="170">
				<template v-slot="scope">{{ scope.row.createTime | dateFormat }}</template>
			</el-table-column>
			<el-table-column label="最近更新" width="170">
				<template v-slot="scope">{{ scope.row.updateTime | dateFormat }}</template>
			</el-table-column>
			<el-table-column label="操作" width="200">
				<template v-slot="scope">
					<el-button type="primary" icon="el-icon-edit" size="mini" @click="goBlogEditPage(scope.row.id)">编辑</el-button>
					<el-popconfirm title="确定删除吗？" icon="el-icon-delete" iconColor="red" @onConfirm="deleteBlogById(scope.row.id)">
						<el-button size="mini" type="danger" icon="el-icon-delete" slot="reference">删除</el-button>
					</el-popconfirm>
				</template>
			</el-table-column>
		</el-table>

		<!--分页-->
		<el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange" :current-page="queryInfo.pageNum"
		               :page-sizes="[10, 20, 30, 50]" :page-size="queryInfo.pageSize" :total="total"
		               layout="total, sizes, prev, pager, next, jumper" background>
		</el-pagination>

		<!--编辑可见性状态对话框-->
		<el-dialog title="博客可见性" width="30%" :visible.sync="dialogVisible">
			<!--内容主体-->
			<el-form label-width="50px" @submit.native.prevent>
				<el-form-item>
					<el-radio-group v-model="radio">
						<el-radio :label="1">公开</el-radio>
						<el-radio :label="2">私密</el-radio>
						<el-radio :label="3">密码保护</el-radio>
					</el-radio-group>
				</el-form-item>
				<el-form-item label="密码" v-if="radio===3">
					<el-input v-model="visForm.password"></el-input>
				</el-form-item>
				<el-form-item v-if="radio!==2">
					<el-row>
						<el-col :span="6">
							<el-switch v-model="visForm.appreciation" active-text="赞赏"></el-switch>
						</el-col>
						<el-col :span="6">
							<el-switch v-model="visForm.recommend" active-text="推荐"></el-switch>
						</el-col>
						<el-col :span="6">
							<el-switch v-model="visForm.commentEnabled" active-text="评论"></el-switch>
						</el-col>
						<el-col :span="6">
							<el-switch v-model="visForm.top" active-text="置顶"></el-switch>
						</el-col>
					</el-row>
				</el-form-item>
			</el-form>
			<!--底部-->
			<span slot="footer">
				<el-button @click="dialogVisible=false">取 消</el-button>
				<el-button type="primary" @click="saveVisibility">保存</el-button>
			</span>
		</el-dialog>

		<el-dialog title="分类管理" width="860px" :visible.sync="categoryDialogVisible" :close-on-click-modal="false" append-to-body>
			<CategoryList v-if="categoryDialogVisible" embedded @changed="handleCategoryChanged"/>
			<span slot="footer"><el-button @click="categoryDialogVisible=false">关闭</el-button></span>
		</el-dialog>

		<el-dialog title="标签管理" width="920px" :visible.sync="tagDialogVisible" :close-on-click-modal="false" append-to-body>
			<TagList v-if="tagDialogVisible" embedded @changed="handleTagChanged"/>
			<span slot="footer"><el-button @click="tagDialogVisible=false">关闭</el-button></span>
		</el-dialog>

		<!--快捷导入：与知识库「迁移本地博客」同一套逻辑-->
		<KnowledgeImportDialog :visible.sync="importVisible" @success="getData"/>
	</div>
</template>

<script>
	import Breadcrumb from "@/components/Breadcrumb";
	import PageTip from "@/components/PageTip";
	import KnowledgeImportDialog from "@/components/KnowledgeImportDialog";
	import {getDataByQuery, deleteBlogById, updateTop, updateRecommend, updateVisibility} from '@/api/blog'
	import CategoryList from '@/views/blog/category/CategoryList'
	import TagList from '@/views/blog/tag/TagList'

	export default {
		name: "BlogList",
		components: {Breadcrumb, PageTip, KnowledgeImportDialog, CategoryList, TagList},
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
				dialogVisible: false,
				categoryDialogVisible: false,
				tagDialogVisible: false,
				importVisible: false,
				blogId: 0,
				radio: 1,
				visForm: {
					appreciation: false,
					recommend: false,
					commentEnabled: false,
					top: false,
					published: false,
					password: '',
				}
			}
		},
		created() {
			this.getData()
		},
		methods: {
			handleCategoryChanged() { this.getData() },
			handleTagChanged() {},
			goWriteBlogPage() {
				this.$router.push('/blog/write')
			},
			getData() {
				getDataByQuery(this.queryInfo).then(res => {
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
			//切换博客置顶状态
			blogTopChanged(row) {
				updateTop(row.id, row.top).then(res => {
					this.msgSuccess(res.msg);
				})
			},
			//切换博客推荐状态
			blogRecommendChanged(row) {
				updateRecommend(row.id, row.recommend).then(res => {
					this.msgSuccess(res.msg);
				})
			},
			//编辑博客可见性
			editBlogVisibility(row) {
				this.visForm = {
					appreciation: row.appreciation,
					recommend: row.recommend,
					commentEnabled: row.commentEnabled,
					top: row.top,
					published: row.published,
					password: row.password,
				}
				this.blogId = row.id
				this.radio = this.visForm.published ? (this.visForm.password !== '' ? 3 : 1) : 2
				this.dialogVisible = true
			},
			//修改博客可见性
			saveVisibility() {
				if (this.radio === 3 && (this.visForm.password === '' || this.visForm.password === null)) {
					return this.msgError("密码保护模式必须填写密码！")
				}
				if (this.radio === 2) {
					this.visForm.appreciation = false
					this.visForm.recommend = false
					this.visForm.commentEnabled = false
					this.visForm.top = false
					this.visForm.published = false
				} else {
					this.visForm.published = true
				}
				if (this.radio !== 3) {
					this.visForm.password = ''
				}
				updateVisibility(this.blogId, this.visForm).then(res => {
					this.msgSuccess(res.msg)
					this.getData()
					this.dialogVisible = false
				})
			},
			//监听 pageSize 改变事件
			handleSizeChange(newSize) {
				this.queryInfo.pageSize = newSize
				this.getData()
			},
			//监听页码改变的事件
			handleCurrentChange(newPage) {
				this.queryInfo.pageNum = newPage
				this.getData()
			},
			goBlogEditPage(id) {
				this.$router.push(`/blog/edit/${id}`)
			},
			deleteBlogById(id) {
				this.$confirm('此操作将把该博客移入<strong style="color: red">回收站</strong>，可在回收站中恢复或彻底删除。是否继续?', '提示', {
					confirmButtonText: '移入回收站',
					cancelButtonText: '取消',
					type: 'warning',
					dangerouslyUseHTMLString: true
				}).then(() => {
					deleteBlogById(id).then(res => {
						this.msgSuccess(res.msg)
						this.getData()
					})
				}).catch(() => {
					this.$message({
						type: 'info',
						message: '已取消删除'
					})
				})
			}
		}
	}
</script>

<style scoped>
	.list-toolbar { margin-bottom: 16px; }
	.search-and-manage { display:flex; align-items:center; gap:10px; }
	.blog-search { width:500px; flex:0 0 500px; }
	.toolbar-actions { text-align: right; }
	.el-button + span {
		margin-left: 10px;
	}
</style>
