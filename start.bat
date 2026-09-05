@echo off
chcp 65001 >nul
setlocal

echo.
echo ╔══════════════════════════════════════════╗
echo ║         GMontinny — Iniciando...         ║
echo ╚══════════════════════════════════════════╝
echo.

:: ── 1. Verifica Docker ────────────────────────────────────────────────────────
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERRO] Docker não está rodando. Abra o Docker Desktop e tente novamente.
    pause
    exit /b 1
)

:: ── 2. Sobe os containers ─────────────────────────────────────────────────────
echo [1/4] Subindo PostgreSQL, RabbitMQ, Redis e Vault...
docker-compose up -d
if errorlevel 1 (
    echo [ERRO] Falha ao subir os containers.
    pause
    exit /b 1
)

:: ── 3. Aguarda o Vault ficar pronto ───────────────────────────────────────────
echo [2/4] Aguardando Vault ficar pronto...
:wait_vault
docker exec vault_gmontinny vault status >nul 2>&1
if errorlevel 1 (
    timeout /t 2 >nul
    goto wait_vault
)

:: ── 4. Popula os secrets no Vault ─────────────────────────────────────────────
echo [3/4] Populando secrets no Vault...
docker exec vault_gmontinny vault kv put secret/gmontinny ^
  jwt.secret="3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b" ^
  db.password="Gmontinny2026" ^
  rabbitmq.password="Gmontinny2026" ^
  redis.password="Redis2026" >nul 2>&1
if errorlevel 1 (
    echo [AVISO] Não foi possível popular o Vault. Verifique se o container está saudável.
)

:: ── 5. Inicia a aplicação ─────────────────────────────────────────────────────
echo [4/4] Iniciando a aplicação Spring Boot...
echo.
echo ╔══════════════════════════════════════════════════════════╗
echo ║  Acesse: http://localhost:8080/swagger-ui.html           ║
echo ║  Login:  admin / Admin@2026                              ║
echo ╚══════════════════════════════════════════════════════════╝
echo.
call gradlew.bat bootRun
