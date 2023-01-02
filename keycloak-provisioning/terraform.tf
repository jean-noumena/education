# ref: https://registry.terraform.io/providers/mrparkers/keycloak/4.1.0/docs/resources/openid_client

variable "root_url" {
  type    = string
  default = "http://localhost:3000"
}

variable "base_url" {
  type    = string
  default = "http://localhost:3000"
}

variable "valid_redirect_uris" {
  type    = list(string)
  default = ["*"]
}

variable "valid_post_logout_redirect_uris" {
  type    = list(string)
  default = ["+"]
}

variable "web_origins" {
  type    = list(string)
  default = ["*"]
}

variable "realm_smtp_from" {
  type    = string
  default = "payee1@noumenadigital.com"
}

variable "realm_smtp_host" {
  type    = string
  default = "smtp.gmail.com"
}

variable "realm_smtp_port" {
  type    = number
  default = 465
}

variable "realm_smtp_auth_username" {
  type    = string
  default = "payee1@noumenadigital.com"
}

variable "realm_smtp_auth_password" {
  type    = string
  default = ""
}

resource "keycloak_realm" "realm" {
  realm                    = "seed"
  # Realm Settings > Login tab
  reset_password_allowed   = true
  login_with_email_allowed = true
  # Realm Settings > Email tab
  smtp_server {
    from = var.realm_smtp_from
    host = var.realm_smtp_host
    port = var.realm_smtp_port
    ssl  = true

    auth {
      username = var.realm_smtp_auth_username
      password = var.realm_smtp_auth_password
    }
  }
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
  realm_id                        = keycloak_realm.realm.id
  client_id                       = "seed"
  access_type                     = "PUBLIC"
  direct_access_grants_enabled    = true
  standard_flow_enabled           = true
  root_url                        = var.root_url
  base_url                        = var.base_url
  valid_redirect_uris             = var.valid_redirect_uris
  valid_post_logout_redirect_uris = var.valid_post_logout_redirect_uris
  web_origins                     = var.web_origins
}

resource "keycloak_user" "payee1" {
  realm_id   = keycloak_realm.realm.id
  username   = "payee1"
  email      = "payee1@noumenadigital.com"
  first_name = "payee1"
  last_name  = "noumena"
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
  email      = "payee2@noumenadigital.com"
  first_name = "payee2"
  last_name  = "noumena"
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
  email      = "issuer1@noumenadigital.com"
  first_name = "issuer1"
  last_name  = "noumena"
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
