<template>
	<div>
		<el-dialog title="导入 Markdown 到知识库" custom-class="knowledge-import-dialog" :visible.sync="dialogVisible" width="680px" :close-on-click-modal="false" @closed="resetImport">
			<el-steps :active="importStepIndex" finish-status="success" align-center class="import-steps">
				<el-step title="选择文件" description="配置导入规则"/>
				<el-step title="内容预检" description="确认目录与文档"/>
				<el-step title="执行导入" description="实时查看进度"/>
			</el-steps>
			<template v-if="importStep === 'form'">
				<div class="import-mode">
					<el-radio-group v-model="importMode" size="small" @change="onModeChange">
						<el-radio-button label="zip"><i class="el-icon-folder-opened"></i> 知识库 ZIP</el-radio-button>
						<el-radio-button label="files"><i class="el-icon-document"></i> Markdown 文件</el-radio-button>
					</el-radio-group>
					<span class="import-mode-tip">{{ importModeTip }}</span>
				</div>

				<div v-if="importMode === 'zip'" class="import-section">
					<div class="import-section-title"><i class="el-icon-folder-opened"></i>选择知识库压缩包</div>
					<el-upload ref="importUpload" drag action="#" accept=".zip,application/zip" :auto-upload="false"
					           :limit="1" :on-change="onUploadChange" :on-remove="onUploadRemove" :on-exceed="onUploadExceed">
						<i class="el-icon-upload"></i>
						<div class="el-upload__text">将 ZIP 文件拖到这里，或 <em>点击选择文件</em></div>
						<div slot="tip" class="el-upload__tip">仅处理文件夹和 .md 文档，其他文件会自动过滤，最大支持 500MB；目录按压缩包结构还原，忽略 knowledgeBasePath</div>
					</el-upload>
				</div>

				<div v-else class="import-section">
					<div class="import-section-title"><i class="el-icon-document"></i>选择 Markdown 文件（单个或多个）</div>
					<el-upload ref="filesUpload" drag action="#" accept=".md,text/markdown" :auto-upload="false" multiple
					           :on-change="onFilesChange" :on-remove="onFilesChange">
						<i class="el-icon-upload"></i>
						<div class="el-upload__text">将 .md 文件拖到这里，或 <em>点击选择文件</em></div>
						<div slot="tip" class="el-upload__tip">按每篇 Markdown 顶部的 knowledgeBasePath 定位知识库目录；为空则落在知识库根目录，缺失目录会自动逐级创建；单篇 ≤ 5MB，总计 ≤ 100MB</div>
					</el-upload>
				</div>

				<div class="import-section meta-entry-section">
					<div class="meta-entry">
						<div class="meta-entry-text">
							<i class="el-icon-info"></i>
							<span>支持在每篇 Markdown 顶部声明<strong>标签、分类、专栏、时间、知识库路径</strong>：标签/分类不存在会自动创建，专栏仅关联已存在的。</span>
						</div>
						<div class="meta-entry-actions">
							<el-button size="mini" type="primary" plain icon="el-icon-view" @click="metaDrawerVisible = true">查看示例与说明</el-button>
							<el-button size="mini" icon="el-icon-download" @click="downloadSampleZip">下载示例 ZIP</el-button>
						</div>
					</div>
				</div>

				<el-form label-position="top" class="import-options">
					<el-form-item label="同名文档处理方式">
						<el-radio-group v-model="importForm.conflictPolicy" class="conflict-options">
							<el-radio-button label="SKIP"><i class="el-icon-remove-outline"></i> 跳过已有文档</el-radio-button>
							<el-radio-button label="RENAME"><i class="el-icon-copy-document"></i> 自动重命名</el-radio-button>
						</el-radio-group>
					</el-form-item>
					<el-form-item class="publish-option">
						<div class="option-line">
							<div><strong>导入后直接发布</strong><p>关闭时将以私密文章保存，可稍后编辑发布。</p></div>
							<el-switch v-model="importForm.published"/>
						</div>
					</el-form-item>
				</el-form>
			</template>
			<template v-else-if="importStep === 'preview'">
				<el-alert title="预检通过，可以开始导入" type="success" :closable="false" show-icon/>
				<div class="preview-stats">
					<div><strong>{{ preview.directoryCount }}</strong><span>目录</span></div>
					<div><strong>{{ preview.documentCount }}</strong><span>Markdown 文档</span></div>
					<div><strong>{{ preview.ignoredCount }}</strong><span>已过滤项目</span></div>
				</div>
				<div class="preview-title">即将导入的内容</div>
				<el-scrollbar class="preview-list">
					<el-tree :data="previewTree" node-key="id" default-expand-all :expand-on-click-node="false" class="preview-tree">
						<template slot-scope="{ node, data }">
							<span class="preview-tree-node">
								<i :class="data.isFile ? 'el-icon-document' : 'el-icon-folder'"></i>
								<span class="preview-tree-label">{{ node.label }}</span>
							</span>
						</template>
					</el-tree>
				</el-scrollbar>
			</template>
			<template v-else>
				<div class="import-progress-card">
					<el-progress type="circle" :width="126" :percentage="progress.progress" :status="progress.status === 'FAILED' ? 'exception' : (progress.status === 'SUCCESS' ? 'success' : null)"/>
					<div class="progress-detail">
						<h3>{{ progress.message || '正在准备导入...' }}</h3>
						<p>已处理 {{ progress.processed || 0 }}/{{ progress.total || 0 }}</p>
						<div class="progress-counts"><span>博客 {{ progress.createdBlogCount || 0 }}</span><span>目录 {{ progress.createdDirectoryCount || 0 }}</span><span>跳过 {{ progress.skippedCount || 0 }}</span></div>
						<p v-if="progress.currentPath" class="current-path" :title="progress.currentPath">当前：{{ progress.currentPath }}</p>
					</div>
				</div>
			</template>
			<span slot="footer">
				<el-button v-if="importStep === 'form'" @click="dialogVisible=false">取消</el-button>
				<el-button v-if="importStep === 'form'" type="primary" :loading="previewing" @click="doPreview">开始预检</el-button>
				<el-button v-if="importStep === 'preview'" @click="importStep='form'">上一步</el-button>
				<el-button v-if="importStep === 'preview'" type="primary" @click="doExecute">确认导入</el-button>
				<el-button v-if="terminal" type="primary" @click="dialogVisible=false">完成</el-button>
			</span>
		</el-dialog>

		<!--示例与说明抽屉（右侧展开）-->
		<el-drawer title="Markdown 导入示例与说明" :visible.sync="metaDrawerVisible" direction="rtl" size="560px" custom-class="knowledge-meta-drawer">
			<div class="meta-drawer-body">
				<p class="meta-help-desc">
					在每篇 <code>.md</code> 的<strong>最顶部</strong>放一个 <code>```json</code> 代码块，即可声明标题、标签、分类、摘要、专栏、创建/更新时间与知识库路径。
					导入时<strong>标签、分类不存在会自动创建并关联</strong>；<strong>专栏只匹配已存在的（含二级专栏）并关联，不会自动创建</strong>。
				</p>
				<p class="meta-help-desc">
					<strong>ZIP 导入</strong>：目录按压缩包内的多级文件夹自动映射为知识库目录，<strong>忽略 knowledgeBasePath</strong>。<br/>
					<strong>Markdown 文件导入</strong>：没有文件夹结构，目录由每篇的 <code>knowledgeBasePath</code> 决定（格式 <code>/a/bb/cc</code>，以 / 开头、末尾不带 /）；为空则落在知识库根目录，缺失目录会自动逐级创建。
				</p>
				<div class="meta-drawer-actions">
					<el-button size="small" type="primary" icon="el-icon-download" @click="downloadSampleZip">下载示例 ZIP</el-button>
					<el-button size="small" plain icon="el-icon-document-copy" @click="copyMetaSample">复制全部示例</el-button>
				</div>

				<div class="meta-sample">
					<div class="meta-sample-bar"><span><i class="el-icon-folder-opened"></i> ZIP 目录结构示例（支持多级文件夹）</span></div>
					<pre class="meta-sample-code">{{ zipSample }}</pre>
				</div>

				<div class="meta-file-list">
					<div v-for="file in fileSamples" :key="file.path" class="meta-sample">
						<div class="meta-sample-bar"><span><i class="el-icon-document"></i> {{ file.path }} 顶部代码块</span></div>
						<pre class="meta-sample-code">{{ file.json }}</pre>
					</div>
				</div>

				<el-table :data="metaFields" size="mini" border class="meta-fields">
					<el-table-column prop="field" label="字段" width="140"></el-table-column>
					<el-table-column prop="desc" label="说明"></el-table-column>
				</el-table>
			</div>
		</el-drawer>
	</div>
</template>

<script>
	import {executeImport, getImportProgress, previewImport, previewImportFiles} from '@/api/knowledge'

	export default {
		name: 'KnowledgeImportDialog',
		props: {
			visible: {
				type: Boolean,
				default: false
			},
			targetParentId: {
				type: Number,
				default: 0
			}
		},
		data() {
			return {
				importStep: 'form', importMode: 'zip', importFile: null, importFiles: [], previewing: false,
				preview: {}, progress: {progress: 0}, pollTimer: null,
				importForm: {targetParentId: 0, published: true, conflictPolicy: 'SKIP'},
				metaDrawerVisible: false,
				zipSample: [
					'blogloom-knowledge.zip',
					'├── 版本发布/',
					'│   └── BlogLoom 1.0 发布.md',
					'└── 使用场景/',
					'    ├── 个人技术博客.md',
					'    └── 开源项目主页.md'
				].join('\n'),
				fileSamples: [
					{
						path: '版本发布/BlogLoom 1.0 发布.md',
						json: [
							'```json',
							'{',
							'  "title": "BlogLoom 1.0 发布",',
							'  "tags": "BlogLoom,版本发布,开源",',
							'  "category": "版本发布",',
							'  "articleSummary": "BlogLoom 1.0 是项目首个正式版本，完成三端分离、内容闭环与站点配置等能力，本文介绍版本范围与升级注意事项。",',
							'  "columns": "更新日志",',
							'  "createTime": "2026-09-19 14:30:00",',
							'  "updateTime": "2026-09-19 14:30:00",',
							'  "knowledgeBasePath": "/版本发布"',
							'}',
							'```'
						].join('\n')
					},
					{
						path: '使用场景/个人技术博客.md',
						json: [
							'```json',
							'{',
							'  "title": "个人技术博客",',
							'  "tags": "BlogLoom,使用场景,技术博客",',
							'  "category": "使用场景",',
							'  "articleSummary": "以 BlogLoom 搭建自主可控的个人技术博客，用文章与专栏沉淀内容，并兼顾评论互动与访问统计。",',
							'  "columns": "使用场景, 建站指南",',
							'  "createTime": "2026-09-18 09:00:00",',
							'  "updateTime": "2026-09-18 09:00:00",',
							'  "knowledgeBasePath": "/使用场景"',
							'}',
							'```'
						].join('\n')
					},
					{
						path: '使用场景/开源项目主页.md',
						json: [
							'```json',
							'{',
							'  "title": "开源项目主页",',
							'  "tags": "BlogLoom,使用场景,开源项目",',
							'  "category": "使用场景",',
							'  "articleSummary": "用 BlogLoom 承载开源项目介绍、更新日志与使用文档，并通过评论收集使用反馈。",',
							'  "columns": "使用场景",',
							'  "createTime": "2026-09-17 20:15:00",',
							'  "updateTime": "2026-09-17 20:15:00",',
							'  "knowledgeBasePath": "/使用场景"',
							'}',
							'```'
						].join('\n')
					}
				],
				metaFields: [
					{field: 'title', desc: '文章标题；缺省时回退为 Markdown 文件名'},
					{field: 'tags', desc: '标签，多个，逗号分隔；数据库中不存在会自动创建'},
					{field: 'category', desc: '分类，单个；不存在会自动创建，缺省为「知识库」'},
					{field: 'articleSummary', desc: '文章摘要（150 字以内），对应文章描述'},
					{field: 'columns', desc: '所属专栏，多个，逗号分隔；仅关联已存在的专栏（含二级专栏），不会自动创建'},
					{field: 'createTime', desc: '创建时间，格式 YYYY-MM-DD HH:mm:ss；缺省为导入时间'},
					{field: 'updateTime', desc: '更新时间，格式 YYYY-MM-DD HH:mm:ss；缺省同创建时间'},
					{field: 'knowledgeBasePath', desc: '知识库目录路径，格式 /a/bb/cc，以 / 开头且末尾不带 /。仅「Markdown 文件」导入时生效；ZIP 导入按压缩包目录结构并忽略该字段。为空时落在知识库根目录'}
				]
			}
		},
		computed: {
			dialogVisible: {
				get() { return this.visible },
				set(value) { this.$emit('update:visible', value) }
			},
			terminal() { return this.importStep === 'progress' && ['SUCCESS', 'FAILED'].includes(this.progress.status) },
			importStepIndex() { return this.importStep === 'form' ? 0 : (this.importStep === 'preview' ? 1 : 2) },
			importModeTip() {
				return this.importMode === 'zip'
					? '按压缩包文件夹结构还原知识库目录，忽略 knowledgeBasePath'
					: '按每篇 Markdown 的 knowledgeBasePath 定位目录，缺失目录自动创建'
			},
			previewTree() {
				const roots = []
				const nodes = {}
				const paths = (this.preview && this.preview.paths) || []
				paths.slice().sort((a, b) => a.localeCompare(b, 'zh-CN')).forEach(path => {
					const segments = String(path).split('/').filter(Boolean)
					let level = roots
					let current = ''
					segments.forEach((name, index) => {
						current = current ? `${current}/${name}` : name
						let node = nodes[current]
						if (!node) {
							node = {
								id: current,
								label: name,
								isFile: index === segments.length - 1 && name.toLowerCase().endsWith('.md'),
								children: []
							}
							nodes[current] = node
							level.push(node)
						}
						level = node.children
					})
				})
				return roots
			}
		},
		watch: {
			visible(value) {
				if (value) this.importForm.targetParentId = this.targetParentId
			}
		},
		beforeDestroy() { this.stopPolling() },
		methods: {
			onUploadChange(file) { this.importFile = file.raw || null },
			onUploadRemove() { this.importFile = null },
			onUploadExceed() { this.msgError('每次只能选择一个 ZIP 文件，请先移除当前文件') },
			onModeChange() { this.clearSelectedFiles() },
			onFilesChange(file, fileList) {
				this.importFiles = (fileList || []).map(item => item.raw).filter(Boolean)
			},
			clearSelectedFiles() {
				this.importFile = null
				this.importFiles = []
				if (this.$refs.importUpload) this.$refs.importUpload.clearFiles()
				if (this.$refs.filesUpload) this.$refs.filesUpload.clearFiles()
			},
			doPreview() {
				if (this.importMode === 'zip') {
					if (!this.importFile) return this.msgError('请选择 ZIP 文件')
					this.previewing = true
					previewImport(this.importFile, this.importForm).then(res => { this.preview = res.data; this.importStep = 'preview' }).finally(() => { this.previewing = false })
					return
				}
				if (!this.importFiles.length) return this.msgError('请选择 Markdown 文件')
				this.previewing = true
				previewImportFiles(this.importFiles, this.importForm).then(res => { this.preview = res.data; this.importStep = 'preview' }).finally(() => { this.previewing = false })
			},
			doExecute() { executeImport(this.preview.token).then(res => { this.importStep = 'progress'; this.poll(res.data.taskId) }) },
			poll(taskId) {
				this.stopPolling()
				const load = () => getImportProgress(taskId).then(res => {
					this.progress = res.data
					if (this.terminal) {
						this.stopPolling()
						if (this.progress.status === 'SUCCESS') this.$emit('success')
					}
				}).catch(() => this.stopPolling())
				load(); this.pollTimer = window.setInterval(load, 1000)
			},
			stopPolling() { if (this.pollTimer) window.clearInterval(this.pollTimer); this.pollTimer = null },
			resetImport() {
				this.stopPolling(); this.importStep = 'form'; this.importMode = 'zip'; this.preview = {}; this.progress = {progress: 0}
				this.clearSelectedFiles()
			},
			downloadSampleZip() {
				const link = document.createElement('a')
				link.href = `${import.meta.env.BASE_URL}knowledge-import-sample.zip`
				link.download = 'knowledge-import-sample.zip'
				document.body.appendChild(link)
				link.click()
				document.body.removeChild(link)
			},
			metaSampleText() {
				const blocks = this.fileSamples.map(file => `# ${file.path}\n${file.json}`)
				return [this.zipSample, ...blocks].join('\n\n')
			},
			copyMetaSample() {
				const text = this.metaSampleText()
				const done = () => this.msgSuccess('示例已复制到剪贴板')
				if (navigator.clipboard && navigator.clipboard.writeText) {
					navigator.clipboard.writeText(text).then(done).catch(() => this.fallbackCopy(text, done))
				} else {
					this.fallbackCopy(text, done)
				}
			},
			fallbackCopy(text, done) {
				const el = document.createElement('textarea')
				el.value = text
				el.style.position = 'fixed'
				el.style.opacity = '0'
				document.body.appendChild(el)
				el.select()
				try { document.execCommand('copy'); done() } catch (e) { this.msgError('复制失败，请手动选择复制') }
				document.body.removeChild(el)
			}
		}
	}
