<template>
	<!--标签云-->
	<div class="ui segments m-box">
		<div class="ui secondary segment"><i class="tags icon"></i>标签云</div>
		<div class="ui yellow segment m-padding-small content-segment">
			<router-link v-if="tagList.length" :to="`/tag/${tag.name}`" class="ui label m-text-500" :class="tag.color" v-for="(tag,index) in tagList" :key="index">
				{{ tag.name }}<span class="tag-count">{{ tag.blogCount || 0 }}</span>
			</router-link>
			<div v-if="!tagList.length" class="empty-state">
				<div class="empty-icon"><i class="tags icon"></i></div>
				<div class="empty-title">暂无标签</div>
				<div class="empty-description">为文章添加标签后将在这里形成标签云</div>
			</div>
		</div>
	</div>
</template>

<script>
	export default {
		name: "Tags",
		props: {
			tagList: {
				type: Array,
				required: true
			},
		},
	}
</script>

<style scoped>
	.secondary.segment {
		padding: 10px;
	}

	.m-padding-small {
		padding: 7px;
	}

	.content-segment {
		min-height: 142px;
	}

	.empty-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		min-height: 126px;
		padding: 10px 6px;
		color: #98a2b3;
		text-align: center;
	}

	.empty-icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 42px;
		height: 42px;
		margin-bottom: 10px;
		border-radius: 50%;
		background: #f3f6f9;
		color: #9aa8b8;
		font-size: 18px;
	}

	.empty-icon i { margin: 0; }
	.empty-title { color: #606b79; font-size: 14px; font-weight: 600; }
	.empty-description { margin-top: 5px; font-size: 12px; line-height: 1.6; }

	.label {
		margin: 3px !important;
	}

	.tag-count {
		display: inline-block;
		margin-left: 5px;
		padding: 0 6px;
		border-radius: 999px;
		background: rgba(0, 0, 0, .1);
		font-size: 11px;
		line-height: 16px;
		vertical-align: 1px;
	}
</style>
