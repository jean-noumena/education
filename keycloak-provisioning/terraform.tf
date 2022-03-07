# reference: https://registry.terraform.io/providers/mrparkers/keycloak/latest/docs/resources/openid_client

locals {
  parties = {
    issuer = "seeduser1"
    payee = "seeduser2"
  }
}

resource "keycloak_realm" "realm" {
  realm = "noumena"
}

resource "keycloak_role" "nm_user" {
  realm_id    = keycloak_realm.realm.id
  name        = "NM_USER"
  description = "Required role for accessing the platform"
}

resource "keycloak_default_roles" "default_roles" {
  realm_id      = keycloak_realm.realm.id
  default_roles = ["offline_access", "uma_authorization", keycloak_role.nm_user.name]
}

resource "keycloak_openid_client" "client" {
  realm_id                     = keycloak_realm.realm.id
  client_id                    = "nm-platform-service-client"
  client_secret                = "87ff12ca-cf29-4719-bda8-c92faa78e3c4"
  access_type                  = "CONFIDENTIAL"
  web_origins                  = ["*"]
  valid_redirect_uris          = ["*"]
  standard_flow_enabled        = true
  direct_access_grants_enabled = true
  service_accounts_enabled     = true
  authorization {
    policy_enforcement_mode          = "ENFORCING"
    decision_strategy                = "UNANIMOUS"
    allow_remote_resource_management = true
  }
}

resource "keycloak_user" "user1" {
  realm_id = keycloak_realm.realm.id
  username = "seeduser1"
  attributes = {
    "party" = jsonencode([local.parties.issuer])
  }
  initial_password {
    value     = "welcome"
    temporary = false
  }
}

resource "keycloak_user" "user2" {
  realm_id = keycloak_realm.realm.id
  username = "seeduser2"
  attributes = {
    "party" = jsonencode([local.parties.payee])
  }
  initial_password {
    value     = "welcome"
    temporary = false
  }
}

resource "keycloak_openid_user_attribute_protocol_mapper" "party_mapper" {
  realm_id  = keycloak_realm.realm.id
  client_id = keycloak_openid_client.client.id
  name      = "party-mapper"

  user_attribute   = "party"
  claim_name       = "party"
  claim_value_type = "JSON"
}