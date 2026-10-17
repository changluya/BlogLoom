# Demo 数据说明

`adm1-mvp.js` 是已经过顶点简化和小岛过滤的海外 ADM1 浏览器离线包，来源为 [geoBoundaries gbOpen](https://www.geoboundaries.org/)。精简展示包仅保留页面运行所需的结果，不再附带约 46MB 的原始 GeoJSON。

## 访客统计模拟数据

`../data.json` 是 Demo 的唯一访客统计源数据，当前包含 19 个国家、40 个城市的静态 UV/PV。

`visitor-stats.js` 由 `data.json` 生成，只用于兼容浏览器通过 `file://` 直接打开 Demo：

```bash
node scripts/build-visitor-data.js
```

修改 `data.json` 后必须重新执行该命令。构建脚本会校验城市 PV 汇总和全球 UV 上限关系。
