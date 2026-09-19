import axios from '@/util/request'

export function getImageHostConfig() {
	return axios({
		url: 'siteSettings',
		method: 'GET'
	})
}

export function updateImageHostConfig(settings) {
	return axios({
		url: 'siteSettings',
		method: 'POST',
		data: {
			settings,
			deleteIds: []
		}
	})
}

export function testImageHost(data) {
	return axios({
		url: 'imageHost/test',
		method: 'POST',
		data
	})
}
