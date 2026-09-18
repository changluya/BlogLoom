const DEFAULT_FAVICON = '/img/site-favicon.png'

export function setFavicon(url) {
	const href = (url || '').trim() || DEFAULT_FAVICON
	let link = document.querySelector("link[rel~='icon']")
	if (!link) {
		link = document.createElement('link')
		link.rel = 'icon'
		document.head.appendChild(link)
	}
	const separator = href.includes('?') ? '&' : '?'
	link.href = `${href}${separator}_favicon=${Date.now()}`
}
