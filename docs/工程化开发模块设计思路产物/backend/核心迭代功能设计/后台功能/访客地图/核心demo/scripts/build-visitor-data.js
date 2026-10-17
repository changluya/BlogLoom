const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const sourcePath = path.join(root, 'data.json');
const outputPath = path.join(root, 'data', 'visitor-stats.js');
const data = JSON.parse(fs.readFileSync(sourcePath, 'utf8'));

if (!data.summary || !Array.isArray(data.countries) || !Array.isArray(data.cities)) {
  throw new Error('data.json must contain summary, countries and cities');
}

const calculatedUv = data.cities.reduce((total, city) => total + Number(city.uv || 0), 0);
const calculatedPv = data.cities.reduce((total, city) => total + Number(city.pv || 0), 0);
if (calculatedPv !== data.summary.pv) {
  throw new Error(`PV summary mismatch: expected ${calculatedPv}, received ${data.summary.pv}`);
}
if (Number(data.summary.uv) > calculatedUv) {
  throw new Error(`Global UV cannot exceed the sum of city UV: ${data.summary.uv} > ${calculatedUv}`);
}

const banner = '// Generated from ../data.json. Run scripts/build-visitor-data.js after editing the JSON source.\n';
fs.writeFileSync(outputPath, `${banner}window.BLOGLOOM_VISITOR_DATA=${JSON.stringify(data)};\n`);
console.log(`Generated ${path.relative(root, outputPath)}: ${data.countries.length} countries, ${data.cities.length} cities`);
