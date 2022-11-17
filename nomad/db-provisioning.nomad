job "db-provisioning" {
  datacenters = [
    "[[ .datacenter ]]",
  ]

  namespace = "[[ .namespace ]]"

  constraint {
    attribute = "${node.class}"
    value     = "worker"
  }

  type = "batch"

  group "db-provisioning" {
    task "db-provisioning" {
      leader = true
      driver = "docker"
      config {
        image        = "ghcr.io/noumenadigital/seed/db-provisioning:[[ .version ]]"
        command      = "/cloud.sh"
        network_mode = "host"
      }

      env {
        VAULT_ADDR = "http://vault.service.consul:8200"
      }

      template {
        destination = "${NOMAD_SECRETS_DIR}/psql"
        env         = true
        data        = <<EOT
{{ with secret "secret/vault/shared/dev" }}
VAULT_TOKEN = {{ .Data.root_token }}
{{ end }}
{{ with secret "secret/postgres-v2/admin" }}
TF_VAR_postgres_username = {{ .Data.username }}
TF_VAR_postgres_password = {{ .Data.password }}
{{ end }}
EOT
      }

      resources {
        memory = 256
      }
    }

    task "filebeat" {
      driver = "docker"
      config {
        image        = "ghcr.io/noumenadigital/filebeat:1.0.3"
        network_mode = "host"
        args         = [
          "platform-db-provisioning",
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
