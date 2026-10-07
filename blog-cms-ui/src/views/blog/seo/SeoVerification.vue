<template>
	<div>
		<el-card>
			<div slot="header" class="card-header">
				<div class="card-header-left">
					<span class="card-title">SEO 平台关联</span>
					<span class="card-tip">把平台给出的验证代码里的 content 值，填到下面标签的引号中即可</span>
				</div>
				<el-button type="primary" size="small" icon="el-icon-check" :loading="saving" @click="submit">保存</el-button>
			</div>

			<div class="tag-list">
				<div class="tag-row" v-for="row in rows" :key="row.key">
					<div class="tag-platform">
						<i :class="row.icon"></i>
						<span>{{ row.label }}</span>
					</div>
					<div class="tag-code">
						<span class="tag-plain">&lt;meta name="</span><span class="tag-name">{{ row.metaName }}</span><span class="tag-plain">" content="</span>
						<el-input
							v-model="form[row.key]"
							size="small"
							class="tag-input"
							:placeholder="row.placeholder"
							clearable
							@blur="normalizeField(row.key)"
						/>
						<span class="tag-plain">" /&gt;</span>
					</div>
					<el-button size="mini" class="tag-copy" @click="copyMeta(row)">复制</el-button>
				</div>
			</div>

			<div class="tip">
				直接把平台给你的那段验证代码<strong>整段粘进去</strong>即可（会自动提取其中的 content 值，无需自己辨认）；
				也可只填 content 值。保存后访问站点首页/聚合页，源码里就会出现这段标签；留空则不输出该标签。
			</div>
		</el-card>
	</div>
</template>

<script>
	import {getSeoVerification, updateSeoVerification} from '@/api/seo'

	export default {
		name: 'SeoVerification',
		data() {
			return {
				saving: false,
				form: {
					baidu: '',
					bing: '',
					google: ''
				},
				rows: [
					{key: 'baidu', label: '百度', icon: 'el-icon-search', metaName: 'baidu-site-verification', placeholder: '粘贴平台验证代码，或直接填 content 值'},
					{key: 'bing', label: 'Bing', icon: 'el-icon-search', metaName: 'msvalidate.01', placeholder: '粘贴平台验证代码，或直接填 content 值'},
					{key: 'google', label: 'Google', icon: 'el-icon-search', metaName: 'google-site-verification', placeholder: '粘贴平台验证代码，或直接填 content 值'}
				]
			}
		},
		created() {
			this.getData()
		},
		methods: {
			getData() {
				getSeoVerification().then(res => {
					const data = res.data || {}
					this.form.baidu = data.baidu || ''
					this.form.bing = data.bing || ''
					this.form.google = data.google || ''
				})
			},
			submit() {
				this.form.baidu = this.normalizeContent(this.form.baidu)
				this.form.bing = this.normalizeContent(this.form.bing)
				this.form.google = this.normalizeContent(this.form.google)
				this.saving = true
				updateSeoVerification({
					baidu: (this.form.baidu || '').trim(),
					bing: (this.form.bing || '').trim(),
					google: (this.form.google || '').trim()
				}).then(() => {
					this.$message.success('保存成功')
					this.getData()
				}).finally(() => {
					this.saving = false
				})
			},
			normalizeField(key) {
				this.form[key] = this.normalizeContent(this.form[key])
			},
			// 兼容两种输入：整段 <meta ... content="xxx" .../>，或直接是 content 值
			normalizeContent(raw) {
				if (!raw) return ''
				const text = String(raw).trim()
				const match = text.match(/content\s*=\s*["']([^"']*)["']/i)
				if (match) return match[1].trim()
				const quoted = text.match(/^["']([^"']*)["']$/)
				if (quoted) return quoted[1].trim()
				return text
			},
			copyMeta(row) {
				const content = (this.form[row.key] || '').trim()
				if (!content) {
					this.$message.warning('请先填写验证 content')
					return
				}
				const tag = `<meta name="${row.metaName}" content="${content}" />`
				this.copyText(tag)
			},
			copyText(text) {
				if (navigator.clipboard && window.isSecureContext) {
					navigator.clipboard.writeText(text).then(() => {
						this.$message.success('已复制')
					}).catch(() => this.fallbackCopy(text))
				} else {
					this.fallbackCopy(text)
				}
			},
			fallbackCopy(text) {
				const input = document.createElement('textarea')
				input.value = text
				input.style.position = 'fixed'
				input.style.opacity = '0'
				document.body.appendChild(input)
				input.select()
				try {
					document.execCommand('copy')
					this.$message.success('已复制')
				} catch (e) {
					this.$message.error('复制失败，请手动复制')
				}
				document.body.removeChild(input)
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

	.card-title {
		font-weight: 600;
	}

	.card-tip {
		margin-left: 10px;
		color: #909399;
		font-size: 12px;
	}

	.tag-list {
		display: flex;
		flex-direction: column;
		gap: 14px;
	}

	.tag-row {
		display: flex;
		align-items: center;
		gap: 12px;
		flex-wrap: wrap;
	}

	.tag-platform {
		display: flex;
		align-items: center;
		gap: 6px;
		width: 84px;
		font-weight: 600;
	}

	.tag-code {
		display: flex;
		align-items: center;
		flex-wrap: wrap;
		flex: 1;
		min-width: 0;
		padding: 7px 12px;
		border-radius: 8px;
		background: #f6f8fa;
		font-family: Menlo, Consolas, monospace;
		font-size: 13px;
		color: #64748b;
	}

	.tag-plain {
		white-space: pre;
	}

	.tag-name {
		color: #c7254e;
	}

	.tag-input {
		width: 360px;
		max-width: 100%;
		margin: 0 2px;
	}

	.tag-copy {
		flex: 0 0 auto;
	}

	.tip {
		margin-top: 14px;
		color: #909399;
		font-size: 12px;
		line-height: 1.7;
	}

	.tip code {
		padding: 1px 5px;
		border-radius: 3px;
		background: #f2f4f7;
		color: #c7254e;
	}
</style>
