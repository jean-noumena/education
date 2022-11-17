terraform {
  required_version = ">= 1.0"
  required_providers {
    keycloak = {
      source  = "mrparkers/keycloak"
      version = "3.6.0"
    }
  }
}

provider "keycloak" {
  client_id = "admin-cli"
  base_path = ""
}
