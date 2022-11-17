locals {
  application_name = "seed"
  platform_name    = "platform-${local.application_name}"
  keycloak_name    = "keycloak-${local.application_name}"
}

resource "random_password" "keycloak" {
  length      = 16
  min_upper   = 2
  min_lower   = 2
  min_numeric = 2
  min_special = 2
}

resource "vault_generic_secret" "keycloak" {
  path      = "secret/postgres-v2/${local.keycloak_name}"
  data_json = <<EOT
{
  "username": "${local.keycloak_name}",
  "password": "${random_password.keycloak.result}"
}
EOT
}

resource "postgresql_role" "keycloak" {
  name     = vault_generic_secret.keycloak.data["username"]
  login    = true
  password = vault_generic_secret.keycloak.data["password"]
}

resource "postgresql_database" "keycloak" {
  name = vault_generic_secret.keycloak.data["username"]
}

resource "random_password" "platform" {
  length      = 16
  min_upper   = 2
  min_lower   = 2
  min_numeric = 2
  min_special = 2
}

resource "vault_generic_secret" "platform" {
  path      = "secret/postgres-v2/${local.platform_name}"
  data_json = <<EOT
{
  "username": "${local.platform_name}",
  "password": "${random_password.platform.result}"
}
EOT
}

resource "postgresql_role" "platform" {
  name     = vault_generic_secret.platform.data["username"]
  password = vault_generic_secret.platform.data["password"]
  login    = true
}

resource "postgresql_database" "platform" {
  name = vault_generic_secret.platform.data["username"]
}
