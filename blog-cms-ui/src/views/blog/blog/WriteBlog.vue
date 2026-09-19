<template>
	<div class="write-blog-page">
		<el-form :model="form" :rules="formRules" ref="formRef" label-position="top">
			<el-form-item label="文章标题" prop="title">
				<el-input v-model="form.title" placeholder="请输入文章标题" maxlength="255" show-word-limit></el-input>
			</el-form-item>
			<el-form-item label="文章正文" prop="content">
				<mavon-editor ref="contentEditor" v-model="form.content" class="content-editor"
				              :scroll-style="true" @imgAdd="handleContentImageAdd"/>
			</el-form-item>
		</el-form>
		<div class="editor-status-bar">
			<div class="article-stats">
				<span class="markdown-label">Markdown</span>
				<span>{{ markdownStats.characters }} 字符</span>
				<span>{{ markdownStats.lines }} 行</span>
				<span>预计阅读 {{ markdownStats.readTime }} 分钟</span>
			</div>
			<el-button type="primary" icon="el-icon-s-promotion" @click="openPublishDialog">发布文章</el-button>
		</div>

		<el-dialog title="发布设置" custom-class="publish-dialog" width="720px" :visible.sync="dialogVisible" :close-on-click-modal="false">
			<el-form ref="publishFormRef" :model="form" :rules="publishRules" label-position="top" @submit.native.prevent>
				<el-form-item label="文章封面" prop="firstPicture">
					<div class="cover-upload-row">
						<ImageUpload v-model="form.firstPicture" :width="240" :height="135" object-fit="contain"
						             :loading="coverUploading" @change="handleCoverChange"/>
						<div class="cover-url-field">
							<el-input v-model="form.firstPicture" placeholder="上传封面或填写图片 URL"/>
							<div class="cover-tip">建议使用 16:9 矩形图片；保存文章后归档至 blogs/{blogId}/cover/。</div>
						</div>
					</div>
				</el-form-item>
				<el-form-item label="文章描述" prop="description">
					<el-input v-model="form.description" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="请输入文章摘要或描述"/>
				</el-form-item>
				<el-row :gutter="18">
					<el-col :span="12">
						<el-form-item label="分类" prop="cate">
							<el-select v-model="form.cate" placeholder="请选择或输入分类" allow-create filterable style="width:100%">
								<el-option v-for="item in categoryList" :key="item.id" :label="item.name" :value="item.id"/>
							</el-select>
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="标签" prop="tagList">
							<el-select v-model="form.tagList" placeholder="请选择或输入标签" allow-create filterable multiple style="width:100%">
								<el-option v-for="item in tagList" :key="item.id" :label="item.name" :value="item.id"/>
							</el-select>
						</el-form-item>
					</el-col>
				</el-row>
				<el-form-item label="所属专栏">
					<el-cascader v-model="form.columnIds" :options="columnOptions" :props="columnProps"
					             clearable collapse-tags filterable style="width:100%" placeholder="可选择多个专栏"/>
				</el-form-item>
				<el-form-item label="文章数据">
					<div class="publish-stats">
						<div><strong>{{ form.words || 0 }}</strong><span>正文字符</span></div>
						<div><strong>{{ form.readTime || 0 }}</strong><span>预计阅读（分钟）</span></div>
						<div><strong>{{ form.views || 0 }}</strong><span>浏览次数</span></div>
					</div>
					<div class="stats-tip"><i class="el-icon-info"></i> 字数和阅读时长根据正文自动计算，浏览次数由系统维护。</div>
				</el-form-item>
				<el-form-item label="可见性">
					<el-radio-group v-model="radio">
						<el-radio :label="1">公开</el-radio>
						<el-radio :label="2">私密</el-radio>
						<el-radio :label="3">密码保护</el-radio>
					</el-radio-group>
				</el-form-item>
				<el-form-item v-if="radio===3" label="访问密码">
					<el-input v-model="form.password" show-password placeholder="请输入访问密码"/>
				</el-form-item>
				<el-form-item v-if="radio!==2" label="发布选项">
					<div class="publish-switches">
						<el-switch v-model="form.appreciation" active-text="赞赏"/>
						<el-switch v-model="form.recommend" active-text="推荐"/>
						<el-switch v-model="form.commentEnabled" active-text="评论"/>
						<el-switch v-model="form.top" active-text="置顶"/>
					</div>
				</el-form-item>
			</el-form>
			<span slot="footer">
				<el-button @click="dialogVisible=false">取消</el-button>
				<el-button type="primary" :loading="submitting" @click="submit">确认发布</el-button>
			</span>
		</el-dialog>
	</div>
