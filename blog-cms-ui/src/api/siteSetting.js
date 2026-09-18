import axios from '@/util/request'

export function getSiteSettingData() {
	return axios({
		url: 'siteSettings',
		method: 'GET'
	})
}

export function update(settings, deleteIds) {
	return axios({
		url: 'siteSettings',
		method: 'POST',
		data: {
			settings,
			deleteIds
		}
	})
}

export function uploadSiteImage(id, file) {
	const data = new FormData()
	data.append('file', file)
	return axios({url: `siteSettings/${id}/image`, method: 'POST', data})
}

export function getWebTitleSuffix() {
	return axios({
		url: 'webTitleSuffix',
		method: 'GET'
	})
}
