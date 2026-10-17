#!/usr/bin/env bash

# 从 ip2region 官方仓库同步最新 IPv4 / IPv6 XDB 数据文件。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
IPDB_DIR="$PROJECT_ROOT/blog-backend/src/main/resources/ipdb"
UPSTREAM_BASE_URL="${IP2REGION_BASE_URL:-https://raw.githubusercontent.com/lionsoul2014/ip2region/master/data}"
UPSTREAM_BASE_URL="${UPSTREAM_BASE_URL%/}"

usage() {
    cat <<EOF
用法：$0

同步 ip2region 官方 data 目录中的两份最新数据库：
  ip2region_v4.xdb  IPv4
  ip2region_v6.xdb  IPv6

可通过 IP2REGION_BASE_URL 指定兼容的下载镜像根地址。
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
    usage
    exit 0
elif [[ "$#" -gt 0 ]]; then
    usage >&2
    exit 2
fi

command -v curl >/dev/null 2>&1 || {
    printf '[ERROR] 未找到 curl，请先安装 curl。\n' >&2
    exit 1
}
if ! command -v shasum >/dev/null 2>&1 && ! command -v sha256sum >/dev/null 2>&1; then
    printf '[ERROR] 未找到 shasum 或 sha256sum，无法输出数据文件校验值。\n' >&2
    exit 1
fi

sha256_file() {
    if command -v shasum >/dev/null 2>&1; then
        LC_ALL=C shasum -a 256 "$1" | awk '{print $1}'
    else
        sha256sum "$1" | awk '{print $1}'
    fi
}

mkdir -p "$IPDB_DIR"
STAGE_DIR="$(mktemp -d "$IPDB_DIR/.ip2region-update.XXXXXX")"

cleanup() {
    rm -f "$STAGE_DIR/ip2region_v4.xdb" "$STAGE_DIR/ip2region_v6.xdb"
    rmdir "$STAGE_DIR" 2>/dev/null || true
}
trap cleanup EXIT

download_xdb() {
    local filename="$1"
    local url="$UPSTREAM_BASE_URL/$filename"
    local output="$STAGE_DIR/$filename"
    local size

    printf '[INFO] 下载 %s\n' "$url"
    curl --fail --location --silent --show-error \
        --retry 3 --connect-timeout 20 --max-time 300 \
        --output "$output" "$url"

    size="$(wc -c < "$output" | tr -d '[:space:]')"
    if [[ ! "$size" =~ ^[0-9]+$ ]] || (( size < 1048576 )); then
        printf '[ERROR] %s 文件异常或不完整（%s bytes），取消更新。\n' "$filename" "$size" >&2
        exit 1
    fi
}

# 两份文件都下载并通过基本校验后才替换，避免只更新成功一份。
download_xdb "ip2region_v4.xdb"
download_xdb "ip2region_v6.xdb"

mv -f "$STAGE_DIR/ip2region_v4.xdb" "$IPDB_DIR/ip2region_v4.xdb"
mv -f "$STAGE_DIR/ip2region_v6.xdb" "$IPDB_DIR/ip2region_v6.xdb"

printf '[SUCCESS] 官方 IPv4 / IPv6 XDB 已同步到：%s\n' "$IPDB_DIR"
printf '  IPv4 SHA-256: %s\n' "$(sha256_file "$IPDB_DIR/ip2region_v4.xdb")"
printf '  IPv6 SHA-256: %s\n' "$(sha256_file "$IPDB_DIR/ip2region_v6.xdb")"
printf '[NOTE] 后端统一查询服务会自动识别 IP 版本，并使用对应的 XDB。\n'
