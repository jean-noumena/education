job "keycloak" {
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

  group "keycloak" {
    count = 1

    network {
      port "http" {
        static = 11000
      }
    }

    service {
      port = "http"
      name = "[[ .keycloak_name ]]"
      tags = [
        "version=[[ .version ]]",
        "traefik.enable=true",
        "traefik.frontend.rule=Host:[[ .keycloak_name ]].[[ .domain ]];PathPrefix:/",
        "traefik.frontend.entryPoints=internal",
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
        image        = "quay.io/keycloak/keycloak:19.0.0"
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
        KC_DB_URL          = "jdbc:postgresql://[[ .postgres_name ]].service.consul/[[ .keycloak_name ]]"
        KC_PROXY           = "edge"
      }

      template {
        destination = "${NOMAD_SECRETS_DIR}/psql"
        env         = true
        data        = <<EOT
{{ with secret "secret/postgres-v2/[[ .keycloak_name ]]" }}
KC_DB_USERNAME = "{{ .Data.username }}"
KC_DB_PASSWORD = "{{ .Data.password }}"
{{ end }}
{{ with secret "secret/seed/keycloak-admin" }}
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
        image        = "ghcr.io/noumenadigital/filebeat:1.0.3"
        network_mode = "host"
        args         = [
          "keycloak",
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
