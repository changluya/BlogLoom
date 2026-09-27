# BlogLoom Windows 一键部署

安装并启动 Docker Desktop 后，打开 **PowerShell**，复制下面一整行执行：

```powershell
$dir = Join-Path $HOME 'blogloom'; New-Item -ItemType Directory -Force $dir | Out-Null; Set-Location $dir; curl.exe --ssl-no-revoke -fL --retry 5 'https://gitee.com/changluJava/blog-loom/raw/master/docker/standalone/windows/install.ps1' -o install.ps1; if ($LASTEXITCODE -eq 0) { powershell.exe -ExecutionPolicy Bypass -File .\install.ps1 }
```

安装器会自动完成目录创建、国内镜像源配置（如需）、镜像拉取和服务启动。
