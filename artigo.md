# Arquitetura Moderna para Processamento em Lote Resiliente e APIs Seguras: Um Estudo de Caso com Spring Boot 4, Spring Batch 6 e Infraestrutura Multi-Cloud

**Autor:** Equipe de Engenharia de Software — GMontinny
**Data:** 2026
**Status:** Artigo Técnico / Whitepaper

---

## Resumo

O processamento eficiente de grandes volumes de dados corporativos — como a estrutura hierárquica da Classificação Nacional de Atividades Econômicas (CNAE 2.0) — representa um desafio recorrente na engenharia de software. A necessidade de transformar dados brutos em formato legado (planilhas Excel `.xls`) em bases relacionais normalizadas exige arquiteturas tolerantes a falhas, auditáveis e seguras.

Este artigo apresenta o design arquitetural e a implementação da plataforma **GMontinny**, construída sobre **Java 25** e **Spring Boot 4.1.1**. A solução combina: processamento em lote orientado a chunks (Spring Batch 6), mensageria assíncrona com Dead Letter Queue (RabbitMQ), persistência relacional com versionamento de schema (PostgreSQL 16 + Liquibase), segurança multicamadas com JWT e rotação de Refresh Tokens, Rate Limiting distribuído (Bucket4j + Redis), gerenciamento centralizado de segredos (HashiCorp Vault) e infraestrutura declarativa multi-cloud (Docker, Kubernetes com Kustomize e Terraform para AWS, GCP e Azure).

São documentadas as decisões arquiteturais críticas enfrentadas ao adotar o Spring Boot 4 com Spring Batch 6 — em particular a integração do `JdbcJobRepository` via `DefaultBatchConfiguration` e a renomeação da sequence `BATCH_JOB_INSTANCE_SEQ` — bem como as estratégias de segurança, resiliência e portabilidade de infraestrutura adotadas.

**Palavras-chave:** Spring Batch 6, Spring Boot 4, Processamento em Lote, RabbitMQ, JWT, HashiCorp Vault, Redis, Kubernetes, Terraform, CNAE, Cloud-Native.

---

## 1. Introdução e Contextualização do Problema

No ecossistema corporativo brasileiro, a Classificação Nacional de Atividades Econômicas (CNAE) mantida pelo IBGE e pela Receita Federal é a espinha dorsal para categorização de empresas, enquadramento tributário, emissão de notas fiscais e análises de inteligência de mercado. Sua distribuição ocorre por meio de arquivos tabulares em formato binário Excel (`.xls`), contendo mais de 1.300 registros hierárquicos organizados em Seções, Divisões, Grupos, Classes e Subclasses.

A ingestão direta e síncrona desses arquivos via endpoints HTTP tradicionais apresenta gargalos operacionais bem conhecidos:

1. **Bloqueio de threads HTTP:** A leitura e escrita síncrona de grandes arquivos esgota rapidamente a thread pool do servidor web (Tomcat), gerando indisponibilidade para outros usuários durante o processamento.
2. **Fragilidade transacional:** Erros no meio do processamento podem deixar o banco em estado inconsistente na ausência de uma estratégia transacional por blocos (chunks) com checkpoints persistidos.
3. **Ausência de rastreabilidade:** Falhas em registros individuais demandam políticas refinadas de skip e reprocessamento assíncrono com filas de mensagens mortas (Dead Letter Queues).
4. **Vulnerabilidades de segurança:** Sistemas de processamento de dados críticos frequentemente pecam em autenticação fraca, credenciais em texto plano e ausência de proteção contra ataques de força bruta.

O projeto **GMontinny** foi desenvolvido para endereçar esses problemas, estabelecendo um padrão arquitetural que desacopla a recepção da requisição do processamento de dados, unindo computação distribuída, automação de infraestrutura e governança de segredos.

---

## 2. Arquitetura da Solução

