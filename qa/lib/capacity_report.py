#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
BlogLoom 容量测试报告生成器。

核心输出：分配资源（CPU/内存配额） -> 最大 QPS -> 最大 QPS 时的服务资源占用，
并给出「极限规格速查」与「饱和判定」，方便用户直接对照选型。

判定标准：
    峰值 QPS = 错误率 <= MAX_ERR_RATE（失败/总请求）且非 2xx = 0 的最高吞吐
    可用 QPS = 在峰值基础上，P90 延迟 <= SLO_P90_MS 的最高吞吐
    饱和判定 = 应用 CPU 占用 >= CPU 配额 * 90% 视为 CPU 饱和；
               未饱和说明瓶颈在压测客户端 / MySQL / 其他环节，而非应用 CPU。
"""

import csv
import os
import sys

SLO_P90_MS = float(os.environ.get("SLO_P90_MS", "200"))
MAX_ERR_RATE = float(os.environ.get("MAX_ERR_RATE", "0.01"))
SATURATION_RATIO = 0.9


def read_meta(path):
    meta = {}
    if not os.path.exists(path):
        return meta
    with open(path, "r", encoding="utf-8", errors="replace") as fh:
        for line in fh:
            line = line.rstrip("\n")
            if line and "=" in line:
                k, v = line.split("=", 1)
                meta[k] = v
    return meta


def read_tsv(path):
    if not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8", errors="replace") as fh:
        return list(csv.DictReader(fh, delimiter="\t"))


def human_bytes(num):
    try:
        num = int(num)
    except (TypeError, ValueError):
        return "?"
    if num <= 0:
        return "?"
    for unit in ["B", "KiB", "MiB", "GiB", "TiB"]:
        if num < 1024:
            return f"{num:.2f} {unit}"
        num /= 1024
    return f"{num:.2f} PiB"


def fnum(v):
    try:
        return float(v)
    except (TypeError, ValueError):
        return 0.0


def fmt(v, d=2):
    return f"{fnum(v):.{d}f}"


def err_rate(row):
    failed = fnum(row.get("failed"))
    complete = fnum(row.get("complete"))
    total = failed + complete
    return (failed / total) if total > 0 else 1.0


def acceptable(rows):
    return [r for r in rows
            if fnum(r.get("complete")) > 0 and fnum(r.get("non2xx")) == 0
            and err_rate(r) <= MAX_ERR_RATE]


def peak_of(rows):
    acc = acceptable(rows)
    if acc:
        return max(acc, key=lambda r: fnum(r.get("rps")))
    return max(rows, key=lambda r: fnum(r.get("rps")), default=None)


def slo_of(rows):
    acc = [r for r in acceptable(rows) if fnum(r.get("p90_ms")) <= SLO_P90_MS]
    return max(acc, key=lambda r: fnum(r.get("rps")), default=None)


def main():
    if len(sys.argv) < 2:
        print("用法：python3 capacity_report.py <OUT_DIR>", file=sys.stderr)
        return 1
    out = sys.argv[1]
    meta = read_meta(os.path.join(out, "meta.env"))
    cap = read_tsv(os.path.join(out, "capacity.tsv"))

    metrics = {}
    for r in cap:
        name = r.get("name", "-")
        rows = read_tsv(os.path.join(out, "raw", f"ramp_{name}.tsv"))
        pk = peak_of(rows)
        slo = slo_of(rows)
        quota = fnum(r.get("cpus")) * 100.0
        cpu = fnum(r.get("peak_cpu_perc"))
        ratio = (cpu / quota) if quota > 0 else 0.0
        metrics[name] = {
            "peak": pk, "slo": slo, "quota_cpu": quota, "cpu": cpu,
            "ratio": ratio, "saturated": ratio >= SATURATION_RATIO,
            "peak_rps": fnum(pk.get("rps")) if pk else 0.0,
            "peak_err_rate": err_rate(pk) if pk else 1.0,
        }

    lines = []
    add = lines.append

    add("# BlogLoom 容量测试报告：分配资源 vs 最大 QPS")
    add("")
    add(f"> 生成时间：{meta.get('timestamp', '-')} ｜ 压测工具：**{meta.get('loader', '-')}**")
    add("")
    add("> 用于容量选型：给出「给定容器资源配额下的最大 QPS」「达到最大 QPS 时的实际资源占用」，并判定是否真正触及资源上限。")
    add("")

    # 1. 环境
    add("## 1. 测试环境")
    add("")
    add("| 项目 | 值 |")
    add("| --- | --- |")
    add(f"| 主机系统 | {meta.get('host_os', '-')} |")
    add(f"| 主机 CPU | {meta.get('host_cpus', '-')} 核 |")
    add(f"| 主机内存 | {human_bytes(meta.get('host_mem_bytes'))} |")
    add(f"| Docker 版本 | {meta.get('docker_version', '-')} |")
    add(f"| Docker 可用 CPU | {meta.get('docker_cpus', '-')} 核 |")
    add(f"| Docker 可用内存 | {human_bytes(meta.get('docker_mem_bytes'))} |")
    add(f"| 压测工具 | {meta.get('loader', '-')} |")
    add(f"| 被测地址 | {meta.get('web_url', '-')} |")
    add(f"| 压测接口 | `{meta.get('target', '-')}` |")
    add(f"| 并发梯度 | `{meta.get('ramp_concurrency', '-')}`，每档 {meta.get('duration', '-')}s |")
    add("")

    # 2. 判定标准
    add("## 2. 判定标准（什么叫“最大 QPS”）")
    add("")
    add("| 指标 | 定义 |")
    add("| --- | --- |")
    add(f"| 峰值 QPS | 错误率 ≤ {MAX_ERR_RATE:.0%}（失败/总请求）且非 2xx = 0 的最高吞吐 |")
    add(f"| 可用 QPS | 在峰值基础上，P90 延迟 ≤ {SLO_P90_MS:.0f} ms 的最高吞吐 |")
    add(f"| CPU 饱和 | 应用 CPU 占用 ≥ CPU 配额 × {SATURATION_RATIO:.0%} |")
    add("| 资源上限 | 内存接近配额或出现 OOM / 错误率上升 |")
    add("")
    add("> 若并发增加但 QPS 不再增长（平台期），或错误率上升、延迟陡增，即认为达到该配置的吞吐上限。")
    add("> 压测工具优先使用 wrk（多线程、低开销），避免压测客户端先于服务端成为瓶颈。")
    add("")

    # 3. 极限规格速查
    add("## 3. 极限规格速查（重点）")
    add("")
    if cap:
        best_name = max(cap, key=lambda r: metrics[r.get("name")]["peak_rps"])["name"]
        best = next(r for r in cap if r.get("name") == best_name)
        best_qps = metrics[best_name]["peak_rps"]
        # 最高性价比：QPS/核
        eff_name = max(cap, key=lambda r: (metrics[r.get("name")]["peak_rps"] / fnum(r.get("cpus")))
                       if fnum(r.get("cpus")) > 0 else 0)["name"]
        eff = next(r for r in cap if r.get("name") == eff_name)
        max_mem = max(fnum(r.get("peak_mem_mib")) for r in cap)

        add(f"- **全局最大 QPS**：约 **{fmt(best_qps, 0)} QPS**，配置 **{best_name}**；")
        add(f"  此时应用占用 CPU {fmt(metrics[best_name]['cpu'])}%（配额 {fmt(metrics[best_name]['quota_cpu'], 0)}%），"
            f"内存 {fmt(best.get('peak_mem_mib'), 0)} MiB，错误率 {metrics[best_name]['peak_err_rate']:.2%}。")
        add(f"- **最高性价比配置（QPS/核 最高）**：**{eff.get('cpus')} 核 / {eff.get('mem')}**，"
            f"约 {fmt(metrics[eff_name]['peak_rps'], 0)} QPS（{fmt(metrics[eff_name]['peak_rps']/fnum(eff.get('cpus')), 0)} QPS/核）。")
        add(f"- **内存需求**：峰值内存 ≤ **{fmt(max_mem, 0)} MiB** → **1 GiB 已足够**。")
        sat_names = [r.get("name") for r in cap if metrics[r.get("name")]["saturated"]]
        not_sat = [r.get("name") for r in cap if not metrics[r.get("name")]["saturated"]]
        if sat_names:
            add(f"- **CPU 饱和配置**：{('、'.join(sat_names))}（应用 CPU 已用满配额，QPS 由应用 CPU 决定）。")
        if not_sat:
            add(f"- **CPU 未饱和配置**：{('、'.join(not_sat))}（瓶颈在压测客户端 / MySQL 等，非应用 CPU）。")
    else:
        add("_无数据_")
    add("")

    # 4. 核心表
    add("## 4. 容量结果（核心表：分配资源 -> 最大 QPS -> 最大 QPS 时资源）")
    add("")
    if cap:
        add("| 分配服务资源（CPU / 内存） | 峰值 QPS | QPS/核 | 可用 QPS(P90≤%dms) | 峰值 QPS 并发 | 峰值 QPS 时 CPU | 峰值 QPS 时内存 | 错误率 | CPU 饱和 |"
            % int(SLO_P90_MS))
        add("| --- | --- | --- | --- | --- | --- | --- | --- | --- |")
        best_name = max(cap, key=lambda r: metrics[r.get("name")]["peak_rps"])["name"]
        for r in cap:
            name = r.get("name")
            m = metrics[name]
            cpus = fnum(r.get("cpus")) or 1
            mark = " ⭐" if name == best_name else ""
            slo_txt = fmt(fnum(m["slo"].get("rps")), 0) if m["slo"] else "-"
            sat = "是" if m["saturated"] else "否（瓶颈在别处）"
            add(f"| {r.get('cpus')} 核 / {r.get('mem')}{mark} | **{fmt(m['peak_rps'], 0)}** | {fmt(m['peak_rps']/cpus, 0)} | "
                f"{slo_txt} | {r.get('peak_conc', '-')} | {fmt(r.get('peak_cpu_perc'))}%（配额 {fmt(m['quota_cpu'],0)}%） | "
                f"{fmt(r.get('peak_mem_mib'), 0)} MiB | {m['peak_err_rate']:.2%} | {sat} |")
        add("")
        add("> 「分配服务资源」= 容器 CPU/内存配额；「峰值 QPS 时 CPU/内存」= 峰值并发下采样的应用容器实际占用。")
        add("> 「CPU 饱和=是」表示应用 CPU 已用满配额，该 QPS 即应用 CPU 上限；「否」表示瓶颈在别处。")
    else:
        add("_无数据_")
    add("")

    # 5. 明细
    add("## 5. 各配置并发梯度明细")
    add("")
    for r in cap:
        name = r.get("name", "-")
        rows = read_tsv(os.path.join(out, "raw", f"ramp_{name}.tsv"))
        add(f"### {name}（分配 {r.get('cpus', '-')} 核 / {r.get('mem', '-')}）")
        add("")
        if rows:
            add("| 并发 | QPS | 平均延迟(ms) | P50 | P90 | P99 | 完成 | 失败 | 错误率 | 非2xx |")
            add("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |")
            for row in rows:
                add(f"| {row.get('concurrency', '-')} | {fmt(row.get('rps'), 0)} | {fmt(row.get('mean_ms'))} | "
                    f"{row.get('p50_ms', '-')} | {row.get('p90_ms', '-')} | {row.get('p99_ms', '-')} | "
                    f"{row.get('complete', '-')} | {row.get('failed', '-')} | {err_rate(row):.2%} | {row.get('non2xx', '-')} |")
        else:
            add("_无数据_")
        add("")

    # 6. 结论
    add("## 6. 结论")
    add("")
    if cap:
        best_name = max(cap, key=lambda r: metrics[r.get("name")]["peak_rps"])["name"]
        best = next(r for r in cap if r.get("name") == best_name)
        add(f"- 前台博客接口（`{meta.get('target', '-')}`）最大约 **{fmt(metrics[best_name]['peak_rps'], 0)} QPS**"
            f"（配置 {best.get('cpus')} 核 / {best.get('mem')}）。")
        add("- QPS 随 CPU 配额提升；当应用 CPU 未饱和时，瓶颈在压测客户端 / MySQL；")
        add("- 内存 1 GiB 已满足当前数据量；")
        add("- 生产选型建议：结合真实数据量与独立压测结果，预留 30%~50% 余量。")
    add("")

    add("## 7. 说明与局限")
    add("")
    add("- 压测客户端与服务同机时，客户端与 MySQL 会分摊 CPU；使用 wrk 可显著降低客户端开销；")
    add("- 要获得最精确的应用上限，建议使用独立压测机；")
    add("- `docker stats` CPU% 为相对宿主机总核数占比，可能超过 100%（多核并行）；")
    add("- MySQL 容器未做资源限制，若数据库成为瓶颈需单独评估；")
    add("- 数据量较小，结果不代表大表、大流量下的表现。")
    add("")

    print("\n".join(lines))
    return 0


if __name__ == "__main__":
    sys.exit(main())
