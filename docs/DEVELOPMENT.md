# Development Guide

## Project Boundaries

- Runtime path:
  - `backend/`: Spring Boot API (`/api/map/*`, `/api/taxi/*`)
  - `frontend/`: Vue app and map visualization
- Offline path:
  - `julia/`: ingestion and data preparation scripts
  - `julia/deprecated/`: archived scripts not used in current ingestion mainline

## Startup Order (Windows PowerShell)

1. Start backend (default profile, real DB service)

```powershell
Set-Location backend
mvn spring-boot:run
```

2. Start frontend (new terminal)

```powershell
Set-Location frontend
npm run dev
```

3. Open browser: `http://localhost:5173`

## Recommended Local Commands (No Interactive Prompt)

Use the repository scripts to avoid repeated port conflicts and `Invoke-WebRequest` interaction prompts:

```powershell
# Start backend (default profile) and auto-wait readiness
powershell -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1 -KillPortOwner

# Start backend in dev profile
powershell -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1 -Profile dev -KillPortOwner

# Smoke test backend APIs
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-backend.ps1

# Stop backend
powershell -ExecutionPolicy Bypass -File .\scripts\stop-backend.ps1
```

## Backend Profiles

- Default profile:
  - Uses `RealTaxiService` (database query path)
  - `app.taxi.mock-enabled: false` in `application.yml`
- `dev` and `test` profiles:
  - Can use `MockTaxiService` only when `app.taxi.mock-enabled=true`
  - Config files:
    - `backend/src/main/resources/application-dev.yml`
    - `backend/src/main/resources/application-test.yml`

Example for dev profile:

```powershell
Set-Location backend
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

## Quick Smoke Checks

After backend startup:

```powershell
Invoke-WebRequest -UseBasicParsing "http://localhost:8081/api/map/boundary?page=0&size=1"
Invoke-WebRequest -UseBasicParsing "http://localhost:8081/api/taxi/stats?startTime=2015-01-03T00:00:00&endTime=2015-01-03T00:30:00"
```

If frontend is running:

- Confirm Vite proxy forwards `/api` to `http://localhost:8081`
- Confirm map requests appear in backend logs (`/api/map/boundary`, `/api/taxi/trajectory`)

## Common Issues

1. `mvn spring-boot:run` ends with exit code 1
- If logs show application started and later stopped, this is often process termination (manual stop or terminal close), not compilation failure.
- Re-run with `-e` to inspect first `Caused by` in stack trace.

2. `APPLICATION FAILED TO START: Port 8081 was already in use`
- Find owner: `Get-NetTCPConnection -LocalPort 8081 -State Listen | Select-Object LocalPort, OwningProcess, State`
- Stop owner: `Stop-Process -Id <PID> -Force`
- Or use `scripts/start-backend.ps1 -KillPortOwner` directly.

3. `npm run dev` exits with code 1
- Check current directory is `frontend/`.
- Ensure dependencies exist (`node_modules`) and run `npm install` if needed.
- Check Vite port 5173 is not occupied.

4. Frontend cannot fetch data
- Ensure backend is running on port 8081 with context path `/api`.
- Verify `frontend/vite.config.js` proxy target is `http://localhost:8081`.

5. Slow or timeout on trajectory endpoints
- Trajectory slice default upper limit is high and can return large payloads.
- Narrow time range or bbox during manual tests.

## Repository Hygiene

- Data files (`data/`, `*.h5`, `*.jld2`) are not tracked by Git.
- Build outputs are ignored (`backend/target/`, `frontend/dist/`).
