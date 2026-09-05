variable "location" {
  description = "Azure region"
  type        = string
  default     = "eastus"
}

variable "environment" {
  type    = string
  default = "prod"
}

variable "aks_kubernetes_version" {
  type    = string
  default = "1.31"
}

variable "aks_node_vm_size" {
  type    = string
  default = "Standard_D2s_v3"
}

variable "aks_node_count" {
  type    = number
  default = 2
}

variable "aks_node_min" {
  type    = number
  default = 1
}

variable "aks_node_max" {
  type    = number
  default = 5
}

variable "postgres_sku" {
  type    = string
  default = "B_Standard_B1ms"
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "redis_capacity" {
  type    = number
  default = 1
}

variable "redis_family" {
  type    = string
  default = "C"
}

variable "redis_sku" {
  type    = string
  default = "Standard"
}

variable "redis_password" {
  type      = string
  sensitive = true
}

variable "rabbitmq_password" {
  type      = string
  sensitive = true
}
