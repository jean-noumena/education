job "platform" {
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

  group "engine" {
    network {
      port "http" {}
      port "admin" {}
      port "management" {}
    }

    service {
      name = "[[ .engine_name ]]"
      port = "http"
      tags = [
        "version=[[ .version ]]",
        "prometheus=/actuator/prometheusmetrics",
        "traefik.enable=true",
        "traefik.http.routers.[[ .engine_name ]].entryPoints=internal",
        "traefik.http.routers.[[ .engine_name ]].rule=Host(`[[ .engine_name ]].[[ .domain ]]`)",
      ]
      check {
        name     = "Engine Health Check"
        type     = "http"
        path     = "/actuator/health"
        interval = "10s"
        timeout  = "1s"
      }
    }

    task "engine" {
      leader = true
      driver = "docker"
      config {
        image        = "ghcr.io/noumenadigital/[[ .repo_name ]]/engine:[[ .version ]]"
        network_mode = "host"
        entrypoint   = ["java","-Djava.security.egd=file:/dev/./urandom","-XX:MaxRAMPercentage=80", "-jar","/engine.jar"]
        args         = ["--server.port=${NOMAD_PORT_http}"]
      }
      env {
        ENGINE_ADMIN_PORT           = "${NOMAD_PORT_admin}"
        ENGINE_MANAGEMENT_PORT      = "${NOMAD_PORT_management}"
        ENGINE_DB_URL               = "jdbc:postgresql://[[ .postgres_fqdn ]]/[[ .platform_database ]]?ssl=true&sslmode=require"
        ENGINE_DB_SCHEMA            = "[[ .platform_db_schema ]]"
        ENGINE_DB_HISTORY_SCHEMA    = "[[ .history_db_schema ]]"
        ENGINE_LOG_CONFIG           = "classpath:/logback-json.xml"
        SERVER_MAX_HTTP_HEADER_SIZE = "32KB"
      }

      template {
        destination = ".env"
        env         = true
        data        = <<EOT
{{ range service "[[ .keycloak_name ]]" }}
ENGINE_AUTH_SERVER_BASE_URL = "http://{{ .Address }}:{{ .Port }}"
{{ end }}
EOT
      }

      template {
        destination = "${NOMAD_SECRETS_DIR}/app"
        env         = true
        data        = <<EOT
{{ with secret "secret/[[ .application_name ]]/[[ .platform_name ]]" }}
ENGINE_DB_USER = "{{ .Data.username }}"
ENGINE_DB_PASSWORD = "{{ .Data.password }}"
{{ end }}
{{ with secret "secret/[[ .application_name ]]/[[ .history_name ]]" }}
ENGINE_DB_HISTORY_USER = "{{ .Data.username }}"
ENGINE_DB_HISTORY_PASSWORD = "{{ .Data.password }}"
{{ end }}
{{ with secret "secret/[[ .application_name ]]/[[ .postgraphile_name ]]" }}
ENGINE_DB_POSTGRAPHILE_USER = "{{ .Data.username }}"
ENGINE_DB_POSTGRAPHILE_PASSWORD = "{{ .Data.password }}"
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
