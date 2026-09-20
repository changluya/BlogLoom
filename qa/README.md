# BlogLoom 压测与资源占用（qa）

对 **Docker 一键启动**的 BlogLoom（`blogloom-app` + `blogloom-mysql`）做压测，包含两个脚本：

| 脚本 | 用途 | 产出 |
| --- | --- | --- |
| `run-stress-test.sh` | 服务启动耗时、空闲资源、各接口 QPS/延迟、并发梯度、峰值资源 | `qa/results/<时间戳>/report.md` |
| `run-capacity-test.sh` | **给定资源配置下的最大 QPS**（CPU/内存配额 → 最大 QPS） | `qa/results/capacity-<时间戳>/report.md` |

---

## 快速使用

```bash
# 1. 启动服务（若尚未启动）
cd docker && ./scripts/deploy.sh && cd ..

# 2. 常规压测与资源占用报告
qa/run-stress-test.sh
cat qa/results/<时间戳>/report.md

# 3. 容量测试：资源配置 -> 最大 QPS
qa/run-capacity-test.sh
cat qa/results/capacity-<时间戳>/report.md
```

运行过程中终端会打印进度与报告摘要，结束后给出报告路径。

> 依赖：`ab`（ApacheBench）、`docker`、`curl`、`python3`。

---

## 容量测试（资源配置 -> 最大 QPS）

`qa/run-capacity-test.sh` 会为 `blogloom-app` 依次设置不同的 CPU/内存配额并重建容器，
在每个配额下逐级提高并发压测，并在峰值并发下采样实际资源占用。

**最大 QPS 判定标准**：

| 指标 | 定义 |
| --- | --- |
| 峰值 QPS | 无失败请求（socket 错误=0、非2xx=0）下观测到的最高吞吐 |
| 可用 QPS | 峰值基础上，P90 延迟 ≤ 200ms 的最高吞吐 |
| CPU 饱和 | 应用 CPU 占用 ≥ CPU 配额 × 90% |

> 若 CPU 未饱和，说明瓶颈在压测客户端 / MySQL，而非应用 CPU，报告会明确标注。
> 压测工具优先使用 `wrk`（多线程、低开销，避免客户端先于服务端成为瓶颈），未安装时回退 `ab`。

默认场景：

```text
1c_1g  → 1 核 / 1G
2c_1g  → 2 核 / 1G
4c_1g  → 4 核 / 1G
8c_1g  → 8 核 / 1G
```

自定义场景（格式 `名称|CPU核数|内存上限`）与参数：

```bash
SCENARIOS="2c_512m|2|512m 2c_1g|2|1g 4c_2g|4|2g" qa/run-capacity-test.sh
RAMP_CONCURRENCY="50 100 200 400" DURATION=10 qa/run-capacity-test.sh
```

资源限制通过 `qa/docker-compose.limits.yml` 以 compose 覆盖方式注入，**不影响正常部署**；
测试结束后脚本会自动恢复为无限制配置。

---

## 它会测什么

| 阶段 | 内容 |
| --- | --- |
| 1. 服务启动 | 重启应用容器，测量到接口可用耗时（含等待 MySQL、执行增量 SQL、Spring 启动） |
| 2. 空闲基线 | 启动后无流量时，两容器 CPU / 内存占用 |
| 3. 单接口对比 | `/`、`/site`、`/blogs`、`/about`、`/archives`、`/moments`、`/blog`、`/comments` 的 QPS 与延迟 |
| 4. 并发梯度 | 默认并发 `1 10 25 50 100 200`，找 QPS 上限与延迟变化 |
| 5. 资源采样 | 压测全程每秒采集两容器 CPU / 内存，统计均值与峰值 |

---

## 常用命令

```bash
# 默认跑一遍
qa/run-stress-test.sh

# 加大压力、指定并发梯度
DURATION=10 RAMP_CONCURRENCY="10 50 100 200 400" qa/run-stress-test.sh

# 换目标接口做并发梯度（默认博客列表）
RAMP_TARGET_NAME=site RAMP_TARGET_PATH=/site qa/run-stress-test.sh

# 指定被测地址
WEB_HOST=127.0.0.1 WEB_PORT=18080 qa/run-stress-test.sh
```

---

## 可调参数（环境变量）

| 变量 | 默认 | 说明 |
| --- | --- | --- |
| `WEB_HOST` / `WEB_PORT` | `127.0.0.1` / 取自 `docker/.env`（否则 `18080`） | 被测服务地址 |
| `DURATION` | `8` | 并发梯度每档压测时长（秒） |
| `ENDPOINT_CONCURRENCY` | `20` | 单接口对比并发 |
| `RAMP_CONCURRENCY` | `1 10 25 50 100 200` | 并发梯度 |
| `RAMP_TARGET_NAME` | `blogs` | 并发梯度目标接口名 |
| `RAMP_TARGET_PATH` | `/blogs?pageNum=1&pageSize=10` | 并发梯度目标路径 |
| `RAMP_TARGET_ACCEPT` | `application/json` | 目标接口 Accept 头 |
| `APP_CONTAINER` / `DB_CONTAINER` | `blogloom-app` / `blogloom-mysql` | 容器名 |

---

## 产物说明

```text
qa/results/<时间戳>/
├── meta.env                  环境与资源配置（镜像、JAVA_OPTS、容器限制、启动耗时）
├── baseline.csv              启动后空闲资源采样
├── samples.csv               压测期间资源采样
├── raw/
│   ├── ab_endpoint_*.txt     单接口 ab 原始输出
│   └── ab_ramp_c*.txt        并发梯度 ab 原始输出
└── report.md                 最终报告
```

报告章节：测试环境 / 被测服务与资源配置 / 服务启动 / 空闲资源占用 / 单接口对比 /
并发梯度与 QPS 上限 / 峰值负载资源占用 / 结论 / 说明与局限。

容量测试产物：

```text
qa/results/capacity-<时间戳>/
├── meta.env              环境与压测参数
├── capacity.tsv          各场景最大 QPS 与资源配置
├── samples.csv           峰值负载资源采样
├── raw/
│   ├── ramp_<场景>.tsv   各场景并发梯度明细
│   └── ab_*.txt          ab 原始输出
└── report.md             容量报告（核心：资源配置 -> 最大 QPS）
```

---

## 说明与局限

- 压测客户端与服务端同机运行，会竞争 CPU，QPS 结果为**保守下限**；
- 首页 `/` 为静态 HTML（需 `Accept: text/html`），接口为 JSON（需 `Accept: application/json`）；
- `docker stats` 的 CPU% 为相对宿主机总核数的占比，可能超过 100%；
- 建议在独立压测机、接近生产数据量下复测以获得更准确的容量结论。
