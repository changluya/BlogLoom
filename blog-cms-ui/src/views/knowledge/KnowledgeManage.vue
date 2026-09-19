<template>
	<div class="knowledge-page">
		<el-card shadow="never">
			<div slot="header" class="toolbar">
				<div class="toolbar-left">
					<PageTip title="知识库管理">
						<p>知识库是<strong>所有博客的统一目录视图</strong>：每一篇博客都会在知识库中有对应位置，就像本地电脑里的文件夹知识库一样。</p>
						<p>你可以创建目录、拖拽归类、重命名、删除，点击文档即可预览或编辑。</p>
						<p>删除博客会先移入<strong>回收站</strong>，可随时恢复；「迁移本地博客」支持批量导入本地 Markdown 并自动归类。</p>
					</PageTip>
					<el-button type="primary" size="small" icon="el-icon-folder-add" @click="createDir">新建目录</el-button>
					<el-button size="small" icon="el-icon-upload2" @click="openImport">迁移本地博客</el-button>
					<el-button size="small" icon="el-icon-refresh" @click="repair">修复未映射博客</el-button>
					<el-button size="small" icon="el-icon-download" :loading="exporting" @click="exportZip">备份知识库</el-button>
				</div>
				<el-button size="small" icon="el-icon-refresh" @click="loadTree">刷新</el-button>
			</div>
			<div class="content">
				<div v-loading="deleting" element-loading-text="正在删除并刷新知识树..." class="tree-panel" :class="{'is-dragging': treeDragging}" :style="{width: leftWidth + 'px'}">
					<div class="tree-toolbar">
						<el-input v-model="treeKeyword" size="small" clearable prefix-icon="el-icon-search"
						          placeholder="搜索文件夹或文档" @input="filterTree"/>
						<div class="tree-toolbar-actions">
							<div>
								<el-button v-if="!batchMode" type="text" size="mini" icon="el-icon-circle-check" @click="toggleBatchMode">选择</el-button>
								<template v-else>
									<el-button type="text" size="mini" @click="toggleBatchMode">取消</el-button>
									<el-button type="text" size="mini" @click="selectAllNodes">全选</el-button>
									<el-button type="text" size="mini" @click="clearSelectedNodes">清空</el-button>
									<el-button type="text" size="mini" class="batch-delete-button" icon="el-icon-delete" :loading="deleting" @click="batchRemove">批量删除</el-button>
								</template>
							</div>
							<div>
							<el-button type="text" size="mini" icon="el-icon-arrow-down" @click="expandAll">全部展开</el-button>
							<el-button type="text" size="mini" icon="el-icon-arrow-up" @click="collapseAll">全部收拢</el-button>
							</div>
						</div>
					</div>
					<el-tree ref="tree" :data="tree" node-key="id" :default-expand-all="false" highlight-current :draggable="!batchMode"
					         :show-checkbox="batchMode"
					         :expand-on-click-node="false" :props="{label: 'name', children: 'children'}"
					         :filter-node-method="filterTreeNode"
					         :allow-drop="allowDrop" @node-drop="onNodeDrop"
					         @node-drag-start="onTreeDragStart" @node-drag-end="onTreeDragEnd"
					         @node-contextmenu="openContextMenu"
					         @node-click="selectNode">
						<span class="tree-row" slot-scope="{data}">
							<i :class="data.type === 'DIR' ? 'el-icon-folder' : 'el-icon-document'"></i>
							<span>{{ data.name }}<span v-if="data.type === 'DOC'">.md</span></span>
							<el-tag v-if="data.blogExists === false" size="mini" type="danger">文章缺失</el-tag>
						</span>
					</el-tree>
					<div v-if="!tree.length" class="empty-tip">暂无知识库节点</div>
				</div>
				<div class="resize-handle" :class="{active: resizing}" @mousedown.prevent="startResize"></div>
				<div class="detail-panel">
					<template v-if="selected">
						<div class="detail-header">
							<div>
								<h3>{{ selected.name }}<span v-if="selected.type === 'DOC'">.md</span></h3>
								<p class="muted">{{ selected.type === 'DIR' ? '目录' : 'Markdown 文档' }}</p>
							</div>
							<div class="detail-actions">
								<el-button size="small" icon="el-icon-edit" @click="rename">重命名</el-button>
								<el-button v-if="selected.type === 'DOC'" size="small" icon="el-icon-download" :loading="documentDownloading" @click="downloadDocument">下载文档</el-button>
								<el-button v-if="selected.type === 'DOC'" type="primary" size="small" @click="editBlog">编辑博客</el-button>
								<el-button v-else type="danger" size="small" :loading="deleting" @click="remove">删除目录</el-button>
							</div>
						</div>
						<div v-if="selected.type === 'DOC'" v-loading="blogLoading" class="blog-preview">
							<el-alert v-if="selected.blogExists === false" title="关联的博客已不存在" type="error" :closable="false" show-icon/>
							<template v-else-if="selectedBlog">
								<div class="blog-meta">
									<span>博客标题：{{ selectedBlog.title }}</span>
									<span v-if="selectedBlog.category">分类：{{ selectedBlog.category.name }}</span>
								</div>
								<mavon-editor v-model="selectedBlog.content" :subfield="false" default-open="preview"
								              :toolbars-flag="false" :editable="false" :scroll-style="true"/>
							</template>
						</div>
						<div v-else class="directory-summary">将目录或文档拖拽到该目录，可调整知识库层级。</div>
					</template>
					<div v-else class="empty-tip">请选择一个节点</div>
				</div>
			</div>
		</el-card>

		<div v-if="contextMenu.visible" class="node-context-menu" :style="{left: contextMenu.x + 'px', top: contextMenu.y + 'px'}" @click.stop>
			<div class="context-menu-item" @click="contextRename"><i class="el-icon-edit"></i>编辑名称</div>
			<div v-if="contextMenu.node && contextMenu.node.type === 'DOC'" class="context-menu-item" @click="contextEditBlog"><i class="el-icon-document-edit"></i>编辑博客</div>
			<div class="context-menu-item danger" @click="contextDelete"><i class="el-icon-delete"></i>删除</div>
		</div>

		<KnowledgeImportDialog :visible.sync="importVisible" @success="loadTree"/>
	</div>
