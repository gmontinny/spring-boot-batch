project_id            = "<your_project_id>"
gcp_region            = "us-central1"
environment           = "prod"
gke_node_machine_type = "e2-standard-2"
gke_node_count        = 2
gke_node_min          = 1
gke_node_max          = 5
cloudsql_tier         = "db-g1-small"
redis_memory_size_gb  = 1
# Secrets: passe via -var ou use Secret Manager / Vault
# db_password       = ""
# redis_password    = ""
# rabbitmq_password = ""