</script>

<style scoped>
	.preview-list { height: 300px; margin-top: 12px; border: 1px solid #ebeef5; padding: 10px; }
	.preview-tree { background: transparent; }
	.preview-tree ::v-deep .el-tree-node__content { height: 28px; }
	.preview-tree ::v-deep .el-tree-node__expand-icon { padding: 6px; }
	.preview-tree-node { display: inline-flex; align-items: center; gap: 6px; }
	.preview-tree-node i { color: #409eff; }
	.preview-tree-node i.el-icon-folder { color: #e6a23c; }
	.preview-tree-label { white-space: nowrap; }
	.import-steps { margin: -4px 0 28px; }
	.import-mode { display: flex; align-items: center; gap: 14px; margin-bottom: 16px; }
	.import-mode-tip { color: #909399; font-size: 12px; line-height: 1.5; }
	.import-section { padding: 18px; background: #f8fafc; border: 1px solid #ebeef5; border-radius: 8px; }
	.import-section-title { margin-bottom: 14px; color: #303133; font-weight: 600; }
	.import-section-title i { margin-right: 7px; color: #409eff; }
	.import-section ::v-deep .el-upload, .import-section ::v-deep .el-upload-dragger { width: 100%; }
	.import-section ::v-deep .el-upload-dragger { height: 156px; padding-top: 20px; border-radius: 7px; background: #fff; }
	.import-section ::v-deep .el-upload-dragger .el-icon-upload { margin: 16px 0 10px; font-size: 54px; }
	.import-section ::v-deep .el-upload-list__item { margin-top: 10px; background: #fff; border: 1px solid #e4e7ed; border-radius: 5px; }
	.import-options { margin-top: 20px; }
	.meta-entry-section { margin-top: 16px; }
	.meta-entry { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
	.meta-entry-text { display: flex; align-items: center; gap: 8px; color: #606266; font-size: 13px; line-height: 1.6; }
	.meta-entry-text i { color: #409eff; font-size: 16px; }
	.meta-entry-actions { flex: 0 0 auto; display: flex; gap: 8px; }
	.meta-drawer-body { padding: 0 20px 24px; }
	.meta-drawer-actions { display: flex; gap: 10px; margin-bottom: 16px; }
	.meta-help-desc { margin: 0 0 14px; color: #606266; font-size: 13px; line-height: 1.7; }
	.meta-help-desc code { padding: 1px 5px; color: #c7254e; background: #fbeef1; border-radius: 3px; font-size: 12px; }
	.meta-sample { margin-bottom: 12px; border: 1px solid #e4e7ed; border-radius: 7px; overflow: hidden; }
	.meta-sample-bar { display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; color: #303133; font-size: 13px; font-weight: 600; background: #eef2f7; }
	.meta-sample-bar i { margin-right: 6px; color: #409eff; }
	.meta-sample-code { margin: 0; padding: 12px 14px; color: #476582; background: #fff; font-family: Consolas, Monaco, "Courier New", monospace; font-size: 12px; line-height: 1.65; white-space: pre; overflow-x: auto; }
	.meta-file-list { display: flex; flex-direction: column; }
	.meta-fields { margin-top: 4px; }
	.conflict-options { width: 100%; display: flex; }
	.conflict-options ::v-deep .el-radio-button { flex: 1; }
	.conflict-options ::v-deep .el-radio-button__inner { width: 100%; padding: 13px 16px; }
	.publish-option { margin-bottom: 0; padding: 14px 16px; border: 1px solid #ebeef5; border-radius: 7px; }
	.option-line { display: flex; align-items: center; justify-content: space-between; }
	.option-line p { margin: 5px 20px 0 0; color: #909399; line-height: 1.5; }
	.preview-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin: 20px 0; }
	.preview-stats > div { padding: 18px 12px; text-align: center; background: #f7f9fc; border-radius: 7px; }
	.preview-stats strong, .preview-stats span { display: block; }
	.preview-stats strong { color: #409eff; font-size: 26px; line-height: 1.2; }
	.preview-stats span { margin-top: 6px; color: #909399; font-size: 13px; }
	.preview-title { color: #303133; font-weight: 600; }
	.import-progress-card { min-height: 240px; display: flex; align-items: center; justify-content: center; gap: 38px; padding: 24px; background: #f8fafc; border-radius: 8px; }
	.progress-detail { min-width: 0; width: 340px; }
	.progress-detail h3 { margin: 0 0 14px; color: #303133; }
	.progress-detail p { color: #606266; }
	.progress-counts { display: flex; gap: 10px; margin: 14px 0; }
	.progress-counts span { padding: 6px 10px; color: #409eff; background: #ecf5ff; border-radius: 4px; font-size: 13px; }
	.current-path { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #909399 !important; }
</style>

<style>
	.knowledge-import-dialog .el-dialog__body { max-height: 66vh; overflow-y: auto; }
	.knowledge-meta-drawer .el-drawer__body { padding: 0; overflow-y: auto; }
	.knowledge-meta-drawer .el-drawer__header { margin-bottom: 14px; color: #303133; font-weight: 600; }
</style>
