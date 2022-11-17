job "keycloak-provisioning" {
  datacenters = [
    "[[ .datacenter ]]",
  ]

  namespace = "[[ .namespace ]]"

  constraint {
    attribute = "${node.class}"
    value     = "worker"
  }

  type = "batch"

  group "keycloak-provisioning" {
    task "keycloak-provisioning" {
      leader = true
      driver = "docker"
      config {
        image        = "ghcr.io/noumenadigital/seed/keycloak-provisioning:[[ .version ]]"
        command      = "/cloud.sh"
        network_mode = "host"
      }

      env {
        KEYCLOAK_URL = "http://[[ .keycloak_name ]].service.consul:11000"
      }

      template {
        destination = "${NOMAD_SECRETS_DIR}/psql"
        env         = true
        data        = <<EOT
{{ with secret "secret/seed/keycloak-admin" }}
KEYCLOAK_USER = {{ .Data.username }}
KEYCLOAK_PASSWORD = {{ .Data.password }}
{{ end }}
EOT
      }

      resources {
        memory = 768
      }
    }

    task "filebeat" {
      driver = "docker"
      config {
        image        = "ghcr.io/noumenadigital/filebeat:1.0.3"
        network_mode = "host"
        args         = [
          "keycloak-provisioning",
          "wildfly",
        ]
      }
      resources {
        memory = 50
      }
    }
  }

  vault {
    policies = [
      "reader",
    ]
  }
}
