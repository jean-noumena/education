locals {
  application_name           = "seed"
  keycloak_name              = "keycloak-${local.application_name}"
  platform_name              = "platform-${local.application_name}"
  history_name               = "history-${local.application_name}"
  postgraphile_name          = "postgraphile-${local.application_name}"
  postgres_keycloak_name     = replace(local.keycloak_name, "-", "_")
  postgres_platform_name     = replace(local.platform_name, "-", "_")
  postgres_history_name      = replace(local.history_name, "-", "_")
  postgres_postgraphile_name = replace(local.postgraphile_name, "-", "_")
}

# keycloak db provisioning
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
  "username": "${local.postgres_keycloak_name}",
  "password": "${random_password.keycloak.result}"
}
EOT
}

resource "postgresql_role" "keycloak" {
  name      = vault_generic_secret.keycloak.data["username"]
  password  = vault_generic_secret.keycloak.data["password"]
  login     = true
}

resource "postgresql_database" "keycloak" {
  name = vault_generic_secret.keycloak.data["username"]
}

resource "postgresql_grant" "keycloak_database_all" {
  database    = postgresql_database.keycloak.name
  role        = postgresql_role.keycloak.name
  object_type = "database"
  privileges  = ["CONNECT", "CREATE", "TEMPORARY"]
}

# platform db provisioning
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
  "username": "${local.postgres_platform_name}",
  "password": "${random_password.platform.result}"
}
EOT
}

resource "postgresql_role" "platform" {
  name        = vault_generic_secret.platform.data["username"]
  password    = vault_generic_secret.platform.data["password"]
  login       = true
  create_role = true
}

resource "postgresql_database" "platform" {
  name = vault_generic_secret.platform.data["username"]
}

resource "postgresql_grant" "platform_database_all" {
  database    = postgresql_database.platform.name
  role        = postgresql_role.platform.name
  object_type = "database"
  privileges  = ["CONNECT", "CREATE", "TEMPORARY"]
}

# history db provisioning
resource "random_password" "history" {
  length      = 16
  min_upper   = 2
  min_lower   = 2
  min_numeric = 2
  min_special = 2
}

resource "vault_generic_secret" "history" {
  path      = "secret/postgres-v2/${local.history_name}"
  data_json = <<EOT
{
  "username": "${local.postgres_history_name}",
  "password": "${random_password.history.result}"
}
EOT
}

resource "postgresql_role" "history" {
  name        = vault_generic_secret.history.data["username"]
  password    = vault_generic_secret.history.data["password"]
  login       = true
  create_role = true
}

# the history db user needs a CREATE grant on the platform db
resource "postgresql_grant" "history_platform_database_all" {
  database    = postgresql_database.platform.name
  role        = postgresql_role.history.name
  object_type = "database"
  privileges  = ["CONNECT", "CREATE", "TEMPORARY"]
}

# postgraphile db provisioning
resource "random_password" "postgraphile" {
  length      = 16
  min_upper   = 2
  min_lower   = 2
  min_numeric = 2
  min_special = 2
}

resource "vault_generic_secret" "postgraphile" {
  path      = "secret/postgres-v2/${local.postgraphile_name}"
  data_json = <<EOT
{
  "username": "${local.postgres_postgraphile_name}",
  "password": "${random_password.postgraphile.result}"
}
EOT
}
