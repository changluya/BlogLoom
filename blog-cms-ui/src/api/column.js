import axios from '@/util/request'

export function getColumnTree() { return axios({url: 'columns/tree', method: 'GET'}) }
export function getColumnOptions() { return axios({url: 'columns/options', method: 'GET'}) }
export function saveColumn(data) { return axios({url: 'column', method: 'POST', data}) }
export function updateColumn(data) { return axios({url: 'column', method: 'PUT', data}) }
export function moveColumn(id, data) { return axios({url: `column/${id}/move`, method: 'PUT', data}) }
export function updateColumnPublished(id, published) { return axios({url: `column/${id}/published`, method: 'PUT', params: {published}}) }
export function deleteColumn(id) { return axios({url: 'column', method: 'DELETE', params: {id}}) }
export function uploadColumnCover(id, file) {
	const data = new FormData()
	data.append('file', file)
	return axios({url: `column/${id}/cover`, method: 'POST', data})
}
