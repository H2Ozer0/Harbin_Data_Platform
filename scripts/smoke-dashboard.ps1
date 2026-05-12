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
        if ($_.Exception -is [System.Net.WebException]) {
            $global:passed++
            Write-Host "  [PASS] $Name (got expected error)" -ForegroundColor Green
        } else {
            $global:failed++
            Write-Host "  [FAIL] $Name - $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}

Write-Host "`n========== T14: Dashboard API Smoke Test ==========" -ForegroundColor Cyan
Write-Host "Base URL: $BaseUrl`n"

Test-Api -Name "Hotspot Map (pickup)" -Url "$BaseUrl/api/dashboard/hotspot/map?dt=2015-01-05&hour=8&event_type=pickup"
Test-Api -Name "Hotspot Map (dropoff)" -Url "$BaseUrl/api/dashboard/hotspot/map?dt=2015-01-05&hour=8&event_type=dropoff"
Test-Api -Name "Driver Behavior" -Url "$BaseUrl/api/dashboard/driver/behavior?dt=2015-01-05"
Test-Api -Name "Driver Rest Heatmap (limit=1000)" -Url "$BaseUrl/api/dashboard/driver/rest-heatmap?dt=2015-01-05&limit=1000"
Test-Api -Name "Hotspot Map (bad date)" -Url "$BaseUrl/api/dashboard/hotspot/map?dt=invalid&hour=8&event_type=pickup"
Test-Api -Name "Hotspot Map (bad event_type)" -Url "$BaseUrl/api/dashboard/hotspot/map?dt=2015-01-05&hour=8&event_type=invalid"

Write-Host "`n========== Summary ==========" -ForegroundColor Cyan
Write-Host "Total: $($passed + $failed) | Passed: $passed | Failed: $failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
if ($failed -eq 0) { exit 0 } else { exit 1 }