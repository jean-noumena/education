job "platform" {
  datacenters = [
    "[[ .datacenter ]]",
  ]

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
      name = "engine"
      port = "http"
      tags = [
        "version=[[ .engine ]]",
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
        ENGINE_AUTH_SERVER_BASE_URL           = "[[ .KEYCLOAK_URL ]]"
        ENGINE_DB_URL                         = "[[ .ENGINE_DB_URL ]]"
        ENGINE_DB_SCHEMA                      = "[[ .ENGINE_DB ]]"
        ENGINE_LOG_CONFIG                     = "classpath:/logback-json.xml"
        SERVER_MAX_HTTP_HEADER_SIZE           = "32KB"
      }
      template {
        env         = true
        destination = ".env"
        data        = <<EOT
{{ with secret "secret/postgres/platform" }}
ENGINE_DB_USER = {{ .Data.username }}
ENGINE_DB_PASSWORD = {{ .Data.password }}
{{ end }}
EOT
      }
      resources {
        memory = 2096
      }
    }

    task "filebeat" {
      driver = "docker"
      config {
        image        = "noumenadigital/filebeat:1.0.57796"
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

  vault {
    policies = [
      "reader",
    ]
  }
}
