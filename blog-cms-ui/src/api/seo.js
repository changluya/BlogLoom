import axios from '@/util/request'

export function getSeoConfig() {
	return axios({
		url: 'seo/config',
		method: 'GET'
	})
}

export function updateSeoConfig(seoDomain) {
	return axios({
		url: 'seo/config',
		method: 'POST',
		data: {seoDomain}
	})
}

// 快捷查看站点级文件内容（robots / sitemap / rss）
export function getSeoFileContent(name) {
	return axios({
		url: `seo/file/${name}/content`,
		method: 'GET'
	})
}

// 快捷下载站点级文件（robots / sitemap / rss）
export function downloadSeoFile(name) {
	return axios({
		url: `seo/file/${name}`,
		method: 'GET',
		responseType: 'blob'
	})
}

// SEO 平台关联（站点验证）
export function getSeoVerification() {
	return axios({
		url: 'seo/verification',
		method: 'GET'
	})
}

export function updateSeoVerification(data) {
	return axios({
		url: 'seo/verification',
		method: 'POST',
		data
	})
}
