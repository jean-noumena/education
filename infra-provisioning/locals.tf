locals {
  url_compatible_special_chars = "-._~"
  keycloak_name                = "keycloak-${var.application_name}"
  platform_name                = "platform-${var.application_name}"
  history_name                 = "history-${var.application_name}"
  postgraphile_name            = "postgraphile-${var.application_name}"
  postgres_keycloak_name       = replace(local.keycloak_name, "-", "_")
  postgres_platform_name       = replace(local.platform_name, "-", "_")
  postgres_history_name        = replace(local.history_name, "-", "_")
  postgres_postgraphile_name   = replace(local.postgraphile_name, "-", "_")
}
