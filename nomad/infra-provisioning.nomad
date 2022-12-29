job "infra-provisioning" {
  type = "batch"
  datacenters = ["[[ .datacenter ]]"]
  namespace = "[[ .namespace ]]"

  constraint {
    attribute = "${node.class}"
    value     = "worker"
  }

  group "infra-provisioning" {
    task "infra-provisioning" {
      leader = true
      driver = "docker"
      config {
        image        = "ghcr.io/noumenadigital/[[ .repo_name ]]/infra-provisioning:[[ .version ]]"
        command      = "/cloud.sh"
        network_mode = "host"
      }

      env {
        VAULT_ADDR = "http://vault.service.consul:8200"
        TF_VAR_application_name = "[[ .application_name ]]"
      }

      template {
        destination = "${NOMAD_SECRETS_DIR}/app"
        env         = true
        data        = <<EOT
{{ with secret "secret/vault/shared/dev" }}
VAULT_TOKEN = "{{ .Data.root_token }}"
{{ end }}
{{ with secret "secret/postgres-v2/admin" }}
TF_VAR_postgres_username = "{{ .Data.username }}"
TF_VAR_postgres_password = "{{ .Data.password }}"
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
        image        = "ghcr.io/noumenadigital/filebeat:[[ .filebeat_version ]]"
        network_mode = "host"
        args         = ["infra-provisioning", "wildfly"]
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
