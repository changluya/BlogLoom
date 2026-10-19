#!/usr/bin/env node
/**
 * 生成海外 ADM1 行政区离线数据。
 *
 * 用法：
 *   node scripts/build-adm1-data.js
 *   node scripts/build-adm1-data.js --countries=NLD,ESP,ITA --output=src/components/VisitorGlobe/data/adm1-europe.js
 *   node scripts/build-adm1-data.js --countries=PRT,IRL,NOR
 *
 * 数据来源：geoBoundaries gbOpen ADM1（CC BY 4.0）。
 * 处理步骤：下载 simplified GeoJSON -> 环抽稀 -> 小岛过滤 -> 合并为浏览器离线包。
 * 生成结果始终以 Object.assign 合并到 window.BLOGLOOM_ADM1，可安全追加新批次。
 * 运行环境需要 Node 18+（使用全局 fetch）。
 */
const fs = require('fs')
const path = require('path')

const API = 'https://www.geoboundaries.org/api/current/gbOpen'
const DEFAULT_COUNTRIES = ['NLD', 'ESP', 'ITA', 'KOR', 'CHE', 'SWE', 'ARE', 'TUR', 'POL', 'CZE']
const DEFAULT_OUTPUT = path.resolve(__dirname, '../src/components/VisitorGlobe/data/adm1-extended.js')
const CACHE_DIR = path.resolve(__dirname, '../.cache/adm1')

/**
 * geoBoundaries gbOpen 的个别国家 ADM1 粒度不符合预期，这里按国家覆盖数据源。
 * ITA：gbOpen 的 ADM1 只有 5 个大区（Nord-Ovest/Nord-Est/Centro/Sud/Isole），
 *      改用 openpolis/geojson-italy 的 20 个意大利大区（CC-BY，数据源 ISTAT）。
 */
const SOURCE_OVERRIDES = {
	ITA: {
		url: 'https://raw.githubusercontent.com/openpolis/geojson-italy/master/geojson/limits_IT_regions.geojson',
		nameField: 'reg_name',
		license: 'CC-BY (ISTAT via openpolis/geojson-italy)'
	}
}

function parseArgs(argv) {
	const options = {countries: DEFAULT_COUNTRIES, output: DEFAULT_OUTPUT, maxPoints: 80, tolerance: 0.012, minArea: 0.004}
	argv.slice(2).forEach(arg => {
		const [key, value] = arg.replace(/^--/, '').split('=')
		if (key === 'countries' && value) options.countries = value.split(',').map(item => item.trim().toUpperCase()).filter(Boolean)
		else if (key === 'output' && value) options.output = path.resolve(process.cwd(), value)
		else if (key === 'merge') { /* 兼容旧参数：生成结果始终合并，不再需要该开关 */ }
		else if (key === 'max-points' && value) options.maxPoints = Number(value)
		else if (key === 'tolerance' && value) options.tolerance = Number(value)
		else if (key === 'min-area' && value) options.minArea = Number(value)
	})
	return options
}

async function fetchJson(url) {
	const response = await fetch(url, {headers: {'User-Agent': 'BlogLoom-adm1-builder'}})
	if (!response.ok) throw new Error(`HTTP ${response.status} for ${url}`)
	return response.json()
}

async function downloadGeoJson(iso3) {
	fs.mkdirSync(CACHE_DIR, {recursive: true})
	const cachePath = path.join(CACHE_DIR, `${iso3}-ADM1.geojson`)
	if (fs.existsSync(cachePath)) return JSON.parse(fs.readFileSync(cachePath, 'utf8'))
	const override = SOURCE_OVERRIDES[iso3]
	let url = override && override.url
	if (!url) {
		const meta = await fetchJson(`${API}/${iso3}/ADM1/`)
		url = meta.simplifiedGeometryGeoJSON || meta.gjDownloadURL
	}
	if (!url) throw new Error(`No download URL for ${iso3}`)
	console.log(`  downloading ${iso3}${override ? ' (override source)' : ''} ...`)
	const geoJson = await fetchJson(url)
	fs.writeFileSync(cachePath, JSON.stringify(geoJson))
	return geoJson
}

function ringArea(ring) {
	let area = 0
	for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
		area += ring[j][0] * ring[i][1] - ring[i][0] * ring[j][1]
	}
	return Math.abs(area / 2)
}

function pointSegmentDistance(point, start, end) {
	const [x, y] = point
	const [x1, y1] = start
	const [x2, y2] = end
	const dx = x2 - x1
	const dy = y2 - y1
	if (dx === 0 && dy === 0) return Math.hypot(x - x1, y - y1)
	const t = Math.max(0, Math.min(1, ((x - x1) * dx + (y - y1) * dy) / (dx * dx + dy * dy)))
	return Math.hypot(x - (x1 + t * dx), y - (y1 + t * dy))
}

