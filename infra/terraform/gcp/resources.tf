# ── APIs ─────────────────────────────────────────────────────────────────────
resource "google_project_service" "apis" {
  for_each = toset([
    "container.googleapis.com",
    "sqladmin.googleapis.com",
    "redis.googleapis.com",
    "secretmanager.googleapis.com",
    "artifactregistry.googleapis.com",
    "servicenetworking.googleapis.com",
  ])
  service            = each.value
  disable_on_destroy = false
}

# ── VPC ──────────────────────────────────────────────────────────────────────
resource "google_compute_network" "gmontinny" {
  name                    = "gmontinny-vpc"
  auto_create_subnetworks = false
  depends_on              = [google_project_service.apis]
}

resource "google_compute_subnetwork" "gmontinny" {
  name          = "gmontinny-subnet"
  ip_cidr_range = "10.0.0.0/20"
  region        = var.gcp_region
  network       = google_compute_network.gmontinny.id

  secondary_ip_range {
    range_name    = "pods"
    ip_cidr_range = "10.1.0.0/16"
  }
  secondary_ip_range {
    range_name    = "services"
    ip_cidr_range = "10.2.0.0/20"
  }
}

# ── GKE ──────────────────────────────────────────────────────────────────────
module "gke" {
  source  = "terraform-google-modules/kubernetes-engine/google"
  version = "~> 33.0"

  project_id = var.project_id
  name       = "gmontinny-gke"
  region     = var.gcp_region

  network           = google_compute_network.gmontinny.name
  subnetwork        = google_compute_subnetwork.gmontinny.name
  ip_range_pods     = "pods"
  ip_range_services = "services"

  kubernetes_version       = var.gke_cluster_version
  remove_default_node_pool = true
  initial_node_count       = 1

  node_pools = [
    {
      name               = "default-pool"
      machine_type       = var.gke_node_machine_type
      min_count          = var.gke_node_min
      max_count          = var.gke_node_max
      initial_node_count = var.gke_node_count
      disk_size_gb       = 50
      auto_repair        = true
      auto_upgrade       = true
      enable_secure_boot = true
    }
  ]

  workload_identity_config = [{
    workload_pool = "${var.project_id}.svc.id.goog"
  }]
}

# ── Artifact Registry ─────────────────────────────────────────────────────────
resource "google_artifact_registry_repository" "gmontinny" {
  location      = var.gcp_region
  repository_id = "gmontinny"
  format        = "DOCKER"
  depends_on    = [google_project_service.apis]
}

# ── Cloud SQL PostgreSQL ──────────────────────────────────────────────────────
resource "google_compute_global_address" "private_ip" {
  name          = "gmontinny-private-ip"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 16
  network       = google_compute_network.gmontinny.id
}

resource "google_service_networking_connection" "private_vpc" {
  network                 = google_compute_network.gmontinny.id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_ip.name]
}

resource "google_sql_database_instance" "gmontinny" {
  name             = "gmontinny-postgres"
  database_version = "POSTGRES_16"
  region           = var.gcp_region

  settings {
    tier              = var.cloudsql_tier
    availability_type = "REGIONAL"
    disk_autoresize   = true

    backup_configuration {
      enabled    = true
      start_time = "03:00"
    }

    ip_configuration {
      ipv4_enabled    = false
      private_network = google_compute_network.gmontinny.id
    }
  }

  deletion_protection = true
  depends_on          = [google_service_networking_connection.private_vpc]
}

resource "google_sql_database" "gmontinny" {
  name     = "gmontinny"
  instance = google_sql_database_instance.gmontinny.name
}

resource "google_sql_user" "gmontinny" {
  name     = "gmontinny"
  instance = google_sql_database_instance.gmontinny.name
  password = var.db_password
}

# ── Memorystore Redis ─────────────────────────────────────────────────────────
resource "google_redis_instance" "gmontinny" {
  name           = "gmontinny-redis"
  tier           = "STANDARD_HA"
  memory_size_gb = var.redis_memory_size_gb
  region         = var.gcp_region

  authorized_network = google_compute_network.gmontinny.id
  connect_mode       = "PRIVATE_SERVICE_ACCESS"
  auth_enabled       = true
  redis_version      = "REDIS_7_0"
  transit_encryption_mode = "SERVER_AUTHENTICATION"

  depends_on = [google_service_networking_connection.private_vpc]
}

# ── Secret Manager ────────────────────────────────────────────────────────────
resource "google_secret_manager_secret" "gmontinny" {
  secret_id  = "gmontinny-secrets"
  depends_on = [google_project_service.apis]

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "gmontinny" {
  secret = google_secret_manager_secret.gmontinny.id
  secret_data = jsonencode({
    db_password       = var.db_password
    redis_password    = var.redis_password
    rabbitmq_password = var.rabbitmq_password
  })
}

# ── Workload Identity ─────────────────────────────────────────────────────────
resource "google_service_account" "gmontinny" {
  account_id   = "gmontinny"
  display_name = "GMontinny App Service Account"
}

resource "google_project_iam_member" "secret_accessor" {
  project = var.project_id
  role    = "roles/secretmanager.secretAccessor"
  member  = "serviceAccount:${google_service_account.gmontinny.email}"
}

resource "google_service_account_iam_member" "workload_identity" {
  service_account_id = google_service_account.gmontinny.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "serviceAccount:${var.project_id}.svc.id.goog[gmontinny/gmontinny]"
}
