import echarts from 'echarts'
import 'echarts/map/js/china'
import 'echarts/map/js/world'
import './data/adm1-mvp'
import chinaCityCoordinates from '@/util/city2coord.json'

const COUNTRY_LABELS = [
	['中国', 'China', 36, 103, 14], ['俄罗斯', 'Russia', 61, 90, 13],
	['美国', 'United States', 39, -99, 13], ['加拿大', 'Canada', 57, -106, 12],
	['巴西', 'Brazil', -11, -53, 13], ['澳大利亚', 'Australia', -25, 134, 12],
	['印度', 'India', 22, 79, 13], ['阿根廷', 'Argentina', -36, -64, 11],
	['哈萨克斯坦', 'Kazakhstan', 48, 67, 11], ['蒙古国', 'Mongolia', 46, 103, 11],
	['沙特阿拉伯', 'Saudi Arabia', 24, 45, 11], ['墨西哥', 'Mexico', 23, -102, 11],
	['印度尼西亚', 'Indonesia', -3, 118, 10], ['南非', 'South Africa', -30, 24, 11],
	['阿尔及利亚', 'Algeria', 28, 2, 10], ['刚果（金）', 'DR Congo', -3, 23, 10]
].map(([nameZh, nameEn, lat, lng, size]) => ({nameZh, nameEn, lat, lng, size}))

const COUNTRY_CODE_BY_MAP_NAME = {
	Australia: 'AU', Brazil: 'BR', Canada: 'CA', China: 'CN', France: 'FR',
	Germany: 'DE', Egypt: 'EG', India: 'IN', Indonesia: 'ID', Japan: 'JP',
	Korea: 'KR', Mexico: 'MX', Russia: 'RU', Singapore: 'SG', 'South Africa': 'ZA',
	'South Korea': 'KR', Thailand: 'TH', 'United Arab Emirates': 'AE',
	'United Kingdom': 'GB', 'United States': 'US'
}

const ADM1_COLORS = {
	USA: '#53c7ff', CAN: '#65d8c7', AUS: '#ffba72', BRA: '#72df91',
	FRA: '#9caeff', DEU: '#79c8ff', IND: '#ff9d72', JPN: '#e593ff',
	GBR: '#a6b9ff', SGP: '#72f0de'
}

const CITY_RADIUS_MIN = 2.6
const CITY_RADIUS_MAX = 9
const CITY_RIPPLE_MAX = 22

const CITY_NAME_EN = {
	'北京市': 'Beijing', '上海市': 'Shanghai', '天津市': 'Tianjin', '重庆市': 'Chongqing',
	'石家庄市': 'Shijiazhuang', '太原市': 'Taiyuan', '呼和浩特市': 'Hohhot',
	'沈阳市': 'Shenyang', '大连市': 'Dalian', '长春市': 'Changchun',
	'哈尔滨市': 'Harbin', '南京市': 'Nanjing', '苏州市': 'Suzhou',
	'杭州市': 'Hangzhou', '宁波市': 'Ningbo', '合肥市': 'Hefei',
	'福州市': 'Fuzhou', '厦门市': 'Xiamen', '南昌市': 'Nanchang',
	'济南市': 'Jinan', '青岛市': 'Qingdao', '郑州市': 'Zhengzhou',
	'武汉市': 'Wuhan', '长沙市': 'Changsha', '广州市': 'Guangzhou',
	'深圳市': 'Shenzhen', '南宁市': 'Nanning', '海口市': 'Haikou',
	'成都市': 'Chengdu', '贵阳市': 'Guiyang', '昆明市': 'Kunming',
	'拉萨市': 'Lhasa', '西安市': "Xi'an", '兰州市': 'Lanzhou',
	'西宁市': 'Xining', '银川市': 'Yinchuan', '乌鲁木齐市': 'Urumqi',
	'香港': 'Hong Kong', '澳门': 'Macao', '台北市': 'Taipei',
	'新加坡': 'Singapore', '东京': 'Tokyo', '首尔': 'Seoul', '曼谷': 'Bangkok',
	'雅加达': 'Jakarta', '德里': 'Delhi', '孟买': 'Mumbai', '迪拜': 'Dubai',
	'莫斯科': 'Moscow', '伦敦': 'London', '巴黎': 'Paris', '柏林': 'Berlin',
	'开罗': 'Cairo', '内罗毕': 'Nairobi', '约翰内斯堡': 'Johannesburg',
	'纽约': 'New York', '旧金山': 'San Francisco', '多伦多': 'Toronto',
	'墨西哥城': 'Mexico City', '圣保罗': 'Sao Paulo',
	'布宜诺斯艾利斯': 'Buenos Aires', '悉尼': 'Sydney', '墨尔本': 'Melbourne'
}

