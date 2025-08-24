@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

REM Hardcoded SYSTEM password as in docker-compose.yml (edit if you change compose)
set "ORACLE_PWD=YourSysPwd"

echo [INFO] Using ORACLE_PWD=%ORACLE_PWD%

docker compose down

REM Rebuild fat-jar image without cache to avoid "no main manifest" issue
docker compose build --no-cache app
if errorlevel 1 (
  echo [ERROR] Build image failed.
  exit /b 1
)

docker compose up -d db
docker compose up db-init
if errorlevel 1 (
  echo [ERROR] db-init failed, see logs below:
  docker compose logs --no-log-prefix db-init
  exit /b 1
)

docker compose up -d app
docker compose logs -f app
