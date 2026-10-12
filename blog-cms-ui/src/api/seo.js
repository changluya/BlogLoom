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

export function getAdsenseConfig() {
	return axios({
		url: 'seo/adsense',
		method: 'GET'
	})
}

// 广告投放位置选项（后台下拉数据源，与后端白名单保持一致）
export function getAdsensePositions() {
	return axios({
		url: 'seo/adsense/positions',
		method: 'GET'
	})
}

export function updateAdsenseConfig(data) {
	return axios({
		url: 'seo/adsense',
		method: 'POST',
		data
	})
}
