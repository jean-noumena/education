job "history" {
  type        = "service"
  datacenters = ["[[ .datacenter ]]"]
  namespace   = "[[ .namespace ]]"

  constraint {
    attribute = "${node.class}"
    value     = "worker"
  }

  update {
    min_healthy_time = "10s"
    auto_revert      = true
  }

  group "history" {

    network {
      port "http" {}
      port "admin" {}
    }

    service {
      name = "[[ .history_name ]]"
      port = "http"
      tags = [
        "version=[[ .version ]]",
        "prometheus=/actuator/prometheusmetrics",
        "traefik.enable=true",
        "traefik.http.routers.[[ .history_name ]].entryPoints=internal",
        "traefik.http.routers.[[ .history_name ]].rule=Host(`[[ .history_name ]].[[ .domain ]]`)",
      ]

      check {
        name     = "History Health Check"
        type     = "http"
        path     = "/actuator/health"
        interval = "10s"
        timeout  = "1s"
      }
    }

    task "history" {
      leader = true
      driver = "docker"

      config {
        image        = "ghcr.io/noumenadigital/packages/history:[[ .platform_version ]]"
        network_mode = "host"
        entrypoint   = ["java","-Djava.security.egd=file:/dev/./urandom","-XX:MaxRAMPercentage=80", "-jar","/history.jar"]
        args         = ["--server.port=${NOMAD_PORT_http}"]
      }

      env {
        HISTORY_ADMIN_PORT       = "${NOMAD_PORT_admin}"
        HISTORY_DB_URL           = "jdbc:postgresql://[[ .postgres_fqdn ]]/[[ .platform_database ]]?ssl=true&sslmode=require"
        HISTORY_DB_SCHEMA        = "[[ .history_db_schema ]]"
        HISTORY_DB_ENGINE_SCHEMA = "[[ .platform_db_schema ]]"
        HISTORY_LOG_CONFIG       = "classpath:/logback-json.xml"
      }

      template {
        destination = "${NOMAD_SECRETS_DIR}/app"
        env         = true
        data        = <<EOT
{{ with secret "secret/[[ .application_name ]]/[[ .history_name ]]" }}
HISTORY_DB_USER = "{{ .Data.username }}"
HISTORY_DB_PASSWORD = "{{ .Data.password }}"
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
        args         = ["platform-history", "java"]
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
