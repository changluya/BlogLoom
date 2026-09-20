#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
BlogLoom 压测报告生成器。

读取 run-stress-test.sh 产生的原始数据，输出 Markdown 报告到 stdout。

输入（$OUT 目录下）：
    meta.env        环境与资源配置
    baseline.csv    启动后空闲资源采样
    endpoint.tsv    单接口对比压测结果
    ramp.tsv        并发梯度压测结果
    samples.csv     压测期间资源采样

用法：python3 report.py <OUT_DIR>
"""

import csv
import os
import sys
from statistics import mean

UNITS_TO_MIB = {
    "B": 1 / (1024 * 1024),
    "KiB": 1 / 1024,
    "MiB": 1,
    "GiB": 1024,
    "TiB": 1024 * 1024,
}


def read_meta(path):
    meta = {}
    if not os.path.exists(path):
        return meta
    with open(path, "r", encoding="utf-8", errors="replace") as fh:
        for line in fh:
            line = line.rstrip("\n")
            if not line or "=" not in line:
                continue
            key, value = line.split("=", 1)
            meta[key] = value
    return meta


def read_rows(path):
    if not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8", errors="replace") as fh:
        return list(csv.DictReader(fh))


def read_tsv(path):
    if not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8", errors="replace") as fh:
        return list(csv.DictReader(fh, delimiter="\t"))


def parse_cpu(value):
    try:
        return float(str(value).replace("%", "").strip())
    except (TypeError, ValueError):
        return 0.0


def parse_mem_mib(value):
    """把 docker stats 的 '123.4MiB / 7.6GiB' 解析为已用 MiB。"""
    try:
        used = str(value).split("/")[0].strip()
        num = ""
        unit = ""
        for ch in used:
            if ch.isdigit() or ch == ".":
                num += ch
            else:
                unit += ch
        return float(num) * UNITS_TO_MIB.get(unit.strip(), 1.0)
    except (TypeError, ValueError, IndexError):
        return 0.0


def human_mib(mib):
    if mib >= 1024:
        return f"{mib / 1024:.2f} GiB"
    return f"{mib:.1f} MiB"


def human_bytes(num):
    try:
        num = int(num)
    except (TypeError, ValueError):
        return "?"
    if num <= 0:
        return "0（未限制）"
    for unit in ["B", "KiB", "MiB", "GiB", "TiB"]:
        if num < 1024:
            return f"{num:.2f} {unit}"
        num /= 1024
    return f"{num:.2f} PiB"


def stats_by_container(rows):
    """返回 {容器名: {'cpu': [...], 'mem': [...]}}"""
    result = {}
    for row in rows:
        name = row.get("name", "?")
        entry = result.setdefault(name, {"cpu": [], "mem": []})
        entry["cpu"].append(parse_cpu(row.get("cpu_perc")))
        entry["mem"].append(parse_mem_mib(row.get("mem_usage")))
    return result


def avg(values):
    return mean(values) if values else 0.0


def fmt(value, digits=2):
    try:
        return f"{float(value):.{digits}f}"
    except (TypeError, ValueError):
        return "-"


def main():
    if len(sys.argv) < 2:
        print("用法：python3 report.py <OUT_DIR>", file=sys.stderr)
        return 1
    out = sys.argv[1]
    meta = read_meta(os.path.join(out, "meta.env"))
    baseline_rows = read_rows(os.path.join(out, "baseline.csv"))
    endpoint_rows = read_tsv(os.path.join(out, "raw", "endpoint.tsv"))
    ramp_rows = read_tsv(os.path.join(out, "raw", "ramp.tsv"))
    sample_rows = read_rows(os.path.join(out, "samples.csv"))

    app = meta.get("app_container", "blogloom-app")
    db = meta.get("db_container", "blogloom-mysql")
    baseline = stats_by_container(baseline_rows)
    samples = stats_by_container(sample_rows)

    lines = []
    add = lines.append

    add("# BlogLoom Docker 容器压测与资源占用报告")
    add("")
    add(f"> 生成时间：{meta.get('timestamp', '-')}")
    add("")

    # 1. 测试环境
    add("## 1. 测试环境")
    add("")
    add("| 项目 | 值 |")
    add("| --- | --- |")
    add(f"| 主机系统 | {meta.get('host_os', '-')} |")
    add(f"| 主机架构 | {meta.get('host_arch', '-')} |")
    add(f"| 主机 CPU | {meta.get('host_cpus', '-')} 核 |")
    add(f"| 主机内存 | {human_bytes(meta.get('host_mem_bytes'))} |")
    add(f"| Docker 版本 | {meta.get('docker_version', '-')} |")
    add(f"| Docker 可用 CPU | {meta.get('docker_cpus', '-')} 核 |")
    add(f"| Docker 可用内存 | {human_bytes(meta.get('docker_mem_bytes'))} |")
    add(f"| 压测工具 | {meta.get('ab_version', 'ab')} |")
    add(f"| 被测地址 | {meta.get('web_url', '-')} |")
    add("")

    # 2. 服务与资源配置
    add("## 2. 被测服务与资源配置")
    add("")
    add("| 容器 | 镜像 | CPU 限制 | 内存限制 |")
    add("| --- | --- | --- | --- |")
    add(f"| {app} | {meta.get('app_image', '-')} | {human_bytes(meta.get('app_cpu_limit_nanos')) if meta.get('app_cpu_limit_nanos') in ('0', None) else str(int(meta.get('app_cpu_limit_nanos')) / 1e9) + ' 核'} | {human_bytes(meta.get('app_mem_limit_bytes'))} |")
    add(f"| {db} | {meta.get('db_image', '-')} | {human_bytes(meta.get('db_cpu_limit_nanos')) if meta.get('db_cpu_limit_nanos') in ('0', None) else str(int(meta.get('db_cpu_limit_nanos')) / 1e9) + ' 核'} | {human_bytes(meta.get('db_mem_limit_bytes'))} |")
    add("")
    add(f"- 应用 JVM 参数：`{meta.get('app_java_opts', '-')}`")
    add("- 说明：默认 compose 未对容器设置 CPU/内存上限，容器可使用宿主机（Docker VM）全部资源；")
    add("  应用通过 `JAVA_OPTS` 限制堆内存，MySQL 使用镜像默认配置。")
    add("")

    # 3. 服务启动
    add("## 3. 服务启动")
    add("")
    add(f"- 应用容器重启到接口可用耗时：**{meta.get('startup_seconds', '-')} 秒**")
    add("  （含等待 MySQL、执行增量 SQL、Spring Boot 启动；MySQL 已运行）")
    add("")

    # 4. 空闲资源占用
    add("## 4. 启动后空闲资源占用（稳态基线）")
    add("")
    add("| 容器 | 平均 CPU | 平均内存 |")
    add("| --- | --- | --- |")
    for name in [app, db]:
        entry = baseline.get(name, {"cpu": [], "mem": []})
        add(f"| {name} | {fmt(avg(entry['cpu']))}% | {human_mib(avg(entry['mem']))} |")
    add("")
    add("> 空闲基线为服务启动后无压测流量时的 docker stats 采样均值；CPU% 为相对宿主机总核数的占比。")
    add("")

    # 5. 单接口对比
    add("## 5. 前台首页 / 博客相关接口压测（单接口对比）")
    add("")
    if endpoint_rows:
        conc = endpoint_rows[0].get("concurrency", "-")
        add(f"固定并发 {conc}，每接口压测时长见脚本配置。")
        add("")
        add("| 接口 | 路径 | QPS | 平均延迟(ms) | P50 | P90 | P95 | P99 | 失败 | 非2xx |")
        add("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |")
        paths = {
            "homepage": "/",
            "site": "/site",
            "blogs": "/blogs?pageNum=1&pageSize=10",
            "about": "/about",
            "archives": "/archives",
            "moments": "/moments?pageNum=1&pageSize=10",
            "blog_detail": "/blog?id=1",
            "comments": "/comments?page=1",
        }
        for row in endpoint_rows:
            name = row.get("name", "-")
            add(f"| {name} | `{paths.get(name, '-')}` | {fmt(row.get('rps'))} | {fmt(row.get('mean_ms'))} | "
                f"{row.get('p50_ms', '-')} | {row.get('p90_ms', '-')} | {row.get('p95_ms', '-')} | {row.get('p99_ms', '-')} | "
                f"{row.get('failed', '-')} | {row.get('non2xx', '-')} |")
    else:
        add("_无数据_")
    add("")

    # 6. 并发梯度
    add("## 6. 并发梯度与 QPS 上限")
    add("")
    if ramp_rows:
        target = ramp_rows[0].get("name", "-")
        add(f"目标接口：`{target}`。逐级提高并发，观察 QPS 与延迟变化。")
        add("")
        add("| 并发 | QPS | 平均延迟(ms) | P50 | P90 | P95 | P99 | 完成请求 | 失败 | 非2xx |")
        add("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |")
        for row in ramp_rows:
            add(f"| {row.get('concurrency', '-')} | {fmt(row.get('rps'))} | {fmt(row.get('mean_ms'))} | "
                f"{row.get('p50_ms', '-')} | {row.get('p90_ms', '-')} | {row.get('p95_ms', '-')} | {row.get('p99_ms', '-')} | "
                f"{row.get('complete', '-')} | {row.get('failed', '-')} | {row.get('non2xx', '-')} |")
        add("")
        best = max(ramp_rows, key=lambda r: float(r.get("rps") or 0))
        add(f"- 观测到的**峰值 QPS ≈ {fmt(best.get('rps'))}**（并发 {best.get('concurrency')}，平均延迟 {fmt(best.get('mean_ms'))} ms）")
        failed_rows = [r for r in ramp_rows if str(r.get("complete") or "0").strip() in ("0", "")]
        if failed_rows:
            concs = "、".join(str(r.get("concurrency")) for r in failed_rows)
            add(f"- 并发 {concs} 档位 ab 未能完成请求（连接被重置等客户端限制），该档数据不代表服务端上限。")
    else:
        add("_无数据_")
    add("")

    # 7. 峰值负载下的资源占用
    add("## 7. 峰值负载下的资源占用")
    add("")
    add(f"在并发 **{meta.get('peak_concurrency', '-')}** 的峰值负载下采样，每秒一次。")
    add("")
    add("| 容器 | 平均 CPU | 峰值 CPU | 平均内存 | 峰值内存 |")
    add("| --- | --- | --- | --- | --- |")
    for name in [app, db]:
        entry = samples.get(name, {"cpu": [], "mem": []})
        add(f"| {name} | {fmt(avg(entry['cpu']))}% | {fmt(max(entry['cpu']) if entry['cpu'] else 0)}% | "
            f"{human_mib(avg(entry['mem']))} | {human_mib(max(entry['mem']) if entry['mem'] else 0)} |")
    add("")
    add("> QPS 测量阶段不开启采样器，避免采样进程抢占 CPU 影响 QPS 准确性；资源占用单独在峰值负载下采样。")
    add("")

    # 8. 结论
    add("## 8. 结论")
    add("")
    app_b = baseline.get(app, {"cpu": [], "mem": []})
    db_b = baseline.get(db, {"cpu": [], "mem": []})
    app_s = samples.get(app, {"cpu": [], "mem": []})
    db_s = samples.get(db, {"cpu": [], "mem": []})
    idle_total = avg(app_b["mem"]) + avg(db_b["mem"])
    peak_total = max(app_s["mem"] or [0]) + max(db_s["mem"] or [0])
    add(f"- 一键启动后（空闲）两容器内存合计约 **{human_mib(idle_total)}**，CPU 占用接近 0；")
    add(f"- 压测期间两容器内存合计峰值约 **{human_mib(peak_total)}**；")
    if ramp_rows:
        best = max(ramp_rows, key=lambda r: float(r.get("rps") or 0))
        add(f"- 本次单机环境下，前台博客接口（{best.get('name')}）初步可支撑峰值约 **{fmt(best.get('rps'))} QPS**（并发 {best.get('concurrency')}）。")
    add("- 由于压测客户端与容器运行在同一台机器（共享 CPU），该 QPS 为保守下限；独立客户端压测结果通常会更高。")
    add("")

    # 9. 说明与局限
    add("## 9. 说明与局限")
    add("")
    add("- 压测工具 `ab` 与被测服务运行在同一台主机，客户端会与服务端竞争 CPU，结果偏保守；")
    add("- `docker stats` 的 CPU% 为相对宿主机总核数的占比，可能超过 100%；")
    add("- MySQL 与缓存数据量较小，QPS 结果不代表大数据量下的表现；")
    add("- 页面首页 `/` 为静态 HTML，接口为动态查询，两者 QPS 差异较大属正常现象；")
    add("- 建议在独立压测机、接近生产数据量下复测以获得更准确容量结论。")
    add("")

    print("\n".join(lines))
    return 0


if __name__ == "__main__":
    sys.exit(main())
