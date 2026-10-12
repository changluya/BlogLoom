<template>
	<div v-if="slot" class="article-ad" :class="`article-ad-${position}`">
		<ins ref="ad" class="adsbygoogle" :style="adStyle"
			:data-ad-client="config.client" :data-ad-slot="slot.slot"
			:data-ad-format="slot.format || 'auto'"
			:data-ad-layout="slot.layout || null"
			:data-ad-layout-key="slot.layoutKey || null"
			:data-full-width-responsive="slot.fullWidth == null ? null : String(slot.fullWidth)" />
	</div>
</template>

<script>
	import {pushAdsense} from '@/util/adsense'

	export default {
		name: 'AdsenseSlot',
		props: {
			config: {type: Object, default: () => ({})},
			position: {type: String, required: true}
		},
		computed: {
			adStyle() { return {display: 'block', textAlign: (this.slot && this.slot.alignment) || 'center'} },
			slot() {
				if (!this.config || this.config.enabled === false || !this.config.client) return null
				return (this.config.slots || []).find(item => item && item.enabled !== false &&
					item.position === this.position && item.slot)
			}
		},
		watch: {
			slot(value) {
				if (value) this.$nextTick(() => pushAdsense(this.$refs.ad))
			}
		},
		mounted() {
			this.$nextTick(() => pushAdsense(this.$refs.ad))
		}
	}
</script>

<style scoped>
	.article-ad { width:100%; min-height:90px; margin:20px 0; overflow:hidden; text-align:center; }
	@media (max-width:768px) { .article-ad { min-height:72px; margin:14px 0; } }
</style>
