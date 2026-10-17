import axios from '@/util/request'

export function getGlobalVisitorMap(params = {}) {
	return axios({
		url: 'visitor-map/global',
		method: 'GET',
		params
	})
}
