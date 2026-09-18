<template>
	<el-upload action="#" :auto-upload="false" :show-file-list="false" :accept="accept" :disabled="disabled || loading" :on-change="handleChange">
		<div class="image-upload" :class="{round, disabled: disabled || loading, 'contain-preview': objectFit === 'contain'}" :style="boxStyle">
			<img v-if="value" :src="previewUrl" :style="previewStyle" alt="图片预览">
			<i v-else class="el-icon-plus"></i>
			<div v-if="loading" class="upload-mask"><i class="el-icon-loading"></i></div>
		</div>
	</el-upload>
</template>

<script>
export default {
	name: 'ImageUpload',
	props: {
		value: {type: String, default: ''},
		width: {type: Number, default: 88},
		height: {type: Number, default: 88},
		round: {type: Boolean, default: false},
		objectFit: {type: String, default: 'cover', validator: value => ['cover', 'contain'].includes(value)},
		maxSizeMb: {type: Number, default: 5},
		accept: {type: String, default: '.jpg,.jpeg,.png,.gif,.webp'},
		loading: {type: Boolean, default: false},
		disabled: {type: Boolean, default: false}
	},
	computed: {
		boxStyle() { return {width: `${this.width}px`, height: `${this.height}px`} },
		previewStyle() { return {objectFit: this.objectFit} },
		previewUrl() {
			const url = (this.value || '').trim()
			if (/^https?:\/\//i.test(url) || /^\/\//.test(url)) return url
			// /img/** 等历史地址始终从当前前端站点加载，兼容原 public/img 资源。
			if (url.startsWith('/')) return `${window.location.origin}${url}`
			return url
		}
	},
	methods: {
		handleChange(file) {
			const extension = ((file.name || '').split('.').pop() || '').toLowerCase()
			if (!['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(extension)) {
				return this.$message.error('仅支持 jpg、jpeg、png、gif、webp 图片')
			}
			if (file.size > this.maxSizeMb * 1024 * 1024) {
				return this.$message.error(`图片不能超过 ${this.maxSizeMb}MB`)
			}
			this.$emit('change', file.raw)
		}
	}
}
</script>

<style scoped>
.image-upload { position:relative; display:flex; align-items:center; justify-content:center; overflow:hidden; box-sizing:border-box; border:1px dashed #d9d9d9; border-radius:8px; color:#8c939d; font-size:24px; cursor:pointer; transition:border-color .2s,color .2s; }
.image-upload:hover { border-color:#409eff; color:#409eff; }
.image-upload.round { border-radius:50%; }
.image-upload.disabled { cursor:not-allowed; opacity:.72; }
.image-upload.contain-preview { background:#f5f7fa; }
.image-upload img { width:100%; height:100%; object-fit:cover; }
.upload-mask { position:absolute; inset:0; display:flex; align-items:center; justify-content:center; background:rgba(255,255,255,.72); color:#409eff; font-size:22px; }
</style>