function simplifyRing(ring, tolerance) {
	if (ring.length <= 4) return ring
	const first = ring[0]
	const last = ring[ring.length - 1]
	const closed = first[0] === last[0] && first[1] === last[1]
	const points = closed ? ring.slice(0, -1) : ring.slice()
	if (points.length <= 4) return ring

	const keep = new Uint8Array(points.length)
	keep[0] = 1
	keep[points.length - 1] = 1
	const stack = [[0, points.length - 1]]
	while (stack.length) {
		const [start, end] = stack.pop()
		let index = -1
		let distance = 0
		for (let cursor = start + 1; cursor < end; cursor += 1) {
			const candidate = pointSegmentDistance(points[cursor], points[start], points[end])
			if (candidate > distance) {
				distance = candidate
				index = cursor
			}
		}
		if (index > 0 && distance > tolerance) {
			keep[index] = 1
			stack.push([start, index], [index, end])
		}
	}

	const simplified = points.filter((point, index) => keep[index] === 1)
	if (closed) simplified.push(simplified[0])
	return simplified
}

function capPoints(ring, maxPoints) {
	if (ring.length <= maxPoints) return ring
	const closed = ring[0][0] === ring[ring.length - 1][0] && ring[0][1] === ring[ring.length - 1][1]
	const points = closed ? ring.slice(0, -1) : ring
	const step = Math.ceil(points.length / (closed ? maxPoints - 1 : maxPoints))
	const capped = points.filter((point, index) => index % step === 0)
	if (closed) capped.push(capped[0])
	return capped
}

function prepareRings(geometry, options) {
	if (!geometry) return []
	const polygons = geometry.type === 'Polygon' ? [geometry.coordinates] : (geometry.type === 'MultiPolygon' ? geometry.coordinates : [])
	const rings = []
	polygons.forEach(polygon => {
		const exterior = polygon[0]
		if (!exterior || ringArea(exterior) < options.minArea) return
		polygon.forEach((ring, index) => {
			if (index > 0 && ringArea(ring) < options.minArea * 0.25) return
			const simplified = capPoints(simplifyRing(ring, options.tolerance), options.maxPoints)
			if (simplified.length >= 4) rings.push(simplified)
		})
	})
	return rings
}

function prepareFeature(feature, options) {
	const rings = prepareRings(feature.geometry, options)
	if (!rings.length) return null
	let minLng = Infinity, maxLng = -Infinity, minLat = Infinity, maxLat = -Infinity
	rings.forEach(ring => ring.forEach(([lng, lat]) => {
		if (lng < minLng) minLng = lng
		if (lng > maxLng) maxLng = lng
		if (lat < minLat) minLat = lat
		if (lat > maxLat) maxLat = lat
	}))
	const properties = feature.properties || {}
	const nameField = (SOURCE_OVERRIDES[options.iso3] || {}).nameField
	return {
		name: (nameField && properties[nameField]) || properties.shapeName || properties.name || '',
		center: [Number(((minLng + maxLng) / 2).toFixed(3)), Number(((minLat + maxLat) / 2).toFixed(3))],
		rings: rings.map(ring => ring.map(([lng, lat]) => [Number(lng.toFixed(3)), Number(lat.toFixed(3))]))
	}
}

function countPoints(regions) {
	return regions.reduce((total, region) => total + region.rings.reduce((sum, ring) => sum + ring.length, 0), 0)
}

async function main() {
	const options = parseArgs(process.argv)
	const result = {}
	for (const iso3 of options.countries) {
		console.log(`[${iso3}]`)
		const geoJson = await downloadGeoJson(iso3)
		const regions = geoJson.features.map(feature => prepareFeature(feature, {...options, iso3})).filter(Boolean)
		result[iso3] = regions
		console.log(`  ${regions.length} regions, ${countPoints(regions)} points`)
	}

	const payload = `${JSON.stringify(result)}\n`
	const banner = '// Generated from geoBoundaries gbOpen ADM1 (CC BY 4.0). Run scripts/build-adm1-data.js to rebuild.\n'
	const assignment = `window.BLOGLOOM_ADM1=Object.assign(window.BLOGLOOM_ADM1||{},${payload.trim()});\n`
	fs.mkdirSync(path.dirname(options.output), {recursive: true})
	fs.writeFileSync(options.output, banner + assignment)
	const size = fs.statSync(options.output).size
	console.log(`\nGenerated ${path.relative(process.cwd(), options.output)}: ${Object.keys(result).length} countries, ${Object.values(result).reduce((sum, item) => sum + item.length, 0)} regions, ${(size / 1024).toFixed(1)} KB`)
}

main().catch(error => {
	console.error(error)
	process.exit(1)
})
