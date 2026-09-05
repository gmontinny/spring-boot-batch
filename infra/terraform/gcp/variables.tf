variable "project_id" {
  description = "GCP Project ID"
  type        = string
}

variable "gcp_region" {
  description = "GCP region"
  type        = string
  default     = "us-central1"
}

variable "environment" {
  type    = string
  default = "prod"
}

variable "gke_cluster_version" {
  type    = string
  default = "latest"
}

variable "gke_node_machine_type" {
  type    = string
  default = "e2-standard-2"
}

variable "gke_node_count" {
  type    = number
  default = 2
}

variable "gke_node_min" {
  type    = number
  default = 1
}

variable "gke_node_max" {
  type    = number
  default = 5
}

variable "cloudsql_tier" {
  type    = string
  default = "db-g1-small"
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "redis_memory_size_gb" {
  type    = number
  default = 1
}

variable "redis_password" {
  type      = string
  sensitive = true
}

variable "rabbitmq_password" {
  type      = string
  sensitive = true
}
