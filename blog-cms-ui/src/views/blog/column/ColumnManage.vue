<template>
	<div class="column-manage">
		<div class="toolbar">
			<div class="toolbar-left">
				<PageTip title="专栏管理">
					<p>专栏用于把<strong>系列文章聚合在一起</strong>，最多支持两级（一级专栏 + 子专栏）。</p>
					<p>可以手动新建、拖拽调整顺序与层级，也可以<strong>批量导入专栏</strong>或<strong>导出专栏备份</strong>。</p>
					<p>专栏会展示在<strong>博客首页</strong>左侧的「TA 的专栏」中，读者可点击进入专栏连续阅读。</p>
				</PageTip>
				<el-button type="primary" size="small" icon="el-icon-plus" @click="openCreate(0)">新建一级专栏</el-button>
			</div>
			<div class="toolbar-right">
				<el-button size="small" icon="el-icon-upload2" @click="openImport">批量导入专栏</el-button>
				<el-button size="small" icon="el-icon-download" :loading="exporting" @click="exportZip">导出专栏备份</el-button>
			</div>
		</div>
		<el-table ref="columnTable" class="column-tree-table" :data="flatColumns" row-key="id" :row-class-name="rowClassName">
			<el-table-column label="专栏" min-width="300">
				<template v-slot="scope">
					<div class="column-cell" :class="{'child-column-cell': scope.row.parentId !== 0}">
						<span class="drag-handle" title="拖拽排序"><i class="el-icon-rank"></i></span>
						<el-image class="column-logo" :src="scope.row.cover" fit="cover">
							<div slot="error" class="logo-fallback"><i class="el-icon-collection"></i></div>
						</el-image>
						<div class="column-copy"><strong>{{ scope.row.name }}</strong><p>{{ scope.row.description || '暂无简介' }}</p></div>
					</div>
				</template>
			</el-table-column>
			<el-table-column prop="blogCount" label="博客数" width="90"/>
			<el-table-column label="前台展示" width="100"><template v-slot="scope"><el-switch v-model="scope.row.published" :disabled="publishedUpdatingId === scope.row.id" @change="publishedChanged(scope.row)"/></template></el-table-column>
			<el-table-column label="操作" width="360">
				<template v-slot="scope">
					<el-button v-if="scope.row.parentId === 0" size="mini" @click="openCreate(scope.row.id)">新建子专栏</el-button>
					<el-button type="primary" size="mini" @click="openEdit(scope.row)">编辑</el-button>
					<el-button size="mini" @click="openMove(scope.row)">移动</el-button>
					<el-popconfirm title="确定删除该空专栏吗？" @onConfirm="remove(scope.row.id)"><el-button slot="reference" type="danger" size="mini">删除</el-button></el-popconfirm>
				</template>
			</el-table-column>
		</el-table>

		<el-dialog :title="form.id ? '编辑专栏' : '创建专栏'" :visible.sync="editVisible" width="620px" :close-on-click-modal="false" @closed="resetForm">
			<el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
				<el-form-item label="专栏名称" prop="name"><el-input v-model="form.name" maxlength="100" show-word-limit/></el-form-item>
				<el-form-item label="专栏简介"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit/></el-form-item>
				<el-form-item label="父专栏">
					<el-select v-model="form.parentId" :disabled="Boolean(form.id)" style="width:100%"><el-option label="无（一级专栏）" :value="0"/><el-option v-for="item in rootOptions" :key="item.id" :label="item.name" :value="item.id" :disabled="item.id===form.id"/></el-select>
				</el-form-item>
				<el-form-item label="排序"><el-input-number v-model="form.sort" :min="0"/></el-form-item>
				<el-form-item label="前台展示"><el-switch v-model="form.published"/></el-form-item>
				<el-form-item label="专栏Logo">
					<el-upload action="#" :auto-upload="false" :show-file-list="false" accept=".jpg,.jpeg,.png,.webp" :on-change="selectCover">
						<div class="cover-picker"><img v-if="coverPreview || form.cover" :src="coverPreview || form.cover"><i v-else class="el-icon-plus"></i></div>
					</el-upload>
					<div class="upload-tip">支持 jpg、jpeg、png、webp，最大 5MB</div>
				</el-form-item>
			</el-form>
			<span slot="footer"><el-button @click="editVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存</el-button></span>
		</el-dialog>

		<el-dialog title="移动专栏" :visible.sync="moveVisible" width="460px">
			<el-form label-width="90px"><el-form-item label="目标父专栏"><el-select v-model="moveForm.targetParentId" style="width:100%"><el-option label="根级（一级专栏）" :value="0"/><el-option v-for="item in moveParents" :key="item.id" :label="item.name" :value="item.id"/></el-select></el-form-item><el-form-item label="排序"><el-input-number v-model="moveForm.targetSort" :min="0"/></el-form-item></el-form>
			<span slot="footer"><el-button @click="moveVisible=false">取消</el-button><el-button type="primary" @click="submitMove">确定</el-button></span>
		</el-dialog>

		<el-dialog title="批量导入专栏" :visible.sync="importVisible" width="640px" :close-on-click-modal="false" @closed="resetImport">
			<template v-if="importStep === 'form'">
				<el-upload ref="importUpload" drag action="#" accept=".zip,application/zip" :auto-upload="false" :limit="1"
				           :on-change="onImportChange" :on-remove="onImportRemove" :on-exceed="onImportExceed">
					<i class="el-icon-upload"></i>
					<div class="el-upload__text">将 columns.zip 拖到这里，或 <em>点击选择文件</em></div>
					<div slot="tip" class="el-upload__tip">压缩包内需包含 columns.json 与 icons 图标目录，最大支持 100MB，可先<el-link type="primary" :underline="false" @click="downloadTemplate">下载模板 ZIP</el-link>参考</div>
				</el-upload>
				<el-form label-width="100px" class="import-options">
					<el-form-item label="同名专栏">
						<el-radio-group v-model="importForm.conflictPolicy">
							<el-radio label="SKIP">跳过已有</el-radio>
							<el-radio label="RENAME">自动重命名</el-radio>
						</el-radio-group>
					</el-form-item>
					<el-form-item label="前台展示"><el-switch v-model="importForm.published"/></el-form-item>
				</el-form>
			</template>
			<template v-else-if="importStep === 'preview'">
				<el-alert title="预检通过，可以开始导入" type="success" :closable="false" show-icon/>
				<div class="preview-stats">
					<div><strong>{{ preview.rootCount || 0 }}</strong><span>一级专栏</span></div>
					<div><strong>{{ preview.childCount || 0 }}</strong><span>子专栏</span></div>
					<div><strong>{{ preview.iconCount || 0 }}</strong><span>图标</span></div>
					<div><strong>{{ preview.missingIconCount || 0 }}</strong><span>缺失图标</span></div>
				</div>
				<div class="preview-title">即将导入的专栏</div>
				<el-scrollbar class="preview-list"><div v-for="path in preview.paths" :key="path" class="preview-path"><i class="el-icon-collection"></i>{{ path }}</div></el-scrollbar>
			</template>
			<template v-else>
				<div class="import-result">
					<i class="el-icon-success"></i>
					<h3>导入完成</h3>
					<p>新增一级专栏 {{ importResult.createdRootCount || 0 }} 个，子专栏 {{ importResult.createdChildCount || 0 }} 个，导入图标 {{ importResult.importedIconCount || 0 }} 个，跳过 {{ importResult.skippedCount || 0 }} 个。</p>
				</div>
			</template>
			<span slot="footer">
				<el-button v-if="importStep === 'form'" @click="importVisible=false">取消</el-button>
				<el-button v-if="importStep === 'form'" type="primary" :loading="previewing" @click="doPreview">开始预检</el-button>
				<el-button v-if="importStep === 'preview'" @click="importStep='form'">上一步</el-button>
				<el-button v-if="importStep === 'preview'" type="primary" :loading="importing" @click="doExecute">确认导入</el-button>
				<el-button v-if="importStep === 'result'" type="primary" @click="importVisible=false">完成</el-button>
			</span>
		</el-dialog>
	</div>
