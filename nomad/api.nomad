job "api" {
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
    min_healthy_time = "30s"
    auto_revert      = true
    max_parallel     = 1
  }

  group "api" {
    count = 1

    network {
      port "http" {}
      port "admin" {}
    }

    restart {
      attempts = 2
    }

    service {
      name = "api"
      port = "http"
      tags = [
        "version=[[ .version ]]",
        "traefik.enable=true",
        "traefik.frontend.rule=Host:api.[[ .domain ]]",
        "traefik.frontend.entryPoints=[[ .entrypoint ]]",
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
        image        = "ghcr.io/noumenadigital/seed/api:[[ .version ]]"
        network_mode = "host"
      }
      env {
        HTTP_PORT       = "${NOMAD_PORT_http}"
        HTTP_ADMIN_PORT = "${NOMAD_PORT_admin}"
        KEYCLOAK_URL    = "[[ .KEYCLOAK_URL ]]"
        ENGINE_URL      = "[[ .ENGINE_ENDPOINT_URL ]]"
        LOG_LEVEL       = "[[ .LOG_LEVEL ]]"
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
          "api",
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
