$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8081/api/dashboard"

$tests = @(
    @{ Name = "heatmap"; Url = "$baseUrl/congestion/heatmap?dt=2015-01-05&hour=12&day_type=workday" }
    @{ Name = "kpi"; Url = "$baseUrl/kpi?dt=2015-01-05" }
    @{ Name = "comparison"; Url = "$baseUrl/congestion/comparison" }
    @{ Name = "trend"; Url = "$baseUrl/congestion/trend?start_dt=2015-01-03&end_dt=2015-01-07" }
)

$allPassed = $true

Write-Host "===== P1 Dashboard API Smoke Test ====="
Write-Host ""

foreach ($test in $tests) {
    $name = $test.Name
    $url = $test.Url
    $start = Get-Date

    try {
        $res = Invoke-WebRequest -UseBasicParsing -Uri $url -Method Get -TimeoutSec 30
        $elapsed = (Get-Date) - $start
        $ms = [math]::Round($elapsed.TotalMilliseconds)
        $data = $res.Content | ConvertFrom-Json

        $ok = ($res.StatusCode -eq 200)
        if ($ok) {
            Write-Host "[PASS] $name" -ForegroundColor Green
        } else {
            Write-Host "[FAIL] $name (HTTP $($res.StatusCode))" -ForegroundColor Red
        }
        Write-Host "       Time: ${ms}ms"

        if ($name -eq "heatmap") {
            $total = $data.total
            $segCount = if ($data.segments) { $data.segments.Count } else { 0 }
            $firstRoad = if ($segCount -gt 0) { $data.segments[0].road_name } else { "N/A" }
            Write-Host "       total=$total segments=$segCount topRoad=$firstRoad"
        }
        elseif ($name -eq "kpi") {
            $v = $data.total_vehicles
            $t = $data.total_trips
            $s = [math]::Round($data.avg_speed_kmh, 1)
            $h = $data.most_active_hour
            $top5count = if ($data.top5_congested) { $data.top5_congested.Count } else { 0 }
            Write-Host "       vehicles=$v trips=$t avgSpeed=$s activeHour=$h top5=$top5count"
        }
        elseif ($name -eq "comparison") {
            $count = if ($data.hourly) { $data.hourly.Count } else { 0 }
            Write-Host "       hours=$count"
        }
        elseif ($name -eq "trend") {
            $count = if ($data.daily) { $data.daily.Count } else { 0 }
            Write-Host "       days=$count"
        }
        Write-Host ""

        if (-not $ok) { $allPassed = $false }
    }
    catch {
        $elapsed = (Get-Date) - $start
        $ms = [math]::Round($elapsed.TotalMilliseconds)
        Write-Host "[FAIL] $name (Exception)" -ForegroundColor Red
        Write-Host "       Time: ${ms}ms"
        Write-Host "       Error: $($_.Exception.Message)"
        Write-Host ""
        $allPassed = $false
    }
}

if ($allPassed) {
    Write-Host "ALL P1 API TESTS PASSED" -ForegroundColor Green
} else {
    Write-Host "SOME P1 API TESTS FAILED" -ForegroundColor Red
}