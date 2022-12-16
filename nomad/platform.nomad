job "platform" {
  datacenters = [
    "[[ .datacenter ]]",
  ]

  namespace = "[[ .namespace ]]"

  constraint {
    attribute = "${node.class}"
    value     = "worker"
  }

  type = "service"

  update {
    min_healthy_time = "10s"
    auto_revert      = true
  }

  group "engine" {

    network {
      port "http" {
        static = 12000
      }
    }

    service {
      name = "[[ .engine_name ]]"
      port = "http"
      tags = [
        "version=[[ .version ]]",
        "prometheus=/actuator/prometheusmetrics"
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
        image        = "ghcr.io/noumenadigital/seed/engine:[[ .version ]]"
        network_mode = "host"
        entrypoint   = ["java","-Djava.security.egd=file:/dev/./urandom","-XX:MaxRAMPercentage=80", "-jar","/engine.jar"]
        args         = [
          "--server.port=${NOMAD_PORT_http}",
        ]
      }
      env {
        ENGINE_AUTH_SERVER_BASE_URL           = "http://[[ .keycloak_name ]].service.consul:11000"
        ENGINE_DB_URL                         = "jdbc:postgresql://[[ .postgres_name ]].service.consul/[[ .platform_database ]]"
        ENGINE_DB_SCHEMA                      = "[[ .platform_db_schema ]]"
        ENGINE_DB_HISTORY_SCHEMA              = "[[ .history_db_schema ]]"
        ENGINE_LOG_CONFIG                     = "classpath:/logback-json.xml"
        SERVER_MAX_HTTP_HEADER_SIZE           = "32KB"
      }
      template {
        env         = true
        destination = ".env"
        data        = <<EOT
{{ with secret "secret/postgres-v2/[[ .platform_name ]]" }}
ENGINE_DB_USER = "{{ .Data.username }}"
ENGINE_DB_PASSWORD = "{{ .Data.password }}"
{{ end }}
{{ with secret "secret/postgres-v2/[[ .history_name ]]" }}
ENGINE_DB_HISTORY_USER = "{{ .Data.username }}"
ENGINE_DB_HISTORY_PASSWORD = "{{ .Data.password }}"
{{ end }}
{{ with secret "secret/postgres-v2/[[ .postgraphile_name ]]" }}
POSTGRAPHILE_DB_USER = "{{ .Data.username }}"
POSTGRAPHILE_DB_PASSWORD = "{{ .Data.password }}"
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
        image        = "ghcr.io/noumenadigital/filebeat:1.0.3"
        network_mode = "host"
        args         = [
          "platform-engine",
          "java",
        ]
      }
      resources {
        memory = 50
      }
    }
  }

  group "history" {

    network {
      # healthcheck
      port "http" {
        static = 12010
      }
      # todo @amela - setup /admin endpoints (API)
      # port "admin" {
      #   static = 12711
      # }
    }

    service {
      name = "[[ .history_name ]]"
      port = "http"
      tags = [
        "version=[[ .version ]]",
        "prometheus=/actuator/prometheusmetrics"
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
        args         = [
          "--server.port=${NOMAD_PORT_http}",
        ]
      }
      env {
        HISTORY_DB_URL                         = "jdbc:postgresql://[[ .postgres_name ]].service.consul/[[ .history_database ]]"
        HISTORY_DB_SCHEMA                      = "[[ .history_db_schema ]]"
        HISTORY_DB_ENGINE_SCHEMA               = "[[ .platform_db_schema ]]"
        HISTORY_LOG_CONFIG                     = "classpath:/logback-json.xml"
        HISTORY_LOG_LEVEL                      = "INFO"
      }
      template {
        env         = true
        destination = ".env"
        data        = <<EOT
{{ with secret "secret/postgres-v2/[[ .history_name ]]" }}
HISTORY_DB_USER = {{ .Data.username }}
HISTORY_DB_PASSWORD = {{ .Data.password }}
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
        image        = "ghcr.io/noumenadigital/filebeat:1.0.3"
        network_mode = "host"
        args         = [
          "platform-history",
          "java",
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
