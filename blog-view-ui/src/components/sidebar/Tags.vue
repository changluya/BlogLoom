<template>
	<!--标签云-->
	<div class="ui segments m-box">
		<div class="ui secondary segment"><i class="tags icon"></i>标签云<span v-if="tagList.length" class="segment-count">{{ tagList.length }}</span></div>
		<div class="ui yellow segment m-padding-small content-segment">
			<router-link v-if="tagList.length" :to="`/tag/${tag.name}`" class="ui label m-text-500" :class="tag.color"
			             :style="tag.fontStyle" v-for="tag in sizedTags" :key="tag.id || tag.name">
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
		computed: {
			// 按文章数量给标签字号做梯度：数量越多字越大，形成标签云效果（12~16px）
			sizedTags() {
				const list = this.tagList || []
				if (!list.length) return []
				const counts = list.map(tag => Number(tag.blogCount) || 0)
				const max = Math.max(...counts)
				const min = Math.min(...counts)
				const span = max - min
				return list.map(tag => {
					const count = Number(tag.blogCount) || 0
					const size = span === 0 ? 11 : 10 + Math.round((count - min) / span * 3)
					return {...tag, fontStyle: {fontSize: size + 'px'}}
				})
			}
		}
	}
</script>

<style scoped>
	.secondary.segment {
		padding: 10px;
	}

	.segment-count {
		margin-left: 6px;
		padding: 0 7px;
		border-radius: 999px;
		background: #fdf6ec;
		color: #e6a23c;
		font-size: 12px;
		line-height: 17px;
		vertical-align: 1px;
	}

	.m-padding-small {
		padding: 6px;
	}

	/* 固定高度上限，标签过多时在面板内上下滚动，保持侧栏整体高度稳定 */
	.content-segment {
		display: flex;
		flex-wrap: wrap;
		gap: 5px;
		align-content: flex-start;
		min-height: 142px;
		max-height: 210px;
		overflow-y: auto;
		overscroll-behavior: contain;
	}

	.content-segment::-webkit-scrollbar { width: 5px; }
	.content-segment::-webkit-scrollbar-thumb { border-radius: 999px; background: #cbd5e1; }
	.content-segment::-webkit-scrollbar-track { background: transparent; }

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

	/* flex-grow 让同一行的标签自动撑满整行宽度 */
	.label {
		flex: 1 1 auto;
		min-width: 0;
		margin: 0 !important;
		padding: 3px 8px !important;
		border-radius: 4px !important;
		line-height: 1.1 !important;
		text-align: center;
		white-space: nowrap;
	}

	.tag-count {
		display: inline-block;
		margin-left: 4px;
		padding: 0 5px;
		border-radius: 999px;
		background: rgba(0, 0, 0, .1);
		font-size: 10px;
		line-height: 14px;
		vertical-align: 1px;
	}
</style>