</template>

	<script>
	import {getCategoryAndTag, saveBlog, getBlogById, updateBlog, uploadBlogResource} from '@/api/blog'
	import {getColumnOptions} from '@/api/column'
	import ImageUpload from '@/components/ImageUpload'

		export default {
		name: "WriteBlog",
		components: {ImageUpload},
		data() {
			return {
				categoryList: [],
				tagList: [],
				columnOptions: [],
				columnProps: {multiple: true, emitPath: false, checkStrictly: true, value: 'id', label: 'name', children: 'children'},
				dialogVisible: false,
				submitting: false,
				coverUploading: false,
				radio: 1,
				form: {
					title: '',
					firstPicture: '',
					description: '',
					content: '',
					cate: null,
					tagList: [],
					words: null,
					readTime: null,
					views: 0,
					appreciation: false,
					recommend: false,
					commentEnabled: false,
					top: false,
					published: false,
					password: '',
					columnIds: [],
				},
				formRules: {
					title: [{required: true, message: '请输入标题', trigger: 'change'}],
					content: [{required: true, message: '请输入文章正文', trigger: 'change'}],
				},
				publishRules: {
					firstPicture: [{required: true, message: '请上传或填写文章封面', trigger: 'change'}],
					description: [{required: true, message: '请输入文章描述', trigger: 'change'}],
					cate: [{required: true, message: '请选择分类', trigger: 'change'}],
					tagList: [{required: true, message: '请选择标签', trigger: 'change'}],
				},
			}
		},
		computed: {
			markdownStats() {
				const content = this.form.content || ''
				const characters = content.replace(/\s+/g, '').length
				return {characters, lines: content ? content.split(/\r?\n/).length : 0, readTime: characters ? Math.max(1, Math.round(characters / 200)) : 0}
			}
		},
		watch: {
			'form.words'(newValue) {
				this.form.readTime = newValue ? Math.round(newValue / 200) : null
			},
		},
		created() {
			this.getData()
			if (this.$route.params.id) {
				this.getBlog(this.$route.params.id)
			}
		},
		methods: {
			getData() {
				getCategoryAndTag().then(res => {
					this.categoryList = res.data.categories
					this.tagList = res.data.tags
				})
				getColumnOptions().then(res => { this.columnOptions = res.data || [] })
			},
			getBlog(id) {
				getBlogById(id).then(res => {
					this.computeCategoryAndTag(res.data)
					this.form = res.data
					this.radio = this.form.published ? (this.form.password !== '' ? 3 : 1) : 2
				})
			},
			computeCategoryAndTag(blog) {
				blog.cate = blog.category.id
				blog.tagList = []
				blog.tags.forEach(item => {
					blog.tagList.push(item.id)
				})
				blog.columnIds = blog.columnIds || []
			},
			handleContentImageAdd(pos, file) {
				this.uploadEditorImage('contentEditor', pos, file)
			},
			uploadEditorImage(editorRef, pos, file) {
				uploadBlogResource(file).then(res => {
					const editor = this.$refs[editorRef]
					if (editor && res.data && res.data.url) {
						editor.$img2Url(pos, res.data.url)
					}
				})
			},
			handleCoverChange(file) {
				this.coverUploading = true
				uploadBlogResource(file).then(res => {
					if (res.data && res.data.url) {
						this.form.firstPicture = res.data.url
						this.$nextTick(() => this.$refs.publishFormRef && this.$refs.publishFormRef.validateField('firstPicture'))
					}
				}).finally(() => { this.coverUploading = false })
			},
			openPublishDialog() {
				this.$refs.formRef.validate(valid => {
					if (!valid) return this.msgError('请先填写文章标题和正文')
					this.form.words = (this.form.content || '').replace(/\s+/g, '').length
					this.form.readTime = this.form.words ? Math.max(1, Math.round(this.form.words / 200)) : 0
					if (this.form.views === null || this.form.views === undefined) this.form.views = 0
					this.dialogVisible = true
					this.$nextTick(() => { if (this.$refs.publishFormRef) this.$refs.publishFormRef.clearValidate() })
				})
			},
			submit() {
				if (this.radio === 3 && (this.form.password === '' || this.form.password === null)) {
					return this.msgError("密码保护模式必须填写密码！")
				}
				this.$refs.publishFormRef.validate(valid => {
					if (valid) {
						if (this.radio === 2) {
							this.form.appreciation = false
							this.form.recommend = false
							this.form.commentEnabled = false
							this.form.top = false
							this.form.published = false
						} else {
							this.form.published = true
						}
						if (this.radio !== 3) {
							this.form.password = ''
						}
						if (this.$route.params.id) {
							this.form.category = null
							this.form.tags = null
							this.submitting = true
							updateBlog(this.form).then(res => {
								this.msgSuccess(res.msg)
								this.$router.push('/blog/content/articles')
							}).finally(() => { this.submitting = false })
						} else {
							this.submitting = true
							saveBlog(this.form).then(res => {
								this.msgSuccess(res.msg)
								this.$router.push('/blog/content/articles')
							}).finally(() => { this.submitting = false })
						}
					} else {
						return this.msgError('请填写完整的发布信息')
					}
				})
			}
		}
	}
