<template>
	<div ref="container" class="globe-canvas-host"></div>
</template>

<script>
	import {createVisitorGlobe} from './renderer'

	export default {
		name: 'GlobeCanvas',
		props: {
			dataset: {type: Object, required: true},
			language: {type: String, default: 'zh'},
			autoRotate: {type: Boolean, default: true}
		},
		data() {
			return {renderer: null}
		},
		watch: {
			dataset(value) {
				if (this.renderer) this.renderer.setDataset(value)
			},
			language(value) {
				if (this.renderer) this.renderer.setLanguage(value)
			},
			autoRotate(value) {
				if (this.renderer) this.renderer.setAutoRotate(value)
			}
		},
		mounted() {
			this.renderer = createVisitorGlobe(this.$refs.container, this.dataset, {
				onStatus: status => this.$emit('status', status),
				onHover: payload => this.$emit('hover', payload)
			})
			this.renderer.setLanguage(this.language)
			this.renderer.setAutoRotate(this.autoRotate)
		},
		beforeDestroy() {
			if (this.renderer) this.renderer.destroy()
			this.renderer = null
		},
		methods: {
			reset() {
				if (this.renderer) this.renderer.reset()
			}
		}
	}
</script>

<style scoped>
	.globe-canvas-host {
		position: absolute;
		inset: 0;
		overflow: hidden;
	}

	.globe-canvas-host::v-deep canvas {
		display: block;
		width: 100%;
		height: 100%;
		touch-action: none;
		cursor: grab;
	}
</style>
