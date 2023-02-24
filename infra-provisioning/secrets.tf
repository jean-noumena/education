# keycloak db secrets
resource "random_password" "keycloak_admin" {
  length      = 16
  min_upper   = 2
  min_lower   = 2
  min_numeric = 2
  min_special = 2
}

resource "vault_generic_secret" "keycloak_admin" {
  path      = "secret/${var.application_name}/keycloak-admin"
  data_json = <<EOT
{
  "username": "admin",
  "password": "${random_password.keycloak_admin.result}"
}
EOT
}

resource "random_password" "keycloak" {
  length      = 16
  min_upper   = 2
  min_lower   = 2
  min_numeric = 2
  min_special = 2
}

resource "vault_generic_secret" "keycloak" {
  path      = "secret/${var.application_name}/${local.keycloak_name}"
  data_json = <<EOT
{
  "username": "${local.postgres_keycloak_name}",
  "password": "${random_password.keycloak.result}"
}
EOT
}

# platform db secrets
resource "random_password" "platform" {
  length      = 16
  min_upper   = 2
  min_lower   = 2
  min_numeric = 2
  min_special = 2
}

resource "vault_generic_secret" "platform" {
  path      = "secret/${var.application_name}/${local.platform_name}"
  data_json = <<EOT
{
  "username": "${local.postgres_platform_name}",
  "password": "${random_password.platform.result}"
}
EOT
}

# history db secrets
resource "random_password" "history" {
  length      = 16
  min_upper   = 2
  min_lower   = 2
  min_numeric = 2
  min_special = 2
}

resource "vault_generic_secret" "history" {
  path      = "secret/${var.application_name}/${local.history_name}"
  data_json = <<EOT
{
  "username": "${local.postgres_history_name}",
  "password": "${random_password.history.result}"
}
EOT
}

# postgraphile db secrets
resource "random_password" "postgraphile" {
  length      = 16
  min_upper   = 2
  min_lower   = 2
  min_numeric = 2
  min_special = 2
  override_special = local.url_compatible_special_chars
}

resource "vault_generic_secret" "postgraphile" {
  path      = "secret/${var.application_name}/${local.postgraphile_name}"
  data_json = <<EOT
{
  "username": "${local.postgres_postgraphile_name}",
  "password": "${random_password.postgraphile.result}"
}
EOT
}
