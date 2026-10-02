export function supportsPasswordCredential() {
	return window.isSecureContext
		&& 'credentials' in navigator
		&& 'PasswordCredential' in window
}

export async function saveLoginCredential(username, password) {
	if (!supportsPasswordCredential()) return false
	const id = String(username || '').trim()
	const secret = String(password || '')
	if (!id || !secret) return false
	try {
		const credential = new window.PasswordCredential({id, password: secret, name: id})
		await navigator.credentials.store(credential)
		return true
	} catch (error) {
		return false
	}
}

export async function getLoginCredential() {
	if (!supportsPasswordCredential()) return null
	try {
		return await navigator.credentials.get({password: true, mediation: 'optional'})
	} catch (error) {
		return null
	}
}
