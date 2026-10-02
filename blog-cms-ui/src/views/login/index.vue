<template>
	<div class="login-page">
		<div class="background-shape shape-one"></div><div class="background-shape shape-two"></div><div class="background-dots"></div>
		<header class="login-header">
			<div class="brand"><img src="/img/site-favicon.png" alt="BlogLoom"><span><strong>BlogLoom</strong> Admin</span></div>
			<a class="home-link" href="/" title="返回博客首页"><i class="el-icon-house"></i><span>返回博客首页</span></a>
		</header>
		<main class="login-layout">
			<section class="intro-panel">
				<div class="intro-copy">
					<h1><span>BlogLoom</span> <em>管理后台</em></h1>
					<p>高效管理内容、专栏、分类与数据，让技术博客运营更轻松</p>
					<div class="title-accent"><span></span><i></i></div>
					<div class="feature-list">
						<div class="feature-item"><span class="feature-icon blue"><i class="el-icon-document"></i></span><div><b>内容管理</b><small>文章、专栏、分类</small></div></div>
						<div class="feature-item"><span class="feature-icon green"><i class="el-icon-data-analysis"></i></span><div><b>数据概览</b><small>流量、用户、排行</small></div></div>
						<div class="feature-item"><span class="feature-icon purple"><i class="el-icon-position"></i></span><div><b>发布运营</b><small>内容发布与站点管理</small></div></div>
					</div>
				</div>
			</section>
			<section class="form-panel">
				<div class="login-card">
					<div class="login-heading"><h2>欢迎登录</h2><p>登录 BlogLoom 管理后台</p><span>专注内容创作、结构管理与运营分析</span></div>
					<el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form" @submit.native.prevent>
						<el-form-item prop="username"><el-input ref="username" v-model="loginForm.username" name="username" autocomplete="username" placeholder="请输入用户名" tabindex="1"><i slot="prefix" class="el-icon-user"></i></el-input></el-form-item>
						<el-form-item prop="password"><el-input ref="password" :key="passwordType" v-model="loginForm.password" :type="passwordType" name="password" autocomplete="current-password" placeholder="请输入密码" tabindex="2" @keyup.enter.native="handleLogin"><i slot="prefix" class="el-icon-lock"></i><i slot="suffix" class="el-icon-view password-toggle" :class="{'is-visible': passwordType !== 'password'}" @click="showPwd"></i></el-input></el-form-item>
						<div class="form-options"><el-checkbox v-model="loginForm.rememberPassword" title="下次进入登录页时自动填充账号密码，仅建议在个人设备上使用">记住密码</el-checkbox><span>管理员账号登录</span></div>
						<el-button class="login-button" type="primary" :loading="loading" @click.native.prevent="handleLogin"><span>登录</span><i class="el-icon-right"></i></el-button>
					</el-form>
					<div class="admin-only"><span></span><p><i class="el-icon-lock"></i> 仅管理员可访问后台系统</p><span></span></div>
				</div>
			</section>
		</main>
	</div>
</template>

<script>
import {login} from '@/api/login'
import {getLoginCredential, saveLoginCredential} from '@/utils/loginCredential'
export default {
	name: 'Login',
	data() { return {loginForm: {username: '', password: '', rememberPassword: false}, loginRules: {username: [{required: true, message: '请输入用户名', trigger: 'blur'}], password: [{required: true, message: '请输入密码', trigger: 'blur'}]}, loading: false, passwordType: 'password'} },
	async mounted() {
		window.localStorage.removeItem('rememberedUsername')
		const credential = await getLoginCredential()
		if (credential) {
			this.loginForm.username = credential.id || ''
			this.loginForm.password = credential.password || ''
			this.loginForm.rememberPassword = true
		}
		this.$nextTick(() => (this.loginForm.username ? this.$refs.password : this.$refs.username).focus())
	},
	methods: {
		showPwd() { this.passwordType = this.passwordType === 'password' ? 'text' : 'password'; this.$nextTick(() => this.$refs.password.focus()) },
		handleLogin() { this.$refs.loginForm.validate(valid => { if (!valid) return; const username = this.loginForm.username.trim(); const password = this.loginForm.password; this.loading = true; login({username, password}).then(async res => { if (this.loginForm.rememberPassword) await saveLoginCredential(username, password); this.msgSuccess(res.msg); window.localStorage.setItem('token', res.data.token); window.localStorage.setItem('user', JSON.stringify(res.data.user)); this.$router.push(this.$route.query.redirect || '/') }).finally(() => { this.loading = false }) }) }
	}
}
</script>

