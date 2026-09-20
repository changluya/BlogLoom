import axios from 'axios'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import {Message} from 'element-ui'

const request = axios.create({
	baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8090/admin/',
	timeout: 5000
})

const loginPath = `${import.meta.env.BASE_URL || '/'}login`.replace(/\/{2,}/g, '/')

const redirectToLogin = () => {
	window.localStorage.removeItem('token')
	window.localStorage.removeItem('user')
	if (window.location.pathname !== loginPath) {
		const redirect = `${window.location.pathname}${window.location.search}`
		window.location.replace(`${loginPath}?redirect=${encodeURIComponent(redirect)}`)
	}
}

let CancelToken = axios.CancelToken

// 请求拦截
request.interceptors.request.use(config => {
		//对于访客模式，除GET请求外，都拦截并提示
		const userJson = window.localStorage.getItem('user') || '{}'
		const user = JSON.parse(userJson)
		if (userJson !== '{}' && user.role !== 'ROLE_admin' && config.method !== 'get') {
			config.cancelToken = new CancelToken(function executor(cancel) {
				cancel('演示模式，不允许操作')
			})
			return config
		}

		NProgress.start()
		const token = window.localStorage.getItem('token')
		if (token) {
			config.headers.Authorization = token
		}
		return config
	},
	error => {
		console.info(error)
		return Promise.reject(error)
	}
)

// 响应拦截
request.interceptors.response.use(response => {
	NProgress.done()
	if (response.config.responseType === 'blob') return response
	const res = response.data
		if (res.code !== 200) {
			let msg = res.msg || 'Error'
			Message.error(msg)
			if (res.code === 401 || res.code === 403) {
				redirectToLogin()
			}
			return Promise.reject(new Error(msg))
		}
		return res
	},
	error => {
		NProgress.done()
		console.info(error)
		if (error.response && (error.response.status === 401 || error.response.status === 403)) {
			redirectToLogin()
		}
		Message.error(error.message)
		return Promise.reject(error)
	}
)

export default request