</template>

<script>
	import {getBlogById} from '@/api/blog'
	import {batchDeleteNodes, createDirectory, deleteNode, exportKnowledge, exportKnowledgeDocument, getKnowledgeTree, moveNode, renameNode, repairKnowledge} from '@/api/knowledge'
	import PageTip from '@/components/PageTip'
	import KnowledgeImportDialog from '@/components/KnowledgeImportDialog'

	export default {
		name: 'KnowledgeManage',
		components: {PageTip, KnowledgeImportDialog},
		data() {
			return {
				tree: [], treeKeyword: '', batchMode: false, selected: null, selectedBlog: null, blogLoading: false, importVisible: false,
				exporting: false, documentDownloading: false, deleting: false,
				contextMenu: {visible: false, x: 0, y: 0, node: null}, leftWidth: Number(window.localStorage.getItem('knowledge-tree-width')) || 380, resizing: false, treeDragging: false
			}
		},
		created() { this.loadTree(); document.addEventListener('click', this.closeContextMenu) },
		beforeDestroy() { document.removeEventListener('click', this.closeContextMenu); this.stopResize(); this.removeDragGhost() },
		methods: {
			loadTree() { return getKnowledgeTree().then(res => { this.tree = res.data || []; this.selected = null; this.selectedBlog = null; this.$nextTick(() => this.filterTree(this.treeKeyword)) }) },
			selectNode(node) {
				this.closeContextMenu()
				this.selected = node
				this.selectedBlog = null
				if (node.type !== 'DOC' || node.blogExists === false) return
				const blogId = node.blogId
				this.blogLoading = true
				getBlogById(blogId).then(res => {
					if (this.selected && this.selected.blogId === blogId) this.selectedBlog = res.data
				}).finally(() => { if (this.selected && this.selected.blogId === blogId) this.blogLoading = false })
			},
			openContextMenu(event, node) {
				event.preventDefault()
				this.selectNode(node)
				this.contextMenu = {visible: true, x: event.clientX, y: event.clientY, node}
			},
			closeContextMenu() { this.contextMenu.visible = false },
			startResize() {
				this.resizing = true
				document.addEventListener('mousemove', this.onResize)
				document.addEventListener('mouseup', this.stopResize)
				document.body.style.userSelect = 'none'
				document.body.style.cursor = 'col-resize'
			},
			onResize(event) {
				const content = this.$el.querySelector('.content')
				if (!content) return
				const rect = content.getBoundingClientRect()
				this.leftWidth = Math.max(260, Math.min(event.clientX - rect.left, rect.width - 420))
			},
			stopResize() {
				if (!this.resizing) return
				this.resizing = false
				document.removeEventListener('mousemove', this.onResize)
				document.removeEventListener('mouseup', this.stopResize)
				document.body.style.userSelect = ''
				document.body.style.cursor = ''
				window.localStorage.setItem('knowledge-tree-width', String(Math.round(this.leftWidth)))
			},
			expandAll() { this.setTreeExpanded(this.tree, true) },
			collapseAll() { this.setTreeExpanded(this.tree, false) },
			filterTree(value) { if (this.$refs.tree) this.$refs.tree.filter((value || '').trim()) },
			filterTreeNode(value, data) {
				if (!value) return true
				return (data.name || '').toLowerCase().includes(value.toLowerCase())
			},
			toggleBatchMode() {
				this.batchMode = !this.batchMode
				this.$nextTick(() => { if (this.$refs.tree) this.$refs.tree.setCheckedKeys([]) })
			},
			selectAllNodes() {
				if (!this.$refs.tree) return
				const ids = []
				const collect = nodes => (nodes || []).forEach(node => { ids.push(node.id); collect(node.children || []) })
				collect(this.tree)
				this.$refs.tree.setCheckedKeys(ids)
			},
			clearSelectedNodes() { if (this.$refs.tree) this.$refs.tree.setCheckedKeys([]) },
			batchRemove() {
				const ids = this.$refs.tree ? this.$refs.tree.getCheckedKeys() : []
				if (!ids.length) return this.msgError('请先选择要删除的节点')
				const selected = new Set(ids)
				const nodes = []
				const collect = (items, ancestorSelected) => (items || []).forEach(node => {
					const currentSelected = selected.has(node.id)
					if (currentSelected && !ancestorSelected) nodes.push(node)
					collect(node.children || [], ancestorSelected || currentSelected)
				})
				collect(this.tree, false)
				const documentCount = nodes.reduce((count, node) => count + this.countDocuments(node), 0)
				const message = `确定删除选中的 ${ids.length} 个节点吗？实际将删除 ${documentCount} 篇文档及对应博客，目录下的全部内容也会被级联删除，此操作不可恢复。`
				this.$confirm(message, '批量删除确认', {type: 'warning', confirmButtonText: '确定删除'}).then(() => {
					this.deleting = true
					return batchDeleteNodes(ids)
				}).then(() => {
					this.batchMode = false
					this.msgSuccess('批量删除成功')
					return this.loadTree()
				}).catch(() => {}).finally(() => { this.deleting = false })
			},
			setTreeExpanded(nodes, expanded) {
				(nodes || []).forEach(node => {
					const treeNode = this.$refs.tree && this.$refs.tree.getNode(node.id)
					if (treeNode) treeNode.expanded = expanded
					this.setTreeExpanded(node.children || [], expanded)
				})
			},
			onTreeDragStart(node, event) {
				this.treeDragging = true
				this.removeDragGhost()
				const source = event && event.target && event.target.closest ? event.target.closest('.el-tree-node__content') : null
				if (!source || !event.dataTransfer) return
				const ghost = source.cloneNode(true)
				ghost.classList.add('knowledge-drag-ghost')
				ghost.style.width = `${Math.max(180, Math.min(source.offsetWidth, 360))}px`
				document.body.appendChild(ghost)
				this.dragGhost = ghost
				event.dataTransfer.setDragImage(ghost, 22, Math.min(18, Math.max(8, ghost.offsetHeight / 2)))
			},
			onTreeDragEnd() { this.treeDragging = false; this.removeDragGhost() },
			removeDragGhost() {
				if (this.dragGhost && this.dragGhost.parentNode) this.dragGhost.parentNode.removeChild(this.dragGhost)
				this.dragGhost = null
			},
			contextRename() { this.closeContextMenu(); this.rename() },
			contextEditBlog() { this.closeContextMenu(); this.editBlog() },
			contextDelete() {
				this.closeContextMenu(); this.remove()
			},
			createDir() {
				const parentId = this.selected && this.selected.type === 'DIR' ? this.selected.id : 0
				this.$prompt('请输入目录名称', '新建目录').then(({value}) => createDirectory({parentId, name: value})).then(() => { this.msgSuccess('创建成功'); this.loadTree() }).catch(() => {})
			},
			rename() { this.$prompt('请输入新名称', '重命名', {inputValue: this.selected.name}).then(({value}) => renameNode(this.selected.id, value)).then(() => { this.msgSuccess('重命名成功'); this.loadTree() }).catch(() => {}) },
			allowDrop(draggingNode, dropNode, type) {
				return type !== 'inner' || dropNode.data.type === 'DIR'
			},
			onNodeDrop(draggingNode, dropNode, type) {
				this.treeDragging = false
				this.removeDragGhost()
				let parentId
				let targetSort
				if (type === 'inner') {
					parentId = dropNode.data.id
					targetSort = (dropNode.data.children || []).findIndex(item => item.id === draggingNode.data.id)
				} else {
					parentId = dropNode.data.parentId || 0
					const siblings = parentId === 0 ? this.tree : (this.$refs.tree.getNode(parentId).data.children || [])
					targetSort = siblings.findIndex(item => item.id === draggingNode.data.id)
				}
				moveNode(draggingNode.data.id, parentId, Math.max(0, targetSort)).then(() => {
					this.msgSuccess('移动成功')
					this.loadTree()
				}).catch(() => this.loadTree())
			},
			countDocuments(node) { return node.type === 'DOC' ? 1 : (node.children || []).reduce((count, child) => count + this.countDocuments(child), 0) },
			remove() {
				if (!this.selected) return
				const documentCount = this.countDocuments(this.selected)
				const message = this.selected.type === 'DOC'
					? `确定删除文档“${this.selected.name}.md”吗？对应博客将移入回收站，可稍后恢复。`
					: `确定删除目录“${this.selected.name}”吗？其下 ${documentCount} 篇文档对应的博客将移入回收站，目录结构不保留。`
				this.$confirm(message, '删除确认', {type: 'warning', confirmButtonText: '确定删除'}).then(() => {
					this.deleting = true
					return deleteNode(this.selected.id)
				}).then(() => {
					this.msgSuccess('删除成功')
					return this.loadTree()
				}).catch(() => {}).finally(() => { this.deleting = false })
			},
			editBlog() { this.$router.push(`/blog/edit/${this.selected.blogId}`) },
			repair() { repairKnowledge().then(res => { this.msgSuccess(`已补建 ${res.data.created} 个节点`); this.loadTree() }) },
			exportZip() {
				this.exporting = true
				exportKnowledge().then(response => {
					const url = window.URL.createObjectURL(response.data)
					const link = document.createElement('a')
					link.href = url
					link.download = 'blogloom-knowledge.zip'
					document.body.appendChild(link); link.click(); document.body.removeChild(link)
					window.URL.revokeObjectURL(url)
				}).finally(() => { this.exporting = false })
			},
			downloadDocument() {
				if (!this.selected || this.selected.type !== 'DOC') return
				this.documentDownloading = true
				exportKnowledgeDocument(this.selected.id).then(response => {
					const url = window.URL.createObjectURL(response.data)
					const link = document.createElement('a')
					link.href = url
					link.download = `${this.selected.name}.md`
					document.body.appendChild(link); link.click(); document.body.removeChild(link)
					window.URL.revokeObjectURL(url)
				}).finally(() => { this.documentDownloading = false })
			},
			openImport() { this.importVisible = true },
		}
	}