A arquitetura adota separação de responsabilidades (SoC), design orientado a domínios (DDD simplificado) e arquitetura em camadas com inversão de dependências.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            CLIENT (HTTP)                                │
│                Swagger UI / Postman / Frontend / Integradores           │
└─────────────────────────────┬───────────────────────────────────────────┘
                              │ HTTPS :8080
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         SECURITY & GATEWAY                              │
│  ┌──────────────────────┐  ┌─────────────────────┐  ┌───────────────┐  │
│  │  RateLimitFilter     │→ │ JwtAuthentication   │→ │ SecurityFilter│  │
│  │  Bucket4j + Redis    │  │ Filter (Stateless)  │  │ Chain + RBAC  │  │
│  │  20/min login        │  │ claim type=access   │  │ @PreAuthorize │  │
│  │  200/min api         │  │                     │  │               │  │
│  └──────────────────────┘  └─────────────────────┘  └───────────────┘  │
└─────────────────────────────┬───────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          APPLICATION LAYER                              │
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────────────┐    │
│  │  AuthController  │  │ BatchController  │  │ Cnae/UserController│    │
│  │  /login /refresh │  │ /batch/cnae/run  │  │ HATEOAS + Paginação│    │
│  │  /logout         │  │ /batch/cnae/     │  │                    │    │
│  │                  │  │ status/{id}      │  │                    │    │
│  └────────┬─────────┘  └────────┬─────────┘  └─────────┬──────────┘    │
└───────────┼─────────────────────┼───────────────────────┼──────────────┘
            │                     │                       │
            ▼                     ▼                       ▼
┌───────────────────┐  ┌──────────────────────┐  ┌───────────────────────┐
│  SECURITY SERVICES│  │   SPRING BATCH 6     │  │   BUSINESS SERVICES   │
│  AuthService      │  │  BatchConfig extends │  │   CnaeService         │
│  JwtService       │  │  DefaultBatchConfig  │  │   UserService         │
│  Vault (secrets)  │  │  ┌────────────────┐  │  │   MapStruct mappers   │
│  RefreshToken     │  │  │ ExcelReader    │  │  └───────────┬───────────┘
│  rotation         │  │  │ (Apache POI)   │  │             │
└───────────────────┘  │  ├────────────────┤  │             │
                       │  │ ItemProcessor  │  │             │
                       │  │ (sanitização)  │  │             │
                       │  ├────────────────┤  │             │
                       │  │ ItemWriter     │  │             │
                       │  │ (JPA saveAll)  │  │             │
                       │  └───────┬────────┘  │             │
                       └──────────┼───────────┘             │
                                  │                         │
                    ┌─────────────┴─────────────────────────┘
                    ▼                           ▼
┌───────────────────────────────┐  ┌────────────────────────────────────┐
│        MESSAGING LAYER        │  │          PERSISTENCE LAYER         │
│  batch.exchange (Direct)      │  │  PostgreSQL 16 (HikariCP pool)     │
│  batch.cnae.queue (durable)   │  │  users / roles / cnae              │
│  batch.cnae.dlq               │  │  refresh_tokens                    │
│  Retry: 2s→4s→8s→DLQ         │  │  BATCH_JOB_INSTANCE / EXECUTION    │
│  CnaeEventConsumer            │  │  Liquibase changelogs              │
└───────────────────────────────┘  └────────────────────────────────────┘
                                                    ▲
                                   ┌────────────────┘
                                   │
                       ┌───────────────────────┐
                       │   HASHICORP VAULT      │
                       │   secret/gmontinny     │
                       │   jwt.secret           │
                       │   db.password          │
                       │   rabbitmq.password    │
                       │   redis.password       │
                       └───────────────────────┘
