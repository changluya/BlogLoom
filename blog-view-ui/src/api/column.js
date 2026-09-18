import axios from '@/plugins/axios'

export function getColumnTree() { return axios({url: 'columns/tree', method: 'GET'}) }
export function getColumnDetail(id) { return axios({url: `column/${id}`, method: 'GET'}) }
export function getColumnBlogs(id, pageNum) { return axios({url: `column/${id}/blogs`, method: 'GET', params: {pageNum}}) }
