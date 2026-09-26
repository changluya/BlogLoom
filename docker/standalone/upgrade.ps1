#Requires -Version 5.1
param([string]$Version)

$ErrorActionPreference = 'Stop'

function Invoke-DockerCompose([string[]]$Arguments) {
    & docker compose @Arguments
    if ($LASTEXITCODE -ne 0) { throw "docker compose $($Arguments -join ' ') 执行失败（退出码 $LASTEXITCODE）" }
}

function Set-DotEnvValue([string]$Name, [string]$Value) {
    $path = Join-Path (Get-Location) '.env'
    # 兼容 Windows PowerShell 5.1：空集合从 if 表达式输出时会变成 $null。
    $lines = New-Object 'System.Collections.Generic.List[string]'
    if (Test-Path -LiteralPath $path) {
        foreach ($line in [IO.File]::ReadAllLines($path)) { $lines.Add($line) }
    }
    $found = $false
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match "^$([regex]::Escape($Name))=") { $lines[$i] = "$Name=$Value"; $found = $true }
    }
    if (-not $found) { $lines.Add("$Name=$Value") }
    [IO.File]::WriteAllLines($path, $lines, [Text.UTF8Encoding]::new($false))
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw '未检测到 Docker。' }
& docker compose version *> $null
if ($LASTEXITCODE -ne 0) { throw '未检测到 Docker Compose v2。' }
if (-not (Test-Path 'docker-compose.yml')) { throw '当前目录没有 docker-compose.yml，请在 BlogLoom 部署目录执行。' }

$imageRepo = if ($env:IMAGE_REPO) { $env:IMAGE_REPO } else { 'codercl/blogloom' }
if ([string]::IsNullOrWhiteSpace($Version)) {
    try {
        $response = Invoke-RestMethod -UseBasicParsing -Uri "https://hub.docker.com/v2/repositories/$imageRepo/tags?page_size=100" -TimeoutSec 15
        $latest = $response.results | Where-Object name -Match '^v?\d+\.\d+\.\d+$' | ForEach-Object {
            [PSCustomObject]@{ Version = [version]($_.name -replace '^v', ''); Tag = $_.name }
        } | Sort-Object Version -Descending | Select-Object -First 1
        $Version = $latest.Tag
    } catch { Write-Warning "无法解析最新版本：$($_.Exception.Message)" }
}
if ([string]::IsNullOrWhiteSpace($Version)) { $Version = if ($env:IMAGE_TAG) { $env:IMAGE_TAG } else { 'latest' } }

Set-DotEnvValue 'IMAGE_TAG' $Version
Write-Host "[upgrade] 拉取镜像 ${imageRepo}:${Version} ..."
Invoke-DockerCompose -Arguments @('pull')
Write-Host '[upgrade] 重启服务...'
Invoke-DockerCompose -Arguments @('up', '-d')
Write-Host "[done] 已升级到 ${imageRepo}:${Version}"
