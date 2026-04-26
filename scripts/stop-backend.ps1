$ErrorActionPreference = "Stop"
$backendDir = Join-Path $PSScriptRoot "..\backend"
$pidFile = Join-Path $backendDir "backend_pid.txt"
$targetPort = 8081

$stopped = $false

if (Test-Path $pidFile) {
    $pidText = (Get-Content $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1).Trim()
    if ($pidText -match "^\d+$") {
        $procId = [int]$pidText
        try {
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            $stopped = $true
        } catch {}
    }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}

$listeners = Get-NetTCPConnection -LocalPort $targetPort -State Listen -ErrorAction SilentlyContinue
if ($listeners) {
    $owners = $listeners | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($ownerId in $owners) {
        Stop-Process -Id $ownerId -Force -ErrorAction SilentlyContinue
        $stopped = $true
    }
}

if ($stopped) {
    Write-Host "Backend process stopped."
} else {
    Write-Host "No backend process found on port $targetPort."
}
