#!/bin/bash
set -e

echo ""
echo "╔══════════════════════════════════════════╗"
echo "║         GMontinny — Iniciando...         ║"
echo "╚══════════════════════════════════════════╝"
echo ""

# ── 1. Verifica Docker ────────────────────────────────────────────────────────
if ! docker info > /dev/null 2>&1; then
    echo "[ERRO] Docker não está rodando. Inicie o Docker e tente novamente."
    exit 1
fi

# ── 2. Sobe os containers ─────────────────────────────────────────────────────
echo "[1/4] Subindo PostgreSQL, RabbitMQ, Redis e Vault..."
docker-compose up -d

# ── 3. Aguarda o Vault ficar pronto ───────────────────────────────────────────
echo "[2/4] Aguardando Vault ficar pronto..."
until docker exec vault_gmontinny vault status > /dev/null 2>&1; do
    sleep 2
done

# ── 4. Popula os secrets no Vault ─────────────────────────────────────────────
echo "[3/4] Populando secrets no Vault..."
docker exec vault_gmontinny vault kv put secret/gmontinny \
  jwt.secret="3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b" \
  db.password="Gmontinny2026" \
  rabbitmq.password="Gmontinny2026" \
  redis.password="Redis2026" > /dev/null 2>&1 || \
  echo "[AVISO] Não foi possível popular o Vault. Verifique se o container está saudável."

# ── 5. Inicia a aplicação ─────────────────────────────────────────────────────
echo "[4/4] Iniciando a aplicação Spring Boot..."
echo ""
echo "╔══════════════════════════════════════════════════════════╗"
echo "║  Acesse: http://localhost:8080/swagger-ui.html           ║"
echo "║  Login:  admin / Admin@2026                              ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""
./gradlew bootRun
