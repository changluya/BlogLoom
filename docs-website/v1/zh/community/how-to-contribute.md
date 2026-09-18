---
title: "参与贡献"
description: 如何提交 Issue、Pull Request 与维护文档。
---

欢迎通过 Issue 提交问题、建议或功能需求，也欢迎通过 Pull Request 参与改进。

## 提交前建议

1. 分别验证涉及模块可以正常构建。
2. 数据库结构或初始数据发生变化时，同步维护全量 SQL 与必要的增量 SQL。
3. 不提交本地日志、上传文件、真实账号密码和访问密钥。
4. 在变更说明中写清影响模块、验证方式和兼容性注意事项。

## 构建验证

```bash
# 后端
cd blog-backend && mvn clean package

# 管理后台
cd blog-cms-ui && npm install && npm run build

# 博客前台
cd blog-view-ui && npm install && npm run build
```

## 文档贡献

- 文档位于 `docs-website/`，使用 Mintlify + Markdown。
- 修改后运行 `npm run check` 与 `npm run validate` 保证导航与页面一致。
- 保持 `en` 与 `zh` 双语平行。

## 许可

贡献代码即表示同意以项目的 MIT License 发布。

## 下一步

- [贡献者](/v1/zh/community/contributors)
- [路线图](/v1/zh/community/roadmap)