function decodeEchartsGeoJson(geoJson) {
	if (!geoJson || !geoJson.UTF8Encoding) return geoJson
	const scale = geoJson.UTF8Scale || 1024
	geoJson.features.forEach(feature => {
		const geometry = feature.geometry
		if (!geometry) return
		const decodeRing = (encoded, offset) => {
			const decoded = []
			let previousX = offset[0]
			let previousY = offset[1]
			for (let index = 0; index < encoded.length; index += 2) {
				let x = encoded.charCodeAt(index) - 64
				let y = encoded.charCodeAt(index + 1) - 64
				x = (x >> 1) ^ (-(x & 1))
				y = (y >> 1) ^ (-(y & 1))
				x += previousX
				y += previousY
				previousX = x
				previousY = y
				decoded.push([x / scale, y / scale])
			}
			return decoded
		}
		if (geometry.type === 'Polygon') {
			geometry.coordinates = geometry.coordinates.map((ring, index) =>
				decodeRing(ring, geometry.encodeOffsets[index]))
		} else if (geometry.type === 'MultiPolygon') {
			geometry.coordinates = geometry.coordinates.map((polygon, polygonIndex) =>
				polygon.map((ring, ringIndex) =>
					decodeRing(ring, geometry.encodeOffsets[polygonIndex][ringIndex])))
		}
	})
	geoJson.UTF8Encoding = false
	return geoJson
}

function prepareFeatures(geoJson, maxPoints) {
	if (!geoJson || !Array.isArray(geoJson.features)) return []
	return geoJson.features.map(feature => {
		if (!feature.geometry) return null
		const polygons = feature.geometry.type === 'Polygon'
			? [feature.geometry.coordinates]
			: feature.geometry.coordinates
		const rings = []
		polygons.forEach(polygon => polygon.forEach(ring => {
			if (ring.length <= maxPoints) {
				rings.push(ring)
				return
			}
			const step = Math.ceil(ring.length / maxPoints)
			rings.push(ring.filter((point, index) => index % step === 0))
		}))
		return {name: feature.properties && feature.properties.name, rings}
	}).filter(Boolean)
}