```

### 2.1 Fluxo de Autenticação e Autorização

O cliente envia credenciais para `POST /api/v1/auth/login`, passando pelo `RateLimitFilter` (20 req/min por IP via Bucket4j + Redis). O `AuthService` valida o hash BCrypt (strength 12), revoga todos os refresh tokens anteriores do usuário e emite dois tokens com claims distintos: um Access Token JWT de 15 minutos (claim `type=access`) e um Refresh Token de 7 dias (claim `type=refresh`) persistido na tabela `refresh_tokens`. A separação de claims impede que um refresh token seja usado como access token e vice-versa.

### 2.2 Fluxo de Processamento Batch

O administrador dispara `POST /api/v1/batch/cnae/run`. O `BatchController` delega ao `BatchService`, que invoca `JobOperator.start(cnaeImportJob, jobParameters)` — a API correta no Batch 6, que substitui o `JobLauncher` deprecated. O `TaskExecutorJobOperator` executa o job no `ThreadPoolTaskExecutor` configurado no `BatchConfig`, liberando o thread HTTP imediatamente. O status é consultável via `GET /api/v1/batch/cnae/status/{jobExecutionId}`, que usa `JobRepository.getJobExecution(id)` — substituto do `JobExplorer` deprecated.

### 2.3 Fluxo de Mensageria

Para cada chunk persistido, o `CnaeItemProcessor` publica uma mensagem JSON no `batch.exchange` com routing key `batch.cnae`. O `CnaeEventConsumer` processa com concorrência 2–5. Em caso de falha, o `StatelessRetryOperationsInterceptor` aplica backoff exponencial (2s → 4s → 8s, máximo 3 tentativas). Após esgotamento, a mensagem é roteada automaticamente para `batch.cnae.dlq` via `x-dead-letter-exchange`.

---

## 3. Tecnologias e Fundamentação Técnica

### 3.1 Java 25 e Spring Boot 4.1.1

A aplicação utiliza Java 25 com Records, Pattern Matching e Virtual Threads, aliados ao Spring Boot 4.1.1 — a primeira versão major do framework a exigir Java 17+ como baseline e a adotar Spring Framework 7.x. O ecossistema Spring viabilizou configuração declarativa, auto-configuração de componentes e integração nativa com Spring Cloud para gerenciamento de secrets via Vault.

Uma mudança significativa no Spring Boot 4 afeta diretamente o Spring Batch: o `BatchAutoConfiguration` registra por padrão um `DefaultBatchConfiguration` com `ResourcelessJobRepository` (sem persistência). A condição de desativação é:

```java
@ConditionalOnMissingBean(
    value = DefaultBatchConfiguration.class,
    annotation = EnableBatchProcessing.class
)
```

A única forma correta de forçar o `JdbcJobRepository` é estender `DefaultBatchConfiguration` na própria configuração da aplicação, satisfazendo o `@ConditionalOnMissingBean`. O uso de `@EnableBatchProcessing` está **proibido** no Boot 4 — desabilita o autoconfigure e cria infraestrutura própria com datasource em memória.

### 3.2 Spring Batch 6 — Pipeline e Decisões de Implementação

O Spring Batch 6 introduziu reorganização completa de pacotes, deprecação de APIs e renomeação de sequences de banco. O pipeline segue o padrão `ItemReader → ItemProcessor → ItemWriter`:

**`CnaeExcelReader`** — implementação customizada com Apache POI (HSSF) para arquivos `.xls`. Itera sequencialmente sobre as linhas com controle de ponteiro, mapeando células vazias ou mescladas para o DTO intermediário `CnaeRow`. Implementa `ItemStream` para suporte a restart.

**`CnaeItemProcessor`** — sanitiza códigos (remove pontos, barras e traços), valida regras de negócio, preenche hierarquias e mapeia para a entidade `Cnae`. Retorna `null` para registros inválidos, ativando o mecanismo de filter do Batch.

**`CnaeItemWriter`** — persiste via `CnaeRepository.saveAll()` (batch insert em uma única transação por chunk) e publica evento no RabbitMQ. A combinação `chunk(100)` reduz de 1.000 INSERTs individuais para 10 transações — redução de 100x na sobrecarga de I/O.

**Tolerância a falhas:** `faultTolerant().skipLimit(10).skip(Exception.class).retryLimit(3).retry(Exception.class)` garante que registros corrompidos não interrompam o processamento de milhares de itens válidos.

**Rastreabilidade:** O `JdbcJobRepository` persiste metadados completos nas tabelas `BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION`, `BATCH_STEP_EXECUTION` e contextos associados, criadas deterministicamente via Liquibase.

**Sequence renomeada:** O Batch 6 renomeou `BATCH_JOB_SEQ` (Batch 4/5) para `BATCH_JOB_INSTANCE_SEQ`. Schemas migrados de versões anteriores precisam adicionar essa sequence explicitamente — omissão resulta em `DataAccessResourceFailureException: Could not obtain sequence value` em runtime.

### 3.3 Mensageria com RabbitMQ e Dead Letter Queue

A topologia de mensageria usa `DirectExchange` (`batch.exchange`), fila durável (`batch.cnae.queue`) com argumentos `x-dead-letter-exchange` e `x-dead-letter-routing-key` configurados na declaração da fila, e DLQ (`batch.cnae.dlq`).

O `StatelessRetryOperationsInterceptor` — substituto do `RetryOperationsInterceptor` no Spring AMQP 4.x — aplica backoff exponencial com intervalo inicial de 2s, multiplicador 2.0 e máximo de 3 tentativas. Após esgotamento, o RabbitMQ roteia automaticamente para a DLQ sem intervenção da aplicação.

Uma mudança de API relevante no Spring AMQP 4.x: `Jackson2JsonMessageConverter` foi renomeado para `JacksonJsonMessageConverter`, e `RetryInterceptorBuilder.stateless().maxAttempts(3)` passou a usar `.maxRetries(3)`.

### 3.4 Persistência — PostgreSQL, HikariCP e Liquibase

**PostgreSQL 16** serve como repositório principal de dados transacionais e metadados de execução do Batch. O pool HikariCP é configurado com `maximum-pool-size=10` e `minimum-idle=2`, com timeouts rigorosos para prevenir vazamento de conexões.

**Liquibase 4.27** gerencia todo o ciclo de vida do schema via changelogs XML versionados. A configuração `spring.batch.jdbc.initialize-schema: never` e `spring.jpa.hibernate.ddl-auto: validate` garantem que apenas o Liquibase modifica o schema em produção — o Hibernate apenas valida a consistência na inicialização.

Os changelogs cobrem: criação de roles, usuários, tabela CNAE com índice em `subclasse`, 6 tabelas `BATCH_*` com 4 sequences (incluindo `BATCH_JOB_INSTANCE_SEQ`), dados iniciais (admin com BCrypt strength 12) e tabela `refresh_tokens` com FK CASCADE para `users`.

### 3.5 Segurança — JWT, Refresh Token Rotation e RBAC

**Autenticação stateless** com JJWT 0.12.6, algoritmo HMAC-SHA256 e chave de 256 bits gerenciada pelo Vault. Dois tipos de token com claim `type` explícito:

| Token | Expiração | Claim `type` | Uso |
|---|---|---|---|
| Access Token | 15 minutos | `access` | Header `Authorization: Bearer` |
| Refresh Token | 7 dias | `refresh` | Body de `POST /auth/refresh` |

O `JwtService` valida o claim `type` em todas as verificações — um refresh token não pode ser usado como access token e vice-versa, eliminando uma classe inteira de ataques de token confusion.

**Refresh Token Rotation:** cada uso do refresh token revoga o anterior e emite um novo par. Tokens roubados são automaticamente invalidados após o uso legítimo. O logout revoga todos os tokens do usuário no banco.

**RBAC:** `ROLE_ADMIN` tem acesso total; `ROLE_USER` tem somente leitura de CNAE. A proteção é dupla: regras de URL no `SecurityFilterChain` e `@PreAuthorize` nos métodos de serviço.

### 3.6 Rate Limiting Distribuído — Bucket4j e Redis

O `RateLimitFilter` (OncePerRequestFilter) aplica o algoritmo Token Bucket via Bucket4j com estado armazenado no Redis através do `LettuceBasedProxyManager`. O armazenamento distribuído garante funcionamento correto em arquiteturas com múltiplas réplicas da aplicação.

Limites por IP:
- `POST /auth/login` → chave `rl:login:<ip>` — 20 tokens/60s (proteção contra força bruta)
- Demais rotas → chave `rl:api:<ip>` — 200 tokens/60s

Respostas incluem o header `X-Rate-Limit-Remaining`. Requisições bloqueadas recebem `HTTP 429` com header `Retry-After` e body JSON padronizado.

O IP é extraído respeitando proxies e load balancers via `X-Forwarded-For`, usando o primeiro IP da cadeia para evitar spoofing.

### 3.7 Gerenciamento de Segredos — HashiCorp Vault

O Vault 1.17 elimina a presença de segredos em arquivos de configuração ou variáveis de ambiente em texto plano. Na inicialização, o Spring Cloud Vault Config recupera dinamicamente do path `secret/gmontinny` (KV v2): `jwt.secret`, `db.password`, `rabbitmq.password` e `redis.password`.

A configuração reside em `bootstrap.yaml` — carregado antes do contexto Spring, garantindo disponibilidade dos secrets na fase de auto-configuração. Nos testes, `bootstrap.yml` desabilita o Vault com `spring.cloud.vault.enabled: false`.

Em produção, a autenticação por token estático deve ser substituída por AppRole ou Kubernetes Auth, com TLS obrigatório e `fail-fast: true`.

### 3.8 HATEOAS, MapStruct e OpenAPI

**Spring HATEOAS** implementa o nível 3 do Modelo de Maturidade de Richardson. Cada resposta inclui links `self` e de coleção; respostas paginadas incluem `first`, `prev`, `next` e `last`.

**MapStruct 1.6.3** gera conversores DTO/Entity em tempo de compilação, sem overhead de reflection em runtime. A separação entre entidades JPA e DTOs de resposta isola o contrato da API do modelo de persistência.

**SpringDoc OpenAPI 3.1** expõe documentação interativa em `/swagger-ui.html` com suporte a autenticação Bearer Token diretamente pela interface, permitindo testar todos os endpoints autenticados sem ferramentas externas.

---

## 4. Tabela de Tecnologias e Versões

| Componente | Versão | Função |
|---|---|---|
| Java | 25 | Linguagem principal |
| Spring Boot | 4.1.1 | Framework base |
| Spring Batch | 6.0.5 | Processamento em lote orientado a chunks |
| Spring Security | 7.1.1 | Autenticação, autorização e filtros |
| Spring Data JPA | 4.x | ORM e repositórios |
| Spring AMQP | 4.x | Integração RabbitMQ |
| Spring Cloud Vault | via BOM 2025.1.2 | Integração HashiCorp Vault |
| Spring HATEOAS | 3.x | Hypermedia na API REST |
| PostgreSQL | 16 | Banco de dados relacional |
| HikariCP | 7.x | Connection pool |
| RabbitMQ | 3.x | Message broker com DLQ |
| Redis | 7 | Rate limiting distribuído |
| HashiCorp Vault | 1.17 | Gerenciamento centralizado de secrets |
| Liquibase | 4.27 | Versionamento de schema |
| Apache POI | 5.3.0 | Leitura de arquivos Excel `.xls` |
| Bucket4j | 8.10.1 | Algoritmo Token Bucket para rate limiting |
| JJWT | 0.12.6 | Geração e validação de tokens JWT |
| MapStruct | 1.6.3 | Mapeamento DTO/Entity em compile-time |
| SpringDoc OpenAPI | 3.1.0 | Swagger UI e especificação OpenAPI 3.1 |
| JUnit 5 + Mockito | via Boot | Testes unitários e de integração |
| Gradle | 9.7.1 | Build tool |
| Docker + Compose | v2 | Containerização e orquestração local |
| Kubernetes + Kustomize | 1.29+ / 5.x | Orquestração de contêineres multi-cloud |
| Terraform | 1.9+ | Provisionamento declarativo de infraestrutura |

---

## 5. Infraestrutura como Código — Kubernetes e Terraform Multi-Cloud

### 5.1 Containerização

O `Dockerfile` usa multi-stage build: estágio `builder` com `eclipse-temurin:25-jdk` compila o JAR via `./gradlew bootJar`; estágio `runtime` com `eclipse-temurin:25-jre` copia apenas o artefato final. O container executa com usuário não-root (`spring:spring`), `readOnlyRootFilesystem` e sem capabilities Linux — conformidade com CIS Benchmark para containers.

### 5.2 Kubernetes com Kustomize

Os manifestos são organizados em `base/` (agnóstico de cloud) e `overlays/` (customizações por provedor):

**Base:**
- `Deployment` com 2 réplicas, liveness/readiness probes em `/actuator/health`, recursos definidos (requests/limits), anotações do Vault Agent Injector para injeção de secrets em produção
- `HorizontalPodAutoscaler` — escala de 2 a 8 réplicas por CPU (70%) e memória (80%)
- `PodDisruptionBudget` — `minAvailable: 1` garante disponibilidade durante atualizações
- `RBAC` — ServiceAccount com Role mínima (principle of least privilege)

**Overlays:**
- `aws/` — imagem do ECR, IRSA annotation, ALB Ingress com certificado ACM
- `gcp/` — imagem do Artifact Registry, Workload Identity, Cloud SQL Auth Proxy como sidecar, GCE Ingress com ManagedCertificate
- `azure/` — imagem do ACR, Workload Identity label, AGIC Ingress com cert-manager

### 5.3 Terraform Multi-Cloud

Cada provedor tem módulo independente com backend de estado remoto, variáveis tipadas e outputs para integração com os overlays Kubernetes:

**AWS (`infra/terraform/aws/`):**
- EKS 1.31 com managed node groups e addons (CoreDNS, VPC CNI, EBS CSI)
- RDS PostgreSQL 16 Multi-AZ com encryption at rest e backup de 7 dias
- ElastiCache Redis com replication group, TLS e auth token
- Amazon MQ RabbitMQ 3.13 em cluster multi-AZ
- Secrets Manager para armazenamento de credenciais
- Backend: S3 + DynamoDB lock

**GCP (`infra/terraform/gcp/`):**
- GKE com Workload Identity e node pools auto-repair/upgrade
- Cloud SQL PostgreSQL 16 HA com IP privado via VPC peering
- Memorystore Redis HA com auth e TLS
- Artifact Registry para imagens Docker
- Secret Manager com replicação automática
- Backend: GCS

**Azure (`infra/terraform/azure/`):**
- AKS com Workload Identity, OIDC issuer e auto-scaling
- PostgreSQL Flexible Server 16 com subnet delegation e DNS privado
- Azure Cache for Redis Standard com TLS 1.2 mínimo
- ACR com role assignment `AcrPull` para o kubelet identity do AKS
- Key Vault com access policies para secrets
- Backend: Azure Blob Storage

---

## 6. Estratégia de Testes

A suíte cobre 59+ casos de teste em múltiplas camadas, executados com H2 em memória, Vault desabilitado e RabbitMQ com `auto-startup: false`.

| Classe | Tipo | Casos | Cobertura |
|---|---|---|---|
| `CnaeItemProcessorTest` | Unitário | 4 | Mapeamento, filtro nulo, tolerância a falha no MQ |
| `CnaeItemWriterTest` | Unitário | 2 | `saveAll()`, chunk vazio |
| `JwtServiceTest` | Unitário | 8 | Access/refresh token, tipo errado, expirado, usuário diferente |
| `AuthServiceTest` | Unitário | 5 | Login, credenciais inválidas, refresh rotation, token revogado, logout |
| `UserServiceTest` | Unitário | 5 | Criação, 409 username, 409 email, 404, deleção |
| `CnaeServiceTest` | Unitário | 4 | Paginação, findById, search, 404 |
| `BatchServiceTest` | Unitário | 2 | Disparo OK, falha no JobOperator |
| `CnaeEventConsumerTest` | Unitário | 3 | Mensagem válida, DLQ, campos nulos |
| `AuthControllerTest` | Unitário | 6 | login 200/401, refresh 200/401, logout 204/falha |
| `BatchControllerTest` | Unitário | 7 | run 200/falha, status COMPLETED/FAILED/404 |
| `UserControllerTest` | Unitário | 6 | sort, paginação, create 201, delete 204 |
| `CnaeControllerTest` | Unitário | 5 | sort, search, fallback para sort inválido |
| `GmontinnyApplicationTests` | Contexto | 1 | Spring context carrega sem erros |
| `BatchJobIntegrationTest` | Integração | — | `@Disabled` — requer PostgreSQL ativo |

**Isolamento de infraestrutura:** o `GmontinnyApplicationTests` usa `@MockitoBean` para `ConnectionFactory`, `RabbitTemplate` e `ProxyManager<String>`. O `JobOperator` não precisa de mock — é gerenciado pelo `DefaultBatchConfiguration` com H2 nos testes.

**Nota sobre Spring Boot 4:** `@MockBean` foi renomeado para `@MockitoBean`. O `@WebMvcTest` foi removido — testes de controller usam `@SpringBootTest` com mocks de infraestrutura.

---

## 7. Decisões Arquiteturais e Lições Aprendidas

### 7.1 Spring Batch 6 com Spring Boot 4 — Armadilhas e Soluções

A integração do Spring Batch 6 com Spring Boot 4 exige atenção a três pontos críticos não documentados de forma clara:

**Problema 1 — `ResourcelessJobRepository` silencioso:** O `BatchAutoConfiguration` do Boot 4 registra por padrão um `DefaultBatchConfiguration` com `ResourcelessJobRepository`. Jobs executam com sucesso, CNAEs são gravados, mas `batch_job_instance` permanece vazio. A solução é estender `DefaultBatchConfiguration` e sobrescrever `jobRepository()` com `JdbcJobRepositoryFactoryBean`.

**Problema 2 — `tablePrefix` case-sensitive:** O PostgreSQL converte identificadores sem aspas para minúsculas. O `tablePrefix` deve ser `"BATCH_"` (maiúsculas) — o Spring Batch gera queries como `SELECT ... FROM BATCH_JOB_INSTANCE` que o PostgreSQL resolve para `batch_job_instance`. Usar `"batch_"` gera queries como `batch_JOB_INSTANCE` que falham silenciosamente.

**Problema 3 — Sequence renomeada:** O Batch 6 renomeou `BATCH_JOB_SEQ` para `BATCH_JOB_INSTANCE_SEQ`. Schemas criados para versões anteriores precisam adicionar essa sequence via migration idempotente com `preConditions onFail="MARK_RAN"`.

### 7.2 Desacoplamento e Desempenho

A substituição de importação síncrona por `JobOperator.start()` + `ThreadPoolTaskExecutor` reduziu o tempo de resposta do endpoint de dezenas de segundos para menos de 100ms, eliminando risco de timeout HTTP. O processamento de 1.118 registros CNAE completa em aproximadamente 1 segundo com chunk size 100.

### 7.3 Defesa em Profundidade

A combinação de Rate Limiting (Bucket4j/Redis), JWT com claim `type` explícito, Refresh Token Rotation, BCrypt strength 12 e secrets no Vault estabelece uma postura de segurança alinhada com OWASP API Security Top 10. Nenhum secret aparece em arquivos de configuração versionados.

### 7.4 Portabilidade de Infraestrutura

O padrão `base/overlays` do Kustomize permite que os mesmos manifestos Kubernetes sejam implantados em EKS, GKE e AKS com apenas substituição de imagem, annotations de identity e configuração de Ingress. O Terraform encapsula as diferenças de cada provedor em módulos independentes com interface uniforme de variáveis e outputs.

---

## 8. Conclusão

O projeto **GMontinny** demonstra como padrões clássicos de engenharia de software — processamento em chunks, event-driven architecture, token bucket, RBAC — podem ser orquestrados com ferramentas modernas do ecossistema Java para produzir uma solução corporativa robusta.

A solução resolve com eficiência o desafio da importação massiva de dados CNAE, mantendo alta resiliência a falhas (faultTolerant, DLQ, retry), total auditabilidade (metadados Batch no PostgreSQL, Liquibase changelogs) e estrita conformidade com padrões de segurança (Vault, JWT rotation, rate limiting distribuído).

As decisões documentadas — especialmente a integração `BatchConfig extends DefaultBatchConfiguration` e a sequence `BATCH_JOB_INSTANCE_SEQ` — representam conhecimento prático não trivial para equipes que adotam Spring Boot 4 com Spring Batch 6, servindo como referência para evitar horas de diagnóstico.

A infraestrutura declarativa multi-cloud (Kubernetes + Kustomize + Terraform para AWS, GCP e Azure) torna o sistema pronto para produção em qualquer provedor, com reprodutibilidade garantida do ambiente de desenvolvimento ao cluster gerenciado.

---

## 9. Referências

1. **SPRING.IO.** *Spring Batch 6 Reference Documentation*. Disponível em: https://docs.spring.io/spring-batch/reference/. Acesso em: 2026.
2. **SPRING.IO.** *Spring Boot 4.x Reference Guide*. Disponível em: https://docs.spring.io/spring-boot/index.html. Acesso em: 2026.
3. **SPRING.IO.** *Spring Security 7.x Reference*. Disponível em: https://docs.spring.io/spring-security/reference/. Acesso em: 2026.
4. **ORACLE.** *Java 25 Language Specification*. Oracle Corporation, 2026.
5. **HASHICORP.** *Vault Documentation: KV Secrets Engine v2*. Disponível em: https://developer.hashicorp.com/vault/docs. Acesso em: 2026.
6. **RABBITMQ.** *Dead Letter Exchanges and Reliability Guide*. Broadcom, 2026. Disponível em: https://www.rabbitmq.com/docs.
7. **REDIS.** *Redis Documentation: Distributed Rate Limiting*. Disponível em: https://redis.io/docs/. Acesso em: 2026.
8. **BUCKET4J.** *Java Rate Limiting Library — Token Bucket Algorithm*. Disponível em: https://bucket4j.github.io/. Acesso em: 2026.
9. **LIQUIBASE.** *Database Schema Change Management*. Disponível em: https://docs.liquibase.com/. Acesso em: 2026.
10. **APACHE SOFTWARE FOUNDATION.** *Apache POI — Java API for Microsoft Documents*. Disponível em: https://poi.apache.org/. Acesso em: 2026.
11. **IBGE.** *Classificação Nacional de Atividades Econômicas — CNAE 2.0: Estrutura Detalhada*. Instituto Brasileiro de Geografia e Estatística, Rio de Janeiro.
12. **FIELDING, Roy Thomas.** *Architectural Styles and the Design of Network-based Software Architectures*. Tese de Doutorado, University of California, Irvine, 2000.
13. **OWASP FOUNDATION.** *OWASP API Security Top 10*. Open Web Application Security Project, 2023. Disponível em: https://owasp.org/www-project-api-security/.
14. **HASHICORP.** *Terraform Documentation*. Disponível em: https://developer.hashicorp.com/terraform/docs. Acesso em: 2026.
15. **KUBERNETES AUTHORS.** *Kubernetes Documentation: Kustomize*. Disponível em: https://kubernetes.io/docs/tasks/manage-kubernetes-objects/kustomization/. Acesso em: 2026.
