#Requires -Version 5.1
<#
BlogLoom Windows 一键部署脚本。

在 PowerShell 中执行：
  New-Item -ItemType Directory -Force blogloom | Out-Null; Set-Location blogloom
  irm https://raw.githubusercontent.com/changluya/BlogLoom/master/docker/standalone/install.ps1 | iex
#>

$ErrorActionPreference = 'Stop'

function Get-Setting([string]$Name, [string]$DefaultValue) {
    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) { return $DefaultValue }
    return $value
}

function Invoke-DockerCompose([string[]]$Arguments) {
    & docker compose @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose $($Arguments -join ' ') 执行失败（退出码 $LASTEXITCODE）"
    }
}

function Get-LatestTag([string]$Repository) {
    try {
        $response = Invoke-RestMethod -UseBasicParsing -Uri "https://hub.docker.com/v2/repositories/$Repository/tags?page_size=100" -TimeoutSec 15
        $versions = foreach ($item in $response.results) {
            if ($item.name -match '^v?(\d+)\.(\d+)\.(\d+)$') {
                [PSCustomObject]@{ Version = [version]"$($Matches[1]).$($Matches[2]).$($Matches[3])"; Tag = $item.name }
            }
        }
        return ($versions | Sort-Object Version -Descending | Select-Object -First 1).Tag
    } catch {
        Write-Warning "无法从 Docker Hub 解析最新版本，将使用 latest：$($_.Exception.Message)"
        return $null
    }
}

function New-RandomHex([int]$ByteCount = 16) {
    $bytes = New-Object byte[] $ByteCount
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return (($bytes | ForEach-Object { $_.ToString('x2') }) -join '')
}

function Read-DotEnv {
    $values = @{}
    if (Test-Path -LiteralPath '.env') {
        foreach ($line in [IO.File]::ReadAllLines((Join-Path (Get-Location) '.env'))) {
            if ($line -match '^([^#=]+)=(.*)$') { $values[$Matches[1]] = $Matches[2] }
        }
    }
    return $values
}

function Set-DotEnvValue([string]$Name, [string]$Value) {
    $path = Join-Path (Get-Location) '.env'
    $lines = if (Test-Path -LiteralPath $path) { [Collections.Generic.List[string]]::new([IO.File]::ReadAllLines($path)) } else { [Collections.Generic.List[string]]::new() }
    $replaced = $false
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match "^$([regex]::Escape($Name))=") {
            $lines[$i] = "$Name=$Value"
            $replaced = $true
        }
    }
    if (-not $replaced) { $lines.Add("$Name=$Value") }
    [IO.File]::WriteAllLines($path, $lines, [Text.UTF8Encoding]::new($false))
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw '未检测到 Docker，请先安装并启动 Docker Desktop：https://docs.docker.com/desktop/setup/install/windows-install/'
}
& docker compose version *> $null
if ($LASTEXITCODE -ne 0) { throw '未检测到 Docker Compose v2，请确认 Docker Desktop 已启动。' }

$imageRepo = Get-Setting 'IMAGE_REPO' 'codercl/blogloom'
$webPort = Get-Setting 'WEB_PORT' '18080'
$tag = Get-Setting 'IMAGE_TAG' ''
if ([string]::IsNullOrWhiteSpace($tag)) { $tag = Get-LatestTag $imageRepo }
if ([string]::IsNullOrWhiteSpace($tag)) { $tag = 'latest' }
Write-Host "[init] 使用镜像版本：${imageRepo}:${tag}"

