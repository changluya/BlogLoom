---
title: "Contributing"
description: How to file issues, open pull requests and maintain docs.
---

Issues for problems, suggestions or feature requests are welcome, as are pull requests.

## Before you submit

1. Verify each affected module builds successfully.
2. When database structure or seed data changes, maintain both the full and incremental SQL scripts.
3. Do not commit local logs, uploads, real credentials or access keys.
4. Describe affected modules, verification method and compatibility notes in the change description.

## Build verification

```bash
# Backend
cd blog-backend && mvn clean package

# Admin console
cd blog-cms-ui && npm install && npm run build

# Public blog
cd blog-view-ui && npm install && npm run build
```

## Documentation

- Docs live in `docs-website/` using Mintlify + Markdown.
- After changes, run `npm run check` and `npm run validate` to keep navigation and pages in sync.
- Keep `en` and `zh` in parallel.

## License

By contributing, you agree to release your work under the project's MIT License.

## Next steps

- [Contributors](/v1/en/community/contributors)
- [Roadmap](/v1/en/community/roadmap)
