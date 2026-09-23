<template>
	<div>
		<div class="page-actions">
			<el-button type="primary" size="small" icon="el-icon-check" @click="submit">保存全部</el-button>
		</div>
		<el-card>
			<div slot="header">
				<span class="card-title">设置图床</span>
				<span class="image-tip" style="margin-left:10px">选择图片上传渠道，并配置对应渠道的参数</span>
			</div>
			<el-form label-position="right" label-width="120px">
				<el-form-item label="上传渠道选择">
					<el-select v-model="form.uploadChannelChoose" size="small" style="width: 260px">
						<el-option v-for="item in channelOptions" :key="item.value" :label="item.label" :value="item.value"/>
					</el-select>
				</el-form-item>
			</el-form>

			<div class="channel-list">
				<div class="channel-box" :class="{'is-active': form.uploadChannelChoose === 'local'}">
					<div class="channel-header">
						<span class="channel-name">本地</span>
						<el-tag v-if="form.uploadChannelChoose === 'local'" size="mini" effect="dark">当前使用</el-tag>
						<el-button class="test-btn" size="mini" plain icon="el-icon-connection"
						           :loading="testingChannel === 'local'" @click="testChannel('local')">测试连通性</el-button>
					</div>
					<el-form label-position="right" label-width="120px" size="small" class="channel-form">
						<el-form-item label="访问地址">
							<el-input v-model="form.uploadChannelLocal.address" placeholder="本地资源访问地址前缀，如 http://localhost:8090"></el-input>
						</el-form-item>
						<div class="channel-desc local-desc">
							使用系统默认的本地存储目录（conf/upload），无需额外配置。资源访问地址形如
							<code>{{ localSampleUrl }}</code>
							，留空则回退到后端 blog.api 配置。
						</div>
					</el-form>
				</div>

				<div class="channel-box" :class="{'is-active': form.uploadChannelChoose === 'aliyun'}">
					<div class="channel-header">
						<span class="channel-name">阿里云 OSS</span>
						<el-tag v-if="form.uploadChannelChoose === 'aliyun'" size="mini" effect="dark">当前使用</el-tag>
						<el-button class="test-btn" size="mini" plain icon="el-icon-connection"
						           :loading="testingChannel === 'aliyun'" @click="testChannel('aliyun')">测试连通性</el-button>
					</div>
					<el-form label-position="right" label-width="120px" size="small" class="channel-form">
						<el-form-item label="accessKeyId">
							<el-input v-model="form.uploadChannelAliyun.accessKeyId" placeholder="阿里云访问密钥 ID"></el-input>
						</el-form-item>
						<el-form-item label="accessKeySecret">
							<el-input v-model="form.uploadChannelAliyun.accessKeySecret" placeholder="阿里云访问密钥 Secret"></el-input>
						</el-form-item>
						<el-form-item label="bucket">
							<el-input v-model="form.uploadChannelAliyun.bucket" placeholder="OSS Bucket 名称"></el-input>
						</el-form-item>
						<el-form-item label="area">
							<el-input v-model="form.uploadChannelAliyun.area" placeholder="地域节点，如 oss-cn-beijing"></el-input>
						</el-form-item>
						<el-form-item label="path">
							<el-input v-model="form.uploadChannelAliyun.path" placeholder="上传路径前缀，如 img/2024/"></el-input>
						</el-form-item>
					</el-form>
				</div>
			</div>
		</el-card>
	</div>
</template>