<style lang="scss" scoped>
* { box-sizing:border-box; }
.login-page { position:relative; width:100%; height:100vh; min-height:620px; overflow:hidden; isolation:isolate; color:#10234a; background:radial-gradient(circle at 15% 1%,rgba(255,255,255,.98),transparent 31%),radial-gradient(circle at 82% 12%,rgba(177,216,255,.62),transparent 29%),linear-gradient(135deg,#f9fcff 0%,#edf6ff 48%,#e6f2ff 100%); }
.login-page::before,.login-page::after { position:absolute; z-index:-2; border-radius:70px; background:linear-gradient(145deg,rgba(197,224,255,.38),rgba(226,240,255,.04)); content:''; transform:rotate(45deg); }
.login-page::before { width:720px; height:720px; top:-520px; left:-110px; border:2px solid rgba(75,151,255,.23); background:rgba(255,255,255,.5); }.login-page::after { width:600px; height:600px; right:-330px; bottom:-300px; border:2px solid rgba(103,166,255,.2); background:rgba(205,228,255,.18); }
.background-shape { position:absolute; z-index:-3; border-radius:50%; pointer-events:none; }.shape-one { width:440px; height:440px; top:-205px; right:-35px; background:rgba(119,187,255,.23); }.shape-two { width:420px; height:420px; bottom:-300px; left:-165px; background:radial-gradient(circle at 60% 35%,rgba(75,129,255,.58),rgba(134,181,255,.1) 58%,transparent 70%); }
.background-dots { position:absolute; z-index:-1; right:0; bottom:3%; width:150px; height:150px; opacity:.18; background-image:radial-gradient(#5d9ff2 2.4px,transparent 2.4px); background-size:18px 18px; }
.login-header { display:flex; width:min(1470px,calc(100% - 80px)); height:100px; margin:0 auto; align-items:center; justify-content:space-between; }.brand { display:flex; align-items:center; gap:14px; color:#18233a; font-size:25px; letter-spacing:-.02em; }.brand img { width:48px; height:48px; }.brand strong { color:#071b43; font-weight:850; }.home-link { display:flex; align-items:center; gap:9px; color:#6680a3; font-size:17px; text-decoration:none; transition:.2s; }.home-link:hover { color:#287ff0; transform:translateY(-1px); }.home-link i { font-size:21px; }
.login-layout { display:grid; width:min(1470px,calc(100% - 80px)); height:calc(100vh - 100px); min-height:520px; margin:0 auto; padding:12px 0 58px; grid-template-columns:minmax(0,1fr) minmax(460px,575px); gap:clamp(58px,6vw,100px); align-items:center; }
.intro-panel { display:flex; min-width:0; align-items:center; align-self:stretch; }.intro-copy { width:min(100%,860px); transform:translateY(-6px); }.intro-copy h1 { margin:0; font-size:clamp(46px,3.8vw,60px); line-height:1.08; letter-spacing:-.05em; }.intro-copy h1 span { color:#061a43; font-weight:900; }.intro-copy h1 em { color:#247af1; font-style:normal; font-weight:900; }.intro-copy > p { max-width:800px; margin:16px 0 0; color:#617b9f; font-size:clamp(18px,1.4vw,22px); line-height:1.5; letter-spacing:.01em; }
.title-accent { display:flex; margin-top:18px; align-items:center; gap:13px; }.title-accent span,.title-accent i { display:block; height:5px; border-radius:6px; }.title-accent span { width:54px; background:linear-gradient(90deg,#216df5,#73a6ff); }.title-accent i { width:34px; background:#d9e6f8; }
.feature-list { display:grid; margin-top:25px; grid-template-columns:repeat(3,minmax(0,1fr)); gap:18px; }.feature-item { display:flex; min-width:0; min-height:205px; padding:27px 27px 24px; align-items:flex-start; flex-direction:column; border:1px solid rgba(255,255,255,.9); border-radius:20px; background:rgba(255,255,255,.72); box-shadow:0 16px 38px rgba(63,108,162,.10); backdrop-filter:blur(10px); transition:.2s; }.feature-item:hover { border-color:rgba(124,184,251,.85); background:rgba(255,255,255,.9); transform:translateY(-3px); }.feature-icon { display:grid; width:62px; height:62px; flex:0 0 62px; border-radius:19px; place-items:center; font-size:31px; box-shadow:0 10px 22px rgba(48,124,220,.12); }.feature-icon.blue { color:#168bf5; background:linear-gradient(145deg,#e5f3ff,#cde7ff); }.feature-icon.green { color:#0bb982; background:linear-gradient(145deg,#e1fbf5,#c8f4ea); }.feature-icon.purple { color:#7154ef; background:linear-gradient(145deg,#f1edff,#e3d9ff); }.feature-item b,.feature-item small { display:block; }.feature-item b { margin-top:17px; margin-bottom:6px; color:#071a40; font-size:20px; }.feature-item small { color:#6f87aa; font-size:15px; line-height:1.45; }
.form-panel { display:flex; align-items:center; justify-content:center; }.login-card { width:min(100%,575px); min-height:658px; padding:59px 59px 42px; border:1px solid rgba(255,255,255,.86); border-radius:27px; background:rgba(255,255,255,.94); box-shadow:0 28px 70px rgba(38,86,151,.16),inset 0 1px 0 #fff; backdrop-filter:blur(16px); }.login-heading h2 { margin:0; color:#071a40; font-size:48px; line-height:1; letter-spacing:-.04em; }.login-heading p { margin:25px 0 8px; color:#3f5d83; font-size:24px; }.login-heading span { color:#8499b7; font-size:17px; }
.login-form { margin-top:37px; }.login-form ::v-deep .el-form-item { margin-bottom:34px; }.login-form ::v-deep .el-form-item__error { padding-top:6px; line-height:16px; }.login-form ::v-deep .el-input__inner { height:68px; padding-right:58px; padding-left:67px; border-color:#cbd8e8; border-radius:12px; color:#233b60; font-size:18px; background:rgba(255,255,255,.8); }.login-form ::v-deep .el-input__inner:focus { border-color:#388af7; box-shadow:0 0 0 4px rgba(56,138,247,.10); }.login-form ::v-deep .el-input__prefix { left:26px; color:#3e5e8d; font-size:26px; line-height:68px; }.login-form ::v-deep .el-input__suffix { right:23px; color:#3e5e8d; font-size:23px; line-height:68px; }.password-toggle { cursor:pointer; transition:.2s; }.password-toggle:hover,.password-toggle.is-visible { color:#287ff0; }
.form-options { display:flex; margin-top:0; align-items:center; justify-content:space-between; color:#607a9e; font-size:16px; }.form-options ::v-deep .el-checkbox__inner { width:22px; height:22px; }.form-options ::v-deep .el-checkbox__label { color:#405c82; font-size:16px; }.login-button { display:flex; width:100%; height:76px; margin-top:31px; border:0; border-radius:14px; align-items:center; justify-content:center; gap:18px; font-size:22px; font-weight:750; background:linear-gradient(100deg,#216bf5,#28b9ed); box-shadow:0 14px 28px rgba(35,126,241,.25); transition:.22s; }.login-button:hover { transform:translateY(-2px); box-shadow:0 18px 34px rgba(35,126,241,.31); }.login-button i { font-size:22px; }
.admin-only { display:flex; margin-top:53px; align-items:center; gap:20px; color:#6e87aa; font-size:15px; }.admin-only span { height:1px; flex:1; background:#bdd0e8; }.admin-only p { margin:0; white-space:nowrap; }.admin-only i { margin-right:5px; }
@media (max-height:760px) and (min-width:901px) { .login-header { height:66px; }.login-layout { height:calc(100vh - 66px); padding:4px 0 16px; grid-template-columns:minmax(0,1fr) 460px; }.brand { font-size:20px; }.brand img { width:40px; height:40px; }.intro-copy h1 { font-size:40px; }.intro-copy > p { margin-top:10px; font-size:15px; }.title-accent { margin-top:16px; }.feature-list { margin-top:24px; gap:14px; }.feature-item { min-height:152px; padding:22px 20px; }.feature-icon { width:48px; height:48px; flex-basis:48px; border-radius:15px; font-size:25px; }.feature-item b { margin-top:14px; margin-bottom:4px; font-size:16px; }.feature-item small { font-size:12px; }.login-card { width:460px; min-height:0; padding:30px 36px 24px; border-radius:21px; }.login-heading h2 { font-size:32px; }.login-heading p { margin:15px 0 5px; font-size:18px; }.login-heading span { font-size:13px; }.login-form { margin-top:20px; }.login-form ::v-deep .el-form-item { margin-bottom:27px; }.login-form ::v-deep .el-form-item__error { padding-top:4px; line-height:14px; }.login-form ::v-deep .el-input__inner { height:50px; padding-left:46px; font-size:14px; }.login-form ::v-deep .el-input__prefix { left:15px; font-size:19px; line-height:50px; }.login-form ::v-deep .el-input__suffix { right:15px; font-size:18px; line-height:50px; }.form-options,.form-options ::v-deep .el-checkbox__label { font-size:13px; }.form-options ::v-deep .el-checkbox__inner { width:16px; height:16px; }.login-button { height:50px; margin-top:18px; border-radius:9px; font-size:16px; }.admin-only { margin-top:22px; font-size:12px; } }
@media (max-width:900px) { .login-page { min-height:540px; }.login-header,.login-layout { width:min(560px,calc(100% - 32px)); }.login-header { height:68px; }.login-layout { display:flex; height:calc(100vh - 68px); min-height:472px; padding:12px 0 24px; align-items:center; justify-content:center; }.intro-panel { display:none; }.login-card { width:100%; max-width:430px; min-height:0; padding:34px 34px 26px; border-radius:21px; }.login-heading h2 { font-size:34px; }.login-heading p { margin:17px 0 6px; font-size:18px; }.login-heading span { font-size:13px; }.login-form { margin-top:23px; }.login-form ::v-deep .el-form-item { margin-bottom:28px; }.login-form ::v-deep .el-form-item__error { padding-top:4px; line-height:14px; }.login-form ::v-deep .el-input__inner { height:50px; padding-left:45px; font-size:14px; }.login-form ::v-deep .el-input__prefix { left:14px; font-size:19px; line-height:50px; }.login-form ::v-deep .el-input__suffix { right:14px; font-size:18px; line-height:50px; }.form-options,.form-options ::v-deep .el-checkbox__label { font-size:13px; }.form-options ::v-deep .el-checkbox__inner { width:17px; height:17px; }.login-button { height:52px; margin-top:20px; border-radius:10px; font-size:17px; }.admin-only { margin-top:25px; font-size:12px; } }
@media (max-width:520px) { .login-header { height:62px; }.brand { gap:8px; font-size:17px; }.brand img { width:36px; height:36px; }.home-link span { display:none; }.home-link i { font-size:20px; }.login-layout { height:calc(100vh - 62px); min-height:478px; padding:10px 0 18px; }.login-card { padding:30px 22px 24px; border-radius:17px; }.login-heading h2 { font-size:30px; }.login-heading p { font-size:16px; } }
</style>
