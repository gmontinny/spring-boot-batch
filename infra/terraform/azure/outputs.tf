output "aks_cluster_name" {
  value = azurerm_kubernetes_cluster.gmontinny.name
}

output "aks_kube_config" {
  value     = azurerm_kubernetes_cluster.gmontinny.kube_config_raw
  sensitive = true
}

output "acr_login_server" {
  value = azurerm_container_registry.gmontinny.login_server
}

output "postgres_fqdn" {
  value     = azurerm_postgresql_flexible_server.gmontinny.fqdn
  sensitive = true
}

output "redis_hostname" {
  value     = azurerm_redis_cache.gmontinny.hostname
  sensitive = true
}

output "redis_ssl_port" {
  value = azurerm_redis_cache.gmontinny.ssl_port
}

output "key_vault_uri" {
  value = azurerm_key_vault.gmontinny.vault_uri
}
