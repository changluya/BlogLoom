<template>
	<el-popover placement="bottom-start" :width="popoverWidth" trigger="hover" popper-class="page-tip-popover">
		<div class="page-tip-pop">
			<div v-if="title" class="page-tip-pop-title">{{ title }}</div>
			<div class="page-tip-pop-text"><slot/></div>
		</div>
		<i slot="reference" class="el-icon-info page-tip-icon" :aria-label="title"></i>
	</el-popover>
</template>

<script>
	export default {
		name: 'PageTip',
		props: {
			title: {
				type: String,
				default: ''
			},
			popoverWidth: {
				type: Number,
				default: 380
			}
		}
	}
</script>

<style scoped>
	.page-tip-icon {
		color: #909399;
		font-size: 16px;
		cursor: pointer;
		transition: color .2s;
	}

	.page-tip-icon:hover {
		color: #409eff;
	}
</style>

<!-- 气泡内容会被挂到 body，需使用非 scoped 样式 -->
<style>
	.page-tip-pop {
		color: #5b6b7c;
		font-size: 12px;
		line-height: 1.75;
	}

	.page-tip-pop-title {
		margin-bottom: 3px;
		color: #303133;
		font-size: 13px;
		font-weight: 600;
	}

	.page-tip-pop-text p {
		margin: 0 0 4px;
	}

	.page-tip-pop-text p:last-child {
		margin-bottom: 0;
	}

	.page-tip-pop-text strong {
		color: #303133;
	}
</style>
