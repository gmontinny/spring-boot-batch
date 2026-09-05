terraform {
  required_version = ">= 1.9"
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.0"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.0"
    }
  }
  backend "azurerm" {
    resource_group_name  = "gmontinny-tfstate-rg"
    storage_account_name = "gmontinnytfstate"
    container_name       = "tfstate"
    key                  = "azure/terraform.tfstate"
  }
}

provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy = false
    }
  }
}

provider "kubernetes" {
  host                   = azurerm_kubernetes_cluster.gmontinny.kube_config[0].host
  client_certificate     = base64decode(azurerm_kubernetes_cluster.gmontinny.kube_config[0].client_certificate)
  client_key             = base64decode(azurerm_kubernetes_cluster.gmontinny.kube_config[0].client_key)
  cluster_ca_certificate = base64decode(azurerm_kubernetes_cluster.gmontinny.kube_config[0].cluster_ca_certificate)
}

provider "helm" {
  kubernetes {
    host                   = azurerm_kubernetes_cluster.gmontinny.kube_config[0].host
    client_certificate     = base64decode(azurerm_kubernetes_cluster.gmontinny.kube_config[0].client_certificate)
    client_key             = base64decode(azurerm_kubernetes_cluster.gmontinny.kube_config[0].client_key)
    cluster_ca_certificate = base64decode(azurerm_kubernetes_cluster.gmontinny.kube_config[0].cluster_ca_certificate)
  }
}