</template>

<script>
import {getColumnTree, saveColumn, updateColumn, moveColumn, updateColumnPublished, deleteColumn, uploadColumnCover, previewColumnImport, executeColumnImport, exportColumns} from '@/api/column'
import PageTip from '@/components/PageTip'

const emptyForm = () => ({id: null, parentId: 0, name: '', description: '', cover: '', sort: 0, published: true})
export default {
	name: 'ColumnManage',
	components: {PageTip},
	data() { return {tree: [], draggingColumn: null, publishedUpdatingId: null, editVisible: false, moveVisible: false, saving: false, form: emptyForm(), coverFile: null, coverPreview: '', moveTarget: null, moveForm: {targetParentId: 0, targetSort: 0}, importVisible: false, importStep: 'form', importFile: null, previewing: false, importing: false, exporting: false, preview: {}, importResult: {}, importForm: {conflictPolicy: 'SKIP', published: true}, rules: {name: [{required: true, message: '请输入专栏名称', trigger: 'blur'}]}} },
	computed: {
		rootOptions() { return this.tree.filter(item => item.parentId === 0) },
		moveParents() { return this.rootOptions.filter(item => !this.moveTarget || item.id !== this.moveTarget.id) },
		flatColumns() {
			const rows = []
			this.tree.forEach(root => {
				const children = root.children || []
				// 表格采用自行扁平化的两级结构，移除 children 可避免 Element Table 自动渲染展开箭头。
				rows.push({...root, hasChildren: children.length > 0, children: undefined})
				children.forEach(child => rows.push({...child, hasChildren: false, children: undefined}))
			})
			return rows
		}
	},
	created() { this.load() },
	methods: {
		load() { getColumnTree().then(res => { this.tree = res.data || []; this.$nextTick(this.bindRowDrag) }) },
		rowClassName({row}) { return `column-drag-row column-row-${row.id}` },
		bindRowDrag() {
			const table = this.$refs.columnTable && this.$refs.columnTable.$el
			if (!table) return
			table.querySelectorAll('.el-table__body-wrapper tbody tr').forEach((rowEl, index) => {
				const row = this.flatColumns[index]
				if (!row) return
				rowEl.setAttribute('draggable', 'true')
				rowEl.ondragstart = event => { this.draggingColumn = row; rowEl.classList.add('is-dragging'); event.dataTransfer.effectAllowed = 'move' }
				rowEl.ondragend = () => { this.draggingColumn = null; rowEl.classList.remove('is-dragging'); this.clearDragState(table) }
				rowEl.ondragover = event => {
					const mode = this.dropMode(event, rowEl, row)
					if (!this.canDrop(this.draggingColumn, row, mode)) return
					event.preventDefault()
					event.dataTransfer.dropEffect = 'move'
					this.clearDragState(table)
					rowEl.classList.add(`is-drag-over-${mode}`)
				}
				rowEl.ondragleave = () => { rowEl.classList.remove('is-drag-over-before', 'is-drag-over-inside', 'is-drag-over-after') }
				rowEl.ondrop = event => { event.preventDefault(); this.clearDragState(table); this.reorder(this.draggingColumn, row, event, rowEl) }
			})
		},
		clearDragState(table) { table.querySelectorAll('tr').forEach(item => item.classList.remove('is-drag-over-before', 'is-drag-over-inside', 'is-drag-over-after')) },
		dropMode(event, targetElement, target) {
			const rect = targetElement.getBoundingClientRect()
			const offset = event.clientY - rect.top
			// 一级行的中间区域表示“放入该专栏”；上下区域表示同级前后排序。
			if (target.parentId === 0 && offset >= rect.height * .3 && offset <= rect.height * .7) return 'inside'
			return offset > rect.height / 2 ? 'after' : 'before'
		},
		canDrop(source, target, mode) {
			if (!source || !target || source.id === target.id) return false
			const demote = mode === 'inside' || target.parentId !== 0
			// 含有子专栏的一级专栏不能降为二级，否则原子节点会变成第三级。
			return !(source.parentId === 0 && source.hasChildren && demote)
		},
		async reorder(source, target, event, targetElement) {
			const mode = this.dropMode(event, targetElement, target)
			if (!this.canDrop(source, target, mode)) return
			// 一级行中部为放入该一级；上下边缘为根级排序；二级行前后为其所属父专栏内排序。
			const targetParentId = mode === 'inside' ? target.id : target.parentId
			const sourceParentId = source.parentId
			// 后端的 Long 在部分环境中可能被序列化为字符串，统一按字符串比较，避免二级同级列表匹配不到。
			const sameId = (left, right) => String(left) === String(right)
			const targetSiblings = this.flatColumns.filter(item => sameId(item.parentId, targetParentId) && !sameId(item.id, source.id))
			let targetIndex = mode === 'inside' ? targetSiblings.length : targetSiblings.findIndex(item => sameId(item.id, target.id))
			if (targetIndex < 0) targetIndex = targetSiblings.length
			if (mode === 'after') targetIndex += 1
			targetSiblings.splice(targetIndex, 0, {...source, parentId: targetParentId})

			try {
				// 排序请求顺序提交，避免并发更新后重新查询时出现二级专栏顺序未生效或抖动。
				for (let index = 0; index < targetSiblings.length; index += 1) {
					await moveColumn(targetSiblings[index].id, {targetParentId, targetSort: (index + 1) * 10})
				}
				// 跨层级移动后，把原层级剩余专栏的排序值重新压紧。
				if (!sameId(sourceParentId, targetParentId)) {
					const sourceSiblings = this.flatColumns.filter(item => sameId(item.parentId, sourceParentId) && !sameId(item.id, source.id))
					for (let index = 0; index < sourceSiblings.length; index += 1) {
						await moveColumn(sourceSiblings[index].id, {targetParentId: sourceParentId, targetSort: (index + 1) * 10})
					}
				}
				this.msgSuccess(sameId(sourceParentId, targetParentId) ? '排序成功' : '移动成功')
			} finally {
				this.load()
			}
		},
		openCreate(parentId) { this.form = emptyForm(); this.form.parentId = parentId; this.editVisible = true },
		openEdit(row) { this.form = {...row, children: undefined}; this.editVisible = true },
		openMove(row) { this.moveTarget = row; this.moveForm = {targetParentId: row.parentId, targetSort: row.sort}; this.moveVisible = true },
		publishedChanged(row) {
			this.publishedUpdatingId = row.id
			updateColumnPublished(row.id, row.published)
				.then(res => this.msgSuccess(res.msg || (row.published ? '已开启前台展示' : '已关闭前台展示')))
				.catch(() => { row.published = !row.published })
				.finally(() => { this.publishedUpdatingId = null })
		},
		selectCover(file) { if (file.size > 5 * 1024 * 1024) return this.msgError('图片不能超过 5MB'); this.coverFile = file.raw; this.coverPreview = URL.createObjectURL(file.raw) },
		submit() { this.$refs.formRef.validate(async valid => { if (!valid) return; this.saving = true; try { const res = this.form.id ? await updateColumn(this.form) : await saveColumn(this.form); const id = this.form.id || (res.data && res.data.id); if (this.coverFile && id) await uploadColumnCover(id, this.coverFile); this.msgSuccess(res.msg); this.editVisible = false; this.load() } finally { this.saving = false } }) },
		submitMove() { moveColumn(this.moveTarget.id, this.moveForm).then(res => { this.msgSuccess(res.msg); this.moveVisible = false; this.load() }) },
		remove(id) { deleteColumn(id).then(res => { this.msgSuccess(res.msg); this.load() }) },
		resetForm() { if (this.coverPreview) URL.revokeObjectURL(this.coverPreview); this.coverPreview = ''; this.coverFile = null; this.form = emptyForm() },
		openImport() { this.importVisible = true },
		onImportChange(file) { this.importFile = file.raw || null },
		onImportRemove() { this.importFile = null },
		onImportExceed() { this.msgError('每次只能选择一个 ZIP 文件，请先移除当前文件') },
		doPreview() {
			if (!this.importFile) return this.msgError('请选择 ZIP 文件')
			this.previewing = true
			previewColumnImport(this.importFile, this.importForm).then(res => { this.preview = res.data; this.importStep = 'preview' }).finally(() => { this.previewing = false })
		},
		doExecute() {
			this.importing = true
			executeColumnImport(this.preview.token).then(res => { this.importResult = res.data || {}; this.importStep = 'result'; this.load() }).finally(() => { this.importing = false })
		},
		exportZip() {
			this.exporting = true
			exportColumns().then(response => {
				const url = window.URL.createObjectURL(response.data)
				const link = document.createElement('a')
				link.href = url
				link.download = 'columns.zip'
				document.body.appendChild(link); link.click(); document.body.removeChild(link)
				window.URL.revokeObjectURL(url)
			}).finally(() => { this.exporting = false })
		},
		downloadTemplate() {
			const link = document.createElement('a')
			link.href = `${import.meta.env.BASE_URL}columns-template.zip`
			link.download = 'columns-template.zip'
			document.body.appendChild(link); link.click(); document.body.removeChild(link)
		},
		resetImport() {
			this.importStep = 'form'; this.importFile = null; this.preview = {}; this.importResult = {}
			if (this.$refs.importUpload) this.$refs.importUpload.clearFiles()
		}
	}
}
</script>

