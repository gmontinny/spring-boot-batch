output "eks_cluster_name" {
  value = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  value = module.eks.cluster_endpoint
}

output "ecr_repository_url" {
  value = aws_ecr_repository.gmontinny.repository_url
}

output "rds_endpoint" {
  value     = aws_db_instance.gmontinny.endpoint
  sensitive = true
}

output "redis_endpoint" {
  value     = aws_elasticache_replication_group.gmontinny.primary_endpoint_address
  sensitive = true
}

output "rabbitmq_endpoint" {
  value     = aws_mq_broker.gmontinny.instances[0].endpoints[0]
  sensitive = true
}
