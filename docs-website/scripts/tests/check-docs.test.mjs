import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { navigationPages, checkSite } from '../check-docs.mjs';

test('navigationPages flattens strings and nested groups', () => {
  const tab = {
    tab: 'Guide',
    groups: [
      { group: 'Start', pages: ['v1/en/guide/index', 'v1/en/guide/quickstart'] },
      {
        group: 'Nested',
        pages: [{ group: 'Inner', pages: ['v1/en/guide/inner'] }],
      },
    ],
  };
  assert.deepEqual(navigationPages(tab), [
    'v1/en/guide/index',
    'v1/en/guide/quickstart',
    'v1/en/guide/inner',
  ]);
});

test('navigationPages supports a root string', () => {
  assert.deepEqual(navigationPages({ root: 'v1/en/intro', pages: ['v1/en/a'] }), [
    'v1/en/intro',
    'v1/en/a',
  ]);
});

function makeSite(files) {
  const docs = fs.mkdtempSync(path.join(os.tmpdir(), 'blogloom-docs-'));
  const config = {
    navigation: {
      languages: [
        {
          language: 'en',
          default: true,
          versions: [
            {
              version: 'v1',
              tabs: [
                {
                  tab: 'Home',
                  pages: ['v1/en/intro'],
                },
              ],
            },
          ],
        },
      ],
    },
  };
  fs.writeFileSync(path.join(docs, 'docs.json'), JSON.stringify(config));
  for (const [route, contents] of Object.entries(files)) {
    const file = path.join(docs, `${route}.md`);
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, contents);
  }
  return docs;
}

test('checkSite accepts a valid site', () => {
  const docs = makeSite({ 'v1/en/intro': '---\ntitle: Home\n---\n' });
  assert.deepEqual(checkSite(docs), []);
});

test('checkSite reports a missing navigation page', () => {
  const docs = makeSite({});
  assert.deepEqual(checkSite(docs), ['Missing navigation page: /v1/en/intro']);
});

test('checkSite reports a page missing a title', () => {
  const docs = makeSite({ 'v1/en/intro': '# No front-matter\n' });
  assert.deepEqual(checkSite(docs), ['/v1/en/intro: missing title']);
});

test('checkSite reports a page missing from navigation', () => {
  const docs = makeSite({
    'v1/en/intro': '---\ntitle: Home\n---\n',
    'v1/en/orphan': '---\ntitle: Orphan\n---\n',
  });
  assert.deepEqual(checkSite(docs), ['Page missing from navigation: /v1/en/orphan']);
});