</script>

<style scoped>
	.knowledge-page { padding: 20px; }
	.toolbar, .content, .tree-row { display: flex; align-items: center; }
	.toolbar { justify-content: space-between; }
	.toolbar-left { display: flex; align-items: center; gap: 8px; }
	.content { align-items: stretch; height: calc(100vh - 190px); min-height: 520px; overflow: hidden; }
	.tree-panel { flex: 0 0 auto; min-width: 260px; padding: 12px 16px 12px 0; overflow: auto; }
	.tree-panel ::v-deep .el-tree-node__content, .tree-panel ::v-deep .tree-row { cursor: move; }
	.tree-panel.is-dragging, .tree-panel.is-dragging ::v-deep .el-tree-node__content, .tree-panel.is-dragging ::v-deep .tree-row { cursor: grabbing !important; }
	.tree-panel.is-dragging ::v-deep .el-tree-node.is-dragging > .el-tree-node__content { opacity: .65; background: #ecf5ff; }
	.tree-toolbar { position: sticky; top: -12px; z-index: 2; padding: 5px 0 3px; background: #fff; border-bottom: 1px solid #f0f2f5; }
	.tree-toolbar-actions { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 0 12px; min-height: 30px; }
	.batch-delete-button { color: #f56c6c; }
	.resize-handle { position: relative; flex: 0 0 7px; cursor: col-resize; background: #f5f7fa; border-left: 1px solid #ebeef5; border-right: 1px solid #ebeef5; transition: background .15s; }
	.resize-handle:hover, .resize-handle.active { background: #d9ecff; }
	.resize-handle::after { content: ''; position: absolute; width: 2px; height: 36px; background: #c0c4cc; left: 2px; top: 50%; transform: translateY(-50%); }
	.detail-panel { flex: 1 1 auto; min-width: 420px; padding: 24px; overflow: hidden; display: flex; flex-direction: column; box-sizing: border-box; }
	.detail-header { display: flex; flex: 0 0 auto; align-items: flex-start; justify-content: space-between; gap: 20px; border-bottom: 1px solid #ebeef5; margin-bottom: 18px; }
	.detail-header h3 { margin: 0 0 8px; }
	.detail-actions { display: flex; flex-shrink: 0; }
	.blog-preview { flex: 1 1 auto; min-height: 0; overflow-y: auto; padding-right: 8px; }
	.blog-meta { display: flex; gap: 24px; color: #909399; font-size: 13px; margin-bottom: 14px; }
	.blog-preview ::v-deep .v-note-wrapper { min-height: 100%; box-shadow: none; border: 1px solid #ebeef5; z-index: 1; }
	.directory-summary { color: #909399; padding: 30px 0; }
	.tree-row { gap: 7px; }
	.muted { color: #909399; }
	.empty-tip { color: #909399; padding: 60px 20px; text-align: center; }
	.node-context-menu { position: fixed; z-index: 3000; min-width: 140px; padding: 6px 0; background: #fff; border: 1px solid #ebeef5; border-radius: 4px; box-shadow: 0 4px 14px rgba(0,0,0,.14); }
	.context-menu-item { padding: 9px 16px; cursor: pointer; color: #303133; font-size: 14px; }
	.context-menu-item:hover { background: #f5f7fa; }
	.context-menu-item i { margin-right: 8px; }
	.context-menu-item.danger { color: #f56c6c; }
</style>

<style>
	.knowledge-drag-ghost { position: fixed; left: -10000px; top: -10000px; z-index: 9999; display: flex; align-items: center; height: 34px; box-sizing: border-box; padding: 0 12px; overflow: hidden; color: #409eff; background: rgba(236, 245, 255, .88); border: 1px solid rgba(64, 158, 255, .35); border-radius: 5px; box-shadow: 0 5px 14px rgba(64, 158, 255, .18); opacity: .82; }
	.knowledge-drag-ghost .el-tree-node__expand-icon { display: none; }
</style>
