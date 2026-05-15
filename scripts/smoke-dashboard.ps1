param(
    [string]$BaseUrl = "http://localhost:8081",
    [switch]$Verbose
)

$passed = 0
$failed = 0

function Test-Api {
    param($Name, $Url, $TimeoutSec = 60)
    try {
        $res = Invoke-WebRequest -UseBasicParsing -Uri $Url -Method Get -TimeoutSec $TimeoutSec
        $global:passed++
        Write-Host "  [PASS] $Name" -ForegroundColor Green
        if ($Verbose) {
            $c = $res.Content
            Write-Host "         $($c.Substring(0, [Math]::Min(150, $c.Length)))"
        }
    } catch {
        $global:failed++
        Write-Host "  [FAIL] $Name - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n========== Dashboard API Smoke Test (P1 + P2) ==========" -ForegroundColor Cyan
Write-Host "Base URL: $BaseUrl`n"

Write-Host "--- P1: Congestion APIs ---" -ForegroundColor Yellow
Test-Api -Name "Congestion Heatmap" -Url "$BaseUrl/api/dashboard/congestion/heatmap?dt=2015-01-05&hour=12&day_type=workday"
Test-Api -Name "KPI" -Url "$BaseUrl/api/dashboard/kpi?dt=2015-01-05"
Test-Api -Name "Congestion Comparison" -Url "$BaseUrl/api/dashboard/congestion/comparison"
Test-Api -Name "Congestion Trend" -Url "$BaseUrl/api/dashboard/congestion/trend?start_dt=2015-01-03&end_dt=2015-01-07"
Test-Api -Name "Road Type Speed" -Url "$BaseUrl/api/dashboard/congestion/road-type-speed?dt=2015-01-05"
Test-Api -Name "Duration Ranking" -Url "$BaseUrl/api/dashboard/congestion/duration-ranking?dt=2015-01-05"

Write-Host ""
Write-Host "--- P2: Hotspot & Driver APIs ---" -ForegroundColor Yellow
Test-Api -Name "Hotspot Map (pickup)" -Url "$BaseUrl/api/dashboard/hotspot/map?dt=2015-01-05&hour=8&event_type=pickup"
Test-Api -Name "Hotspot Map (dropoff)" -Url "$BaseUrl/api/dashboard/hotspot/map?dt=2015-01-05&hour=8&event_type=dropoff"
Test-Api -Name "Driver Behavior" -Url "$BaseUrl/api/dashboard/driver/behavior?dt=2015-01-05"
Test-Api -Name "Driver Rest Heatmap (limit=1000)" -Url "$BaseUrl/api/dashboard/driver/rest-heatmap?dt=2015-01-05&limit=1000"

Write-Host "`n========== Summary ==========" -ForegroundColor Cyan
Write-Host "Total: $($passed + $failed) | Passed: $passed | Failed: $failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
if ($failed -eq 0) { exit 0 } else { exit 1 }
