<template>
	<div class="sidebar-logo-container">
		<transition name="sidebarLogoFade">
			<router-link key="blogloom-logo" class="sidebar-logo-link" to="/">
				<img v-if="logo" :src="logo" class="sidebar-logo">
				<h1 class="sidebar-title">BlogLoom Admin</h1>
			</router-link>
		</transition>
	</div>
</template>

<script>
	export default {
		name: 'SidebarLogo',
		props: {
			collapse: {
				type: Boolean,
				required: true
			}
		},
		computed: {
			logo() {
				return this.$store.state.settings.logo
			}
		}
	}
</script>

<style lang="scss" scoped>
	.sidebarLogoFade-enter-active {
		transition: opacity 1.5s;
	}

	.sidebarLogoFade-enter,
	.sidebarLogoFade-leave-to {
		opacity: 0;
	}

	.sidebar-logo-container {
		position: relative;
		width: 100%;
		height: 50px;
		line-height: 50px;
		background: #202938;
		text-align: center;
		overflow: hidden;

		& .sidebar-logo-link {
			display: flex;
			align-items: center;
			justify-content: flex-start;
			height: 100%;
			width: 100%;
			padding: 0 14px;
			box-sizing: border-box;
			transition: background-color .2s ease;

			&:hover {
				background: rgba(255, 255, 255, .035);
			}

			& .sidebar-logo {
				flex: 0 0 30px;
				width: 30px;
				height: 30px;
				margin-right: 10px;
				border-radius: 7px;
				object-fit: contain;
				filter: drop-shadow(0 2px 4px rgba(0, 0, 0, .22));
			}

			& .sidebar-title {
				min-width: 0;
				margin: 0;
				color: #fff;
				font-weight: 600;
				line-height: 50px;
				font-size: 14px;
				letter-spacing: .2px;
				font-family: Avenir, Helvetica Neue, Arial, Helvetica, sans-serif;
				white-space: nowrap;
			}
		}

	}
</style>
