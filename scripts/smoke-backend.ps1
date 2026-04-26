$ErrorActionPreference = "Stop"

$boundaryUrl = "http://localhost:8081/api/map/boundary?page=0&size=1"
$statsUrl = "http://localhost:8081/api/taxi/stats?startTime=2015-01-03T00:00:00&endTime=2015-01-03T00:30:00"

$boundaryRes = Invoke-WebRequest -UseBasicParsing -Uri $boundaryUrl -Method Get -TimeoutSec 15
$statsRes = Invoke-WebRequest -UseBasicParsing -Uri $statsUrl -Method Get -TimeoutSec 15

$boundary = $boundaryRes.Content | ConvertFrom-Json
$stats = $statsRes.Content | ConvertFrom-Json

Write-Host "Boundary: status=$($boundaryRes.StatusCode), totalElements=$($boundary.totalElements), contentCount=$($boundary.content.Count)"
Write-Host "Taxi stats: status=$($statsRes.StatusCode), tripCount=$($stats.tripCount)"