<script>
	import {getImageHostConfig, updateImageHostConfig, testImageHost} from "@/api/imageHost";

	export default {
		name: "ImageHostSetting",
		data() {
			return {
				channelOptions: [
					{label: '本地', value: 'local'},
					{label: '阿里云 OSS', value: 'aliyun'}
				],
				records: [],
				testingChannel: null,
				form: {
					uploadChannelChoose: 'local',
					uploadChannelLocal: {
						address: ''
					},
					uploadChannelAliyun: {
						accessKeyId: '',
						accessKeySecret: '',
						bucket: '',
						area: '',
						path: ''
					}
				}
			}
		},
		computed: {
			localSampleUrl() {
				const address = (this.form.uploadChannelLocal.address || '').trim().replace(/\/+$/, '')
				const base = address || window.location.origin
				return `${base}/static/blogColumn/1/uuid.png`
			}
		},
		created() {
			this.getData()
		},
		methods: {
			getData() {
				getImageHostConfig().then(res => {
					this.records = (res.data && res.data.type6) || []
					const choose = this.records.find(item => item.nameEn === 'uploadChannelChoose')
					if (choose && choose.value) this.form.uploadChannelChoose = choose.value
					const local = this.records.find(item => item.nameEn === 'uploadChannelLocal')
					if (local && local.value) {
						try {
							const value = JSON.parse(local.value)
							this.form.uploadChannelLocal = {
								address: value.address || ''
							}
						} catch (e) {
							// 历史脏数据忽略，保留默认空配置
						}
					}
					const aliyun = this.records.find(item => item.nameEn === 'uploadChannelAliyun')
					if (aliyun && aliyun.value) {
						try {
							const value = JSON.parse(aliyun.value)
							this.form.uploadChannelAliyun = {
								accessKeyId: value.accessKeyId || '',
								accessKeySecret: value.accessKeySecret || '',
								bucket: value.bucket || '',
								area: value.area || '',
								path: value.path || ''
							}
						} catch (e) {
							// 历史脏数据忽略，保留默认空配置
						}
					}
				})
			},
			testChannel(channel) {
				const value = channel === 'aliyun' ? JSON.stringify(this.form.uploadChannelAliyun) : ''
				this.testingChannel = channel
				const payload = {channel, value}
				testImageHost(payload).then(res => {
					this.msgSuccess((res.data && res.data.message) || res.msg)
				}).finally(() => {
					this.testingChannel = null
				})
			},
			submit() {
				const settings = []
				const choose = this.records.find(item => item.nameEn === 'uploadChannelChoose')
				if (choose) settings.push({...choose, value: this.form.uploadChannelChoose})
				const local = this.records.find(item => item.nameEn === 'uploadChannelLocal')
				if (local) settings.push({...local, value: JSON.stringify(this.form.uploadChannelLocal)})
				const aliyun = this.records.find(item => item.nameEn === 'uploadChannelAliyun')
				if (aliyun) settings.push({...aliyun, value: JSON.stringify(this.form.uploadChannelAliyun)})
				if (!settings.length) return this.msgError('未找到图床配置项，请先执行数据库增量脚本')
				updateImageHostConfig(settings).then(res => {
					this.msgSuccess(res.msg)
					this.getData()
				})
			}
		}
	}
</script>

<style scoped>
	.page-actions {
		display: flex;
		align-items: center;
		justify-content: flex-end;
		margin-bottom: 12px;
	}

	.card-title {
		color: #303133;
		font-size: 15px;
		font-weight: 700;
		letter-spacing: .3px;
	}

	.image-tip {
		color: #909399;
		font-size: 12px;
	}

	.channel-list {
		display: flex;
		flex-wrap: wrap;
		gap: 16px;
		margin-top: 8px;
	}

	.channel-box {
		flex: 1 1 360px;
		min-width: 320px;
		padding: 16px;
		border: 1px solid #ebeef5;
		border-radius: 8px;
		background: #fff;
		transition: border-color .2s, box-shadow .2s;
	}

	.channel-box.is-active {
		border-color: #409eff;
		box-shadow: 0 0 10px rgba(64, 158, 255, .55);
	}

	.channel-header {
		display: flex;
		align-items: center;
		gap: 8px;
		margin-bottom: 12px;
	}

	.channel-name {
		color: #303133;
		font-size: 14px;
		font-weight: 700;
	}

	.test-btn {
		margin-left: auto;
	}

	.channel-desc {
		color: #909399;
		font-size: 13px;
		line-height: 1.6;
	}

	.channel-form {
		margin-bottom: -18px;
	}

	.local-desc {
		margin-bottom: 18px;
	}

	.local-desc code {
		padding: 1px 5px;
		border-radius: 4px;
		background: #f4f4f5;
		color: #606266;
		font-size: 12px;
		word-break: break-all;
	}
</style>
