data "azurerm_client_config" "current" {}

locals {
  prefix = "gmontinny"
  tags = {
    Project     = "gmontinny"
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

# ── Resource Group ────────────────────────────────────────────────────────────
resource "azurerm_resource_group" "gmontinny" {
  name     = "${local.prefix}-rg"
  location = var.location
  tags     = local.tags
}

# ── Virtual Network ───────────────────────────────────────────────────────────
resource "azurerm_virtual_network" "gmontinny" {
  name                = "${local.prefix}-vnet"
  address_space       = ["10.0.0.0/16"]
  location            = azurerm_resource_group.gmontinny.location
  resource_group_name = azurerm_resource_group.gmontinny.name
  tags                = local.tags
}

resource "azurerm_subnet" "aks" {
  name                 = "aks-subnet"
  resource_group_name  = azurerm_resource_group.gmontinny.name
  virtual_network_name = azurerm_virtual_network.gmontinny.name
  address_prefixes     = ["10.0.1.0/24"]
}

resource "azurerm_subnet" "db" {
  name                 = "db-subnet"
  resource_group_name  = azurerm_resource_group.gmontinny.name
  virtual_network_name = azurerm_virtual_network.gmontinny.name
  address_prefixes     = ["10.0.2.0/24"]

  delegation {
    name = "postgres-delegation"
    service_delegation {
      name    = "Microsoft.DBforPostgreSQL/flexibleServers"
      actions = ["Microsoft.Network/virtualNetworks/subnets/join/action"]
    }
  }
}

# ── ACR ───────────────────────────────────────────────────────────────────────
resource "azurerm_container_registry" "gmontinny" {
  name                = "${local.prefix}acr"
  resource_group_name = azurerm_resource_group.gmontinny.name
  location            = azurerm_resource_group.gmontinny.location
  sku                 = "Standard"
  admin_enabled       = false
  tags                = local.tags
}

# ── AKS ───────────────────────────────────────────────────────────────────────
resource "azurerm_kubernetes_cluster" "gmontinny" {
  name                = "${local.prefix}-aks"
  location            = azurerm_resource_group.gmontinny.location
  resource_group_name = azurerm_resource_group.gmontinny.name
  dns_prefix          = local.prefix
  kubernetes_version  = var.aks_kubernetes_version
  tags                = local.tags

  default_node_pool {
    name                = "default"
    vm_size             = var.aks_node_vm_size
    node_count          = var.aks_node_count
    min_count           = var.aks_node_min
    max_count           = var.aks_node_max
    auto_scaling_enabled = true
    vnet_subnet_id      = azurerm_subnet.aks.id
    os_disk_size_gb     = 50
  }

  identity {
    type = "SystemAssigned"
  }

  network_profile {
    network_plugin = "azure"
    network_policy = "azure"
  }

  oidc_issuer_enabled       = true
  workload_identity_enabled = true
}

# AKS pull from ACR
resource "azurerm_role_assignment" "aks_acr_pull" {
  principal_id                     = azurerm_kubernetes_cluster.gmontinny.kubelet_identity[0].object_id
  role_definition_name             = "AcrPull"
  scope                            = azurerm_container_registry.gmontinny.id
  skip_service_principal_aad_check = true
}

# ── Azure Database for PostgreSQL Flexible Server ─────────────────────────────
resource "azurerm_private_dns_zone" "postgres" {
  name                = "${local.prefix}.postgres.database.azure.com"
  resource_group_name = azurerm_resource_group.gmontinny.name
}

resource "azurerm_private_dns_zone_virtual_network_link" "postgres" {
  name                  = "${local.prefix}-postgres-dns-link"
  private_dns_zone_name = azurerm_private_dns_zone.postgres.name
  virtual_network_id    = azurerm_virtual_network.gmontinny.id
  resource_group_name   = azurerm_resource_group.gmontinny.name
}

resource "azurerm_postgresql_flexible_server" "gmontinny" {
  name                   = "${local.prefix}-postgres"
  resource_group_name    = azurerm_resource_group.gmontinny.name
  location               = azurerm_resource_group.gmontinny.location
  version                = "16"
  delegated_subnet_id    = azurerm_subnet.db.id
  private_dns_zone_id    = azurerm_private_dns_zone.postgres.id
  administrator_login    = "gmontinny"
  administrator_password = var.db_password
  sku_name               = var.postgres_sku
  storage_mb             = 32768
  backup_retention_days  = 7
  geo_redundant_backup_enabled = false
  tags                   = local.tags

  depends_on = [azurerm_private_dns_zone_virtual_network_link.postgres]
}

resource "azurerm_postgresql_flexible_server_database" "gmontinny" {
  name      = "gmontinny"
  server_id = azurerm_postgresql_flexible_server.gmontinny.id
  charset   = "UTF8"
  collation = "en_US.utf8"
}

# ── Azure Cache for Redis ─────────────────────────────────────────────────────
resource "azurerm_redis_cache" "gmontinny" {
  name                = "${local.prefix}-redis"
  location            = azurerm_resource_group.gmontinny.location
  resource_group_name = azurerm_resource_group.gmontinny.name
  capacity            = var.redis_capacity
  family              = var.redis_family
  sku_name            = var.redis_sku
  enable_non_ssl_port = false
  minimum_tls_version = "1.2"
  tags                = local.tags

  redis_configuration {
    maxmemory_policy = "allkeys-lru"
  }
}

# ── Key Vault ─────────────────────────────────────────────────────────────────
resource "azurerm_key_vault" "gmontinny" {
  name                = "${local.prefix}-kv"
  location            = azurerm_resource_group.gmontinny.location
  resource_group_name = azurerm_resource_group.gmontinny.name
  tenant_id           = data.azurerm_client_config.current.tenant_id
  sku_name            = "standard"
  tags                = local.tags

  access_policy {
    tenant_id = data.azurerm_client_config.current.tenant_id
    object_id = data.azurerm_client_config.current.object_id

    secret_permissions = ["Get", "List", "Set", "Delete", "Purge"]
  }
}

resource "azurerm_key_vault_secret" "db_password" {
  name         = "db-password"
  value        = var.db_password
  key_vault_id = azurerm_key_vault.gmontinny.id
}

resource "azurerm_key_vault_secret" "redis_password" {
  name         = "redis-password"
  value        = var.redis_password
  key_vault_id = azurerm_key_vault.gmontinny.id
}

resource "azurerm_key_vault_secret" "rabbitmq_password" {
  name         = "rabbitmq-password"
  value        = var.rabbitmq_password
  key_vault_id = azurerm_key_vault.gmontinny.id
}

# ── AGIC — Application Gateway Ingress Controller ─────────────────────────────
resource "helm_release" "agic" {
  name       = "ingress-azure"
  repository = "https://appgwingress.blob.core.windows.net/ingress-azure-helm-package/"
  chart      = "ingress-azure"
  namespace  = "kube-system"

  set {
    name  = "appgw.subscriptionId"
    value = data.azurerm_client_config.current.subscription_id
  }
  set {
    name  = "appgw.resourceGroup"
    value = azurerm_resource_group.gmontinny.name
  }
  set {
    name  = "armAuth.type"
    value = "aadPodIdentity"
  }

  depends_on = [azurerm_kubernetes_cluster.gmontinny]
}
