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
