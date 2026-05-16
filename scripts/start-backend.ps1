param(
    [ValidateSet("default", "dev", "test")]
    [string]$Profile = "default",
    [switch]$KillPortOwner
)

$ErrorActionPreference = "Stop"
$backendDir = Join-Path $PSScriptRoot "..\backend"
$pidFile = Join-Path $backendDir "backend_pid.txt"
$targetPort = 8082

Set-Location $backendDir

$listeners = Get-NetTCPConnection -LocalPort $targetPort -State Listen -ErrorAction SilentlyContinue
if ($listeners) {
    $owners = $listeners | Select-Object -ExpandProperty OwningProcess -Unique
    if ($KillPortOwner) {
        foreach ($procId in $owners) {
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        }
        Start-Sleep -Seconds 1
    } else {
        throw "Port $targetPort is already in use by PID(s): $($owners -join ', '). Re-run with -KillPortOwner."
    }
}

$args = @("spring-boot:run")
if ($Profile -ne "default") {
    $args += "-Dspring-boot.run.profiles=$Profile"
}

$mvnw = Join-Path $backendDir "mvnw.cmd"
if (Test-Path $mvnw) {
    $mvnLauncher = $mvnw
} elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
    $mvnLauncher = "mvn"
} else {
    throw "未找到 Maven：请安装 Maven 并加入 PATH，或使用项目自带的 backend\mvnw.cmd（应已随仓库提供）。"
}

$proc = Start-Process -FilePath $mvnLauncher -ArgumentList $args -WorkingDirectory $backendDir -PassThru -WindowStyle Hidden
$proc.Id | Out-File -FilePath $pidFile -Encoding utf8 -Force

$ready = $false
$start = Get-Date
while (((Get-Date) - $start).TotalSeconds -lt 90) {
    try {
        $res = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8082/api/map/boundary?page=0&size=1" -Method Get -TimeoutSec 5
        if ($res.StatusCode -eq 200) {
            $ready = $true
            break
        }
    } catch {
        Start-Sleep -Seconds 2
    }
}

if (-not $ready) {
    try { Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue } catch {}
    throw "Backend failed to become ready within timeout."
}

Write-Host "Backend started. PID=$($proc.Id), profile=$Profile, port=$targetPort"