</script>

<style scoped>
	.write-blog-page { padding-bottom: 78px; }
	.editor-status-bar { position: fixed; left: 190px; right: 0; bottom: 0; z-index: 1600; display: flex; align-items: center; justify-content: space-between; min-height: 64px; padding: 0 24px; box-sizing: border-box; background: rgba(255,255,255,.97); border-top: 1px solid #ebeef5; box-shadow: 0 -5px 16px rgba(31,45,61,.08); backdrop-filter: blur(8px); transition: left .28s; }
	.editor-status-bar ::v-deep .el-button { min-width: 112px; }
	.article-stats { display: flex; align-items: center; flex-wrap: wrap; gap: 6px 16px; color: #909399; font-size: 13px; }
	.markdown-label { color: #606266; font-weight: 600; }
	.publish-switches { display: flex; flex-wrap: wrap; gap: 18px 28px; padding: 12px 14px; background: #f7f9fc; border-radius: 6px; }
	.cover-upload-row { display: flex; align-items: center; gap: 18px; }
	.cover-url-field { flex: 1; min-width: 0; }
	.cover-tip { margin-top: 8px; color: #909399; font-size: 12px; line-height: 1.5; }
	.publish-stats { display: grid; grid-template-columns: repeat(3, 1fr); overflow: hidden; border: 1px solid #ebeef5; border-radius: 7px; background: #f8fafc; }
	.publish-stats > div { padding: 15px 10px; text-align: center; border-right: 1px solid #ebeef5; }
	.publish-stats > div:last-child { border-right: 0; }
	.publish-stats strong, .publish-stats span { display: block; }
	.publish-stats strong { color: #303133; font-size: 21px; line-height: 1.3; }
	.publish-stats span { margin-top: 4px; color: #909399; font-size: 12px; }
	.stats-tip { margin-top: 8px; color: #909399; font-size: 12px; line-height: 1.5; }
	.content-editor { height: calc(100vh - 250px); min-height: 500px; max-height: 680px; overflow: hidden; }
	.content-editor.fullscreen {
		position: fixed !important;
		inset: 0 !important;
		width: 100vw !important;
		height: 100vh !important;
		min-width: 0;
		min-height: 0;
		max-width: none;
		max-height: none;
		margin: 0 !important;
		border-radius: 0;
		z-index: 3000 !important;
	}
	.content-editor ::v-deep .v-note-panel { min-height: 0; }
	.content-editor ::v-deep .v-note-edit,
	.content-editor ::v-deep .v-note-show { min-height: 0; }
	@media (max-height: 760px) {
		.content-editor:not(.fullscreen) { height: 500px; }
	}
	@media (max-width: 640px) {
		.cover-upload-row { align-items: flex-start; flex-direction: column; }
		.cover-url-field { width: 100%; }
	}
</style>

<style>
	#app .hideSidebar .editor-status-bar { left: 54px; }
	#app .mobile .editor-status-bar { left: 0; }
	.publish-dialog .el-dialog__body { max-height: calc(100vh - 210px); overflow-y: auto; padding-top: 14px; padding-bottom: 10px; }
	@media (max-width: 800px) {
		.publish-dialog { width: calc(100vw - 30px) !important; }
	}
</style>