if (-not (Test-Path -LiteralPath 'docker-compose.yml')) {
    $compose = @'
name: blogloom

services:
  mysql:
    image: mysql:8.0
    container_name: blogloom-mysql
    restart: unless-stopped
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-blogloom_root_pwd}
      MYSQL_ROOT_HOST: "%"
      MYSQL_DATABASE: ${MYSQL_DATABASE:-blogloom}
      TZ: ${TZ:-Asia/Shanghai}
    ports:
      - "${MYSQL_PORT:-13306}:3306"
    volumes:
      - ./data/mysql:/var/lib/mysql
    healthcheck:
      test: ["CMD-SHELL", "mysql -h 127.0.0.1 -uroot -p$$MYSQL_ROOT_PASSWORD -e 'SELECT 1' >/dev/null 2>&1"]
      interval: 5s
      timeout: 5s
      retries: 30

  blogloom:
    image: ${IMAGE_REPO:-codercl/blogloom}:${IMAGE_TAG:-latest}
    container_name: blogloom-app
    restart: unless-stopped
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      DB_HOST: mysql
      DB_PORT: "3306"
      DB_USER: root
      DB_PASSWORD: ${MYSQL_ROOT_PASSWORD:-blogloom_root_pwd}
      DB_NAME: ${MYSQL_DATABASE:-blogloom}
      BLOG_NAME: ${BLOG_NAME:-BlogLoom}
      BLOG_API: ${BLOG_API:-http://localhost:18080}
      BLOG_CMS: ${BLOG_CMS:-http://localhost:18080/cms}
      BLOG_VIEW: ${BLOG_VIEW:-http://localhost:18080}
      TOKEN_SECRET: ${TOKEN_SECRET:-please-change-this-token-secret}
      TZ: ${TZ:-Asia/Shanghai}
      JAVA_OPTS: ${JAVA_OPTS:--Xms256m -Xmx512m}
    ports:
      - "${WEB_PORT:-18080}:8090"
    volumes:
      - ./data/logs:/opt/blogloom/conf/logs
      - ./data/upload:/opt/blogloom/conf/upload
      - ./data/sql-local:/opt/blogloom/sql/local
'@
    [IO.File]::WriteAllText((Join-Path (Get-Location) 'docker-compose.yml'), $compose, [Text.UTF8Encoding]::new($false))
    Write-Host '[init] 已生成 docker-compose.yml'
}

Set-DotEnvValue 'IMAGE_TAG' $tag
$envValues = Read-DotEnv
if ($envValues.ContainsKey('MYSQL_ROOT_PASSWORD')) {
    $mysqlPassword = $envValues['MYSQL_ROOT_PASSWORD']
} elseif ((Test-Path 'data/mysql') -and (Get-ChildItem 'data/mysql' -Force -ErrorAction SilentlyContinue | Select-Object -First 1)) {
    $mysqlPassword = Get-Setting 'MYSQL_ROOT_PASSWORD' 'blogloom_root_pwd'
    Set-DotEnvValue 'MYSQL_ROOT_PASSWORD' $mysqlPassword
    Write-Warning '检测到已有数据库数据，已沿用历史默认密码。'
} else {
    $mysqlPassword = Get-Setting 'MYSQL_ROOT_PASSWORD' (New-RandomHex)
    Set-DotEnvValue 'MYSQL_ROOT_PASSWORD' $mysqlPassword
    Write-Host '[init] 已生成随机数据库密码并写入 .env'
}
$envValues = Read-DotEnv
if ($envValues.ContainsKey('TOKEN_SECRET')) {
    $tokenSecret = $envValues['TOKEN_SECRET']
} else {
    $tokenSecret = Get-Setting 'TOKEN_SECRET' (New-RandomHex)
    Set-DotEnvValue 'TOKEN_SECRET' $tokenSecret
    Write-Host '[init] 已生成随机令牌密钥并写入 .env'
}

# 保存 Windows 升级脚本，与 Linux 版安装后生成 upgrade.sh 的行为一致。
if (-not (Test-Path -LiteralPath 'upgrade.ps1')) {
    try {
        $upgradeUri = 'https://raw.githubusercontent.com/changluya/BlogLoom/master/docker/standalone/upgrade.ps1'
        $upgradeContent = (Invoke-WebRequest -UseBasicParsing -Uri $upgradeUri -TimeoutSec 15).Content
        [IO.File]::WriteAllText((Join-Path (Get-Location) 'upgrade.ps1'), $upgradeContent, [Text.UTF8Encoding]::new($false))
        Write-Host '[init] 已生成 upgrade.ps1'
    } catch {
        Write-Warning "未能保存 upgrade.ps1，不影响本次部署：$($_.Exception.Message)"
    }
}

foreach ($directory in @('data/mysql', 'data/logs', 'data/upload', 'data/sql-local')) {
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
}

Write-Host "[deploy] Windows/$env:PROCESSOR_ARCHITECTURE，Docker 将自动选择对应的 Linux 镜像架构"
Write-Host '[deploy] 拉取镜像并启动服务（首次会下载镜像，请稍候）...'
Invoke-DockerCompose -Arguments @('pull')
Invoke-DockerCompose -Arguments @('up', '-d')

$mysqlPort = Get-Setting 'MYSQL_PORT' '13306'
$database = Get-Setting 'MYSQL_DATABASE' 'blogloom'
Write-Host ''
Write-Host '================ BlogLoom 部署完成 ================'
Write-Host "  博客前台：http://localhost:$webPort"
Write-Host "  管理后台：http://localhost:$webPort/cms"
Write-Host '  默认账号：admin / 123456（请登录后立即修改）'
Write-Host "  MySQL：localhost:$mysqlPort  用户 root  库 $database"
Write-Host "  MySQL 密码：$mysqlPassword"
Write-Host "  运行版本：${imageRepo}:${tag}"
Write-Host "  数据目录：$((Resolve-Path 'data').Path)"
Write-Host '  版本升级：powershell -ExecutionPolicy Bypass -File .\upgrade.ps1'
Write-Host '  查看日志：docker logs -f blogloom-app'
Write-Host '  停止服务：docker compose down'
Write-Host '==================================================='
