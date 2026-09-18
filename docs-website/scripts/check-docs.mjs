/** Validate every published page and navigation entry in the site. */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import matter from 'gray-matter';

/** Directories that never contain published pages. */
const IGNORED_DIRS = new Set(['node_modules', '.git', 'scripts', '_build', 'dist']);

function pageFiles(directory) {
  if (!fs.existsSync(directory)) return [];
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    if (entry.isDirectory() && IGNORED_DIRS.has(entry.name)) return [];
    const file = path.join(directory, entry.name);
    return entry.isDirectory() ? pageFiles(file) : /\.mdx?$/.test(entry.name) ? [file] : [];
  });
}

/** Normalize a navigation route so it always has a leading slash. */
function normalizeRoute(route) {
  return route.startsWith('/') ? route : `/${route}`;
}

export function navigationPages(value) {
  if (Array.isArray(value)) return value.flatMap(navigationPages);
  if (!value || typeof value !== 'object') return [];
  const result = typeof value.root === 'string' ? [value.root] : [];
  for (const [key, child] of Object.entries(value)) {
    if (key === 'pages') {
      result.push(...child.flatMap((item) => typeof item === 'string' ? [item] : navigationPages(item)));
    } else if (key !== 'root') result.push(...navigationPages(child));
  }
  return result;
}

export function checkSite(docs) {
  const config = JSON.parse(fs.readFileSync(path.join(docs, 'docs.json'), 'utf8'));
  const errors = [];
  const routes = [];
  for (const lang of config.navigation.languages) {
    for (const version of lang.versions) {
      for (const tab of version.tabs) {
        for (const rawRoute of navigationPages(tab)) {
          const route = normalizeRoute(rawRoute);
          if (route.startsWith(`/${version.version}/${lang.language}/`)) {
            routes.push(route);
          } else {
            errors.push(`Route outside its language/version scope: ${route}`);
          }
        }
      }
    }
  }
  const pages = new Map();
  for (const file of pageFiles(docs)) {
    const relative = path.relative(docs, file);
    // Root-level Markdown (README, methodology docs, ...) is not a published page.
    if (!relative.includes(path.sep)) continue;
    const route = '/' + relative.replace(/\.mdx?$/, '').split(path.sep).join('/');
    try {
      const { data } = matter(fs.readFileSync(file, 'utf8'));
      if (pages.has(route)) errors.push(`Duplicate page route: ${route}`);
      pages.set(route, data);
      if (!data.title) errors.push(`${route}: missing title`);
    } catch (error) {
      errors.push(`${route}: ${error.message}`);
    }
  }
  const published = new Set(routes);
  for (const route of published) if (!pages.has(route)) errors.push(`Missing navigation page: ${route}`);
  for (const route of pages.keys()) if (!published.has(route)) errors.push(`Page missing from navigation: ${route}`);
  return errors;
}

const errors = checkSite(fileURLToPath(new URL('..', import.meta.url)));
if (errors.length) {
  console.error(errors.join('\n'));
  process.exit(1);
}
console.log('All pages, titles and navigation entries are valid.');
