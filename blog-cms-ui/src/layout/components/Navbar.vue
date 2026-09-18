<template>
	<div class="navbar">
		<hamburger :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar"/>

		<breadcrumb class="breadcrumb-container"/>

		<div class="right-menu">
			<el-dropdown class="avatar-container" trigger="click">
				<div class="avatar-wrapper">
					<img :src="user.avatar" class="user-avatar">
				</div>
				<el-dropdown-menu slot="dropdown" class="user-dropdown">
					<a target="_blank" href="https://github.com/changluya/BlogLoom">
						<el-dropdown-item>
							<SvgIcon icon-class="github" class-name="svg"/>
							<span>GitHub</span>
						</el-dropdown-item>
					</a>
					<el-dropdown-item @click.native="openPasswordDialog">
						<SvgIcon icon-class="password" class-name="svg"/>
						<span>修改密码</span>
					</el-dropdown-item>
					<el-dropdown-item @click.native="logout">
						<SvgIcon icon-class="logout" class-name="svg"/>
						<span>退出</span>
					</el-dropdown-item>
				</el-dropdown-menu>
			</el-dropdown>
		</div>

		<el-dialog title="修改密码" :visible.sync="pwdDialogVisible" width="420px" append-to-body>
			<el-form :model="pwdForm" label-width="80px">
				<el-form-item label="账号">
					<el-input v-model="pwdForm.username" disabled></el-input>
				</el-form-item>
				<el-form-item label="新密码">
					<el-input v-model="pwdForm.password" type="password" show-password placeholder="请输入新密码"></el-input>
				</el-form-item>
				<el-form-item label="确认密码">
					<el-input v-model="pwdForm.confirm" type="password" show-password placeholder="请再次输入新密码"></el-input>
				</el-form-item>
			</el-form>
			<span slot="footer">
				<el-button @click="pwdDialogVisible = false">取消</el-button>
				<el-button type="primary" :loading="pwdSaving" @click="submitPassword">确定</el-button>
			</span>
		</el-dialog>
	</div>
</template>

<script>
	import {mapGetters} from 'vuex'
	import Breadcrumb from '@/components/Breadcrumb'
	import Hamburger from '@/components/Hamburger'
	import SvgIcon from '@/components/SvgIcon'
	import {changeAccount} from '@/api/account'

	export default {
		components: {
			Breadcrumb,
			Hamburger,
			SvgIcon
		},
		data() {
			return {
				user: null,
				pwdDialogVisible: false,
				pwdSaving: false,
				pwdForm: {
					username: '',
					password: '',
					confirm: ''
				}
			}
		},
		computed: {
			...mapGetters([
				'sidebar',
			])
		},
		created() {
			this.getUserInfo()
		},
		methods: {
			toggleSideBar() {
				this.$store.dispatch('app/toggleSideBar')
			},
			openPasswordDialog() {
				this.pwdForm = {
					username: (this.user && this.user.username) || '',
					password: '',
					confirm: ''
				}
				this.pwdDialogVisible = true
			},
			submitPassword() {
				if (!this.pwdForm.password) return this.msgError('请输入新密码')
				if (this.pwdForm.password !== this.pwdForm.confirm) return this.msgError('两次输入的密码不一致')
				this.pwdSaving = true
				changeAccount({username: this.pwdForm.username, password: this.pwdForm.password}).then(res => {
					this.pwdDialogVisible = false
					this.msgSuccess(res.msg || '修改成功，请重新登录')
					window.localStorage.removeItem('token')
					window.localStorage.removeItem('user')
					this.$router.push('/login')
				}).finally(() => {
					this.pwdSaving = false
				})
			},
			getUserInfo() {
				this.user = JSON.parse(window.localStorage.getItem('user') || null)
				if (!this.user) {
					this.$router.push('/login')
				}
			},
			logout() {
				window.localStorage.removeItem('token')
				window.localStorage.removeItem('user')
				this.$router.push('/login')
				this.msgSuccess('退出成功')
			}
		}
	}
</script>

<style lang="scss" scoped>
	.navbar {
		height: 50px;
		overflow: hidden;
		position: relative;
		background: #fff;
		box-shadow: 0 1px 4px rgba(0, 21, 41, .08);
		user-select: none;

		.hamburger-container {
			line-height: 46px;
			height: 100%;
			float: left;
			cursor: pointer;
			transition: background .3s;
			-webkit-tap-highlight-color: transparent;

			&:hover {
				background: rgba(0, 0, 0, .025)
			}
		}

		.breadcrumb-container {
			float: left;
		}

		.right-menu {
			float: right;
			height: 100%;
			line-height: 50px;

			&:focus {
				outline: none;
			}

			.right-menu-item {
				display: inline-block;
				padding: 0 8px;
				height: 100%;
				font-size: 18px;
				color: #5a5e66;
				vertical-align: text-bottom;

				&.hover-effect {
					cursor: pointer;
					transition: background .3s;

					&:hover {
						background: rgba(0, 0, 0, .025)
					}
				}
			}

			.avatar-container {
				margin-right: 20px;

				.avatar-wrapper {
					margin-top: 5px;
					position: relative;

					.user-avatar {
						cursor: pointer;
						width: 40px;
						height: 40px;
						border-radius: 10px;
					}

					.el-icon-caret-bottom {
						cursor: pointer;
						position: absolute;
						right: -20px;
						top: 0px;
						font-size: 12px;
					}
				}
			}
		}
	}

	.user-dropdown .svg {
		margin-right: 5px;
	}

	.el-dropdown-menu {
		margin: 7px 0 0 0 !important;
		padding: 0 !important;
		border: 0 !important;
	}
</style>
