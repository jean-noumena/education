# keycloak db provisioning

resource "postgresql_role" "keycloak" {
  name      = local.postgres_keycloak_name
  password  = random_password.keycloak.result
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
resource "postgresql_role" "platform" {
  name      = local.postgres_platform_name
  password  = random_password.platform.result
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
resource "postgresql_role" "history" {
  name      = local.postgres_history_name
  password  = random_password.history.result
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
