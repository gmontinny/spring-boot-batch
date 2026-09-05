output "gke_cluster_name" {
  value = module.gke.name
}

output "gke_cluster_endpoint" {
  value     = module.gke.endpoint
  sensitive = true
}

output "artifact_registry_url" {
  value = "${var.gcp_region}-docker.pkg.dev/${var.project_id}/gmontinny"
}

output "cloudsql_private_ip" {
  value     = google_sql_database_instance.gmontinny.private_ip_address
  sensitive = true
}

output "redis_host" {
  value     = google_redis_instance.gmontinny.host
  sensitive = true
}

output "workload_identity_sa" {
  value = google_service_account.gmontinny.email
}
