#Requires -Version 5.1

$ErrorActionPreference = 'Stop'

$mirrors = @(
    'https://docker.1panelproxy.com',
    'https://2a6bf1988cb6428c877f723ec7530dbc.mirror.swr.myhuaweicloud.com',
    'https://docker.m.daocloud.io',
    'https://hub-mirror.c.163.com',
    'https://mirror.baidubce.com',
    'https://dockerhub.icu',
    'https://docker.registry.cyou',
    'https://docker-cf.registry.cyou',
    'https://dockercf.jsdelivr.fyi',
    'https://docker.jsdelivr.fyi',
    'https://dockertest.jsdelivr.fyi',
    'https://mirror.aliyuncs.com',
    'https://dockerproxy.com',
    'https://docker.nju.edu.cn',
    'https://docker.mirrors.sjtug.sjtu.edu.cn',
    'https://docker.mirrors.ustc.edu.cn',
    'https://mirror.iscas.ac.cn',
    'https://docker.rainbond.cc'
)

$dockerConfigDirectory = Join-Path $HOME '.docker'
$daemonConfigPath = Join-Path $dockerConfigDirectory 'daemon.json'
New-Item -ItemType Directory -Force -Path $dockerConfigDirectory | Out-Null

if (Test-Path -LiteralPath $daemonConfigPath) {
    $backupPath = "$daemonConfigPath.$(Get-Date -Format 'yyyyMMddHHmmss').bak"
    Copy-Item -LiteralPath $daemonConfigPath -Destination $backupPath
    Write-Host "[backup] 已备份现有配置：$backupPath"
    $content = [IO.File]::ReadAllText($daemonConfigPath)
    $config = if ([string]::IsNullOrWhiteSpace($content)) { [PSCustomObject]@{} } else { $content | ConvertFrom-Json }
} else {
    $config = [PSCustomObject]@{}
}

if ($null -ne $config.PSObject.Properties['registry-mirrors']) {
    $config.'registry-mirrors' = $mirrors
} else {
    $config | Add-Member -NotePropertyName 'registry-mirrors' -NotePropertyValue $mirrors
}

$json = $config | ConvertTo-Json -Depth 32
[IO.File]::WriteAllText($daemonConfigPath, $json, [Text.UTF8Encoding]::new($false))
Write-Host "[done] Docker 镜像源已写入：$daemonConfigPath"

try {
    & docker desktop restart
    if ($LASTEXITCODE -ne 0) { throw "docker desktop restart 退出码：$LASTEXITCODE" }
    Write-Host '[done] Docker Desktop 已重启，现在可重新执行 BlogLoom 安装命令。'
} catch {
    Write-Warning '配置已保存，但无法自动重启 Docker Desktop，请手动退出并重新打开 Docker Desktop。'
}
