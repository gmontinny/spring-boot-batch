# GMontinny — Spring Boot Batch API

![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen?logo=springboot)
![Spring Batch](https://img.shields.io/badge/Spring%20Batch-6.x-brightgreen?logo=spring)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.x-orange?logo=rabbitmq)
![Redis](https://img.shields.io/badge/Redis-7-red?logo=redis)
![Vault](https://img.shields.io/badge/Vault-1.17-black?logo=vault)
![Liquibase](https://img.shields.io/badge/Liquibase-4.27-red)
![JWT](https://img.shields.io/badge/JWT-0.12.6-black?logo=jsonwebtokens)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI%203.1-85EA2D?logo=swagger)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)
![Tests](https://img.shields.io/badge/Testes-55%2B%20casos-success?logo=junit5)

API REST para processamento em lote de dados CNAE a partir de planilhas Excel, com persistência em PostgreSQL, mensageria via RabbitMQ, segurança JWT com refresh token e rate limiting, secrets centralizados no HashiCorp Vault e documentação Swagger completa.

---

## 🚀 Início Rápido

> Pré-requisitos: **Java 25** e **Docker Desktop** instalados e rodando.

**Windows:**
```bat
start.bat
```

**Linux / macOS:**
```bash
chmod +x start.sh && ./start.sh
```

Os scripts fazem tudo automaticamente:
1. Sobem PostgreSQL, RabbitMQ, Redis e Vault via Docker
2. Populam os secrets no Vault
3. Iniciam a aplicação Spring Boot

Quando aparecer `Started GmontinnyApplication`, acesse:

| O que | URL |
|---|---|
| Swagger UI (API interativa) | http://localhost:8080/swagger-ui.html |
| RabbitMQ | http://localhost:15672 — `gmontinny` / `Gmontinny2026` |
| Vault | http://localhost:8200/ui — token: `gmontinny-vault-token` |

Login padrão: **admin** / **Admin@2026**

---

## Índice

1. [Visão Geral](#1-visão-geral)
2. [Tecnologias](#2-tecnologias)
3. [Arquitetura](#3-arquitetura)
4. [Garantias de Processamento em Lote](#4-garantias-de-processamento-em-lote)
5. [Estrutura do Projeto](#5-estrutura-do-projeto)
6. [Modelo de Dados](#6-modelo-de-dados)
7. [Segurança e Autenticação](#7-segurança-e-autenticação)
8. [Refresh Token](#8-refresh-token)
9. [Rate Limiting](#9-rate-limiting)
10. [HashiCorp Vault](#10-hashicorp-vault)
11. [Spring Batch](#11-spring-batch)
12. [RabbitMQ](#12-rabbitmq)
13. [Liquibase](#13-liquibase)
14. [API Endpoints](#14-api-endpoints)
15. [HATEOAS](#15-hateoas)
16. [Swagger / OpenAPI](#16-swagger--openapi)
17. [Testes](#17-testes)
18. [Configuração de Ambiente](#18-configuração-de-ambiente)
19. [Como Executar](#19-como-executar)
    - [Scripts de Início Rápido](#scripts-de-início-rápido)
20. [Docker](#20-docker)
21. [Boas Práticas Aplicadas](#21-boas-práticas-aplicadas)
22. [Fluxo Completo de Uso](#22-fluxo-completo-de-uso)
23. [Compatibilidade com Spring Boot 4.x / Spring 7.x](#23-compatibilidade-com-spring-boot-4x--spring-7x)

---

## 1. Visão Geral

O **GMontinny** é uma aplicação Spring Boot que demonstra uma arquitetura completa de processamento em lote integrada a uma API REST segura. O sistema importa a tabela CNAE 2.0 (Classificação Nacional de Atividades Econômicas) a partir de um arquivo Excel, processa os dados via Spring Batch, publica eventos no RabbitMQ e persiste os registros no PostgreSQL.

### Principais Capacidades

| Capacidade | Implementação |
|---|---|
| Processamento em lote assíncrono | Spring Batch + `AsyncJobLauncher` |
| Leitura de Excel | Apache POI (formato `.xls`) |
| Persistência | Spring Data JPA + PostgreSQL + HikariCP |
| Versionamento de schema | Liquibase com changelogs XML |
| Mensageria com retry e DLQ | RabbitMQ + backoff exponencial |
| Segurança stateless | Spring Security + JWT |
| Refresh Token com rotation | Persistido no PostgreSQL, revogação em lote |
| Rate Limiting distribuído | Bucket4j + Redis (por IP, por rota) |
| Secrets centralizados | HashiCorp Vault (KV v2) |
| Controle de acesso | Roles `ROLE_ADMIN` e `ROLE_USER` |
| Mapeamento de objetos | MapStruct |
| Hypermedia | Spring HATEOAS |
| Documentação | SpringDoc OpenAPI 3 (Swagger UI) |
| Containerização | Docker + Docker Compose (4 serviços) |
| Testes | JUnit 5 + Mockito (55+ casos) |

---

## 2. Tecnologias

| Tecnologia | Versão | Finalidade |
|---|---|---|
| Java | 25 | Linguagem principal |
| Spring Boot | 4.1.1 | Framework base |
| Spring Batch | 6.0.5 (via Boot) | Processamento em lote |
| Spring Security | 7.x (via Boot) | Autenticação e autorização |
| Spring Data JPA | 4.x (via Boot) | ORM e repositórios |
| Spring Data Redis | 4.x (via Boot) | Store distribuído para rate limiting |
| Spring HATEOAS | 3.x (via Boot) | Hypermedia na API |
| Spring AMQP | 4.x (via Boot) | Integração RabbitMQ |
| Spring Cloud | 2025.1.2 (BOM) | Gerenciamento de dependências Cloud |
| Spring Cloud Vault | via BOM | Integração HashiCorp Vault |
| Liquibase | 4.27 | Migrations de banco |
| PostgreSQL | 16 | Banco de dados relacional |
| HikariCP | via Boot | Connection pool otimizado |
| Redis | 7 | Rate limiting + blacklist de tokens |
| RabbitMQ | 3.x | Message broker com DLQ |
| HashiCorp Vault | 1.17 | Gerenciamento centralizado de secrets |
| JJWT | 0.12.6 | Geração e validação de tokens JWT |
| Bucket4j | 8.10.1 | Rate limiting (token bucket algorithm) |
| MapStruct | 1.6.3 | Mapeamento entre entidades e DTOs |
| Apache POI | 5.3.0 | Leitura de arquivos Excel `.xls` |
| SpringDoc OpenAPI | 3.1.0 | Documentação Swagger UI |
| Lombok | latest (via Boot) | Redução de boilerplate |
| H2 | via Boot (test) | Banco em memória para testes |
| JUnit 5 + Mockito | via Boot (test) | Testes unitários e de integração |
| Gradle | 9.7.1 | Build tool |
| Docker | — | Containerização |

---

## 3. Arquitetura

### Visão Macro

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            CLIENT (HTTP)                                │
│                Swagger UI / Postman / Frontend                          │
└─────────────────────────────┬───────────────────────────────────────────┘
                              │ HTTP :8080
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       SPRING BOOT APPLICATION                           │
│                                                                         │
│  ┌─────────────────┐   ┌──────────────┐   ┌──────────────────────────┐ │
│  │ RateLimitFilter │──▶│  JWT Filter  │──▶│       Controllers        │ │
│  │ Bucket4j+Redis  │   │  (Stateless) │   │  Auth/User/CNAE/Batch    │ │
│  └─────────────────┘   └──────────────┘   └────────────┬─────────────┘ │
│         │                                              │               │
│         ▼                                              ▼               │
│  ┌─────────────┐                          ┌────────────────────────┐   │
│  │    REDIS    │                          │        Services        │   │
│  │  rl:login:* │                          │  Auth/User/CNAE/Batch  │   │
│  │  rl:api:*   │                          └────────────┬───────────┘   │
│  └─────────────┘                                       │               │
│                                                        ▼               │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                       SPRING BATCH                               │   │
│  │  AsyncJobLauncher → ThreadPoolTaskExecutor (batch-worker-*)      │   │
│  │  Reader (POI) → Processor (+RabbitMQ) → Writer (JPA)            │   │
│  │  chunk(100) | retryLimit(3) | skipLimit(10) | StepListener      │   │
│  └──────────────────────────────┬──────────────────────────────────┘   │
│                                 │                                       │
│  ┌──────────────────────────────▼──────────────────────────────────┐   │
│  │                         RABBITMQ                                 │   │
│  │  batch.exchange → batch.cnae.queue → CnaeEventConsumer          │   │
│  │                └→ batch.cnae.dlq   → onCnaeDeadLetter           │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │              POSTGRESQL (HikariCP pool: 2~10)                    │   │
│  │  users │ roles │ user_roles │ refresh_tokens │ cnae │ BATCH_*   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │              HASHICORP VAULT (secret/gmontinny)                  │   │
│  │  jwt.secret │ db.password │ rabbitmq.password │ redis.password  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

### Fluxo de Autenticação Completo (Login + Refresh + Logout)

```
─── LOGIN ──────────────────────────────────────────────────────────────
Client                  RateLimitFilter          API              DB
  │                           │                   │               │
  │── POST /auth/login ───────▶│                   │               │
  │                           │── bucket ok? ─────▶│               │
  │                           │   (20 req/min)     │               │
  │                           │                   │── authenticate │
  │                           │                   │── revokeAll ──▶│
  │                           │                   │── persist RT ─▶│
  │◀── {accessToken(15min),   │                   │               │
  │     refreshToken(7d),     │                   │               │
  │     expiresIn} ───────────│───────────────────│               │

─── USO DO ACCESS TOKEN ────────────────────────────────────────────────
  │── GET /api/v1/cnae ────────────────────────────▶│               │
  │   Authorization: Bearer <accessToken>           │               │
  │◀── 200 + HATEOAS ──────────────────────────────│               │

─── REFRESH (token rotation) ───────────────────────────────────────────
  │── POST /auth/refresh ──────────────────────────▶│               │
  │   {refreshToken: "old"}                         │── findByToken▶│
  │                                                 │── revoke old ▶│
  │                                                 │── persist new▶│
  │◀── {newAccessToken, newRefreshToken} ───────────│               │

─── LOGOUT ─────────────────────────────────────────────────────────────
  │── POST /auth/logout ───────────────────────────▶│               │
  │   Authorization: Bearer <accessToken>           │── revokeAll ─▶│
  │◀── 204 No Content ─────────────────────────────│               │
```

### Fluxo de Rate Limiting

```
Request (qualquer rota)
         │
         ▼
RateLimitFilter (OncePerRequestFilter)
  ├── Extrai IP (X-Forwarded-For ou remoteAddr)
  ├── Determina bucket key:
  │     /auth/login → "rl:login:<ip>"  (20 req/60s)
  │     demais      → "rl:api:<ip>"    (200 req/60s)
  ├── Consulta ProxyManager → Redis (Bucket4j distribuído)
  ├── tryConsumeAndReturnRemaining(1)
  │
  ├── Consumido? → adiciona header X-Rate-Limit-Remaining
  │               → passa para o próximo filtro
  │
  └── Bloqueado? → HTTP 429 Too Many Requests
                 → header Retry-After: <segundos>
                 → body JSON com mensagem
```

### Fluxo Spring Batch

```
POST /api/v1/batch/cnae/run  (thread HTTP retorna imediatamente)
         │
         ▼
   AsyncJobLauncher.run(cnaeImportJob, {timestamp})
         │
         ▼
   ┌─────────────────────────────────────────────────────┐
   │                  cnaeImportStep                      │
   │  chunk: 100  │  skipLimit: 10  │  retryLimit: 3      │
   │                                                      │
   │  CnaeExcelReader → CnaeItemProcessor → CnaeItemWriter│
   │  └─ POI lê .xls   └─ publica MQ      └─ saveAll()   │
   └─────────────────────────────────────────────────────┘
         │
         ▼
   RabbitMQ: batch.cnae.queue
   └─ Retry backoff: 2s → 4s → 8s → DLQ
```

---

## 4. Garantias de Processamento em Lote

| Requisito | Mecanismo | Configurável |
|---|---|---|
| Utilização de Recursos | `AsyncJobLauncher` + HikariCP pool | `maximum-pool-size` |
| Redução de Sobrecarga | `chunk(100)` + `saveAll()` | `app.batch.chunk-size` |
| Processamento Paralelo | `ThreadPoolTaskExecutor` (`batch-worker-*`) | `app.batch.thread-pool-size` |
| Execução Agendada | `@EnableScheduling` + `@Scheduled` (descomentável) | `app.batch.cron` |
| Consistência | Transação por chunk + `StepExecutionListener` | — |
| Tolerância a Falhas | `faultTolerant()` + retry RabbitMQ + DLQ | `app.batch.skip-limit` |
| Capacidade de Reiniciar | `JobRepository` PostgreSQL + `RunIdIncrementer` | — |
| Redução de Custos | Todos os mecanismos acima combinados | — |

### Utilização de Recursos

O `AsyncJobLauncher` executa o job em thread separada (`job-launcher-*`), liberando imediatamente o thread HTTP. O `HikariCP` gerencia o pool de conexões com `maximum-pool-size: 10` e `minimum-idle: 2`.

```java
// BatchConfig.java
@Bean @Primary
public JobLauncher asyncJobLauncher() throws Exception {
    TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
    launcher.setTaskExecutor(new SimpleAsyncTaskExecutor("job-launcher-"));
    launcher.afterPropertiesSet();
    return launcher;
}
```

### Redução da Sobrecarga de I/O

```
Sem chunk:  1.000 registros = 1.000 INSERTs = 1.000 transações
Com chunk:  1.000 registros = 10 saveAll()  = 10 transações  (redução de 100x)
```

### Processamento Paralelo

```java
// BatchConfig.java
executor.setCorePoolSize(threadPoolSize);      // padrão: 4
executor.setMaxPoolSize(threadPoolSize * 2);   // padrão: 8
executor.setQueueCapacity(50);
executor.setThreadNamePrefix("batch-worker-");
executor.setWaitForTasksToCompleteOnShutdown(true);
```

### Execução Agendada

```java
// BatchService.java — descomente para ativar
// @Scheduled(cron = "${app.batch.cron:0 0 2 * * *}")
public void runScheduled() { runCnaeImport(); }
```

| Expressão | Significado |
|---|---|
| `0 0 2 * * *` | Todo dia às 02:00 |
| `0 0 0 * * MON` | Toda segunda-feira à meia-noite |
| `0 0/30 * * * *` | A cada 30 minutos |

### Tolerância a Falhas

```java
// BatchConfig.java
.faultTolerant()
    .skipLimit(skipLimit)    // até 10 itens ignorados
    .skip(Exception.class)
    .retryLimit(3)           // até 3 tentativas por item
    .retry(Exception.class)
```

### Capacidade de Reiniciar

O Spring Batch persiste o estado no PostgreSQL (tabelas `BATCH_*`). Se o job falhar no chunk 5 de 10, o estado `FAILED` é salvo e o job pode ser retomado do ponto de falha.

---

## 5. Estrutura do Projeto

```
gmontinny/
├── data/
│   └── CNAE20_EstruturaDetalhada.xls
│
├── src/main/
│   ├── java/br/com/gmontinny/
│   │   ├── GmontinnyApplication.java          # @EnableBatchProcessing + @EnableScheduling
│   │   │
│   │   ├── batch/
│   │   │   ├── CnaeRow.java
│   │   │   ├── reader/CnaeExcelReader.java
│   │   │   ├── processor/CnaeItemProcessor.java
│   │   │   └── writer/CnaeItemWriter.java
│   │   │
│   │   ├── config/
│   │   │   ├── BatchConfig.java               # Job, Step, AsyncJobLauncher, ThreadPool
│   │   │   ├── RabbitMQConfig.java            # Exchange, Queue, DLQ, retry backoff
│   │   │   ├── RedisConfig.java               # Bean ProxyManager<String> para Bucket4j
│   │   │   ├── SecurityConfig.java            # FilterChain + RateLimitFilter + JWT
│   │   │   ├── VaultConfig.java               # HashiCorp Vault (AbstractVaultConfiguration)
│   │   │   ├── OpenApiConfig.java             # Swagger + Bearer Auth
│   │   │   └── GlobalExceptionHandler.java    # ProblemDetail (RFC 9457)
│   │   │
│   │   ├── controller/
│   │   │   ├── AuthController.java            # /login + /refresh + /logout
│   │   │   ├── UserController.java            # CRUD /users (ADMIN)
│   │   │   ├── CnaeController.java            # GET /cnae (ADMIN+USER)
│   │   │   └── BatchController.java           # POST /batch/cnae/run (ADMIN)
│   │   │
│   │   ├── domain/
│   │   │   ├── entity/
│   │   │   │   ├── User.java
│   │   │   │   ├── Role.java
│   │   │   │   ├── Cnae.java
│   │   │   │   └── RefreshToken.java          # token + user + expiresAt + revoked
│   │   │   └── repository/
│   │   │       ├── UserRepository.java
│   │   │       ├── RoleRepository.java
│   │   │       ├── CnaeRepository.java
│   │   │       └── RefreshTokenRepository.java # findByToken + revokeAllByUserId
│   │   │
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── UserRequest.java
│   │   │   │   └── RefreshTokenRequest.java   # refreshToken
│   │   │   └── response/
│   │   │       ├── AuthResponse.java          # token + refreshToken + expiresIn
│   │   │       ├── UserResponse.java
│   │   │       └── CnaeResponse.java
│   │   │
│   │   ├── mapper/
│   │   │   ├── UserMapper.java
│   │   │   └── CnaeMapper.java
│   │   │
│   │   ├── messaging/
│   │   │   └── CnaeEventConsumer.java         # queue + DLQ listener
│   │   │
│   │   ├── security/
│   │   │   ├── JwtService.java                # access + refresh token (claim type)
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── RateLimitFilter.java           # Bucket4j + Redis (por IP, por rota)
│   │   │   └── UserDetailsServiceImpl.java
│   │   │
│   │   └── service/
│   │       ├── AuthService.java               # login + refresh (rotation) + logout
│   │       ├── UserService.java
│   │       ├── CnaeService.java
│   │       └── BatchService.java
│   │
│   └── resources/
│       ├── application.yml
│       ├── bootstrap.yaml                     # Vault config (carregado antes do contexto)
│       ├── data/CNAE20_EstruturaDetalhada.xls
│       └── db/changelog/
│           ├── db.changelog-master.xml
│           └── v1/
│               ├── 001-create-roles.xml
│               ├── 002-create-users.xml
│               ├── 003-create-user-roles.xml
│               ├── 004-create-cnae.xml
│               ├── 005-create-batch-metadata.xml
│               ├── 006-insert-default-data.xml
│               └── 007-create-refresh-tokens.xml  # token + user_id + expires_at + revoked
│
├── src/test/
│   ├── java/br/com/gmontinny/
│   │   ├── batch/
│   │   │   ├── CnaeItemProcessorTest.java
│   │   │   └── CnaeItemWriterTest.java
│   │   ├── controller/
│   │   │   ├── AuthControllerTest.java        # login/refresh/logout
│   │   │   └── BatchControllerTest.java
│   │   ├── messaging/CnaeEventConsumerTest.java
│   │   ├── security/JwtServiceTest.java       # access/refresh/tipo errado/expirado
│   │   ├── service/
│   │   │   ├── AuthServiceTest.java           # login/refresh rotation/logout
│   │   │   ├── BatchServiceTest.java
│   │   │   ├── CnaeServiceTest.java
│   │   │   └── UserServiceTest.java
│   │   └── GmontinnyApplicationTests.java
│   └── resources/
│       ├── application.yml                    # H2 + propriedades de teste
│       └── bootstrap.yml                      # Vault desabilitado (spring.cloud.vault.enabled: false)
│
├── .env                                       # PostgreSQL + RabbitMQ + Redis + Vault
├── docker-compose.yml                         # 4 serviços com healthcheck
├── Dockerfile                                 # Multi-stage build (JDK 25)
├── start.bat                                  # Início rápido — Windows
├── start.sh                                   # Início rápido — Linux/macOS
└── build.gradle
```

---

## 6. Modelo de Dados

### Diagrama Entidade-Relacionamento

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│    users     │       │  user_roles  │       │    roles     │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ id (PK)      │──────▶│ user_id (FK) │◀──────│ id (PK)      │
│ username     │       │ role_id (FK) │       │ name         │
│ email        │       └──────────────┘       └──────────────┘
│ password     │
│ enabled      │       ┌──────────────────────────────────┐
│ created_at   │       │         refresh_tokens           │
└──────┬───────┘       ├──────────────────────────────────┤
       │               │ id (PK)                          │
       └──────────────▶│ token        VARCHAR(512) UNIQUE │
                       │ user_id (FK) → users.id CASCADE  │
                       │ expires_at   TIMESTAMP           │
                       │ revoked      BOOLEAN             │
                       │ created_at   TIMESTAMP           │
                       └──────────────────────────────────┘

┌──────────────────────────────────────┐
│                 cnae                 │
├──────────────────────────────────────┤
│ id (PK)                              │
│ secao        VARCHAR(10)             │
│ divisao      VARCHAR(10)             │
│ grupo        VARCHAR(10)             │
│ classe       VARCHAR(10)             │
│ subclasse    VARCHAR(20)  [INDEX]    │
│ denominacao  VARCHAR(500) NOT NULL   │
│ observacoes  TEXT                    │
│ processed_at TIMESTAMP               │
└──────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│              Spring Batch Metadata Tables                    │
├─────────────────────────────────────────────────────────────┤
│ BATCH_JOB_INSTANCE / BATCH_JOB_EXECUTION                    │
│ BATCH_JOB_EXECUTION_PARAMS / BATCH_STEP_EXECUTION           │
│ BATCH_STEP_EXECUTION_CONTEXT / BATCH_JOB_EXECUTION_CONTEXT  │
└─────────────────────────────────────────────────────────────┘
```

### Changelogs Liquibase

| Arquivo | O que cria |
|---|---|
| `001-create-roles.xml` | Tabela `roles` |
| `002-create-users.xml` | Tabela `users` |
| `003-create-user-roles.xml` | Tabela `user_roles` (N:N) + FKs |
| `004-create-cnae.xml` | Tabela `cnae` + índice `subclasse` |
| `005-create-batch-metadata.xml` | 6 tabelas `BATCH_*` + sequences |
| `006-insert-default-data.xml` | Roles padrão + usuário admin |
| `007-create-refresh-tokens.xml` | Tabela `refresh_tokens` + índices + FK CASCADE |

---

## 7. Segurança e Autenticação

### Estratégia

Autenticação **stateless** com tokens **JWT**. Dois tipos de token com claims distintos:

| Token | Expiração | Claim `type` | Uso |
|---|---|---|---|
| Access Token | 15 minutos | `access` | Header `Authorization: Bearer` |
| Refresh Token | 7 dias | `refresh` | Body de `POST /auth/refresh` |

Um refresh token **não pode** ser usado como access token e vice-versa — o `JwtService` valida o claim `type` em todas as verificações.

### Roles e Permissões

| Role | Permissões |
|---|---|
| `ROLE_ADMIN` | Acesso total: usuários, batch, CNAE, auth |
| `ROLE_USER` | Somente leitura de CNAE (`GET /api/v1/cnae/**`) |

### Matriz de Acesso

| Endpoint | Público | ROLE_USER | ROLE_ADMIN |
|---|:---:|:---:|:---:|
| `POST /api/v1/auth/login` | ✅ | ✅ | ✅ |
| `POST /api/v1/auth/refresh` | ✅ | ✅ | ✅ |
| `POST /api/v1/auth/logout` | ❌ | ✅ | ✅ |
| `GET /swagger-ui.html` | ✅ | ✅ | ✅ |
| `GET /api/v1/cnae/**` | ❌ | ✅ | ✅ |
| `GET /api/v1/users/**` | ❌ | ❌ | ✅ |
| `POST /api/v1/users` | ❌ | ❌ | ✅ |
| `DELETE /api/v1/users/{id}` | ❌ | ❌ | ✅ |
| `POST /api/v1/batch/cnae/run` | ❌ | ❌ | ✅ |

### Pipeline de Segurança (ordem dos filtros)

```
Request
  │
  ▼
RateLimitFilter          ← bloqueia antes de qualquer processamento
  │ (429 se exceder)
  ▼
JwtAuthenticationFilter  ← valida Bearer token + claim type=access
  │
  ▼
SecurityFilterChain      ← verifica roles por rota
  │
  ▼
Controller → @PreAuthorize (dupla verificação por método)
```

### Usuário Padrão (seed via Liquibase)

| Campo | Valor |
|---|---|
| Username | `admin` |
| Password | `Admin@2026` |
| Email | `admin@gmontinny.com.br` |
| Role | `ROLE_ADMIN` |

> Senha armazenada com **BCrypt strength 12**.

---

## 8. Refresh Token

### Ciclo de Vida

```
Login
  └─ Revoga todos os refresh tokens anteriores do usuário
  └─ Gera access token (15 min) + refresh token (7 dias)
  └─ Persiste refresh token na tabela refresh_tokens
  └─ Retorna {token, refreshToken, type, username, expiresIn}

Uso normal (access token válido)
  └─ Usa access token no header Authorization: Bearer <token>

Access token expirado → Refresh
  └─ POST /auth/refresh com {refreshToken: "..."}
  └─ Valida token no banco (não revogado, não expirado)
  └─ Valida claim type=refresh no JWT
  └─ Revoga o refresh token atual (rotation)
  └─ Gera novo par access + refresh
  └─ Persiste novo refresh token
  └─ Retorna novo par

Logout
  └─ POST /auth/logout (requer access token válido)
  └─ Revoga TODOS os refresh tokens do usuário no banco
  └─ Access token continua válido até expirar (15 min)
```

### Token Rotation

Cada uso do refresh token **invalida o anterior** e emite um novo. Isso garante que:
- Um refresh token roubado só pode ser usado uma vez
- Após o uso legítimo, o token roubado é automaticamente invalidado
- Tentativas de reutilização retornam `401 Unauthorized`

### Estrutura da Tabela

```sql
CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    token      VARCHAR(512) UNIQUE NOT NULL,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    revoked    BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Configuração

```yaml
app.jwt:
  expiration-ms: 900000          # 15 minutos — access token
  refresh-expiration-ms: 604800000  # 7 dias — refresh token
```

---

## 9. Rate Limiting

### Algoritmo Token Bucket (Bucket4j + Redis)

O rate limiting usa o algoritmo **Token Bucket**: cada IP tem um balde com capacidade máxima de tokens. Cada requisição consome 1 token. Os tokens são reabastecidos gradualmente (greedy refill).

O estado dos buckets é armazenado no **Redis** via `LettuceBasedProxyManager`, tornando o rate limiting **distribuído** — funciona corretamente com múltiplas instâncias da aplicação.

### Limites Configurados

| Rota | Chave Redis | Capacidade | Reabastecimento | Configurável |
|---|---|---|---|---|
| `POST /auth/login` | `rl:login:<ip>` | 20 tokens | 20/60s | `app.rate-limit.login-*` |
| Demais rotas | `rl:api:<ip>` | 200 tokens | 200/60s | `app.rate-limit.api-*` |

O limite de login é mais restritivo para **prevenir ataques de força bruta**.

### Headers de Resposta

Toda requisição recebe o header:
```
X-Rate-Limit-Remaining: 19
```

Quando bloqueada (`429 Too Many Requests`):
```
HTTP/1.1 429 Too Many Requests
Retry-After: 45
X-Rate-Limit-Remaining: 0
Content-Type: application/json

{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit atingido. Tente novamente em 45 segundos."
}
```

### Resolução de IP

O filtro extrai o IP real do cliente respeitando proxies e load balancers:

```java
// RateLimitFilter.java
String forwarded = request.getHeader("X-Forwarded-For");
if (forwarded != null && !forwarded.isBlank()) {
    return forwarded.split(",")[0].trim();  // primeiro IP da cadeia
}
return request.getRemoteAddr();
```

### Configuração

```yaml
app.rate-limit:
  login-capacity: 20
  login-refill-tokens: 20
  login-refill-seconds: 60
  api-capacity: 200
  api-refill-tokens: 200
  api-refill-seconds: 60
```

---

## 10. HashiCorp Vault

### Por que Vault?

Secrets como JWT secret, senhas de banco e credenciais de serviços **não devem** estar em arquivos de configuração ou variáveis de ambiente em texto plano em produção. O Vault centraliza, versiona e audita o acesso a todos os secrets.

### Secrets Armazenados

| Path no Vault | Secret | Usado em |
|---|---|---|
| `secret/gmontinny` | `jwt.secret` | Assinatura JWT |
| `secret/gmontinny` | `db.password` | Conexão PostgreSQL |
| `secret/gmontinny` | `rabbitmq.password` | Conexão RabbitMQ |
| `secret/gmontinny` | `redis.password` | Conexão Redis |

### Configuração (bootstrap.yaml)

A configuração do Vault fica em `bootstrap.yaml` — carregado **antes** do contexto Spring, garantindo que os secrets estejam disponíveis na inicialização:

```yaml
# src/main/resources/bootstrap.yaml
spring:
  cloud:
    vault:
      enabled: ${VAULT_ENABLED:true}
      host: ${VAULT_HOST:localhost}
      port: ${VAULT_PORT:8200}
      scheme: http
      authentication: TOKEN
      token: ${VAULT_DEV_TOKEN:gmontinny-vault-token}
      kv:
        enabled: true
        backend: secret
        default-context: gmontinny   # path: secret/gmontinny
      fail-fast: false
  config:
    import: "optional:vault://"
```

### Populando Secrets (modo dev)

Após subir o `docker-compose`, execute:

```bash
# Via CLI dentro do container
docker exec vault_gmontinny vault kv put secret/gmontinny \
  jwt.secret="3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b" \
  db.password="Gmontinny2026" \
  rabbitmq.password="Gmontinny2026" \
  redis.password="Redis2026"

# Verificar
docker exec vault_gmontinny vault kv get secret/gmontinny
```

### Vault UI

```
URL:   http://localhost:8200/ui
Token: gmontinny-vault-token
```

### Modo Dev vs Produção

| Aspecto | Modo Dev (atual) | Produção |
|---|---|---|
| Autenticação | Token estático | AppRole ou Kubernetes Auth |
| Armazenamento | Memória (volátil) | Backend persistente (Raft/Consul) |
| TLS | Desabilitado | Obrigatório |
| Token | `VAULT_DEV_ROOT_TOKEN_ID` | Token com TTL curto + renovação |
| `fail-fast` | `false` | `true` |

> Em produção, remova `VAULT_DEV_ROOT_TOKEN_ID` do docker-compose e configure o Vault em modo server com TLS e backend persistente.

---

## 11. Spring Batch

### Conceitos Utilizados

| Conceito | Implementação |
|---|---|
| `Job` | `cnaeImportJob` — orquestra o processamento |
| `Step` | `cnaeImportStep` — único step com chunk |
| `ItemReader` | `CnaeExcelReader` — lê `.xls` via Apache POI |
| `ItemProcessor` | `CnaeItemProcessor` — valida, mapeia e publica no MQ |
| `ItemWriter` | `CnaeItemWriter` — persiste via `saveAll()` |
| `JobRepository` | Metadados no PostgreSQL (tabelas `BATCH_*`) |
| `AsyncJobLauncher` | Execução não bloqueante (thread `job-launcher-*`) |
| `ThreadPoolTaskExecutor` | Pool de workers (`batch-worker-*`) |
| `RunIdIncrementer` | Nova `JobInstance` a cada execução |
| `faultTolerant()` | `skipLimit(10)` + `retryLimit(3)` |
| `StepExecutionListener` | Log de métricas ao final do step |

### Parâmetros Configuráveis

```yaml
app.batch:
  chunk-size: 100        # registros por transação
  skip-limit: 10         # máximo de itens ignorados
  thread-pool-size: 4    # workers paralelos
  # cron: "0 0 2 * * *" # agendamento (descomentável)
```

### Verificar Execuções no Banco

```sql
SELECT job_execution_id, status, exit_code, start_time, end_time
FROM BATCH_JOB_EXECUTION ORDER BY create_time DESC;

SELECT step_name, read_count, write_count, filter_count, skip_count, status
FROM BATCH_STEP_EXECUTION ORDER BY create_time DESC;
```

---

## 12. RabbitMQ

### Topologia

```
CnaeItemProcessor
      │ convertAndSend(JSON)
      ▼
batch.exchange (Direct, durable)
      │ routing-key: batch.cnae
      ▼
batch.cnae.queue (durable)
  x-dead-letter-exchange:    batch.exchange
  x-dead-letter-routing-key: batch.cnae.dead
      │
      ▼
CnaeEventConsumer
  concurrency: 2~5
  RetryInterceptor: 3 tentativas, backoff 2s→4s→8s
      │ falha após 3x
      ▼
batch.cnae.dlq
  onCnaeDeadLetter() → log + ponto de extensão
```

### Constantes

| Constante | Valor |
|---|---|
| `BATCH_EXCHANGE` | `batch.exchange` |
| `BATCH_QUEUE` | `batch.cnae.queue` |
| `BATCH_ROUTING_KEY` | `batch.cnae` |
| `DLQ_QUEUE` | `batch.cnae.dlq` |
| `DLQ_ROUTING_KEY` | `batch.cnae.dead` |

### Retry com Backoff Exponencial

| Tentativa | Intervalo | Ação |
|---|---|---|
| 1ª | imediato | Processa |
| 2ª | 2 segundos | Reprocessa |
| 3ª | 4 segundos | Reprocessa |
| Esgotado | — | Encaminha para DLQ |

### Management UI

```
URL:     http://localhost:15672
Usuário: gmontinny
Senha:   Gmontinny2026
```

---

## 13. Liquibase

### Changelogs

```
db/changelog/
├── db.changelog-master.xml
└── v1/
    ├── 001-create-roles.xml
    ├── 002-create-users.xml
    ├── 003-create-user-roles.xml
    ├── 004-create-cnae.xml
    ├── 005-create-batch-metadata.xml
    ├── 006-insert-default-data.xml
    └── 007-create-refresh-tokens.xml   ← novo
```

### Configuração

```yaml
spring:
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
    enabled: true
  batch.jdbc.initialize-schema: never   # Liquibase gerencia BATCH_*
  jpa.hibernate.ddl-auto: validate      # Hibernate só valida
```

---

## 14. API Endpoints

### Base URL

```
http://localhost:8080/api/v1
```

### Autenticação

#### `POST /api/v1/auth/login`

**Request:**
```json
{ "username": "admin", "password": "Admin@2026" }
```

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "expiresIn": 900000
}
```

**Response `429 Too Many Requests`** (após 20 tentativas/min):
```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit atingido. Tente novamente em 45 segundos."
}
```

---

#### `POST /api/v1/auth/refresh`

Renova o access token. O refresh token atual é revogado e um novo par é emitido.

**Request:**
```json
{ "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }
```

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "expiresIn": 900000
}
```

**Response `401 Unauthorized`** (token expirado, revogado ou inválido):
```json
{ "status": 401, "detail": "Refresh token expirado ou revogado" }
```

---

#### `POST /api/v1/auth/logout`

Requer `Authorization: Bearer <accessToken>`. Revoga todos os refresh tokens do usuário.

**Response `204 No Content`**

---

### Usuários — requer `ROLE_ADMIN`

#### `GET /api/v1/users?page=0&size=10`
#### `GET /api/v1/users/{id}`
#### `POST /api/v1/users` → `201 Created`
#### `DELETE /api/v1/users/{id}` → `204 No Content`

**Request de criação:**
```json
{
  "username": "joao.silva",
  "email": "joao@email.com",
  "password": "Senha@123",
  "roles": ["ROLE_USER"]
}
```

---

### CNAE — requer `ROLE_ADMIN` ou `ROLE_USER`

#### `GET /api/v1/cnae?page=0&size=20`
#### `GET /api/v1/cnae/{id}`
#### `GET /api/v1/cnae/search?q=agricultura&page=0&size=10`

---

### Batch — requer `ROLE_ADMIN`

#### `POST /api/v1/batch/cnae/run`

Retorna imediatamente com o `jobExecutionId`. Use o endpoint de status para acompanhar.

**Response `200 OK`:**
```json
{ "message": "Job iniciado com id: 1 | Status: STARTING" }
```

#### `GET /api/v1/batch/cnae/status/{jobExecutionId}`

**Response `200 OK` — em execução:**
```json
{
  "jobExecutionId": 1,
  "status": "STARTED",
  "exitCode": "UNKNOWN",
  "startTime": "2026-09-05 10:42:22",
  "endTime": null,
  "lidos": 300,
  "gravados": 200,
  "pulados": 0,
  "filtrados": 0
}
```

**Response `200 OK` — concluído:**
```json
{
  "jobExecutionId": 1,
  "status": "COMPLETED",
  "exitCode": "COMPLETED",
  "startTime": "2026-09-05 10:42:22",
  "endTime": "2026-09-05 10:42:23",
  "lidos": 1118,
  "gravados": 1118,
  "pulados": 0,
  "filtrados": 0
}
```

**Response `200 OK` — falhou:**
```json
{
  "jobExecutionId": 1,
  "status": "FAILED",
  "exitCode": "FAILED",
  "erro": "mensagem do erro"
}
```

---

### Códigos de Resposta

| Código | Significado |
|---|---|
| `200 OK` | Sucesso |
| `201 Created` | Recurso criado |
| `204 No Content` | Operação sem retorno |
| `400 Bad Request` | Dados inválidos |
| `401 Unauthorized` | Token ausente, inválido ou expirado |
| `403 Forbidden` | Role insuficiente |
| `404 Not Found` | Recurso não encontrado |
| `409 Conflict` | Username ou e-mail duplicado |
| `429 Too Many Requests` | Rate limit atingido |
| `500 Internal Server Error` | Erro inesperado |

---

## 15. HATEOAS

A API implementa HATEOAS seguindo o nível 3 do modelo de maturidade de Richardson.

### Links Gerados

| Recurso | Rel | Descrição |
|---|---|---|
| `UserResponse` | `self` | Link para o próprio usuário |
| `UserResponse` | `users` | Link para a coleção |
| `CnaeResponse` | `self` | Link para o próprio CNAE |
| `CnaeResponse` | `cnae` | Link para a coleção |
| Paginação | `first`, `prev`, `self`, `next`, `last` | Navegação entre páginas |

### Exemplo de Resposta Paginada

```json
{
  "_embedded": { "cnaeResponseList": [ { "id": 1, "denominacao": "Cultivo de arroz",
    "_links": {
      "self": { "href": "http://localhost:8080/api/v1/cnae/1" },
      "cnae": { "href": "http://localhost:8080/api/v1/cnae" }
    }
  }]},
  "_links": {
    "first": { "href": "http://localhost:8080/api/v1/cnae?page=0&size=20" },
    "self":  { "href": "http://localhost:8080/api/v1/cnae?page=0&size=20" },
    "next":  { "href": "http://localhost:8080/api/v1/cnae?page=1&size=20" },
    "last":  { "href": "http://localhost:8080/api/v1/cnae?page=9&size=20" }
  },
  "page": { "size": 20, "totalElements": 1318, "totalPages": 66, "number": 0 }
}
```

---

## 16. Swagger / OpenAPI

### Acesso

| Interface | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| JSON OpenAPI | http://localhost:8080/v3/api-docs |

### Como Autenticar no Swagger

1. Execute `POST /api/v1/auth/login` com as credenciais do admin
2. Copie o valor do campo `token` da resposta
3. Clique em **Authorize** (cadeado) no topo da página
4. Informe: `Bearer eyJhbGci...`
5. Clique em **Authorize** — todos os endpoints enviarão o token automaticamente

### Organização por Tags

| Tag | Endpoints |
|---|---|
| **Autenticação** | `POST /login`, `POST /refresh`, `POST /logout` |
| **Usuários** | CRUD completo |
| **CNAE** | Listagem, busca por ID, pesquisa |
| **Batch** | Disparo do job |

---

## 17. Testes

### Estratégia

44+ casos de teste cobrindo todas as camadas:

- **Unitários** — `@ExtendWith(MockitoExtension.class)`, sem contexto Spring
- **Contexto** — `@SpringBootTest` com `@TestPropertySource` e mocks de infraestrutura

Ambiente de teste: **H2 em memória**, Liquibase desabilitado, Vault desabilitado via `bootstrap.yml`.

### Cobertura por Classe

| Classe de Teste | Tipo | Casos | O que valida |
|---|---|---|---|
| `CnaeItemProcessorTest` | Unitário | 4 | Mapeamento, filtro nulo, publicação MQ tolerante a falha |
| `CnaeItemWriterTest` | Unitário | 2 | `saveAll()`, chunk vazio |
| `JwtServiceTest` | Unitário | 8 | Access token, refresh token, tipo errado, expirado, usuário diferente |
| `AuthServiceTest` | Unitário | 5 | Login, credenciais inválidas, refresh rotation, token revogado, logout |
| `UserServiceTest` | Unitário | 5 | Criação, 409 username, 409 email, 404, deleção |
| `CnaeServiceTest` | Unitário | 4 | Paginação, findById, search, 404 |
| `BatchServiceTest` | Unitário | 2 | Disparo OK, falha no launcher |
| `CnaeEventConsumerTest` | Unitário | 3 | Mensagem válida, DLQ, campos nulos |
| `AuthControllerTest` | Unitário | 6 | login 200, login 401, refresh 200, refresh 401, logout 204, logout falha |
| `BatchControllerTest` | Unitário | 7 | run 200, run falha, run chamada única, status COMPLETED, status FAILED, status 404 |
| `UserControllerTest` | Unitário | 6 | sort inválido → fallback id, sort válido, direction desc, 200, create 201, delete 204 |
| `CnaeControllerTest` | Unitário | 5 | sort inválido → fallback id, sort válido desc, 200, search sort inválido, search 200 |
| `GmontinnyApplicationTests` | Contexto | 1 | Spring context carrega sem erros |

### Executar os Testes

```bash
./gradlew test

# Com relatório detalhado
./gradlew test --info

# Relatório HTML: build/reports/tests/test/index.html
```

### Configuração do Ambiente de Teste

```yaml
# src/test/resources/bootstrap.yml  — desabilita Vault antes do contexto subir
spring:
  cloud:
    vault:
      enabled: false
  config:
    import: ""

# src/test/resources/application.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
  liquibase:
    enabled: false
  batch:
    jdbc:
      initialize-schema: always
  rabbitmq:
    listener:
      simple:
        auto-startup: false   # não conecta ao RabbitMQ nos testes
  main:
    allow-bean-definition-overriding: true
app:
  jwt:
    secret: 3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b
  rate-limit:
    login-capacity: 100
    api-capacity: 1000
```

### Isolamento de Infraestrutura nos Testes

O `GmontinnyApplicationTests` usa `@TestPropertySource` para sobrescrever propriedades e `@MockitoBean` para isolar dependências externas:

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.vault.enabled=false",
    "spring.config.import=",
    "spring.rabbitmq.listener.simple.auto-startup=false",
    ...
})
class GmontinnyApplicationTests {
    @MockitoBean ConnectionFactory connectionFactory;      // RabbitMQ
    @MockitoBean RabbitTemplate rabbitTemplate;
    @MockitoBean JobLauncher asyncJobLauncher;             // Spring Batch
    @MockitoBean ProxyManager<String> rateLimitProxyManager; // Redis/Bucket4j
}
```

---

## 18. Configuração de Ambiente

### Arquivo `.env`

```properties
# PostgreSQL
POSTGRES_USER=gmontinny
POSTGRES_PASSWORD=Gmontinny2026
POSTGRES_DB=gmontinny
POSTGRES_PORT=5432

# RabbitMQ
RABBITMQ_DEFAULT_USER=gmontinny
RABBITMQ_DEFAULT_PASS=Gmontinny2026
RABBITMQ_PORT=5672
RABBITMQ_MANAGEMENT_PORT=15672

# Redis
REDIS_PASSWORD=Redis2026
REDIS_PORT=6379

# Vault (modo dev — substituir em produção)
VAULT_DEV_TOKEN=gmontinny-vault-token
VAULT_PORT=8200
VAULT_ADDR=http://localhost:8200
```

### Variáveis da Aplicação

| Variável | Padrão | Descrição |
|---|---|---|
| `JWT_SECRET` | *(hex 64 chars)* | Chave JWT — preferir via Vault |
| `app.jwt.expiration-ms` | `900000` | Expiração do access token (15 min) |
| `app.jwt.refresh-expiration-ms` | `604800000` | Expiração do refresh token (7 dias) |
| `app.batch.chunk-size` | `100` | Registros por transação |
| `app.batch.skip-limit` | `10` | Máximo de itens ignorados |
| `app.batch.thread-pool-size` | `4` | Workers paralelos |
| `app.rate-limit.login-capacity` | `20` | Tokens por janela no login |
| `app.rate-limit.api-capacity` | `200` | Tokens por janela nas demais rotas |

### Gerar JWT_SECRET seguro

```bash
# Linux/macOS
openssl rand -hex 64

# PowerShell
[System.Convert]::ToBase64String(
  [System.Security.Cryptography.RandomNumberGenerator]::GetBytes(64))
```

---

## 19. Como Executar

### Pré-requisitos

- Java 25 (JDK)
- Docker e Docker Compose

### Scripts de Início Rápido

Use os scripts na raiz do projeto — eles automatizam todos os passos:

```bat
:: Windows
start.bat
```

```bash
# Linux / macOS
chmod +x start.sh && ./start.sh
```

O script executa na ordem:
1. Verifica se o Docker está rodando
2. Sobe os 4 containers (`docker-compose up -d`)
3. Aguarda o Vault ficar saudável
4. Popula os secrets no Vault automaticamente
5. Inicia a aplicação com `./gradlew bootRun`

### Passo a Passo Manual

**1. Clone o repositório**
```bash
git clone https://github.com/seu-usuario/gmontinny.git
cd gmontinny
```

**2. Suba a infraestrutura**
```bash
docker-compose up -d
```

**3. Popule os secrets no Vault**
```bash
docker exec vault_gmontinny vault kv put secret/gmontinny \
  jwt.secret="3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b" \
  db.password="Gmontinny2026" \
  rabbitmq.password="Gmontinny2026" \
  redis.password="Redis2026"
```

> ⚠️ O Vault em modo dev perde os secrets ao reiniciar o container. Sempre repopule após `docker-compose down` + `up`.

**4. Execute a aplicação**
```bash
# Windows
gradlew.bat bootRun

# Linux/macOS
./gradlew bootRun
```

**5. Acesse os serviços**

| Serviço | URL |
|---|---|
| API / Swagger UI | http://localhost:8080/swagger-ui.html |
| RabbitMQ Management | http://localhost:15672 |
| Vault UI | http://localhost:8200/ui |

### Executar os Testes

```bash
./gradlew test
```

---

## 20. Docker

### Serviços do docker-compose.yml

| Serviço | Imagem | Porta(s) | Healthcheck |
|---|---|---|---|
| `postgres` | `postgres:16-alpine` | `5432` | `pg_isready` |
| `rabbitmq` | `rabbitmq:3-management-alpine` | `5672`, `15672` | `rabbitmq-diagnostics ping` |
| `redis` | `redis:7-alpine` | `6379` | `redis-cli ping` |
| `vault` | `hashicorp/vault:1.17` | `8200` | `vault status` |

Todos os serviços têm `restart: unless-stopped` e healthcheck configurado.

### Dockerfile (Multi-stage Build)

```
Stage 1 (builder): eclipse-temurin:25-jdk
  └── ./gradlew bootJar → app.jar

Stage 2 (runtime): eclipse-temurin:25-jre
  └── Copia apenas o JAR
  └── Usuário não-root: spring:spring
  └── EXPOSE 8080
```

### Build e Execução

```bash
# Build da imagem
docker build -t gmontinny:latest .

# Executar
docker run -d \
  --name gmontinny-app \
  -e POSTGRES_USER=gmontinny \
  -e POSTGRES_PASSWORD=Gmontinny2026 \
  -e POSTGRES_DB=gmontinny \
  -e RABBITMQ_DEFAULT_USER=gmontinny \
  -e RABBITMQ_DEFAULT_PASS=Gmontinny2026 \
  -e REDIS_PASSWORD=Redis2026 \
  -e VAULT_DEV_TOKEN=gmontinny-vault-token \
  -p 8080:8080 \
  gmontinny:latest

# Comandos úteis
docker-compose up -d        # subir infraestrutura
docker-compose logs -f      # logs em tempo real
docker-compose down         # parar
docker-compose down -v      # parar + remover volumes
```

---

## 21. Boas Práticas Aplicadas

### Segurança

| Prática | Implementação |
|---|---|
| Secrets centralizados | HashiCorp Vault — nenhum secret hardcoded |
| Tokens com tipo explícito | Claim `type: access/refresh` — cross-use impossível |
| Refresh token rotation | Cada uso invalida o anterior |
| Rate limiting distribuído | Bucket4j + Redis — protege contra força bruta |
| Senhas com hash forte | BCrypt strength 12 |
| Dupla verificação de acesso | `SecurityFilterChain` + `@PreAuthorize` |
| Container não-root | `USER spring:spring` no Dockerfile |
| Sessão stateless | JWT sem estado no servidor |

### Arquitetura

| Prática | Implementação |
|---|---|
| Separação de responsabilidades | Controller → Service → Repository |
| DTOs como contrato | `record` para requests, `RepresentationModel` para responses |
| Entidades isoladas da API | MapStruct separa entidade do DTO |
| Configuração externalizada | `application.yml` + Vault + env vars |
| Versionamento de schema | Liquibase com changelogs numerados |
| Tratamento de erros padronizado | `GlobalExceptionHandler` + `ProblemDetail` (RFC 9457) |

### Processamento em Lote

| Prática | Implementação |
|---|---|
| Execução assíncrona | `AsyncJobLauncher` não bloqueia HTTP |
| Processamento paralelo | `ThreadPoolTaskExecutor` configurável |
| Tolerância a falhas | `faultTolerant()` + retry + skip |
| Capacidade de reinício | `JobRepository` + `RunIdIncrementer` |
| Agendamento automático | `@EnableScheduling` + `@Scheduled` |
| Observabilidade | `StepExecutionListener` com métricas completas |

### Mensageria

| Prática | Implementação |
|---|---|
| Dead Letter Queue | Mensagens com falha não são perdidas |
| Retry com backoff exponencial | 2s → 4s → 8s antes de ir para DLQ |
| Filas duráveis | Sobrevivem a restart do RabbitMQ |
| Consumers paralelos | `concurrency: 2`, `max-concurrency: 5` |

---

## 22. Fluxo Completo de Uso

### 1. Subir a infraestrutura

```bash
docker-compose up -d
docker exec vault_gmontinny vault kv put secret/gmontinny \
  jwt.secret="3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b" \
  db.password="Gmontinny2026" rabbitmq.password="Gmontinny2026" redis.password="Redis2026"
./gradlew bootRun
```

### 2. Login — obter tokens

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@2026"}'

# Resposta
{
  "token": "<ACCESS_TOKEN>",
  "refreshToken": "<REFRESH_TOKEN>",
  "type": "Bearer",
  "username": "admin",
  "expiresIn": 900000
}
```

### 3. Usar o access token (válido por 15 min)

```bash
curl http://localhost:8080/api/v1/cnae?page=0&size=5 \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

### 4. Renovar o access token (quando expirar)

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<REFRESH_TOKEN>"}'

# Retorna novo par de tokens (rotation)
```

### 5. Disparar a importação CNAE e acompanhar

```bash
# Disparar
curl -X POST http://localhost:8080/api/v1/batch/cnae/run \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
# { "message": "Job iniciado com id: 1 | Status: STARTING" }

# Acompanhar status (use o id retornado acima)
curl http://localhost:8080/api/v1/batch/cnae/status/1 \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
# STARTING → STARTED → COMPLETED (lidos=1118, gravados=1118)
```

### 6. Testar o rate limiting

```bash
# Após 20 tentativas de login em 60 segundos:
# HTTP 429 Too Many Requests
# Retry-After: 45
# X-Rate-Limit-Remaining: 0
```

### 7. Logout

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
# HTTP 204 No Content — todos os refresh tokens revogados
```

### 8. Verificar secrets no Vault

```bash
# Vault UI: http://localhost:8200/ui (token: gmontinny-vault-token)
# CLI:
docker exec vault_gmontinny vault kv get secret/gmontinny
```

### 9. Verificar metadados do Batch

```sql
SELECT step_name, read_count, write_count, skip_count, status
FROM BATCH_STEP_EXECUTION ORDER BY create_time DESC;
```

### 10. Ativar execução agendada (opcional)

Descomente em `BatchService.java`:
```java
@Scheduled(cron = "${app.batch.cron:0 0 2 * * *}")
public void runScheduled() { runCnaeImport(); }
```

---

## 23. Compatibilidade com Spring Boot 4.x / Spring 7.x

Este projeto usa Spring Boot **4.1.1** com Java **25** — versões de ponta que introduziram quebras de compatibilidade significativas em relação ao ecossistema anterior. As adaptações abaixo foram identificadas inspecionando os JARs diretamente.

### Spring Batch 6.x — Pacotes Renomeados

O Spring Batch 6.x reorganizou completamente seus pacotes. Os módulos `spring-batch-core` e `spring-batch-infrastructure` **não são exportados como API transitíva** pelo `spring-boot-starter-batch` 4.x e precisam ser declarados explicitamente:

```groovy
implementation 'org.springframework.batch:spring-batch-core'
implementation 'org.springframework.batch:spring-batch-infrastructure'
```

| Pacote antigo (5.x) | Pacote novo (6.x) |
|---|---|
| `org.springframework.batch.item.ItemReader` | `org.springframework.batch.infrastructure.item.ItemReader` |
| `org.springframework.batch.item.ItemWriter` | `org.springframework.batch.infrastructure.item.ItemWriter` |
| `org.springframework.batch.item.ItemProcessor` | `org.springframework.batch.infrastructure.item.ItemProcessor` |
| `org.springframework.batch.item.Chunk` | `org.springframework.batch.infrastructure.item.Chunk` |
| `org.springframework.batch.core.Job` | `org.springframework.batch.core.job.Job` |
| `org.springframework.batch.core.Step` | `org.springframework.batch.core.step.Step` |
| `org.springframework.batch.core.StepExecution` | `org.springframework.batch.core.step.StepExecution` |
| `org.springframework.batch.core.StepExecutionListener` | `org.springframework.batch.core.listener.StepExecutionListener` |
| `org.springframework.batch.core.JobExecution` | `org.springframework.batch.core.job.JobExecution` |
| `org.springframework.batch.core.JobParameters` | `org.springframework.batch.core.job.parameters.JobParameters` |
| `org.springframework.batch.core.JobParametersBuilder` | `org.springframework.batch.core.job.parameters.JobParametersBuilder` |
| `org.springframework.batch.core.explore.JobExplorer` | `org.springframework.batch.core.repository.explore.JobExplorer` |
| `org.springframework.batch.item.ItemStream` | `org.springframework.batch.infrastructure.item.ItemStream` |
| `org.springframework.batch.item.ExecutionContext` | `org.springframework.batch.infrastructure.item.ExecutionContext` |

### Spring AMQP 4.x — API Alterada

| Antes (3.x) | Depois (4.x) |
|---|---|
| `Jackson2JsonMessageConverter` | `JacksonJsonMessageConverter` |
| `RetryInterceptorBuilder.stateless().maxAttempts(3)` | `.maxRetries(3)` |
| Retorno `RetryOperationsInterceptor` | `StatelessRetryOperationsInterceptor` |

### Spring Security 7.x — API Alterada

```java
// Antes
new DaoAuthenticationProvider();
provider.setUserDetailsService(userDetailsService);

// Depois
new DaoAuthenticationProvider(userDetailsService);
```

### Spring Boot 4.x — Testes

| Antes (3.x) | Depois (4.x) |
|---|---|
| `@MockBean` | `@MockitoBean` |
| `@WebMvcTest` (`o.s.b.test.autoconfigure.web.servlet`) | Removido — usar `@SpringBootTest` unitário |
| `@AutoConfigureMockMvc` (`o.s.b.test.autoconfigure.web.servlet`) | `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` |
| `spring-boot-starter-test` inclui MockMvc | Adicionar `spring-boot-starter-webmvc-test` |

### Spring Cloud Vault — Configuração

O `spring-cloud-starter-vault-config` deve ser gerenciado pelo **BOM do Spring Cloud** (não com versão explícita) para garantir compatibilidade:

```groovy
ext { set('springCloudVersion', "2025.1.2") }

dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-vault-config' // sem versão
}

dependencyManagement {
    imports { mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}" }
}
```

A configuração do Vault deve ficar em `bootstrap.yaml` (não em `application.yml`) para ser carregada antes do contexto Spring. Nos testes, `bootstrap.yml` desabilita o Vault com `spring.cloud.vault.enabled: false`.

### Jackson 3.x (tools.jackson)

O Spring Boot 4.x usa **Jackson 3.x** cujo groupId mudou de `com.fasterxml.jackson` para `tools.jackson`. Em testes que precisam de `ObjectMapper` diretamente:

```java
// Antes
import com.fasterxml.jackson.databind.ObjectMapper;

// Depois
import tools.jackson.databind.ObjectMapper;
```

### RateLimitFilter — Refatoração para Testabilidade

O `ProxyManager<String>` do Bucket4j foi extraído para um `@Bean` dedicado em `RedisConfig`, permitindo mock nos testes sem dependência direta do `LettuceConnectionFactory` no construtor do filtro:

```java
// RedisConfig.java
@Bean
public ProxyManager<String> rateLimitProxyManager(LettuceConnectionFactory factory) {
    RedisClient client = (RedisClient) factory.getNativeClient();
    StatefulRedisConnection<String, byte[]> conn =
            client.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    return LettuceBasedProxyManager.builderFor(conn).build();
}

// RateLimitFilter.java — recebe por injeção
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private final ProxyManager<String> rateLimitProxyManager;
}
```

---

## Licença

Este projeto está licenciado sob a [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

---

*Desenvolvido com Java 25 + Spring Boot 4.1.1*
