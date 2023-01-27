job "keycloak" {
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

  group "keycloak" {
    network {
      port "http" {}
    }

    service {
      name = "[[ .keycloak_name ]]"
      port = "http"
      tags = [
        "version=[[ .version ]]",
        "traefik.enable=true",
        "traefik.http.routers.keycloak.entryPoints=internal",
        "traefik.http.routers.keycloak.rule=Host(`[[ .keycloak_name ]].[[ .domain ]]`) && PathPrefix(`/`)",
        "traefik.http.routers.keycloak.service=[[ .keycloak_name ]]@consulcatalog"
      ]
      check {
        name     = "Keycloak HTTP interface"
        type     = "http"
        path     = "/health"
        interval = "10s"
        timeout  = "1s"
      }
    }

    task "keycloak" {
      leader = true
      driver = "docker"
      config {
        image        = "quay.io/keycloak/keycloak:[[ .keycloak_version ]]"
        network_mode = "host"
        args         = [
          "start",
          "--auto-build",
          "--db=postgres"
        ]
      }

      env {
        KC_HEALTH_ENABLED  = "true"
        KC_HTTP_ENABLED    = "true"
        KC_HTTP_HOST       = "0.0.0.0"
        KC_HTTP_PORT       = "${NOMAD_PORT_http}"
        KC_HOSTNAME        = "[[ .keycloak_name ]].[[ .domain ]]"
        KC_HOSTNAME_STRICT = "false"
        KC_DB_URL          = "jdbc:postgresql://[[ .postgres_fqdn ]]/[[ .keycloak_database ]]?ssl=true&sslmode=require"
        KC_PROXY           = "edge"
      }

      template {
        destination = "${NOMAD_SECRETS_DIR}/app"
        env         = true
        data        = <<EOT
{{ with secret "secret/[[ .application_name ]]/[[ .keycloak_name ]]" }}
KC_DB_USERNAME = "{{ .Data.username }}"
KC_DB_PASSWORD = "{{ .Data.password }}"
{{ end }}
{{ with secret "secret/[[ .application_name ]]/keycloak-admin" }}
KEYCLOAK_ADMIN = "{{ .Data.username }}"
KEYCLOAK_ADMIN_PASSWORD = "{{ .Data.password }}"
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
        image        = "ghcr.io/noumenadigital/filebeat:[[ .filebeat_version ]]"
        network_mode = "host"
        args         = ["keycloak", "wildfly"]
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
