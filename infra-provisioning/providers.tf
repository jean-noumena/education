terraform {
  required_version = ">= 1.0"
  required_providers {
    random = {
      version = "3.4.3"
    }
    vault = {
      version = "3.10.0"
    }
    postgresql = {
      source  = "cyrilgdn/postgresql"
      version = "1.17.1"
    }
  }
}

provider "vault" {}

provider "postgresql" {
  host            = var.postgres_host
  username        = var.postgres_username
  password        = var.postgres_password
  superuser       = false
  sslmode         = "require"
  connect_timeout = 15
}
