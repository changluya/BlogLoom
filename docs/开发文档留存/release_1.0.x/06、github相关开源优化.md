
# 背景

进行github仓库提交需要优化如下：

```shell
仓库体检结果，按优先级列一下：
高价值（建议尽快做）
- 没有 Release / Tag：CHANGELOG.md 和 Docker 的 ./upgrade.sh 1.0.1 都依赖版本号，但仓库零 tag、零 Release。建议打 v1.0.0 等 tag 并建 Release（升级脚本和用户下载都受益）。
- 没有 CI 构建：现有 workflow 只校验 docs-website，后端 Maven 构建、两个前端的 npm build 都没有 CI，README 也没有构建徽章。
- 没有社交预览图：分享到微信/Slack/Twitter 时是默认灰图，可上传一张 banner（可直接用 assets/blogloom-logo.png）。
中等（体验/规范）
- README 目录树过期：缺 CHANGELOG.md、docs-website/、codes/；也没链接 CHANGELOG。
- 缺社区文件：无 CONTRIBUTING.md、SECURITY.md、Issue/PR 模板（README 有「参与贡献」章节但没落到模板）。
- codes/ 目录（快捷提示词.md + 图片）看起来是内部草稿，建议移入 docs/ 或从公开仓库移除。
- Homepage 为空：docs-website/ 已有官网雏形，可部署到 GitHub Pages 并回填到仓库主页/描述链接。
- Discussions 关闭、Wiki 开启：想收 Q&A 可开 Discussions；Wiki 若不用建议关闭减少入口噪音。
低优先（清理）
- 分支较多：feat_1.0.x_1~5、main、release_1.0.x，默认分支是 master；main 已过期，建议统一或删除已合并分支。
- 仓库约 30MB，ipdb/ip2region.xdb/.db 约 19MB，截图未压缩，可考虑压缩或 Git LFS。
- README 可加徽章：CI 状态、Release、Stars。
要我先从打 Tag + Release和加 CI 构建开始吗？还是你想先处理 README / 社区文件这类轻量项？
```


# 完成进度

## 已完成（高价值项）

- **Release / Tag**：在 `95d3f9d` 打 annotated tag `v1.0.0` 并推送，创建 GitHub Release 并设为 Latest。
- **CI 构建**：新增 `.github/workflows/ci.yml`，含后端 `mvn verify`（JDK 8）与前后台两个前端 `npm ci && npm run build`；master 上三个 job 全绿。
- **修复历史失败 CI**：移除 `.gitignore` 中 `package-lock.json` 忽略规则并跟踪三个 lockfile，长期失败的 `Validate Docs` 恢复 success。
- **README 徽章**：新增 CI 状态与 Release 徽章。
- **Release Notes 入库**：新增 `docs/releases/v1.0.0.md`，与 Release 页面内容一致。
- **社交预览图**：生成 `assets/social-preview.png`（1280×640）并加入 README「产品展示」章节；待手动上传到 Settings → Social preview。
- **README 精简**：移除顶部 NBlog 提及（仅文末鸣谢保留），弱化 MySQL 缓存实现细节（去掉 `cache_entry` 描述）。
- **README 目录结构补全**：补齐 `.github/`、`CHANGELOG.md`、`docs-website/`、`docs/releases/`。
- **忽略内部草稿目录**：`.gitignore` 新增 `/codes/` 并取消跟踪 `codes/快捷提示词.md`。

## 合并记录

```text
feat_1.0.x_6  →  release_1.0.x  →  master   （均指向 9eaf568）
```

## 待办（中/低优先）

- 社区文件（CONTRIBUTING/SECURITY/Issue 模板）
- Homepage / GitHub Pages、Discussions/Wiki 开关、分支与体积清理
- CI actions 升级到 v5（消除 Node 20 deprecation 告警）
- 以上改动统一提交并同步 `release_1.0.x` / `master`

# 开发分支

blogloom：feat_1.0.x_6