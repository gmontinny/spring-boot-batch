aws_region            = "us-east-1"
environment           = "prod"
vpc_cidr              = "10.0.0.0/16"
eks_cluster_version   = "1.31"
eks_node_instance_type = "t3.medium"
eks_node_desired      = 2
eks_node_min          = 1
eks_node_max          = 5
rds_instance_class    = "db.t3.medium"
rds_allocated_storage = 20
redis_node_type       = "cache.t3.micro"
# Secrets: passe via -var ou use AWS Secrets Manager / Vault
# db_password       = ""
# redis_password    = ""
# rabbitmq_password = ""
