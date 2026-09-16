import axios from '@/util/request'

export const getKnowledgeTree = () => axios({url: 'knowledge/tree', method: 'GET'})
export const createDirectory = data => axios({url: 'knowledge/directories', method: 'POST', data})
export const renameNode = (id, name) => axios({url: `knowledge/nodes/${id}`, method: 'PUT', data: {name}})
export const moveNode = (id, targetParentId, targetSort) => axios({url: `knowledge/nodes/${id}/move`, method: 'PUT', data: {targetParentId, targetSort}})
export const deleteNode = id => axios({url: `knowledge/nodes/${id}`, method: 'DELETE', timeout: 600000})
export const batchDeleteNodes = ids => axios({url: 'knowledge/nodes/batch-delete', method: 'POST', data: {ids}, timeout: 600000})
export const repairKnowledge = () => axios({url: 'knowledge/repair', method: 'POST'})

export function previewImport(file, options) {
	const data = new FormData()
	data.append('file', file)
	Object.keys(options).forEach(key => data.append(key, options[key]))
	return axios({url: 'knowledge/import/preview', method: 'POST', data, timeout: 600000})
}

export const executeImport = token => axios({url: `knowledge/import/${token}/execute`, method: 'POST'})
export const getImportProgress = taskId => axios({url: `knowledge/import/tasks/${taskId}`, method: 'GET'})
export const exportKnowledge = () => axios({url: 'knowledge/export', method: 'GET', responseType: 'blob', timeout: 60000})
export const exportKnowledgeDocument = id => axios({url: `knowledge/export/documents/${id}`, method: 'GET', responseType: 'blob', timeout: 60000})
