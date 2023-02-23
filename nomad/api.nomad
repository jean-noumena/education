job "api" {
  type        = "service"
  datacenters = ["[[ .datacenter ]]"]
  namespace   = "[[ .namespace ]]"

  constraint {
    attribute = "${node.class}"
    value     = "worker"
  }

  update {
    min_healthy_time = "30s"
    auto_revert      = true
    max_parallel     = 1
  }

  group "api" {
    network {
      port "http" {}
      port "admin" {}
    }

    restart {
      attempts = 2
    }

    service {
      name = "[[ .api_name ]]"
      port = "http"
      tags = [
        "version=[[ .version ]]",
        "traefik.enable=true",
        "traefik.http.routers.[[ .api_name ]].entryPoints=external",
        "traefik.http.routers.[[ .api_name ]].rule=Host(`[[ .api_name ]].[[ .domain ]]`)",
      ]
    }

    service {
      name = "api-service-status"
      port = "admin"
      check {
        type     = "http"
        name     = "API Health Check"
        path     = "/health"
        interval = "10s"
        timeout  = "2s"
      }
      tags = [
        "prometheus=/metrics"
      ]
    }

    task "api" {
      leader = true
      driver = "docker"
      config {
        image        = "ghcr.io/noumenadigital/[[ .repo_name ]]/api:[[ .version ]]"
        network_mode = "host"
      }
      env {
        HTTP_PORT            = "${NOMAD_PORT_http}"
        HTTP_ADMIN_PORT      = "${NOMAD_PORT_admin}"
        LOG_LEVEL            = "[[ .log_level ]]"
        ACCESS_LOG_VERBOSITY = "[[ .access_log_verbosity ]]"
        API_SERVER_URL       = "https://[[ .api_name ]].[[ .domain ]]"
      }

      template {
        destination = ".env"
        env         = true
        data        = <<EOT
{{ range service "[[ .keycloak_name ]]" }}
KEYCLOAK_URL = "http://{{ .Address }}:{{ .Port }}"
{{ end }}
{{ range service "[[ .engine_name ]]" }}
ENGINE_URL = "http://{{ .Address }}:{{ .Port }}"
{{ end }}
{{ range service "[[ .postgraphile_name ]]" }}
READ_MODEL_URL = "http://{{ .Address }}:{{ .Port }}"
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
        args         = ["api", "java"]
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
