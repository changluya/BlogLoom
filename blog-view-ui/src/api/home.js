import axios from '@/plugins/axios'

export function getBlogList(pageNum, sort) {
	return axios({
		url: 'blogs',
		method: 'GET',
		params: {
			pageNum,
			sort
		}
	})
}
