location               = "eastus"
environment            = "prod"
aks_kubernetes_version = "1.31"
aks_node_vm_size       = "Standard_D2s_v3"
aks_node_count         = 2
aks_node_min           = 1
aks_node_max           = 5
postgres_sku           = "B_Standard_B1ms"
redis_capacity         = 1
redis_family           = "C"
redis_sku              = "Standard"
# Secrets: passe via -var ou use Key Vault / Vault
# db_password       = ""
# redis_password    = ""
# rabbitmq_password = ""
