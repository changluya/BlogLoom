export function initAdsense(config) {
	if (!config || config.enabled === false || !config.client) return
	const hasEnabledSlot = (config.slots || []).some(item => item && item.enabled !== false && item.slot)
	if (config.autoAds !== true && !hasEnabledSlot) return
	window.__adsenseClient = config.client
	if (document.querySelector('script[data-blogloom-adsense], script[src*="pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"]')) return
	const script = document.createElement('script')
	script.async = true
	script.crossOrigin = 'anonymous'
	script.dataset.blogloomAdsense = 'true'
	script.src = `https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${encodeURIComponent(config.client)}`
	document.head.appendChild(script)
}

export function pushAdsense(element) {
	if (!window.__adsenseClient || !element || element.dataset.pushed) return
	element.dataset.pushed = 'true'
	try {
		(window.adsbygoogle = window.adsbygoogle || []).push({})
	} catch (e) {
		// 广告被拦截或 SDK 未就绪时不影响正文阅读
	}
}
