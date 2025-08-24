$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$ORACLE_PWD = "YourSysPwd"  # must match docker-compose.yml

docker compose down
docker compose build --no-cache app
docker compose up -d db
docker compose up db-init
docker compose up -d app
docker compose logs -f app
