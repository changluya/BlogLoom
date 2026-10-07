<template>
	<div>
		<el-card>
			<div slot="header" class="card-header">
				<div class="card-header-left">
					<span class="card-title">SEO 配置</span>
					<span class="card-tip">配置站点的 SEO 域名，canonical / sitemap / RSS 等绝对地址将统一使用该域名</span>
				</div>
				<el-button type="primary" size="small" icon="el-icon-check" :loading="saving" @click="submit">保存</el-button>
			</div>
			<el-form label-position="right" label-width="120px" size="small">
				<el-form-item label="SEO 域名">
					<el-input v-model="form.seoDomain" placeholder="如 https://blog.changlu.cloud" style="width: 420px"/>
					<div class="field-tip">
						示例：<code>https://blog.changlu.cloud</code>，请填写包含 <code>http://</code> 或 <code>https://</code> 的完整域名（末尾不要带 <code>/</code>）。<br>
						默认读取配置参数 <code>{{ defaultSeoDomain || '-' }}</code>；
						<span v-if="configured">当前已使用后台配置，留空则回退配置参数。</span>
						<span v-else>当前使用配置参数，保存后将以后台配置为准。</span>
					</div>
				</el-form-item>
			</el-form>
		</el-card>

		<el-card class="file-card">
			<div slot="header">
				<span class="card-title">核心文件</span>
				<span class="card-tip">robots.txt / sitemap.xml / rss.xml，支持快捷查看与下载</span>
			</div>
			<el-table :data="files" size="small" border>
				<el-table-column prop="label" label="文件" width="130"/>
				<el-table-column prop="path" label="访问路径" width="150"/>
				<el-table-column prop="desc" label="说明"/>
				<el-table-column label="操作" width="170" align="center">
					<template slot-scope="{row}">
						<el-button size="mini" @click="viewFile(row)">查看</el-button>
						<el-button size="mini" type="primary" @click="downloadFile(row)">下载</el-button>
					</template>
				</el-table-column>
			</el-table>
		</el-card>

		<el-dialog :title="dialog.title" :visible.sync="dialog.visible" width="72%" top="6vh" append-to-body>
			<div v-loading="dialog.loading">
				<pre class="file-content">{{ dialog.content }}</pre>
			</div>
			<div slot="footer">
				<el-button size="small" @click="dialog.visible = false">关闭</el-button>
				<el-button size="small" type="primary" @click="downloadCurrent">下载</el-button>
			</div>
		</el-dialog>
	</div>
</template>

<script>
	import {getSeoConfig, updateSeoConfig, getSeoFileContent, downloadSeoFile} from '@/api/seo'

	export default {
		name: 'SeoConfig',
		data() {
			return {
				saving: false,
				configured: false,
				defaultSeoDomain: '',
				form: {
					seoDomain: ''
				},
				files: [
					{name: 'robots', label: 'robots.txt', path: '/robots.txt', desc: '抓取规则，声明 Sitemap 地址'},
					{name: 'sitemap', label: 'sitemap.xml', path: '/sitemap.xml', desc: '公开文章地图，辅助搜索引擎发现'},
					{name: 'rss', label: 'rss.xml', path: '/rss.xml', desc: '最近公开文章的订阅源'}
				],
				dialog: {
					visible: false,
					loading: false,
					title: '',
					content: '',
					name: ''
				}
			}
		},
		created() {
			this.getData()
		},
		methods: {
			getData() {
				getSeoConfig().then(res => {
					const data = res.data || {}
					this.configured = !!data.configured
					this.defaultSeoDomain = data.defaultSeoDomain || ''
					this.form.seoDomain = data.seoDomain || data.defaultSeoDomain || ''
				})
			},
			submit() {
				const domain = (this.form.seoDomain || '').trim()
				if (domain && !/^https?:\/\/.+/.test(domain)) {
					this.$message.error('SEO 域名需以 http:// 或 https:// 开头')
					return
				}
				this.saving = true
				updateSeoConfig(domain).then(() => {
					this.$message.success('保存成功')
					this.getData()
				}).finally(() => {
					this.saving = false
				})
			},
			viewFile(row) {
				this.dialog.loading = true
				this.dialog.title = row.label
				this.dialog.content = ''
				this.dialog.name = row.name
				this.dialog.visible = true
				getSeoFileContent(row.name).then(res => {
					this.dialog.content = res.data || ''
				}).finally(() => {
					this.dialog.loading = false
				})
			},
			downloadCurrent() {
				const row = this.files.find(item => item.name === this.dialog.name)
				if (row) this.downloadFile(row)
			},
			downloadFile(row) {
				downloadSeoFile(row.name).then(res => {
					const blob = res.data instanceof Blob ? res.data : new Blob([res.data])
					const url = window.URL.createObjectURL(blob)
					const link = document.createElement('a')
					link.href = url
					link.download = row.label
					document.body.appendChild(link)
					link.click()
					document.body.removeChild(link)
					window.URL.revokeObjectURL(url)
				})
			}
		}
	}
</script>

<style scoped>
	.card-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
	}

	.card-header-left {
		display: flex;
		align-items: center;
		min-width: 0;
	}

	.file-card {
		margin-top: 16px;
	}

	.card-title {
		font-weight: 600;
	}

	.card-tip {
		margin-left: 10px;
		color: #909399;
		font-size: 12px;
	}

	.field-tip {
		margin-top: 6px;
		color: #909399;
		font-size: 12px;
		line-height: 1.5;
	}

	.file-content {
		max-height: 60vh;
		margin: 0;
		padding: 14px;
		overflow: auto;
		border-radius: 6px;
		background: #0f172a;
		color: #e2e8f0;
		font-family: Menlo, Consolas, monospace;
		font-size: 12px;
		line-height: 1.6;
		white-space: pre-wrap;
		word-break: break-all;
	}
</style>