export function createVisitorGlobe(container, dataset, options = {}) {
	const canvas = document.createElement('canvas')
	canvas.className = 'visitor-globe-canvas'
	canvas.setAttribute('aria-label', '可旋转和缩放的全球访客地球')
	container.appendChild(canvas)
	const context = canvas.getContext('2d')
	const state = {
		yaw: -1.83, pitch: 0.25, zoom: 1, autoRotate: true,
		dragging: false, moved: false, pointerX: 0, pointerY: 0,
		language: 'zh', visible: true
	}
	const frame = {centerX: 0, centerY: 0, radius: 1, yawCos: 1, yawSin: 0, pitchCos: 1, pitchSin: 0}
	const worldMap = echarts.getMap('world')
	const chinaMap = echarts.getMap('china')
	const worldFeatures = prepareFeatures(decodeEchartsGeoJson(worldMap && (worldMap.geoJson || worldMap.geoJSON)), 120)
		.filter(feature => feature.name !== 'China')
	const chinaFeatures = prepareFeatures(decodeEchartsGeoJson(chinaMap && (chinaMap.geoJson || chinaMap.geoJSON)), 220)
		.map(feature => {
			let minLng = Infinity, maxLng = -Infinity, minLat = Infinity, maxLat = -Infinity
			feature.rings.forEach(ring => ring.forEach(coordinate => {
				const lng = Number(coordinate[0]), lat = Number(coordinate[1])
				if (lng < minLng) minLng = lng
				if (lng > maxLng) maxLng = lng
				if (lat < minLat) minLat = lat
				if (lat > maxLat) maxLat = lat
			}))
			return {...feature, bbox: [minLng, minLat, maxLng, maxLat]}
		})
	const adm1ByCountry = window.BLOGLOOM_ADM1 || {}
	let cities = []
	let countriesByCode = {}
	let cityStats = new Map()
	let maxUv = 1
	let referenceCities = []
	let animationFrame = 0
	let revealedCountries = 0
	let revealedCities = 0
	let lastFrameTime = 0
	let destroyed = false
	let provinceStats = new Map()
	let hoveredProvince = null
	const timers = []

	function normalizeProvinceName(name) {
		if (!name) return ''
		return String(name)
			.replace(/特别行政区$/, '')
			.replace(/(维吾尔|壮族|回族)?自治区$/, '')
			.replace(/[省市]$/, '')
			.trim()
	}

	function pointInRing(lng, lat, ring) {
		let inside = false
		for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
			const xi = ring[i][0], yi = ring[i][1]
			const xj = ring[j][0], yj = ring[j][1]
			if (((yi > lat) !== (yj > lat)) && (lng < (xj - xi) * (lat - yi) / (yj - yi) + xi)) inside = !inside
		}
		return inside
	}

	function provinceByPoint(lat, lng) {
		const latitude = Number(lat), longitude = Number(lng)
		for (let index = 0; index < chinaFeatures.length; index += 1) {
			const province = chinaFeatures[index]
			const bbox = province.bbox
			if (longitude < bbox[0] || longitude > bbox[2] || latitude < bbox[1] || latitude > bbox[3]) continue
			for (let ringIndex = 0; ringIndex < province.rings.length; ringIndex += 1) {
				if (pointInRing(longitude, latitude, province.rings[ringIndex])) return province.name
			}
		}
		return ''
	}

	function buildProvinceStats(list) {
		const stats = new Map()
		list.forEach(city => {
			if (city.countryCode !== 'CN') return
			const province = normalizeProvinceName(city.regionName) || provinceByPoint(city.lat, city.lng)
			if (!province) return
			const item = stats.get(province) || {cityCount: 0, maxUv: 0, pv: 0}
			item.cityCount += 1
			item.maxUv = Math.max(item.maxUv, Number(city.uv) || 0)
			item.pv += Number(city.pv) || 0
			stats.set(province, item)
		})
		return stats
	}

	function applyDataset(source, revealAll = true) {
		const nextCities = (source && source.cities) || []
		cities = nextCities
		countriesByCode = Object.fromEntries(((source && source.countries) || []).map(item => [item.countryCode, item]))
		cityStats = new Map(nextCities.map(city => [city.city, city]))
		maxUv = Math.max(1, ...nextCities.map(cityVolume))
		provinceStats = buildProvinceStats(nextCities)
		const knownNames = new Set(Object.keys(chinaCityCoordinates))
		referenceCities = Object.entries(chinaCityCoordinates).map(([city, coordinate]) => ({
			city, country: '中国', countryCode: 'CN', lng: Number(coordinate[0]), lat: Number(coordinate[1])
		}))
		nextCities.forEach(city => { if (!knownNames.has(city.city)) referenceCities.push(city) })
		if (revealAll) {
			revealedCountries = Math.max(revealedCountries, Object.keys(countriesByCode).length)
			revealedCities = Math.max(revealedCities, nextCities.length)
		}
	}

	applyDataset(dataset, false)

	const cityLabel = city => state.language === 'en'
		? (city.cityNameEn || city.cityEn || CITY_NAME_EN[city.city] || city.city)
		: (city.cityNameZh || city.city)
	const notifyStatus = (tone, text) => options.onStatus && options.onStatus({tone, text})
	const notifyHover = payload => options.onHover && options.onHover(payload)

	function resize() {
		const ratio = Math.min(window.devicePixelRatio || 1, 1.35)
		const width = Math.max(1, container.clientWidth)
		const height = Math.max(1, container.clientHeight)
		canvas.width = Math.floor(width * ratio)
		canvas.height = Math.floor(height * ratio)
		canvas.style.width = width + 'px'
		canvas.style.height = height + 'px'
		context.setTransform(ratio, 0, 0, ratio, 0, 0)
	}

	function updateFrame(width, height) {
		frame.centerX = width * (width > 980 ? 0.48 : 0.5)
		frame.centerY = height * 0.53
		frame.radius = Math.min(width, height) * 0.35 * state.zoom
		frame.yawCos = Math.cos(state.yaw)
		frame.yawSin = Math.sin(state.yaw)
		frame.pitchCos = Math.cos(state.pitch)
		frame.pitchSin = Math.sin(state.pitch)
	}

	function projectUnit(unit) {
		const x = unit[0] * frame.yawCos + unit[2] * frame.yawSin
		const z = unit[2] * frame.yawCos - unit[0] * frame.yawSin
		const y = unit[1] * frame.pitchCos - z * frame.pitchSin
		const rotatedZ = unit[1] * frame.pitchSin + z * frame.pitchCos
		return {x: frame.centerX + x * frame.radius, y: frame.centerY - y * frame.radius, z: rotatedZ}
	}

	function project(lat, lng) {
		const latitude = Number(lat) * Math.PI / 180
		const longitude = Number(lng) * Math.PI / 180
		const cosLat = Math.cos(latitude)
		return projectUnit([cosLat * Math.sin(longitude), Math.sin(latitude), cosLat * Math.cos(longitude)])
	}

	function projectCoordinate(coordinate) {
		const latitude = Number(coordinate[1]) * Math.PI / 180
		const longitude = Number(coordinate[0]) * Math.PI / 180
		const cosLat = Math.cos(latitude)
		return projectUnit([cosLat * Math.sin(longitude), Math.sin(latitude), cosLat * Math.cos(longitude)])
	}

	function traceFeatures(features) {
		features.forEach(feature => feature.rings.forEach(ring => {
			let drawing = false
			ring.forEach(coordinate => {
				const point = projectCoordinate(coordinate)
				if (point.z <= 0) {
					drawing = false
					return
				}
				if (!drawing) context.moveTo(point.x, point.y)
				else context.lineTo(point.x, point.y)
				drawing = true
			})
			if (drawing) context.closePath()
		}))
	}

	function heatColor(uv, alpha) {
		const ratio = Math.min((uv || 0) / maxUv, 1)
		return `rgba(${Math.round(18 + ratio * 62)},${Math.round(81 + ratio * 155)},${Math.round(114 + ratio * 125)},${alpha})`
	}

	function drawGrid() {
		context.strokeStyle = 'rgba(102, 193, 220, .12)'
		context.lineWidth = 0.65
		for (let lat = -60; lat <= 60; lat += 30) {
			context.beginPath()
			let drawing = false
			for (let lng = -180; lng <= 180; lng += 7) {
				const point = project(lat, lng)
				if (point.z <= 0) { drawing = false; continue }
				if (!drawing) context.moveTo(point.x, point.y)
				else context.lineTo(point.x, point.y)
				drawing = true
			}
			context.stroke()
		}
		for (let lng = -150; lng <= 180; lng += 30) {
			context.beginPath()
			let drawing = false
			for (let lat = -90; lat <= 90; lat += 7) {
				const point = project(lat, lng)
				if (point.z <= 0) { drawing = false; continue }
				if (!drawing) context.moveTo(point.x, point.y)
				else context.lineTo(point.x, point.y)
				drawing = true
			}
			context.stroke()
		}
	}

	function drawWorld() {
		context.beginPath()
		traceFeatures(worldFeatures)
		context.fillStyle = 'rgba(12, 42, 62, .34)'
		context.fill('evenodd')
		context.strokeStyle = 'rgba(66, 138, 166, .46)'
		context.lineWidth = 0.65
		context.stroke()
		const visibleCodes = new Set(Object.keys(countriesByCode).slice(0, revealedCountries))
		worldFeatures.forEach(feature => {
			const code = COUNTRY_CODE_BY_MAP_NAME[feature.name]
			if (!code || !visibleCodes.has(code)) return
			context.beginPath()
			traceFeatures([feature])
			context.fillStyle = heatColor(countriesByCode[code].uv, 0.52)
			context.fill('evenodd')
			context.strokeStyle = 'rgba(87, 206, 228, .72)'
			context.lineWidth = 0.9
			context.stroke()
		})
	}

	function drawChina() {
		const revealed = revealedCountries > 2
		chinaFeatures.forEach(province => {
			const key = normalizeProvinceName(province.name)
			const stats = provinceStats.get(key)
			const hovered = hoveredProvince === province.name
			context.beginPath()
			traceFeatures([province])
			if (stats) {
				context.fillStyle = heatColor(stats.maxUv, hovered ? 0.82 : 0.6)
			} else {
				context.fillStyle = hovered
					? 'rgba(44, 68, 170, .88)'
					: (revealed ? 'rgba(23, 30, 141, .78)' : 'rgba(13, 20, 89, .68)')
			}
			context.fill('evenodd')
			context.strokeStyle = hovered ? '#8df2ff' : 'rgba(56, 157, 255, .72)'
			context.lineWidth = hovered ? 1.5 : 0.8
			context.stroke()
		})
	}

	function drawAdm1() {
		if (state.zoom < 0.82 || state.dragging) return
		Object.entries(adm1ByCountry).forEach(([countryCode, regions]) => {
			context.beginPath()
			regions.forEach(region => region.rings.forEach(ring => {
				let drawing = false
				ring.forEach(coordinate => {
					const point = projectCoordinate(coordinate)
					if (point.z <= 0) { drawing = false; return }
					if (!drawing) context.moveTo(point.x, point.y)
					else context.lineTo(point.x, point.y)
					drawing = true
				})
			}))
			context.strokeStyle = ADM1_COLORS[countryCode] || '#4daecd'
			context.globalAlpha = Math.min(0.78, 0.32 + Math.max(0, state.zoom - 1) * 0.4)
			context.lineWidth = state.zoom > 1.4 ? 0.9 : 0.55
			context.stroke()
			context.globalAlpha = 1
		})
	}

	function drawCountryLabels() {
		const occupied = []
		COUNTRY_LABELS.forEach(country => {
			const point = project(country.lat, country.lng)
			if (point.z < 0.18) return
			const label = state.language === 'en' ? country.nameEn : country.nameZh
			context.font = `600 ${country.size}px Inter, "PingFang SC", sans-serif`
			const width = context.measureText(label).width
			const box = {left: point.x - width / 2 - 3, right: point.x + width / 2 + 3, top: point.y - country.size, bottom: point.y + 4}
			if (occupied.some(item => box.left < item.right && box.right > item.left && box.top < item.bottom && box.bottom > item.top)) return
			occupied.push(box)
			context.textAlign = 'center'
			context.lineWidth = 3
			context.strokeStyle = 'rgba(1, 7, 13, .9)'
			context.strokeText(label, point.x, point.y)
			context.fillStyle = country.nameZh === '中国' ? '#f2fbff' : 'rgba(190, 224, 234, .9)'
			context.fillText(label, point.x, point.y)
		})
		context.textAlign = 'start'
	}

	function cityVolume(city) {
		const uv = Math.max(0, Number(city.uv) || 0)
		return uv > 0 ? uv : Math.max(0, Number(city.pv) || 0)
	}

	function cityIntensity(city) {
		const value = cityVolume(city)
		if (maxUv <= 1) return value > 0 ? 0.35 : 0
		return Math.min(Math.log1p(value) / Math.log1p(maxUv), 1)
	}

	function cityRadius(city, pulse) {
		const base = CITY_RADIUS_MIN + cityIntensity(city) * (CITY_RADIUS_MAX - CITY_RADIUS_MIN)
		return Math.min(Math.max(base * pulse, CITY_RADIUS_MIN), CITY_RADIUS_MAX)
	}

	function drawCities(timestamp) {
		cities.slice(0, revealedCities).forEach(city => {
			const point = project(city.lat, city.lng)
			if (point.z <= 0) return
			const intensity = cityIntensity(city)
			const pulse = 1 + Math.sin(timestamp / 500 + city.rank) * 0.12
			const radius = cityRadius(city, pulse)
			context.fillStyle = city.rank <= 3 ? '#c5fbff' : '#54d9e8'
			context.shadowBlur = Math.min(10 + intensity * 20, 24)
			context.shadowColor = '#55ddec'
			context.beginPath()
			context.arc(point.x, point.y, radius, 0, Math.PI * 2)
			context.fill()
			context.shadowBlur = 0
			if (city.rank <= 5) {
				context.strokeStyle = 'rgba(105, 236, 245, .4)'
				context.beginPath()
				context.arc(point.x, point.y, Math.min(radius * 2.2 + (timestamp / 100 % 16), CITY_RIPPLE_MAX), 0, Math.PI * 2)
				context.stroke()
			}
			if (city.rank <= 5 && point.z > 0.25) {
				context.fillStyle = 'rgba(218, 250, 255, .92)'
				context.font = '11px Inter, "PingFang SC", sans-serif'
				context.fillText(cityLabel(city), point.x + radius + 6, point.y - radius - 4)
			}
		})
	}

	function draw(timestamp = 0) {
		if (destroyed) return
		if (!state.visible) {
			animationFrame = window.requestAnimationFrame(draw)
			return
		}
		const width = container.clientWidth
		const height = container.clientHeight
		updateFrame(width, height)
		context.clearRect(0, 0, width, height)
		const glow = context.createRadialGradient(frame.centerX - frame.radius * 0.3, frame.centerY - frame.radius * 0.35, frame.radius * 0.05, frame.centerX, frame.centerY, frame.radius * 1.2)
		glow.addColorStop(0, 'rgba(38, 112, 153, .28)')
		glow.addColorStop(0.58, 'rgba(12, 48, 76, .18)')
		glow.addColorStop(1, 'rgba(2, 8, 16, 0)')
		context.fillStyle = glow
		context.beginPath()
		context.arc(frame.centerX, frame.centerY, frame.radius * 1.28, 0, Math.PI * 2)
		context.fill()
		const globe = context.createRadialGradient(frame.centerX - frame.radius * 0.35, frame.centerY - frame.radius * 0.38, frame.radius * 0.08, frame.centerX, frame.centerY, frame.radius)
		globe.addColorStop(0, '#183d57')
		globe.addColorStop(0.58, '#0a1a2a')
		globe.addColorStop(1, '#02070d')
		context.fillStyle = globe
		context.beginPath()
		context.arc(frame.centerX, frame.centerY, frame.radius, 0, Math.PI * 2)
		context.fill()
		context.save()
		context.beginPath()
		context.arc(frame.centerX, frame.centerY, frame.radius, 0, Math.PI * 2)
		context.clip()
		drawGrid()
		drawWorld()
		drawAdm1()
		drawChina()
		drawCountryLabels()
		drawCities(timestamp)
		context.restore()
		context.strokeStyle = 'rgba(103, 213, 235, .34)'
		context.lineWidth = 1
		context.beginPath()
		context.arc(frame.centerX, frame.centerY, frame.radius, 0, Math.PI * 2)
		context.stroke()
		if (state.autoRotate && !state.dragging && timestamp - lastFrameTime > 15) {
			state.yaw += 0.0024
			lastFrameTime = timestamp
		}
		animationFrame = window.requestAnimationFrame(draw)
	}

	function nearestCity(clientX, clientY, threshold = 28) {
		const bounds = canvas.getBoundingClientRect()
		const x = clientX - bounds.left
		const y = clientY - bounds.top
		let nearest = null
		let distance = threshold
		referenceCities.forEach(city => {
			const point = project(city.lat, city.lng)
			const candidate = Math.hypot(point.x - x, point.y - y)
			if (point.z > 0 && candidate < distance) {
				nearest = city
				distance = candidate
			}
		})
		return nearest
	}

	function unprojectPoint(x, y) {
		const dx = (x - frame.centerX) / frame.radius
		const dy = (frame.centerY - y) / frame.radius
		const distance = dx * dx + dy * dy
		if (distance > 1) return null
		const z = Math.sqrt(1 - distance)
		const unitY = dy * frame.pitchCos + z * frame.pitchSin
		const rotatedZ = z * frame.pitchCos - dy * frame.pitchSin
		const unitX = dx * frame.yawCos - rotatedZ * frame.yawSin
		const unitZ = dx * frame.yawSin + rotatedZ * frame.yawCos
		const lat = Math.asin(Math.max(-1, Math.min(1, unitY))) * 180 / Math.PI
		const lng = Math.atan2(unitX, unitZ) * 180 / Math.PI
		return {lat, lng}
	}

	function provinceAt(clientX, clientY) {
		const bounds = canvas.getBoundingClientRect()
		const point = unprojectPoint(clientX - bounds.left, clientY - bounds.top)
		if (!point) return ''
		for (let index = 0; index < chinaFeatures.length; index += 1) {
			const province = chinaFeatures[index]
			const bbox = province.bbox
			if (point.lng < bbox[0] || point.lng > bbox[2] || point.lat < bbox[1] || point.lat > bbox[3]) continue
			for (let ringIndex = 0; ringIndex < province.rings.length; ringIndex += 1) {
				if (pointInRing(point.lng, point.lat, province.rings[ringIndex])) return province.name
			}
		}
		return ''
	}

	function pointerDown(event) {
		state.dragging = true
		state.moved = false
		state.pointerX = event.clientX
		state.pointerY = event.clientY
		canvas.setPointerCapture(event.pointerId)
	}

	function pointerMove(event) {
		if (!state.dragging) {
			const city = nearestCity(event.clientX, event.clientY)
			const cityStat = city ? cityStats.get(city.city) : null
			if (city && cityStat) {
				hoveredProvince = city.countryCode === 'CN'
					? (normalizeProvinceName(city.regionName) || provinceByPoint(city.lat, city.lng))
					: null
				canvas.style.cursor = 'pointer'
				notifyHover({city, stats: cityStat, x: event.clientX, y: event.clientY})
				return
			}
			const province = provinceAt(event.clientX, event.clientY)
			hoveredProvince = province || null
			const stat = province ? provinceStats.get(normalizeProvinceName(province)) : null
			canvas.style.cursor = stat ? 'pointer' : 'grab'
			notifyHover(stat
				? {province, provinceStats: stat, x: event.clientX, y: event.clientY}
				: null)
			return
		}
		notifyHover(null)
		const dx = event.clientX - state.pointerX
		const dy = event.clientY - state.pointerY
		if (Math.abs(dx) + Math.abs(dy) > 3) state.moved = true
		state.yaw += dx * 0.008
		state.pitch = Math.max(-1.1, Math.min(1.1, state.pitch + dy * 0.006))
		state.pointerX = event.clientX
		state.pointerY = event.clientY
	}

	function pointerUp(event) {
		state.dragging = false
		if (state.moved) return
		const city = nearestCity(event.clientX, event.clientY, 32)
		if (!city || !cityStats.get(city.city)) return
		state.yaw = city.lng * Math.PI / 180 * -1 + 0.18
		state.pitch = city.lat * Math.PI / 180 * 0.25
		state.zoom = 1.55
		notifyStatus('FOCUS', cityLabel(city))
	}

	function wheel(event) {
		event.preventDefault()
		state.zoom = Math.max(0.72, Math.min(2.25, state.zoom - event.deltaY * 0.001))
	}

	function visibilityChange() {
		state.visible = !document.hidden
	}

	canvas.addEventListener('pointerdown', pointerDown)
	canvas.addEventListener('pointermove', pointerMove)
	canvas.addEventListener('pointerup', pointerUp)
	canvas.addEventListener('pointercancel', pointerUp)
	canvas.addEventListener('pointerleave', () => {
		hoveredProvince = null
		notifyHover(null)
	})
	canvas.addEventListener('wheel', wheel, {passive: false})
	window.addEventListener('resize', resize)
	document.addEventListener('visibilitychange', visibilityChange)
	resize()
	notifyStatus('LIGHTING', 'loading visitor regions')
	timers.push(window.setInterval(() => {
		revealedCountries += 1
		if (revealedCountries >= Object.keys(countriesByCode).length) window.clearInterval(timers[0])
	}, 140))
	timers.push(window.setTimeout(() => {
		const cityTimer = window.setInterval(() => {
			revealedCities += 1
			if (revealedCities >= cities.length) {
				window.clearInterval(cityTimer)
				notifyStatus('READY', 'drag · zoom · hover')
			}
		}, 90)
		timers.push(cityTimer)
	}, 500))
	animationFrame = window.requestAnimationFrame(draw)

	return {
		setDataset(source) { applyDataset(source) },
		setLanguage(language) { state.language = language },
		setAutoRotate(enabled) { state.autoRotate = enabled },
		getState() { return {yaw: state.yaw, pitch: state.pitch, zoom: state.zoom} },
		reset() {
			state.yaw = -1.83
			state.pitch = 0.25
			state.zoom = 1
			notifyStatus('READY', 'global view restored')
		},
		destroy() {
			destroyed = true
			window.cancelAnimationFrame(animationFrame)
			timers.forEach(timer => {
				window.clearTimeout(timer)
				window.clearInterval(timer)
			})
			window.removeEventListener('resize', resize)
			document.removeEventListener('visibilitychange', visibilityChange)
			canvas.removeEventListener('pointerdown', pointerDown)
			canvas.removeEventListener('pointermove', pointerMove)
			canvas.removeEventListener('pointerup', pointerUp)
			canvas.removeEventListener('pointercancel', pointerUp)
			canvas.removeEventListener('wheel', wheel)
			if (canvas.parentNode === container) container.removeChild(canvas)
		}
	}
}

export function getCityDisplayName(city, language) {
	return language === 'en'
		? (city.cityNameEn || city.cityEn || CITY_NAME_EN[city.city] || city.city)
		: (city.cityNameZh || city.city)
}
