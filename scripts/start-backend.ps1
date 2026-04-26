param(
    [ValidateSet("default", "dev", "test")]
    [string]$Profile = "default",
    [switch]$KillPortOwner
)

$ErrorActionPreference = "Stop"
$backendDir = Join-Path $PSScriptRoot "..\backend"
$pidFile = Join-Path $backendDir "backend_pid.txt"
$targetPort = 8081

Set-Location $backendDir

$listeners = Get-NetTCPConnection -LocalPort $targetPort -State Listen -ErrorAction SilentlyContinue
if ($listeners) {
    $owners = $listeners | Select-Object -ExpandProperty OwningProcess -Unique
    if ($KillPortOwner) {
        foreach ($pid in $owners) {
            Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
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

$proc = Start-Process mvn -ArgumentList $args -PassThru -WindowStyle Hidden
$proc.Id | Out-File -FilePath $pidFile -Encoding utf8 -Force

$ready = $false
$start = Get-Date
while (((Get-Date) - $start).TotalSeconds -lt 90) {
    try {
        $res = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8081/api/map/boundary?page=0&size=1" -Method Get -TimeoutSec 5
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
