# reference: https://registry.terraform.io/providers/mrparkers/keycloak/latest/docs/resources/openid_client

resource "keycloak_realm" "realm" {
  realm = "seed"
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
  client_id                    = "seed"
  access_type                  = "PUBLIC"
  direct_access_grants_enabled = true
}

resource "keycloak_user" "payee1" {
  realm_id   = keycloak_realm.realm.id
  username   = "payee1"
  attributes = {
    "party" = jsonencode(["payee"])
  }
  initial_password {
    value     = "welcome1"
    temporary = false
  }
}

resource "keycloak_user" "payee2" {
  realm_id   = keycloak_realm.realm.id
  username   = "payee2"
  attributes = {
    "party" = jsonencode(["payee"])
  }
  initial_password {
    value     = "welcome2"
    temporary = false
  }
}

resource "keycloak_user" "issuer1" {
  realm_id   = keycloak_realm.realm.id
  username   = "issuer1"
  attributes = {
    "party" = jsonencode(["issuer"])
  }
  initial_password {
    value     = "welcome3"
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
