import axios from '@/util/request'

export function getDashboard() {
	return axios({
		url: 'dashboard',
		method: 'GET'
	})
}

export function getDashboardRanking(type) {
	return axios({
		url: 'dashboard/ranking',
		method: 'GET',
		params: {type}
	})
}
