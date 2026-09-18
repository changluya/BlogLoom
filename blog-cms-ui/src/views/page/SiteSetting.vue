<template>
	<div>
		<div class="page-actions">
			<el-button type="primary" size="small" icon="el-icon-check" @click="submit">保存全部</el-button>
		</div>
		<el-row :gutter="20">
			<el-col :span="12">
				<el-card>
					<div slot="header">
						<span>基础设置</span>
					</div>
					<el-form label-position="right" label-width="100px">
						<el-form-item v-for="item in basicSettings" :key="item.id">
							<template slot="label">
								<span>{{ settingLabel(item) }}</span>
								<el-tooltip v-if="settingTip(item)" :content="settingTip(item)" placement="top">
									<i class="el-icon-question setting-tip-icon"></i>
								</el-tooltip>
							</template>
							<div v-if="item.nameEn === 'copyright'" class="copyright-editor">
								<el-input v-model="item.value.title" size="mini"><template slot="prepend">版权文案</template></el-input>
								<el-input v-model="item.value.siteName" size="mini"><template slot="prepend">站点名称</template></el-input>
								<el-input v-model="item.value.siteUrl" size="mini" placeholder="/"><template slot="prepend">站点链接</template></el-input>
							</div>
							<div v-else-if="item.nameEn === 'hitokotoTexts'" class="roll-text-editor">
								<div class="roll-text-row" :class="{'is-dragging': draggingRollTextIndex === index}"
								     v-for="(text, index) in item.rollTexts" :key="index"
								     @dragover.prevent @drop="dropRollText(item, index)">
									<span class="roll-text-drag-handle" draggable="true" title="拖拽调整顺序"
									      @dragstart="startRollTextDrag(index, $event)" @dragend="draggingRollTextIndex = null">
										<i class="el-icon-rank"></i>
									</span>
									<el-input v-model="item.rollTexts[index]" size="mini" :placeholder="`第 ${index + 1} 条文案`"></el-input>
									<el-button type="danger" size="mini" icon="el-icon-delete" plain
									           :disabled="item.rollTexts.length === 1" @click="removeRollText(item, index)"></el-button>
								</div>
								<el-button type="primary" size="mini" icon="el-icon-plus" plain @click="addRollText(item)">添加文案</el-button>
							</div>
							<div v-else-if="isImageSetting(item)" class="site-image-editor">
								<ImageUpload v-model="item.value" :object-fit="item.nameEn === 'favicon' ? 'contain' : 'cover'"
								             :loading="uploadingId === item.id" @change="file => uploadImage(item, file)"/>
								<div>
									<el-input v-model="item.value" size="mini"></el-input>
									<span class="image-tip">点击图片可重新上传，历史 /img/** 地址继续兼容</span>
								</div>
							</div>
							<el-input v-else v-model="item.value" size="mini"></el-input>
						</el-form-item>
					</el-form>
				</el-card>
			</el-col>
			<el-col :span="12">
				<el-card>
					<div slot="header">
						<span>资料卡</span>
					</div>
					<el-form label-position="right" label-width="100px">
						<el-form-item v-for="item in typeMap.type2" :key="item.id">
							<template slot="label">
								<span>{{ item.nameZh }}</span>
								<el-tooltip v-if="settingTip(item)" :content="settingTip(item)" placement="top">
									<i class="el-icon-question setting-tip-icon"></i>
								</el-tooltip>
							</template>
							<div v-if="item.nameEn === 'rollText'" class="roll-text-editor">
								<div class="roll-text-row" :class="{'is-dragging': draggingRollTextIndex === index}"
								     v-for="(text, index) in item.rollTexts" :key="index"
								     @dragover.prevent @drop="dropRollText(item, index)">
									<span class="roll-text-drag-handle" draggable="true" title="拖拽调整顺序"
									      @dragstart="startRollTextDrag(index, $event)" @dragend="draggingRollTextIndex = null">
										<i class="el-icon-rank"></i>
									</span>
									<el-input v-model="item.rollTexts[index]" size="mini" :placeholder="`第 ${index + 1} 条个签`"></el-input>
									<el-button type="danger" size="mini" icon="el-icon-delete" plain
									           :disabled="item.rollTexts.length === 1" @click="removeRollText(item, index)"></el-button>
								</div>
								<el-button type="primary" size="mini" icon="el-icon-plus" plain @click="addRollText(item)">添加个签</el-button>
							</div>
							<div v-else-if="item.nameEn=='favorite'">
								<el-col :span="20">
									<el-input v-model="item.value" size="mini"></el-input>
								</el-col>
								<el-col :span="4">
									<el-button type="danger" size="mini" icon="el-icon-delete" @click="deleteFavorite(item)">删除</el-button>
								</el-col>
							</div>
							<div v-else-if="isImageSetting(item)" class="site-image-editor">
								<ImageUpload v-model="item.value" round :loading="uploadingId === item.id" @change="file => uploadImage(item, file)"/>
								<div><el-input v-model="item.value" size="mini"></el-input><span class="image-tip">点击图片可重新上传</span></div>
							</div>
							<div v-else>
								<el-input v-model="item.value" size="mini"></el-input>
							</div>
						</el-form-item>
						<el-button type="primary" size="mini" icon="el-icon-plus" @click="addFavorite">添加自定义</el-button>
					</el-form>
				</el-card>
			</el-col>
		</el-row>

		<el-row style="margin-top: 20px">
			<el-card>
				<div slot="header">
					<span>页脚徽标</span>
				</div>
				<el-form :inline="true" v-for="badge in typeMap.type3" :key="badge.id">
					<el-form-item label="title">
						<el-input v-model="badge.value.title" size="mini"></el-input>
					</el-form-item>
					<el-form-item label="url">
						<el-input v-model="badge.value.url" size="mini"></el-input>
					</el-form-item>
					<el-form-item label="subject">
						<el-input v-model="badge.value.subject" size="mini"></el-input>
					</el-form-item>
					<el-form-item label="value">
						<el-input v-model="badge.value.value" size="mini"></el-input>
					</el-form-item>
					<el-form-item label="color">
						<el-input v-model="badge.value.color" size="mini"></el-input>
					</el-form-item>
					<el-form-item>
						<el-button type="danger" size="mini" icon="el-icon-delete" @click="deleteBadge(badge)">删除</el-button>
					</el-form-item>
				</el-form>
				<el-button type="primary" size="mini" icon="el-icon-plus" @click="addBadge">添加 badge</el-button>
			</el-card>
		</el-row>
	</div>
</template>

<script>
	import Breadcrumb from "@/components/Breadcrumb";
	import ImageUpload from "@/components/ImageUpload";
	import {getSiteSettingData, update, uploadSiteImage} from "@/api/siteSetting";
	import {setFavicon} from '@/util/favicon'
	import _ from 'lodash'

	export default {
		name: "SiteSetting",
		components: {Breadcrumb, ImageUpload},
		data() {
			return {
				deleteIds: [],
				uploadingId: null,
				draggingRollTextIndex: null,
				typeMap: {},
			}
		},
		created() {
			this.getData()
		},
		computed: {
			basicSettings() {
				const settings = (this.typeMap && this.typeMap.type1) || []
				const favicon = settings.filter(item => item.nameEn === 'favicon')
				const others = settings.filter(item => item.nameEn !== 'favicon')
				const blogNameIndex = others.findIndex(item => item.nameEn === 'blogName')
				others.splice(blogNameIndex < 0 ? 0 : blogNameIndex, 0, ...favicon)
				return others
			}
		},
		methods: {
			settingLabel(item) {
				return item && item.nameEn === 'footerImgUrl' ? '页脚二维码' : item.nameZh
			},
			settingTip(item) {
				if (!item) return ''
				if (item.nameEn === 'favicon') return '用于网站 Tab 和后台左上角图标'
				if (item.nameEn === 'footerImgUrl') return '用于前台页脚展示，通常为手机访问本站的二维码'
				if (item.nameEn === 'rollText') return '多条个签请使用英文逗号分隔，并分别用双引号包裹，例如："第一条个签","第二条个签"'
				if (item.nameEn === 'hitokotoTexts') return '前台页脚每次刷新随机展示其中一句，多条请使用英文逗号分隔，并分别用双引号包裹'
				return ''
			},
			isImageSetting(item) {
				return item && ['favicon', 'footerImgUrl', 'reward', 'avatar'].includes(item.nameEn)
			},
			uploadImage(item, file) {
				if (!item.id) return this.msgError('请先保存该配置项')
				this.uploadingId = item.id
				uploadSiteImage(item.id, file).then(res => {
					item.value = res.data.url
					if (item.nameEn === 'favicon') {
						setFavicon(item.value)
						this.$store.dispatch('settings/changeSetting', {key: 'logo', value: item.value})
					}
					this.msgSuccess(res.msg)
				}).finally(() => { this.uploadingId = null })
			},
			getData() {
				getSiteSettingData().then(res => {
					this.typeMap = res.data
					const favicon = res.data.type1.find(item => item.nameEn === 'favicon')
					setFavicon(favicon && favicon.value)
					if (favicon && favicon.value) {
						this.$store.dispatch('settings/changeSetting', {key: 'logo', value: favicon.value})
					}
					res.data.type1.forEach(item => {
						if (item.nameEn === 'copyright') {
							try {
								const value = JSON.parse(item.value || '{}')
								item.value = {title: value.title || '', siteName: value.siteName || '', siteUrl: value.siteUrl || '/'}
							} catch (e) {
								item.value = {title: '', siteName: '', siteUrl: '/'}
							}
						}
						if (item.nameEn === 'hitokotoTexts') this.$set(item, 'rollTexts', this.parseRollTexts(item.value))
					})
					res.data.type2.forEach(item => {
						if (item.nameEn === 'rollText') this.$set(item, 'rollTexts', this.parseRollTexts(item.value))
					})
					res.data.type3.forEach(item => {
						item.value = JSON.parse(item.value)
					})
				})
			},
			addFavorite() {
				this.typeMap.type2.push({
					key: Date.now(),
					nameEn: "favorite",
					nameZh: "自定义",
					type: 2,
					value: "{\"title\":\"\",\"content\":\"\"}"
				})
			},
			parseRollTexts(value) {
				const texts = []
				const pattern = /"(.*?)"/g
				let match
				while ((match = pattern.exec(value || '')) !== null) texts.push(match[1])
				if (!texts.length && value) texts.push(value)
				return texts.length ? texts : ['']
			},
			addRollText(item) {
				item.rollTexts.push('')
			},
			removeRollText(item, index) {
				if (item.rollTexts.length > 1) item.rollTexts.splice(index, 1)
			},
			startRollTextDrag(index, event) {
				this.draggingRollTextIndex = index
				event.dataTransfer.effectAllowed = 'move'
			},
			dropRollText(item, targetIndex) {
				const sourceIndex = this.draggingRollTextIndex
				if (sourceIndex === null || sourceIndex === targetIndex) return
				const moved = item.rollTexts.splice(sourceIndex, 1)[0]
				item.rollTexts.splice(targetIndex, 0, moved)
				this.draggingRollTextIndex = null
			},
			addBadge() {
				this.typeMap.type3.push({
					key: Date.now(),
					nameEn: "badge",
					nameZh: "徽标",
					type: 3,
					value: {
						color: "",
						subject: "",
						title: "",
						url: "",
						value: ""
					}
				})
			},
			deleteFavorite(favorite) {
				let arr = this.typeMap.type2
				if (favorite.id) {
					this.deleteIds.push(favorite.id)
					arr.forEach((item, index) => {
						if (item.id === favorite.id) {
							arr.splice(index, 1)
							return
						}
					})
				} else {
					arr.forEach((item, index) => {
						if (item.key === favorite.key) {
							arr.splice(index, 1)
							return
						}
					})
				}
			},
			deleteBadge(badge) {
				let arr = this.typeMap.type3
				if (badge.id) {
					this.deleteIds.push(badge.id)
					arr.forEach((item, index) => {
						if (item.id === badge.id) {
							arr.splice(index, 1)
							return
						}
					})
				} else {
					arr.forEach((item, index) => {
						if (item.key === badge.key) {
							arr.splice(index, 1)
							return
						}
					})
				}
			},
			submit() {
				const result = _.cloneDeep(this.typeMap)
				result.type1.forEach(item => {
					if (item.nameEn === 'copyright') item.value = JSON.stringify(item.value)
					if (item.nameEn === 'hitokotoTexts') {
						item.value = (item.rollTexts || []).map(text => (text || '').trim()).filter(Boolean).map(text => JSON.stringify(text)).join(',')
						delete item.rollTexts
					}
				})
				result.type2.forEach(item => {
					if (item.nameEn === 'rollText') {
						item.value = (item.rollTexts || []).map(text => (text || '').trim()).filter(Boolean).map(text => JSON.stringify(text)).join(',')
						delete item.rollTexts
					}
				})
				result.type3.forEach(item => {
					item.value = JSON.stringify(item.value)
				})
				let updateArr = []
				updateArr.push(...result.type1)
				updateArr.push(...result.type2)
				updateArr.push(...result.type3)
				update(updateArr, this.deleteIds).then(res => {
					this.deleteIds = []
					this.getData()
					this.msgSuccess(res.msg)
				})
			}
		}
	}
</script>

<style scoped>
	.page-actions { display:flex; align-items:center; justify-content:flex-end; margin-bottom:12px; }
	.site-image-editor { display:flex; align-items:center; gap:12px; }
	.site-image-editor > div:last-child { flex:1; min-width:0; }
	.copyright-editor { display:flex; flex-direction:column; gap:8px; }
	.copyright-editor ::v-deep .el-input-group__prepend { width:64px; padding:0 12px; text-align:center; }
	.image-tip { display:block; margin-top:6px; color:#909399; font-size:12px; line-height:1.4; }
	.setting-tip-icon { margin-left:4px; color:#909399; cursor:help; }
	.roll-text-editor { display:flex; flex-direction:column; gap:8px; }
	.roll-text-row { display:flex; align-items:center; gap:8px; }
	.roll-text-row.is-dragging { opacity:.5; }
	.roll-text-drag-handle { display:inline-flex; width:20px; height:28px; flex:0 0 20px; align-items:center; justify-content:center; color:#c0c4cc; cursor:grab; }
	.roll-text-drag-handle:active { cursor:grabbing; }
	.roll-text-drag-handle:hover { color:#409eff; }
	.roll-text-row .el-input { flex:1; }
	.roll-text-row .el-button { flex:0 0 auto; }
</style>
