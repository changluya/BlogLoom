import axios from '@/util/request'

export function login({username, password}) {
	return axios({
		url: 'login',
		method: 'POST',
		data: {username, password}
	})
}
