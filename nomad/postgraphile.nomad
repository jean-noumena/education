job "postgraphile" {
  type = "service"
  datacenters = ["[[ .datacenter ]]"]
  namespace = "[[ .namespace ]]"

  constraint {
    attribute = "${node.class}"
    value     = "worker"
  }

  update {
    min_healthy_time = "10s"
    auto_revert      = true
  }

  group "postgraphile" {
    network {
      port "http" {}
    }

    task "postgraphile" {
      leader = true
      driver = "docker"
      config {
        image        = "ghcr.io/noumenadigital/packages/postgraphile:[[ .platform_version ]]"
        network_mode = "host"
      }

      env {
        POSTGRAPHILE_PORT                          = "${NOMAD_PORT_http}"
        POSTGRAPHILE_DB_SCHEMA                     = "[[ .platform_db_schema ]]"
        POSTGRAPHILE_TRUSTED_ISSUERS               = "https://[[ .keycloak_name ]].[[ .domain ]]/**"
        POSTGRAPHILE_ENGINE_HEALTH_TIMEOUT_SECONDS = 300
      }

      template {
        destination = ".env"
        env         = true
        data        = <<EOT
{{ range service "[[ .engine_name ]]" }}
POSTGRAPHILE_ENGINE_HEALTH_ENDPOINT = "http://{{ .Address }}:{{ .Port }}/actuator/health"
{{ end }}
EOT
      }

      template {
        destination = "${NOMAD_SECRETS_DIR}/app"
        env         = true
        data        = <<EOT
{{ with secret "secret/[[ .application_name ]]/[[ .postgraphile_name ]]" }}
POSTGRAPHILE_DB_URL = postgres://{{ .Data.username }}:{{ .Data.password }}@[[ .postgres_fqdn ]]/[[ .platform_database ]]?sslmode=required
{{ end }}
{{ with secret "secret/[[ .application_name ]]/[[ .postgraphile_name ]]" }}
POSTGRAPHILE_DB_USER = {{ .Data.username }}
{{ end }}
EOT
      }
      resources {
        memory = 256
      }

      service {
        name = "[[ .postgraphile_name ]]"
        port = "http"
        tags = [
          "version=[[ .platform_version ]]",
          "traefik.enable=true",
          "traefik.http.routers.postgraphile.entryPoints=internal",
          "traefik.http.routers.postgraphile.rule=Host(`[[ .postgraphile_name ]].[[ .domain ]]`)",
          "traefik.http.routers.postgraphile.service=[[ .postgraphile_name ]]@consulcatalog"
        ]

        # argument body is not supported in currently deployed nomad version 1.0.1
        # once nomad is upgraded to 1.10.0 this check can be converted into http post check
        check {
          type     = "tcp"
          interval = "10s"
          timeout  = "2s"

          check_restart {
            limit           = 3
            grace           = "30s"
            ignore_warnings = false
          }
        }
      }
    }

    task "filebeat" {
      driver = "docker"
      config {
        image        = "ghcr.io/noumenadigital/filebeat:[[ .filebeat_version ]]"
        network_mode = "host"
        args         = ["platform-engine", "java"]
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