<style scoped>
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.toolbar-left { display: flex; align-items: center; gap: 8px; }
.toolbar-right { display: flex; gap: 8px; }
.column-tree-table ::v-deep th.el-table__cell { height:44px; background:#f7f8fa; color:#909399; font-weight:500; }
.column-tree-table ::v-deep .el-table__row { height:76px; transition:background-color .2s, opacity .2s, box-shadow .2s; }
.column-tree-table ::v-deep .column-drag-row { cursor:grab; }
.column-tree-table ::v-deep .column-drag-row:active { cursor:grabbing; }
.column-tree-table ::v-deep .column-drag-row.is-dragging { opacity:.42; }
.column-tree-table ::v-deep .column-drag-row.is-drag-over-before td { background:#ecf5ff !important; box-shadow:inset 0 3px 0 #409eff; }
.column-tree-table ::v-deep .column-drag-row.is-drag-over-after td { background:#ecf5ff !important; box-shadow:inset 0 -3px 0 #409eff; }
.column-tree-table ::v-deep .column-drag-row.is-drag-over-inside td { background:#e8f4ff !important; box-shadow:inset 0 0 0 2px #409eff; }
.column-cell { position:relative; display:flex; align-items:center; gap:12px; min-height:52px; }
.child-column-cell { min-height:48px; padding-left:52px; }
.drag-handle { display:inline-flex; width:18px; height:32px; flex:0 0 18px; align-items:center; justify-content:center; color:#c0c4cc; font-size:15px; cursor:grab; }
.column-drag-row:hover .drag-handle { color:#909399; }
.column-copy { min-width:0; flex:1; text-align:left; }
.column-copy strong,.column-copy p { display:block; text-align:left; }
.column-cell p { margin:4px 0 0; color:#909399; font-size:12px; }
.column-logo { width:42px; height:42px; flex:0 0 42px; border-radius:9px; background:#f2f4f7; box-shadow:0 2px 7px rgba(31,45,61,.08); }
.child-column-cell .column-logo { width:36px; height:36px; flex-basis:36px; border-radius:8px; }
.logo-fallback { display:flex; width:100%; height:100%; align-items:center; justify-content:center; color:#909399; font-size:20px; }
.cover-picker { display:flex; width:120px; height:120px; align-items:center; justify-content:center; overflow:hidden; border:1px dashed #d9d9d9; border-radius:8px; color:#8c939d; font-size:28px; }
.cover-picker img { width:100%; height:100%; object-fit:cover; }
.upload-tip { color:#909399; font-size:12px; }
.import-options { margin-top:20px; }
.preview-stats { display:flex; gap:12px; margin:18px 0; }
.preview-stats > div { flex:1; display:flex; flex-direction:column; align-items:center; padding:14px 0; background:#f5f7fa; border-radius:8px; }
.preview-stats strong { font-size:22px; color:#303133; }
.preview-stats span { margin-top:4px; color:#909399; font-size:12px; }
.preview-title { margin-bottom:8px; color:#303133; font-weight:600; }
.preview-list { height:220px; border:1px solid #ebeef5; border-radius:6px; padding:8px 12px; }
.preview-path { display:flex; align-items:center; gap:8px; padding:5px 0; color:#606266; font-size:13px; }
.preview-path i { color:#409eff; }
.import-result { padding:26px 0; text-align:center; }
.import-result i { color:#67c23a; font-size:48px; }
.import-result h3 { margin:12px 0 8px; color:#303133; }
.import-result p { margin:0; color:#909399; font-size:13px; }
</style>
