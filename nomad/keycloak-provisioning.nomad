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
EOT
      }

      resources {
        memory = 128
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
