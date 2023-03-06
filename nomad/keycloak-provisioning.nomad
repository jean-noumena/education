job "keycloak-provisioning" {
  type = "batch"
  datacenters = ["[[ .datacenter ]]"]
  namespace = "[[ .namespace ]]"

  constraint {
    attribute = "${node.class}"
    value     = "worker"
  }

  group "keycloak-provisioning" {
    task "keycloak-provisioning" {
      leader = true
      driver = "docker"
      config {
        image        = "ghcr.io/noumenadigital/[[ .repo_name ]]/keycloak-provisioning:[[ .version ]]"
        command      = "/cloud.sh"
        network_mode = "host"
      }

      env {
        TF_VAR_application_name                = "[[ .application_name ]]"
        TF_VAR_default_password                = "[[ .keycloak_users_default_password ]]"
        TF_VAR_root_url                        = "https://[[ .frontend_name ]].[[ .domain ]]"
        TF_VAR_base_url                        = "https://[[ .frontend_name ]].[[ .domain ]]"
        TF_VAR_valid_redirect_uris             = "[\"*\"]"
        TF_VAR_valid_post_logout_redirect_uris = "[\"+\"]"
        TF_VAR_web_origins                     = "[\"*\"]"
        TF_VAR_realm_smtp_from                 = "[[ .smtp_email_sender ]]"
        TF_VAR_realm_smtp_host                 = "[[ .smtp_host ]]"
        TF_VAR_realm_smtp_port                 = "465"
      }

      template {
        destination = ".env"
        env         = true
        data        = <<EOT
{{ range service "[[ .keycloak_name ]]" }}
KEYCLOAK_URL = "http://{{ .Address }}:{{ .Port }}"
{{ end }}
EOT
      }

      template {
        destination = "${NOMAD_SECRETS_DIR}/app"
        env         = true
        data        = <<EOT
{{ with secret "secret/[[ .application_name ]]/keycloak-admin" }}
KEYCLOAK_USER = "{{ .Data.username }}"
KEYCLOAK_PASSWORD = "{{ .Data.password }}"
{{ end }}
{{ with secret "secret/[[ .application_name ]]/smtp" }}
TF_VAR_realm_smtp_auth_username = "{{ .Data.username }}"
TF_VAR_realm_smtp_auth_password = "{{ .Data.password }}"
{{ end }}
EOT
      }

      resources {
        memory = 512
      }
    }

    task "filebeat" {
      driver = "docker"
      config {
        image        = "ghcr.io/noumenadigital/filebeat:[[ .filebeat_version ]]"
        network_mode = "host"
        args         = ["keycloak-provisioning", "wildfly"]
      }
      resources {
        memory = 64
      }
    }
  }

  vault {
    policies = ["reader"]
  }
}
