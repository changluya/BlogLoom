<template>
	<!--自定义模块：由站点配置 customModule 控制，内容支持自定义 HTML-->
	<div v-if="visible" class="ui segments m-box custom-module">
		<div v-if="title" class="ui secondary segment"><i class="puzzle piece icon"></i>{{ title }}</div>
		<div class="ui segment custom-module-body" v-html="content"></div>
	</div>
</template>

<script>
	export default {
		name: 'CustomModule',
		props: {
			module: {
				type: Object,
				default: null
			}
		},
		computed: {
			title() {
				return this.module ? (this.module.title || '') : ''
			},
			content() {
				return this.module ? (this.module.content || '') : ''
			},
			visible() {
				return !!(this.module && this.module.enabled && (this.content || this.title))
			}
		}
	}
</script>

<style scoped>
	.custom-module-body {
		padding: 12px;
	}

	/* 用户自定义 HTML 内的图片自适应，避免溢出卡片 */
	.custom-module-body ::v-deep img {
		max-width: 100%;
	}

	.custom-module-body ::v-deep a {
		word-break: break-all;
	}
</style>
