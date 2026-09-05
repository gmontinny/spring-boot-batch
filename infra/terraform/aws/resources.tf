# ── VPC ──────────────────────────────────────────────────────────────────────
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"

  name = "gmontinny-vpc"
  cidr = var.vpc_cidr

  azs             = ["${var.aws_region}a", "${var.aws_region}b", "${var.aws_region}c"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]

  enable_nat_gateway   = true
  single_nat_gateway   = true
  enable_dns_hostnames = true

  private_subnet_tags = {
    "kubernetes.io/role/internal-elb" = "1"
  }
  public_subnet_tags = {
    "kubernetes.io/role/elb" = "1"
  }
}

# ── EKS ──────────────────────────────────────────────────────────────────────
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.0"

  cluster_name    = "gmontinny-eks"
  cluster_version = var.eks_cluster_version

  vpc_id                         = module.vpc.vpc_id
  subnet_ids                     = module.vpc.private_subnets
  cluster_endpoint_public_access = true

  eks_managed_node_groups = {
    default = {
      instance_types = [var.eks_node_instance_type]
      min_size       = var.eks_node_min
      max_size       = var.eks_node_max
      desired_size   = var.eks_node_desired
    }
  }

  # Addons
  cluster_addons = {
    coredns                = { most_recent = true }
    kube-proxy             = { most_recent = true }
    vpc-cni                = { most_recent = true }
    aws-ebs-csi-driver     = { most_recent = true }
  }
}

# ── ECR ──────────────────────────────────────────────────────────────────────
resource "aws_ecr_repository" "gmontinny" {
  name                 = "gmontinny"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

# ── RDS PostgreSQL ────────────────────────────────────────────────────────────
resource "aws_db_subnet_group" "gmontinny" {
  name       = "gmontinny-db-subnet"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_security_group" "rds" {
  name   = "gmontinny-rds-sg"
  vpc_id = module.vpc.vpc_id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }
}

resource "aws_db_instance" "gmontinny" {
  identifier        = "gmontinny-postgres"
  engine            = "postgres"
  engine_version    = "16"
  instance_class    = var.rds_instance_class
  allocated_storage = var.rds_allocated_storage
  storage_encrypted = true

  db_name  = "gmontinny"
  username = "gmontinny"
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.gmontinny.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  backup_retention_period = 7
  deletion_protection     = true
  skip_final_snapshot     = false
  final_snapshot_identifier = "gmontinny-final"

  multi_az = true
}

# ── ElastiCache Redis ─────────────────────────────────────────────────────────
resource "aws_elasticache_subnet_group" "gmontinny" {
  name       = "gmontinny-redis-subnet"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_security_group" "redis" {
  name   = "gmontinny-redis-sg"
  vpc_id = module.vpc.vpc_id

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }
}

resource "aws_elasticache_replication_group" "gmontinny" {
  replication_group_id = "gmontinny-redis"
  description          = "Redis for gmontinny rate limiting"

  node_type            = var.redis_node_type
  num_cache_clusters   = 2
  port                 = 6379
  subnet_group_name    = aws_elasticache_subnet_group.gmontinny.name
  security_group_ids   = [aws_security_group.redis.id]

  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  auth_token                 = var.redis_password
}

# ── Amazon MQ (RabbitMQ) ──────────────────────────────────────────────────────
resource "aws_security_group" "mq" {
  name   = "gmontinny-mq-sg"
  vpc_id = module.vpc.vpc_id

  ingress {
    from_port       = 5671
    to_port         = 5671
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }
}

resource "aws_mq_broker" "gmontinny" {
  broker_name        = "gmontinny-rabbitmq"
  engine_type        = "RabbitMQ"
  engine_version     = "3.13"
  host_instance_type = "mq.m5.large"
  deployment_mode    = "CLUSTER_MULTI_AZ"

  subnet_ids         = module.vpc.private_subnets
  security_groups    = [aws_security_group.mq.id]
  publicly_accessible = false

  user {
    username = "gmontinny"
    password = var.rabbitmq_password
  }
}

# ── AWS Secrets Manager ───────────────────────────────────────────────────────
resource "aws_secretsmanager_secret" "gmontinny" {
  name                    = "gmontinny/secrets"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "gmontinny" {
  secret_id = aws_secretsmanager_secret.gmontinny.id
  secret_string = jsonencode({
    db_password       = var.db_password
    redis_password    = var.redis_password
    rabbitmq_password = var.rabbitmq_password
  })
}

# ── ALB Ingress Controller (Helm) ─────────────────────────────────────────────
resource "helm_release" "aws_load_balancer_controller" {
  name       = "aws-load-balancer-controller"
  repository = "https://aws.github.io/eks-charts"
  chart      = "aws-load-balancer-controller"
  namespace  = "kube-system"

  set {
    name  = "clusterName"
    value = module.eks.cluster_name
  }
  set {
    name  = "serviceAccount.create"
    value = "true"
  }

  depends_on = [module.eks]
}
