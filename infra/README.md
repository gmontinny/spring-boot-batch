# GMontinny — Infraestrutura

Kubernetes (Kustomize) + Terraform para AWS, GCP e Azure.

---

## Estrutura

```
infra/
├── kubernetes/
│   ├── base/                    # Manifestos agnósticos de cloud
│   │   ├── namespace.yaml
│   │   ├── rbac.yaml
│   │   ├── configmap.yaml
│   │   ├── secret.yaml          # Apenas dev/CI — produção usa Vault Agent Injector
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   ├── ingress.yaml
│   │   ├── hpa.yaml             # HorizontalPodAutoscaler (2–8 réplicas)
│   │   ├── pdb.yaml             # PodDisruptionBudget (minAvailable: 1)
│   │   └── kustomization.yaml
│   └── overlays/
│       ├── aws/                 # EKS + ALB + ECR + IRSA
│       ├── gcp/                 # GKE + Cloud Load Balancer + Artifact Registry + Workload Identity
│       └── azure/               # AKS + AGIC + ACR + Workload Identity
└── terraform/
    ├── aws/                     # EKS, ECR, RDS, ElastiCache, AmazonMQ, Secrets Manager
    ├── gcp/                     # GKE, Artifact Registry, Cloud SQL, Memorystore, Secret Manager
    └── azure/                   # AKS, ACR, PostgreSQL Flexible, Redis Cache, Key Vault
```

---

## Kubernetes

### Pré-requisitos

- `kubectl` >= 1.29
- `kustomize` >= 5.0

### Deploy

```bash
# AWS (EKS)
kubectl apply -k infra/kubernetes/overlays/aws

# GCP (GKE)
kubectl apply -k infra/kubernetes/overlays/gcp

# Azure (AKS)
kubectl apply -k infra/kubernetes/overlays/azure
```

### Configurar contexto

```bash
# AWS
aws eks update-kubeconfig --region us-east-1 --name gmontinny-eks

# GCP
gcloud container clusters get-credentials gmontinny-gke --region us-central1 --project <project_id>

# Azure
az aks get-credentials --resource-group gmontinny-rg --name gmontinny-aks
```

### Substituir placeholders nos overlays

Antes do deploy, edite os arquivos de overlay substituindo:

| Placeholder | Descrição |
|---|---|
| `<aws_account_id>` | ID da conta AWS |
| `<region>` | Região (ex: `us-east-1`) |
| `<rds_endpoint>` | Output do Terraform: `rds_endpoint` |
| `<elasticache_endpoint>` | Output do Terraform: `redis_endpoint` |
| `<amazonmq_endpoint>` | Output do Terraform: `rabbitmq_endpoint` |
| `<project_id>` | GCP Project ID |
| `<memorystore_ip>` | Output do Terraform: `redis_host` |
| `<acr_name>` | Nome do ACR (output: `acr_login_server`) |
| `<postgres_server>` | Output do Terraform: `postgres_fqdn` |
| `<redis_name>` | Output do Terraform: `redis_hostname` |

---

## Terraform

### Pré-requisitos

- Terraform >= 1.9
- CLI da cloud configurada (aws, gcloud, az)

### Fluxo padrão

```bash
cd infra/terraform/<aws|gcp|azure>

terraform init
terraform plan -var="db_password=<senha>" \
               -var="redis_password=<senha>" \
               -var="rabbitmq_password=<senha>"
terraform apply
```

### Secrets — nunca no tfvars

Passe secrets via variável de ambiente ou arquivo separado não versionado:

```bash
# Opção 1: variáveis de ambiente
export TF_VAR_db_password="MinhaSenh@123"
export TF_VAR_redis_password="Redis@123"
export TF_VAR_rabbitmq_password="Rabbit@123"
terraform apply

# Opção 2: arquivo secrets.tfvars (adicionar ao .gitignore)
terraform apply -var-file="secrets.tfvars"
```

### Backend de estado remoto

Crie os recursos de backend antes do `terraform init`:

**AWS:**
```bash
aws s3 mb s3://gmontinny-tfstate --region us-east-1
aws dynamodb create-table --table-name gmontinny-tflock \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST
```

**GCP:**
```bash
gsutil mb -l us-central1 gs://gmontinny-tfstate
```

**Azure:**
```bash
az group create -n gmontinny-tfstate-rg -l eastus
az storage account create -n gmontinnytfstate -g gmontinny-tfstate-rg -l eastus --sku Standard_LRS
az storage container create -n tfstate --account-name gmontinnytfstate
```

---

## Serviços por Cloud

| Componente | AWS | GCP | Azure |
|---|---|---|---|
| Kubernetes | EKS | GKE | AKS |
| Container Registry | ECR | Artifact Registry | ACR |
| PostgreSQL | RDS PostgreSQL 16 | Cloud SQL PostgreSQL 16 | PostgreSQL Flexible Server 16 |
| Redis | ElastiCache (Redis 7) | Memorystore (Redis 7) | Azure Cache for Redis |
| RabbitMQ | Amazon MQ (RabbitMQ 3.13) | RabbitMQ no GKE (Helm) | RabbitMQ no AKS (Helm) |
| Secrets | Secrets Manager | Secret Manager | Key Vault |
| Identity | IRSA | Workload Identity | Workload Identity |
| Ingress | AWS Load Balancer Controller (ALB) | GCE Ingress + Managed Cert | AGIC (Application Gateway) |

---

## Notas de Produção

- **Vault**: o `deployment.yaml` base inclui anotações do Vault Agent Injector. Configure o Vault com AppRole ou Kubernetes Auth em produção.
- **TLS**: todos os Ingress têm TLS configurado. Use cert-manager ou certificados gerenciados pela cloud.
- **RabbitMQ no GCP/Azure**: não há serviço gerenciado nativo equivalente ao Amazon MQ. Use o chart Helm oficial (`bitnami/rabbitmq`) ou um serviço externo (CloudAMQP).
- **Redis no Azure**: porta SSL `6380` — ajuste `REDIS_PORT` no overlay.
- **Cloud SQL no GCP**: o overlay inclui o Cloud SQL Auth Proxy como sidecar para conexão segura via IP privado.
